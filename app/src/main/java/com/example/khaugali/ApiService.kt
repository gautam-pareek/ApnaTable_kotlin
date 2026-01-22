package com.example.khaugali

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

// Request/Response Data Classes
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val imageUri: String?,
    val userType: String
)

data class RegisterResponse(
    val status: String,
    val userId: Int,
    val message: String? = null
)

data class LoginRequest(
    val identifier: String,
    val password: String,
    val userType: String
)

data class LoginResponse(
    val id: Int,
    val name: String,
    val email: String,
    val imageUri: String?,
    val userType: String,
    val status: String? = null,
    val message: String? = null
)

data class MenuItemRequest(
    val userId: Int,
    val name: String,
    val category: String,
    val price: String,
    val rating: Float = 0f,
    val prepTime: String = "",
    val ingredients: String = "",
    val images: List<String> = emptyList()
)

data class MenuItemResponse(
    val id: Int,
    val userId: Int,
    val name: String,
    val category: String,
    val price: String,
    val rating: Float,
    val prepTime: String,
    val ingredients: String,
    val images: List<String>
)

data class LayoutRequest(
    val userId: Int,
    val gridRow: Int,
    val gridCol: Int,
    val type: String,
    val label: String = ""
)

data class LayoutResponse(
    val id: Int,
    val userId: Int,
    val gridRow: Int,
    val gridCol: Int,
    val type: String,
    val label: String,
    val status: String
)

data class ImageUploadRequest(
    val image: String,  // Base64 encoded image
    val filename: String
)

data class ImageUploadResponse(
    val status: String,
    val imageUrl: String? = null,
    val message: String? = null
)

// API Service Interface
interface ApiService {
    // Authentication
    @POST("register")
    fun register(@Body request: RegisterRequest): Call<RegisterResponse>

    @POST("login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    // Menu Management
    @GET("menu/{userId}")
    fun getMenu(@Path("userId") userId: Int): Call<List<MenuItemResponse>>

    @POST("menu")
    fun addMenuItem(@Body request: MenuItemRequest): Call<RegisterResponse>

    @PUT("menu/{menuId}")
    fun updateMenuItem(@Path("menuId") menuId: Int, @Body request: MenuItemRequest): Call<RegisterResponse>

    @DELETE("menu/{menuId}")
    fun deleteMenuItem(@Path("menuId") menuId: Int): Call<RegisterResponse>

    // Search
    @GET("search")
    fun searchMenu(@Query("q") query: String): Call<List<MenuItemResponse>>

    // Image Upload
    @POST("upload-image")
    fun uploadImage(@Body request: ImageUploadRequest): Call<ImageUploadResponse>

    // Layout Management
    @POST("layouts")
    fun addLayout(@Body request: LayoutRequest): Call<RegisterResponse>

    @GET("layouts/{userId}")
    fun getLayouts(@Path("userId") userId: Int): Call<List<LayoutResponse>>

    @DELETE("layouts/{userId}")
    fun deleteLayouts(@Path("userId") userId: Int): Call<RegisterResponse>

    // Health Check
    @GET("/")
    fun healthCheck(): Call<Map<String, String>>
}
