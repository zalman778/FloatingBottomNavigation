package com.hwx.flowing.navigation

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.FrameLayout
import android.widget.ImageView
import com.hwx.myapplication.R


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
    }

    private data class MenuItem(
        val icon: Int,
    )

    private val menuItems: List<MenuItem> = listOf(
        MenuItem(R.drawable.ic_dashboard_black_24dp),
        MenuItem(R.drawable.ic_home_black_24dp),
        MenuItem(R.drawable.ic_notifications_black_24dp),
        MenuItem(R.drawable.ic_dashboard_black_24dp),
        MenuItem(R.drawable.ic_home_black_24dp),
    )

    private var currentItem: MenuItem = menuItems.first() //avx:todo - del this

    private val everyIconBottomPx = DEFAULT_HEIGHT - BOTTOM_PADDING
    private val everyIconTopPx = everyIconBottomPx - ICON_SIZE
    private val selectableResId = getSelectableResId()

    init {
        setWillNotDraw(false)
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

    private var iconAreaWidth: Int = 0

    //avx:todo - make selectable as circle..
    private fun drawIcons() {
        val resources = context.resources
        val viewWidth = 1080 //avx:todo - edit this...
        iconAreaWidth = viewWidth / menuItems.size
        var currentIconStartPx = iconAreaWidth / 2 - ICON_SIZE
        menuItems.forEach {

            val image = ImageView(context)
            image.layoutParams = LayoutParams((ICON_SIZE * 2).toInt(), ICON_SIZE.toInt()).apply {
                setMargins(
                    currentIconStartPx.toInt(),
                    everyIconTopPx.toInt(),
                    currentIconStartPx.toInt() + (ICON_SIZE * 2).toInt(),
                    everyIconBottomPx.toInt(),
                )
            }
            val drawable = resources.getDrawable(it.icon, context.theme)
            image.setImageDrawable(drawable)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                image.foreground = context.getDrawable(selectableResId)
            }
            image.isClickable = true
            image.adjustViewBounds = true
            image.scaleType = ImageView.ScaleType.CENTER_INSIDE
            addView(image)

            currentIconStartPx += iconAreaWidth
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawWave(canvas)
    }

    private val whitePaint = Paint().apply {
        isAntiAlias = true
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val backPath = Path()

    private var currentIdx: Int = 2

    private fun drawWave(canvas: Canvas) {
        backPath.moveTo(0F, 0F)
        backPath.lineTo(0F, height.toFloat())
        val preCubicShift = iconAreaWidth / 4F
        val halfOfIconAreaWidth = iconAreaWidth / 2F
        backPath.lineTo(currentIdx * iconAreaWidth - preCubicShift, height.toFloat())

        val currentIconMiddleX = iconAreaWidth * (currentIdx + 0.5F)
        backPath.cubicTo(
            currentIconMiddleX - preCubicShift, height.toFloat(),
            currentIconMiddleX - halfOfIconAreaWidth, 0f,
            currentIconMiddleX, ACTIVE_ICON_VERTICAL_SHIFT / 2
        )
        backPath.cubicTo(
            currentIconMiddleX + halfOfIconAreaWidth, 0f,
            currentIconMiddleX + preCubicShift, height.toFloat(),
            (currentIdx + 1) * iconAreaWidth + preCubicShift, height.toFloat()
        )
        backPath.lineTo(width.toFloat(), height.toFloat())
        backPath.lineTo(width.toFloat(), 0f)
        backPath.lineTo(0F, 0F)
        canvas.drawPath(backPath, whitePaint)
        backPath.reset()
    }
}