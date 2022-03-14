package com.mehramoon.imagepickerwithcropping.crop

import android.content.res.Resources
import android.graphics.Bitmap.CompressFormat
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.util.TypedValue


class CropImageOptions : Parcelable {

    var cropShape: CropImageView.CropShape
    var snapRadius: Float
    var touchRadius: Float
    var guidelines: CropImageView.Guidelines
    var scaleType: CropImageView.ScaleType
    var showCropOverlay: Boolean
    var showProgressBar: Boolean
    var autoZoomEnabled: Boolean
    var multiTouchEnabled: Boolean
    var maxZoom: Int
    var initialCropWindowPaddingRatio: Float
    var fixAspectRatio: Boolean
    var aspectRatioX: Int
    var aspectRatioY: Int
    var borderLineThickness: Float
    var borderLineColor: Int
    var borderCornerThickness: Float
    var borderCornerOffset: Float
    var borderCornerLength: Float
    var borderCornerColor: Int
    var guidelinesThickness: Float
    var guidelinesColor: Int
    var backgroundColor: Int
    var minCropWindowWidth: Int
    var minCropWindowHeight: Int
    var minCropResultWidth: Int
    var minCropResultHeight: Int
    var maxCropResultWidth: Int
    var maxCropResultHeight: Int
    var activityTitle: String?
    var activityMenuIconColor: Int
    var outputUri: Uri?
    var outputCompressFormat: CompressFormat
    var outputCompressQuality: Int
    var outputRequestWidth: Int
    var outputRequestHeight: Int
    var outputRequestSizeOptions: CropImageView.RequestSizeOptions
    var noOutputImage: Boolean
    var initialCropWindowRectangle: Rect?
    var initialRotation: Int
    var allowRotation: Boolean
    var allowCounterRotation: Boolean
    var rotationDegrees: Int


    constructor() {
        val dm = Resources.getSystem().displayMetrics
        cropShape = CropImageView.CropShape.RECTANGLE
        snapRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3f, dm)
        touchRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24f, dm)
        guidelines = CropImageView.Guidelines.ON_TOUCH
        scaleType = CropImageView.ScaleType.FIT_CENTER
        showCropOverlay = true
        showProgressBar = true
        autoZoomEnabled = true
        multiTouchEnabled = false
        maxZoom = 4
        initialCropWindowPaddingRatio = 0.1f
        fixAspectRatio = false
        aspectRatioX = 1
        aspectRatioY = 1
        borderLineThickness = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3f, dm)
        borderLineColor = Color.argb(170, 255, 255, 255)
        borderCornerThickness = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, dm)
        borderCornerOffset = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5f, dm)
        borderCornerLength = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14f, dm)
        borderCornerColor = Color.WHITE
        guidelinesThickness = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, dm)
        guidelinesColor = Color.argb(170, 255, 255, 255)
        backgroundColor = Color.argb(119, 0, 0, 0)
        minCropWindowWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 42f, dm).toInt()
        minCropWindowHeight =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 42f, dm).toInt()
        minCropResultWidth = 40
        minCropResultHeight = 40
        maxCropResultWidth = 99999
        maxCropResultHeight = 99999
        activityTitle = ""
        activityMenuIconColor = 0
        outputUri = Uri.EMPTY
        outputCompressFormat = CompressFormat.JPEG
        outputCompressQuality = 90
        outputRequestWidth = 0
        outputRequestHeight = 0
        outputRequestSizeOptions = CropImageView.RequestSizeOptions.NONE
        noOutputImage = false
        initialCropWindowRectangle = null
        initialRotation = -1
        allowRotation = true
        allowCounterRotation = false
        rotationDegrees = 90
    }

    /**
     * Create object from parcel.
     */
    protected constructor(`in`: Parcel) {
        cropShape = CropImageView.CropShape.values()[`in`.readInt()]
        snapRadius = `in`.readFloat()
        touchRadius = `in`.readFloat()
        guidelines = CropImageView.Guidelines.values()[`in`.readInt()]
        scaleType = CropImageView.ScaleType.values()[`in`.readInt()]
        showCropOverlay = `in`.readByte().toInt() != 0
        showProgressBar = `in`.readByte().toInt() != 0
        autoZoomEnabled = `in`.readByte().toInt() != 0
        multiTouchEnabled = `in`.readByte().toInt() != 0
        maxZoom = `in`.readInt()
        initialCropWindowPaddingRatio = `in`.readFloat()
        fixAspectRatio = `in`.readByte().toInt() != 0
        aspectRatioX = `in`.readInt()
        aspectRatioY = `in`.readInt()
        borderLineThickness = `in`.readFloat()
        borderLineColor = `in`.readInt()
        borderCornerThickness = `in`.readFloat()
        borderCornerOffset = `in`.readFloat()
        borderCornerLength = `in`.readFloat()
        borderCornerColor = `in`.readInt()
        guidelinesThickness = `in`.readFloat()
        guidelinesColor = `in`.readInt()
        backgroundColor = `in`.readInt()
        minCropWindowWidth = `in`.readInt()
        minCropWindowHeight = `in`.readInt()
        minCropResultWidth = `in`.readInt()
        minCropResultHeight = `in`.readInt()
        maxCropResultWidth = `in`.readInt()
        maxCropResultHeight = `in`.readInt()
        activityTitle = `in`.readString()
        activityMenuIconColor = `in`.readInt()
        outputUri = `in`.readParcelable(Uri::class.java.classLoader)
        outputCompressFormat = CompressFormat.valueOf(`in`.readString()!!)
        outputCompressQuality = `in`.readInt()
        outputRequestWidth = `in`.readInt()
        outputRequestHeight = `in`.readInt()
        outputRequestSizeOptions = CropImageView.RequestSizeOptions.values()[`in`.readInt()]
        noOutputImage = `in`.readByte().toInt() != 0
        initialCropWindowRectangle = `in`.readParcelable(Rect::class.java.classLoader)
        initialRotation = `in`.readInt()
        allowRotation = `in`.readByte().toInt() != 0
        allowCounterRotation = `in`.readByte().toInt() != 0
        rotationDegrees = `in`.readInt()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(cropShape.ordinal)
        dest.writeFloat(snapRadius)
        dest.writeFloat(touchRadius)
        dest.writeInt(guidelines.ordinal)
        dest.writeInt(scaleType.ordinal)
        dest.writeByte((if (showCropOverlay) 1 else 0).toByte())
        dest.writeByte((if (showProgressBar) 1 else 0).toByte())
        dest.writeByte((if (autoZoomEnabled) 1 else 0).toByte())
        dest.writeByte((if (multiTouchEnabled) 1 else 0).toByte())
        dest.writeInt(maxZoom)
        dest.writeFloat(initialCropWindowPaddingRatio)
        dest.writeByte((if (fixAspectRatio) 1 else 0).toByte())
        dest.writeInt(aspectRatioX)
        dest.writeInt(aspectRatioY)
        dest.writeFloat(borderLineThickness)
        dest.writeInt(borderLineColor)
        dest.writeFloat(borderCornerThickness)
        dest.writeFloat(borderCornerOffset)
        dest.writeFloat(borderCornerLength)
        dest.writeInt(borderCornerColor)
        dest.writeFloat(guidelinesThickness)
        dest.writeInt(guidelinesColor)
        dest.writeInt(backgroundColor)
        dest.writeInt(minCropWindowWidth)
        dest.writeInt(minCropWindowHeight)
        dest.writeInt(minCropResultWidth)
        dest.writeInt(minCropResultHeight)
        dest.writeInt(maxCropResultWidth)
        dest.writeInt(maxCropResultHeight)
        dest.writeString(activityTitle)
        dest.writeInt(activityMenuIconColor)
        dest.writeParcelable(outputUri, flags)
        dest.writeString(outputCompressFormat.name)
        dest.writeInt(outputCompressQuality)
        dest.writeInt(outputRequestWidth)
        dest.writeInt(outputRequestHeight)
        dest.writeInt(outputRequestSizeOptions.ordinal)
        dest.writeInt(if (noOutputImage) 1 else 0)
        dest.writeParcelable(initialCropWindowRectangle, flags)
        dest.writeInt(initialRotation)
        dest.writeByte((if (allowRotation) 1 else 0).toByte())
        dest.writeByte((if (allowCounterRotation) 1 else 0).toByte())
        dest.writeInt(rotationDegrees)
    }

    override fun describeContents(): Int {
        return 0
    }

    /**
     * Validate all the options are withing valid range.
     *
     * @throws IllegalArgumentException if any of the options is not valid
     */
    fun validate() {
        require(maxZoom >= 0) { "Cannot set max zoom to a number < 1" }
        require(touchRadius >= 0) { "Cannot set touch radius value to a number <= 0 " }
        require(!(initialCropWindowPaddingRatio < 0 || initialCropWindowPaddingRatio >= 0.5)) { "Cannot set initial crop window padding value to a number < 0 or >= 0.5" }
        require(aspectRatioX > 0) { "Cannot set aspect ratio value to a number less than or equal to 0." }
        require(aspectRatioY > 0) { "Cannot set aspect ratio value to a number less than or equal to 0." }
        require(borderLineThickness >= 0) { "Cannot set line thickness value to a number less than 0." }
        require(borderCornerThickness >= 0) { "Cannot set corner thickness value to a number less than 0." }
        require(guidelinesThickness >= 0) { "Cannot set guidelines thickness value to a number less than 0." }
        require(minCropWindowHeight >= 0) { "Cannot set min crop window height value to a number < 0 " }
        require(minCropResultWidth >= 0) { "Cannot set min crop result width value to a number < 0 " }
        require(minCropResultHeight >= 0) { "Cannot set min crop result height value to a number < 0 " }
        require(maxCropResultWidth >= minCropResultWidth) { "Cannot set max crop result width to smaller value than min crop result width" }
        require(maxCropResultHeight >= minCropResultHeight) { "Cannot set max crop result height to smaller value than min crop result height" }
        require(outputRequestWidth >= 0) { "Cannot set request width value to a number < 0 " }
        require(outputRequestHeight >= 0) { "Cannot set request height value to a number < 0 " }
        require(!(rotationDegrees < 0 || rotationDegrees > 360)) { "Cannot set rotation degrees value to a number < 0 or > 360" }
    }


    companion object CREATOR : Parcelable.Creator<CropImageOptions> {
        override fun createFromParcel(parcel: Parcel): CropImageOptions {
            return CropImageOptions(parcel)
        }

        override fun newArray(size: Int): Array<CropImageOptions?> {
            return arrayOfNulls(size)
        }
    }
}