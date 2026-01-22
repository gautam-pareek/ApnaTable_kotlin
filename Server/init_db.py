import sqlite3
import os

DB_NAME = "restaurant.db"

def init_db():
    """Initialize the SQLite database with required tables"""
    conn = sqlite3.connect(DB_NAME)
    cursor = conn.cursor()

    # USERS TABLE
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS Users (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT NOT NULL,
        email TEXT UNIQUE NOT NULL,
        password TEXT NOT NULL,
        imageUri TEXT DEFAULT '',
        userType TEXT NOT NULL,
        createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )
    """)

    # MENU TABLE
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS Menu (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        userId INTEGER NOT NULL,
        name TEXT NOT NULL,
        category TEXT NOT NULL,
        price REAL NOT NULL,
        rating REAL DEFAULT 0.0,
        prepTime TEXT DEFAULT '',
        ingredients TEXT DEFAULT '',
        images TEXT DEFAULT '',
        createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY(userId) REFERENCES Users(id) ON DELETE CASCADE
    )
    """)

    # LAYOUTS TABLE (for restaurant table layouts)
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS Layouts (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        userId INTEGER NOT NULL,
        gridRow INTEGER NOT NULL,
        gridCol INTEGER NOT NULL,
        type TEXT NOT NULL,
        label TEXT DEFAULT '',
        status TEXT DEFAULT 'available',
        createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY(userId) REFERENCES Users(id) ON DELETE CASCADE
    )
    """)

    # ORDERS TABLE (for customer orders)
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS Orders (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        customerId INTEGER NOT NULL,
        businessId INTEGER NOT NULL,
        tableId INTEGER DEFAULT NULL,
        items TEXT NOT NULL,  -- JSON string of ordered items
        totalAmount REAL NOT NULL,
        status TEXT DEFAULT 'pending',  -- pending, confirmed, preparing, ready, delivered
        orderType TEXT DEFAULT 'dine_in',  -- dine_in, takeaway, delivery
        createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY(customerId) REFERENCES Users(id) ON DELETE CASCADE,
        FOREIGN KEY(businessId) REFERENCES Users(id) ON DELETE CASCADE
    )
    """)

    # Create indexes for better performance
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_users_email ON Users(email)")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_menu_userId ON Menu(userId)")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_layouts_userId ON Layouts(userId)")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_orders_customerId ON Orders(customerId)")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_orders_businessId ON Orders(businessId)")

    conn.commit()
    conn.close()
    print("✅ Database initialized successfully!")
    print("📊 Created tables: Users, Menu, Layouts, Orders")
    print("🔍 Created indexes for better performance")

def reset_db():
    """Reset the database (delete all data)"""
    if os.path.exists(DB_NAME):
        os.remove(DB_NAME)
        print("🗑️  Old database removed")
    init_db()
    print("🔄 Database reset complete")

if __name__ == "__main__":
    import sys
    
    if len(sys.argv) > 1 and sys.argv[1] == "reset":
        reset_db()
    else:
        init_db()
