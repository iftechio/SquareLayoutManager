package io.iftech.android.library.square

import android.animation.ValueAnimator
import android.graphics.Point
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.animation.doOnEnd
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.*

class SquareLayoutManager @JvmOverloads constructor(
    val spanCount: Int = 3,
    private val startPosition: Int = -1
) : RecyclerView.LayoutManager() {

    private var viewGap = 0f

    val rowCount: Int by lazy {
        ceil(itemCount / spanCount.toFloat()).toInt()
    }

    private var onceCompleteScrollLengthForVer = -1f
    private var onceCompleteScrollLengthForHor = -1f

    private var firstChildCompleteScrollLengthForVer = -1f
    private var firstChildCompleteScrollLengthForHor = -1f

    private var verScrollLock = false
    private var horScrollLock = false

    private var firstVisiblePos = 0

    var childHeight = 0
        private set

    var childWidth = 0
        private set

    private var verticalOffset: Long = 0
    private var horizontalOffset: Long = 0

    private val verticalMaxOffset: Float
        get() = if (childHeight == 0 || itemCount == 0) 0f else (childHeight + viewGap) * (rowCount - 1)

    private val verticalMinOffset: Float
        get() = if (childHeight == 0) 0f else (height - childHeight) / 2f

    private val horizontalMaxOffset: Float
        get() = if (childWidth == 0 || itemCount == 0) 0f else (childWidth + viewGap) * (spanCount - 1)

    private val horizontalMinOffset: Float
        get() = if (childWidth == 0) 0f else (width - childWidth) / 2f

    private var selectAnimator: ValueAnimator? = null

    /**
     * 使用 SnapHelper 自动选中最靠近中心的 Item，默认为true
     */
    var isAutoSelect = true

    /**
     * 初始化布局的时候，是否定位到中心的 Item
     */
    var isInitLayoutCenter = true
    private var isFirstLayout = true

    private var lastSelectedPosition = 0
    private var onItemSelectedListener: (Int) -> Unit = {}

    /**
     * 滑动到指定位置
     */
    fun smoothScrollToPosition(position: Int) {
        if (position > -1 && position < itemCount) {
            startValueAnimator(position)
        }
    }

    /**
     * 滑动到中心
     */
    fun smoothScrollToCenter() {
        val centerPos = rowCount / 2 * spanCount + spanCount / 2
        smoothScrollToPosition(centerPos)
    }

    /**
     * 设置选中 Item 的监听回调
     */
    fun setOnItemSelectedListener(listener: (Int) -> Unit) {
        onItemSelectedListener = listener
    }

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(
            RecyclerView.LayoutParams.WRAP_CONTENT,
            RecyclerView.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        if (state.itemCount == 0) {
            removeAndRecycleAllViews(recycler)
            return
        }
        onceCompleteScrollLengthForVer = -1f
        onceCompleteScrollLengthForHor = -1f

        detachAndScrapAttachedViews(recycler)

        onLayout(recycler, 0, 0)
    }

    private fun onLayout(
        recycler: RecyclerView.Recycler,
        dx: Int,
        dy: Int
    ): Point {
        val pointResult = checkMoveLimit(dy, dx)

        detachAndScrapAttachedViews(recycler)

        var verStart: Float
        var horStart: Float

        val normalViewOffsetForVer: Float
        val normalViewOffsetForHor: Float

        var tempView: View? = null
        var tempPosition = -1

        val firstVisiblePosForVer: Int
        val firstVisiblePosForHor: Int

        if (onceCompleteScrollLengthForVer == -1f) {
            tempPosition = firstVisiblePos
            tempView = recycler.getViewForPosition(tempPosition)
            measureChildWithMargins(tempView, 0, 0)
            childHeight = getDecoratedMeasurementVertical(tempView)
            childWidth = getDecoratedMeasurementHorizontal(tempView)
        }

        firstChildCompleteScrollLengthForVer = height / 2f + childHeight / 2f
        firstChildCompleteScrollLengthForHor = width / 2f + childWidth / 2f

        if (isInitLayoutCenter && isFirstLayout) {
            isFirstLayout = false
            val centerPos = rowCount / 2 * spanCount + spanCount / 2
            val startPos = if (startPosition != -1) startPosition else centerPos
            onItemSelectedListener(startPos)
            lastSelectedPosition = startPos
            val distanceForVer = calculateScrollToPositionVerOffset(startPos)
            val distanceForHor = calculateScrollToPositionHorOffset(startPos)
            verticalOffset += distanceForVer.toLong()
            horizontalOffset += distanceForHor.toLong()
        }

        if (verticalOffset >= firstChildCompleteScrollLengthForVer) {
            verStart = viewGap
            onceCompleteScrollLengthForVer = childHeight + viewGap
            firstVisiblePosForVer =
                floor(abs(verticalOffset - firstChildCompleteScrollLengthForVer) / onceCompleteScrollLengthForVer.toDouble())
                    .toInt() + 1
            normalViewOffsetForVer =
                abs(verticalOffset - firstChildCompleteScrollLengthForVer) % onceCompleteScrollLengthForVer
        } else {
            firstVisiblePosForVer = 0
            verStart = verticalMinOffset
            onceCompleteScrollLengthForVer = firstChildCompleteScrollLengthForVer
            normalViewOffsetForVer = abs(verticalOffset) % onceCompleteScrollLengthForVer
        }

        if (horizontalOffset >= firstChildCompleteScrollLengthForHor) {
            horStart = viewGap
            onceCompleteScrollLengthForHor = childWidth + viewGap
            firstVisiblePosForHor =
                floor(abs(horizontalOffset - firstChildCompleteScrollLengthForHor) / onceCompleteScrollLengthForHor.toDouble())
                    .toInt() + 1
            normalViewOffsetForHor =
                abs(horizontalOffset - firstChildCompleteScrollLengthForHor) % onceCompleteScrollLengthForHor
        } else {
            firstVisiblePosForHor = 0
            horStart = horizontalMinOffset
            onceCompleteScrollLengthForHor = firstChildCompleteScrollLengthForHor
            normalViewOffsetForHor = abs(horizontalOffset) % onceCompleteScrollLengthForHor
        }
        firstVisiblePos = firstVisiblePosForVer * spanCount + firstVisiblePosForHor

        verStart -= normalViewOffsetForVer
        horStart -= normalViewOffsetForHor
        val startLeft = horStart

        var index = firstVisiblePos
        var verCount = 1

        while (index != -1) {
            val item = if (index == tempPosition && tempView != null) {
                tempView
            } else {
                recycler.getViewForPosition(index)
            }

            val focusPositionForVer =
                (abs(verticalOffset) / (childHeight + viewGap)).toInt()
            val focusPositionForHor =
                (abs(horizontalOffset) / (childWidth + viewGap)).toInt()
            val focusPosition = focusPositionForVer * spanCount + focusPositionForHor

            if (index <= focusPosition) {
                addView(item)
            } else {
                addView(item, 0)
            }

            measureChildWithMargins(item, 0, 0)

            val left = horStart.toInt()
            val top = verStart.toInt()
            val right = left + getDecoratedMeasurementHorizontal(item)
            val bottom = top + getDecoratedMeasurementVertical(item)

            val minScale = 0.8f

            val childCenterY = (top + bottom) / 2
            val parentCenterY = height / 2
            val fractionScaleY = abs(parentCenterY - childCenterY) / parentCenterY.toFloat()
            val scaleX = 1.0f - (1.0f - minScale) * fractionScaleY

            val childCenterX = (right + left) / 2
            val parentCenterX = width / 2
            val fractionScaleX = abs(parentCenterX - childCenterX) / parentCenterX.toFloat()
            val scaleY = 1.0f - (1.0f - minScale) * fractionScaleX

            item.scaleX = max(min(scaleX, scaleY), minScale)
            item.scaleY = max(min(scaleX, scaleY), minScale)

            layoutDecoratedWithMargins(item, left, top, right, bottom)

            val verticalIndex = {
                verStart += childHeight + viewGap
                horStart = startLeft
                if (verStart > height - paddingBottom) {
                    index = -1
                } else {
                    index = firstVisiblePos + verCount * spanCount
                    verCount++
                }
            }

            if ((index + 1) % spanCount != 0) {
                horStart += childWidth + viewGap
                if (horStart > width - paddingRight) {
                    verticalIndex()
                } else {
                    index++
                }
            } else {
                verticalIndex()
            }

            if (index >= itemCount) {
                index = -1
            }
        }
        verScrollLock = false
        horScrollLock = false
        return pointResult
    }

    private fun checkMoveLimit(dy: Int, dx: Int): Point {
        var dyResult = dy
        var dxResult = dx

        if (dyResult < 0) {
            if (verticalOffset < 0) {
                verticalOffset = 0.also { dyResult = it }.toLong()
            }
        }
        if (dyResult > 0) {
            if (verticalOffset >= verticalMaxOffset) {
                verticalOffset = verticalMaxOffset.toLong()
                dyResult = 0
            }
        }

        if (dxResult < 0) {
            if (horizontalOffset < 0) {
                horizontalOffset = 0.also { dxResult = it }.toLong()
            }
        }
        if (dxResult > 0) {
            if (horizontalOffset >= horizontalMaxOffset) {
                horizontalOffset = horizontalMaxOffset.toLong()
                dxResult = 0
            }
        }
        return Point(dxResult, dyResult)
    }

    override fun canScrollHorizontally() = horScrollLock.not()

    override fun canScrollVertically() = verScrollLock.not()

    override fun scrollHorizontallyBy(
        dx: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ): Int {
        if (dx == 0 || childCount == 0) {
            return 0
        }
        verScrollLock = true
        if (abs(dx.toFloat()) < 0.00000001f) {
            return 0
        }
        horizontalOffset += dx
        return onLayout(recycler, dx, 0).x
    }

    override fun scrollVerticallyBy(
        dy: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ): Int {
        if (dy == 0 || childCount == 0) {
            return 0
        }
        horScrollLock = true
        if (abs(dy.toFloat()) < 0.00000001f) {
            return 0
        }
        verticalOffset += dy
        return onLayout(recycler, 0, dy).y
    }

    override fun onScrollStateChanged(state: Int) {
        super.onScrollStateChanged(state)
        if (state == RecyclerView.SCROLL_STATE_DRAGGING) {
            cancelAnimator()
        }
    }

    override fun onAttachedToWindow(view: RecyclerView?) {
        super.onAttachedToWindow(view)
        if (isAutoSelect) {
            SquareSnapHelper().attachToRecyclerView(view)
        }
    }

    fun calculateDistanceToPositionForVer(targetPos: Int): Int {
        return childHeight * (targetPos / spanCount) - verticalOffset.toInt()
    }

    fun calculateDistanceToPositionForHor(targetPos: Int): Int {
        return childWidth * (targetPos % spanCount) - horizontalOffset.toInt()
    }

    override fun smoothScrollToPosition(
        recyclerView: RecyclerView?,
        state: RecyclerView.State?,
        position: Int
    ) {
        smoothScrollToPosition(position)
    }


    private fun startValueAnimator(position: Int) {
        cancelAnimator()
        val distanceForVer = calculateScrollToPositionVerOffset(position)
        val distanceForHor = calculateScrollToPositionHorOffset(position)

        val minDuration: Long = 200
        val maxDuration: Long = 400

        val distanceFractionForVer: Float =
            abs(distanceForVer) / (childHeight + viewGap)
        val distanceFractionForHor: Float =
            abs(distanceForHor) / (childWidth + viewGap)

        val durationForVer = if (distanceForVer <= childHeight + viewGap) {
            (minDuration + (maxDuration - minDuration) * distanceFractionForVer).toLong()
        } else {
            (maxDuration * distanceFractionForVer).toLong()
        }

        val durationForHor = if (distanceForHor <= childWidth + viewGap) {
            (minDuration + (maxDuration - minDuration) * distanceFractionForHor).toLong()
        } else {
            (maxDuration * distanceFractionForHor).toLong()
        }

        val duration = max(durationForVer, durationForHor)

        selectAnimator = ValueAnimator.ofFloat(0.0f, duration.toFloat()).apply {
            this.duration = duration
            interpolator = DecelerateInterpolator()

            val startedOffsetForVer = verticalOffset.toFloat()
            val startedOffsetForHor = horizontalOffset.toFloat()

            addUpdateListener { animation ->
                val value = animation.animatedValue as Float
                verticalOffset =
                    (startedOffsetForVer + value * (distanceForVer / duration.toFloat())).toLong()
                horizontalOffset =
                    (startedOffsetForHor + value * (distanceForHor / duration.toFloat())).toLong()

                requestLayout()
            }
            doOnEnd {
                if (lastSelectedPosition != position) {
                    onItemSelectedListener(position)
                    lastSelectedPosition = position
                }
            }
            start()
        }
    }


    fun cancelAnimator() {
        selectAnimator?.takeIf { it.isStarted || it.isRunning }?.apply {
            cancel()
        }
    }

    private fun calculateScrollToPositionVerOffset(position: Int) =
        position / spanCount * (childHeight + viewGap) - abs(verticalOffset)

    private fun calculateScrollToPositionHorOffset(position: Int) =
        position % spanCount * (childWidth + viewGap) - abs(horizontalOffset)

    private fun getDecoratedMeasurementHorizontal(view: View): Int {
        val params = view.layoutParams as RecyclerView.LayoutParams
        return getDecoratedMeasuredWidth(view) + params.leftMargin + params.rightMargin
    }

    private fun getDecoratedMeasurementVertical(view: View): Int {
        val params = view.layoutParams as RecyclerView.LayoutParams
        return getDecoratedMeasuredHeight(view) + params.topMargin + params.bottomMargin
    }
}