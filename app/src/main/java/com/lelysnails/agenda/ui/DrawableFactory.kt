package com.lelysnails.agenda.ui

import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.View

object DrawableFactory {

    fun rounded(
        view: View,
        fill: Int,
        stroke: Int? = null,
        strokeWidthDp: Float = 1.5f,
        radiusDp: Float,
        dash: Boolean = false
    ): GradientDrawable {
        val dm = view.resources.displayMetrics
        val radius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, radiusDp, dm)
        val strokeW = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, strokeWidthDp, dm).toInt()
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(fill)
            cornerRadius = radius
            if (stroke != null) {
                if (dash) {
                    setStroke(
                        strokeW, stroke,
                        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, dm),
                        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3f, dm)
                    )
                } else {
                    setStroke(strokeW, stroke)
                }
            }
        }
    }

    fun gradient(
        view: View,
        start: Int,
        end: Int,
        radiusDp: Float,
        stroke: Int? = null
    ): GradientDrawable {
        val dm = view.resources.displayMetrics
        val radius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, radiusDp, dm)
        return GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(start, end)
        ).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            if (stroke != null) {
                val strokeW = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1.5f, dm).toInt()
                setStroke(strokeW, stroke)
            }
        }
    }

    fun oval(fill: Int, stroke: Int? = null, strokeWidthPx: Int = 3): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(fill)
            if (stroke != null) setStroke(strokeWidthPx, stroke)
        }
    }
}
