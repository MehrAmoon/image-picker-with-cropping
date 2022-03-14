package com.mehramoon.imagepickerwithcropping.crop

import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF

internal class CropWindowMoveHandler(


    private val mType: Type, cropWindowHandler: CropWindowHandler, touchX: Float, touchY: Float
) {

    private val mMinCropWidth: Float
    private val mMinCropHeight: Float
    private val mMaxCropWidth: Float
    private val mMaxCropHeight: Float
    private val mTouchOffset = PointF()

    /**
     * @param x the new x-coordinate of this handle
     * @param y the new y-coordinate of this handle
     * @param bounds the bounding rectangle of the image
     * @param viewWidth The bounding image view width used to know the crop overlay is at view edges.
     * @param viewHeight The bounding image view height used to know the crop overlay is at view edges.
     * @param parentView the parent View containing the image
     * @param snapMargin the maximum distance (in pixels) at which the crop window should snap to the image
     * @param fixedAspectRatio is the aspect ration fixed and 'targetAspectRatio' should be used
     * @param aspectRatio the aspect ratio to maintain
     */
    fun move(
        rect: RectF,
        x: Float,
        y: Float,
        bounds: RectF,
        viewWidth: Int,
        viewHeight: Int,
        snapMargin: Float,
        fixedAspectRatio: Boolean,
        aspectRatio: Float
    ) {

        val adjX = x + mTouchOffset.x
        val adjY = y + mTouchOffset.y
        if (mType == Type.CENTER) {
            moveCenter(rect, adjX, adjY, bounds, viewWidth, viewHeight, snapMargin)
        } else {
            if (fixedAspectRatio) {
                moveSizeWithFixedAspectRatio(
                    rect,
                    adjX,
                    adjY,
                    bounds,
                    viewWidth,
                    viewHeight,
                    snapMargin,
                    aspectRatio
                )
            } else {
                moveSizeWithFreeAspectRatio(
                    rect,
                    adjX,
                    adjY,
                    bounds,
                    viewWidth,
                    viewHeight,
                    snapMargin
                )
            }
        }
    }

    private fun calculateTouchOffset(rect: RectF?, touchX: Float, touchY: Float) {
        var touchOffsetX = 0f
        var touchOffsetY = 0f
        when (mType) {
            Type.TOP_LEFT -> {
                touchOffsetX = rect!!.left - touchX
                touchOffsetY = rect.top - touchY
            }
            Type.TOP_RIGHT -> {
                touchOffsetX = rect!!.right - touchX
                touchOffsetY = rect.top - touchY
            }
            Type.BOTTOM_LEFT -> {
                touchOffsetX = rect!!.left - touchX
                touchOffsetY = rect.bottom - touchY
            }
            Type.BOTTOM_RIGHT -> {
                touchOffsetX = rect!!.right - touchX
                touchOffsetY = rect.bottom - touchY
            }
            Type.LEFT -> {
                touchOffsetX = rect!!.left - touchX
                touchOffsetY = 0f
            }
            Type.TOP -> {
                touchOffsetX = 0f
                touchOffsetY = rect!!.top - touchY
            }
            Type.RIGHT -> {
                touchOffsetX = rect!!.right - touchX
                touchOffsetY = 0f
            }
            Type.BOTTOM -> {
                touchOffsetX = 0f
                touchOffsetY = rect!!.bottom - touchY
            }
            Type.CENTER -> {
                touchOffsetX = rect!!.centerX() - touchX
                touchOffsetY = rect.centerY() - touchY
            }
            else -> {}
        }
        mTouchOffset.x = touchOffsetX
        mTouchOffset.y = touchOffsetY
    }

    private fun moveCenter(
        rect: RectF,
        x: Float,
        y: Float,
        bounds: RectF,
        viewWidth: Int,
        viewHeight: Int,
        snapRadius: Float
    ) {
        var dx = x - rect.centerX()
        var dy = y - rect.centerY()
        if (rect.left + dx < 0 || rect.right + dx > viewWidth || rect.left + dx < bounds.left || rect.right + dx > bounds.right) {
            dx /= 1.05f
            mTouchOffset.x -= dx / 2
        }
        if (rect.top + dy < 0 || rect.bottom + dy > viewHeight || rect.top + dy < bounds.top || rect.bottom + dy > bounds.bottom) {
            dy /= 1.05f
            mTouchOffset.y -= dy / 2
        }
        rect.offset(dx, dy)
        snapEdgesToBounds(rect, bounds, snapRadius)
    }

    private fun moveSizeWithFreeAspectRatio(
        rect: RectF,
        x: Float,
        y: Float,
        bounds: RectF,
        viewWidth: Int,
        viewHeight: Int,
        snapMargin: Float
    ) {
        when (mType) {
            Type.TOP_LEFT -> {
                adjustTop(rect, y, bounds, snapMargin, 0f, false, false)
                adjustLeft(rect, x, bounds, snapMargin, 0f, false, false)
            }
            Type.TOP_RIGHT -> {
                adjustTop(rect, y, bounds, snapMargin, 0f, false, false)
                adjustRight(rect, x, bounds, viewWidth, snapMargin, 0f, false, false)
            }
            Type.BOTTOM_LEFT -> {
                adjustBottom(rect, y, bounds, viewHeight, snapMargin, 0f, false, false)
                adjustLeft(rect, x, bounds, snapMargin, 0f, false, false)
            }
            Type.BOTTOM_RIGHT -> {
                adjustBottom(rect, y, bounds, viewHeight, snapMargin, 0f, false, false)
                adjustRight(rect, x, bounds, viewWidth, snapMargin, 0f, false, false)
            }
            Type.LEFT -> adjustLeft(rect, x, bounds, snapMargin, 0f, false, false)
            Type.TOP -> adjustTop(rect, y, bounds, snapMargin, 0f, false, false)
            Type.RIGHT -> adjustRight(rect, x, bounds, viewWidth, snapMargin, 0f, false, false)
            Type.BOTTOM -> adjustBottom(rect, y, bounds, viewHeight, snapMargin, 0f, false, false)
            else -> {}
        }
    }

    private fun moveSizeWithFixedAspectRatio(
        rect: RectF,
        x: Float,
        y: Float,
        bounds: RectF,
        viewWidth: Int,
        viewHeight: Int,
        snapMargin: Float,
        aspectRatio: Float
    ) {
        when (mType) {
            Type.TOP_LEFT -> if (calculateAspectRatio(
                    x,
                    y,
                    rect.right,
                    rect.bottom
                ) < aspectRatio
            ) {
                adjustTop(rect, y, bounds, snapMargin, aspectRatio, true, false)
                adjustLeftByAspectRatio(rect, aspectRatio)
            } else {
                adjustLeft(rect, x, bounds, snapMargin, aspectRatio, true, false)
                adjustTopByAspectRatio(rect, aspectRatio)
            }
            Type.TOP_RIGHT -> if (calculateAspectRatio(
                    rect.left,
                    y,
                    x,
                    rect.bottom
                ) < aspectRatio
            ) {
                adjustTop(rect, y, bounds, snapMargin, aspectRatio, false, true)
                adjustRightByAspectRatio(rect, aspectRatio)
            } else {
                adjustRight(rect, x, bounds, viewWidth, snapMargin, aspectRatio, true, false)
                adjustTopByAspectRatio(rect, aspectRatio)
            }
            Type.BOTTOM_LEFT -> if (calculateAspectRatio(
                    x,
                    rect.top,
                    rect.right,
                    y
                ) < aspectRatio
            ) {
                adjustBottom(rect, y, bounds, viewHeight, snapMargin, aspectRatio, true, false)
                adjustLeftByAspectRatio(rect, aspectRatio)
            } else {
                adjustLeft(rect, x, bounds, snapMargin, aspectRatio, false, true)
                adjustBottomByAspectRatio(rect, aspectRatio)
            }
            Type.BOTTOM_RIGHT -> if (calculateAspectRatio(
                    rect.left,
                    rect.top,
                    x,
                    y
                ) < aspectRatio
            ) {
                adjustBottom(rect, y, bounds, viewHeight, snapMargin, aspectRatio, false, true)
                adjustRightByAspectRatio(rect, aspectRatio)
            } else {
                adjustRight(rect, x, bounds, viewWidth, snapMargin, aspectRatio, false, true)
                adjustBottomByAspectRatio(rect, aspectRatio)
            }
            Type.LEFT -> {
                adjustLeft(rect, x, bounds, snapMargin, aspectRatio, true, true)
                adjustTopBottomByAspectRatio(rect, bounds, aspectRatio)
            }
            Type.TOP -> {
                adjustTop(rect, y, bounds, snapMargin, aspectRatio, true, true)
                adjustLeftRightByAspectRatio(rect, bounds, aspectRatio)
            }
            Type.RIGHT -> {
                adjustRight(rect, x, bounds, viewWidth, snapMargin, aspectRatio, true, true)
                adjustTopBottomByAspectRatio(rect, bounds, aspectRatio)
            }
            Type.BOTTOM -> {
                adjustBottom(rect, y, bounds, viewHeight, snapMargin, aspectRatio, true, true)
                adjustLeftRightByAspectRatio(rect, bounds, aspectRatio)
            }
            else -> {}
        }
    }


    private fun snapEdgesToBounds(edges: RectF, bounds: RectF, margin: Float) {
        if (edges.left < bounds.left + margin) {
            edges.offset(bounds.left - edges.left, 0f)
        }
        if (edges.top < bounds.top + margin) {
            edges.offset(0f, bounds.top - edges.top)
        }
        if (edges.right > bounds.right - margin) {
            edges.offset(bounds.right - edges.right, 0f)
        }
        if (edges.bottom > bounds.bottom - margin) {
            edges.offset(0f, bounds.bottom - edges.bottom)
        }
    }

    /**
     * @param left the position that the left edge is dragged to
     * @param bounds the bounding box of the image that is being cropped
     * @param snapMargin the snap distance to the image edge (in pixels)
     */
    private fun adjustLeft(
        rect: RectF,
        left: Float,
        bounds: RectF,
        snapMargin: Float,
        aspectRatio: Float,
        topMoves: Boolean,
        bottomMoves: Boolean
    ) {
        var newLeft = left
        if (newLeft < 0) {
            newLeft /= 1.05f
            mTouchOffset.x -= newLeft / 1.1f
        }
        if (newLeft < bounds.left) {
            mTouchOffset.x -= (newLeft - bounds.left) / 2f
        }
        if (newLeft - bounds.left < snapMargin) {
            newLeft = bounds.left
        }

        // Checks if the window is too small horizontally
        if (rect.right - newLeft < mMinCropWidth) {
            newLeft = rect.right - mMinCropWidth
        }

        // Checks if the window is too large horizontally
        if (rect.right - newLeft > mMaxCropWidth) {
            newLeft = rect.right - mMaxCropWidth
        }
        if (newLeft - bounds.left < snapMargin) {
            newLeft = bounds.left
        }

        // check vertical bounds if aspect ratio is in play
        if (aspectRatio > 0) {
            var newHeight = (rect.right - newLeft) / aspectRatio

            // Checks if the window is too small vertically
            if (newHeight < mMinCropHeight) {
                newLeft = Math.max(bounds.left, rect.right - mMinCropHeight * aspectRatio)
                newHeight = (rect.right - newLeft) / aspectRatio
            }

            // Checks if the window is too large vertically
            if (newHeight > mMaxCropHeight) {
                newLeft = Math.max(bounds.left, rect.right - mMaxCropHeight * aspectRatio)
                newHeight = (rect.right - newLeft) / aspectRatio
            }

            // if top AND bottom edge moves by aspect ratio check that it is within full height bounds
            if (topMoves && bottomMoves) {
                newLeft = Math.max(
                    newLeft,
                    Math.max(bounds.left, rect.right - bounds.height() * aspectRatio)
                )
            } else {
                // if top edge moves by aspect ratio check that it is within bounds
                if (topMoves && rect.bottom - newHeight < bounds.top) {
                    newLeft =
                        Math.max(bounds.left, rect.right - (rect.bottom - bounds.top) * aspectRatio)
                    newHeight = (rect.right - newLeft) / aspectRatio
                }

                // if bottom edge moves by aspect ratio check that it is within bounds
                if (bottomMoves && rect.top + newHeight > bounds.bottom) {
                    newLeft = Math.max(
                        newLeft,
                        Math.max(bounds.left, rect.right - (bounds.bottom - rect.top) * aspectRatio)
                    )
                }
            }
        }
        rect.left = newLeft
    }

    /**
     * @param right the position that the right edge is dragged to
     * @param bounds the bounding box of the image that is being cropped
     * @param viewWidth
     * @param snapMargin the snap distance to the image edge (in pixels)
     */
    private fun adjustRight(
        rect: RectF,
        right: Float,
        bounds: RectF,
        viewWidth: Int,
        snapMargin: Float,
        aspectRatio: Float,
        topMoves: Boolean,
        bottomMoves: Boolean
    ) {
        var newRight = right
        if (newRight > viewWidth) {
            newRight = viewWidth + (newRight - viewWidth) / 1.05f
            mTouchOffset.x -= (newRight - viewWidth) / 1.1f
        }
        if (newRight > bounds.right) {
            mTouchOffset.x -= (newRight - bounds.right) / 2f
        }

        // If close to the edge
        if (bounds.right - newRight < snapMargin) {
            newRight = bounds.right
        }

        // Checks if the window is too small horizontally
        if (newRight - rect.left < mMinCropWidth) {
            newRight = rect.left + mMinCropWidth
        }

        // Checks if the window is too large horizontally
        if (newRight - rect.left > mMaxCropWidth) {
            newRight = rect.left + mMaxCropWidth
        }

        // If close to the edge
        if (bounds.right - newRight < snapMargin) {
            newRight = bounds.right
        }

        // check vertical bounds if aspect ratio is in play
        if (aspectRatio > 0) {
            var newHeight = (newRight - rect.left) / aspectRatio

            // Checks if the window is too small vertically
            if (newHeight < mMinCropHeight) {
                newRight = Math.min(bounds.right, rect.left + mMinCropHeight * aspectRatio)
                newHeight = (newRight - rect.left) / aspectRatio
            }

            // Checks if the window is too large vertically
            if (newHeight > mMaxCropHeight) {
                newRight = Math.min(bounds.right, rect.left + mMaxCropHeight * aspectRatio)
                newHeight = (newRight - rect.left) / aspectRatio
            }

            // if top AND bottom edge moves by aspect ratio check that it is within full height bounds
            if (topMoves && bottomMoves) {
                newRight = Math.min(
                    newRight,
                    Math.min(bounds.right, rect.left + bounds.height() * aspectRatio)
                )
            } else {
                // if top edge moves by aspect ratio check that it is within bounds
                if (topMoves && rect.bottom - newHeight < bounds.top) {
                    newRight =
                        Math.min(bounds.right, rect.left + (rect.bottom - bounds.top) * aspectRatio)
                    newHeight = (newRight - rect.left) / aspectRatio
                }

                // if bottom edge moves by aspect ratio check that it is within bounds
                if (bottomMoves && rect.top + newHeight > bounds.bottom) {
                    newRight = Math.min(
                        newRight,
                        Math.min(bounds.right, rect.left + (bounds.bottom - rect.top) * aspectRatio)
                    )
                }
            }
        }
        rect.right = newRight
    }

    /**
     * @param top the x-position that the top edge is dragged to
     * @param bounds the bounding box of the image that is being cropped
     * @param snapMargin the snap distance to the image edge (in pixels)
     */
    private fun adjustTop(
        rect: RectF,
        top: Float,
        bounds: RectF,
        snapMargin: Float,
        aspectRatio: Float,
        leftMoves: Boolean,
        rightMoves: Boolean
    ) {
        var newTop = top
        if (newTop < 0) {
            newTop /= 1.05f
            mTouchOffset.y -= newTop / 1.1f
        }
        if (newTop < bounds.top) {
            mTouchOffset.y -= (newTop - bounds.top) / 2f
        }
        if (newTop - bounds.top < snapMargin) {
            newTop = bounds.top
        }

        // Checks if the window is too small vertically
        if (rect.bottom - newTop < mMinCropHeight) {
            newTop = rect.bottom - mMinCropHeight
        }

        // Checks if the window is too large vertically
        if (rect.bottom - newTop > mMaxCropHeight) {
            newTop = rect.bottom - mMaxCropHeight
        }
        if (newTop - bounds.top < snapMargin) {
            newTop = bounds.top
        }

        // check horizontal bounds if aspect ratio is in play
        if (aspectRatio > 0) {
            var newWidth = (rect.bottom - newTop) * aspectRatio

            // Checks if the crop window is too small horizontally due to aspect ratio adjustment
            if (newWidth < mMinCropWidth) {
                newTop = Math.max(bounds.top, rect.bottom - mMinCropWidth / aspectRatio)
                newWidth = (rect.bottom - newTop) * aspectRatio
            }

            // Checks if the crop window is too large horizontally due to aspect ratio adjustment
            if (newWidth > mMaxCropWidth) {
                newTop = Math.max(bounds.top, rect.bottom - mMaxCropWidth / aspectRatio)
                newWidth = (rect.bottom - newTop) * aspectRatio
            }

            // if left AND right edge moves by aspect ratio check that it is within full width bounds
            if (leftMoves && rightMoves) {
                newTop = Math.max(
                    newTop,
                    Math.max(bounds.top, rect.bottom - bounds.width() / aspectRatio)
                )
            } else {
                // if left edge moves by aspect ratio check that it is within bounds
                if (leftMoves && rect.right - newWidth < bounds.left) {
                    newTop =
                        Math.max(bounds.top, rect.bottom - (rect.right - bounds.left) / aspectRatio)
                    newWidth = (rect.bottom - newTop) * aspectRatio
                }

                // if right edge moves by aspect ratio check that it is within bounds
                if (rightMoves && rect.left + newWidth > bounds.right) {
                    newTop = Math.max(
                        newTop,
                        Math.max(bounds.top, rect.bottom - (bounds.right - rect.left) / aspectRatio)
                    )
                }
            }
        }
        rect.top = newTop
    }

    /**
     * @param bottom the position that the bottom edge is dragged to
     * @param bounds the bounding box of the image that is being cropped
     * @param viewHeight
     * @param snapMargin the snap distance to the image edge (in pixels)
     */
    private fun adjustBottom(
        rect: RectF,
        bottom: Float,
        bounds: RectF,
        viewHeight: Int,
        snapMargin: Float,
        aspectRatio: Float,
        leftMoves: Boolean,
        rightMoves: Boolean
    ) {
        var newBottom = bottom
        if (newBottom > viewHeight) {
            newBottom = viewHeight + (newBottom - viewHeight) / 1.05f
            mTouchOffset.y -= (newBottom - viewHeight) / 1.1f
        }
        if (newBottom > bounds.bottom) {
            mTouchOffset.y -= (newBottom - bounds.bottom) / 2f
        }
        if (bounds.bottom - newBottom < snapMargin) {
            newBottom = bounds.bottom
        }

        // Checks if the window is too small vertically
        if (newBottom - rect.top < mMinCropHeight) {
            newBottom = rect.top + mMinCropHeight
        }

        // Checks if the window is too small vertically
        if (newBottom - rect.top > mMaxCropHeight) {
            newBottom = rect.top + mMaxCropHeight
        }
        if (bounds.bottom - newBottom < snapMargin) {
            newBottom = bounds.bottom
        }

        // check horizontal bounds if aspect ratio is in play
        if (aspectRatio > 0) {
            var newWidth = (newBottom - rect.top) * aspectRatio

            // Checks if the window is too small horizontally
            if (newWidth < mMinCropWidth) {
                newBottom = Math.min(bounds.bottom, rect.top + mMinCropWidth / aspectRatio)
                newWidth = (newBottom - rect.top) * aspectRatio
            }

            // Checks if the window is too large horizontally
            if (newWidth > mMaxCropWidth) {
                newBottom = Math.min(bounds.bottom, rect.top + mMaxCropWidth / aspectRatio)
                newWidth = (newBottom - rect.top) * aspectRatio
            }

            // if left AND right edge moves by aspect ratio check that it is within full width bounds
            if (leftMoves && rightMoves) {
                newBottom = Math.min(
                    newBottom,
                    Math.min(bounds.bottom, rect.top + bounds.width() / aspectRatio)
                )
            } else {
                // if left edge moves by aspect ratio check that it is within bounds
                if (leftMoves && rect.right - newWidth < bounds.left) {
                    newBottom =
                        Math.min(bounds.bottom, rect.top + (rect.right - bounds.left) / aspectRatio)
                    newWidth = (newBottom - rect.top) * aspectRatio
                }

                // if right edge moves by aspect ratio check that it is within bounds
                if (rightMoves && rect.left + newWidth > bounds.right) {
                    newBottom = Math.min(
                        newBottom,
                        Math.min(bounds.bottom, rect.top + (bounds.right - rect.left) / aspectRatio)
                    )
                }
            }
        }
        rect.bottom = newBottom
    }

    private fun adjustLeftByAspectRatio(rect: RectF, aspectRatio: Float) {
        rect.left = rect.right - rect.height() * aspectRatio
    }

    private fun adjustTopByAspectRatio(rect: RectF, aspectRatio: Float) {
        rect.top = rect.bottom - rect.width() / aspectRatio
    }

    private fun adjustRightByAspectRatio(rect: RectF, aspectRatio: Float) {
        rect.right = rect.left + rect.height() * aspectRatio
    }

    private fun adjustBottomByAspectRatio(rect: RectF, aspectRatio: Float) {
        rect.bottom = rect.top + rect.width() / aspectRatio
    }

    private fun adjustLeftRightByAspectRatio(rect: RectF, bounds: RectF, aspectRatio: Float) {
        rect.inset((rect.width() - rect.height() * aspectRatio) / 2, 0f)
        if (rect.left < bounds.left) {
            rect.offset(bounds.left - rect.left, 0f)
        }
        if (rect.right > bounds.right) {
            rect.offset(bounds.right - rect.right, 0f)
        }
    }

    private fun adjustTopBottomByAspectRatio(rect: RectF, bounds: RectF, aspectRatio: Float) {
        rect.inset(0f, (rect.height() - rect.width() / aspectRatio) / 2)
        if (rect.top < bounds.top) {
            rect.offset(0f, bounds.top - rect.top)
        }
        if (rect.bottom > bounds.bottom) {
            rect.offset(0f, bounds.bottom - rect.bottom)
        }
    }

    enum class Type {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, LEFT, TOP, RIGHT, BOTTOM, CENTER
    }

    companion object {

        private val MATRIX = Matrix()

        private fun calculateAspectRatio(
            left: Float,
            top: Float,
            right: Float,
            bottom: Float
        ): Float {
            return (right - left) / (bottom - top)
        }
    }

    /**
     * @param edgeMoveType the type of move this handler is executing
     * @param horizontalEdge the primary edge associated with this handle; may be null
     * @param verticalEdge the secondary edge associated with this handle; may be null
     * @param cropWindowHandler main crop window handle to get and update the crop window edges
     * @param touchX the location of the initial toch possition to measure move distance
     * @param touchY the location of the initial toch possition to measure move distance
     */
    init {
        mMinCropWidth = cropWindowHandler.getMinCropWidth()
        mMinCropHeight = cropWindowHandler.getMinCropHeight()
        mMaxCropWidth = cropWindowHandler.getMaxCropWidth()
        mMaxCropHeight = cropWindowHandler.getMaxCropHeight()
        calculateTouchOffset(cropWindowHandler.getRect(), touchX, touchY)
    }
}