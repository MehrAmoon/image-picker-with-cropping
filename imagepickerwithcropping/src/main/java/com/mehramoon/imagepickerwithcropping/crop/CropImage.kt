package com.mehramoon.imagepickerwithcropping.crop

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.Bitmap.CompressFormat
import android.net.Uri
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import com.mehramoon.imagepickerwithcropping.R
import com.mehramoon.imagepickerwithcropping.Utils
import io.reactivex.annotations.NonNull
import io.reactivex.annotations.Nullable
import java.io.File


object CropImage {
    const val CROP_IMAGE_EXTRA_SOURCE = "CROP_IMAGE_EXTRA_SOURCE"
    const val CROP_IMAGE_EXTRA_OPTIONS = "CROP_IMAGE_EXTRA_OPTIONS"
    const val CROP_IMAGE_EXTRA_RESULT = "CROP_IMAGE_EXTRA_RESULT"
    const val PICK_IMAGE_CHOOSER_REQUEST_CODE = 200
    const val PICK_IMAGE_PERMISSIONS_REQUEST_CODE = 201
    const val CAMERA_CAPTURE_PERMISSIONS_REQUEST_CODE = 2011
    const val CROP_IMAGE_ACTIVITY_REQUEST_CODE = 203
    const val CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE = 204

    fun toOvalBitmap(@NonNull bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val color = -0xbdbdbe
        val paint = Paint()
        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = color
        val rect = RectF(0F, 0F, width.toFloat(), height.toFloat())
        canvas.drawOval(rect, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        bitmap.recycle()
        return output
    }


    fun startPickImageActivity(@NonNull activity: Activity) {
        activity.startActivityForResult(
            getPickImageChooserIntent(activity),
            PICK_IMAGE_CHOOSER_REQUEST_CODE
        )
    }

    fun startPickImageActivity(@NonNull fragment: Fragment) {
        fragment.startActivityForResult(
            fragment.activity?.let { getPickImageChooserIntent(it) },
            PICK_IMAGE_CHOOSER_REQUEST_CODE
        )
    }


    private fun getPickImageChooserIntent(@NonNull context: Context): Intent {
        return getPickImageChooserIntent(
            context, context.getString(
                R.string.pick_image_intent_chooser_title
            ), false
        )
    }


    fun getPickImageChooserIntent(
        @NonNull context: Context,
        title: CharSequence?,
        includeDocuments: Boolean
    ): Intent {
        val allIntents: MutableList<Intent> = ArrayList()
        val packageManager = context.packageManager

        if (!isExplicitCameraPermissionRequired(context)) {
            allIntents.addAll(getCameraIntents(context, packageManager))
        }
        var galleryIntents =
            getGalleryIntents(packageManager, Intent.ACTION_GET_CONTENT, includeDocuments)
        if (galleryIntents.isEmpty()) {
            galleryIntents = getGalleryIntents(packageManager, Intent.ACTION_PICK, includeDocuments)
        }
        allIntents.addAll(galleryIntents)
        val target: Intent
        if (allIntents.isEmpty()) {
            target = Intent()
        } else {
            target = allIntents[allIntents.size - 1]
            allIntents.removeAt(allIntents.size - 1)
        }

        val chooserIntent = Intent.createChooser(target, title)

        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, allIntents.toTypedArray())
        return chooserIntent
    }

    fun getCameraIntent(@NonNull context: Context, outputFileUri: Uri?): Intent {
        var outputFileUri = outputFileUri
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (outputFileUri == null) {
            outputFileUri = getCaptureImageOutputUri(context)
        }
        outputFileUri?.let {
            intent.putExtra(MediaStore.EXTRA_OUTPUT,  Utils.getIntentUri(context, it) )
        }

        return intent
    }


    private fun getCameraIntents(
        @NonNull context: Context,
        @NonNull packageManager: PackageManager
    ): List<Intent> {

        val allIntents: MutableList<Intent> = ArrayList()
        val outputFileUri = getCaptureImageOutputUri(context)
        val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val listCam = packageManager.queryIntentActivities(captureIntent, 0)
        for (res in listCam) {
            val intent = Intent(captureIntent)
            intent.component = ComponentName(res.activityInfo.packageName, res.activityInfo.name)
            intent.setPackage(res.activityInfo.packageName)
            if (outputFileUri != null) {
               intent.putExtra(MediaStore.EXTRA_OUTPUT, Utils.getIntentUri(context, outputFileUri))
            }
            allIntents.add(intent)
        }
        return allIntents
    }


    fun getGalleryIntents(
        @NonNull packageManager: PackageManager,
        action: String,
        includeDocuments: Boolean
    ): List<Intent> {
        val intents: MutableList<Intent> = ArrayList()
        val galleryIntent = if (action === Intent.ACTION_GET_CONTENT) Intent(action) else Intent(
            action,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        galleryIntent.type = "image/*"
        val listGallery = packageManager.queryIntentActivities(galleryIntent, 0)
        for (res in listGallery) {
            val intent = Intent(galleryIntent)
            intent.component = ComponentName(res.activityInfo.packageName, res.activityInfo.name)
            intent.setPackage(res.activityInfo.packageName)
            intents.add(intent)
        }

        // remove documents intent
        if (!includeDocuments) {
            for (intent in intents) {
                if (intent.component!!.className == "com.android.documentsui.DocumentsActivity") {
                    intents.remove(intent)
                    break
                }
            }
        }
        return intents
    }


    fun isExplicitCameraPermissionRequired(@NonNull context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            hasPermissionInManifest(context, "android.permission.CAMERA") &&
                    context.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
        } else false
    }


    fun hasPermissionInManifest(
        @NonNull context: Context,
        @NonNull permissionName: String?
    ): Boolean {
        val packageName = context.packageName
        try {
            val packageInfo =
                context.packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            val declaredPermissions = packageInfo.requestedPermissions
            if (declaredPermissions != null && declaredPermissions.isNotEmpty()) {
                for (p in declaredPermissions) {
                    if (p.equals(permissionName, ignoreCase = true)) {
                        return true
                    }
                }
            }
        } catch (e: PackageManager.NameNotFoundException) {
        }
        return false
    }


    private fun getCaptureImageOutputUri(@NonNull context: Context): Uri? {
        var outputFileUri: Uri? = null
        val getImage = context.externalCacheDir
        if (getImage != null) {
            outputFileUri = Uri.fromFile(File(getImage.path, "pickImageResult.jpeg"))
        }
        return outputFileUri
    }


    fun getPickImageResultUri(@NonNull context: Context, @Nullable data: Intent?): Uri? {
        var isCamera = true
        if (data != null && data.data != null ) {
            val action = data.action
            isCamera = action != null && action == MediaStore.ACTION_IMAGE_CAPTURE
        }
        return if (isCamera || data!!.data == null) getCaptureImageOutputUri(context)
        else
            data.data
    }


    fun isReadExternalStoragePermissionsRequired(
        @NonNull context: Context,
        @NonNull uri: Uri?
    ): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context.checkSelfPermission(
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) != PackageManager.PERMISSION_GRANTED &&
                isUriRequiresPermissions(context, uri)
    }


    private fun isUriRequiresPermissions(@NonNull context: Context, @NonNull uri: Uri?): Boolean {
        return try {
            val resolver = context.contentResolver
            val stream = resolver.openInputStream(uri!!)
            stream!!.close()
            false
        } catch (e: Exception) {
            true
        }
    }


    fun activity(@Nullable uri: Uri): ActivityBuilder {
        return ActivityBuilder(uri)
    }


    fun getActivityResult(@Nullable data: Intent?): ActivityResult? {
        return if (data != null) data.getParcelableExtra<Parcelable>(CROP_IMAGE_EXTRA_RESULT) as ActivityResult? else null
    }

    class ActivityBuilder(@field:Nullable @param:Nullable private val mSource: Uri) {

        private val mOptions: CropImageOptions = CropImageOptions()

        private fun getIntent(@NonNull context: Context?): Intent {
            return getIntent(context, CropImageActivity::class.java)
        }


        fun getIntent(@NonNull context: Context?, @Nullable cls: Class<*>?): Intent {
            mOptions.validate()
            val intent = Intent()
            intent.setClass(context!!, cls!!)
            intent.putExtra(CROP_IMAGE_EXTRA_SOURCE, mSource)
            intent.putExtra(CROP_IMAGE_EXTRA_OPTIONS, mOptions)
            return intent
        }

        fun start(@NonNull activity: Activity) {
            mOptions.validate()
            activity.startActivityForResult(getIntent(activity), CROP_IMAGE_ACTIVITY_REQUEST_CODE)
        }

        fun start(@NonNull activity: Activity, @Nullable cls: Class<*>?) {
            mOptions.validate()
            activity.startActivityForResult(
                getIntent(activity, cls),
                CROP_IMAGE_ACTIVITY_REQUEST_CODE
            )
        }

        fun start(@NonNull context: Context?, @NonNull fragment: Fragment) {
            fragment.startActivityForResult(getIntent(context), CROP_IMAGE_ACTIVITY_REQUEST_CODE)
        }

        fun start(
            @NonNull context: Context?,
            @NonNull fragment: Fragment,
            @Nullable cls: Class<*>?
        ) {
            fragment.startActivityForResult(
                getIntent(context, cls),
                CROP_IMAGE_ACTIVITY_REQUEST_CODE
            )
        }

        /**
         * *Default: RECTANGLE*
         */
        fun setCropShape(@NonNull cropShape: CropImageView.CropShape?): ActivityBuilder {
            if (cropShape != null) {
                mOptions.cropShape = cropShape
            }
            return this
        }

        /**
         * *Default: 3dp*
         */
        fun setSnapRadius(snapRadius: Float): ActivityBuilder {
            mOptions.snapRadius = snapRadius
            return this
        }

        /**
         * *Default: 48dp*
         */
        fun setTouchRadius(touchRadius: Float): ActivityBuilder {
            mOptions.touchRadius = touchRadius
            return this
        }

        /**
         * *Default: ON_TOUCH*
         */
        fun setGuidelines(@NonNull guidelines: CropImageView.Guidelines?): ActivityBuilder {
            if (guidelines != null) {
                mOptions.guidelines = guidelines
            }
            return this
        }

        /**
         * *Default: FIT_CENTER*
         */
        fun setScaleType(@NonNull scaleType: CropImageView.ScaleType?): ActivityBuilder {
            if (scaleType != null) {
                mOptions.scaleType = scaleType
            }
            return this
        }

        /**
         * *default: true, may disable for animation or frame transition.*
         */
        fun setShowCropOverlay(showCropOverlay: Boolean): ActivityBuilder {
            mOptions.showCropOverlay = showCropOverlay
            return this
        }

        /**
         * default: true.
         */
        fun setAutoZoomEnabled(autoZoomEnabled: Boolean): ActivityBuilder {
            mOptions.autoZoomEnabled = autoZoomEnabled
            return this
        }

        /**
         * default: true.
         */
        fun setMultiTouchEnabled(multiTouchEnabled: Boolean): ActivityBuilder {
            mOptions.multiTouchEnabled = multiTouchEnabled
            return this
        }

        /**
         * *Default: 4*
         */
        fun setMaxZoom(maxZoom: Int): ActivityBuilder {
            mOptions.maxZoom = maxZoom
            return this
        }

        /**
         * *Default: 0.1*
         */
        fun setInitialCropWindowPaddingRatio(initialCropWindowPaddingRatio: Float): ActivityBuilder {
            mOptions.initialCropWindowPaddingRatio = initialCropWindowPaddingRatio
            return this
        }

        /**
         * *Default: false*
         */
        fun setFixAspectRatio(fixAspectRatio: Boolean): ActivityBuilder {
            mOptions.fixAspectRatio = fixAspectRatio
            return this
        }

        /**
         * *Default: 1/1*
         */
        fun setAspectRatio(aspectRatioX: Int, aspectRatioY: Int): ActivityBuilder {
            mOptions.aspectRatioX = aspectRatioX
            mOptions.aspectRatioY = aspectRatioY
            mOptions.fixAspectRatio = true
            return this
        }

        /**
         * *Default: 3dp*
         */
        fun setBorderLineThickness(borderLineThickness: Float): ActivityBuilder {
            mOptions.borderLineThickness = borderLineThickness
            return this
        }

        /**
         * *Default: Color.argb(170, 255, 255, 255)*
         */
        fun setBorderLineColor(borderLineColor: Int): ActivityBuilder {
            mOptions.borderLineColor = borderLineColor
            return this
        }

        /**
         * *Default: 2dp*
         */
        fun setBorderCornerThickness(borderCornerThickness: Float): ActivityBuilder {
            mOptions.borderCornerThickness = borderCornerThickness
            return this
        }

        /**
         * *Default: 5dp*
         */
        fun setBorderCornerOffset(borderCornerOffset: Float): ActivityBuilder {
            mOptions.borderCornerOffset = borderCornerOffset
            return this
        }

        /**
         * *Default: 14dp*
         */
        fun setBorderCornerLength(borderCornerLength: Float): ActivityBuilder {
            mOptions.borderCornerLength = borderCornerLength
            return this
        }

        /**
         * *Default: WHITE*
         */
        fun setBorderCornerColor(borderCornerColor: Int): ActivityBuilder {
            mOptions.borderCornerColor = borderCornerColor
            return this
        }

        /**
         * *Default: 1dp*
         */
        fun setGuidelinesThickness(guidelinesThickness: Float): ActivityBuilder {
            mOptions.guidelinesThickness = guidelinesThickness
            return this
        }

        /**
         * *Default: Color.argb(170, 255, 255, 255)*
         */
        fun setGuidelinesColor(guidelinesColor: Int): ActivityBuilder {
            mOptions.guidelinesColor = guidelinesColor
            return this
        }

        /**
         * *Default: Color.argb(119, 0, 0, 0)*
         */
        fun setBackgroundColor(backgroundColor: Int): ActivityBuilder {
            mOptions.backgroundColor = backgroundColor
            return this
        }

        /**
         * *Default: 42dp, 42dp*
         */
        fun setMinCropWindowSize(
            minCropWindowWidth: Int,
            minCropWindowHeight: Int
        ): ActivityBuilder {
            mOptions.minCropWindowWidth = minCropWindowWidth
            mOptions.minCropWindowHeight = minCropWindowHeight
            return this
        }

        /**
         * *Default: 40px, 40px*
         */
        fun setMinCropResultSize(
            minCropResultWidth: Int,
            minCropResultHeight: Int
        ): ActivityBuilder {
            mOptions.minCropResultWidth = minCropResultWidth
            mOptions.minCropResultHeight = minCropResultHeight
            return this
        }

        /**
         * *Default: 99999, 99999*
         */
        fun setMaxCropResultSize(
            maxCropResultWidth: Int,
            maxCropResultHeight: Int
        ): ActivityBuilder {
            mOptions.maxCropResultWidth = maxCropResultWidth
            mOptions.maxCropResultHeight = maxCropResultHeight
            return this
        }

        /**
         * *Default: ""*
         */
        fun setActivityTitle(activityTitle: String?): ActivityBuilder {
            mOptions.activityTitle = activityTitle
            return this
        }

        /**
         * *Default: NONE*
         */
        fun setActivityMenuIconColor(activityMenuIconColor: Int): ActivityBuilder {
            mOptions.activityMenuIconColor = activityMenuIconColor
            return this
        }

        /**
         * *Default: NONE, will create a temp file*
         */
        fun setOutputUri(outputUri: Uri?): ActivityBuilder {
            mOptions.outputUri = outputUri
            return this
        }

        /**
         * *Default: JPEG*
         */
        fun setOutputCompressFormat(outputCompressFormat: CompressFormat?): ActivityBuilder {
            if (outputCompressFormat != null) {
                mOptions.outputCompressFormat = outputCompressFormat
            }
            return this
        }

        /**
         * (0 - 100)
         * *Default: 90*
         */
        fun setOutputCompressQuality(outputCompressQuality: Int): ActivityBuilder {
            mOptions.outputCompressQuality = outputCompressQuality
            return this
        }

        /**
         * *Default: 0, 0 - not set, will not resize*
         */
        fun setRequestedSize(reqWidth: Int, reqHeight: Int): ActivityBuilder {
            return setRequestedSize(
                reqWidth,
                reqHeight,
                CropImageView.RequestSizeOptions.RESIZE_INSIDE
            )
        }

        /**
         * *Default: 0, 0 - not set, will not resize*
         */
        private fun setRequestedSize(
            reqWidth: Int,
            reqHeight: Int,
            options: CropImageView.RequestSizeOptions?
        ): ActivityBuilder {
            mOptions.outputRequestWidth = reqWidth
            mOptions.outputRequestHeight = reqHeight
            if (options != null) {
                mOptions.outputRequestSizeOptions = options
            }
            return this
        }

        /**
         * *Default: false*
         */
        fun setNoOutputImage(noOutputImage: Boolean): ActivityBuilder {
            mOptions.noOutputImage = noOutputImage
            return this
        }

        /**
         * *Default: NONE - will initialize using initial crop window padding ratio*
         */
        fun setInitialCropWindowRectangle(initialCropWindowRectangle: Rect?): ActivityBuilder {
            mOptions.initialCropWindowRectangle = initialCropWindowRectangle
            return this
        }

        /**
         * *Default: NONE - will read image exif data*
         */
        fun setInitialRotation(initialRotation: Int): ActivityBuilder {
            mOptions.initialRotation = initialRotation
            return this
        }

        /**
         * *Default: true*
         */
        fun setAllowRotation(allowRotation: Boolean): ActivityBuilder {
            mOptions.allowRotation = allowRotation
            return this
        }

        /**
         * *Default: false*
         */
        fun setAllowCounterRotation(allowCounterRotation: Boolean): ActivityBuilder {
            mOptions.allowCounterRotation = allowCounterRotation
            return this
        }

        /**
         * rotate clockwise or counter-clockwise (0-360)
         * *Default: 90*
         */
        fun setRotationDegrees(rotationDegrees: Int): ActivityBuilder {
            mOptions.rotationDegrees = rotationDegrees
            return this
        }

    }

    open class ActivityResult : CropImageView.CropResult, Parcelable {
        constructor(
            bitmap: Bitmap?,
            uri: Uri?,
            error: Exception?,
            cropPoints: FloatArray,
            cropRect: Rect?,
            rotation: Int,
            sampleSize: Int
        ) : super(bitmap, uri, error, cropPoints, cropRect, rotation, sampleSize) {
        }

        protected constructor(`in`: Parcel) : super(
            null,
            `in`.readParcelable<Parcelable>(Uri::class.java.classLoader) as Uri?,
            `in`.readSerializable() as Exception?,
            `in`.createFloatArray(),
            `in`.readParcelable<Parcelable>(Rect::class.java.classLoader) as Rect?,
            `in`.readInt(), `in`.readInt()
        ) {
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeParcelable(uri, flags)
            dest.writeSerializable(error)
            dest.writeFloatArray(cropPoints)
            dest.writeParcelable(cropRect, flags)
            dest.writeInt(rotation)
            dest.writeInt(sampleSize)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<ActivityResult> {
            override fun createFromParcel(parcel: Parcel): ActivityResult {
                return ActivityResult(parcel)
            }

            override fun newArray(size: Int): Array<ActivityResult?> {
                return arrayOfNulls(size)
            }
        }
    }
}