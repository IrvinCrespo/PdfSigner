package com.matrix.lumston.pdfsigner

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View


@Suppress("DEPRECATION")
class SignView : View {

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    var mPaint : Paint
    var width : Int? = 0
    var height : Int? = 0
    lateinit var mBitmap : Bitmap
    lateinit var mCanvas : Canvas
    var mPath : Path
    var mBitmapPaint : Paint
    var mContext : Context
    var circlePaint : Paint
    var circlePath : Path

    var drawListener : DrawingListener? = null

    init {

        mContext = context

        mPaint = Paint()
        mPaint.isAntiAlias = true
        mPaint.isDither = true
        mPaint.color = Color.BLACK
        mPaint.style = Paint.Style.STROKE
        mPaint.strokeJoin = Paint.Join.ROUND
        mPaint.strokeCap = Paint.Cap.ROUND
        mPaint.strokeWidth = 5f

        mPath = Path()
        mBitmapPaint = Paint(Paint.DITHER_FLAG)
        circlePaint = Paint()
        circlePath = Path()
        circlePaint.isAntiAlias = true
        circlePaint.color = Color.BLUE
        circlePaint.style = Paint.Style.STROKE
        circlePaint.strokeJoin = Paint.Join.MITER
        circlePaint.strokeWidth = 4f
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        mCanvas = Canvas(mBitmap)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.drawBitmap( mBitmap,0f,0f, mBitmapPaint)
        canvas?.drawPath( mPath,  mPaint)

        canvas?.drawPath( circlePath,  circlePaint)

        drawListener?.onDrawPath(mPath,mPaint,mBitmapPaint,mBitmap)
        drawListener?.onDrawSign(canvas)
    }

    var  mX  : Float = 0f
    var mY : Float = 0f
    val TOUCH_TOLERANCE = 4

    fun touch_start(x: Float, y: Float) {
        mPath.reset()
        mPath.moveTo(x, y)
        mX = x
        mY = y
    }

    fun touch_move(x: Float, y: Float) {
        val dx = Math.abs(x - mX)
        val dy = Math.abs(y - mY)
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2)
            mX = x
            mY = y
            circlePath.reset()
            circlePath.addCircle(mX, mY, 30f, Path.Direction.CW)
        }
    }

    fun touch_up() {
        mPath.lineTo(mX, mY)
        circlePath.reset()
        // commit the path to our offscreen
        mCanvas.drawPath(mPath, mPaint)
        // kill this so we don't double draw
        mPath.reset()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val x = event!!.x
        val y = event!!.y

        when(event.getAction()){

            MotionEvent.ACTION_DOWN -> {
                touch_start(x, y);
                invalidate();
            }

            MotionEvent.ACTION_MOVE -> {
                touch_move(x, y);
                invalidate();
            }

            MotionEvent.ACTION_UP -> {
                touch_up();
                invalidate();
            }
        }
        return true
    }

    fun getBitmap() : Bitmap {
        val v = this as View
        val b = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        v.layout(v.left , v.top, v.right , v.bottom)
        v.draw(c)
        return b
    }

    fun clean(){
        mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        invalidate()
    }

}

interface DrawingListener{
    fun onDrawSign(canvas:Canvas?)
    fun onDrawPath(mPath : Path,  mPaint:Paint, mBitmapPaint : Paint, mBitmap : Bitmap)
}