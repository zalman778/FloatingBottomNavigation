package com.hwx.flowing.navigation

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import com.hwx.myapplication.R

/**
 * stolen from here - https://cdn.dribbble.com/users/824356/videos/13100/mockup_2.mp4
 */
class FlowingBottomNavigationView: View {

    private companion object {
        val DEFAULT_HEIGHT = 60F.toPixels()

        val ICON_SIZE = 30F.toPixels()
        val BOTTOM_PADDING = 5F.toPixels()
    }

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    constructor(
        context: Context?,
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

    private var imagesList = emptyList<Drawable>()

    init {
        val resources = context.resources
        imagesList = menuItems.map { resources.getDrawable(it.icon, context.theme) }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthValue = MeasureSpec.getSize(widthMeasureSpec)
        //val widthResolved = resolveSize(widthValue, widthMode)
        val height = DEFAULT_HEIGHT.toInt()
        setMeasuredDimension(widthValue, height)
    }

    private val iconBounds = Rect(0, 0, 0, 0)

    override fun onDraw(canvas: Canvas) {

        //draw icons all over nav panel
        val widthForIcon = measuredWidth / imagesList.size

        val everyIconBottomPx = DEFAULT_HEIGHT - BOTTOM_PADDING
        val everyIconTopPx = everyIconBottomPx - ICON_SIZE
        var currentIconStartPx = widthForIcon / 2 - ICON_SIZE / 2
        imagesList.forEach {
            val left = currentIconStartPx
            val right = left + ICON_SIZE
            iconBounds.set(left.toInt(), everyIconTopPx.toInt(), right.toInt(), everyIconBottomPx.toInt())
            it.bounds = iconBounds
            it.draw(canvas)
            currentIconStartPx += widthForIcon
        }
    }
}