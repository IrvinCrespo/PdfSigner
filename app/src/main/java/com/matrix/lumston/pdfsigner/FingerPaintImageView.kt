package com.matrix.lumston.pdfsigner

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView


public class FingerPaintImageView @JvmOverloads constructor(context: Context,
                                                     attrs: AttributeSet? = null,
                                                     defStyleAttr: Int = 0,
                                                     defStyleRes: Int = 0) :
    AppCompatImageView(context, attrs, defStyleAttr) {

    val NONE = 0
    val DRAG = 1
    val ZOOM = 2
    val CLICK = 3

    private var mode = NONE

    val matrixX = Matrix()

    private val last = PointF()
    private val start = PointF()
    private val minScale = 0.5f
    private var maxScale = 4f
    private lateinit var m: FloatArray

    private var redundantXSpace = 0f
    private  var redundantYSpace:kotlin.Float = 0f
    private var saveScale = 1f
    private var right = 0f
    private var bottom:kotlin.Float = 0f
    private var originalBitmapWidth:kotlin.Float = 0f
    private var originalBitmapHeight:kotlin.Float = 0f

    private var mScaleDetector: ScaleGestureDetector? = null



    inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
            return super.onScaleBegin(detector)
        }

        override fun onScale(detector: ScaleGestureDetector?): Boolean {
            val scaleFactor = detector!!.scaleFactor
            val newScale: Float = saveScale * scaleFactor
            if (newScale < maxScale && newScale > minScale) {
                saveScale = newScale
                val width: Float = getWidth().toFloat()
                val height: Float = getHeight().toFloat()
                right = originalBitmapWidth * saveScale - width
                bottom = originalBitmapHeight * saveScale - height
                val scaledBitmapWidth: Float = originalBitmapWidth * saveScale
                val scaledBitmapHeight: Float = originalBitmapHeight * saveScale
                if (scaledBitmapWidth <= width || scaledBitmapHeight <= height) {
                    matrixX.postScale(scaleFactor, scaleFactor, width / 2, height / 2)
                } else {
                    matrixX.postScale(
                        scaleFactor,
                        scaleFactor,
                        detector!!.focusX,
                        detector!!.focusY
                    )
                }
            }
            return true
        }


    }



    private enum class BrushType {
        BLUR, EMBOSS, NORMAL
    }

    private val defaultStrokeColor = Color.WHITE
    private val defaultStrokeWidth = 12f
    private val defaultTouchTolerance = 4f
    private val defaultBitmapPaint = Paint(Paint.DITHER_FLAG)
    private var brushBitmap: Bitmap? = null
    private var brushCanvas: Canvas? = null
    private var countDrawn = 0
    private var currentBrush = BrushType.NORMAL

    var inEditMode = false

    var inZoomMode  = false

    var inInsertingMode = false

    var insertBitmap : Bitmap? = null

    // Zoom features
    var scaleDetector : ScaleGestureDetector
    private var scaleFactor = 1f

    private val defaultEmboss: EmbossMaskFilter by lazy {
        EmbossMaskFilter(floatArrayOf(1F, 1F, 1F), 0.4F, 6F, 3.5F)
    }
    private val defaultBlur: BlurMaskFilter by lazy {
        BlurMaskFilter(5F, BlurMaskFilter.Blur.NORMAL)
    }

    var strokeColor = defaultStrokeColor
        set(value) {
            field = value
            pathPaint.color = value
        }

    var strokeWidth = defaultStrokeWidth
        set(value) {
            field = value
            pathPaint.strokeWidth = value
        }

    private val matrixValues = FloatArray(9)
        get() = field.apply { imageMatrix.getValues(this) }

    var touchTolerance = defaultTouchTolerance

    private val pathPaint = Paint().also {
        it.isAntiAlias = true
        it.isDither = true
        it.color = strokeColor
        it.style = Paint.Style.STROKE
        it.strokeJoin = Paint.Join.ROUND
        it.strokeCap = Paint.Cap.ROUND
        it.strokeWidth = strokeWidth
    }

    private var currentX = 0f
    private var currentY = 0f
    private var paths: MutableList<Pair<Path, Paint>> = mutableListOf()
    
    init {

        mScaleDetector = ScaleGestureDetector(context, ScaleListener())
        m = FloatArray(9)
        setImageMatrix(matrixX)
        setScaleType(ScaleType.MATRIX)


        if (attrs != null) {
            val typedArray = context.theme.obtainStyledAttributes(attrs,
                R.styleable.FingerPaintImageView, defStyleAttr, defStyleRes)
            try {
                strokeColor = typedArray.getColor(R.styleable.FingerPaintImageView_strokeColor, defaultStrokeColor)
                strokeWidth = typedArray.getDimension(R.styleable.FingerPaintImageView_strokeWidth, defaultStrokeWidth)
                inEditMode = typedArray.getBoolean(R.styleable.FingerPaintImageView_inEditMode, false)
                touchTolerance = typedArray.getFloat(R.styleable.FingerPaintImageView_touchTolerance, defaultTouchTolerance)
            } finally {
                typedArray.recycle()
            }
        }

        scaleDetector = ScaleGestureDetector(context, object :
            ScaleGestureDetector.SimpleOnScaleGestureListener() {

            override fun onScale(detector: ScaleGestureDetector?): Boolean {
                scaleFactor *= detector?.scaleFactor!!
                scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 10.0f))
                scaleX = scaleFactor
                scaleY = scaleFactor
                //invalidate()
                return true
            }
        })
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val bmHeight: Int = getBmHeight()
        val bmWidth: Int = getBmWidth()
        val width = measuredWidth.toFloat()
        val height = measuredHeight.toFloat()
        //Fit to screen.
        val scale = if (width > height) height / bmHeight else width / bmWidth
        matrixX.setScale(scale, scale)
        saveScale = 1f
        originalBitmapWidth = scale * bmWidth
        originalBitmapHeight = scale * bmHeight

        // Center the image
        redundantYSpace = height - originalBitmapHeight
        redundantXSpace = width - originalBitmapWidth
        matrixX.postTranslate(redundantXSpace / 2, redundantYSpace / 2)
        imageMatrix = matrixX
    }

    /**
     * Get current screen's width and height
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        brushBitmap = Bitmap.createBitmap(w,
            h,
            Bitmap.Config.ARGB_8888)
        brushCanvas = Canvas(brushBitmap!!)
    }

    /**
     * If there are any paths drawn on top of the image, this will return a bitmap with the original
     * content plus the drawings on top of it. Otherwise, the original bitmap will be returned.
     */
    override fun getDrawable(): Drawable? {
        return super.getDrawable()?.let {
            if (!isModified()) return it

            val inverse = Matrix().apply { imageMatrix.invert(this) }
            val scale = FloatArray(9).apply { inverse.getValues(this) }[Matrix.MSCALE_X]
            
            // draw original bitmap
            val result = Bitmap.createBitmap(it.intrinsicWidth, it.intrinsicHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)
            it.draw(canvas)

            val transformedPath = Path()
            val transformedPaint = Paint()
            paths.forEach { (path, paint) ->
                path.transform(inverse, transformedPath)
                transformedPaint.set(paint)
                transformedPaint.strokeWidth *= scale
                canvas.drawPath(transformedPath, transformedPaint)
            }
            BitmapDrawable(resources, result)
        }
    }

    private fun getCurrentPath() = paths.lastOrNull()?.first


    var touchX = 0f
    var touchY = 0f
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (inEditMode) {
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    handleTouchStart(event)
                    invalidate()
                }
                MotionEvent.ACTION_MOVE -> {
                    handleTouchMove(event)
                    invalidate()
                }
                MotionEvent.ACTION_UP -> {
                    handleTouchEnd()
                    countDrawn++
                    invalidate()
                }
            }

        }else if(!inEditMode && inZoomMode){
            //scaleDetector.onTouchEvent(event)
            mScaleDetector?.onTouchEvent(event)
            mScaleDetector!!.onTouchEvent(event)

            matrixX.getValues(m)
            val x = m[Matrix.MTRANS_X]
            val y = m[Matrix.MTRANS_Y]
            val curr = PointF(event!!.x, event.y)

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    last[event.x] = event.y
                    start.set(last)
                    mode = DRAG
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    last[event.x] = event.y
                    start.set(last)
                    mode = ZOOM
                }
                MotionEvent.ACTION_MOVE ->                 //if the mode is ZOOM or
                    //if the mode is DRAG and already zoomed
                    if (mode === ZOOM || mode === DRAG && saveScale > minScale) {
                        var deltaX = curr.x - last.x // x difference
                        var deltaY = curr.y - last.y // y difference
                        val scaleWidth =
                            Math.round(originalBitmapWidth * saveScale)
                                .toFloat() // width after applying current scale
                        val scaleHeight =
                            Math.round(originalBitmapHeight * saveScale)
                                .toFloat() // height after applying current scale
                        var limitX = false
                        var limitY = false

                        //if scaleWidth is smaller than the views width
                        //in other words if the image width fits in the view
                        //limit left and right movement
                        if (scaleWidth < width && scaleHeight < height) {
                            // don't do anything
                        } else if (scaleWidth < width) {
                            deltaX = 0f
                            limitY = true
                        } else if (scaleHeight < height) {
                            deltaY = 0f
                            limitX = true
                        } else {
                            limitX = true
                            limitY = true
                        }
                        if (limitY) {
                            if (y + deltaY > 0) {
                                deltaY = -y
                            } else if (y + deltaY < -bottom) {
                                deltaY = -(y + bottom)
                            }
                        }
                        if (limitX) {
                            if (x + deltaX > 0) {
                                deltaX = -x
                            } else if (x + deltaX < -right) {
                                deltaX = -(x + right)
                            }
                        }
                        //move the image with the matrix
                        matrixX.postTranslate(deltaX, deltaY)
                        //set the last touch location to the current
                        last[curr.x] = curr.y
                    }
                MotionEvent.ACTION_UP -> {
                    mode = NONE
                    val xDiff = Math.abs(curr.x - start.x).toInt()
                    val yDiff = Math.abs(curr.y - start.y).toInt()
                    if (xDiff < CLICK && yDiff < CLICK) performClick()
                }
                MotionEvent.ACTION_POINTER_UP -> mode = NONE
            }
            imageMatrix = matrixX
            invalidate()
        }else if(inInsertingMode){

            when(event?.action){

                MotionEvent.ACTION_UP -> {
                    touchX = event.x
                    touchY = event.y
                    invalidate()
                }

            }

        }

        return true
    }

    private fun handleTouchStart(event: MotionEvent) {
        val sourceBitmap = super.getDrawable() ?: return

        val xTranslation = matrixValues[Matrix.MTRANS_X]
        val yTranslation = matrixValues[Matrix.MTRANS_Y]
        val scale = matrixValues[Matrix.MSCALE_X]

        val imageBounds = RectF(
            xTranslation,
            yTranslation,
            xTranslation + sourceBitmap.intrinsicWidth * scale,
            yTranslation + sourceBitmap.intrinsicHeight * scale)

        // make sure drawings are kept within the image bounds
        if (imageBounds.contains(event.x, event.y)) {
            paths.add(Path().also { it.moveTo(event.x + 1, event.y + 1) } to Paint(pathPaint))
            currentX = event.x
            currentY = event.y
        }
    }

    private fun handleTouchMove(event: MotionEvent) {
        val sourceBitmap = super.getDrawable() ?: return

        val xTranslation = matrixValues[Matrix.MTRANS_X]
        val yTranslation = matrixValues[Matrix.MTRANS_Y]
        val scale = matrixValues[Matrix.MSCALE_X]

        val xPos = event.x.coerceIn(xTranslation, xTranslation + sourceBitmap.intrinsicWidth * scale)
        val yPos = event.y.coerceIn(yTranslation, yTranslation + sourceBitmap.intrinsicHeight * scale)

        val dx = Math.abs(xPos - currentX)
        val dy = Math.abs(yPos - currentY)

        if (dx >= touchTolerance || dy >= touchTolerance) {
            getCurrentPath()?.quadTo(currentX, currentY, (xPos + currentX) / 2, (yPos + currentY) / 2)
            currentX = xPos
            currentY = yPos
        }
    }

    private fun handleTouchEnd() = getCurrentPath()?.lineTo(currentX, currentY)

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if(inEditMode){

            brushBitmap?.eraseColor(Color.TRANSPARENT)
            brushCanvas?.drawColor(Color.TRANSPARENT)
            canvas?.save()
            //canvas?.scale(scaleFactor, scaleFactor)
            for (index in paths.indices) {
                val path = paths[index]
                if (index >= countDrawn) {
                    path.second.maskFilter =
                        when (currentBrush) {
                            BrushType.EMBOSS -> defaultEmboss
                            BrushType.BLUR -> defaultBlur
                            BrushType.NORMAL -> null
                        }
                }
                brushCanvas?.drawPath(paths[index].first, paths[index].second)
            }
            canvas?.drawBitmap(brushBitmap!!, 0f, 0f, defaultBitmapPaint)
            canvas?.restore()

        }
        if(inInsertingMode){
            canvas?.save()
            insertBitmap?.let {
                canvas?.drawBitmap(it, touchX,touchY,defaultBitmapPaint)
                canvas?.restore()
            }

        }

    }

    /**
     * Enable normal mode
     */
    fun normal() {
        currentBrush = BrushType.NORMAL
    }

    /**
     * Change brush type to emboss
     */
    fun emboss() {
        currentBrush = BrushType.EMBOSS
    }

    /**
     * Change brush type to blur
     */
    fun blur() {
        currentBrush = BrushType.BLUR
    }

    /**
     * Removes the last full path from the view.
     */
    fun undo() {
        paths.takeIf { it.isNotEmpty() }?.removeAt(paths.lastIndex)
        countDrawn--
        invalidate()
    }

    /**
     * Returns true if any paths are currently drawn on the image, false otherwise.
     */
    fun isModified(): Boolean {
        return if (paths != null) {
            paths.isNotEmpty()
        } else {
            false
        }
    }

    /**
     * Clears all existing paths from the image.
     */
    fun clear() {
        paths.clear()
        countDrawn = 0
        invalidate()
    }

    fun setMaxZoom(x: Float) {
        maxScale = x
    }

    private fun getBmWidth(): Int {
        val drawable = drawable
        return drawable?.intrinsicWidth ?: 0
    }

    private fun getBmHeight(): Int {
        val drawable = drawable
        return drawable?.intrinsicHeight ?: 0
    }
}