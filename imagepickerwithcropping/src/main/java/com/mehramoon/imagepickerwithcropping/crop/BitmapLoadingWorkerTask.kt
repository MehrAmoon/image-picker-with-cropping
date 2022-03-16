package com.mehramoon.imagepickerwithcropping.crop

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.mehramoon.imagepickerwithcropping.Utils.executeAsyncTask
import kotlinx.coroutines.*
import kotlinx.coroutines.NonCancellable.isCancelled
import java.lang.ref.WeakReference


/**
 * Task to load bitmap asynchronously from the UI thread.
 */

class BitmapLoadingWorkerTask(cropImageView: CropImageView, val uri: Uri){

    var bitmapLoadingWorkerTaskJob: Job? = null
    private val mCropImageViewReference: WeakReference<CropImageView> = WeakReference(cropImageView)
    private val mContext: Context
    private val mWidth: Int
    private val mHeight: Int
    lateinit var decodeResult: BitmapUtils.BitmapSampled

    init {
        mContext = cropImageView.context
        val metrics = cropImageView.resources.displayMetrics
        val densityAdj = if (metrics.density > 1) (1 / metrics.density).toDouble() else 1.toDouble()
        mWidth = (metrics.widthPixels * densityAdj).toInt()
        mHeight = (metrics.heightPixels * densityAdj).toInt()
    }

    fun bitmapLoadingWorkerTaskRun() {
        decodeResult = BitmapUtils.decodeSampledBitmap(
            mContext,
            uri, mWidth, mHeight
        )
        GlobalScope.launch { bitmapLoadingWorkerTaskFun() }
    }

    suspend fun bitmapLoadingWorkerTaskFun() {
        bitmapLoadingWorkerTaskJob = coroutineScope {
            executeAsyncTask(onPreExecute = {
                // ... runs in Main Thread

            }, doInBackground = {
                // ... runs in Worker(Background) Thread
                "Result" // send data to "onPostExecute"

                if (!isCancelled) {
                    val rotateResult: BitmapUtils.RotateBitmapResult = BitmapUtils.rotateBitmapByExif(
                        decodeResult.bitmap, mContext,
                        uri
                    )
                    return@executeAsyncTask Result(
                        uri,
                        rotateResult.bitmap,
                        decodeResult.sampleSize,
                        rotateResult.degrees
                    )
                }
                null

            }, onPostExecute = {
                // runs in Main Thread
                // ... here "it" is the data returned from "doInBackground"

                if (it != null) {
                    var completeCalled = false
                    if (!isCancelled) {
                        val cropImageView = mCropImageViewReference.get()
                        if (cropImageView != null) {
                            completeCalled = true
                            cropImageView.onSetImageUriAsyncComplete(it)
                        }
                    }
                    if (!completeCalled && it.bitmap != null) {
                        // fast release of unused bitmap
                        it.bitmap.recycle()
                    }
                }

            })
        }
    }


    class Result {
        val uri: Uri
        val bitmap: Bitmap?
        val loadSampleSize: Int
        val degreesRotated: Int
        val error: Exception?

        internal constructor(uri: Uri, bitmap: Bitmap?, loadSampleSize: Int, degreesRotated: Int) {
            this.uri = uri
            this.bitmap = bitmap
            this.loadSampleSize = loadSampleSize
            this.degreesRotated = degreesRotated
            error = null
        }

        internal constructor(uri: Uri, error: Exception?) {
            this.uri = uri
            bitmap = null
            loadSampleSize = 0
            degreesRotated = 0
            this.error = error
        }
    }
}