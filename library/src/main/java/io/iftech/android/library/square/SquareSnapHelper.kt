package io.iftech.android.library.square

import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Scroller
import androidx.recyclerview.widget.OrientationHelper
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal class SquareSnapHelper : RecyclerView.OnFlingListener() {

    private var mVerticalHelper: OrientationHelper? = null
    private var mHorizontalHelper: OrientationHelper? = null

    private var mRecyclerView: RecyclerView? = null
    private var mGravityScroller: Scroller? = null

    private val mScrollListener: RecyclerView.OnScrollListener =
        object : RecyclerView.OnScrollListener() {
            var mScrolled = false
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE && mScrolled) {
                    mScrolled = false
                    snapToTargetExistingView()
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dx != 0 || dy != 0) {
                    mScrolled = true
                }
            }
        }

    fun snapToTargetExistingView() {
        val layoutManager = mRecyclerView?.layoutManager ?: return
        val snapView: View = findSnapView(layoutManager) ?: return
        val position = layoutManager.getPosition(snapView)
        if (layoutManager !is SquareLayoutManager) {
            return
        }
        layoutManager.smoothScrollToPosition(position)
    }

    fun attachToRecyclerView(recyclerView: RecyclerView?) {
        if (mRecyclerView === recyclerView) {
            return
        }
        if (mRecyclerView != null) {
            destroyCallbacks()
        }
        mRecyclerView = recyclerView
        recyclerView?.let {
            mGravityScroller = Scroller(
                it.context,
                DecelerateInterpolator()
            )
            setupCallbacks()
        }

    }

    private fun destroyCallbacks() {
        mRecyclerView?.removeOnScrollListener(mScrollListener)
        mRecyclerView?.onFlingListener = null
    }

    private fun setupCallbacks() {
        check(mRecyclerView?.onFlingListener == null) { "An instance of OnFlingListener already set." }
        mRecyclerView?.addOnScrollListener(mScrollListener)
        mRecyclerView?.onFlingListener = this
    }

    override fun onFling(velocityX: Int, velocityY: Int): Boolean {
        val layoutManager = mRecyclerView?.layoutManager ?: return false
        val minFlingVelocity = mRecyclerView?.minFlingVelocity ?: return false

        return ((abs(velocityY) > minFlingVelocity || abs(velocityX) > minFlingVelocity) && snapFromFling(
            layoutManager,
            velocityX,
            velocityY
        ))
    }

    private fun snapFromFling(
        layoutManager: RecyclerView.LayoutManager, velocityX: Int,
        velocityY: Int
    ): Boolean {
        if (layoutManager !is SquareLayoutManager) {
            return false
        }
        val targetPosition: Int =
            findTargetSnapPosition(layoutManager, velocityX, velocityY)
        if (targetPosition == RecyclerView.NO_POSITION) {
            return false
        }
        layoutManager.smoothScrollToPosition(targetPosition)
        return true
    }

    private fun findSnapView(layoutManager: RecyclerView.LayoutManager): View? {
        return findCenterView(
            layoutManager,
            getVerticalHelper(layoutManager),
            getHorizontalHelper(layoutManager)
        )
    }

    private fun findCenterView(
        layoutManager: RecyclerView.LayoutManager,
        verHelper: OrientationHelper, horHelper: OrientationHelper
    ): View? {
        val childCount = layoutManager.childCount
        if (childCount == 0) {
            return null
        }
        var closestChild: View? = null
        val verCenter = verHelper.startAfterPadding + verHelper.totalSpace / 2
        val horCenter = horHelper.startAfterPadding + horHelper.totalSpace / 2

        var verAbsClosest = Int.MAX_VALUE
        var horAbsClosest = Int.MAX_VALUE

        for (i in 0 until childCount) {
            val child = layoutManager.getChildAt(i)

            val childVerCenter = (verHelper.getDecoratedStart(child)
                    + verHelper.getDecoratedMeasurement(child) / 2)
            val verAbsDistance = abs(childVerCenter - verCenter)

            val childHorCenter = (horHelper.getDecoratedStart(child)
                    + horHelper.getDecoratedMeasurement(child) / 2)
            val horAbsDistance = abs(childHorCenter - horCenter)

            if (verAbsDistance < verAbsClosest || horAbsDistance < horAbsClosest) {
                verAbsClosest = verAbsDistance
                horAbsClosest = horAbsDistance
                closestChild = child
            }
        }
        return closestChild
    }

    private fun findTargetSnapPosition(
        layoutManager: RecyclerView.LayoutManager, velocityX: Int,
        velocityY: Int
    ): Int {
        if (layoutManager !is SquareLayoutManager) {
            return RecyclerView.NO_POSITION
        }

        val itemCount = layoutManager.itemCount
        if (itemCount == 0) {
            return RecyclerView.NO_POSITION
        }

        val currentView = findSnapView(layoutManager) ?: return RecyclerView.NO_POSITION
        val currentPosition = layoutManager.getPosition(currentView)
        if (currentPosition == RecyclerView.NO_POSITION) {
            return RecyclerView.NO_POSITION
        }

        var hDeltaJump =
            if (layoutManager.canScrollHorizontally() && ((abs(velocityY) - abs(velocityX)) > 4000).not()) {
                estimateNextPositionDiffForFling(
                    layoutManager,
                    getHorizontalHelper(layoutManager), velocityX, 0
                )
            } else {
                0
            }

        var vDeltaJump =
            if (layoutManager.canScrollVertically() && ((abs(velocityX) - abs(velocityY)) > 4000).not()) {
                estimateNextPositionDiffForFling(
                    layoutManager,
                    getVerticalHelper(layoutManager), 0, velocityY
                )
            } else {
                0
            }

        val spanCount = layoutManager.spanCount
        val rowCount = layoutManager.rowCount

        val currentHorPos = currentPosition % spanCount + 1
        val currentVerPos = currentPosition / spanCount + 1

        hDeltaJump = when {
            currentHorPos + hDeltaJump >= spanCount -> {
                abs(spanCount - currentHorPos)
            }
            currentHorPos + hDeltaJump <= 0 -> {
                -(currentHorPos - 1)
            }
            else -> {
                hDeltaJump
            }
        }

        vDeltaJump = when {
            currentVerPos + vDeltaJump >= rowCount -> {
                abs(rowCount - currentVerPos)
            }
            currentVerPos + vDeltaJump <= 0 -> {
                -(currentVerPos - 1)
            }
            else -> {
                vDeltaJump
            }
        }

        vDeltaJump = if (vDeltaJump > 0) min(3, vDeltaJump) else max(-3, vDeltaJump)
        hDeltaJump = if (hDeltaJump > 0) min(3, hDeltaJump) else max(-3, hDeltaJump)

        val deltaJump = hDeltaJump + vDeltaJump * spanCount

        if (deltaJump == 0) {
            return RecyclerView.NO_POSITION
        }

        var targetPos = currentPosition + deltaJump

        if (targetPos < 0) {
            targetPos = 0
        }
        if (targetPos >= itemCount) {
            targetPos = itemCount - 1
        }
        return targetPos
    }

    private fun estimateNextPositionDiffForFling(
        layoutManager: RecyclerView.LayoutManager,
        helper: OrientationHelper, velocityX: Int, velocityY: Int
    ): Int {
        val distances = calculateScrollDistance(velocityX, velocityY) ?: return -1
        val distancePerChild = computeDistancePerChild(layoutManager, helper)
        if (distancePerChild <= 0) {
            return 0
        }
        val distance =
            if (abs(distances[0]) > abs(distances[1])) distances[0] else distances[1]
        return (distance / distancePerChild).roundToInt()
    }

    private fun calculateScrollDistance(velocityX: Int, velocityY: Int): IntArray? {
        val outDist = IntArray(2)
        mGravityScroller?.fling(
            0,
            0,
            velocityX,
            velocityY,
            Int.MIN_VALUE,
            Int.MAX_VALUE,
            Int.MIN_VALUE,
            Int.MAX_VALUE
        )
        outDist[0] = mGravityScroller?.finalX ?: return null
        outDist[1] = mGravityScroller?.finalY ?: return null
        return outDist
    }

    private fun computeDistancePerChild(
        layoutManager: RecyclerView.LayoutManager,
        helper: OrientationHelper
    ): Float {
        val childCount = layoutManager.childCount
        if (childCount == 0) {
            return INVALID_DISTANCE
        }
        if (layoutManager !is SquareLayoutManager) {
            return INVALID_DISTANCE
        }

        return if (helper == getVerticalHelper(layoutManager)) {
            layoutManager.childHeight.toFloat()
        } else {
            layoutManager.childWidth.toFloat()
        }
    }

    private fun getVerticalHelper(layoutManager: RecyclerView.LayoutManager): OrientationHelper {
        if (mVerticalHelper == null) {
            mVerticalHelper = OrientationHelper.createVerticalHelper(layoutManager)
        }
        return mVerticalHelper!!
    }

    private fun getHorizontalHelper(
        layoutManager: RecyclerView.LayoutManager
    ): OrientationHelper {
        if (mHorizontalHelper == null) {
            mHorizontalHelper = OrientationHelper.createHorizontalHelper(layoutManager)
        }
        return mHorizontalHelper!!
    }

    companion object {
        private const val INVALID_DISTANCE = 1f
    }

}