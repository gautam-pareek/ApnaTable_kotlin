package com.example.khaugali

import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.min

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var profileImage: ImageView
    private lateinit var profileName: TextView
    private lateinit var profileLayout: LinearLayout
    private lateinit var aiQueryEditText: EditText
    private lateinit var sendAiQueryButton: Button
    private lateinit var searchResultsRecycler: RecyclerView
    private lateinit var searchAdapter: FoodSearchAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawerLayout = findViewById(R.id.drawerLayout)
        profileImage = findViewById(R.id.profileImage)
        profileName = findViewById(R.id.profileName)
        profileLayout = findViewById(R.id.profileLayout)
        aiQueryEditText = findViewById(R.id.aiQueryEditText)
        sendAiQueryButton = findViewById(R.id.sendAiQueryButton)

        // RecyclerView below the search bar
        searchResultsRecycler = findViewById(R.id.searchResultsRecycler)
        if (searchResultsRecycler != null) {
            searchResultsRecycler.layoutManager = LinearLayoutManager(this)
            
            // Initialize adapter with empty list
            searchAdapter = FoodSearchAdapter(emptyList()) { item ->
                // Open TableStatusActivity when food item is clicked in customer UI
                val intent = Intent(this, TableStatusActivity::class.java)
                startActivity(intent)
            }
            searchResultsRecycler.adapter = searchAdapter
        } else {
            Toast.makeText(this, "RecyclerView not found!", Toast.LENGTH_SHORT).show()
        }

        val drawerProfile = findViewById<TextView>(R.id.drawerProfile)
        val drawerAbout = findViewById<TextView>(R.id.drawerAbout)
        val drawerLogout = findViewById<TextView>(R.id.drawerLogout)

        // Load user data
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val name = sharedPref.getString("loggedInName", "Log In")
        val imageUri = sharedPref.getString("loggedInImageUri", null)
        profileName.text = name

        if (!imageUri.isNullOrEmpty()) {
            try {
                val uri = Uri.parse(imageUri)
                val inputStream = contentResolver.openInputStream(uri)
                val drawable = Drawable.createFromStream(inputStream, uri.toString())
                profileImage.setImageDrawable(drawable)
                inputStream?.close()
            } catch (e: Exception) {
                e.printStackTrace()
                profileImage.setImageResource(R.drawable.account)
            }
        } else {
            profileImage.setImageResource(R.drawable.account)
        }

        // Drawer click handling
        profileLayout.setOnClickListener { drawerLayout.openDrawer(GravityCompat.END) }
        drawerProfile.setOnClickListener { Toast.makeText(this, "Profile clicked", Toast.LENGTH_SHORT).show() }
        drawerAbout.setOnClickListener { Toast.makeText(this, "About clicked", Toast.LENGTH_SHORT).show() }
        drawerLogout.setOnClickListener {
            sharedPref.edit().clear().apply()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Window insets fix
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainContent)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Search button click
        sendAiQueryButton.setOnClickListener {
            val query = aiQueryEditText.text.toString().trim()
            if (query.isEmpty()) return@setOnClickListener

            // Show loading
            sendAiQueryButton.isEnabled = false
            sendAiQueryButton.text = "Searching..."

            // Try server search first
            RetrofitClient.instance.searchMenu(query).enqueue(object : retrofit2.Callback<List<MenuItemResponse>> {
                override fun onResponse(call: retrofit2.Call<List<MenuItemResponse>>, response: retrofit2.Response<List<MenuItemResponse>>) {
                    sendAiQueryButton.isEnabled = true
                    sendAiQueryButton.text = "Search"

                    if (response.isSuccessful && response.body() != null) {
                        val serverResults = response.body()!!
                        
                        // Convert server response to MenuItem format
                        val menuItems = serverResults.map { serverItem ->
                            MenuItem(
                                id = serverItem.id,
                                name = serverItem.name,
                                images = serverItem.images,
                                category = serverItem.category,
                                price = serverItem.price,
                                rating = serverItem.rating,
                                prepTime = serverItem.prepTime,
                                ingredients = serverItem.ingredients
                            )
                        }

                        // Update existing adapter with new data
                        updateSearchResults(menuItems, "server")
                        
                        Toast.makeText(this@MainActivity, "Found ${menuItems.size} items from server", Toast.LENGTH_SHORT).show()
                    } else {
                        // Fallback to local database if server fails
                        Toast.makeText(this@MainActivity, "Server search failed, trying local...", Toast.LENGTH_SHORT).show()
                        tryLocalSearch(query)
                    }
                }

                override fun onFailure(call: retrofit2.Call<List<MenuItemResponse>>, t: Throwable) {
                    sendAiQueryButton.isEnabled = true
                    sendAiQueryButton.text = "Search"
                    
                    // Fallback to local database if server is unreachable
                    Toast.makeText(this@MainActivity, "Server unreachable, trying local...", Toast.LENGTH_SHORT).show()
                    tryLocalSearch(query)
                }
            })
        }
    }

    // Fuzzy match to handle typos
    private fun fuzzyMatch(query: String, text: String): Boolean {
        val q = query.lowercase()
        val t = text.lowercase()
        if (t.contains(q)) return true
        return levenshtein(q, t) <= 2
    }

    // Levenshtein distance
    private fun levenshtein(lhs: String, rhs: String): Int {
        val lhsLen = lhs.length
        val rhsLen = rhs.length
        val dp = Array(lhsLen + 1) { IntArray(rhsLen + 1) }
        for (i in 0..lhsLen) dp[i][0] = i
        for (j in 0..rhsLen) dp[0][j] = j
        for (i in 1..lhsLen) {
            for (j in 1..rhsLen) {
                dp[i][j] = if (lhs[i - 1] == rhs[j - 1]) dp[i - 1][j - 1]
                else 1 + min(min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1])
            }
        }
        return dp[lhsLen][rhsLen]
    }

    // Fallback to local database search
    private fun tryLocalSearch(query: String) {
        val dbHelper = UserDatabaseHelper(this)
        val allMenu = dbHelper.searchMenuItems(query) // fetch all menu items

        val results = allMenu.filter {
            fuzzyMatch(query, it.name) || fuzzyMatch(query, it.category)
        }

        updateSearchResults(results, "local")
        
        Toast.makeText(this, "Found ${results.size} items from local database", Toast.LENGTH_SHORT).show()
    }

    private fun updateSearchResults(menuItems: List<MenuItem>, source: String) {
        try {
            // Create new adapter with the results
            searchAdapter = FoodSearchAdapter(menuItems) { item ->
                // Open TableStatusActivity when food item is clicked in customer UI
                val intent = Intent(this, TableStatusActivity::class.java)
                startActivity(intent)
            }
            searchResultsRecycler.adapter = searchAdapter
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error displaying search results: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
