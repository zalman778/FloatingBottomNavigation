package com.hwx.flowing.navigation

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Handler
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationSet
import android.widget.FrameLayout
import android.widget.ImageView
import com.hwx.myapplication.R
import java.lang.ref.WeakReference


/**
 * stolen from here - https://cdn.dribbble.com/users/824356/videos/13100/mockup_2.mp4
 */
class FlowingBottomNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private companion object {
        val DEFAULT_HEIGHT = 60F.toPixels()

        val ICON_SIZE = 45F.toPixels()
        val BOTTOM_PADDING = 0F.toPixels()

        val TOP_WAVE_PADDING = 5F.toPixels()
        val ACTIVE_ICON_VERTICAL_SHIFT = 10F.toPixels()
        val ANIM_ICON_SHIFT_DURATION = 300L
    }

    data class MenuItem(
        val icon: Int,
    )

    private val menuItems: List<MenuItem> = listOf(
        MenuItem(R.drawable.ic_dashboard_black_24dp),
        MenuItem(R.drawable.ic_home_black_24dp),
        MenuItem(R.drawable.ic_notifications_black_24dp),
        MenuItem(R.drawable.ic_account_balance_24px),
        MenuItem(R.drawable.ic_assignment_ind_24px),
    )

    private var currentIdx: Int = 4

    private val everyIconBottomPx = DEFAULT_HEIGHT - BOTTOM_PADDING
    private val everyIconTopPx = everyIconBottomPx - ICON_SIZE
    private val selectableResId = getSelectableResId()

    private var iconAreaWidth: Int = 0
    private val whitePaint = Paint().apply {
        isAntiAlias = true
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val backPath = Path()
    private val weakIconViews = HashMap<Int, WeakReference<View>>()
    private var preCubicShift = 0f
    private var halfOfIconAreaWidth = 0f
    private var currentIconMiddleX = 0f

    init {
        setWillNotDraw(false)
        updateIconWidth()
        setCurrentItem(MenuItem(R.drawable.ic_notifications_black_24dp))
    }

    private fun updateIconWidth() {
        val viewWidth = 1080 //avx:todo - edit this...
        iconAreaWidth = viewWidth / menuItems.size
        preCubicShift = iconAreaWidth / 4F
        halfOfIconAreaWidth = iconAreaWidth / 2F
    }

    fun setCurrentItem(item: MenuItem) {
        val newIdx = menuItems.indexOf(item).takeIf { it != -1 } ?: return
        animateWave(currentIdx, newIdx)
        currentIdx = newIdx
        currentIconMiddleX = getIconMiddleX(currentIdx)
        invalidate()
    }

    private fun getIconMiddleX(idx: Int) = iconAreaWidth * (idx + 0.5F)

    private fun animateWave(fromIdx: Int, toIdx: Int) {
        val fromValue = getIconMiddleX(fromIdx)
        val toValue = getIconMiddleX(toIdx)

        val waveAnimator = ValueAnimator.ofFloat(fromValue, toValue).apply {
            duration = ANIM_ICON_SHIFT_DURATION
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                val newX = it.animatedValue as? Float ?: return@addUpdateListener
                currentIconMiddleX = newX
                invalidate()
            }
        }

        val fromIcon = weakIconViews[fromIdx]?.get() ?: return
        val fromIconAnimator = ObjectAnimator.ofFloat(0f, height.toFloat()).apply {
            duration = ANIM_ICON_SHIFT_DURATION / 2
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                val newValue = it.animatedValue as? Float ?: return@addUpdateListener
                fromIcon.translationY = newValue
            }
        }

        val toIcon = weakIconViews[toIdx]?.get() ?: return
        val toIconAnimator = ObjectAnimator.ofFloat(height.toFloat(), 0f).apply {
            duration = ANIM_ICON_SHIFT_DURATION / 2
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                val newValue = it.animatedValue as? Float ?: return@addUpdateListener
                toIcon.translationY = newValue
            }
        }

        waveAnimator.start()
        fromIconAnimator.start()


        Handler().postDelayed({
            toIconAnimator.start()
        }, ANIM_ICON_SHIFT_DURATION / 2)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        layoutParams = layoutParams.apply {
            height = DEFAULT_HEIGHT.toInt()
            width = 1080
        }

        drawIcons()
    }

    private fun getSelectableResId(): Int {
        val outValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        return outValue.resourceId
    }

    //avx:todo - make selectable as circle..
    private fun drawIcons() {
        val resources = context.resources

        var currentIconStartPx = iconAreaWidth / 2 - ICON_SIZE
        menuItems.forEachIndexed { idx, item ->

            val image = ImageView(context)
            image.layoutParams = LayoutParams((ICON_SIZE * 2).toInt(), ICON_SIZE.toInt()).apply {
                setMargins(
                    currentIconStartPx.toInt(),
                    everyIconTopPx.toInt(),
                    currentIconStartPx.toInt() + (ICON_SIZE * 2).toInt(),
                    everyIconBottomPx.toInt(),
                )
            }
            val drawable = resources.getDrawable(item.icon, context.theme)
            image.setImageDrawable(drawable)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                image.foreground = context.getDrawable(selectableResId)
            }
            image.isClickable = true
            image.adjustViewBounds = true
            image.scaleType = ImageView.ScaleType.CENTER_INSIDE
            image.setOnClickListener {
                setCurrentItem(item)
            }
            addView(image)
            weakIconViews[idx] = WeakReference(image)
            currentIconStartPx += iconAreaWidth
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawWave(canvas)
    }

    private fun drawWave(canvas: Canvas) {
        backPath.moveTo(0F, height.toFloat())
        backPath.lineTo(0F, 0F)
        backPath.lineTo(currentIconMiddleX - halfOfIconAreaWidth - preCubicShift, 0F)
        drawCircleCutCubics(currentIconMiddleX, preCubicShift, halfOfIconAreaWidth)
        backPath.lineTo(width.toFloat(), 0F)
        backPath.lineTo(width.toFloat(), height.toFloat())
        backPath.lineTo(0F, height.toFloat())
        canvas.drawPath(backPath, whitePaint)
        backPath.reset()
    }

    private fun drawCircleCutCubics(
        currentIconMiddleX: Float,
        preCubicShift: Float,
        halfOfIconAreaWidth: Float
    ) {
        backPath.cubicTo(
            currentIconMiddleX - preCubicShift, 0F,
            currentIconMiddleX - halfOfIconAreaWidth, height.toFloat(),
            currentIconMiddleX, height.toFloat() - ACTIVE_ICON_VERTICAL_SHIFT / 2
        )
        backPath.cubicTo(
            currentIconMiddleX + halfOfIconAreaWidth, height.toFloat(),
            currentIconMiddleX + preCubicShift, 0F,
            currentIconMiddleX + halfOfIconAreaWidth + preCubicShift, 0F
        )
    }
}