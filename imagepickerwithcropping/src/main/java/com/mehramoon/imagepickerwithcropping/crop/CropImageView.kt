package com.mehramoon.imagepickerwithcropping.crop

import android.app.Activity
import android.content.Context
import android.graphics.*
import android.graphics.Bitmap.CompressFormat
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import com.mehramoon.imagepickerwithcropping.R

import com.mehramoon.imagepickerwithcropping.crop.BitmapUtils.cropBitmap
import com.mehramoon.imagepickerwithcropping.crop.BitmapUtils.cropBitmapObjectHandleOOM
import com.mehramoon.imagepickerwithcropping.crop.BitmapUtils.getRectBottom
import com.mehramoon.imagepickerwithcropping.crop.BitmapUtils.getRectCenterX
import com.mehramoon.imagepickerwithcropping.crop.BitmapUtils.getRectCenterY
import com.mehramoon.imagepickerwithcropping.crop.BitmapUtils.getRectFromPoints
import com.mehramoon.imagepickerwithcropping.crop.BitmapUtils.getRectHeight
import com.mehramoon.imagepickerwithcropping.crop.BitmapUtils.getRectLeft
import com.mehramoon.imagepickerwithcropping.crop.BitmapUtils.getRectRight
import com.mehramoon.imagepickerwithcropping.crop.BitmapUtils.getRectTop
import com.mehramoon.imagepickerwithcropping.crop.BitmapUtils.getRectWidth
import com.mehramoon.imagepickerwithcropping.crop.BitmapUtils.resizeBitmap
import com.mehramoon.imagepickerwithcropping.crop.BitmapUtils.rotateBitmapByExif
import java.lang.ref.WeakReference
import java.util.*


/**
 * Custom view that provides cropping capabilities to an image.
 */
class CropImageView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    FrameLayout(context, attrs) {
    //region: Fields and Consts
    /**
     * Image view widget used to show the image for cropping.
     */
    private val mImageView: ImageView

    /**
     * Overlay over the image view to show cropping UI.
     */
    private val mCropOverlayView: CropOverlayView?

    /**
     * The matrix used to transform the cropping image in the image view
     */
    private val mImageMatrix = Matrix()

    /**
     * Reusing matrix instance for reverse matrix calculations.
     */
    private val mImageInverseMatrix = Matrix()

    /**
     * Progress bar widget to show progress bar on async image loading and cropping.
     */
    private val mProgressBar: ProgressBar

    /**
     * Rectengale used in image matrix transformation calculation (reusing rect instance)
     */
    private val mImagePoints = FloatArray(8)

    /**
     * Animation class to smooth animate zoom-in/out
     */
    private var mAnimation: CropImageAnimation? = null
    private var mBitmap: Bitmap? = null
    private var mDegreesRotated = 0
    private var mLayoutWidth = 0
    private var mLayoutHeight = 0
    private var mImageResource = 0

    /**
     * The initial scale type of the image in the crop image view
     */
    private var mScaleType: ScaleType

    /**
     * if to show crop overlay UI what contains the crop window UI surrounded by background over the cropping
     * image.<br></br>
     * default: true, may disable for animation or frame transition.
     */
    private var mShowCropOverlay = true

    /**
     * if to show progress bar when image async loading/cropping is in progress.<br></br>
     * default: true, disable to provide custom progress bar UI.
     */
    private var mShowProgressBar = true

    /**
     * if auto-zoom functionality is enabled.<br></br>
     * default: true.
     */
    private var mAutoZoomEnabled = true

    /**
     * The max zoom allowed during cropping
     */
    private var mMaxZoom: Int

    /**
     * callback to be invoked when image async loading is complete.
     */
    private var mOnSetImageUriCompleteListener: OnSetImageUriCompleteListener? = null

    /**
     * callback to be invoked when image async cropping is complete.
     */
    private var mOnCropImageCompleteListener: OnCropImageCompleteListener? = null

    /**
     * callback to be invoked when image async cropping is complete (get bitmap)
     */
    @Deprecated("")
    private var mOnGetCroppedImageCompleteListener: OnGetCroppedImageCompleteListener? = null

    /**
     * callback to be invoked when image async cropping is complete (save to uri)
     */
    @Deprecated("")
    private var mOnSaveCroppedImageCompleteListener: OnSaveCroppedImageCompleteListener? = null
    /**
     * Get the URI of an image that was set by URI, null otherwise.
     */
    /**
     * The URI that the image was loaded from (if loaded from URI)
     */
    var imageUri: Uri? = null
        private set

    /**
     * The sample size the image was loaded by if was loaded by URI
     */
    private var mLoadedSampleSize = 1

    /**
     * The current zoom level to to scale the cropping image
     */
    private var mZoom = 1f

    /**
     * The X offset that the cropping image was translated after zooming
     */
    private var mZoomOffsetX = 0f

    /**
     * The Y offset that the cropping image was translated after zooming
     */
    private var mZoomOffsetY = 0f

    /**
     * Used to restore the cropping windows rectangle after state restore
     */
    private var mRestoreCropWindowRect: RectF? = null

    /**
     * Used to detect size change to handle auto-zoom using [.handleCropWindowChanged] in
     * [.layout].
     */
    private var mSizeChanged = false

    /**
     * Task used to load bitmap async from UI thread
     */
    private var mBitmapLoadingWorkerTask: WeakReference<BitmapLoadingWorkerTask>? = null

    /**
     * Task used to crop bitmap async from UI thread
     */
    private var mBitmapCroppingWorkerTask: WeakReference<BitmapCroppingWorkerTask>? = null
    /**
     * Get the scale type of the image in the crop view.
     */
    /**
     * Set the scale type of the image in the crop view
     */
    var scaleType: ScaleType
        get() = mScaleType
        set(scaleType) {
            if (scaleType != mScaleType) {
                mScaleType = scaleType
                mZoom = 1f
                mZoomOffsetY = 0f
                mZoomOffsetX = mZoomOffsetY
                mCropOverlayView!!.resetCropOverlayView()
                requestLayout()
            }
        }
    /**
     * The shape of the cropping area - rectangle/circular.
     */
    /**
     * The shape of the cropping area - rectangle/circular.<br></br>
     * To set square/circle crop shape set aspect ratio to 1:1.
     */
    var cropShape: CropShape?
        get() = mCropOverlayView!!.cropShape
        set(cropShape) {
            mCropOverlayView!!.setCropShape(cropShape!!)
        }
    /**
     * if auto-zoom functionality is enabled. default: true.
     */
    /**
     * Set auto-zoom functionality to enabled/disabled.
     */
    var isAutoZoomEnabled: Boolean
        get() = mAutoZoomEnabled
        set(autoZoomEnabled) {
            if (mAutoZoomEnabled != autoZoomEnabled) {
                mAutoZoomEnabled = autoZoomEnabled
                handleCropWindowChanged(false, false)
                mCropOverlayView!!.invalidate()
            }
        }

    /**
     * Set multi touch functionality to enabled/disabled.
     */
    fun setMultiTouchEnabled(multiTouchEnabled: Boolean) {
        if (mCropOverlayView!!.setMultiTouchEnabled(multiTouchEnabled)) {
            handleCropWindowChanged(false, false)
            mCropOverlayView.invalidate()
        }
    }
    /**
     * The max zoom allowed during cropping.
     */
    /**
     * The max zoom allowed during cropping.
     */
    var maxZoom: Int
        get() = mMaxZoom
        set(maxZoom) {
            if (mMaxZoom != maxZoom && maxZoom > 0) {
                mMaxZoom = maxZoom
                handleCropWindowChanged(false, false)
                mCropOverlayView!!.invalidate()
            }
        }

    /**
     * the min size the resulting cropping image is allowed to be, affects the cropping window limits
     * (in pixels).<br></br>
     */
    fun setMinCropResultSize(minCropResultWidth: Int, minCropResultHeight: Int) {
        mCropOverlayView!!.setMinCropResultSize(minCropResultWidth, minCropResultHeight)
    }

    /**
     * the max size the resulting cropping image is allowed to be, affects the cropping window limits
     * (in pixels).<br></br>
     */
    fun setMaxCropResultSize(maxCropResultWidth: Int, maxCropResultHeight: Int) {
        mCropOverlayView!!.setMaxCropResultSize(maxCropResultWidth, maxCropResultHeight)
    }
    /**
     * Get the amount of degrees the cropping image is rotated cloackwise.<br></br>
     *
     * @return 0-360
     */
    /**
     * Set the amount of degrees the cropping image is rotated cloackwise.<br></br>
     *
     * @param degrees 0-360
     */
    var rotatedDegrees: Int
        get() = mDegreesRotated
        set(degrees) {
            if (mDegreesRotated != degrees) {
                rotateImage(degrees - mDegreesRotated)
            }
        }

    /**
     * whether the aspect ratio is fixed or not; true fixes the aspect ratio, while false allows it to be changed.
     */
    val isFixAspectRatio: Boolean
        get() = mCropOverlayView!!.isFixAspectRatio

    /**
     * Sets whether the aspect ratio is fixed or not; true fixes the aspect ratio, while false allows it to be changed.
     */
    fun setFixedAspectRatio(fixAspectRatio: Boolean) {
        mCropOverlayView!!.setFixedAspectRatio(fixAspectRatio)
    }
    /**
     * Get the current guidelines option set.
     */
    /**
     * Sets the guidelines for the CropOverlayView to be either on, off, or to show when resizing the application.
     */
    var guidelines: Guidelines?
        get() = mCropOverlayView!!.guidelines
        set(guidelines) {
            mCropOverlayView!!.setGuidelines(guidelines!!)
        }

    /**
     * both the X and Y values of the aspectRatio.
     */
    val aspectRatio: Pair<Int, Int>
        get() = Pair(mCropOverlayView!!.aspectRatioX, mCropOverlayView.aspectRatioY)

    /**
     * Sets the both the X and Y values of the aspectRatio.<br></br>
     * Sets fixed aspect ratio to TRUE.
     *
     * @param aspectRatioX int that specifies the new X value of the aspect ratio
     * @param aspectRatioY int that specifies the new Y value of the aspect ratio
     */
    fun setAspectRatio(aspectRatioX: Int, aspectRatioY: Int) {
        mCropOverlayView!!.aspectRatioX = aspectRatioX
        mCropOverlayView.aspectRatioY = aspectRatioY
        setFixedAspectRatio(true)
    }

    /**
     * Clears set aspect ratio values and sets fixed aspect ratio to FALSE.
     */
    fun clearAspectRatio() {
        mCropOverlayView!!.aspectRatioX = 1
        mCropOverlayView.aspectRatioY = 1
        setFixedAspectRatio(false)
    }

    /**
     * An edge of the crop window will snap to the corresponding edge of a
     * specified bounding box when the crop window edge is less than or equal to
     * this distance (in pixels) away from the bounding box edge. (default: 3dp)
     */
    fun setSnapRadius(snapRadius: Float) {
        if (snapRadius >= 0) {
            mCropOverlayView!!.setSnapRadius(snapRadius)
        }
    }
    /**
     * if to show progress bar when image async loading/cropping is in progress.<br></br>
     * default: true, disable to provide custom progress bar UI.
     */
    /**
     * if to show progress bar when image async loading/cropping is in progress.<br></br>
     * default: true, disable to provide custom progress bar UI.
     */
    var isShowProgressBar: Boolean
        get() = mShowProgressBar
        set(showProgressBar) {
            if (mShowProgressBar != showProgressBar) {
                mShowProgressBar = showProgressBar
                setProgressBarVisibility()
            }
        }
    /**
     * if to show crop overlay UI what contains the crop window UI surrounded by background over the cropping
     * image.<br></br>
     * default: true, may disable for animation or frame transition.
     */
    /**
     * if to show crop overlay UI what contains the crop window UI surrounded by background over the cropping
     * image.<br></br>
     * default: true, may disable for animation or frame transition.
     */
    var isShowCropOverlay: Boolean
        get() = mShowCropOverlay
        set(showCropOverlay) {
            if (mShowCropOverlay != showCropOverlay) {
                mShowCropOverlay = showCropOverlay
                setCropOverlayVisibility()
            }
        }
    /**
     * Returns the integer of the imageResource
     */
    /**
     * Sets a Drawable as the content of the CropImageView.
     *
     * @param resId the drawable resource ID to set
     */
    var imageResource: Int
        get() = mImageResource
        set(resId) {
            if (resId != 0) {
                mCropOverlayView!!.initialCropWindowRect = null
                val bitmap = BitmapFactory.decodeResource(resources, resId)
                setBitmap(bitmap, resId)
            }
        }// get the points of the crop rectangle adjusted to source bitmap

    // get the rectangle for the points (it may be larger than original if rotation is not stright)
    /**
     * Set the crop window position and size to the given rectangle.<br></br>
     * Image to crop must be first set before invoking this, for async - after complete callback.
     *
     * @param rect window rectangle (position and size) relative to source bitmap
     */
    /**
     * Gets the crop window's position relative to the source Bitmap (not the image
     * displayed in the CropImageView) using the original image rotation.
     *
     * @return a Rect instance containing cropped area boundaries of the source Bitmap
     */
    var cropRect: Rect?
        get() = if (mBitmap != null) {

            // get the points of the crop rectangle adjusted to source bitmap
            val points = cropPoints
            val orgWidth = mBitmap!!.width * mLoadedSampleSize
            val orgHeight = mBitmap!!.height * mLoadedSampleSize

            // get the rectangle for the points (it may be larger than original if rotation is not stright)
            getRectFromPoints(
                points,
                orgWidth,
                orgHeight,
                mCropOverlayView!!.isFixAspectRatio,
                mCropOverlayView.aspectRatioX,
                mCropOverlayView.aspectRatioY
            )
        } else {
            null
        }
        set(rect) {
            mCropOverlayView!!.initialCropWindowRect = rect
        }// Get crop window position relative to the displayed image.

    /**
     * Gets the 4 points of crop window's position relative to the source Bitmap (not the image
     * displayed in the CropImageView) using the original image rotation.<br></br>
     * Note: the 4 points may not be a rectangle if the image was rotates to NOT stright angle (!= 90/180/270).
     *
     * @return 4 points (x0,y0,x1,y1,x2,y2,x3,y3) of cropped area boundaries
     */
    val cropPoints: FloatArray
        get() {

            // Get crop window position relative to the displayed image.
            val cropWindowRect = mCropOverlayView!!.cropWindowRect
            val points = floatArrayOf(
                cropWindowRect.left,
                cropWindowRect.top,
                cropWindowRect.right,
                cropWindowRect.top,
                cropWindowRect.right,
                cropWindowRect.bottom,
                cropWindowRect.left,
                cropWindowRect.bottom
            )
            mImageMatrix.invert(mImageInverseMatrix)
            mImageInverseMatrix.mapPoints(points)
            for (i in points.indices) {
                points[i] *= mLoadedSampleSize.toFloat()
            }
            return points
        }

    /**
     * Reset crop window to initial rectangle.
     */
    fun resetCropRect() {
        mZoom = 1f
        mZoomOffsetX = 0f
        mZoomOffsetY = 0f
        mDegreesRotated = 0
        applyImageMatrix(width.toFloat(), height.toFloat(), false, false)
        mCropOverlayView!!.resetCropWindowRect()
    }

    /**
     * Gets the cropped image based on the current crop window.
     *
     * @return a new Bitmap representing the cropped image
     */
    val croppedImage: Bitmap?
        get() = getCroppedImage(0, 0, RequestSizeOptions.NONE)

    /**
     * Gets the cropped image based on the current crop window.<br></br>
     * Uses [RequestSizeOptions.RESIZE_INSIDE] option.
     *
     * @param reqWidth the width to resize the cropped image to
     * @param reqHeight the height to resize the cropped image to
     * @return a new Bitmap representing the cropped image
     */
    fun getCroppedImage(reqWidth: Int, reqHeight: Int): Bitmap? {
        return getCroppedImage(reqWidth, reqHeight, RequestSizeOptions.RESIZE_INSIDE)
    }

    /**
     * Gets the cropped image based on the current crop window.<br></br>
     *
     * @param reqWidth the width to resize the cropped image to (see options)
     * @param reqHeight the height to resize the cropped image to (see options)
     * @param options the resize method to use, see its documentation
     * @return a new Bitmap representing the cropped image
     */
    fun getCroppedImage(reqWidth: Int, reqHeight: Int, options: RequestSizeOptions): Bitmap? {
        var reqWidth = reqWidth
        var reqHeight = reqHeight
        var croppedBitmap: Bitmap? = null
        if (mBitmap != null) {
            mImageView.clearAnimation()
            reqWidth = if (options != RequestSizeOptions.NONE) reqWidth else 0
            reqHeight = if (options != RequestSizeOptions.NONE) reqHeight else 0
            croppedBitmap =
                if (imageUri != null && (mLoadedSampleSize > 1 || options == RequestSizeOptions.SAMPLING)) {
                    val orgWidth = mBitmap!!.width * mLoadedSampleSize
                    val orgHeight = mBitmap!!.height * mLoadedSampleSize
                    val bitmapSampled = cropBitmap(
                        context,
                        imageUri!!,
                        cropPoints,
                        mDegreesRotated,
                        orgWidth,
                        orgHeight,
                        mCropOverlayView!!.isFixAspectRatio,
                        mCropOverlayView.aspectRatioX,
                        mCropOverlayView.aspectRatioY,
                        reqWidth,
                        reqHeight
                    )
                    bitmapSampled.bitmap
                } else {
                    cropBitmapObjectHandleOOM(
                        mBitmap!!,
                        cropPoints,
                        mDegreesRotated,
                        mCropOverlayView!!.isFixAspectRatio,
                        mCropOverlayView.aspectRatioX,
                        mCropOverlayView.aspectRatioY
                    ).bitmap
                }
            croppedBitmap = resizeBitmap(
                croppedBitmap!!, reqWidth, reqHeight, options
            )
        }
        return croppedBitmap
    }

    /**
     * Gets the cropped image based on the current crop window.<br></br>
     * The result will be invoked to listener set by [.setOnGetCroppedImageCompleteListener].
     */
    val croppedImageAsync: Unit
        get() {
            getCroppedImageAsync(0, 0, RequestSizeOptions.NONE)
        }

    /**
     * Gets the cropped image based on the current crop window.<br></br>
     * Uses [RequestSizeOptions.RESIZE_INSIDE] option.<br></br>
     * The result will be invoked to listener set by [.setOnCropImageCompleteListener].
     *
     * @param reqWidth the width to resize the cropped image to
     * @param reqHeight the height to resize the cropped image to
     */
    fun getCroppedImageAsync(reqWidth: Int, reqHeight: Int) {
        getCroppedImageAsync(reqWidth, reqHeight, RequestSizeOptions.RESIZE_INSIDE)
    }

    /**
     * Gets the cropped image based on the current crop window.<br></br>
     * The result will be invoked to listener set by [.setOnCropImageCompleteListener].
     *
     * @param reqWidth the width to resize the cropped image to (see options)
     * @param reqHeight the height to resize the cropped image to (see options)
     * @param options the resize method to use, see its documentation
     */
    fun getCroppedImageAsync(reqWidth: Int, reqHeight: Int, options: RequestSizeOptions) {
        require(!(mOnCropImageCompleteListener == null && mOnGetCroppedImageCompleteListener == null)) { "mOnCropImageCompleteListener is not set" }
        startCropWorkerTask(reqWidth, reqHeight, options, null, null, 0)
    }

    /**
     * Save the cropped image based on the current crop window to the given uri.<br></br>
     * Uses [RequestSizeOptions.RESIZE_INSIDE] option.<br></br>
     * The result will be invoked to listener set by [.setOnGetCroppedImageCompleteListener].
     *
     * @param saveUri the Android Uri to save the cropped image to
     * @param saveCompressFormat the compression format to use when writing the image
     * @param saveCompressQuality the quality (if applicable) to use when writing the image (0 - 100)
     * @param reqWidth the width to resize the cropped image to
     * @param reqHeight the height to resize the cropped image to
     */
    fun saveCroppedImageAsync(
        saveUri: Uri?,
        saveCompressFormat: CompressFormat?,
        saveCompressQuality: Int,
        reqWidth: Int,
        reqHeight: Int
    ) {
        saveCroppedImageAsync(
            saveUri,
            saveCompressFormat,
            saveCompressQuality,
            reqWidth,
            reqHeight,
            RequestSizeOptions.RESIZE_INSIDE
        )
    }
    /**
     * Save the cropped image based on the current crop window to the given uri.<br></br>
     * The result will be invoked to listener set by [.setOnGetCroppedImageCompleteListener].
     *
     * @param saveUri the Android Uri to save the cropped image to
     * @param saveCompressFormat the compression format to use when writing the image
     * @param saveCompressQuality the quality (if applicable) to use when writing the image (0 - 100)
     * @param reqWidth the width to resize the cropped image to (see options)
     * @param reqHeight the height to resize the cropped image to (see options)
     * @param options the resize method to use, see its documentation
     */
    /**
     * Save the cropped image based on the current crop window to the given uri.<br></br>
     * Uses JPEG image compression with 90 compression quality.<br></br>
     * The result will be invoked to listener set by [.setOnGetCroppedImageCompleteListener].
     *
     * @param saveUri the Android Uri to save the cropped image to
     */
    /**
     * Save the cropped image based on the current crop window to the given uri.<br></br>
     * The result will be invoked to listener set by [.setOnGetCroppedImageCompleteListener].
     *
     * @param saveUri the Android Uri to save the cropped image to
     * @param saveCompressFormat the compression format to use when writing the image
     * @param saveCompressQuality the quality (if applicable) to use when writing the image (0 - 100)
     */
    @JvmName("saveCroppedImageAsync1")
    @JvmOverloads
    fun saveCroppedImageAsync(
        saveUri: Uri?,
        saveCompressFormat: CompressFormat? = CompressFormat.JPEG,
        saveCompressQuality: Int = 90,
        reqWidth: Int = 0,
        reqHeight: Int = 0,
        options: RequestSizeOptions = RequestSizeOptions.NONE
    ) {
        require(!(mOnCropImageCompleteListener == null && mOnSaveCroppedImageCompleteListener == null)) { "mOnCropImageCompleteListener is not set" }
        startCropWorkerTask(
            reqWidth,
            reqHeight,
            options,
            saveUri,
            saveCompressFormat,
            saveCompressQuality
        )
    }

    /**
     * Set the callback to be invoked when image async loading ([.setImageUriAsync])
     * is complete (successful or failed).
     */
    fun setOnSetImageUriCompleteListener(listener: OnSetImageUriCompleteListener?) {
        mOnSetImageUriCompleteListener = listener
    }

    /**
     * Set the callback to be invoked when image async cropping image ([.getCroppedImageAsync] or
     * [.saveCroppedImageAsync]) is complete (successful or failed).
     */
    fun setOnCropImageCompleteListener(listener: OnCropImageCompleteListener?) {
        mOnCropImageCompleteListener = listener
    }

    /**
     * Set the callback to be invoked when image async get cropping image ([.getCroppedImageAsync])
     * is complete (successful or failed).
     *
     */
    @Deprecated("use {@link #setOnCropImageCompleteListener(OnCropImageCompleteListener)}.")
    fun setOnGetCroppedImageCompleteListener(listener: OnGetCroppedImageCompleteListener?) {
        mOnGetCroppedImageCompleteListener = listener
    }

    /**
     * Set the callback to be invoked when image async save cropping image ([.saveCroppedImageAsync])
     * is complete (successful or failed).
     *
     */
    @Deprecated("use {@link #setOnCropImageCompleteListener(OnCropImageCompleteListener)}.")
    fun setOnSaveCroppedImageCompleteListener(listener: OnSaveCroppedImageCompleteListener?) {
        mOnSaveCroppedImageCompleteListener = listener
    }

    /**
     * Sets a Bitmap as the content of the CropImageView.
     *
     * @param bitmap the Bitmap to set
     */
    fun setImageBitmap(bitmap: Bitmap?) {
        mCropOverlayView!!.initialCropWindowRect = null
        setBitmap(bitmap)
    }

    /**
     * Sets a Bitmap and initializes the image rotation according to the EXIT data.<br></br>
     * <br></br>
     * The EXIF can be retrieved by doing the following:
     * `ExifInterface exif = new ExifInterface(path);`
     *
     * @param bitmap the original bitmap to set; if null, this
     * @param exif the EXIF information about this bitmap; may be null
     */
    fun setImageBitmap(bitmap: Bitmap?, exif: ExifInterface?) {
        val setBitmap: Bitmap?
        if (bitmap != null && exif != null) {
            val result = rotateBitmapByExif(bitmap, exif)
            setBitmap = result.bitmap
            mDegreesRotated = result.degrees
        } else {
            setBitmap = bitmap
        }
        mCropOverlayView!!.initialCropWindowRect = null
        setBitmap(setBitmap)
    }

    /**
     * Sets a bitmap loaded from the given Android URI as the content of the CropImageView.<br></br>
     * Can be used with URI from gallery or camera source.<br></br>
     * Will rotate the image by exif data.<br></br>
     *
     * @param uri the URI to load the image from
     */
    fun setImageUriAsync(uri: Uri?) {
        if (uri != null) {
            val currentTask =
                if (mBitmapLoadingWorkerTask != null) mBitmapLoadingWorkerTask!!.get() else null
            currentTask?.cancel(true)

            // either no existing task is working or we canceled it, need to load new URI
            clearImageInt()
            mCropOverlayView!!.initialCropWindowRect = null
            mBitmapLoadingWorkerTask = WeakReference(BitmapLoadingWorkerTask(this, uri))
            mBitmapLoadingWorkerTask!!.get()!!.execute()
            setProgressBarVisibility()
        }
    }

    /**
     * Clear the current image set for cropping.
     */
    fun clearImage() {
        clearImageInt()
        mCropOverlayView!!.initialCropWindowRect = null
    }

    /**
     * Rotates image by the specified number of degrees clockwise.<br></br>
     * Negative values represent counter-clockwise rotations.
     *
     * @param degrees Integer specifying the number of degrees to rotate.
     */
    fun rotateImage(degrees: Int) {
        var degrees = degrees
        if (mBitmap != null) {
            // Force degrees to be a non-zero value between 0 and 360 (inclusive)
            degrees = if (degrees < 0) {
                degrees % 360 + 360
            } else {
                degrees % 360
            }
            val flipAxes =
                !mCropOverlayView!!.isFixAspectRatio && (degrees > 45 && degrees < 135 || degrees > 215 && degrees < 305)
            BitmapUtils.RECT.set(mCropOverlayView.cropWindowRect)
            var halfWidth =
                (if (flipAxes) BitmapUtils.RECT.height() else BitmapUtils.RECT.width()) / 2f
            var halfHeight =
                (if (flipAxes) BitmapUtils.RECT.width() else BitmapUtils.RECT.height()) / 2f
            mImageMatrix.invert(mImageInverseMatrix)
            BitmapUtils.POINTS[0] = BitmapUtils.RECT.centerX()
            BitmapUtils.POINTS[1] = BitmapUtils.RECT.centerY()
            BitmapUtils.POINTS[2] = 0F
            BitmapUtils.POINTS[3] = 0F
            BitmapUtils.POINTS[4] = 1F
            BitmapUtils.POINTS[5] = 0F
            mImageInverseMatrix.mapPoints(BitmapUtils.POINTS)

            // This is valid because degrees is not negative.
            mDegreesRotated = (mDegreesRotated + degrees) % 360
            applyImageMatrix(width.toFloat(), height.toFloat(), true, false)

            // adjust the zoom so the crop window size remains the same even after image scale change
            mImageMatrix.mapPoints(BitmapUtils.POINTS2, BitmapUtils.POINTS)
            mZoom /= Math.sqrt(
                Math.pow(
                    (BitmapUtils.POINTS2[4] - BitmapUtils.POINTS2[2]).toDouble(),
                    2.0
                ) + Math.pow(
                    (
                            BitmapUtils.POINTS2[5] - BitmapUtils.POINTS2[3]).toDouble(), 2.0
                )
            ).toFloat()
            mZoom = Math.max(mZoom, 1f)
            applyImageMatrix(width.toFloat(), height.toFloat(), true, false)
            mImageMatrix.mapPoints(BitmapUtils.POINTS2, BitmapUtils.POINTS)

            // adjust the width/height by the changes in scaling to the image
            val change = Math.sqrt(
                Math.pow(
                    (BitmapUtils.POINTS2[4] - BitmapUtils.POINTS2[2]).toDouble(),
                    2.0
                ) + Math.pow(
                    (
                            BitmapUtils.POINTS2[5] - BitmapUtils.POINTS2[3]).toDouble(), 2.0
                )
            )
            halfWidth *= change.toFloat()
            halfHeight *= change.toFloat()

            // calculate the new crop window rectangle to center in the same location and have proper width/height
            BitmapUtils.RECT[BitmapUtils.POINTS2[0] - halfWidth, BitmapUtils.POINTS2[1] - halfHeight, BitmapUtils.POINTS2[0] + halfWidth] =
                BitmapUtils.POINTS2[1] + halfHeight
            mCropOverlayView.resetCropOverlayView()
            mCropOverlayView.cropWindowRect = BitmapUtils.RECT
            applyImageMatrix(width.toFloat(), height.toFloat(), true, false)
            handleCropWindowChanged(false, false)

            // make sure the crop window rectangle is within the cropping image bounds after all the changes
            mCropOverlayView.fixCurrentCropWindowRect()
        }
    }
    //region: Private methods
    /**
     * On complete of the async bitmap loading by [.setImageUriAsync] set the result
     * to the widget if still relevant and call listener if set.
     *
     * @param result the result of bitmap loading
     */
    fun onSetImageUriAsyncComplete(result: BitmapLoadingWorkerTask.Result) {
        mBitmapLoadingWorkerTask = null
        setProgressBarVisibility()
        if (result.error == null) {
            setBitmap(result.bitmap, result.uri, result.loadSampleSize, result.degreesRotated)
        }
        val listener = mOnSetImageUriCompleteListener
        listener?.onSetImageUriComplete(this, result.uri, result.error)
    }

    /**
     * On complete of the async bitmap cropping by [.getCroppedImageAsync] call listener if set.
     *
     * @param result the result of bitmap cropping
     */
    fun onImageCroppingAsyncComplete(result: BitmapCroppingWorkerTask.Result) {
        mBitmapCroppingWorkerTask = null
        setProgressBarVisibility()
        val listener = mOnCropImageCompleteListener
        if (listener != null) {
            val cropResult = CropResult(
                result.bitmap, result.uri, result.error,
                cropPoints,
                cropRect,
                rotatedDegrees, result.sampleSize
            )
            listener.onCropImageComplete(this, cropResult)
        }
        if (result.isSave) {
            val listener2 = mOnSaveCroppedImageCompleteListener
            listener2?.onSaveCroppedImageComplete(this, result.uri, result.error)
        } else {
            val listener2 = mOnGetCroppedImageCompleteListener
            listener2?.onGetCroppedImageComplete(this, result.bitmap, result.error)
        }
    }

    /**
     * [.setBitmap]}
     */
    private fun setBitmap(bitmap: Bitmap?) {
        setBitmap(bitmap, 0, null, 1, 0)
    }

    /**
     * [.setBitmap]}
     */
    private fun setBitmap(bitmap: Bitmap, imageResource: Int) {
        setBitmap(bitmap, imageResource, null, 1, 0)
    }

    /**
     * [.setBitmap]}
     */
    private fun setBitmap(
        bitmap: Bitmap?,
        imageUri: Uri,
        loadSampleSize: Int,
        degreesRotated: Int
    ) {
        setBitmap(bitmap, 0, imageUri, loadSampleSize, degreesRotated)
    }

    /**
     * Set the given bitmap to be used in for cropping<br></br>
     * Optionally clear full if the bitmap is new, or partial clear if the bitmap has been manipulated.
     */
    private fun setBitmap(
        bitmap: Bitmap?,
        imageResource: Int,
        imageUri: Uri?,
        loadSampleSize: Int,
        degreesRotated: Int
    ) {
        if (mBitmap == null || mBitmap != bitmap) {
            mImageView.clearAnimation()
            clearImageInt()
            mBitmap = bitmap
            mImageView.setImageBitmap(mBitmap)
            this.imageUri = imageUri
            mImageResource = imageResource
            mLoadedSampleSize = loadSampleSize
            mDegreesRotated = degreesRotated
            applyImageMatrix(width.toFloat(), height.toFloat(), true, false)
            if (mCropOverlayView != null) {
                mCropOverlayView.resetCropOverlayView()
                setCropOverlayVisibility()
            }
        }
    }

    /**
     * Clear the current image set for cropping.<br></br>
     * Full clear will also clear the data of the set image like Uri or Resource id while partial clear
     * will only clear the bitmap and recycle if required.
     */
    private fun clearImageInt() {

        // if we allocated the bitmap, release it as fast as possible
        if (mBitmap != null && (mImageResource > 0 || imageUri != null)) {
            mBitmap!!.recycle()
        }
        mBitmap = null

        // clean the loaded image flags for new image
        mImageResource = 0
        imageUri = null
        mLoadedSampleSize = 1
        mDegreesRotated = 0
        mZoom = 1f
        mZoomOffsetX = 0f
        mZoomOffsetY = 0f
        mImageMatrix.reset()
        mImageView.setImageBitmap(null)
        setCropOverlayVisibility()
    }

    /**
     * Gets the cropped image based on the current crop window.<br></br>
     * If (reqWidth,reqHeight) is given AND image is loaded from URI cropping will try to use sample size to fit in
     * the requested width and height down-sampling if possible - optimization to get best size to quality.<br></br>
     * The result will be invoked to listener set by [.setOnGetCroppedImageCompleteListener].
     *
     * @param reqWidth the width to resize the cropped image to (see options)
     * @param reqHeight the height to resize the cropped image to (see options)
     * @param options the resize method to use on the cropped bitmap
     * @param saveUri optional: to save the cropped image to
     * @param saveCompressFormat if saveUri is given, the given compression will be used for saving the image
     * @param saveCompressQuality if saveUri is given, the given quality will be used for the compression.
     */
    fun startCropWorkerTask(
        reqWidth: Int,
        reqHeight: Int,
        options: RequestSizeOptions,
        saveUri: Uri?,
        saveCompressFormat: CompressFormat?,
        saveCompressQuality: Int
    ) {
        var reqWidth = reqWidth
        var reqHeight = reqHeight
        if (mBitmap != null) {
            mImageView.clearAnimation()
            val currentTask =
                if (mBitmapCroppingWorkerTask != null) mBitmapCroppingWorkerTask!!.get() else null
            currentTask?.cancel(true)
            reqWidth = if (options != RequestSizeOptions.NONE) reqWidth else 0
            reqHeight = if (options != RequestSizeOptions.NONE) reqHeight else 0
            val orgWidth = mBitmap!!.width * mLoadedSampleSize
            val orgHeight = mBitmap!!.height * mLoadedSampleSize
            mBitmapCroppingWorkerTask =
                if (imageUri != null && (mLoadedSampleSize > 1 || options == RequestSizeOptions.SAMPLING)) {
                    WeakReference(
                        BitmapCroppingWorkerTask(
                            this,
                            imageUri,
                            cropPoints,
                            mDegreesRotated,
                            orgWidth,
                            orgHeight,
                            mCropOverlayView!!.isFixAspectRatio,
                            mCropOverlayView.aspectRatioX,
                            mCropOverlayView.aspectRatioY,
                            reqWidth,
                            reqHeight,
                            options,
                            saveUri,
                            saveCompressFormat!!,
                            saveCompressQuality
                        )
                    )
                } else {
                    WeakReference(
                        BitmapCroppingWorkerTask(
                            this,
                            mBitmap,
                            cropPoints,
                            mDegreesRotated,
                            mCropOverlayView!!.isFixAspectRatio,
                            mCropOverlayView.aspectRatioX,
                            mCropOverlayView.aspectRatioY,
                            reqWidth,
                            reqHeight,
                            options,
                            saveUri,
                            saveCompressFormat!!,
                            saveCompressQuality
                        )
                    )
                }
            mBitmapCroppingWorkerTask!!.get()
            setProgressBarVisibility()
        }
    }

    public override fun onSaveInstanceState(): Parcelable? {
        val bundle = Bundle()
        bundle.putParcelable("instanceState", super.onSaveInstanceState())
        bundle.putParcelable("LOADED_IMAGE_URI", imageUri)
        bundle.putInt("LOADED_IMAGE_RESOURCE", mImageResource)
        if (imageUri == null && mImageResource < 1) {
            bundle.putParcelable("SET_BITMAP", mBitmap)
        }
        if (imageUri != null && mBitmap != null) {
            val key = UUID.randomUUID().toString()
            BitmapUtils.mStateBitmap = Pair(key, WeakReference(mBitmap))
            bundle.putString("LOADED_IMAGE_STATE_BITMAP_KEY", key)
        }
        if (mBitmapLoadingWorkerTask != null) {
            val task = mBitmapLoadingWorkerTask!!.get()
            if (task != null) {
                bundle.putParcelable("LOADING_IMAGE_URI", task.uri)
            }
        }
        bundle.putInt("LOADED_SAMPLE_SIZE", mLoadedSampleSize)
        bundle.putInt("DEGREES_ROTATED", mDegreesRotated)
        bundle.putParcelable("INITIAL_CROP_RECT", mCropOverlayView!!.initialCropWindowRect)
        BitmapUtils.RECT.set(mCropOverlayView.cropWindowRect)
        mImageMatrix.invert(mImageInverseMatrix)
        mImageInverseMatrix.mapRect(BitmapUtils.RECT)
        bundle.putParcelable("CROP_WINDOW_RECT", BitmapUtils.RECT)
        bundle.putString("CROP_SHAPE", mCropOverlayView.cropShape!!.name)
        bundle.putBoolean("CROP_AUTO_ZOOM_ENABLED", mAutoZoomEnabled)
        bundle.putInt("CROP_MAX_ZOOM", mMaxZoom)
        return bundle
    }

    public override fun onRestoreInstanceState(state: Parcelable) {
        if (state is Bundle) {
            val bundle = state

            // prevent restoring state if already set by outside code
            if (mBitmapLoadingWorkerTask == null && imageUri == null && mBitmap == null && mImageResource == 0) {
                var uri = bundle.getParcelable<Uri>("LOADED_IMAGE_URI")
                if (uri != null) {
                    val key = bundle.getString("LOADED_IMAGE_STATE_BITMAP_KEY")
                    if (key != null) {
                        val stateBitmap =
                            if (BitmapUtils.mStateBitmap != null && BitmapUtils.mStateBitmap!!.first == key) BitmapUtils.mStateBitmap!!.second.get() else null
                        if (stateBitmap != null && !stateBitmap.isRecycled) {
                            BitmapUtils.mStateBitmap = null
                            setBitmap(stateBitmap, uri, bundle.getInt("LOADED_SAMPLE_SIZE"), 0)
                        }
                    }
                    if (imageUri == null) {
                        setImageUriAsync(uri)
                    }
                } else {
                    val resId = bundle.getInt("LOADED_IMAGE_RESOURCE")
                    if (resId > 0) {
                        imageResource = resId
                    } else {
                        val bitmap = bundle.getParcelable<Bitmap>("SET_BITMAP")
                        if (bitmap != null) {
                            setBitmap(bitmap)
                        } else {
                            uri = bundle.getParcelable("LOADING_IMAGE_URI")
                            uri?.let { setImageUriAsync(it) }
                        }
                    }
                }
                mDegreesRotated = bundle.getInt("DEGREES_ROTATED")
                mCropOverlayView!!.initialCropWindowRect =
                    bundle.getParcelable<Parcelable>("INITIAL_CROP_RECT") as Rect?
                mRestoreCropWindowRect = bundle.getParcelable("CROP_WINDOW_RECT")
                mCropOverlayView.setCropShape(
                    CropShape.valueOf(
                        bundle.getString("CROP_SHAPE")!!
                    )
                )
                mAutoZoomEnabled = bundle.getBoolean("CROP_AUTO_ZOOM_ENABLED")
                mMaxZoom = bundle.getInt("CROP_MAX_ZOOM")
            }
            super.onRestoreInstanceState(bundle.getParcelable("instanceState"))
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        var heightSize = MeasureSpec.getSize(heightMeasureSpec)
        if (mBitmap != null) {

            // Bypasses a baffling bug when used within a ScrollView, where heightSize is set to 0.
            if (heightSize == 0) {
                heightSize = mBitmap!!.height
            }
            val desiredWidth: Int
            val desiredHeight: Int
            var viewToBitmapWidthRatio = Double.POSITIVE_INFINITY
            var viewToBitmapHeightRatio = Double.POSITIVE_INFINITY

            // Checks if either width or height needs to be fixed
            if (widthSize < mBitmap!!.width) {
                viewToBitmapWidthRatio = widthSize.toDouble() / mBitmap!!.width.toDouble()
            }
            if (heightSize < mBitmap!!.height) {
                viewToBitmapHeightRatio = heightSize.toDouble() / mBitmap!!.height.toDouble()
            }

            // If either needs to be fixed, choose smallest ratio and calculate from there
            if (viewToBitmapWidthRatio != Double.POSITIVE_INFINITY || viewToBitmapHeightRatio != Double.POSITIVE_INFINITY) {
                if (viewToBitmapWidthRatio <= viewToBitmapHeightRatio) {
                    desiredWidth = widthSize
                    desiredHeight = (mBitmap!!.height * viewToBitmapWidthRatio).toInt()
                } else {
                    desiredHeight = heightSize
                    desiredWidth = (mBitmap!!.width * viewToBitmapHeightRatio).toInt()
                }
            } else {
                // Otherwise, the picture is within frame layout bounds. Desired width is simply picture size
                desiredWidth = mBitmap!!.width
                desiredHeight = mBitmap!!.height
            }
            val width = getOnMeasureSpec(widthMode, widthSize, desiredWidth)
            val height = getOnMeasureSpec(heightMode, heightSize, desiredHeight)
            mLayoutWidth = width
            mLayoutHeight = height
            setMeasuredDimension(mLayoutWidth, mLayoutHeight)
        } else {
            setMeasuredDimension(widthSize, heightSize)
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        if (mLayoutWidth > 0 && mLayoutHeight > 0) {
            // Gets original parameters, and creates the new parameters
            val origParams = this.layoutParams
            origParams.width = mLayoutWidth
            origParams.height = mLayoutHeight
            layoutParams = origParams
            if (mBitmap != null) {
                applyImageMatrix((r - l).toFloat(), (b - t).toFloat(), true, false)

                // after state restore we want to restore the window crop, possible only after widget size is known
                if (mRestoreCropWindowRect != null) {
                    mImageMatrix.mapRect(mRestoreCropWindowRect)
                    mCropOverlayView!!.cropWindowRect = mRestoreCropWindowRect as RectF
                    handleCropWindowChanged(false, false)
                    mCropOverlayView.fixCurrentCropWindowRect()
                    mRestoreCropWindowRect = null
                } else if (mSizeChanged) {
                    mSizeChanged = false
                    handleCropWindowChanged(false, false)
                }
            } else {
                updateImageBounds(true)
            }
        } else {
            updateImageBounds(true)
        }
    }

    /**
     * Detect size change to handle auto-zoom using [.handleCropWindowChanged] in
     * [.layout].
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mSizeChanged = oldw > 0 && oldh > 0
    }

    /**
     * Handle crop window change to:<br></br>
     * 1. Execute auto-zoom-in/out depending on the area covered of cropping window relative to the
     * available view area.<br></br>
     * 2. Slide the zoomed sub-area if the cropping window is outside of the visible view sub-area.<br></br>
     *
     * @param inProgress is the crop window change is still in progress by the user
     * @param animate if to animate the change to the image matrix, or set it directly
     */
    private fun handleCropWindowChanged(inProgress: Boolean, animate: Boolean) {
        val width = width
        val height = height
        if (mBitmap != null && width > 0 && height > 0) {
            val cropRect = mCropOverlayView!!.cropWindowRect
            if (inProgress) {
                if (cropRect.left < 0 || cropRect.top < 0 || cropRect.right > width || cropRect.bottom > height) {
                    applyImageMatrix(width.toFloat(), height.toFloat(), false, false)
                }
            } else if (mAutoZoomEnabled || mZoom > 1) {
                var newZoom = 0f
                // keep the cropping window covered area to 50%-65% of zoomed sub-area
                if (mZoom < mMaxZoom && cropRect.width() < width * 0.5f && cropRect.height() < height * 0.5f) {
                    newZoom = Math.min(
                        mMaxZoom.toFloat(),
                        Math.min(
                            width / (cropRect.width() / mZoom / 0.64f),
                            height / (cropRect.height() / mZoom / 0.64f)
                        )
                    )
                }
                if (mZoom > 1 && (cropRect.width() > width * 0.65f || cropRect.height() > height * 0.65f)) {
                    newZoom = Math.max(
                        1f,
                        Math.min(
                            width / (cropRect.width() / mZoom / 0.51f),
                            height / (cropRect.height() / mZoom / 0.51f)
                        )
                    )
                }
                if (!mAutoZoomEnabled) {
                    newZoom = 1f
                }
                if (newZoom > 0 && newZoom != mZoom) {
                    if (animate) {
                        if (mAnimation == null) {
                            // lazy create animation single instance
                            mAnimation = CropImageAnimation(
                                mImageView,
                                mCropOverlayView
                            )
                        }
                        // set the state for animation to start from
                        mAnimation!!.setStartState(mImagePoints, mImageMatrix)
                    }
                    mZoom = newZoom
                    applyImageMatrix(width.toFloat(), height.toFloat(), true, animate)
                }
            }
        }
    }

    /**
     * Apply matrix to handle the image inside the image view.
     *
     * @param width the width of the image view
     * @param height the height of the image view
     */
    private fun applyImageMatrix(width: Float, height: Float, center: Boolean, animate: Boolean) {
        if (mBitmap != null && width > 0 && height > 0) {
            mImageMatrix.invert(mImageInverseMatrix)
            val cropRect = mCropOverlayView!!.cropWindowRect
            mImageInverseMatrix.mapRect(cropRect)
            mImageMatrix.reset()

            // move the image to the center of the image view first so we can manipulate it from there
            mImageMatrix.postTranslate(
                (width - mBitmap!!.width) / 2,
                (height - mBitmap!!.height) / 2
            )
            mapImagePointsByImageMatrix()

            // rotate the image the required degrees from center of image
            if (mDegreesRotated > 0) {
                mImageMatrix.postRotate(
                    mDegreesRotated.toFloat(),
                    getRectCenterX(mImagePoints),
                    getRectCenterY(mImagePoints)
                )
                mapImagePointsByImageMatrix()
            }

            // scale the image to the image view, image rect transformed to know new width/height
            val scale =
                Math.min(width / getRectWidth(mImagePoints), height / getRectHeight(mImagePoints))
            if (mScaleType == ScaleType.FIT_CENTER || mScaleType == ScaleType.CENTER_INSIDE && scale < 1 || scale > 1 && mAutoZoomEnabled) {
                mImageMatrix.postScale(
                    scale,
                    scale,
                    getRectCenterX(mImagePoints),
                    getRectCenterY(mImagePoints)
                )
                mapImagePointsByImageMatrix()
            }

            // scale by the current zoom level
            mImageMatrix.postScale(
                mZoom,
                mZoom,
                getRectCenterX(mImagePoints),
                getRectCenterY(mImagePoints)
            )
            mapImagePointsByImageMatrix()
            mImageMatrix.mapRect(cropRect)
            if (center) {
                // set the zoomed area to be as to the center of cropping window as possible
                mZoomOffsetX = if (width > getRectWidth(mImagePoints)) 0F else Math.max(
                    Math.min(
                        width / 2 - cropRect.centerX(),
                        -getRectLeft(mImagePoints)
                    ), getWidth() - getRectRight(mImagePoints)
                ) / mZoom
                mZoomOffsetY = if (height > getRectHeight(mImagePoints)) 0F else Math.max(
                    Math.min(
                        height / 2 - cropRect.centerY(),
                        -getRectTop(mImagePoints)
                    ), getHeight() - getRectBottom(mImagePoints)
                ) / mZoom
            } else {
                // adjust the zoomed area so the crop window rectangle will be inside the area in case it was moved outside
                mZoomOffsetX = Math.min(
                    Math.max(mZoomOffsetX * mZoom, -cropRect.left), -cropRect.right + width
                ) / mZoom
                mZoomOffsetY = Math.min(
                    Math.max(mZoomOffsetY * mZoom, -cropRect.top), -cropRect.bottom + height
                ) / mZoom
            }

            // apply to zoom offset translate and update the crop rectangle to offset correctly
            mImageMatrix.postTranslate(mZoomOffsetX * mZoom, mZoomOffsetY * mZoom)
            cropRect.offset(mZoomOffsetX * mZoom, mZoomOffsetY * mZoom)
            mCropOverlayView.cropWindowRect = cropRect
            mapImagePointsByImageMatrix()

            // set matrix to apply
            if (animate) {
                // set the state for animation to end in, start animation now
                mAnimation!!.setEndState(mImagePoints, mImageMatrix)
                mImageView.startAnimation(mAnimation)
            } else {
                mImageView.imageMatrix = mImageMatrix
            }

            // update the image rectangle in the crop overlay
            updateImageBounds(false)
        }
    }

    /**
     * Adjust the given image rectangle by image transformation matrix to know the final rectangle of the image.<br></br>
     * To get the proper rectangle it must be first reset to orginal image rectangle.
     */
    private fun mapImagePointsByImageMatrix() {
        mImagePoints[0] = 0F
        mImagePoints[1] = 0F
        mImagePoints[2] = mBitmap!!.width.toFloat()
        mImagePoints[3] = 0F
        mImagePoints[4] = mBitmap!!.width.toFloat()
        mImagePoints[5] = mBitmap!!.height.toFloat()
        mImagePoints[6] = 0F
        mImagePoints[7] = mBitmap!!.height.toFloat()
        mImageMatrix.mapPoints(mImagePoints)
    }

    /**
     * Set visibility of crop overlay to hide it when there is no image or specificly set by client.
     */
    private fun setCropOverlayVisibility() {
        if (mCropOverlayView != null) {
            mCropOverlayView.visibility =
                if (mShowCropOverlay && mBitmap != null) VISIBLE else INVISIBLE
        }
    }

    /**
     * Set visibility of progress bar when async loading/cropping is in process and show is enabled.
     */
    private fun setProgressBarVisibility() {
        val visible = mShowProgressBar &&
                (mBitmap == null && mBitmapLoadingWorkerTask != null || mBitmapCroppingWorkerTask != null)
        mProgressBar.visibility = if (visible) VISIBLE else INVISIBLE
    }

    /**
     * Update the scale factor between the actual image bitmap and the shown image.<br></br>
     */
    private fun updateImageBounds(clear: Boolean) {
        if (mBitmap != null && !clear) {

            // Get the scale factor between the actual Bitmap dimensions and the displayed dimensions for width/height.
            val scaleFactorWidth = mBitmap!!.width * mLoadedSampleSize / getRectWidth(mImagePoints)
            val scaleFactorHeight =
                mBitmap!!.height * mLoadedSampleSize / getRectHeight(mImagePoints)
            mCropOverlayView!!.setCropWindowLimits(
                width.toFloat(),
                height.toFloat(),
                scaleFactorWidth,
                scaleFactorHeight
            )
        }

        // set the bitmap rectangle and update the crop window after scale factor is set
        mCropOverlayView!!.setBounds(if (clear) null else mImagePoints, width, height)
    }
    //endregion
    //region: Inner class: CropShape
    /**
     * The possible cropping area shape.<br></br>
     * To set square/circle crop shape set aspect ratio to 1:1.
     */
    enum class CropShape {
        RECTANGLE, OVAL
    }
    //endregion
    //region: Inner class: ScaleType
    /**
     * Options for scaling the bounds of cropping image to the bounds of Crop Image View.<br></br>
     * Note: Some options are affected by auto-zoom, if enabled.
     */
    enum class ScaleType {
        /**
         * Scale the image uniformly (maintain the image's aspect ratio) to fit in crop image view.<br></br>
         * The largest dimension will be equals to crop image view and the second dimension will be smaller.
         */
        FIT_CENTER,

        /**
         * Center the image in the view, but perform no scaling.<br></br>
         * Note: If auto-zoom is enabled and the source image is smaller than crop image view then it will be
         * scaled uniformly to fit the crop image view.
         */
        CENTER,

        /**
         * Scale the image uniformly (maintain the image's aspect ratio) so that both
         * dimensions (width and height) of the image will be equal to or **larger** than the
         * corresponding dimension of the view (minus padding).<br></br>
         * The image is then centered in the view.
         */
        CENTER_CROP,

        /**
         * Scale the image uniformly (maintain the image's aspect ratio) so that both
         * dimensions (width and height) of the image will be equal to or **less** than the
         * corresponding dimension of the view (minus padding).<br></br>
         * The image is then centered in the view.<br></br>
         * Note: If auto-zoom is enabled and the source image is smaller than crop image view then it will be
         * scaled uniformly to fit the crop image view.
         */
        CENTER_INSIDE
    }
    //endregion
    //region: Inner class: Guidelines
    /**
     * The possible guidelines showing types.
     */
    enum class Guidelines {
        /**
         * Never show
         */
        OFF,

        /**
         * Show when crop move action is live
         */
        ON_TOUCH,

        /**
         * Always show
         */
        ON
    }
    //endregion
    //region: Inner class: RequestSizeOptions
    /**
     * Possible options for handling requested width/height for cropping.
     */
    enum class RequestSizeOptions {
        /**
         * No resize/sampling is done unless required for memory management (OOM).
         */
        NONE,

        /**
         * Only sample the image during loading (if image set using URI) so the smallest of the image
         * dimensions will be between the requested size and x2 requested size.<br></br>
         * NOTE: resulting image will not be exactly requested width/height
         * see: [Loading Large
 * Bitmaps Efficiently](http://developer.android.com/training/displaying-bitmaps/load-bitmap.html).
         */
        SAMPLING,

        /**
         * Resize the image uniformly (maintain the image's aspect ratio) so that both
         * dimensions (width and height) of the image will be equal to or **less** than the
         * corresponding requested dimension.<br></br>
         * If the image is smaller than the requested size it will NOT change.
         */
        RESIZE_INSIDE,

        /**
         * Resize the image uniformly (maintain the image's aspect ratio) to fit in the given width/height.<br></br>
         * The largest dimension will be equals to the requested and the second dimension will be smaller.<br></br>
         * If the image is smaller than the requested size it will enlarge it.
         */
        RESIZE_FIT,

        /**
         * Resize the image to fit exactly in the given width/height.<br></br>
         * This resize method does NOT preserve aspect ratio.<br></br>
         * If the image is smaller than the requested size it will enlarge it.
         */
        RESIZE_EXACT
    }
    //endregion
    //region: Inner class: OnSetImageUriCompleteListener
    /**
     * Interface definition for a callback to be invoked when image async loading is complete.
     */
    interface OnSetImageUriCompleteListener {
        /**
         * Called when a crop image view has completed loading image for cropping.<br></br>
         * If loading failed error parameter will contain the error.
         *
         * @param view The crop image view that loading of image was complete.
         * @param uri the URI of the image that was loading
         * @param error if error occurred during loading will contain the error, otherwise null.
         */
        fun onSetImageUriComplete(view: CropImageView?, uri: Uri?, error: Exception?)
    }
    //endregion
    //region: Inner class: OnGetCroppedImageCompleteListener
    /**
     * Interface definition for a callback to be invoked when image async crop is complete.
     */
    interface OnCropImageCompleteListener {
        /**
         * Called when a crop image view has completed cropping image.<br></br>
         * Result object contains the cropped bitmap, saved cropped image uri, crop points data or
         * the error occured during cropping.
         *
         * @param view The crop image view that cropping of image was complete.
         * @param result the crop image result data (with cropped image or error)
         */
        fun onCropImageComplete(view: CropImageView?, result: CropResult?)
    }
    //endregion
    //region: Inner class: OnGetCroppedImageCompleteListener
    /**
     * Interface definition for a callback to be invoked when image async cropping is complete.
     *
     */
    @Deprecated(
        """use {@link #setOnCropImageCompleteListener(OnCropImageCompleteListener)} and {@link
     * OnCropImageCompleteListener}."""
    )
    interface OnGetCroppedImageCompleteListener {
        /**
         * Called when a crop image view has completed cropping image.<br></br>
         * If cropping failed error parameter will contain the error.
         *
         * @param view The crop image view that cropping of image was complete.
         * @param bitmap the cropped image bitmap (null if failed)
         * @param error if error occurred during cropping will contain the error, otherwise null.
         */
        fun onGetCroppedImageComplete(view: CropImageView?, bitmap: Bitmap?, error: Exception?)
    }
    //endregion
    //region: Inner class: OnSaveCroppedImageCompleteListener
    /**
     * Interface definition for a callback to be invoked when image async cropping is complete.
     *
     */
    @Deprecated(
        """use {@link #setOnCropImageCompleteListener(OnCropImageCompleteListener)} and {@link
     * OnCropImageCompleteListener}."""
    )
    interface OnSaveCroppedImageCompleteListener {
        /**
         * Called when a crop image view has completed cropping image.<br></br>
         * If cropping failed error parameter will contain the error.
         *
         * @param view The crop image view that cropping of image was complete.
         * @param uri the cropped image uri (null if failed)
         * @param error if error occurred during cropping will contain the error, otherwise null.
         */
        fun onSaveCroppedImageComplete(view: CropImageView?, uri: Uri?, error: Exception?)
    }
    //endregion
    //region: Inner class: ActivityResult
    /**
     * Result data of crop image.
     */
    open class CropResult internal constructor(
        /**
         * The cropped image bitmap result.<br></br>
         * Null if save cropped image was executed, no output requested or failure.
         */
        val bitmap: Bitmap?,
        /**
         * The Android uri of the saved cropped image result.<br></br>
         * Null if get cropped image was executed, no output requested or failure.
         */
        var uri: Uri?,
        error: Exception?,
        cropPoints: FloatArray?,
        cropRect: Rect?,
        rotation: Int,
        sampleSize: Int
    ) {
        /**
         * The cropped image bitmap result.<br></br>
         * Null if save cropped image was executed, no output requested or failure.
         */
        /**
         * The Android uri of the saved cropped image result
         * Null if get cropped image was executed, no output requested or failure.
         */
        /**
         * The error that failed the loading/cropping (null if successful)
         */
        /**
         * The error that failed the loading/cropping (null if successful)
         */
        val error: Exception?
        /**
         * The 4 points of the cropping window in the source image
         */
        /**
         * The 4 points of the cropping window in the source image
         */
        lateinit var cropPoints: FloatArray
        /**
         * The rectangle of the cropping window in the source image
         */
        /**
         * The rectangle of the cropping window in the source image
         */
        val cropRect: Rect?
        /**
         * The final rotation of the cropped image relative to source
         */
        /**
         * The final rotation of the cropped image relative to source
         */
        val rotation: Int
        /**
         * sample size used creating the crop bitmap to lower its size
         */
        /**
         * sample size used creating the crop bitmap to lower its size
         */
        val sampleSize: Int

        /**
         * Is the result is success or error.
         */
        val isSuccessful: Boolean
            get() = error == null

        init {
            uri = uri
            this.error = error
            if (cropPoints != null) {
                this.cropPoints = cropPoints
            }
            this.cropRect = cropRect
            this.rotation = rotation
            this.sampleSize = sampleSize
        }
    } //endregion

    companion object {
        /**
         * Determines the specs for the onMeasure function. Calculates the width or height
         * depending on the mode.
         *
         * @param measureSpecMode The mode of the measured width or height.
         * @param measureSpecSize The size of the measured width or height.
         * @param desiredSize The desired size of the measured width or height.
         * @return The final size of the width or height.
         */
        private fun getOnMeasureSpec(
            measureSpecMode: Int,
            measureSpecSize: Int,
            desiredSize: Int
        ): Int {

            // Measure Width
            val spec: Int
            spec = if (measureSpecMode == MeasureSpec.EXACTLY) {
                // Must be this size
                measureSpecSize
            } else if (measureSpecMode == MeasureSpec.AT_MOST) {
                // Can't be bigger than...; match_parent value
                Math.min(desiredSize, measureSpecSize)
            } else {
                // Be whatever you want; wrap_content
                desiredSize
            }
            return spec
        }
    }

    //endregion
    init {
        var options: CropImageOptions? = null
        val intent = if (context is Activity) context.intent else null
        if (intent != null) {
            options = intent.getParcelableExtra(CropImage.CROP_IMAGE_EXTRA_OPTIONS)
        }
        if (options == null) {
            options = CropImageOptions()
            if (attrs != null) {
                val ta = context.obtainStyledAttributes(attrs, R.styleable.CropImageView, 0, 0)
                try {
                    options.fixAspectRatio = ta.getBoolean(
                        R.styleable.CropImageView_cropFixAspectRatio,
                        options.fixAspectRatio
                    )
                    options.aspectRatioX = ta.getInteger(
                        R.styleable.CropImageView_cropAspectRatioX,
                        options.aspectRatioX
                    )
                    options.aspectRatioY = ta.getInteger(
                        R.styleable.CropImageView_cropAspectRatioY,
                        options.aspectRatioY
                    )
                    options.scaleType = ScaleType.values()[ta.getInt(
                        R.styleable.CropImageView_cropScaleType, options.scaleType.ordinal
                    )]
                    options.autoZoomEnabled = ta.getBoolean(
                        R.styleable.CropImageView_cropAutoZoomEnabled,
                        options.autoZoomEnabled
                    )
                    options.multiTouchEnabled = ta.getBoolean(
                        R.styleable.CropImageView_cropMultiTouchEnabled,
                        options.multiTouchEnabled
                    )
                    options.maxZoom =
                        ta.getInteger(R.styleable.CropImageView_cropMaxZoom, options.maxZoom)
                    options.cropShape = CropShape.values()[ta.getInt(
                        R.styleable.CropImageView_cropShape, options.cropShape.ordinal
                    )]
                    options.guidelines = Guidelines.values()[ta.getInt(
                        R.styleable.CropImageView_cropGuidelines, options.guidelines.ordinal
                    )]
                    options.snapRadius = ta.getDimension(
                        R.styleable.CropImageView_cropSnapRadius,
                        options.snapRadius
                    )
                    options.touchRadius = ta.getDimension(
                        R.styleable.CropImageView_cropTouchRadius,
                        options.touchRadius
                    )
                    options.initialCropWindowPaddingRatio = ta.getFloat(
                        R.styleable.CropImageView_cropInitialCropWindowPaddingRatio,
                        options.initialCropWindowPaddingRatio
                    )
                    options.borderLineThickness = ta.getDimension(
                        R.styleable.CropImageView_cropBorderLineThickness,
                        options.borderLineThickness
                    )
                    options.borderLineColor = ta.getInteger(
                        R.styleable.CropImageView_cropBorderLineColor,
                        options.borderLineColor
                    )
                    options.borderCornerThickness = ta.getDimension(
                        R.styleable.CropImageView_cropBorderCornerThickness,
                        options.borderCornerThickness
                    )
                    options.borderCornerOffset = ta.getDimension(
                        R.styleable.CropImageView_cropBorderCornerOffset,
                        options.borderCornerOffset
                    )
                    options.borderCornerLength = ta.getDimension(
                        R.styleable.CropImageView_cropBorderCornerLength,
                        options.borderCornerLength
                    )
                    options.borderCornerColor = ta.getInteger(
                        R.styleable.CropImageView_cropBorderCornerColor,
                        options.borderCornerColor
                    )
                    options.guidelinesThickness = ta.getDimension(
                        R.styleable.CropImageView_cropGuidelinesThickness,
                        options.guidelinesThickness
                    )
                    options.guidelinesColor = ta.getInteger(
                        R.styleable.CropImageView_cropGuidelinesColor,
                        options.guidelinesColor
                    )
                    options.backgroundColor = ta.getInteger(
                        R.styleable.CropImageView_cropBackgroundColor,
                        options.backgroundColor
                    )
                    options.showCropOverlay = ta.getBoolean(
                        R.styleable.CropImageView_cropShowCropOverlay,
                        mShowCropOverlay
                    )
                    options.showProgressBar = ta.getBoolean(
                        R.styleable.CropImageView_cropShowProgressBar,
                        mShowProgressBar
                    )
                    options.borderCornerThickness = ta.getDimension(
                        R.styleable.CropImageView_cropBorderCornerThickness,
                        options.borderCornerThickness
                    )
                    options.minCropWindowWidth = ta.getDimension(
                        R.styleable.CropImageView_cropMinCropWindowWidth,
                        options.minCropWindowWidth.toFloat()
                    )
                        .toInt()
                    options.minCropWindowHeight = ta.getDimension(
                        R.styleable.CropImageView_cropMinCropWindowHeight,
                        options.minCropWindowHeight.toFloat()
                    )
                        .toInt()
                    options.minCropResultWidth =
                        ta.getFloat(
                            R.styleable.CropImageView_cropMinCropResultWidthPX,
                            options.minCropResultWidth.toFloat()
                        )
                            .toInt()
                    options.minCropResultHeight = ta.getFloat(
                        R.styleable.CropImageView_cropMinCropResultHeightPX,
                        options.minCropResultHeight.toFloat()
                    )
                        .toInt()
                    options.maxCropResultWidth =
                        ta.getFloat(
                            R.styleable.CropImageView_cropMaxCropResultWidthPX,
                            options.maxCropResultWidth.toFloat()
                        )
                            .toInt()
                    options.maxCropResultHeight = ta.getFloat(
                        R.styleable.CropImageView_cropMaxCropResultHeightPX,
                        options.maxCropResultHeight.toFloat()
                    )
                        .toInt()

                    // if aspect ratio is set then set fixed to true
                    if (ta.hasValue(R.styleable.CropImageView_cropAspectRatioX) &&
                        ta.hasValue(R.styleable.CropImageView_cropAspectRatioX) &&
                        !ta.hasValue(R.styleable.CropImageView_cropFixAspectRatio)
                    ) {
                        options.fixAspectRatio = true
                    }
                } finally {
                    ta.recycle()
                }
            }
        }
        options.validate()
        mScaleType = options.scaleType
        mAutoZoomEnabled = options.autoZoomEnabled
        mMaxZoom = options.maxZoom
        mShowCropOverlay = options.showCropOverlay
        mShowProgressBar = options.showProgressBar
        val inflater = LayoutInflater.from(context)
        val v = inflater.inflate(R.layout.crop_image_view, this, true)
        mImageView = v.findViewById<View>(R.id.ImageView_image) as ImageView
        mImageView.scaleType = ImageView.ScaleType.MATRIX
        mCropOverlayView = v.findViewById<View>(R.id.CropOverlayView) as CropOverlayView
        mCropOverlayView!!.setCropWindowChangeListener(object :
            CropOverlayView.CropWindowChangeListener {
            override fun onCropWindowChanged(inProgress: Boolean) {
                handleCropWindowChanged(inProgress, true)
            }
        })
        mCropOverlayView.setInitialAttributeValues(options)
        mProgressBar = v.findViewById<View>(R.id.CropProgressBar) as ProgressBar
        setProgressBarVisibility()
    }
}