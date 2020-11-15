package com.hwx.flowing.navigation

import android.content.res.Resources
import android.util.DisplayMetrics

/**
 * This method converts device specific pixels to density independent pixels.
 *
 * @return A float value to represent dp equivalent to px value
 */
fun Float.toDp(): Float {
    return this / (Resources.getSystem().displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
}

/**
 * This method converts dp unit to equivalent pixels, depending on device density.
 *
 * @return A float value to represent px equivalent to dp depending on device density
 */
fun Float.toPixels(): Float {
    return this * (Resources.getSystem().displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
}