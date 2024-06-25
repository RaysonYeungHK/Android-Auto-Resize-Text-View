package com.codedeco.lib.ui.widget

import android.content.Context
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.util.TypedValue
import androidx.appcompat.widget.AppCompatTextView
import com.codedeco.lib.autosizetextview.R

/**
 * For width, besides of wrap_content, it could be
 * - fixed value (e.g. 20dp, 40dp, etc)
 * - match_constraint
 * - match_parent
 * If container has dynamic width for holding the text, it doesn't make sense to have auto resize function for text
 */
class AutoSizeTextView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    companion object {
        // Reference: TextView.UNSET_AUTO_SIZE_UNIFORM_CONFIGURATION_VALUE
        private const val UNSET_VALUE = -1f
    }

    // Besides of text, it is not recommended to update those values in runtime since it results in very bad UX
    private var minTextSize: Float = UNSET_VALUE
    private var maxTextSize: Float = UNSET_VALUE
    private var originalTextSize: Float = UNSET_VALUE

    private var isDirty: Boolean = true

    init {
        val ta = context.obtainStyledAttributes(
            attrs,
            R.styleable.AutoSizeTextView,
            defStyleAttr,
            0
        )
        minTextSize = ta.getDimension(
            R.styleable.AutoSizeTextView_android_autoSizeMinTextSize,
            UNSET_VALUE
        )
        maxTextSize = ta.getDimension(
            R.styleable.AutoSizeTextView_android_autoSizeMaxTextSize,
            UNSET_VALUE
        )
        originalTextSize = ta.getDimension(
            R.styleable.AutoSizeTextView_android_textSize,
            UNSET_VALUE
        )
        ta.recycle()
    }

    override fun setText(text: CharSequence?, type: BufferType?) {
        super.setText(text, type)
        isDirty = true
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        // Calculation is done after onMeasure, but before onDraw
        if (isDirty || changed) {
            resizeText(text.toString())
        }
    }

    private fun resizeText(text: String) {
        val width = right - left - compoundPaddingLeft - compoundPaddingRight

        // We don't care about height, since it could be updated dynamically
        if (width <= 0) {
            return
        }

        // If autoSizeMinTextSize is not set, 1 px is used since it is required for calculation
        val min = maxOf(1f, minTextSize)
        // Pick largest value of textSize and autoSizeMaxTextSize
        // It doesn't make sense textSize != autoSizeMaxTextSize, if either is set, it should be upper limit of scale size
        val max = maxOf(maxTextSize, originalTextSize)

        if (min > max) {
            throw IllegalArgumentException("minimum text size should not larger than maximum text size")
        }

        // Copy current paint to prevent any pollution from other thread
        val paint = TextPaint(paint).apply {
            // Reset textSize from largest
            textSize = max
        }

        // Calculate expected height, based on largest font, maxLines and line height
        val expectedHeight = with(paint.fontMetrics) {
            descent - ascent
        } * maxLines

        // Check with biggest size and see if we really need to resize
        var measuredHeight = StaticLayout.Builder.obtain(
            text,
            0,
            text.length,
            paint,
            width
        ).setIncludePad(includeFontPadding)
            .setLineSpacing(lineSpacingExtra, lineSpacingMultiplier)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .build()
            .height
        // No need to resize, just update the text size and return
        if (measuredHeight <= expectedHeight) {
            updateTextSize(max)
            return
        }

        // Using binary search to find the most fit text size for display
        var low = min.toInt()
        var high = max.toInt()

        var targetTextSize = low + (high - low) / 2
        while (low <= high) {
            paint.textSize = targetTextSize.toFloat()
            measuredHeight = StaticLayout.Builder.obtain(
                text,
                0,
                text.length,
                paint,
                width
            ).setIncludePad(includeFontPadding)
                .setLineSpacing(lineSpacingExtra, lineSpacingMultiplier)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .build().height
            if (measuredHeight <= expectedHeight) {
                low = targetTextSize + 1
            } else {
                high = targetTextSize - 1
            }
            targetTextSize = low + (high - low) / 2
        }

        updateTextSize(targetTextSize.toFloat())
        isDirty = false
    }

    private fun updateTextSize(textSizeInPixel: Float) {
        // Update text size for text view to have better view bounds calculation from system
        setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizeInPixel)
        // Immediately calling requestLayout() to trigger onMeasure(Int, Int) cannot update view bounds properly
        // Especially in recyclerview
        post {
            requestLayout()
        }
    }
}