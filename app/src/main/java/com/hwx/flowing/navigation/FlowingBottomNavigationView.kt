package com.hwx.flowing.navigation

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.Handler
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import com.hwx.myapplication.R
import java.lang.ref.WeakReference
import kotlin.math.abs


/**
 * stolen from here - https://cdn.dribbble.com/users/824356/videos/13100/mockup_2.mp4
 *
 */
class FlowingBottomNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private companion object {
        val DEFAULT_HEIGHT = 60F.toPixels()

        val ICON_SIZE = 50F.toPixels()
        val BOTTOM_PADDING = 0F.toPixels()

        val WAVE_BOTTOM_STATE_HEIGHT = 10F.toPixels()
        val WAVE_TOP_PADDING = 10F.toPixels()
        const val ANIM_ICON_SHIFT_DURATION = 400L
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
        setShadowLayer(4f, 0f, 0f, Color.BLACK)
    }

    private val backPath = Path()
    private val weakIconViews = HashMap<Int, WeakReference<View>>()
    private var weakActionIcon: WeakReference<ImageView>? = null

    private var preCubicShift = 0f
    private var halfOfIconAreaWidth = 0f
    private var currentIconMiddleX = 0f

    private var waveAnimator: Animator? = null
    private var fromIconAnimatorFirst: Animator? = null
    private var toIconAnimatorFirst: Animator? = null
    private var fadeIconsAnimators: Set<Animator>? = null

    private val pointsForIcons = mutableMapOf<Int, Float>()

    init {
        setWillNotDraw(false)
    }

    private fun updateIconWidth() {
        iconAreaWidth = width / menuItems.size
        preCubicShift = iconAreaWidth / 3F
        halfOfIconAreaWidth = iconAreaWidth / 2F
        for (idx in 0..menuItems.size) {
            pointsForIcons[idx] = iconAreaWidth * idx + iconAreaWidth / 2f
        }
    }

    fun setCurrentItem(item: MenuItem, isAnimate: Boolean = true) {
        val newIdx = menuItems.indexOf(item).takeIf { it != -1 } ?: return
        if (newIdx == currentIdx) return
        if (isAnimate) {
            animateWave(newIdx)
        }
        currentIdx = newIdx
        currentIconMiddleX = getIconMiddleX(currentIdx)
        onMiddleXChanged(currentIconMiddleX)
        invalidate()
    }

    private fun getIconMiddleX(idx: Int) = iconAreaWidth * (idx + 0.5F)

    private fun animateWave(toIdx: Int) {
        waveAnimator?.cancel()
        fromIconAnimatorFirst?.cancel()
        toIconAnimatorFirst?.cancel()
        fadeIconsAnimators?.forEach { it.cancel() }

        val toValue = getIconMiddleX(toIdx)

        waveAnimator = ValueAnimator.ofFloat(currentIconMiddleX, toValue).apply {
            duration = ANIM_ICON_SHIFT_DURATION
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                val newX = it.animatedValue as? Float ?: return@addUpdateListener
                currentIconMiddleX = newX
                onMiddleXChanged(newX)
                invalidate()
            }
        }

        val actionIcon = weakActionIcon?.get() ?: return
        //avx:todo - cancel animators..
        val actionAnimatorFirst =
            ObjectAnimator.ofFloat(actionIcon.translationY, height.toFloat() * 1.5f).apply {
                duration = ANIM_ICON_SHIFT_DURATION / 2
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener {
                    val newValue = it.animatedValue as? Float ?: return@addUpdateListener
                    actionIcon.translationY = newValue
                }
            }

        val actionAnimatorSecond = ObjectAnimator.ofFloat(height.toFloat() * 1.5f, 0f).apply {
            duration = ANIM_ICON_SHIFT_DURATION / 2
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                val newValue = it.animatedValue as? Float ?: return@addUpdateListener
                actionIcon.translationY = newValue
            }
        }
        waveAnimator?.start()
        actionAnimatorFirst?.start()

        Handler().postDelayed({
            updateActionIconImage(toIdx)
            actionAnimatorSecond?.start()
        }, ANIM_ICON_SHIFT_DURATION / 2)
    }

    private fun updateActionIconImage(toIdx: Int) {
        val imageId = menuItems.getOrNull(toIdx)?.icon ?: return
        val actionImage = weakActionIcon?.get() ?: return
        val drawable = resources.getDrawable(imageId, context.theme)
        actionImage.setImageDrawable(drawable)
    }

    private fun onMiddleXChanged(newX: Float) {
        pointsForIcons.forEach {
            val icon = weakIconViews[it.key]?.get() ?: return@forEach
            val distance = abs(newX - it.value)
            val newIconAlpha = when {
                distance < iconAreaWidth / 2 -> 0f
                distance < iconAreaWidth -> {
                    (distance - (iconAreaWidth / 2)) / (iconAreaWidth / 2)
                }
                else -> {
                    1f
                }
            }
            icon.alpha = newIconAlpha
            icon.translationY = (1 - newIconAlpha) * DEFAULT_HEIGHT / 6
        }
        weakActionIcon?.get()?.translationX = newX - ICON_SIZE / 2
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        layoutParams = layoutParams.apply {
            height = DEFAULT_HEIGHT.toInt()
        }

    }

    private fun getSelectableResId(): Int {
        val outValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        return outValue.resourceId
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        updateIconWidth()
        drawIcons()
        setCurrentItem(MenuItem(R.drawable.ic_notifications_black_24dp), false)
    }

    private fun drawIcons() {
        val resources = context.resources

        var currentIconStartPx = iconAreaWidth / 2 - ICON_SIZE / 2
        menuItems.forEachIndexed { idx, item ->

            val image = ImageView(context)
            image.layoutParams = LayoutParams(ICON_SIZE.toInt(), ICON_SIZE.toInt()).apply {
                setMargins(
                    currentIconStartPx.toInt(),
                    everyIconTopPx.toInt(),
                    currentIconStartPx.toInt() + ICON_SIZE.toInt(),
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
            image.addRoundedCorners(ICON_SIZE / 2)
            addView(image)
            weakIconViews[idx] = WeakReference(image)
            currentIconStartPx += iconAreaWidth
        }

        val actionImage = ImageView(context)
        weakActionIcon = WeakReference(actionImage)
        actionImage.layoutParams = LayoutParams(ICON_SIZE.toInt(), ICON_SIZE.toInt()).apply {
            gravity = Gravity.START
        }
        actionImage.setBackgroundColor(Color.WHITE)
        actionImage.elevation = 2f.toPixels()
        actionImage.addRoundedCorners(ICON_SIZE / 2)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            actionImage.foreground = context.getDrawable(selectableResId)
        }
        actionImage.isClickable = true
        actionImage.adjustViewBounds = true
        actionImage.scaleType = ImageView.ScaleType.CENTER_INSIDE

        val drawable = resources.getDrawable(menuItems.first().icon, context.theme)
        actionImage.setImageDrawable(drawable)
        addView(actionImage)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawWave(canvas)
    }

    private fun ImageView.addRoundedCorners(radius: Float) {
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View?, outline: Outline?) {
                if (outline == null || view == null) return
                outline.setRoundRect(0, 0, view.width, view.height, radius)
            }
        }
        clipToOutline = true
    }

    private fun drawWave(canvas: Canvas) {
        backPath.moveTo(0F, height.toFloat())
        backPath.lineTo(0F, WAVE_TOP_PADDING)
        val startX =
            (currentIconMiddleX - halfOfIconAreaWidth - preCubicShift).takeIf { it > 0 } ?: 0f
        backPath.lineTo(startX, WAVE_TOP_PADDING)
        drawCircleCutCubics(currentIconMiddleX, preCubicShift, halfOfIconAreaWidth)
        backPath.lineTo(width.toFloat(), WAVE_TOP_PADDING)
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
            currentIconMiddleX - preCubicShift, WAVE_TOP_PADDING,
            currentIconMiddleX - halfOfIconAreaWidth, height.toFloat(),
            currentIconMiddleX, height.toFloat() - WAVE_BOTTOM_STATE_HEIGHT / 2
        )
        val endX = (currentIconMiddleX + halfOfIconAreaWidth + preCubicShift).takeIf { it < width }
            ?: width.toFloat()
        backPath.cubicTo(
            currentIconMiddleX + halfOfIconAreaWidth, height.toFloat(),
            currentIconMiddleX + preCubicShift, WAVE_TOP_PADDING,
            endX, WAVE_TOP_PADDING
        )
    }
}
