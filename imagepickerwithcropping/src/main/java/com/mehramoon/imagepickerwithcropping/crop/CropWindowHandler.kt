package com.mehramoon.imagepickerwithcropping.crop

import android.graphics.RectF


internal class CropWindowHandler {

    private val mEdges = RectF()
    private val mGetEdges = RectF()
    private var mMinCropWindowWidth = 0f
    private var mMinCropWindowHeight = 0f
    private var mMaxCropWindowWidth = 0f
    private var mMaxCropWindowHeight = 0f
    private var mMinCropResultWidth = 0f
    private var mMinCropResultHeight = 0f
    private var mMaxCropResultWidth = 0f
    private var mMaxCropResultHeight = 0f
    private var mScaleFactorWidth = 1f
    private var mScaleFactorHeight = 1f

    fun getRect(): RectF {
        mGetEdges.set(mEdges)
        return mGetEdges
    }


    fun getMinCropWidth(): Float {
        return mMinCropWindowWidth.coerceAtLeast(mMinCropResultWidth / mScaleFactorWidth)
    }


    fun getMinCropHeight(): Float {
        return mMinCropWindowHeight.coerceAtLeast(mMinCropResultHeight / mScaleFactorHeight)
    }


    fun getMaxCropWidth(): Float {
        return mMaxCropWindowWidth.coerceAtMost(mMaxCropResultWidth / mScaleFactorWidth)
    }


    fun getMaxCropHeight(): Float {
        return mMaxCropWindowHeight.coerceAtMost(mMaxCropResultHeight / mScaleFactorHeight)
    }


    fun getScaleFactorWidth(): Float {
        return mScaleFactorWidth
    }

    fun getScaleFactorHeight(): Float {
        return mScaleFactorHeight
    }

    fun setMinCropResultSize(minCropResultWidth: Int, minCropResultHeight: Int) {
        mMinCropResultWidth = minCropResultWidth.toFloat()
        mMinCropResultHeight = minCropResultHeight.toFloat()
    }

    fun setMaxCropResultSize(maxCropResultWidth: Int, maxCropResultHeight: Int) {
        mMaxCropResultWidth = maxCropResultWidth.toFloat()
        mMaxCropResultHeight = maxCropResultHeight.toFloat()
    }

    fun setCropWindowLimits(
        maxWidth: Float,
        maxHeight: Float,
        scaleFactorWidth: Float,
        scaleFactorHeight: Float
    ) {
        mMaxCropWindowWidth = maxWidth
        mMaxCropWindowHeight = maxHeight
        mScaleFactorWidth = scaleFactorWidth
        mScaleFactorHeight = scaleFactorHeight
    }

    fun setInitialAttributeValues(options: CropImageOptions) {
        mMinCropWindowWidth = options.minCropWindowWidth.toFloat()
        mMinCropWindowHeight = options.minCropWindowHeight.toFloat()
        mMinCropResultWidth = options.minCropResultWidth.toFloat()
        mMinCropResultHeight = options.minCropResultHeight.toFloat()
        mMaxCropResultWidth = options.maxCropResultWidth.toFloat()
        mMaxCropResultHeight = options.maxCropResultHeight.toFloat()
    }

    fun setRect(rect: RectF?) {
        mEdges.set(rect!!)
    }


    fun showGuidelines(): Boolean {
        return !(mEdges.width() < 100 || mEdges.height() < 100)
    }

    /**
     * @param x the x-coordinate of the touch point
     * @param y the y-coordinate of the touch point
     * @param targetRadius the target radius in pixels
     * @return the Handle that was pressed; null if no Handle was pressed
     */
    fun getMoveHandler(
        x: Float,
        y: Float,
        targetRadius: Float,
        cropShape: CropImageView.CropShape
    ): CropWindowMoveHandler? {
        val type = if (cropShape === CropImageView.CropShape.OVAL) getOvalPressedMoveType(
            x,
            y
        ) else getRectanglePressedMoveType(x, y, targetRadius)
        return if (type != null) CropWindowMoveHandler(type, this, x, y) else null
    }

    /**
     * @param x the x-coordinate of the touch point
     * @param y the y-coordinate of the touch point
     * @param targetRadius the target radius in pixels
     * @return the Handle that was pressed; null if no Handle was pressed
     */
    private fun getRectanglePressedMoveType(
        x: Float,
        y: Float,
        targetRadius: Float
    ): CropWindowMoveHandler.Type? {
        var moveType: CropWindowMoveHandler.Type? = null

        if (isInCornerTargetZone(x, y, mEdges.left, mEdges.top, targetRadius)) {
            moveType = CropWindowMoveHandler.Type.TOP_LEFT
        } else if (isInCornerTargetZone(x, y, mEdges.right, mEdges.top, targetRadius)) {
            moveType = CropWindowMoveHandler.Type.TOP_RIGHT
        } else if (isInCornerTargetZone(x, y, mEdges.left, mEdges.bottom, targetRadius)) {
            moveType = CropWindowMoveHandler.Type.BOTTOM_LEFT
        } else if (isInCornerTargetZone(x, y, mEdges.right, mEdges.bottom, targetRadius)) {
            moveType = CropWindowMoveHandler.Type.BOTTOM_RIGHT
        } else if (isInCenterTargetZone(
                x,
                y,
                mEdges.left,
                mEdges.top,
                mEdges.right,
                mEdges.bottom
            ) && focusCenter()
        ) {
            moveType = CropWindowMoveHandler.Type.CENTER
        } else if (isInHorizontalTargetZone(
                x,
                y,
                mEdges.left,
                mEdges.right,
                mEdges.top,
                targetRadius
            )
        ) {
            moveType = CropWindowMoveHandler.Type.TOP
        } else if (isInHorizontalTargetZone(
                x,
                y,
                mEdges.left,
                mEdges.right,
                mEdges.bottom,
                targetRadius
            )
        ) {
            moveType = CropWindowMoveHandler.Type.BOTTOM
        } else if (isInVerticalTargetZone(
                x,
                y,
                mEdges.left,
                mEdges.top,
                mEdges.bottom,
                targetRadius
            )
        ) {
            moveType = CropWindowMoveHandler.Type.LEFT
        } else if (isInVerticalTargetZone(
                x,
                y,
                mEdges.right,
                mEdges.top,
                mEdges.bottom,
                targetRadius
            )
        ) {
            moveType = CropWindowMoveHandler.Type.RIGHT
        } else if (isInCenterTargetZone(
                x,
                y,
                mEdges.left,
                mEdges.top,
                mEdges.right,
                mEdges.bottom
            ) && !focusCenter()
        ) {
            moveType = CropWindowMoveHandler.Type.CENTER
        }
        return moveType
    }

    /**
     * @param x the x-coordinate of the touch point
     * @param y the y-coordinate of the touch point
     * @return the Handle that was pressed; null if no Handle was pressed
     */
    private fun getOvalPressedMoveType(x: Float, y: Float): CropWindowMoveHandler.Type {

        val cellLength = mEdges.width() / 6
        val leftCenter = mEdges.left + cellLength
        val rightCenter = mEdges.left + 5 * cellLength
        val cellHeight = mEdges.height() / 6
        val topCenter = mEdges.top + cellHeight
        val bottomCenter = mEdges.top + 5 * cellHeight
        val moveType: CropWindowMoveHandler.Type = if (x < leftCenter) {
            if (y < topCenter) {
                CropWindowMoveHandler.Type.TOP_LEFT
            } else if (y < bottomCenter) {
                CropWindowMoveHandler.Type.LEFT
            } else {
                CropWindowMoveHandler.Type.BOTTOM_LEFT
            }
        } else if (x < rightCenter) {
            if (y < topCenter) {
                CropWindowMoveHandler.Type.TOP
            } else if (y < bottomCenter) {
                CropWindowMoveHandler.Type.CENTER
            } else {
                CropWindowMoveHandler.Type.BOTTOM
            }
        } else {
            if (y < topCenter) {
                CropWindowMoveHandler.Type.TOP_RIGHT
            } else if (y < bottomCenter) {
                CropWindowMoveHandler.Type.RIGHT
            } else {
                CropWindowMoveHandler.Type.BOTTOM_RIGHT
            }
        }
        return moveType
    }


    private fun focusCenter(): Boolean {
        return !showGuidelines()
    }

    companion object {
        /**
         * @param x the x-coordinate of the touch point
         * @param y the y-coordinate of the touch point
         * @param handleX the x-coordinate of the corner handle
         * @param handleY the y-coordinate of the corner handle
         * @param targetRadius the target radius in pixels
         * @return true if the touch point is in the target touch zone; false
         * otherwise
         */
        private fun isInCornerTargetZone(
            x: Float,
            y: Float,
            handleX: Float,
            handleY: Float,
            targetRadius: Float
        ): Boolean {
            return Math.abs(x - handleX) <= targetRadius && Math.abs(y - handleY) <= targetRadius
        }

        /**
         * @param x the x-coordinate of the touch point
         * @param y the y-coordinate of the touch point
         * @param handleXStart the left x-coordinate of the horizontal bar handle
         * @param handleXEnd the right x-coordinate of the horizontal bar handle
         * @param handleY the y-coordinate of the horizontal bar handle
         * @param targetRadius the target radius in pixels
         * @return true if the touch point is in the target touch zone; false
         * otherwise
         */
        private fun isInHorizontalTargetZone(
            x: Float,
            y: Float,
            handleXStart: Float,
            handleXEnd: Float,
            handleY: Float,
            targetRadius: Float
        ): Boolean {
            return x > handleXStart && x < handleXEnd && Math.abs(y - handleY) <= targetRadius
        }

        /**
         * @param x the x-coordinate of the touch point
         * @param y the y-coordinate of the touch point
         * @param handleX the x-coordinate of the vertical bar handle
         * @param handleYStart the top y-coordinate of the vertical bar handle
         * @param handleYEnd the bottom y-coordinate of the vertical bar handle
         * @param targetRadius the target radius in pixels
         * @return true if the touch point is in the target touch zone; false
         * otherwise
         */
        private fun isInVerticalTargetZone(
            x: Float,
            y: Float,
            handleX: Float,
            handleYStart: Float,
            handleYEnd: Float,
            targetRadius: Float
        ): Boolean {
            return Math.abs(x - handleX) <= targetRadius && y > handleYStart && y < handleYEnd
        }

        /**
         * @param x the x-coordinate of the touch point
         * @param y the y-coordinate of the touch point
         * @param left the x-coordinate of the left bound
         * @param top the y-coordinate of the top bound
         * @param right the x-coordinate of the right bound
         * @param bottom the y-coordinate of the bottom bound
         * @return true if the touch point is inside the bounding rectangle; false
         * otherwise
         */
        private fun isInCenterTargetZone(
            x: Float,
            y: Float,
            left: Float,
            top: Float,
            right: Float,
            bottom: Float
        ): Boolean {
            return x > left && x < right && y > top && y < bottom
        }
    }
}