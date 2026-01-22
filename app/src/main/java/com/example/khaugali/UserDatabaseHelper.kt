package com.example.khaugali

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class LayoutItem(val row: Int, val col: Int, val type: String, val label: String)

class UserDatabaseHelper(context: Context) : SQLiteOpenHelper(context, "UserDB", null, 4) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE Users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT, " +
                    "email TEXT UNIQUE, " +  // enforce unique emails
                    "password TEXT, " +
                    "imageUri TEXT, " +
                    "userType TEXT)"
        )

        db.execSQL(
            "CREATE TABLE Menu (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "userId INTEGER, " +
                    "name TEXT, " +
                    "category TEXT, " +
                    "price REAL, " +
                    "rating REAL, " +
                    "prepTime TEXT, " +
                    "ingredients TEXT, " +
                    "images TEXT, " +
                    "FOREIGN KEY(userId) REFERENCES Users(id))"
        )

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS Layouts (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "userId INTEGER, " +
                    "gridRow INTEGER, " +
                    "gridCol INTEGER, " +
                    "type TEXT, " +
                    "label TEXT, " +
                    "FOREIGN KEY(userId) REFERENCES Users(id))"
        )

        // Table for booking information
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS TableBookings (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "tableId TEXT UNIQUE, " +
                    "customerName TEXT, " +
                    "timeFrom TEXT, " +
                    "timeTo TEXT, " +
                    "bookingDate TEXT)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS Users")
        db.execSQL("DROP TABLE IF EXISTS Menu")
        db.execSQL("DROP TABLE IF EXISTS Layouts")
        onCreate(db)
    }

    // ---------------- USERS ----------------

    fun insertUser(user: User): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put("name", user.name)
            put("email", user.email)
            put("password", user.password)
            put("imageUri", user.imageUri)
            put("userType", user.userType)
        }
        val result = db.insert("Users", null, contentValues)
        db.close()
        return result != -1L
    }

    fun getUser(identifier: String, password: String, userType: String): User? {
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM Users WHERE (email=? OR name=?) AND password=? AND userType=?",
            arrayOf(identifier, identifier, password, userType)
        )

        var user: User? = null
        if (cursor.moveToFirst()) {
            user = User(
                id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                email = cursor.getString(cursor.getColumnIndexOrThrow("email")),
                password = cursor.getString(cursor.getColumnIndexOrThrow("password")),
                imageUri = cursor.getString(cursor.getColumnIndexOrThrow("imageUri")),
                userType = cursor.getString(cursor.getColumnIndexOrThrow("userType"))
            )
        }
        cursor.close()
        db.close()
        return user
    }

    fun getAllUsers(): List<User> {
        val list = mutableListOf<User>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM Users", null)
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    User(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                        name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                        email = cursor.getString(cursor.getColumnIndexOrThrow("email")),
                        password = cursor.getString(cursor.getColumnIndexOrThrow("password")),
                        imageUri = cursor.getString(cursor.getColumnIndexOrThrow("imageUri")),
                        userType = cursor.getString(cursor.getColumnIndexOrThrow("userType"))
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }

    // ---------------- MENU ----------------

    fun insertMenuItem(menuItem: MenuItem, userId: Int) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("userId", userId)
            put("name", menuItem.name)
            put("category", menuItem.category)
            val numericPrice = menuItem.price.replace("₹", "").trim().toDoubleOrNull() ?: 0.0
            put("price", numericPrice)
            put("rating", menuItem.rating)
            put("prepTime", menuItem.prepTime)
            put("ingredients", menuItem.ingredients)
            put("images", menuItem.images.joinToString(","))
        }
        db.insert("Menu", null, values)
        db.close()
    }

    fun getMenuItems(userId: Int): List<MenuItem> {
        val list = mutableListOf<MenuItem>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM Menu WHERE userId = ?", arrayOf(userId.toString()))

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                val category = cursor.getString(cursor.getColumnIndexOrThrow("category"))
                val price = cursor.getDouble(cursor.getColumnIndexOrThrow("price"))
                val rating = cursor.getFloat(cursor.getColumnIndexOrThrow("rating"))
                val prepTime = cursor.getString(cursor.getColumnIndexOrThrow("prepTime"))
                val ingredients = cursor.getString(cursor.getColumnIndexOrThrow("ingredients"))
                val images = cursor.getString(cursor.getColumnIndexOrThrow("images")).split(",")

                list.add(
                    MenuItem(
                        id = id,
                        name = name,
                        images = images,
                        category = category,
                        price = "₹$price",
                        rating = rating,
                        prepTime = prepTime,
                        ingredients = ingredients
                    )
                )
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return list
    }

    fun updateMenuItem(menuItem: MenuItem) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("name", menuItem.name)
            put("category", menuItem.category)
            val numericPrice = menuItem.price.replace("₹", "").trim().toDoubleOrNull() ?: 0.0
            put("price", numericPrice)
            put("rating", menuItem.rating)
            put("prepTime", menuItem.prepTime)
            put("ingredients", menuItem.ingredients)
            put("images", menuItem.images.joinToString(","))
        }
        db.update("Menu", values, "id=?", arrayOf(menuItem.id.toString()))
        db.close()
    }

    fun deleteMenuItem(id: Int) {
        val db = writableDatabase
        db.delete("Menu", "id=?", arrayOf(id.toString()))
        db.close()
    }

    // ---------------- LAYOUTS ----------------

    fun insertLayoutItem(userId: Int, gridRow: Int, gridCol: Int, type: String, label: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("userId", userId)
            put("gridRow", gridRow)
            put("gridCol", gridCol)
            put("type", type)
            put("label", label)
        }
        db.insert("Layouts", null, values)
        db.close()
    }

    fun getLayoutItems(userId: Int): List<LayoutItem> {

        val list = mutableListOf<LayoutItem>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM Layouts WHERE userId=?", arrayOf(userId.toString()))
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    LayoutItem(
                        row = cursor.getInt(cursor.getColumnIndexOrThrow("gridRow")),
                        col = cursor.getInt(cursor.getColumnIndexOrThrow("gridCol")),
                        type = cursor.getString(cursor.getColumnIndexOrThrow("type")),
                        label = cursor.getString(cursor.getColumnIndexOrThrow("label"))
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }

    fun deleteLayouts(userId: Int) {
        val db = writableDatabase
        db.delete("Layouts", "userId=?", arrayOf(userId.toString()))
        db.close()
    }

    // ---------------- MENU SEARCH ----------------
    fun searchMenuItems(query: String): List<MenuItem> {
        val list = mutableListOf<MenuItem>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM Menu WHERE LOWER(name) LIKE ? OR LOWER(category) LIKE ?",
            arrayOf("%${query.lowercase()}%", "%${query.lowercase()}%")
        )
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                val category = cursor.getString(cursor.getColumnIndexOrThrow("category"))
                val price = cursor.getDouble(cursor.getColumnIndexOrThrow("price"))
                val rating = cursor.getFloat(cursor.getColumnIndexOrThrow("rating"))
                val prepTime = cursor.getString(cursor.getColumnIndexOrThrow("prepTime"))
                val ingredients = cursor.getString(cursor.getColumnIndexOrThrow("ingredients"))
                val images = cursor.getString(cursor.getColumnIndexOrThrow("images")).split(",")

                list.add(
                    MenuItem(
                        id,
                        name,
                        images,
                        category,
                        "₹$price",
                        rating,
                        prepTime,
                        ingredients
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }

    // ---------------- TABLE BOOKINGS ----------------

    fun saveTableBooking(booking: TableBooking): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("tableId", booking.tableId)
            put("customerName", booking.customerName)
            put("timeFrom", booking.timeFrom)
            put("timeTo", booking.timeTo)
            put("bookingDate", booking.bookingDate)
        }
        
        // Try to insert, if table already has a booking, update it
        val result = db.insertWithOnConflict("TableBookings", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        db.close()
        return result != -1L
    }

    fun getTableBooking(tableId: String): TableBooking? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM TableBookings WHERE tableId = ?", arrayOf(tableId))
        
        var booking: TableBooking? = null
        if (cursor.moveToFirst()) {
            booking = TableBooking(
                id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                tableId = cursor.getString(cursor.getColumnIndexOrThrow("tableId")),
                customerName = cursor.getString(cursor.getColumnIndexOrThrow("customerName")),
                timeFrom = cursor.getString(cursor.getColumnIndexOrThrow("timeFrom")),
                timeTo = cursor.getString(cursor.getColumnIndexOrThrow("timeTo")),
                bookingDate = cursor.getString(cursor.getColumnIndexOrThrow("bookingDate"))
            )
        }
        cursor.close()
        db.close()
        return booking
    }

    fun deleteTableBooking(tableId: String): Boolean {
        val db = writableDatabase
        val result = db.delete("TableBookings", "tableId = ?", arrayOf(tableId))
        db.close()
        return result > 0
    }
}