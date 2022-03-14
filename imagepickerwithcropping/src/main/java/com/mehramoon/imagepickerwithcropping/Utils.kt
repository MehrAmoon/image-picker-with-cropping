package com.mehramoon.imagepickerwithcropping

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


object Utils {

    fun getIntentUri(context: Context, uri: Uri): Uri {
        //support android N+
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            getContentUri(context, uri)
        } else {
            uri
        }
    }

    fun getContentUri(context: Context, fileUri: Uri): Uri {
        return FileProvider.getUriForFile(
            context,
            context.applicationContext.packageName + ".imagePicker.provider",
            File(fileUri.path)
        )
    }

    fun getRealPathFromURI(context: Context, contentUri: Uri?): String {
        var cursor: Cursor? = null
        var imageId: Long
        var realPath = ""
        val proj = arrayOf(MediaStore.Images.Media._ID)
        cursor = context.contentResolver.query(contentUri!!, proj, null, null, null)
        if (cursor != null) {
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (cursor.moveToNext()) {
                imageId = cursor.getLong(columnIndex)
                val uriImage = Uri.withAppendedPath(contentUri, "" + imageId)
                realPath = uriImage.toString()
            }
        }
        cursor?.close()
        return realPath
    }

    fun <R> CoroutineScope.executeAsyncTask(
        onPreExecute: () -> Unit,
        doInBackground: () -> R,
        onPostExecute: (R) -> Unit
    ) = launch {
        onPreExecute()
        val result = withContext(Dispatchers.IO) { // runs in background thread without blocking the Main Thread
            doInBackground()
        }
        onPostExecute(result)
    }
}