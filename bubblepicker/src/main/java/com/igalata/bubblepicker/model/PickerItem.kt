package com.igalata.bubblepicker.model

import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.support.annotation.ColorInt

/**
 * Created by irinagalata on 1/19/17.
 */
data class PickerItem @JvmOverloads constructor(
        var id: String = "",
        var title: String? = null,
        var subTitle: String? = null,
        var icon: Drawable? = null,
        var iconOnTop: Boolean = true,
        @ColorInt var color: Int? = null,
        var gradient: BubbleGradient? = null,
        var overlayAlpha: Float = 0.5f,
        var typeface: Typeface = Typeface.DEFAULT_BOLD,
        var subTextTypeFace: Typeface = Typeface.DEFAULT,
        @ColorInt var textColor: Int? = null,
        @ColorInt var subTextColor: Int? = null,
        var textSize: Float = 40f,
        var subTextSize: Float = 30f,
        var backgroundImage: Drawable? = null,
        var isSelected: Boolean = false,
        var circleScale: Float = 1f,
        var customData: Any? = null
)