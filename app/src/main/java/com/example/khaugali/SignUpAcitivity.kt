package com.example.khaugali

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import java.io.File
import java.io.FileOutputStream

class SignUpActivity : AppCompatActivity() {
    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false
    private var selectedImageUri: Uri? = null

    private val REQUEST_IMAGE_GALLERY = 100
    private val REQUEST_IMAGE_CAMERA = 101
    private val REQUEST_CAMERA_PERMISSION = 102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up_acitivity)

        val nameEditText = findViewById<EditText>(R.id.nameEditText)
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.signupPasswordEditText)
        val confirmPasswordEditText = findViewById<EditText>(R.id.confirmPasswordEditText)
        val signupButton = findViewById<MaterialButton>(R.id.signupButton)
        val profileImage = findViewById<ImageView>(R.id.profileImageView)

        val galleryButton = findViewById<Button>(R.id.pickGalleryButton)
        val cameraButton = findViewById<Button>(R.id.takePhotoButton)

        val eyeOpenIcon = ContextCompat.getDrawable(this, R.drawable.ic_eye_on)
        val eyeOffIcon = ContextCompat.getDrawable(this, R.drawable.ic_eye_off)

        // Pick from gallery
        galleryButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "image/*"
            startActivityForResult(intent, REQUEST_IMAGE_GALLERY)
        }

        // Take photo
        cameraButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
            } else {
                openCamera()
            }
        }

        // Also allow click on image to open gallery
        profileImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "image/*"
            startActivityForResult(intent, REQUEST_IMAGE_GALLERY)
        }

        // Password toggle
        passwordEditText.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (passwordEditText.right - passwordEditText.compoundPaddingEnd)) {
                    isPasswordVisible = !isPasswordVisible
                    if (isPasswordVisible) {
                        passwordEditText.transformationMethod = HideReturnsTransformationMethod.getInstance()
                        passwordEditText.setCompoundDrawablesWithIntrinsicBounds(null, null, eyeOpenIcon, null)
                    } else {
                        passwordEditText.transformationMethod = PasswordTransformationMethod.getInstance()
                        passwordEditText.setCompoundDrawablesWithIntrinsicBounds(null, null, eyeOffIcon, null)
                    }
                    passwordEditText.setSelection(passwordEditText.text.length)
                    return@setOnTouchListener true
                }
            }
            false
        }

        // Confirm password toggle
        confirmPasswordEditText.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (confirmPasswordEditText.right - confirmPasswordEditText.compoundPaddingEnd)) {
                    isConfirmPasswordVisible = !isConfirmPasswordVisible
                    if (isConfirmPasswordVisible) {
                        confirmPasswordEditText.transformationMethod = HideReturnsTransformationMethod.getInstance()
                        confirmPasswordEditText.setCompoundDrawablesWithIntrinsicBounds(null, null, eyeOpenIcon, null)
                    } else {
                        confirmPasswordEditText.transformationMethod = PasswordTransformationMethod.getInstance()
                        confirmPasswordEditText.setCompoundDrawablesWithIntrinsicBounds(null, null, eyeOffIcon, null)
                    }
                    confirmPasswordEditText.setSelection(confirmPasswordEditText.text.length)
                    return@setOnTouchListener true
                }
            }
            false
        }

        // Signup button click
        signupButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            val userType = intent.getStringExtra("userType") ?: "customer"

            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            } else {
                // Show loading
                signupButton.isEnabled = false
                signupButton.text = "Signing up..."

                // Try server registration first
                val registerRequest = RegisterRequest(
                    name = name,
                    email = email,
                    password = password,
                    imageUri = selectedImageUri?.toString(),
                    userType = userType
                )
                
                RetrofitClient.instance.register(registerRequest).enqueue(object : retrofit2.Callback<RegisterResponse> {
                    override fun onResponse(call: retrofit2.Call<RegisterResponse>, response: retrofit2.Response<RegisterResponse>) {
                        signupButton.isEnabled = true
                        signupButton.text = "Sign Up"

                        if (response.isSuccessful && response.body() != null) {
                            val result = response.body()!!
                            Toast.makeText(this@SignUpActivity, "Sign up successful (Server)", Toast.LENGTH_SHORT).show()
                            
                            val intent = Intent(this@SignUpActivity, LoginActivity::class.java)
                            intent.putExtra("userType", userType)
                            startActivity(intent)
                            finish()
                        } else {
                            // Fallback to local database if server fails
                            Toast.makeText(this@SignUpActivity, "Server registration failed, trying local...", Toast.LENGTH_SHORT).show()
                            tryLocalSignup(name, email, password, selectedImageUri, userType)
                        }
                    }

                    override fun onFailure(call: retrofit2.Call<RegisterResponse>, t: Throwable) {
                        signupButton.isEnabled = true
                        signupButton.text = "Sign Up"
                        
                        // Fallback to local database if server is unreachable
                        Toast.makeText(this@SignUpActivity, "Server unreachable, trying local...", Toast.LENGTH_SHORT).show()
                        tryLocalSignup(name, email, password, selectedImageUri, userType)
                    }
                })
            }
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, REQUEST_IMAGE_CAMERA)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val profileImage = findViewById<ImageView>(R.id.profileImageView)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_GALLERY -> {
                    val uri = data?.data
                    try {
                        uri?.let {
                            contentResolver.takePersistableUriPermission(
                                it,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        }

                        val bitmap = getDownscaledBitmap(this, uri!!, 1024)
                        if (bitmap != null) {
                            profileImage.setImageBitmap(bitmap)
                            selectedImageUri = saveImageToCache(bitmap)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                REQUEST_IMAGE_CAMERA -> {
                    val bitmap = data?.extras?.get("data") as? Bitmap
                    profileImage.setImageBitmap(bitmap)
                    selectedImageUri = saveImageToCache(bitmap!!)
                }
            }
        }
    }

    private fun saveImageToCache(bitmap: Bitmap): Uri {
        val file = File(cacheDir, "profile_image_${System.currentTimeMillis()}.jpg")
        val out = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        out.flush()
        out.close()
        return Uri.fromFile(file)
    }

    private fun getDownscaledBitmap(context: Context, imageUri: Uri, maxSize: Int): Bitmap? {
        val inputStream1 = context.contentResolver.openInputStream(imageUri)
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeStream(inputStream1, null, options)
        inputStream1?.close()

        val scale = maxOf(1, maxOf(options.outWidth / maxSize, options.outHeight / maxSize))

        val inputStream2 = context.contentResolver.openInputStream(imageUri)
        val options2 = BitmapFactory.Options().apply { inSampleSize = scale }
        val bitmap = BitmapFactory.decodeStream(inputStream2, null, options2)
        inputStream2?.close()

        return bitmap
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Fallback to local database signup
    private fun tryLocalSignup(name: String, email: String, password: String, selectedImageUri: Uri?, userType: String) {
        val dbHelper = UserDatabaseHelper(this)
        val user = User(
            id = 0, // Will be auto-generated by database
            name = name,
            email = email,
            password = password,
            imageUri = selectedImageUri?.toString() ?: "",
            userType = userType
        )
        val success = dbHelper.insertUser(user)
        if (success) {
            Toast.makeText(this, "Sign up successful (Local)", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.putExtra("userType", userType)
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, "Sign up failed", Toast.LENGTH_SHORT).show()
        }
    }
}
