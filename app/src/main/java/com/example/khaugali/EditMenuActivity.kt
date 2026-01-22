package com.example.khaugali

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class EditMenuActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MenuAdapter
    private var menuList = mutableListOf<MenuItem>()

    private lateinit var dbHelper: UserDatabaseHelper
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_menu)

        recyclerView = findViewById(R.id.recyclerMenu)
        dbHelper = UserDatabaseHelper(this)

        // Get logged-in user ID from SharedPreferences
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        userId = sharedPref.getInt("loggedInUserId", -1)
        if (userId == -1) {
            Toast.makeText(this, "User not found. Please login again.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Initialize adapter first
        adapter = MenuAdapter(menuList,
            showDelete = true,   // Enable delete button for Edit page
            onItemClick = { menuItem -> openAddMenuDialog(menuItem) },
            onDeleteClick = { menuItem -> confirmDelete(menuItem) }
        )
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Load menu items from server
        loadMenuFromServer()
    }

    // ---------- DELETE WITH CONFIRMATION ----------
    private fun confirmDelete(menuItem: MenuItem) {
        AlertDialog.Builder(this)
            .setTitle("Delete Menu Item")
            .setMessage("Are you sure you want to delete '${menuItem.name}'?")
            .setPositiveButton("Yes") { _, _ ->
                // Try server delete first
                RetrofitClient.instance.deleteMenuItem(menuItem.id).enqueue(object : retrofit2.Callback<RegisterResponse> {
                    override fun onResponse(call: retrofit2.Call<RegisterResponse>, response: retrofit2.Response<RegisterResponse>) {
                        if (response.isSuccessful) {
                            val index = menuList.indexOfFirst { it.id == menuItem.id }
                            if (index != -1) {
                                menuList.removeAt(index)
                                adapter.notifyItemRemoved(index)
                            }
                            Toast.makeText(this@EditMenuActivity, "Deleted from server", Toast.LENGTH_SHORT).show()
                        } else {
                            // Fallback to local delete
                            deleteMenuItemLocally(menuItem)
                        }
                    }

                    override fun onFailure(call: retrofit2.Call<RegisterResponse>, t: Throwable) {
                        // Fallback to local delete
                        deleteMenuItemLocally(menuItem)
                    }
                })
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun deleteMenuItemLocally(menuItem: MenuItem) {
        dbHelper.deleteMenuItem(menuItem.id)
        val index = menuList.indexOfFirst { it.id == menuItem.id }
        if (index != -1) {
            menuList.removeAt(index)
            adapter.notifyItemRemoved(index)
        }
        Toast.makeText(this, "Deleted locally", Toast.LENGTH_SHORT).show()
    }

    // ---------- OPEN ADD MENU FORM PRE-FILLED ----------
    private fun openAddMenuDialog(menuItem: MenuItem) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_menu, null)

        val etProductName = dialogView.findViewById<EditText>(R.id.etProductName)
        val etCategory = dialogView.findViewById<AutoCompleteTextView>(R.id.etCategory)
        val etPrice = dialogView.findViewById<EditText>(R.id.etPrice)
        val etPrepTime = dialogView.findViewById<EditText>(R.id.etPrepTime)
        val etIngredients = dialogView.findViewById<EditText>(R.id.etIngredients)
        val etRating = dialogView.findViewById<EditText>(R.id.etRating)
        val ratingBar = dialogView.findViewById<RatingBar>(R.id.ratingBar)

        // Pre-fill form fields
        etProductName.setText(menuItem.name)
        etCategory.setText(menuItem.category)
        etPrice.setText(menuItem.price)
        etPrepTime.setText(menuItem.prepTime.replace(" min", ""))
        etIngredients.setText(menuItem.ingredients)
        etRating.setText(menuItem.rating.toString())
        ratingBar.rating = menuItem.rating

        // Category dropdown
        val categories = listOf("Beverages", "Starters", "Soups", "Salads", "Main Course", "Desserts")
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        etCategory.setAdapter(categoryAdapter)
        etCategory.setOnClickListener { etCategory.showDropDown() }

        // Rating text updates RatingBar
        etRating.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val value = s.toString().toFloatOrNull()
                ratingBar.rating = if (value != null && value in 0.0..5.0) value else 0f
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Price ₹ formatting
        etPrice.addTextChangedListener(object : TextWatcher {
            private var isEditing = false
            override fun afterTextChanged(s: Editable?) {
                if (isEditing) return
                isEditing = true
                val text = s.toString().replace("₹", "").trim()
                if (text.isNotEmpty()) etPrice.setText("₹$text")
                etPrice.setSelection(etPrice.text.length)
                isEditing = false
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Edit Menu Item")
            .setPositiveButton("Save") { _, _ ->
                if (etProductName.text.isNullOrEmpty() ||
                    etCategory.text.isNullOrEmpty() ||
                    etPrice.text.isNullOrEmpty() ||
                    etRating.text.isNullOrEmpty() ||
                    etPrepTime.text.isNullOrEmpty() ||
                    etIngredients.text.isNullOrEmpty()
                ) {
                    Toast.makeText(this, "All fields are required!", Toast.LENGTH_SHORT).show()
                } else {
                    val updatedItem = MenuItem(
                        id = menuItem.id,
                        name = etProductName.text.toString(),
                        images = menuItem.images, // keep existing images
                        category = etCategory.text.toString(),
                        price = etPrice.text.toString(),
                        rating = etRating.text.toString().toFloatOrNull() ?: 0f,
                        prepTime = etPrepTime.text.toString() + " min",
                        ingredients = etIngredients.text.toString()
                    )
                    // Try server update first
                    val menuRequest = MenuItemRequest(
                        userId = userId,
                        name = updatedItem.name,
                        category = updatedItem.category,
                        price = updatedItem.price,
                        rating = updatedItem.rating,
                        prepTime = updatedItem.prepTime,
                        ingredients = updatedItem.ingredients,
                        images = updatedItem.images
                    )
                    
                    RetrofitClient.instance.updateMenuItem(updatedItem.id, menuRequest).enqueue(object : retrofit2.Callback<RegisterResponse> {
                        override fun onResponse(call: retrofit2.Call<RegisterResponse>, response: retrofit2.Response<RegisterResponse>) {
                            if (response.isSuccessful) {
                                val index = menuList.indexOfFirst { it.id == updatedItem.id }
                                if (index != -1) {
                                    menuList[index] = updatedItem
                                    adapter.notifyItemChanged(index)
                                }
                                Toast.makeText(this@EditMenuActivity, "Updated on server", Toast.LENGTH_SHORT).show()
                            } else {
                                // Fallback to local update
                                updateMenuItemLocally(updatedItem)
                            }
                        }

                        override fun onFailure(call: retrofit2.Call<RegisterResponse>, t: Throwable) {
                            // Fallback to local update
                            updateMenuItemLocally(updatedItem)
                        }
                    })
                }
            }
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .create()

        dialog.show()
    }

    private fun updateMenuItemLocally(updatedItem: MenuItem) {
        dbHelper.updateMenuItem(updatedItem)
        val index = menuList.indexOfFirst { it.id == updatedItem.id }
        if (index != -1) {
            menuList[index] = updatedItem
            adapter.notifyItemChanged(index)
        }
        Toast.makeText(this, "Updated locally", Toast.LENGTH_SHORT).show()
    }

    private fun loadMenuFromServer() {
        RetrofitClient.instance.getMenu(userId).enqueue(object : retrofit2.Callback<List<MenuItemResponse>> {
            override fun onResponse(call: retrofit2.Call<List<MenuItemResponse>>, response: retrofit2.Response<List<MenuItemResponse>>) {
                if (response.isSuccessful && response.body() != null) {
                    val serverMenu = response.body()!!
                    
                    // Convert server response to MenuItem format
                    val newMenuItems = serverMenu.map { serverItem ->
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
                    
                    // Clear and add new items to existing list
                    menuList.clear()
                    menuList.addAll(newMenuItems)
                    adapter.notifyDataSetChanged()
                    Toast.makeText(this@EditMenuActivity, "Loaded ${menuList.size} items from server", Toast.LENGTH_SHORT).show()
                } else {
                    // Fallback to local database
                    Toast.makeText(this@EditMenuActivity, "Server returned empty, trying local...", Toast.LENGTH_SHORT).show()
                    loadMenuFromLocal()
                }
            }

            override fun onFailure(call: retrofit2.Call<List<MenuItemResponse>>, t: Throwable) {
                // Fallback to local database
                Toast.makeText(this@EditMenuActivity, "Server unreachable, loading local data...", Toast.LENGTH_SHORT).show()
                loadMenuFromLocal()
            }
        })
    }

    private fun loadMenuFromLocal() {
        val localItems = dbHelper.getMenuItems(userId)
        menuList.clear()
        menuList.addAll(localItems)
        adapter.notifyDataSetChanged()
        Toast.makeText(this, "Loaded ${menuList.size} items from local database", Toast.LENGTH_SHORT).show()
    }
}
