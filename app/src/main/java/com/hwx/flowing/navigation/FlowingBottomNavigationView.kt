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
class FlowingBottomNavigationView : FrameLayout {

    private companion object {
        val DEFAULT_HEIGHT = 60F.toPixels()

        val ICON_SIZE = 45F.toPixels()
        val BOTTOM_PADDING = 0F.toPixels()

        val TOP_WAVE_PADDING = 5F.toPixels()
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    private data class MenuItem(
        val icon: Int,
    )

    private val menuItems: List<MenuItem> = listOf(
        MenuItem(R.drawable.ic_dashboard_black_24dp),
        MenuItem(R.drawable.ic_home_black_24dp),
        MenuItem(R.drawable.ic_notifications_black_24dp),
    )

    private var currentItem: MenuItem = menuItems.first() //avx:todo - del this

    private val everyIconBottomPx = DEFAULT_HEIGHT - BOTTOM_PADDING
    private val everyIconTopPx = everyIconBottomPx - ICON_SIZE
    private val selectableResId = getSelectableResId()

    init {

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
        val viewWidth = 1080 //avx:todo - edit this...
        val widthForIcon = viewWidth / menuItems.size
        var currentIconStartPx = widthForIcon / 2 - ICON_SIZE
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

            currentIconStartPx += widthForIcon
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

    private fun drawWave(canvas: Canvas) {
        val viewWidth = 1080F
        val path = Path()
        path.moveTo(0F, TOP_WAVE_PADDING)
        path.lineTo(viewWidth, TOP_WAVE_PADDING)
        path.lineTo(viewWidth, DEFAULT_HEIGHT)
        path.lineTo(0F, DEFAULT_HEIGHT)
        path.lineTo(0F, 0F)
        canvas.drawPath(path, whitePaint)
    }
}