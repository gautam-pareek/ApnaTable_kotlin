# Khaugali Server

A Flask-based REST API server for the Khaugali restaurant management app.

## Setup

1. **Install Python dependencies:**
   ```bash
   pip install -r requirements.txt
   ```

2. **Initialize the database:**
   ```bash
   python init_db.py
   ```

3. **Start the server:**
   ```bash
   python server.py
   ```

The server will run on `http://192.168.74.174:5000`

## API Endpoints

### Authentication
- `POST /register` - User registration
- `POST /login` - User login

### Menu Management
- `GET /menu/<user_id>` - Get user's menu items
- `POST /menu` - Add new menu item
- `PUT /menu/<menu_id>` - Update menu item
- `DELETE /menu/<menu_id>` - Delete menu item

### Search
- `GET /search?q=query` - Search menu items

### Layout Management
- `GET /layouts/<user_id>` - Get user's table layouts
- `POST /layouts` - Add new layout
- `DELETE /layouts/<layout_id>` - Delete layout

### Health Check
- `GET /` - Server health check

## Database Schema

- **Users**: User accounts (customers and businesses)
- **Menu**: Food items for each business
- **Layouts**: Table layouts for restaurants
- **Orders**: Customer orders (future feature)

## Testing

Test the server with curl or your Android app:
```bash
curl http://192.168.74.174:5000/
```
