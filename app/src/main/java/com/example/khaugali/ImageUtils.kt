package com.example.khaugali

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.InputStream

object ImageUtils {
    
    fun uriToBase64(context: Context, uri: Uri): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            bitmapToBase64(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun bitmapToBase64(bitmap: Bitmap): String? {
        return try {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val byteArray = outputStream.toByteArray()
            outputStream.close()
            
            android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun getFilenameFromUri(uri: Uri): String {
        val path = uri.path ?: ""
        val filename = path.substringAfterLast("/")
        return if (filename.isNotEmpty() && filename.contains(".")) {
            filename
        } else {
            "image_${System.currentTimeMillis()}.jpg"
        }
    }
}
