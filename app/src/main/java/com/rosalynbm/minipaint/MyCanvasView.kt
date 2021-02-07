package com.rosalynbm.minipaint

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.core.content.res.ResourcesCompat
import kotlin.math.abs

private const val STROKE_WIDTH = 12f // has to be float

class MyCanvasView(context: Context): View(context) {

    // Bitmap and canvas for caching what has been drawn before.
    private lateinit var extraCanvas: Canvas
    private lateinit var extraBitmap: Bitmap

    private val backgroundColor = ResourcesCompat.getColor(resources, R.color.colorBackground, null)
    // Hold the color to draw with
    private val drawColor = ResourcesCompat.getColor(resources, R.color.colorPaint, null)

    // Set up the paint with which to draw.
    private val paint = Paint().apply {
        color = drawColor
        // Smooths out edges of what is drawn without affecting shape.
        isAntiAlias = true
        // Dithering affects how colors with higher-precision than the device are down-sampled.
        isDither = true
        style = Paint.Style.STROKE // default: FILL
        // Specifies how lines and curve segments join a stroked path
        strokeJoin = Paint.Join.ROUND // default: MITER
        // Sets the shape of the end of the line to be a cap. Paints.cap specifies how to the beginning
        // and ending of stroked lines and path look
        strokeCap = Paint.Cap.ROUND // default: BUTT
        strokeWidth = STROKE_WIDTH // default: Hairline-width (really thin)
    }

    // Store the path that is being drawn when following the user's touch on the screen
    private var path = Path()
    // For caching the x and y coordinates of the current touch event (the MotionEvent coordinates)
    private var motionTouchEventX = 0f
    private var motionTouchEventY = 0f
    // To cache the latest x and y values. After the user stops moving and lifts their touch,
    // these are the starting point for the next path (the next segment of the line to draw).
    private var currentX = 0f
    private var currentY = 0f
    // Using a path, there is no need to draw every pixel, and each time you request a refresh
    // after display. Instead, you can interpolate a path between points for much better performance
    // If the finger has barely moved, there is no need to draw. If the finger has moved less than
    // a touchTolerance distance, don’t draw.
    //ScaledTouchSlop returns the distance in pixels a touch can wander before the system thinks
    // the user is scrolling
    private val touchTolerance = ViewConfiguration.get(context).scaledTouchSlop
    private lateinit var frame: Rect


    /**
     * Called by the Android system with the changed screen dimensions, that is, with a new
     * width and height (to change to) and the old width and height (to change from).
     */
    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)

        // To avoid a memory leak, leaving the old bitmaps around, call recycle extraBitmap before
        // creating the next one.
        if (::extraBitmap.isInitialized) extraBitmap.recycle()


        // ARGB_8888 stores each color in 4 bytes and is recommended.
        extraBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        extraCanvas = Canvas(extraBitmap)
        extraCanvas.drawColor(backgroundColor)

        // Calculate a rectangular frame around the picture.
        val inset = 40
        frame = Rect(inset, inset, width - inset, height - inset)
    }

    /**
     * The 2D coordinate system used for drawing on a Canvas is in pixels, and
     * the origin (0,0) is at the top left corner of the Canvas.
     */
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.let {
            it.drawBitmap(extraBitmap, 0f, 0f, null)
            // Draw a frame around the canvas.
            it.drawRect(frame, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        motionTouchEventX = event.x
        motionTouchEventY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> touchStart()
            MotionEvent.ACTION_MOVE -> touchMove()
            MotionEvent.ACTION_UP -> touchUp() //releasing touch on the screen
        }
        return true
    }

    private fun touchStart() {
        path.reset()
        path.moveTo(motionTouchEventX, motionTouchEventY)
        currentX = motionTouchEventX
        currentY = motionTouchEventY
    }

    private fun touchMove() {
        // Calculate the distance that has been moved
        val dx = abs(motionTouchEventX - currentX)
        val dy = abs(motionTouchEventY - currentY)

        // If the movement was further than the touch tolerance, add a segment to the path.
        if (dx >= touchTolerance || dy >= touchTolerance) {
            // QuadTo() adds a quadratic bezier from the last point,
            // approaching control point (x1,y1), and ending at (x2,y2). Using quadTo() instead of
            // lineTo() create a smoothly drawn line without corners. See Bezier Curves.
            path.quadTo(currentX, currentY, (motionTouchEventX + currentX) / 2, (motionTouchEventY + currentY) / 2)
            // Set the starting point for the next segment to the endpoint of this segment
            currentX = motionTouchEventX
            currentY = motionTouchEventY
            // Draw the path in the extra bitmap to cache it.
            extraCanvas.drawPath(path, paint)
        }
        invalidate()
    }

    private fun touchUp() {
        // Reset the path so it doesn't get drawn again.
        path.reset()
    }

}