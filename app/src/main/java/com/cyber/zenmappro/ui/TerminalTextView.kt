package com.cyber.zenmappro.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

/**
 * Custom TextView with terminal/hacker aesthetic
 * Features: Glow effect, blinking cursor option, monospace font
 */
class TerminalTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var showCursor = false
    private var cursorVisible = true
    private var cursorBlinkRate = 500L // milliseconds
    
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#39FF14")
        style = Paint.Style.STROKE
        strokeWidth = 1f
        maskFilter = android.graphics.BlurMaskFilter(5f, android.graphics.BlurMaskFilter.Blur.NORMAL)
    }
    
    private val cursorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#39FF14")
        style = Paint.Style.FILL
    }
    
    init {
        // Set default terminal styling
        setTextColor(Color.parseColor("#39FF14"))
        setBackgroundColor(Color.parseColor("#0A0A0A"))
        setTypeface(android.graphics.Typeface.MONOSPACE)
        setPadding(16, 16, 16, 16)
        
        // Parse custom attributes if needed
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.TerminalTextView)
            showCursor = typedArray.getBoolean(R.styleable.TerminalTextView_showCursor, false)
            typedArray.recycle()
        }
        
        if (showCursor) {
            startCursorBlink()
        }
    }
    
    private fun startCursorBlink() {
        postDelayed(cursorBlinkRunnable, cursorBlinkRate)
    }
    
    private val cursorBlinkRunnable = object : Runnable {
        override fun run() {
            cursorVisible = !cursorVisible
            invalidate()
            postDelayed(this, cursorBlinkRate)
        }
    }
    
    override fun onDraw(canvas: Canvas) {
        // Draw glow effect behind text
        val textColor = currentTextColor
        setTextColor(Color.TRANSPARENT)
        super.onDraw(canvas)
        setTextColor(textColor)
        
        // Draw normal text
        super.onDraw(canvas)
        
        // Draw cursor if enabled
        if (showCursor && cursorVisible) {
            val textWidth = paint.measureText(text.toString())
            val cursorX = paddingLeft.toFloat() + textWidth + 4f
            val cursorHeight = lineHeight.toFloat()
            canvas.drawRect(cursorX, paddingTop.toFloat(), cursorX + 3f, cursorHeight, cursorPaint)
        }
    }
    
    fun appendLog(message: String) {
        val currentTime = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault())
            .format(java.util.Date())
        val newLine = "[$currentTime] $message\n"
        text = text.toString() + newLine
        
        // Auto-scroll to bottom
        post {
            parent?.requestChildFocus(this, this)
        }
    }
    
    fun clearLog() {
        text = ""
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(cursorBlinkRunnable)
    }
}
