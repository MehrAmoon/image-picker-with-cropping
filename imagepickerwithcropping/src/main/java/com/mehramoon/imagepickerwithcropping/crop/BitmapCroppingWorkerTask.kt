package com.mehramoon.imagepickerwithcropping.crop

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.net.Uri
import com.mehramoon.imagepickerwithcropping.Utils.executeAsyncTask
import kotlinx.coroutines.*
import kotlinx.coroutines.NonCancellable.isCancelled
import java.lang.ref.WeakReference


class BitmapCroppingWorkerTask {

    var bitmapCroppingWorkerTaskJob: Job? = null
    private val mCropImageViewReference: WeakReference<CropImageView>
    private val mBitmap: Bitmap?
    val uri: Uri?
    private val mContext: Context
    private val mCropPoints: FloatArray
    private val mDegreesRotated: Int
    private val mOrgWidth: Int
    private val mOrgHeight: Int
    private val mFixAspectRatio: Boolean
    private val mAspectRatioX: Int
    private val mAspectRatioY: Int
    private val mReqWidth: Int
    private val mReqHeight: Int
    private val mReqSizeOptions: CropImageView.RequestSizeOptions
    private val mSaveUri: Uri?
    private val mSaveCompressFormat: CompressFormat
    private val mSaveCompressQuality: Int


    constructor(
        cropImageView: CropImageView, bitmap: Bitmap?, cropPoints: FloatArray,
        degreesRotated: Int, fixAspectRatio: Boolean, aspectRatioX: Int, aspectRatioY: Int,
        reqWidth: Int, reqHeight: Int, options: CropImageView.RequestSizeOptions,
        saveUri: Uri?, saveCompressFormat: CompressFormat, saveCompressQuality: Int) {
        mCropImageViewReference = WeakReference(cropImageView)
        mContext = cropImageView.context
        mBitmap = bitmap
        mCropPoints = cropPoints
        uri = null
        mDegreesRotated = degreesRotated
        mFixAspectRatio = fixAspectRatio
        mAspectRatioX = aspectRatioX
        mAspectRatioY = aspectRatioY
        mReqWidth = reqWidth
        mReqHeight = reqHeight
        mReqSizeOptions = options
        mSaveUri = saveUri
        mSaveCompressFormat = saveCompressFormat
        mSaveCompressQuality = saveCompressQuality
        mOrgWidth = 0
        mOrgHeight = 0
    }

    constructor(
        cropImageView: CropImageView, uri: Uri?, cropPoints: FloatArray,
        degreesRotated: Int, orgWidth: Int, orgHeight: Int,
        fixAspectRatio: Boolean, aspectRatioX: Int, aspectRatioY: Int,
        reqWidth: Int, reqHeight: Int, options: CropImageView.RequestSizeOptions,
        saveUri: Uri?, saveCompressFormat: CompressFormat, saveCompressQuality: Int) {
        mCropImageViewReference = WeakReference(cropImageView)
        mContext = cropImageView.context
        this.uri = uri
        mCropPoints = cropPoints
        mDegreesRotated = degreesRotated
        mFixAspectRatio = fixAspectRatio
        mAspectRatioX = aspectRatioX
        mAspectRatioY = aspectRatioY
        mOrgWidth = orgWidth
        mOrgHeight = orgHeight
        mReqWidth = reqWidth
        mReqHeight = reqHeight
        mReqSizeOptions = options
        mSaveUri = saveUri
        mSaveCompressFormat = saveCompressFormat
        mSaveCompressQuality = saveCompressQuality
        mBitmap = null
    }


    fun bitmapCroppingWorkerTaskRun() {
        GlobalScope.launch { bitmapCroppingWorkerTaskFun() }
    }

    suspend fun bitmapCroppingWorkerTaskFun() {
        bitmapCroppingWorkerTaskJob = coroutineScope {
            executeAsyncTask(onPreExecute = {
                // ... runs in Main Thread


            }, doInBackground = {
                // ... runs in Worker(Background) Thread
                "Result" // send data to "onPostExecute"

                if (!isCancelled) {
                    val bitmapSampled: BitmapUtils.BitmapSampled = if (uri != null) {
                        BitmapUtils.cropBitmap(
                            mContext,
                            uri, mCropPoints, mDegreesRotated, mOrgWidth, mOrgHeight,
                            mFixAspectRatio, mAspectRatioX, mAspectRatioY, mReqWidth, mReqHeight
                        )
                    } else if (mBitmap != null) {
                        BitmapUtils.cropBitmapObjectHandleOOM(
                            mBitmap,
                            mCropPoints,
                            mDegreesRotated,
                            mFixAspectRatio,
                            mAspectRatioX,
                            mAspectRatioY
                        )
                    } else {
                        return@executeAsyncTask Result(null as Bitmap?, 1)
                    }
                    val bitmap: Bitmap? = bitmapSampled.bitmap?.let {
                        BitmapUtils.resizeBitmap(
                            it,
                            mReqWidth,
                            mReqHeight,
                            mReqSizeOptions
                        )
                    }
                    return@executeAsyncTask if (mSaveUri == null) {
                        Result(bitmap, bitmapSampled.sampleSize)
                    } else {
                        if (bitmap != null) {
                            BitmapUtils.writeBitmapToUri(
                                mContext,
                                bitmap,
                                mSaveUri,
                                mSaveCompressFormat,
                                mSaveCompressQuality
                            )
                        }
                        bitmap?.recycle()
                        Result(mSaveUri, bitmapSampled.sampleSize)
                    }
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
                            cropImageView.onImageCroppingAsyncComplete(it)
                        }
                    }
                    if (!completeCalled && it.bitmap != null) {
                        it.bitmap.recycle()
                    }
                }

            })
        }
    }


    class Result {
        val bitmap: Bitmap?
        val uri: Uri?
        val error: Exception?
        val isSave: Boolean
        val sampleSize: Int

        constructor(bitmap: Bitmap?, sampleSize: Int) {
            this.bitmap = bitmap
            uri = null
            error = null
            isSave = false
            this.sampleSize = sampleSize
        }

        constructor(uri: Uri?, sampleSize: Int) {
            bitmap = null
            this.uri = uri
            error = null
            isSave = true
            this.sampleSize = sampleSize
        }

        constructor(error: Exception?, isSave: Boolean) {
            bitmap = null
            uri = null
            this.error = error
            this.isSave = isSave
            sampleSize = 1
        }
    }

}
