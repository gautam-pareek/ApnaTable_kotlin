package com.example.khaugali

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.gridlayout.widget.GridLayout
import android.app.Dialog
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.EditText
import android.widget.Button

// Data class for table bookings
data class TableBooking(
    val id: Int = 0,
    val tableId: String,
    val customerName: String,
    val timeFrom: String,
    val timeTo: String,
    val bookingDate: String = System.currentTimeMillis().toString()
)

class TableStatusActivity : AppCompatActivity() {

    private lateinit var statusGrid: GridLayout
    private val totalCols = 9
    private val totalRows = 18
    private lateinit var userType: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_table_status)

        statusGrid = findViewById(R.id.statusGrid)
        statusGrid.columnCount = totalCols
        statusGrid.rowCount = totalRows

        // 1. Populate empty grid first
        populateEmptyGrid()

        // 2. Load saved items - try server first, then local database
        val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val currentUserId = prefs.getInt("loggedInUserId", -1)
        userType = prefs.getString("userType", "customer") ?: "customer" // Get user type
        
        if (currentUserId == -1) {
            finish()
            return
        }

        loadLayoutItems(currentUserId)
    }

    // DFS to collect connected tables
    private fun collectTables(
        pos: Pair<Int, Int>,
        gridMap: Map<Pair<Int, Int>, LayoutItem>,
        visited: MutableSet<Pair<Int, Int>>,
        block: MutableList<LayoutItem>
    ) {
        if (pos !in gridMap) return
        val item = gridMap[pos] ?: return
        if (!item.type.startsWith("T", true)) return
        if (pos in visited) return

        visited.add(pos)
        block.add(item)

        val (r, c) = pos
        val neighbors = listOf(
            r - 1 to c,
            r + 1 to c,
            r to c - 1,
            r to c + 1
        )
        neighbors.forEach { collectTables(it, gridMap, visited, block) }
    }

    private fun loadLayoutItems(currentUserId: Int) {
        // Try loading from server first
        RetrofitClient.instance.getLayouts(currentUserId).enqueue(object : retrofit2.Callback<List<LayoutResponse>> {
            override fun onResponse(call: retrofit2.Call<List<LayoutResponse>>, response: retrofit2.Response<List<LayoutResponse>>) {
                if (response.isSuccessful && response.body() != null) {
                    val layoutResponses = response.body()!!
                    if (layoutResponses.isNotEmpty()) {
                        // Convert LayoutResponse objects to LayoutItem objects
                        val layoutItems = layoutResponses.map { response ->
                            LayoutItem(response.gridRow, response.gridCol, response.type, response.label)
                        }
                        Toast.makeText(this@TableStatusActivity, "Loaded ${layoutItems.size} items from server", Toast.LENGTH_SHORT).show()
                        processLayoutItems(layoutItems)
                        return
                    }
                }
                // If server fails or returns empty, try local database
                loadFromLocalDatabase(currentUserId)
            }

            override fun onFailure(call: retrofit2.Call<List<LayoutResponse>>, t: Throwable) {
                // If server fails, try local database
                loadFromLocalDatabase(currentUserId)
            }
        })
    }

    private fun loadFromLocalDatabase(currentUserId: Int) {
        val dbHelper = UserDatabaseHelper(this)
        val items = dbHelper.getLayoutItems(currentUserId)
        Toast.makeText(this, "Loaded ${items.size} items from local database", Toast.LENGTH_SHORT).show()
        if (items.isNotEmpty()) {
            processLayoutItems(items)
        }
    }

    private fun processLayoutItems(items: List<LayoutItem>) {
        // Build map row/col -> item
        val gridMap = mutableMapOf<Pair<Int, Int>, LayoutItem>()
        items.forEach { gridMap[it.row to it.col] = it }

        val visitedTables = mutableSetOf<Pair<Int, Int>>()

        for ((pos, item) in gridMap) {
            // Only start with unvisited tables
            if (!item.type.startsWith("T", true) || visitedTables.contains(pos)) continue

            // Collect connected tables
            val tableBlock = mutableListOf<LayoutItem>()
            collectTables(pos, gridMap, visitedTables, tableBlock)

            // Collect adjacent chairs
            val chairBlock = mutableListOf<LayoutItem>()
            for (table in tableBlock) {
                val neighbors = listOf(
                    table.row - 1 to table.col,
                    table.row + 1 to table.col,
                    table.row to table.col - 1,
                    table.row to table.col + 1
                )
                neighbors.forEach { n ->
                    val neighborItem = gridMap[n]
                    if (neighborItem != null &&
                        neighborItem.type.startsWith("C", true) &&
                        !chairBlock.contains(neighborItem)
                    ) {
                        chairBlock.add(neighborItem)
                    }
                }
            }

            val fullBlock = tableBlock + chairBlock
            if (fullBlock.isEmpty()) continue

            // Assign each cell in the block its icon & click listener
            for (cellItem in fullBlock) {
                val index = cellItem.row * totalCols + cellItem.col
                if (index < 0 || index >= statusGrid.childCount) continue
                val cell = statusGrid.getChildAt(index) as TextView

                val drawableRes = if (cellItem.type.startsWith("T", true)) R.drawable.table_restaurant else R.drawable.chair
                cell.setCompoundDrawablesWithIntrinsicBounds(0, drawableRes, 0, 0)
                cell.setBackgroundColor(Color.TRANSPARENT)

                // Same click listener for entire block
                cell.setOnClickListener {
                    // Show different dialog based on user type
                    if (userType == "customer") {
                        showCustomerBookingDialog(fullBlock)
                    } else {
                        showBusinessTableInfoDialog(fullBlock)
                    }
                }
            }
        }
    }

    private fun populateEmptyGrid() {
        statusGrid.removeAllViews()
        statusGrid.columnCount = totalCols
        statusGrid.rowCount = totalRows

        val displayMetrics = resources.displayMetrics
        val cellWidth = displayMetrics.widthPixels / totalCols
        val cellHeight = displayMetrics.heightPixels / totalRows

        for (row in 0 until totalRows) {
            for (col in 0 until totalCols) {
                val params = GridLayout.LayoutParams(
                    GridLayout.spec(row, GridLayout.FILL),
                    GridLayout.spec(col, GridLayout.FILL)
                ).apply {
                    width = cellWidth
                    height = cellHeight
                    setMargins(2, 2, 2, 2)
                }

                val cell = TextView(this).apply {
                    layoutParams = params
                    setBackgroundColor(Color.WHITE)
                    gravity = Gravity.CENTER
                    tag = null
                }
                statusGrid.addView(cell)
            }
        }
    }
    // ---------------- Popup Dialogs ----------------

    private fun showCustomerBookingDialog(fullBlock: List<LayoutItem>) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_customer_booking)
        dialog.setCancelable(true)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        val etCustomerName = dialog.findViewById<EditText>(R.id.etCustomerName)
        val etTimeFrom = dialog.findViewById<EditText>(R.id.etTimeFrom)
        val etTimeTo = dialog.findViewById<EditText>(R.id.etTimeTo)
        val btnBookTable = dialog.findViewById<Button>(R.id.btnBookTable)
        val btnClose = dialog.findViewById<ImageButton>(R.id.btnClose)

        btnClose.setOnClickListener { dialog.dismiss() }

        btnBookTable.setOnClickListener {
            val customerName = etCustomerName.text.toString().trim()
            val timeFrom = etTimeFrom.text.toString().trim()
            val timeTo = etTimeTo.text.toString().trim()

            if (customerName.isEmpty() || timeFrom.isEmpty() || timeTo.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Save booking to database
            saveTableBooking(fullBlock, customerName, timeFrom, timeTo)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showBusinessTableInfoDialog(fullBlock: List<LayoutItem>) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_table_info)
        dialog.setCancelable(true)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        val tvCustomerName = dialog.findViewById<TextView>(R.id.tvCustomerName)
        val tvTiming = dialog.findViewById<TextView>(R.id.tvTiming)
        val btnClose = dialog.findViewById<ImageButton>(R.id.btnClose)

        btnClose.setOnClickListener { dialog.dismiss() }

        // Check if table has a booking
        val booking = getTableBooking(fullBlock)
        if (booking != null) {
            tvCustomerName.text = "Customer:   "
            tvTiming.text = "Timing: ${booking.timeFrom} - ${booking.timeTo}"
        } else {
            tvCustomerName.text = "Customer: None (Table Empty)"
            tvTiming.text = "Timing: Not booked"
        }

        dialog.show()
    }

    // ---------------- Booking Management ----------------

    private fun saveTableBooking(fullBlock: List<LayoutItem>, customerName: String, timeFrom: String, timeTo: String) {
        // Generate table ID from the first table item in the block
        val tableId = fullBlock.firstOrNull { it.type.startsWith("T", true) }?.type ?: "T1"
        
        val booking = TableBooking(
            tableId = tableId,
            customerName = customerName,
            timeFrom = timeFrom,
            timeTo = timeTo
        )

        // Save to local database
        val dbHelper = UserDatabaseHelper(this)
        val success = dbHelper.saveTableBooking(booking)

        if (success) {
            Toast.makeText(this, "Table booked successfully!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to book table", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getTableBooking(fullBlock: List<LayoutItem>): TableBooking? {
        // Generate table ID from the first table item in the block
        val tableId = fullBlock.firstOrNull { it.type.startsWith("T", true) }?.type ?: return null
        
        // Get from local database
        val dbHelper = UserDatabaseHelper(this)
        return dbHelper.getTableBooking(tableId)
    }
}
