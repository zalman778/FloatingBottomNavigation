package com.hwx.flowing.navigation

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.os.Handler
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.view.ViewOutlineProvider
import android.view.ViewTreeObserver.OnPreDrawListener
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.view.SupportMenuInflater
import androidx.appcompat.view.menu.MenuBuilder
import com.google.android.material.bottomnavigation.BottomNavigationMenu
import com.hwx.example.R
import java.lang.ref.WeakReference
import kotlin.math.abs

@SuppressLint("RestrictedApi")
class FlowingBottomNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), FlowingBottomNavigationConfigurator {

    private companion object {
        fun Float.toPixels() = this *
                (Resources.getSystem().displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)

        val DEFAULT_HEIGHT = 60F.toPixels()
        val ICON_SIZE = 50F.toPixels()
        val BOTTOM_PADDING = 0F.toPixels()
        val WAVE_BOTTOM_STATE_HEIGHT = 8F.toPixels()
        val WAVE_TOP_PADDING = 10F.toPixels()
        const val ANIM_ICON_SHIFT_DURATION = 400L
    }

    interface OnItemSelectedListener {
        fun onItemSelected(item: MenuItem)
    }

    var listener: OnItemSelectedListener? = null
    var animationDuration = ANIM_ICON_SHIFT_DURATION
    var currentIdx: Int = 0
        private set

    private val menuItems = mutableListOf<MenuItem>()
    private val everyIconBottomPx = DEFAULT_HEIGHT - BOTTOM_PADDING
    private val everyIconTopPx = everyIconBottomPx - ICON_SIZE
    private val selectableResId = getSelectableResId()

    private var iconAreaWidth: Int = 0
    private val whitePaint = Paint().apply {
        isAntiAlias = true
        color = Color.WHITE
        style = Paint.Style.FILL
        setShadowLayer(2f, 0f, 0f, Color.BLACK)
    }

    private val backPath = Path()
    private val weakIconViews = HashMap<Int, WeakReference<View>>()
    private var weakActionIcon: WeakReference<ImageView>? = null

    private var preCubicShift = 0f
    private var halfOfIconAreaWidth = 0f
    private var currentIconMiddleX = 0f
    private val pointsForIcons = mutableMapOf<Int, Float>()

    private var waveAnimator: Animator? = null
    private var actionAnimatorFirst: Animator? = null
    private var actionAnimatorSecond: Animator? = null

    private var menu: MenuBuilder? = null
    private val menuInflater by lazy { SupportMenuInflater(getContext()) }
    private var menuResId: Int = 0

    init {
        setWillNotDraw(false)
        menu = BottomNavigationMenu(getContext())

        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.FlowingBottomNavigationView, 0, 0
        ).apply {
            menuResId = getResourceId(R.styleable.FlowingBottomNavigationView_menuResId, 0)
                .takeIf { it != 0 }
                ?: throw IllegalArgumentException("No menu id provided, please, use app:menuResId attribute")
        }.recycle()


        menuInflater.inflate(menuResId, menu)

        menu?.nonActionItems?.map { it as MenuItem }?.let {
            menuItems.addAll(it)
        }

        doBeforeDraw {
            updateIconWidth()
            drawIcons()

            val middleIdx = menuItems.size / 2
            setCurrentItem(middleIdx, false)
        }
    }

    override fun setCurrentItem(newIdx: Int, isAnimate: Boolean) {
        if (newIdx < 0 || newIdx > menuItems.size - 1)
            throw IllegalArgumentException("No such menu item")
        if (newIdx == currentIdx) return
        if (isAnimate) {
            animateWave(newIdx)
        }
        currentIdx = newIdx
        currentIconMiddleX = getIconMiddleX(currentIdx)
        onMiddleXChanged(currentIconMiddleX)
        invalidate()
    }

    override fun setCurrentItem(item: MenuItem, isAnimate: Boolean) {
        val idx = menuItems.indexOf(item).takeIf { it != -1 }
            ?: throw IllegalArgumentException("No such menu item")
        setCurrentItem(idx, isAnimate)
        listener?.onItemSelected(item)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        layoutParams = layoutParams.apply {
            height = DEFAULT_HEIGHT.toInt()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawWave(canvas)
    }

    private fun updateIconWidth() {
        iconAreaWidth = measuredWidth / menuItems.size
        preCubicShift = iconAreaWidth / 3F
        halfOfIconAreaWidth = iconAreaWidth / 2F
        for (idx in 0..menuItems.size) {
            pointsForIcons[idx] = iconAreaWidth * idx + iconAreaWidth / 2f
        }
    }

    private fun getIconMiddleX(idx: Int) = iconAreaWidth * (idx + 0.5F)

    private fun animateWave(toIdx: Int) {
        cancelAnimators()
        val targetValue = getIconMiddleX(toIdx)
        createAnimators(targetValue)
        startAnimators(toIdx)
    }

    private fun cancelAnimators() {
        waveAnimator?.cancel()
        actionAnimatorFirst?.cancel()
        actionAnimatorSecond?.cancel()
    }

    private fun startAnimators(toIdx: Int) {
        waveAnimator?.start()
        actionAnimatorFirst?.start()

        Handler().postDelayed({
            updateActionIconImage(toIdx)
            actionAnimatorSecond?.start()
        }, animationDuration / 2)
    }

    private fun createAnimators(toValue: Float) {
        waveAnimator = ValueAnimator.ofFloat(currentIconMiddleX, toValue).apply {
            duration = animationDuration
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                val newX = it.animatedValue as? Float ?: return@addUpdateListener
                currentIconMiddleX = newX
                onMiddleXChanged(newX)
                invalidate()
            }
        }

        val actionIcon = weakActionIcon?.get() ?: return
        actionAnimatorFirst =
            ObjectAnimator.ofFloat(actionIcon.translationY, height.toFloat() * 1.5f).apply {
                duration = animationDuration / 2
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener {
                    val newValue = it.animatedValue as? Float ?: return@addUpdateListener
                    actionIcon.translationY = newValue
                }
            }

        actionAnimatorSecond = ObjectAnimator.ofFloat(height.toFloat() * 1.5f, 0f).apply {
            duration = animationDuration / 2
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                val newValue = it.animatedValue as? Float ?: return@addUpdateListener
                actionIcon.translationY = newValue
            }
        }
    }

    private fun updateActionIconImage(toIdx: Int) {
        val drawable = menuItems.getOrNull(toIdx)?.icon ?: return
        val actionImage = weakActionIcon?.get() ?: return
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


    private fun getSelectableResId(): Int {
        val outValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        return outValue.resourceId
    }

    private fun drawIcons() {
        removeAllViews()
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
            image.setImageDrawable(item.icon)
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

        actionImage.setImageDrawable(menuItems.first().icon)
        addView(actionImage)
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
        val endX = (currentIconMiddleX + halfOfIconAreaWidth + preCubicShift)
            .takeIf { it < width }
            ?: width.toFloat()
        backPath.cubicTo(
            currentIconMiddleX + halfOfIconAreaWidth, height.toFloat(),
            currentIconMiddleX + preCubicShift, WAVE_TOP_PADDING,
            endX, WAVE_TOP_PADDING
        )
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

    private fun View.doBeforeDraw(
        action: () -> Unit,
    ) {
        val preDrawListener: OnPreDrawListener =
            object : OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    viewTreeObserver.removeOnPreDrawListener(this)
                    action()
                    return true
                }
            }
        viewTreeObserver.addOnPreDrawListener(preDrawListener)
    }
}

interface FlowingBottomNavigationConfigurator {
    fun setCurrentItem(newIdx: Int, isAnimate: Boolean = true)
    fun setCurrentItem(item: MenuItem, isAnimate: Boolean = true)
}
