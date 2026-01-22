package com.example.khaugali

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.MaterialAutoCompleteTextView

class AddMenuActivity : AppCompatActivity() {

    private val PICK_IMAGES = 100
    private var selectedImageUris: List<Uri> = emptyList()

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MenuAdapter
    private var menuList = mutableListOf<MenuItem>()

    private lateinit var dbHelper: UserDatabaseHelper
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_menu)

        val addMenuCapsule = findViewById<CardView>(R.id.addMenuCapsule)
        recyclerView = findViewById(R.id.recyclerMenu)

        // DB helper
        dbHelper = UserDatabaseHelper(this)

        // get logged in user id from SharedPreferences
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        userId = sharedPref.getInt("loggedInUserId", -1)

        // Initialize adapter first
        adapter = MenuAdapter(menuList)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Load saved data from server
        if (userId != -1) {
            loadMenuFromServer()
        } else {
            menuList = mutableListOf()
            adapter.notifyDataSetChanged()
        }

        addMenuCapsule.setOnClickListener {
            showAddMenuForm()
        }
    }

    private fun showAddMenuForm() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_menu, null)

        val etProductName = dialogView.findViewById<EditText>(R.id.etProductName)
        val etCategory = dialogView.findViewById<AutoCompleteTextView>(R.id.etCategory)
        val etPrice = dialogView.findViewById<EditText>(R.id.etPrice)
        val etPrepTime = dialogView.findViewById<EditText>(R.id.etPrepTime)
        val etIngredients = dialogView.findViewById<EditText>(R.id.etIngredients)
        val btnUploadImage = dialogView.findViewById<Button>(R.id.btnUploadImage)
        val etRating = dialogView.findViewById<EditText>(R.id.etRating)
        val ratingBar = dialogView.findViewById<RatingBar>(R.id.ratingBar)

        // Setup category dropdown
        val categories = listOf("Beverages", "Starters", "Soups", "Salads", "Main Course", "Desserts")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        etCategory.setAdapter(adapter)
        etCategory.setOnClickListener { etCategory.showDropDown() }

        // Update stars when user types rating
        etRating.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val value = s.toString().toFloatOrNull()
                if (value != null && value in 0.0..5.0) {
                    ratingBar.rating = value
                } else {
                    ratingBar.rating = 0f
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Auto-add ₹ for price
        etPrice.addTextChangedListener(object : TextWatcher {
            private var isEditing = false
            override fun afterTextChanged(s: Editable?) {
                if (isEditing) return
                isEditing = true
                val text = s.toString().replace("₹", "").trim()
                if (text.isNotEmpty()) {
                    etPrice.setText("₹$text")
                    etPrice.setSelection(etPrice.text.length)
                }
                isEditing = false
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Upload images
        btnUploadImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            startActivityForResult(intent, PICK_IMAGES)
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Add Menu Item")
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
                    val name = etProductName.text.toString()
                    val category = etCategory.text.toString()
                    val priceValue = etPrice.text.toString().replace("₹", "").trim()
                    val priceNumber = priceValue.toDoubleOrNull() ?: 0.0
                    val rating = etRating.text.toString().toFloat()
                    val prepTime = etPrepTime.text.toString() + " min"
                    val ingredients = etIngredients.text.toString()

                    val menuItem = MenuItem(
                        name = name,
                        images = selectedImageUris.map { it.toString() },
                        category = category,
                        price = priceNumber.toString(),
                        rating = rating,
                        prepTime = prepTime,
                        ingredients = ingredients
                    )

                    saveMenuItem(menuItem)
                }
            }
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .create()

        dialog.show()
    }

    private fun saveMenuItem(menuItem: MenuItem) {
        if (userId != -1) {
            // Upload images first if any
            if (menuItem.images.isNotEmpty()) {
                uploadImagesAndSaveMenuItem(menuItem)
            } else {
                saveMenuItemToServer(menuItem, emptyList())
            }
        } else {
            Toast.makeText(this, "Error: User not found", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun uploadImagesAndSaveMenuItem(menuItem: MenuItem) {
        val uploadedImageUrls = mutableListOf<String>()
        val totalImages = menuItem.images.size
        var uploadedCount = 0
        
        if (totalImages == 0) {
            saveMenuItemToServer(menuItem, emptyList())
            return
        }
        
        for (imageUri in menuItem.images) {
            try {
                val base64Image = ImageUtils.uriToBase64(this, android.net.Uri.parse(imageUri))
                val filename = ImageUtils.getFilenameFromUri(android.net.Uri.parse(imageUri))
                
                if (base64Image != null) {
                    val uploadRequest = ImageUploadRequest(base64Image, filename)
                    RetrofitClient.instance.uploadImage(uploadRequest).enqueue(object : retrofit2.Callback<ImageUploadResponse> {
                        override fun onResponse(call: retrofit2.Call<ImageUploadResponse>, response: retrofit2.Response<ImageUploadResponse>) {
                            uploadedCount++
                            if (response.isSuccessful && response.body()?.status == "success") {
                                val imageUrl = "http://192.168.56.174:5000" + response.body()!!.imageUrl
                                uploadedImageUrls.add(imageUrl)
                            }
                            
                            // When all images are processed, save menu item
                            if (uploadedCount == totalImages) {
                                saveMenuItemToServer(menuItem, uploadedImageUrls)
                            }
                        }
                        
                        override fun onFailure(call: retrofit2.Call<ImageUploadResponse>, t: Throwable) {
                            uploadedCount++
                            // When all images are processed, save menu item
                            if (uploadedCount == totalImages) {
                                saveMenuItemToServer(menuItem, uploadedImageUrls)
                            }
                        }
                    })
                } else {
                    uploadedCount++
                    if (uploadedCount == totalImages) {
                        saveMenuItemToServer(menuItem, uploadedImageUrls)
                    }
                }
            } catch (e: Exception) {
                uploadedCount++
                if (uploadedCount == totalImages) {
                    saveMenuItemToServer(menuItem, uploadedImageUrls)
                }
            }
        }
    }
    
    private fun saveMenuItemToServer(menuItem: MenuItem, serverImageUrls: List<String>) {
        val menuRequest = MenuItemRequest(
            userId = userId,
            name = menuItem.name,
            category = menuItem.category,
            price = menuItem.price,
            rating = menuItem.rating,
            prepTime = menuItem.prepTime,
            ingredients = menuItem.ingredients,
            images = serverImageUrls
        )
        
        RetrofitClient.instance.addMenuItem(menuRequest).enqueue(object : retrofit2.Callback<RegisterResponse> {
            override fun onResponse(call: retrofit2.Call<RegisterResponse>, response: retrofit2.Response<RegisterResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!
                    // Update menuItem with server ID and server image URLs
                    val updatedMenuItem = menuItem.copy(id = result.userId, images = serverImageUrls)
                    menuList.add(updatedMenuItem)
                    adapter.notifyItemInserted(menuList.size - 1)
                    Toast.makeText(this@AddMenuActivity, "Menu item saved to server with ${serverImageUrls.size} images", Toast.LENGTH_SHORT).show()
                } else {
                    // Fallback to local database
                    Toast.makeText(this@AddMenuActivity, "Server failed, saving locally...", Toast.LENGTH_SHORT).show()
                    saveMenuItemLocally(menuItem)
                }
            }

            override fun onFailure(call: retrofit2.Call<RegisterResponse>, t: Throwable) {
                // Fallback to local database
                Toast.makeText(this@AddMenuActivity, "Server unreachable, saving locally...", Toast.LENGTH_SHORT).show()
                saveMenuItemLocally(menuItem)
            }
        })
    }

    private fun saveMenuItemLocally(menuItem: MenuItem) {
        dbHelper.insertMenuItem(menuItem, userId)
        menuList.add(menuItem)
        adapter.notifyItemInserted(menuList.size - 1)
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
                    Toast.makeText(this@AddMenuActivity, "Loaded ${menuList.size} items from server", Toast.LENGTH_SHORT).show()
                } else {
                    // Fallback to local database
                    Toast.makeText(this@AddMenuActivity, "Server returned empty, trying local...", Toast.LENGTH_SHORT).show()
                    loadMenuFromLocal()
                }
            }

            override fun onFailure(call: retrofit2.Call<List<MenuItemResponse>>, t: Throwable) {
                // Fallback to local database
                Toast.makeText(this@AddMenuActivity, "Server unreachable, loading local data...", Toast.LENGTH_SHORT).show()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGES && resultCode == Activity.RESULT_OK) {
            val uris = mutableListOf<Uri>()
            if (data?.clipData != null) {
                val count = data.clipData!!.itemCount
                for (i in 0 until count) {
                    uris.add(data.clipData!!.getItemAt(i).uri)
                }
            } else if (data?.data != null) {
                uris.add(data.data!!)
            }
            selectedImageUris = uris
            Toast.makeText(this, "Selected ${uris.size} images", Toast.LENGTH_SHORT).show()
        }
    }
}
