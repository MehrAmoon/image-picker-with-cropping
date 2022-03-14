package com.mehramoon.imagepickerwithcropping.crop

import android.annotation.TargetApi
import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.View
import com.mehramoon.imagepickerwithcropping.crop.BitmapUtils.getRectBottom
import com.mehramoon.imagepickerwithcropping.crop.BitmapUtils.getRectLeft
import com.mehramoon.imagepickerwithcropping.crop.BitmapUtils.getRectRight
import com.mehramoon.imagepickerwithcropping.crop.BitmapUtils.getRectTop
import java.util.*
import kotlin.math.*



class CropOverlayView  //endregion
@JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null) :
    View(context, attrs) {

    private var mScaleDetector: ScaleGestureDetector? = null
    private var mMultiTouchEnabled = false
    private val mCropWindowHandler: CropWindowHandler = CropWindowHandler()
    private var mCropWindowChangeListener: CropWindowChangeListener? = null
    private val mDrawRect = RectF()
    private var mBorderPaint: Paint? = null
    private var mBorderCornerPaint: Paint? = null
    private var mGuidelinePaint: Paint? = null
    private var mBackgroundPaint: Paint? = null
    private val mPath = Path()
    private val mBoundsPoints = FloatArray(8)
    private val mCalcBounds = RectF()
    private var mViewWidth = 0
    private var mViewHeight = 0
    private var mBorderCornerOffset = 0f
    private var mBorderCornerLength = 0f
    private var mInitialCropWindowPaddingRatio = 0f
    private var mTouchRadius = 0f
    private var mSnapRadius = 0f
    private var mMoveHandler: CropWindowMoveHandler? = null

    var isFixAspectRatio = false
        private set

    private var mAspectRatioX = 0
    private var mAspectRatioY = 0
    private var mTargetAspectRatio = mAspectRatioX.toFloat() / mAspectRatioY

    var guidelines: CropImageView.Guidelines? = null
        private set

    var cropShape: CropImageView.CropShape? = null
        private set

    private val mInitialCropWindowRect = Rect()
    private var initializedCropWindow = false
    private var mOriginalLayerType: Int? = null


    fun setCropWindowChangeListener(listener: CropWindowChangeListener?) {
        mCropWindowChangeListener = listener
    }

    var cropWindowRect: RectF
        get() = mCropWindowHandler.getRect()
        set(rect) {
            mCropWindowHandler.setRect(rect)
        }


    fun fixCurrentCropWindowRect() {
        val rect = cropWindowRect
        fixCropWindowRectByRules(rect)
        mCropWindowHandler.setRect(rect)
    }

    fun setBounds(boundsPoints: FloatArray?, viewWidth: Int, viewHeight: Int) {
        if (boundsPoints == null || !Arrays.equals(mBoundsPoints, boundsPoints)) {
            if (boundsPoints == null) {
                Arrays.fill(mBoundsPoints, 0f)
            } else {
                System.arraycopy(boundsPoints, 0, mBoundsPoints, 0, boundsPoints.size)
            }
            mViewWidth = viewWidth
            mViewHeight = viewHeight
            val cropRect: RectF = mCropWindowHandler.getRect()
            if (cropRect.width() == 0f || cropRect.height() == 0f) {
                initCropWindow()
            }
        }
    }


    fun resetCropOverlayView() {
        if (initializedCropWindow) {
            cropWindowRect =
                BitmapUtils.EMPTY_RECT_F
            initCropWindow()
            invalidate()
        }
    }

    fun setCropShape(cropShape: CropImageView.CropShape) {
        if (this.cropShape !== cropShape) {
            this.cropShape = cropShape
                if (this.cropShape === CropImageView.CropShape.OVAL) {
                    mOriginalLayerType = layerType
                    if (mOriginalLayerType != LAYER_TYPE_SOFTWARE) {
                        // TURN off hardware acceleration
                        setLayerType(LAYER_TYPE_SOFTWARE, null)
                    } else {
                        mOriginalLayerType = null
                    }
                } else if (mOriginalLayerType != null) {
                    // return hardware acceleration back
                    setLayerType(mOriginalLayerType!!, null)
                    mOriginalLayerType = null
                }
            invalidate()
        }
    }


    fun setGuidelines(guidelines: CropImageView.Guidelines) {
        if (this.guidelines !== guidelines) {
            this.guidelines = guidelines
            if (initializedCropWindow) {
                invalidate()
            }
        }
    }


    fun setFixedAspectRatio(fixAspectRatio: Boolean) {
        if (isFixAspectRatio != fixAspectRatio) {
            isFixAspectRatio = fixAspectRatio
            if (initializedCropWindow) {
                initCropWindow()
                invalidate()
            }
        }
    }

    var aspectRatioX: Int
        get() = mAspectRatioX
        set(aspectRatioX) {
            require(aspectRatioX > 0) { "Cannot set aspect ratio value to a number less than or equal to 0." }
            if (mAspectRatioX != aspectRatioX) {
                mAspectRatioX = aspectRatioX
                mTargetAspectRatio = mAspectRatioX.toFloat() / mAspectRatioY
                if (initializedCropWindow) {
                    initCropWindow()
                    invalidate()
                }
            }
        }

    var aspectRatioY: Int
        get() = mAspectRatioY
        set(aspectRatioY) {
            if (aspectRatioY <= 0) {
                throw IllegalArgumentException("Cannot set aspect ratio value to a number less than or equal to 0.")
            } else if (mAspectRatioY != aspectRatioY) {
                mAspectRatioY = aspectRatioY
                mTargetAspectRatio = mAspectRatioX.toFloat() / mAspectRatioY
                if (initializedCropWindow) {
                    initCropWindow()
                    invalidate()
                }
            }
        }


    fun setSnapRadius(snapRadius: Float) {
        mSnapRadius = snapRadius
    }


    fun setMultiTouchEnabled(multiTouchEnabled: Boolean): Boolean {
        if (mMultiTouchEnabled != multiTouchEnabled) {
            mMultiTouchEnabled = multiTouchEnabled
            if (mMultiTouchEnabled && mScaleDetector == null) {
                mScaleDetector = ScaleGestureDetector(context, ScaleListener())
            }
            return true
        }
        return false
    }


    fun setMinCropResultSize(minCropResultWidth: Int, minCropResultHeight: Int) {
        mCropWindowHandler.setMinCropResultSize(minCropResultWidth, minCropResultHeight)
    }


    fun setMaxCropResultSize(maxCropResultWidth: Int, maxCropResultHeight: Int) {
        mCropWindowHandler.setMaxCropResultSize(maxCropResultWidth, maxCropResultHeight)
    }


    fun setCropWindowLimits(
        maxWidth: Float,
        maxHeight: Float,
        scaleFactorWidth: Float,
        scaleFactorHeight: Float
    ) {
        mCropWindowHandler.setCropWindowLimits(
            maxWidth,
            maxHeight,
            scaleFactorWidth,
            scaleFactorHeight
        )
    }

    var initialCropWindowRect: Rect?
        get() = mInitialCropWindowRect
        set(rect) {
            mInitialCropWindowRect.set(rect ?: BitmapUtils.EMPTY_RECT)
            if (initializedCropWindow) {
                initCropWindow()
                invalidate()
                callOnCropWindowChanged(false)
            }
        }


    fun resetCropWindowRect() {
        if (initializedCropWindow) {
            initCropWindow()
            invalidate()
            callOnCropWindowChanged(false)
        }
    }


    fun setInitialAttributeValues(options: CropImageOptions) {
        mCropWindowHandler.setInitialAttributeValues(options)
        setCropShape(options.cropShape)
        setSnapRadius(options.snapRadius)
        setGuidelines(options.guidelines)
        setFixedAspectRatio(options.fixAspectRatio)
        aspectRatioX = options.aspectRatioX
        aspectRatioY = options.aspectRatioY
        setMultiTouchEnabled(options.multiTouchEnabled)
        mTouchRadius = options.touchRadius
        mInitialCropWindowPaddingRatio = options.initialCropWindowPaddingRatio
        mBorderPaint = getNewPaintOrNull(options.borderLineThickness, options.borderLineColor)
        mBorderCornerOffset = options.borderCornerOffset
        mBorderCornerLength = options.borderCornerLength
        mBorderCornerPaint =
            getNewPaintOrNull(options.borderCornerThickness, options.borderCornerColor)
        mGuidelinePaint = getNewPaintOrNull(options.guidelinesThickness, options.guidelinesColor)
        mBackgroundPaint = getNewPaint(options.backgroundColor)
    }

    private fun initCropWindow() {
        val leftLimit = getRectLeft(mBoundsPoints).coerceAtLeast(0f)
        val topLimit = getRectTop(mBoundsPoints).coerceAtLeast(0f)
        val rightLimit = getRectRight(mBoundsPoints).coerceAtMost(width.toFloat())
        val bottomLimit = getRectBottom(mBoundsPoints).coerceAtMost(height.toFloat())
        if (rightLimit <= leftLimit || bottomLimit <= topLimit) {
            return
        }
        val rect = RectF()

        // Tells the attribute functions the crop window has already been initialized
        initializedCropWindow = true
        val horizontalPadding = mInitialCropWindowPaddingRatio * (rightLimit - leftLimit)
        val verticalPadding = mInitialCropWindowPaddingRatio * (bottomLimit - topLimit)
        if (mInitialCropWindowRect.width() > 0 && mInitialCropWindowRect.height() > 0) {
            // Get crop window position relative to the displayed image.
            rect.left =
                leftLimit + mInitialCropWindowRect.left / mCropWindowHandler.getScaleFactorWidth()
            rect.top =
                topLimit + mInitialCropWindowRect.top / mCropWindowHandler.getScaleFactorHeight()
            rect.right =
                rect.left + mInitialCropWindowRect.width() / mCropWindowHandler.getScaleFactorWidth()
            rect.bottom =
                rect.top + mInitialCropWindowRect.height() / mCropWindowHandler.getScaleFactorHeight()

            // Correct for floating point errors. Crop rect boundaries should not exceed the source Bitmap bounds.
            rect.left = leftLimit.coerceAtLeast(rect.left)
            rect.top = topLimit.coerceAtLeast(rect.top)
            rect.right = rightLimit.coerceAtMost(rect.right)
            rect.bottom = bottomLimit.coerceAtMost(rect.bottom)
        } else if (isFixAspectRatio && rightLimit > leftLimit && bottomLimit > topLimit) {

            // If the image aspect ratio is wider than the crop aspect ratio,
            // then the image height is the determining initial length. Else, vice-versa.
            val bitmapAspectRatio = (rightLimit - leftLimit) / (bottomLimit - topLimit)
            if (bitmapAspectRatio > mTargetAspectRatio) {
                rect.top = topLimit + verticalPadding
                rect.bottom = bottomLimit - verticalPadding
                val centerX = width / 2f

                //dirty fix for wrong crop overlay aspect ratio when using fixed aspect ratio
                mTargetAspectRatio = mAspectRatioX.toFloat() / mAspectRatioY

                // Limits the aspect ratio to no less than 40 wide or 40 tall
                val cropWidth = Math.max(
                    mCropWindowHandler.getMinCropWidth(),
                    rect.height() * mTargetAspectRatio
                )
                val halfCropWidth = cropWidth / 2f
                rect.left = centerX - halfCropWidth
                rect.right = centerX + halfCropWidth
            } else {
                rect.left = leftLimit + horizontalPadding
                rect.right = rightLimit - horizontalPadding
                val centerY = height / 2f

                // Limits the aspect ratio to no less than 40 wide or 40 tall
                val cropHeight = Math.max(
                    mCropWindowHandler.getMinCropHeight(),
                    rect.width() / mTargetAspectRatio
                )
                val halfCropHeight = cropHeight / 2f
                rect.top = centerY - halfCropHeight
                rect.bottom = centerY + halfCropHeight
            }
        } else {
            // Initialize crop window to have 10% padding w/ respect to image.
            rect.left = leftLimit + horizontalPadding
            rect.top = topLimit + verticalPadding
            rect.right = rightLimit - horizontalPadding
            rect.bottom = bottomLimit - verticalPadding
        }
        fixCropWindowRectByRules(rect)
        mCropWindowHandler.setRect(rect)
    }


    private fun fixCropWindowRectByRules(rect: RectF) {
        if (rect.width() < mCropWindowHandler.getMinCropWidth()) {
            val adj: Float = (mCropWindowHandler.getMinCropWidth() - rect.width()) / 2
            rect.left -= adj
            rect.right += adj
        }
        if (rect.height() < mCropWindowHandler.getMinCropHeight()) {
            val adj: Float = (mCropWindowHandler.getMinCropHeight() - rect.height()) / 2
            rect.top -= adj
            rect.bottom += adj
        }
        if (rect.width() > mCropWindowHandler.getMaxCropWidth()) {
            val adj: Float = (rect.width() - mCropWindowHandler.getMaxCropWidth()) / 2
            rect.left += adj
            rect.right -= adj
        }
        if (rect.height() > mCropWindowHandler.getMaxCropHeight()) {
            val adj: Float = (rect.height() - mCropWindowHandler.getMaxCropHeight()) / 2
            rect.top += adj
            rect.bottom -= adj
        }
        calculateBounds(rect)
        if (mCalcBounds.width() > 0 && mCalcBounds.height() > 0) {
            val leftLimit = mCalcBounds.left.coerceAtLeast(0f)
            val topLimit = mCalcBounds.top.coerceAtLeast(0f)
            val rightLimit = mCalcBounds.right.coerceAtMost(width.toFloat())
            val bottomLimit = mCalcBounds.bottom.coerceAtMost(height.toFloat())
            if (rect.left < leftLimit) {
                rect.left = leftLimit
            }
            if (rect.top < topLimit) {
                rect.top = topLimit
            }
            if (rect.right > rightLimit) {
                rect.right = rightLimit
            }
            if (rect.bottom > bottomLimit) {
                rect.bottom = bottomLimit
            }
        }
        if (isFixAspectRatio && abs(rect.width() - rect.height() * mTargetAspectRatio) > 0.1) {
            if (rect.width() > rect.height() * mTargetAspectRatio) {
                val adj = abs(rect.height() * mTargetAspectRatio - rect.width()) / 2
                rect.left += adj
                rect.right -= adj
            } else {
                val adj = abs(rect.width() / mTargetAspectRatio - rect.height()) / 2
                rect.top += adj
                rect.bottom -= adj
            }
        }
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw translucent background for the cropped area.
        drawBackground(canvas)
        if (mCropWindowHandler.showGuidelines()) {
            // Determines whether guidelines should be drawn or not
            if (guidelines === CropImageView.Guidelines.ON) {
                drawGuidelines(canvas)
            } else if (guidelines === CropImageView.Guidelines.ON_TOUCH && mMoveHandler != null) {
                // Draw only when resizing
                drawGuidelines(canvas)
            }
        }
        drawBorders(canvas)
        drawCorners(canvas)
    }


    private fun drawBackground(canvas: Canvas) {
        val rect: RectF = mCropWindowHandler.getRect()
        val left = getRectLeft(mBoundsPoints).coerceAtLeast(0f)
        val top = getRectTop(mBoundsPoints).coerceAtLeast(0f)
        val right = getRectRight(mBoundsPoints).coerceAtMost(width.toFloat())
        val bottom = getRectBottom(mBoundsPoints).coerceAtMost(height.toFloat())
        if (cropShape == CropImageView.CropShape.RECTANGLE) {
            if (!isNonStraightAngleRotated) {
                canvas.drawRect(left, top, right, rect.top, mBackgroundPaint!!)
                canvas.drawRect(left, rect.bottom, right, bottom, mBackgroundPaint!!)
                canvas.drawRect(left, rect.top, rect.left, rect.bottom, mBackgroundPaint!!)
                canvas.drawRect(rect.right, rect.top, right, rect.bottom, mBackgroundPaint!!)
            } else {
                mPath.reset()
                mPath.moveTo(mBoundsPoints[0], mBoundsPoints[1])
                mPath.lineTo(mBoundsPoints[2], mBoundsPoints[3])
                mPath.lineTo(mBoundsPoints[4], mBoundsPoints[5])
                mPath.lineTo(mBoundsPoints[6], mBoundsPoints[7])
                mPath.close()
                canvas.save()
                canvas.clipPath(mPath, Region.Op.INTERSECT)
                canvas.clipRect(rect, Region.Op.XOR)
                canvas.drawRect(left, top, right, bottom, mBackgroundPaint!!)
                canvas.restore()
            }
        } else {
            mPath.reset()
            if (cropShape === CropImageView.CropShape.OVAL) {
                mDrawRect[rect.left + 2, rect.top + 2, rect.right - 2] = rect.bottom - 2
            } else {
                mDrawRect[rect.left, rect.top, rect.right] = rect.bottom
            }
            mPath.addOval(mDrawRect, Path.Direction.CW)
            canvas.save()
            canvas.clipPath(mPath, Region.Op.INTERSECT)
            canvas.drawRect(left, top, right, bottom, mBackgroundPaint!!)
            canvas.restore()
        }
    }


    private fun drawGuidelines(canvas: Canvas) {
        if (mGuidelinePaint != null) {
            val sw: Float = if (mBorderPaint != null) mBorderPaint!!.strokeWidth else 0F
            val rect: RectF = mCropWindowHandler.getRect()
            rect.inset(sw, sw)
            val oneThirdCropWidth = rect.width() / 3
            val oneThirdCropHeight = rect.height() / 3
            if (cropShape === CropImageView.CropShape.OVAL) {
                val w = rect.width() / 2 - sw
                val h = rect.height() / 2 - sw

                // Draw vertical guidelines.
                val x1 = rect.left + oneThirdCropWidth
                val x2 = rect.right - oneThirdCropWidth
                val yv =
                    (h * sin(acos(((w - oneThirdCropWidth) / w).toDouble()))).toFloat()
                canvas.drawLine(x1, rect.top + h - yv, x1, rect.bottom - h + yv, mGuidelinePaint!!)
                canvas.drawLine(x2, rect.top + h - yv, x2, rect.bottom - h + yv, mGuidelinePaint!!)

                // Draw horizontal guidelines.
                val y1 = rect.top + oneThirdCropHeight
                val y2 = rect.bottom - oneThirdCropHeight
                val xv =
                    (w * cos(asin(((h - oneThirdCropHeight) / h).toDouble()))).toFloat()
                canvas.drawLine(rect.left + w - xv, y1, rect.right - w + xv, y1, mGuidelinePaint!!)
                canvas.drawLine(rect.left + w - xv, y2, rect.right - w + xv, y2, mGuidelinePaint!!)
            } else {

                // Draw vertical guidelines.
                val x1 = rect.left + oneThirdCropWidth
                val x2 = rect.right - oneThirdCropWidth
                canvas.drawLine(x1, rect.top, x1, rect.bottom, mGuidelinePaint!!)
                canvas.drawLine(x2, rect.top, x2, rect.bottom, mGuidelinePaint!!)

                // Draw horizontal guidelines.
                val y1 = rect.top + oneThirdCropHeight
                val y2 = rect.bottom - oneThirdCropHeight
                canvas.drawLine(rect.left, y1, rect.right, y1, mGuidelinePaint!!)
                canvas.drawLine(rect.left, y2, rect.right, y2, mGuidelinePaint!!)
            }
        }
    }


    private fun drawBorders(canvas: Canvas) {
        if (mBorderPaint != null) {
            val w = mBorderPaint!!.strokeWidth
            val rect: RectF = mCropWindowHandler.getRect()
            rect.inset(w / 2, w / 2)
            if (cropShape === CropImageView.CropShape.RECTANGLE) {
                // Draw rectangle crop window border.
                canvas.drawRect(rect, mBorderPaint!!)
            } else {
                // Draw circular crop window border
                canvas.drawOval(rect, mBorderPaint!!)
            }
        }
    }

    /**
     * Draw the corner of crop overlay.
     */
    private fun drawCorners(canvas: Canvas) {
        if (mBorderCornerPaint != null) {
            val lineWidth: Float = if (mBorderPaint != null) mBorderPaint!!.strokeWidth else 0F
            val cornerWidth = mBorderCornerPaint!!.strokeWidth
            val w = cornerWidth / 2 + mBorderCornerOffset
            val rect: RectF = mCropWindowHandler.getRect()
            rect.inset(w, w)
            val cornerOffset = (cornerWidth - lineWidth) / 2
            val cornerExtension = cornerWidth / 2 + cornerOffset

            // Top left
            canvas.drawLine(
                rect.left - cornerOffset,
                rect.top - cornerExtension,
                rect.left - cornerOffset,
                rect.top + mBorderCornerLength,
                mBorderCornerPaint!!
            )
            canvas.drawLine(
                rect.left - cornerExtension,
                rect.top - cornerOffset,
                rect.left + mBorderCornerLength,
                rect.top - cornerOffset,
                mBorderCornerPaint!!
            )

            // Top right
            canvas.drawLine(
                rect.right + cornerOffset,
                rect.top - cornerExtension,
                rect.right + cornerOffset,
                rect.top + mBorderCornerLength,
                mBorderCornerPaint!!
            )
            canvas.drawLine(
                rect.right + cornerExtension,
                rect.top - cornerOffset,
                rect.right - mBorderCornerLength,
                rect.top - cornerOffset,
                mBorderCornerPaint!!
            )

            // Bottom left
            canvas.drawLine(
                rect.left - cornerOffset,
                rect.bottom + cornerExtension,
                rect.left - cornerOffset,
                rect.bottom - mBorderCornerLength,
                mBorderCornerPaint!!
            )
            canvas.drawLine(
                rect.left - cornerExtension,
                rect.bottom + cornerOffset,
                rect.left + mBorderCornerLength,
                rect.bottom + cornerOffset,
                mBorderCornerPaint!!
            )

            // Bottom left
            canvas.drawLine(
                rect.right + cornerOffset,
                rect.bottom + cornerExtension,
                rect.right + cornerOffset,
                rect.bottom - mBorderCornerLength,
                mBorderCornerPaint!!
            )
            canvas.drawLine(
                rect.right + cornerExtension,
                rect.bottom + cornerOffset,
                rect.right - mBorderCornerLength,
                rect.bottom + cornerOffset,
                mBorderCornerPaint!!
            )
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // If this View is not enabled, don't allow for touch interactions.
        return if (isEnabled) {
            if (mMultiTouchEnabled) {
                mScaleDetector!!.onTouchEvent(event)
            }
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    onActionDown(event.x, event.y)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    parent.requestDisallowInterceptTouchEvent(false)
                    onActionUp()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    onActionMove(event.x, event.y)
                    parent.requestDisallowInterceptTouchEvent(true)
                    true
                }
                else -> false
            }
        } else {
            false
        }
    }


    private fun onActionDown(x: Float, y: Float) {
        mMoveHandler = cropShape?.let { mCropWindowHandler.getMoveHandler(x, y, mTouchRadius, it) }
        if (mMoveHandler != null) {
            invalidate()
        }
    }

    /**
     * Clear move handler starting in [.onActionDown] if exists.
     */
    private fun onActionUp() {
        if (mMoveHandler != null) {
            mMoveHandler = null
            callOnCropWindowChanged(false)
            invalidate()
        }
    }


    private fun onActionMove(x: Float, y: Float) {
        if (mMoveHandler != null) {
            var snapRadius = mSnapRadius
            val rect: RectF = mCropWindowHandler.getRect()
            if (calculateBounds(rect)) {
                snapRadius = 0f
            }
            mMoveHandler!!.move(
                rect, x, y, mCalcBounds, mViewWidth, mViewHeight, snapRadius,
                isFixAspectRatio, mTargetAspectRatio
            )
            mCropWindowHandler.setRect(rect)
            callOnCropWindowChanged(true)
            invalidate()
        }
    }


    private fun calculateBounds(rect: RectF): Boolean {
        var left = getRectLeft(mBoundsPoints)
        var top = getRectTop(mBoundsPoints)
        var right = getRectRight(mBoundsPoints)
        var bottom = getRectBottom(mBoundsPoints)
        return if (!isNonStraightAngleRotated) {
            mCalcBounds[left, top, right] = bottom
            false
        } else {
            var x0 = mBoundsPoints[0]
            var y0 = mBoundsPoints[1]
            var x2 = mBoundsPoints[4]
            var y2 = mBoundsPoints[5]
            var x3 = mBoundsPoints[6]
            var y3 = mBoundsPoints[7]
            if (mBoundsPoints[7] < mBoundsPoints[1]) {
                if (mBoundsPoints[1] < mBoundsPoints[3]) {
                    x0 = mBoundsPoints[6]
                    y0 = mBoundsPoints[7]
                    x2 = mBoundsPoints[2]
                    y2 = mBoundsPoints[3]
                    x3 = mBoundsPoints[4]
                    y3 = mBoundsPoints[5]
                } else {
                    x0 = mBoundsPoints[4]
                    y0 = mBoundsPoints[5]
                    x2 = mBoundsPoints[0]
                    y2 = mBoundsPoints[1]
                    x3 = mBoundsPoints[2]
                    y3 = mBoundsPoints[3]
                }
            } else if (mBoundsPoints[1] > mBoundsPoints[3]) {
                x0 = mBoundsPoints[2]
                y0 = mBoundsPoints[3]
                x2 = mBoundsPoints[6]
                y2 = mBoundsPoints[7]
                x3 = mBoundsPoints[0]
                y3 = mBoundsPoints[1]
            }
            val a0 = (y3 - y0) / (x3 - x0)
            val a1 = -1f / a0
            val b0 = y0 - a0 * x0
            val b1 = y0 - a1 * x0
            val b2 = y2 - a0 * x2
            val b3 = y2 - a1 * x2
            val c0 = (rect.centerY() - rect.top) / (rect.centerX() - rect.left)
            val c1 = -c0
            val d0 = rect.top - c0 * rect.left
            val d1 = rect.top - c1 * rect.right
            left = left.coerceAtLeast(if ((d0 - b0) / (a0 - c0) < rect.right) (d0 - b0) / (a0 - c0) else left)
            left = left.coerceAtLeast(if ((d0 - b1) / (a1 - c0) < rect.right) (d0 - b1) / (a1 - c0) else left)
            left = left.coerceAtLeast(if ((d1 - b3) / (a1 - c1) < rect.right) (d1 - b3) / (a1 - c1) else left)
            right = right.coerceAtMost(if ((d1 - b1) / (a1 - c1) > rect.left) (d1 - b1) / (a1 - c1) else right)
            right = right.coerceAtMost(if ((d1 - b2) / (a0 - c1) > rect.left) (d1 - b2) / (a0 - c1) else right)
            right = right.coerceAtMost(if ((d0 - b2) / (a0 - c0) > rect.left) (d0 - b2) / (a0 - c0) else right)
            top = top.coerceAtLeast(Math.max(a0 * left + b0, a1 * right + b1))
            bottom = bottom.coerceAtMost(Math.min(a1 * left + b3, a0 * right + b2))
            mCalcBounds.left = left
            mCalcBounds.top = top
            mCalcBounds.right = right
            mCalcBounds.bottom = bottom
            true
        }
    }


    private val isNonStraightAngleRotated: Boolean
        get() = mBoundsPoints[0] != mBoundsPoints[6] && mBoundsPoints[1] != mBoundsPoints[7]


    private fun callOnCropWindowChanged(inProgress: Boolean) {
        try {
            if (mCropWindowChangeListener != null) {
                mCropWindowChangeListener!!.onCropWindowChanged(inProgress)
            }
        } catch (e: Exception) {
            Log.e("AIC", "Exception in crop window changed", e)
        }
    }


    interface CropWindowChangeListener {

        fun onCropWindowChanged(inProgress: Boolean)
    }

    private inner class ScaleListener : SimpleOnScaleGestureListener() {
        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val rect: RectF = mCropWindowHandler.getRect()
            val x = detector.focusX
            val y = detector.focusY
            val dY = detector.currentSpanY / 2
            val dX = detector.currentSpanX / 2
            val newTop = y - dY
            val newLeft = x - dX
            val newRight = x + dX
            val newBottom = y + dY
            if (newLeft < newRight && newTop <= newBottom && newLeft >= 0 && newRight <= mCropWindowHandler.getMaxCropWidth() && newTop >= 0 && newBottom <= mCropWindowHandler.getMaxCropHeight()) {
                rect[newLeft, newTop, newRight] = newBottom
                mCropWindowHandler.setRect(rect)
                invalidate()
            }
            return true
        }
    }

    companion object {

        private fun getNewPaint(color: Int): Paint {
            val paint = Paint()
            paint.color = color
            return paint
        }

        private fun getNewPaintOrNull(thickness: Float, color: Int): Paint? {
            return if (thickness > 0) {
                val borderPaint = Paint()
                borderPaint.color = color
                borderPaint.strokeWidth = thickness
                borderPaint.style = Paint.Style.STROKE
                borderPaint.isAntiAlias = true
                borderPaint
            } else {
                null
            }
        }
    }
}