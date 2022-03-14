package com.mehramoon.imagepickerwithcropping.crop

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.graphics.*
import android.graphics.Bitmap.CompressFormat
import android.media.ExifInterface
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.util.Pair
import java.io.*
import java.lang.ref.WeakReference
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin


object BitmapUtils {
    val EMPTY_RECT = Rect()
    val EMPTY_RECT_F = RectF()
    val RECT = RectF()
    val POINTS = FloatArray(6)
    val POINTS2 = FloatArray(6)
    private var mMaxTextureSize = 0
    var mStateBitmap: Pair<String, WeakReference<Bitmap>>? = null


    fun rotateBitmapByExifWithUri(bitmap: Bitmap?, context: Context, uri: Uri): RotateBitmapResult {
        try {
            val file = getFileFromUri(context, uri)
            if (file.exists()) {
                val ei = ExifInterface(file.absolutePath)
                return rotateBitmapByExif(bitmap, ei)
            }
        } catch (ignored: Exception) {
        }
        return RotateBitmapResult(bitmap, 0)
    }

    fun rotateBitmapByExif(bitmap: Bitmap?, exif: ExifInterface): RotateBitmapResult {
        val degrees: Int
        val orientation =
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
        return RotateBitmapResult(bitmap, degrees)
    }


    fun decodeSampledBitmap(
        context: Context,
        uri: Uri,
        reqWidth: Int,
        reqHeight: Int
    ): BitmapSampled {
        return try {
            val resolver = context.contentResolver
            val options = decodeImageForOption(resolver, uri)
            options.inSampleSize = calculateInSampleSizeByRequestedSize(
                options.outWidth,
                options.outHeight,
                reqWidth,
                reqHeight
            ).coerceAtLeast(
                calculateInSampleSizeByMaxTextureSize(
                    options.outWidth,
                    options.outHeight
                )
            )

            val bitmap = decodeImage(resolver, uri, options)
            BitmapSampled(bitmap, options.inSampleSize)
        } catch (e: Exception) {
            throw RuntimeException( """ Failed to load sampled bitmap: $uri ${e.message} """.trimIndent(), e)
        }
    }


    fun cropBitmapObjectHandleOOM(
        bitmap: Bitmap, points: FloatArray, degreesRotated: Int,
        fixAspectRatio: Boolean, aspectRatioX: Int, aspectRatioY: Int
    ): BitmapSampled {
        var scale = 1
        while (true) {
            try {
                val cropBitmap = cropBitmapObjectWithScale(
                    bitmap,
                    points,
                    degreesRotated,
                    fixAspectRatio,
                    aspectRatioX,
                    aspectRatioY,
                    1 / scale.toFloat()
                )
                return BitmapSampled(cropBitmap, scale)
            } catch (e: OutOfMemoryError) {
                scale *= 2
                if (scale > 8) {
                    throw e
                }
            }
        }
    }

    private fun cropBitmapObjectWithScale(
        bitmap: Bitmap, points: FloatArray, degreesRotated: Int,
        fixAspectRatio: Boolean, aspectRatioX: Int, aspectRatioY: Int, scale: Float
    ): Bitmap? {

        val rect = getRectFromPoints(
            points,
            bitmap.width,
            bitmap.height,
            fixAspectRatio,
            aspectRatioX,
            aspectRatioY
        )

        val matrix = Matrix()
        matrix.setScale(scale, scale)
        matrix.postRotate(
            degreesRotated.toFloat(),
            (bitmap.width / 2).toFloat(),
            (bitmap.height / 2).toFloat()
        )
        var result = Bitmap.createBitmap(
            bitmap,
            rect.left,
            rect.top,
            rect.width(),
            rect.height(),
            matrix,
            true
        )
        if (result == bitmap) {
            result = bitmap.copy(bitmap.config, false)
        }

        if (degreesRotated % 90 != 0) {
            result = cropForRotatedImage(
                result,
                points,
                rect,
                degreesRotated,
                fixAspectRatio,
                aspectRatioX,
                aspectRatioY
            )
        }
        return result
    }


    fun cropBitmap(
        context: Context, loadedImageUri: Uri, points: FloatArray,
        degreesRotated: Int, orgWidth: Int, orgHeight: Int, fixAspectRatio: Boolean,
        aspectRatioX: Int, aspectRatioY: Int, reqWidth: Int, reqHeight: Int
    ): BitmapSampled {
        var sampleMulti = 1
        while (true) {
            try {
                return cropBitmap(
                    context, loadedImageUri, points,
                    degreesRotated, orgWidth, orgHeight, fixAspectRatio,
                    aspectRatioX, aspectRatioY, reqWidth, reqHeight,
                    sampleMulti
                )
            } catch (e: OutOfMemoryError) {
                sampleMulti *= 2
                if (sampleMulti > 16) {
                    throw RuntimeException(  """ Failed to handle OOM by sampling ($sampleMulti): $loadedImageUri  ${e.message}   """.trimIndent(), e
                    )
                }
            }
        }
    }


    fun getRectLeft(points: FloatArray): Float {
        return points[0].coerceAtMost(points[2]).coerceAtMost(points[4]).coerceAtMost(points[6])
    }


    fun getRectTop(points: FloatArray): Float {
        return points[1].coerceAtMost(points[3]).coerceAtMost(points[5]).coerceAtMost(points[7])
    }


    fun getRectRight(points: FloatArray): Float {
        return points[0].coerceAtLeast(points[2]).coerceAtLeast(points[4]).coerceAtLeast(points[6])
    }


    fun getRectBottom(points: FloatArray): Float {
        return points[1].coerceAtLeast(points[3]).coerceAtLeast(points[5]).coerceAtLeast(points[7])
    }

    /**
     * Get width of the bounding rectangle of the given points.
     */
    fun getRectWidth(points: FloatArray): Float {
        return getRectRight(points) - getRectLeft(points)
    }

    /**
     * Get height of the bounding rectangle of the given points.
     */
    fun getRectHeight(points: FloatArray): Float {
        return getRectBottom(points) - getRectTop(points)
    }

    /**
     * Get horizontal center value of the bounding rectangle of the given points.
     */
    fun getRectCenterX(points: FloatArray): Float {
        return (getRectRight(points) + getRectLeft(points)) / 2f
    }

    /**
     * Get vertical center value of the bounding rectangle of the given points.
     */
    fun getRectCenterY(points: FloatArray): Float {
        return (getRectBottom(points) + getRectTop(points)) / 2f
    }

    /**
     * Get a rectangle for the given 4 points (x0,y0,x1,y1,x2,y2,x3,y3) by finding the min/max 2 points that
     * contains the given 4 points and is a straight rectangle.
     */
    fun getRectFromPoints(
        points: FloatArray,
        imageWidth: Int,
        imageHeight: Int,
        fixAspectRatio: Boolean,
        aspectRatioX: Int,
        aspectRatioY: Int
    ): Rect {
        val left = 0f.coerceAtLeast(getRectLeft(points)).roundToInt()
        val top = 0f.coerceAtLeast(getRectTop(points)).roundToInt()
        val right = imageWidth.toFloat().coerceAtMost(getRectRight(points)).roundToInt()
        val bottom = imageHeight.toFloat().coerceAtMost(getRectBottom(points)).roundToInt()
        val rect = Rect(left, top, right, bottom)
        if (fixAspectRatio) {
            fixRectForAspectRatio(rect, aspectRatioX, aspectRatioY)
        }
        return rect
    }

    /**
     * Fix the given rectangle if it doesn't confirm to aspect ration rule.<br></br>
     * Make sure that width and height are equal if 1:1 fixed aspect ratio is requested.
     */
    private fun fixRectForAspectRatio(rect: Rect, aspectRatioX: Int, aspectRatioY: Int) {
        if (aspectRatioX == aspectRatioY && rect.width() != rect.height()) {
            if (rect.height() > rect.width()) {
                rect.bottom -= rect.height() - rect.width()
            } else {
                rect.right -= rect.width() - rect.height()
            }
        }
    }


    @Throws(FileNotFoundException::class)
    fun writeBitmapToUri(
        context: Context,
        bitmap: Bitmap,
        uri: Uri?,
        compressFormat: CompressFormat?,
        compressQuality: Int
    ) {
        var outputStream: OutputStream? = null
        try {
            outputStream = context.contentResolver.openOutputStream(uri!!)
            bitmap.compress(compressFormat, compressQuality, outputStream)
        } finally {
            closeSafe(outputStream)
        }
    }


    fun resizeBitmap(
        bitmap: Bitmap,
        reqWidth: Int,
        reqHeight: Int,
        options: CropImageView.RequestSizeOptions
    ): Bitmap {
        try {
            if (reqWidth > 0 && reqHeight > 0 && (options == CropImageView.RequestSizeOptions.RESIZE_FIT || options == CropImageView.RequestSizeOptions.RESIZE_INSIDE || options == CropImageView.RequestSizeOptions.RESIZE_EXACT)) {
                var resized: Bitmap? = null
                if (options == CropImageView.RequestSizeOptions.RESIZE_EXACT) {
                    resized = Bitmap.createScaledBitmap(bitmap, reqWidth, reqHeight, false)
                } else {
                    val width = bitmap.width
                    val height = bitmap.height
                    val scale = (width / reqWidth.toFloat()).coerceAtLeast(height / reqHeight.toFloat())
                    if (scale > 1 || options == CropImageView.RequestSizeOptions.RESIZE_FIT) {
                        resized = Bitmap.createScaledBitmap(
                            bitmap,
                            (width / scale).toInt(), (height / scale).toInt(), false
                        )
                    }
                }
                if (resized != null) {
                    if (resized != bitmap) {
                        bitmap.recycle()
                    }
                    return resized
                }
            }
        } catch (e: Exception) {
            Log.w("AIC", "Failed to resize cropped image, return bitmap before resize", e)
        }
        return bitmap
    }


    private fun cropBitmap(
        context: Context, loadedImageUri: Uri, points: FloatArray,
        degreesRotated: Int, orgWidth: Int, orgHeight: Int, fixAspectRatio: Boolean,
        aspectRatioX: Int, aspectRatioY: Int, reqWidth: Int, reqHeight: Int, sampleMulti: Int
    ): BitmapSampled {

        val rect = getRectFromPoints(
            points,
            orgWidth,
            orgHeight,
            fixAspectRatio,
            aspectRatioX,
            aspectRatioY
        )
        val width = if (reqWidth > 0) reqWidth else rect.width()
        val height = if (reqHeight > 0) reqHeight else rect.height()
        var result: Bitmap? = null
        var sampleSize = 1
        try {
            val bitmapSampled =
                decodeSampledBitmapRegion(context, loadedImageUri, rect, width, height, sampleMulti)
            result = bitmapSampled.bitmap
            sampleSize = bitmapSampled.sampleSize
        } catch (ignored: Exception) {
        }
        return if (result != null) {
            try {
                result = rotateBitmapInt(result, degreesRotated)

                if (degreesRotated % 90 != 0) {
                    result = cropForRotatedImage(
                        result,
                        points,
                        rect,
                        degreesRotated,
                        fixAspectRatio,
                        aspectRatioX,
                        aspectRatioY
                    )
                }
            } catch (e: OutOfMemoryError) {
                result.recycle()
                throw e
            }
            BitmapSampled(result, sampleSize)
        } else {
            cropBitmap(
                context,
                loadedImageUri,
                points,
                degreesRotated,
                fixAspectRatio,
                aspectRatioX,
                aspectRatioY,
                sampleMulti,
                rect,
                width,
                height
            )
        }
    }


    private fun cropBitmap(
        context: Context, loadedImageUri: Uri, points: FloatArray,
        degreesRotated: Int, fixAspectRatio: Boolean, aspectRatioX: Int, aspectRatioY: Int,
        sampleMulti: Int, rect: Rect, width: Int, height: Int
    ): BitmapSampled {
        var result: Bitmap? = null
        val sampleSize: Int
        try {
            val options = BitmapFactory.Options()
            sampleSize = sampleMulti * calculateInSampleSizeByRequestedSize(
                rect.width(),
                rect.height(),
                width,
                height
            )
            options.inSampleSize = sampleSize
            val fullBitmap = decodeImage(context.contentResolver, loadedImageUri, options)
            if (fullBitmap != null) {
                try {
                    val points2 = FloatArray(points.size)
                    System.arraycopy(points, 0, points2, 0, points.size)
                    for (i in points2.indices) {
                        points2[i] = points2[i] / options.inSampleSize
                    }
                    result = cropBitmapObjectWithScale(
                        fullBitmap,
                        points2,
                        degreesRotated,
                        fixAspectRatio,
                        aspectRatioX,
                        aspectRatioY,
                        1f
                    )
                } finally {
                    if (result != fullBitmap) {
                        fullBitmap.recycle()
                    }
                }
            }
        } catch (e: OutOfMemoryError) {
            result?.recycle()
            throw e
        } catch (e: Exception) {
            throw RuntimeException( """  Failed to load sampled bitmap: $loadedImageUri   ${e.message}   """.trimIndent(), e)
        }
        return BitmapSampled(result, sampleSize)
    }


    @Throws(FileNotFoundException::class)
    private fun decodeImageForOption(resolver: ContentResolver, uri: Uri): BitmapFactory.Options {
        var stream: InputStream? = null
        return try {
            stream = resolver.openInputStream(uri)
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeStream(stream, EMPTY_RECT, options)
            options.inJustDecodeBounds = false
            options
        } finally {
            closeSafe(stream)
        }
    }


    @Throws(FileNotFoundException::class)
    private fun decodeImage(
        resolver: ContentResolver,
        uri: Uri,
        options: BitmapFactory.Options
    ): Bitmap? {
        do {
            var stream: InputStream? = null
            try {
                stream = resolver.openInputStream(uri)
                return BitmapFactory.decodeStream(stream, EMPTY_RECT, options)
            } catch (e: OutOfMemoryError) {
                options.inSampleSize *= 2
            } finally {
                closeSafe(stream)
            }
        } while (options.inSampleSize <= 512)
        throw RuntimeException("Failed to decode image: $uri")
    }


    private fun decodeSampledBitmapRegion(
        context: Context,
        uri: Uri,
        rect: Rect,
        reqWidth: Int,
        reqHeight: Int,
        sampleMulti: Int
    ): BitmapSampled {
        var stream: InputStream? = null
        var decoder: BitmapRegionDecoder? = null
        try {
            val options = BitmapFactory.Options()
            options.inSampleSize = sampleMulti * calculateInSampleSizeByRequestedSize(
                rect.width(),
                rect.height(),
                reqWidth,
                reqHeight
            )
            stream = context.contentResolver.openInputStream(uri)
            decoder = stream?.let { BitmapRegionDecoder.newInstance(it, false) }
            do {
                try {
                    return BitmapSampled(decoder?.decodeRegion(rect, options), options.inSampleSize)
                } catch (e: OutOfMemoryError) {
                    options.inSampleSize *= 2
                }
            } while (options.inSampleSize <= 512)
        } catch (e: Exception) {
            throw RuntimeException(  """  Failed to load sampled bitmap: $uri   ${e.message}  """.trimIndent(), e )
        } finally {
            closeSafe(stream)
            decoder?.recycle()
        }
        return BitmapSampled(null, 1)
    }


    private fun cropForRotatedImage(
        bitmap: Bitmap?, points: FloatArray, rect: Rect, degreesRotated: Int,
        fixAspectRatio: Boolean, aspectRatioX: Int, aspectRatioY: Int
    ): Bitmap? {
        var bitmap = bitmap
        if (degreesRotated % 90 != 0) {
            var adjLeft = 0
            var adjTop = 0
            var width = 0
            var height = 0
            val rads = Math.toRadians(degreesRotated.toDouble())
            val compareTo =
                if (degreesRotated < 90 || degreesRotated > 180 && degreesRotated < 270) rect.left else rect.right
            var i = 0
            while (i < points.size) {
                if (points[i] >= compareTo - 1 && points[i] <= compareTo + 1) {
                    adjLeft = abs(sin(rads) * (rect.bottom - points[i + 1]))
                        .toInt()
                    adjTop = abs(cos(rads) * (points[i + 1] - rect.top))
                        .toInt()
                    width = abs((points[i + 1] - rect.top) / sin(rads))
                        .toInt()
                    height = abs((rect.bottom - points[i + 1]) / cos(rads))
                        .toInt()
                    break
                }
                i += 2
            }
            rect[adjLeft, adjTop, adjLeft + width] = adjTop + height
            if (fixAspectRatio) {
                fixRectForAspectRatio(rect, aspectRatioX, aspectRatioY)
            }
            val bitmapTmp = bitmap
            bitmap = Bitmap.createBitmap(bitmap!!, rect.left, rect.top, rect.width(), rect.height())
            if (bitmapTmp != bitmap) {
                bitmapTmp!!.recycle()
            }
        }
        return bitmap
    }


    private fun calculateInSampleSizeByRequestedSize(
        width: Int,
        height: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            while (height / 2 / inSampleSize > reqHeight && width / 2 / inSampleSize > reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }


    private fun calculateInSampleSizeByMaxTextureSize(width: Int, height: Int): Int {
        var inSampleSize = 1
        if (mMaxTextureSize == 0) {
            mMaxTextureSize = maxTextureSize
        }
        if (mMaxTextureSize > 0) {
            while (height / inSampleSize > mMaxTextureSize || width / inSampleSize > mMaxTextureSize) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }


    private fun getFileFromUri(context: Context, uri: Uri): File {
        var file = File(uri.path)
        if (file.exists()) {
            return file
        }
        var cursor: Cursor? = null
        try {
            val proj = arrayOf(MediaStore.Images.Media.DATA)
            cursor = context.contentResolver.query(uri, proj, null, null, null)
            if (cursor != null) {
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                cursor.moveToFirst()
                val realPath = cursor.getString(columnIndex)
                file = File(realPath)
            }
        } catch (ignored: Exception) {
        } finally {
            cursor?.close()
        }
        return file
    }


    private fun rotateBitmapInt(bitmap: Bitmap, degrees: Int): Bitmap {
        return if (degrees > 0) {
            val matrix = Matrix()
            matrix.setRotate(degrees.toFloat())
            val newBitmap =
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, false)
            if (newBitmap != bitmap) {
                bitmap.recycle()
            }
            newBitmap
        } else {
            bitmap
        }
    }

    private val maxTextureSize: Int
        get() {
            val imageMaxBitmapDimension  = 2048
            return try {
                val egl = EGLContext.getEGL() as EGL10
                val display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)

                val version = IntArray(2)
                egl.eglInitialize(display, version)

                val totalConfigurations = IntArray(1)
                egl.eglGetConfigs(display, null, 0, totalConfigurations)

                val configurationsList = arrayOfNulls<EGLConfig>(
                    totalConfigurations[0]
                )
                egl.eglGetConfigs(
                    display, configurationsList,
                    totalConfigurations[0], totalConfigurations
                )
                val textureSize = IntArray(1)
                var maximumTextureSize = 0

                for (i in 0 until totalConfigurations[0]) {
                    egl.eglGetConfigAttrib(
                        display,
                        configurationsList[i], EGL10.EGL_MAX_PBUFFER_WIDTH, textureSize
                    )

                    if (maximumTextureSize < textureSize[0]) {
                        maximumTextureSize = textureSize[0]
                    }
                }

                egl.eglTerminate(display)
                maximumTextureSize.coerceAtLeast(imageMaxBitmapDimension)
            } catch (e: Exception) {
                imageMaxBitmapDimension
            }
        }


    private fun closeSafe(closeable: Closeable?) {
        if (closeable != null) {
            try {
                closeable.close()
            } catch (ignored: IOException) {
            }
        }
    }

    class BitmapSampled(val bitmap: Bitmap?, var sampleSize: Int) {
        init { this.sampleSize = sampleSize }
    }

    class RotateBitmapResult(bitmap: Bitmap?, degrees: Int) {
        val bitmap: Bitmap? = bitmap
        val degrees: Int = degrees
    }
}