package com.example.khaugali

data class MenuItem(
    val id: Int = 0,           // required for DB operations
    val name: String,
    val images: List<String>,
    val category: String,
    val price: String,
    val rating: Float,
    val prepTime: String,
    val ingredients: String
)
