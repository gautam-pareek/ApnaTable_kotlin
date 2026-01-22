package com.example.khaugali

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.MotionEvent
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton

class LoginActivity : AppCompatActivity() {
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val loginButton = findViewById<MaterialButton>(R.id.loginButton)
        val signupText = findViewById<TextView>(R.id.signupText)
        val backToUserTypeText = findViewById<TextView>(R.id.backToUserTypeText) // 🔄 Optional link to go back manually

        // Get userType from previous screen
        val userType = intent.getStringExtra("userType") ?: "customer"

        // Password visibility toggle
        val eyeOpenIcon = ContextCompat.getDrawable(this, R.drawable.ic_eye_on)
        val eyeOffIcon = ContextCompat.getDrawable(this, R.drawable.ic_eye_off)
        passwordEditText.setCompoundDrawablesWithIntrinsicBounds(null, null, eyeOffIcon, null)

        passwordEditText.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP &&
                event.rawX >= (passwordEditText.right - passwordEditText.compoundPaddingEnd)
            ) {
                isPasswordVisible = !isPasswordVisible
                passwordEditText.transformationMethod = if (isPasswordVisible)
                    HideReturnsTransformationMethod.getInstance()
                else
                    PasswordTransformationMethod.getInstance()

                passwordEditText.setCompoundDrawablesWithIntrinsicBounds(
                    null, null,
                    if (isPasswordVisible) eyeOpenIcon else eyeOffIcon,
                    null
                )
                passwordEditText.setSelection(passwordEditText.text.length)
                true
            } else false
        }

        // Handle login
        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (TextUtils.isEmpty(email)) {
                Toast.makeText(this, "Please enter your email or username", Toast.LENGTH_SHORT).show()
            } else if (TextUtils.isEmpty(password)) {
                Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show()
            } else {
                // Show loading
                loginButton.isEnabled = false
                loginButton.text = "Logging in..."

                // Try server login first
                val loginRequest = LoginRequest(email, password, userType)
                RetrofitClient.instance.login(loginRequest).enqueue(object : retrofit2.Callback<LoginResponse> {
                    override fun onResponse(call: retrofit2.Call<LoginResponse>, response: retrofit2.Response<LoginResponse>) {
                        loginButton.isEnabled = true
                        loginButton.text = "Login"

                        if (response.isSuccessful && response.body() != null) {
                            val user = response.body()!!
                            
                            // Store login session
                            val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                            with(sharedPref.edit()) {
                                putInt("loggedInUserId", user.id)
                                putString("loggedInName", user.name)
                                putString("loggedInImageUri", user.imageUri)
                                putString("userType", userType)
                                apply()
                            }

                            Toast.makeText(this@LoginActivity, "Login successful (Server)", Toast.LENGTH_SHORT).show()

                            // Redirect to respective home screen
                            val nextIntent = if (userType == "business") {
                                Intent(this@LoginActivity, BusinessMainActivity::class.java)
                            } else {
                                Intent(this@LoginActivity, MainActivity::class.java)
                            }
                            nextIntent.putExtra("userType", userType)
                            nextIntent.putExtra("USER_ID", user.id)

                            startActivity(nextIntent)
                            finish()
                        } else {
                            // Fallback to local database if server fails
                            Toast.makeText(this@LoginActivity, "Server login failed, trying local...", Toast.LENGTH_SHORT).show()
                            tryLocalLogin(email, password, userType)
                        }
                    }

                    override fun onFailure(call: retrofit2.Call<LoginResponse>, t: Throwable) {
                        loginButton.isEnabled = true
                        loginButton.text = "Login"
                        
                        // Fallback to local database if server is unreachable
                        Toast.makeText(this@LoginActivity, "Server unreachable, trying local...", Toast.LENGTH_SHORT).show()
                        tryLocalLogin(email, password, userType)
                    }
                })
            }
        }

        // Go to signup screen, keeping user type
        signupText.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            intent.putExtra("userType", userType)
            startActivity(intent)
        }

        // ⬅️ Optional: go back manually to user type selector
        backToUserTypeText.setOnClickListener {
            val intent = Intent(this, User_Type::class.java)
            startActivity(intent)
            finish()
        }
    }

    // ⬅️ System back also goes to user type selector
    override fun onBackPressed() {
        val intent = Intent(this, User_Type::class.java)
        startActivity(intent)
        super.onBackPressed()
        finish()

    }

    // Fallback to local database login
    private fun tryLocalLogin(email: String, password: String, userType: String) {
        val dbHelper = UserDatabaseHelper(this)
        val user = dbHelper.getUser(email, password, userType)

        if (user != null) {
            // Store login session
            val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
            with(sharedPref.edit()) {
                putInt("loggedInUserId", user.id)
                putString("loggedInName", user.name)
                putString("loggedInImageUri", user.imageUri)
                putString("userType", userType)
                apply()
            }

            Toast.makeText(this, "Login successful (Local)", Toast.LENGTH_SHORT).show()

            // Redirect to respective home screen
            val nextIntent = if (userType == "business") {
                Intent(this, BusinessMainActivity::class.java)
            } else {
                Intent(this, MainActivity::class.java)
            }
            nextIntent.putExtra("userType", userType)
            nextIntent.putExtra("USER_ID", user.id)

            startActivity(nextIntent)
            finish()
        } else {
            Toast.makeText(this, "Invalid credentials or wrong user type", Toast.LENGTH_SHORT).show()
        }
    }
}
