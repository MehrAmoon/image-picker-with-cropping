package com.mehramoon.imagepickerwithcropping

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.mehramoon.imagepickerwithcropping.crop.CropImage
import com.mehramoon.imagepickerwithcropping.crop.CropImageView
import io.reactivex.annotations.NonNull
import java.io.File


open class ImagePicker {
    private var callback: Callback? = null
    private var isCropImage = true
    private var title: CharSequence? = null
    private var pickImageUri: Uri? = null
    private var cropImageUri: Uri? = null

    fun setCropImage(cropImage: Boolean) {
        isCropImage = cropImage
    }

    fun setTitle(title: CharSequence?) {
        this.title = title
    }

    fun startChooser(activity: Activity, @NonNull callback: Callback?) {
        this.callback = callback
        if (CropImage.isExplicitCameraPermissionRequired(activity)) {
            ActivityCompat.requestPermissions(
                activity, arrayOf(Manifest.permission.CAMERA),
                CropImage.CAMERA_CAPTURE_PERMISSIONS_REQUEST_CODE
            )
        } else {
            activity.startActivityForResult(
                CropImage.getPickImageChooserIntent(activity, getTitle(activity), false),
                CropImage.PICK_IMAGE_CHOOSER_REQUEST_CODE
            )
        }
    }

    fun startChooser(fragment: Fragment, @NonNull callback: Callback?) {
        this.callback = callback
        if (fragment.activity?.let { CropImage.isExplicitCameraPermissionRequired(it) } == true) {
            fragment.requestPermissions(
                arrayOf(Manifest.permission.CAMERA),
                CropImage.CAMERA_CAPTURE_PERMISSIONS_REQUEST_CODE
            )
        } else {
            fragment.startActivityForResult(
                fragment.activity?.let {
                    CropImage.getPickImageChooserIntent(
                        it,
                        getTitle(fragment.requireActivity()),
                        false
                    )
                },
                CropImage.PICK_IMAGE_CHOOSER_REQUEST_CODE
            )
        }
    }

    fun startCamera(activity: Activity, @NonNull callback: Callback?) {
        this.callback = callback
        if (CropImage.isExplicitCameraPermissionRequired(activity)) {
            ActivityCompat.requestPermissions(
                activity, arrayOf(Manifest.permission.CAMERA),
                CropImage.CAMERA_CAPTURE_PERMISSIONS_REQUEST_CODE
            )
        } else {
            activity.startActivityForResult(
                CropImage.getCameraIntent(activity, null),
                CropImage.PICK_IMAGE_CHOOSER_REQUEST_CODE
            )
        }
    }

    fun startCamera(fragment: Fragment, @NonNull callback: Callback?) {
        this.callback = callback
        if (fragment.activity?.let { CropImage.isExplicitCameraPermissionRequired(it) } == true) {
            fragment.requestPermissions(
                arrayOf(Manifest.permission.CAMERA),
                CropImage.CAMERA_CAPTURE_PERMISSIONS_REQUEST_CODE
            )
        } else {
            fragment.startActivityForResult(
                fragment.activity?.let { CropImage.getCameraIntent(it, null) },
                CropImage.PICK_IMAGE_CHOOSER_REQUEST_CODE
            )
        }
    }

    fun startGallery(activity: Activity, @NonNull callback: Callback?) {
        this.callback = callback
        activity.startActivityForResult(
            getGalleryIntent(activity, false),
            CropImage.PICK_IMAGE_CHOOSER_REQUEST_CODE
        )
    }

    fun startGallery(fragment: Fragment, @NonNull callback: Callback?) {
        this.callback = callback
        fragment.startActivityForResult(
            fragment.activity?.let { getGalleryIntent(it, false) },
            CropImage.PICK_IMAGE_CHOOSER_REQUEST_CODE
        )
    }

    protected fun getTitle(context: Context): CharSequence? {
        return if (TextUtils.isEmpty(title)) {
            context.getString(R.string.pick_image_intent_chooser_title)
        } else title
    }

    private fun getGalleryIntent(context: Context, includeDocuments: Boolean): Intent {
        val packageManager = context.packageManager
        var galleryIntents = CropImage.getGalleryIntents(packageManager, Intent.ACTION_GET_CONTENT, includeDocuments)
        if (galleryIntents.isEmpty()) {
            galleryIntents = CropImage.getGalleryIntents(packageManager, Intent.ACTION_PICK, includeDocuments)
        }
        val target: Intent?
        if (galleryIntents.isEmpty()) {
            target = Intent()
        } else {
            target = galleryIntents[galleryIntents.size - 1]
            galleryIntents.drop(galleryIntents.size - 1)
        }

        val chooserIntent = Intent.createChooser(target, getTitle(context))

        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, galleryIntents.toTypedArray())
        return chooserIntent
    }

    fun onActivityResult(activity: Activity?, requestCode: Int, resultCode: Int, data: Intent?) {
        onActivityResultInner(activity, null, requestCode, resultCode, data)
    }

    fun onActivityResult(fragment: Fragment?, requestCode: Int, resultCode: Int, data: Intent) {
        onActivityResultInner(null, fragment, requestCode, resultCode, data)
    }

    private fun onActivityResultInner(
        activity: Activity?,
        fragment: Fragment?,
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        if (resultCode == Activity.RESULT_OK) {
            val context: Context = (activity ?: fragment?.activity) as Context
            if (requestCode == CropImage.PICK_IMAGE_CHOOSER_REQUEST_CODE) {
                pickImageUri = CropImage.getPickImageResultUri(context, data)
                if (CropImage.isReadExternalStoragePermissionsRequired(context, pickImageUri)) {
                    if (activity != null) {
                        ActivityCompat.requestPermissions(
                            activity, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                            CropImage.PICK_IMAGE_PERMISSIONS_REQUEST_CODE
                        )
                    } else {
                        fragment?.requestPermissions(
                            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                            CropImage.PICK_IMAGE_PERMISSIONS_REQUEST_CODE
                        )
                    }
                } else {
                    if (activity != null) {
                        pickImageUri?.let { handlePickImage(activity, it) }
                    } else {
                        if (fragment != null) {
                            pickImageUri?.let { handlePickImage(fragment, it) }
                        }
                    }
                }
            } else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
                CropImage.getActivityResult(data)?.let { handleCropResult(context, it) }
            }
        }
    }

    fun onRequestPermissionsResult(
        activity: Activity?, requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        onRequestPermissionsResultInner(activity, null, requestCode, permissions, grantResults)
    }

    fun onRequestPermissionsResult(
        fragment: Fragment?, requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        onRequestPermissionsResultInner(null, fragment, requestCode, permissions, grantResults)
    }

    private fun onRequestPermissionsResultInner(
        activity: Activity?, fragment: Fragment?, requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == CropImage.CAMERA_CAPTURE_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (activity != null) {
                    CropImage.startPickImageActivity(activity)
                } else {
                    if (fragment != null) {
                        CropImage.startPickImageActivity(fragment)
                    }
                }
            } else {
                if (callback != null) {
                    callback!!.onPermissionDenied(requestCode, permissions, grantResults)
                }
            }
        }
        if (requestCode == CropImage.PICK_IMAGE_PERMISSIONS_REQUEST_CODE) {
            if (cropImageUri != null && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (activity != null) {
                    handlePickImage(activity, cropImageUri!!)
                } else {
                    if (fragment != null) {
                        handlePickImage(fragment, cropImageUri!!)
                    }
                }
            } else {
                if (callback != null) {
                    callback!!.onPermissionDenied(requestCode, permissions, grantResults)
                }
            }
        }
    }

    private fun handleCropResult(context: Context, result: CropImageView.CropResult) {
        if (result.error == null) {
            cropImageUri = result.uri
            if (callback != null) {
                callback!!.onCropImage(handleUri(context, cropImageUri))
            }
        } else {
            Log.e(TAG, "handleCropResult error", result.error)
        }
    }

    private fun handlePickImage(activity: Activity, imageUri: Uri) {
        handlePickImageInner(activity, null, imageUri)
    }

    private fun handlePickImage(fragment: Fragment, imageUri: Uri) {
        handlePickImageInner(null, fragment, imageUri)
    }

    private fun handlePickImageInner(activity: Activity?, fragment: Fragment?, imageUri: Uri) {
        if (callback != null) {
            val context: Context = (activity ?: fragment?.context) as Context
            callback!!.onPickImage(handleUri(context, imageUri))
        }
        if (!isCropImage) {
            return
        }
        val builder = CropImage.activity(imageUri)
        callback!!.cropConfig(builder)
        if (activity != null) {
            builder.start(activity)
        } else {
            if (fragment != null) {
                builder.start(fragment.activity, fragment)
            }
        }
    }

    private fun handleUri(context: Context, imageUri: Uri?): Uri? {
        if ("content" == imageUri!!.scheme) {
            imageUri.let { uri ->
                val realPathFromUri = Utils.getRealPathFromURI(context, uri)
                if (!TextUtils.isEmpty(realPathFromUri)) {
                    return Uri.fromFile(File(realPathFromUri))
                }
            }
        }
        return imageUri
    }

    abstract class Callback {
        abstract fun onPickImage(imageUri: Uri?)
        open fun onCropImage(imageUri: Uri?) {}
        open fun cropConfig(builder: CropImage.ActivityBuilder) {
            builder.setMultiTouchEnabled(false)
                .setCropShape(CropImageView.CropShape.OVAL)
                .setRequestedSize(640, 640)
                .setAspectRatio(5, 5)
        }

        open fun onPermissionDenied(
            requestCode: Int,
            permissions: Array<String>?,
            grantResults: IntArray?
        ) {
        }
    }

    companion object {
        private const val TAG = "ImagePicker"
    }
}