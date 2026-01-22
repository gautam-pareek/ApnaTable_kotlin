package com.example.khaugali

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.gridlayout.widget.GridLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton

class CreateTableActivity : AppCompatActivity() {

    private lateinit var tableGrid: GridLayout
    private lateinit var addTableBtn: FloatingActionButton
    private lateinit var toolLayout: LinearLayout
    private var isToolLayoutVisible = false
    private lateinit var toolTable: ImageButton
    private lateinit var toolChair: ImageButton
    private lateinit var toolErase: ImageButton
    private lateinit var toolEdit: ImageButton
    private lateinit var arrowUp: ImageButton
    private lateinit var arrowDown: ImageButton
    private lateinit var arrowLeft: ImageButton
    private lateinit var arrowRight: ImageButton
    private lateinit var moveArrowLayout: View
    private lateinit var editActionsLayout: LinearLayout
    private var isMoveMode = false

    private val selectedCells = mutableListOf<TextView>()
    private var selectedTool: ToolType? = null
    private var tableCount = 0
    private var chairCount = 0

    private val totalCols = 9
    private val totalRows = 18

    private var currentUserId: Int = 0 // Set from login

    enum class ToolType { TABLE, CHAIR, ERASE, EDIT }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_table)

        // ---- Receive current user ID from login ----
        currentUserId = intent.getIntExtra("USER_ID", 0)
        if (currentUserId == 0) {
            val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
            currentUserId = sharedPref.getInt("loggedInUserId", 0)
        }

        if (currentUserId == 0) {
            Toast.makeText(this, "Error: User not found", Toast.LENGTH_SHORT).show()
            finish()
        }

        tableGrid = findViewById(R.id.tableGrid)
        addTableBtn = findViewById(R.id.addTableBtn)
        toolLayout = findViewById(R.id.toolLayout)
        toolTable = findViewById(R.id.toolTable)
        toolChair = findViewById(R.id.toolChair)
        toolErase = findViewById(R.id.toolDelete)
        toolEdit = findViewById(R.id.toolEdit)
        arrowUp = findViewById(R.id.arrowUp)
        arrowDown = findViewById(R.id.arrowDown)
        arrowLeft = findViewById(R.id.arrowLeft)
        arrowRight = findViewById(R.id.arrowRight)
        moveArrowLayout = findViewById(R.id.moveArrowLayout)
        editActionsLayout = findViewById(R.id.editActionsLayout)
        editActionsLayout.visibility = View.GONE

        val moveBtn = findViewById<ImageButton>(R.id.moveBtn)
        val rotateBtn = findViewById<ImageButton>(R.id.rotateBtn)
        val saveLayoutBtn = findViewById<FloatingActionButton>(R.id.saveLayoutBtn)

        saveLayoutBtn.setOnClickListener { 
            // Clear existing data first
            clearLayoutData()
            saveCurrentLayoutToServer()
        }

        // Long press to clear database
        saveLayoutBtn.setOnLongClickListener {
            clearLayoutData()
            Toast.makeText(this, "Database cleared! Try saving again.", Toast.LENGTH_LONG).show()
            true
        }

        // Arrow movement
        arrowUp.setOnClickListener { moveSelectedBy(0, -1) }
        arrowDown.setOnClickListener { moveSelectedBy(0, 1) }
        arrowLeft.setOnClickListener { moveSelectedBy(-1, 0) }
        arrowRight.setOnClickListener { moveSelectedBy(1, 0) }

        // Move button
        moveBtn.setOnClickListener {
            if (!isToolLayoutVisible) {
                Toast.makeText(this, "Open tools first (+)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedCells.isEmpty()) {
                Toast.makeText(this, "Select cells first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            isMoveMode = !isMoveMode
            moveArrowLayout.visibility = if (isMoveMode) View.VISIBLE else View.GONE
            moveBtn.setBackgroundColor(if (isMoveMode) Color.LTGRAY else Color.TRANSPARENT)
        }

        // Rotate button
        rotateBtn.setOnClickListener {
            if (!isToolLayoutVisible) {
                Toast.makeText(this, "Open tools first (+)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            rotateSelectedItems()
            saveCurrentLayout()
        }

        // Toggle tools (+)
        addTableBtn.setOnClickListener {
            isToolLayoutVisible = !isToolLayoutVisible
            toolLayout.visibility = if (isToolLayoutVisible) View.VISIBLE else View.GONE
            editActionsLayout.visibility = if (isToolLayoutVisible) View.VISIBLE else View.GONE
            if (!isToolLayoutVisible) {
                isMoveMode = false
                moveArrowLayout.visibility = View.GONE
                clearSelectedCells()
                selectedTool = null
                highlightSelectedTool()
            }
        }

        // Tool selection
        toolTable.setOnClickListener { selectTool(ToolType.TABLE) }
        toolChair.setOnClickListener { selectTool(ToolType.CHAIR) }
        toolErase.setOnClickListener { selectTool(ToolType.ERASE) }
        toolEdit.setOnClickListener { selectTool(ToolType.EDIT) }

        // Populate grid and load saved layout
        tableGrid.post {
            populateEmptyGrid()   // First create empty grid
            loadUserLayout()      // Then fill saved tables/chairs
        }
    }

    private fun selectTool(tool: ToolType) {
        if (!isToolLayoutVisible) return
        selectedTool = tool
        highlightSelectedTool()
    }

    // ---------------- Cell Handling ----------------
    private fun handleCellClick(cell: TextView) {
        if (!isToolLayoutVisible || selectedTool == null) return

        when (selectedTool!!) {
            ToolType.TABLE -> {
                addItemToCell(cell, "T", R.drawable.table_restaurant)
                saveCurrentLayout() // Save after adding table
            }
            ToolType.CHAIR -> {
                addItemToCell(cell, "C", R.drawable.chair)
                saveCurrentLayout() // Save after adding chair
            }
            ToolType.ERASE -> {
                eraseCell(cell)
                saveCurrentLayout() // Save after erasing
            }
            ToolType.EDIT -> toggleSelectCell(cell)
        }
    }

    private fun addItemToCell(cell: TextView, prefix: String, drawableId: Int) {
        if (cell.tag == null) {
            val id = if (prefix == "T") "T${++tableCount}" else "C${++chairCount}"
            cell.tag = id
            cell.text = ""
            cell.setBackgroundColor(Color.TRANSPARENT)
            cell.setCompoundDrawablesWithIntrinsicBounds(0, drawableId, 0, 0)
        }
    }

    private fun eraseCell(cell: TextView) {
        cell.text = ""
        cell.tag = null
        cell.setBackgroundColor(Color.WHITE)
        cell.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
        selectedCells.remove(cell)
    }

    private fun toggleSelectCell(cell: TextView) {
        if (cell.tag != null) {
            if (selectedCells.contains(cell)) {
                selectedCells.remove(cell)
                cell.setBackgroundColor(Color.TRANSPARENT)
            } else {
                selectedCells.add(cell)
                cell.setBackgroundColor(Color.parseColor("#FFDD57"))
            }
        }
    }

    // ---------------- Move / Rotate ----------------
    private fun moveSelectedBy(dx: Int, dy: Int) {
        if (selectedCells.isEmpty()) return
        val positions = selectedCells.map { tableGrid.indexOfChild(it) }
        val rowColList = positions.map { it / totalCols to it % totalCols }
        val newPositions = mutableListOf<Pair<Int, Int>>()

        // Check bounds & collisions
        for ((row, col) in rowColList) {
            val newRow = row + dy
            val newCol = col + dx
            if (newRow !in 0 until totalRows || newCol !in 0 until totalCols) {
                Toast.makeText(this, "Move out of bounds", Toast.LENGTH_SHORT).show()
                return
            }
            val newIndex = newRow * totalCols + newCol
            val newCell = tableGrid.getChildAt(newIndex) as TextView
            if (newCell.tag != null && !selectedCells.contains(newCell)) {
                Toast.makeText(this, "Cannot move: overlaps another item", Toast.LENGTH_SHORT)
                    .show()
                return
            }
            newPositions.add(newRow to newCol)
        }

        val originalData = selectedCells.map { Pair(it.tag, it) }

        // Clear old cells
        for (cell in selectedCells) eraseCell(cell)

        // Apply to new positions
        selectedCells.clear()
        for (i in newPositions.indices) {
            val (newRow, newCol) = newPositions[i]
            val index = newRow * totalCols + newCol
            val cell = tableGrid.getChildAt(index) as TextView
            val (tag, _) = originalData[i]
            if (tag.toString().startsWith("T")) addItemToCell(
                cell,
                "T",
                R.drawable.table_restaurant
            )
            else addItemToCell(cell, "C", R.drawable.chair)
            cell.tag = tag
            selectedCells.add(cell)
        }
    }

    private fun rotateSelectedItems() {
        if (selectedCells.size <= 1) return
    }

    // ---------------- Highlight & Clear ----------------
    private fun highlightSelectedTool() {
        val selectedColor = Color.parseColor("#FFD580")
        val normalColor = Color.parseColor("#E9A557")
        editActionsLayout.visibility =
            if (selectedTool == ToolType.EDIT) View.VISIBLE else View.GONE

        toolTable.setBackgroundColor(if (selectedTool == ToolType.TABLE) selectedColor else normalColor)
        toolChair.setBackgroundColor(if (selectedTool == ToolType.CHAIR) selectedColor else normalColor)
        toolErase.setBackgroundColor(if (selectedTool == ToolType.ERASE) selectedColor else normalColor)
        toolEdit.setBackgroundColor(if (selectedTool == ToolType.EDIT) selectedColor else normalColor)

        if (selectedTool != ToolType.EDIT) clearSelectedCells()
    }

    private fun clearSelectedCells() {
        for (cell in selectedCells) cell.setBackgroundColor(Color.TRANSPARENT)
        selectedCells.clear()
    }

    // ---------------- Grid ----------------
    private fun populateEmptyGrid() {
        tableGrid.removeAllViews()
        tableGrid.columnCount = totalCols
        tableGrid.rowCount = totalRows
        val displayMetrics = resources.displayMetrics
        val cellWidth = displayMetrics.widthPixels / totalCols
        val cellHeight = displayMetrics.heightPixels / totalRows

        for (row in 0 until totalRows) {
            for (col in 0 until totalCols) {
                val params = GridLayout.LayoutParams(
                    GridLayout.spec(row, GridLayout.FILL),
                    GridLayout.spec(col, GridLayout.FILL)
                ).apply { width = cellWidth; height = cellHeight; setMargins(1, 1, 1, 1) }

                val cell = TextView(this).apply {
                    layoutParams = params
                    setBackgroundColor(Color.WHITE)
                    gravity = Gravity.CENTER
                    tag = null
                    setOnClickListener { handleCellClick(this) }
                }
                tableGrid.addView(cell)
            }
        }
    }

    // ---------------- Server API ----------------
    private fun saveCurrentLayoutToServer() {
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val currentUserId = sharedPref.getInt("loggedInUserId", 0)
        if (currentUserId == 0) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show()
            return
        }

        // Collect layout items from grid
        val layoutItems = mutableListOf<LayoutItem>()
        for (i in 0 until tableGrid.childCount) {
            val cell = tableGrid.getChildAt(i) as TextView
            val tag = cell.tag as? String ?: continue
            val row = i / totalCols
            val col = i % totalCols

            val type = if (tag.startsWith("T")) "TABLE" else "CHAIR"
            layoutItems.add(LayoutItem(row, col, type, tag))
        }

        if (layoutItems.isEmpty()) {
            Toast.makeText(this, "Nothing to save!", Toast.LENGTH_SHORT).show()
            return
        }

        // Save each item to server
        var savedCount = 0
        val totalItems = layoutItems.size

        for (item in layoutItems) {
            val layoutRequest = LayoutRequest(
                userId = currentUserId,
                gridRow = item.row,
                gridCol = item.col,
                type = item.type,
                label = item.label
            )

            RetrofitClient.instance.addLayout(layoutRequest).enqueue(object : retrofit2.Callback<RegisterResponse> {
                override fun onResponse(call: retrofit2.Call<RegisterResponse>, response: retrofit2.Response<RegisterResponse>) {
                    savedCount++
                    if (savedCount == totalItems) {
                        Toast.makeText(this@CreateTableActivity, "Layout saved to server!", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: retrofit2.Call<RegisterResponse>, t: Throwable) {
                    savedCount++
                    if (savedCount == totalItems) {
                        Toast.makeText(this@CreateTableActivity, "Some items failed to save", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }
    }

    private fun clearLayoutData() {
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val currentUserId = sharedPref.getInt("loggedInUserId", 0)
        if (currentUserId == 0) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show()
            return
        }

        // Clear local database as fallback
        val dbHelper = UserDatabaseHelper(this)
        dbHelper.deleteLayouts(currentUserId)
        println("DEBUG: Cleared all layout data for user $currentUserId")
    }

    private fun saveCurrentLayout() {
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val currentUserId = sharedPref.getInt("loggedInUserId", 0)
        if (currentUserId == 0) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show()
            return
        }

        val dbHelper = UserDatabaseHelper(this)

        var hasItems = false
        for (i in 0 until tableGrid.childCount) {
            val cell = tableGrid.getChildAt(i) as TextView
            if (cell.tag != null) {
                hasItems = true
                break
            }
        }

        if (!hasItems) {
            Toast.makeText(this, "Nothing to save!", Toast.LENGTH_SHORT).show()
            return
        }  // Do not save empty grid

        // Clear existing layouts first to avoid conflicts
        dbHelper.deleteLayouts(currentUserId)

        for (i in 0 until tableGrid.childCount) {
            val cell = tableGrid.getChildAt(i) as TextView
            val tag = cell.tag as? String ?: continue
            val row = i / totalCols
            val col = i % totalCols

            val type = if (tag.startsWith("T")) "TABLE" else "CHAIR"
            
            println("DEBUG: Saving item - Row: $row, Col: $col, Type: $type, Label: $tag")
            dbHelper.insertLayoutItem(currentUserId, row, col, type, tag)
        }
        
        // Also save to server
        saveCurrentLayoutToServer()
        
        Toast.makeText(this, "Layout saved!", Toast.LENGTH_SHORT).show()
    }


    private fun loadUserLayout() {
        // Always populate empty grid first
        populateEmptyGrid()

        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val currentUserId = sharedPref.getInt("loggedInUserId", 0)
        if (currentUserId == 0) {
            println("DEBUG: No user ID found in shared preferences")
            return
        }

        // First try to load from server
        loadLayoutFromServer(currentUserId)
        
        // Also load from local database as fallback
        loadLayoutFromLocalDB(currentUserId)
    }
    
    private fun loadLayoutFromServer(userId: Int) {
        RetrofitClient.instance.getLayouts(userId).enqueue(object : retrofit2.Callback<List<LayoutResponse>> {
            override fun onResponse(call: retrofit2.Call<List<LayoutResponse>>, response: retrofit2.Response<List<LayoutResponse>>) {
                if (response.isSuccessful) {
                    val layoutItems = response.body()
                    if (layoutItems != null && layoutItems.isNotEmpty()) {
                        println("DEBUG: Loaded ${layoutItems.size} layout items from server")
                        
                        // Clear grid before applying server layouts
                        populateEmptyGrid()
                        
                        // Apply layouts from server
                        for (item in layoutItems) {
                            val index = item.gridRow * totalCols + item.gridCol
                            if (index < 0 || index >= tableGrid.childCount) {
                                println("DEBUG: Index $index out of bounds (max: ${tableGrid.childCount})")
                                continue
                            }

                            val cell = tableGrid.getChildAt(index) as TextView
                            cell.tag = item.label
                            cell.text = ""

                            when (item.type.uppercase()) {
                                "TABLE" -> {
                                    cell.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.table_restaurant, 0, 0)
                                }
                                "CHAIR" -> {
                                    cell.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.chair, 0, 0)
                                }
                                else -> {
                                    cell.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                                }
                            }
                        }
                        
                        // Sync server layouts to local database
                        syncServerLayoutsToLocal(userId, layoutItems)
                        
                        Toast.makeText(this@CreateTableActivity, "Layout loaded from server", Toast.LENGTH_SHORT).show()
                    } else {
                        println("DEBUG: No layout items found on server")
                        // If no server layouts, try local database
                        loadLayoutFromLocalDB(userId)
                    }
                } else {
                    println("DEBUG: Failed to load layouts from server: ${response.code()}")
                    // Fallback to local database
                    loadLayoutFromLocalDB(userId)
                }
            }

            override fun onFailure(call: retrofit2.Call<List<LayoutResponse>>, t: Throwable) {
                println("DEBUG: Error loading layouts from server: ${t.message}")
                // Fallback to local database
                loadLayoutFromLocalDB(userId)
            }
        })
    }
    
    private fun syncServerLayoutsToLocal(userId: Int, serverLayouts: List<LayoutResponse>) {
        val dbHelper = UserDatabaseHelper(this)
        
        // Clear existing local layouts first
        dbHelper.deleteLayouts(userId)
        
        // Insert server layouts to local database
        for (item in serverLayouts) {
            dbHelper.insertLayoutItem(userId, item.gridRow, item.gridCol, item.type, item.label)
        }
        
        println("DEBUG: Synced ${serverLayouts.size} server layouts to local database")
    }
    
    private fun loadLayoutFromLocalDB(userId: Int) {
        val dbHelper = UserDatabaseHelper(this)
        val layoutItems = dbHelper.getLayoutItems(userId)

        // Debugging: log number of items
        println("DEBUG: Loaded layout items count from local DB: ${layoutItems.size}")

        if (layoutItems.isEmpty()) {
            return  // No items to load
        }
        
        for (item in layoutItems) {
            println("DEBUG: Processing item - Row: ${item.row}, Col: ${item.col}, Type: ${item.type}, Label: ${item.label}")
            
            // Skip empty labels just in case
            if (item.label.isNullOrBlank()) {
                println("DEBUG: Skipping item with blank label")
                continue
            }

            val index = item.row * totalCols + item.col
            if (index < 0 || index >= tableGrid.childCount) {
                println("DEBUG: Index $index out of bounds (max: ${tableGrid.childCount})")
                continue
            }

            val cell = tableGrid.getChildAt(index) as TextView
            cell.tag = item.label
            cell.text = ""

            when (item.type.uppercase()) {   // Ensure case-insensitive match
                "TABLE" -> {
                    println("DEBUG: Setting table icon for cell at index $index")
                    cell.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.table_restaurant, 0, 0)
                }
                "CHAIR" -> {
                    println("DEBUG: Setting chair icon for cell at index $index")
                    cell.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.chair, 0, 0)
                }
                else -> {
                    println("DEBUG: Unknown type '${item.type}' for cell at index $index")
                    cell.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                }
            }
        }
        println("DEBUG: Finished loading layout from local DB")
    }
}

