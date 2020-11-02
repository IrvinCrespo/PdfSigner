package com.matrix.lumston.pdfsigner

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.davemorrissey.labs.subscaleview.ImageSource
import kotlinx.android.synthetic.main.page_view.view.*


class PageView : LinearLayout {

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    var scaleDetector : ScaleGestureDetector
    private var scaleFactor = 1f

    var parentGroup : ViewGroup? = null
    var pagePdf : PagePdf? = null
    set(value) {
        field = value
        image_pdf.setImage(ImageSource.bitmap(pagePdf?.bitmap!!))
        //image_pdf?.setImageBitmap(pagePdf?.bitmap)
    }

    init {

        View.inflate(context,R.layout.page_view,this)
        //LayoutInflater.from(context).inflate(R.layout.page_view,null, true)

        scaleDetector = ScaleGestureDetector(context, object :
            ScaleGestureDetector.SimpleOnScaleGestureListener() {

            override fun onScale(detector: ScaleGestureDetector?): Boolean {
                scaleFactor *= detector?.scaleFactor!!
                scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 10.0f))
                invalidate()
                return true
            }

        })

        setWillNotDraw(false)

    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {

        return onTouchEvent(ev)
    }



    override fun onTouchEvent(event: MotionEvent?): Boolean {
        //scaleDetector.onTouchEvent(event)
        return true
    }


    override fun onDraw(canvas: Canvas?) {
        canvas?.save()
        Log.d("Scale gesture","$scaleFactor")
        canvas?.scale(scaleFactor, scaleFactor)
        //image_pdf?.draw(canvas)
        canvas?.restore()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        //Log.d("Attached to window","${pagePdf?.bitmap}")
    }

}