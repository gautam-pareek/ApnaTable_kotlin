import sqlite3

# Connect to the database
conn = sqlite3.connect('restaurant.db')
cursor = conn.cursor()

# Check if user already exists
cursor.execute("SELECT * FROM Users WHERE email = ? OR name = ?", ('test@business.com', 'TestBusiness'))
if cursor.fetchone():
    print("Test user already exists")
else:
    # Add a test business user
    cursor.execute("""
    INSERT INTO Users (name, email, password, imageUri, userType) 
    VALUES (?, ?, ?, ?, ?)
    """, ('TestBusiness', 'test@business.com', 'password123', '', 'business'))
    conn.commit()
    print("Test business user added successfully")

# Show all users
cursor.execute("SELECT * FROM Users")
rows = cursor.fetchall()
print("\nUsers in database:")
for row in rows:
    print(row)

conn.close()