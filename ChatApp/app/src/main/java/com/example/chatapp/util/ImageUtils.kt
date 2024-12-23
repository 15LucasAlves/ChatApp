package com.example.chatapp.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageUtils {
    fun createImageFile(context: Context): File {
        val fileName = "JPEG_${UUID.randomUUID()}"
        return File.createTempFile(fileName, ".jpg", context.cacheDir)
    }

    fun getImageUri(context: Context, file: File): Uri {
        return Uri.fromFile(file)
    }

    suspend fun compressImage(context: Context, uri: Uri): Uri {
        // Implement image compression logic here
        return uri
    }
} 