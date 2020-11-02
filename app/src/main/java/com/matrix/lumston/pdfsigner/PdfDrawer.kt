package com.matrix.lumston.pdfsigner

import android.app.Activity
import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import kotlinx.android.synthetic.main.pdf_view.view.*
import java.io.File
import java.io.FileOutputStream

class PdfDrawer : LinearLayout {

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    var fragmentManager:FragmentManager? = null
    val fragments = mutableListOf<Fragment>()

    var lifeCycle : Lifecycle? = null

    //var filePath = "/storage/emulated/0/Download/82_Reglamento_de_la_Ley_Federal_de_Radio_y_Television_01.pdf"

    var filePath = "/storage/emulated/0/Download/pdfs/testpdfsigned.pdf"

    init {
        LayoutInflater.from(context).inflate(R.layout.pdf_view, this)
    }

    var bitmap : Bitmap? = null
    var currentPage : PageFragment? = null
    get() {
        return ( fragments.get(viewPager?.currentItem!!)  as PageFragment)
    }

    var enableSwipe : Boolean = true
    set(value) {
        viewPager.isUserInputEnabled = value
        field = value
    }

    var enableZooming : Boolean = false
    set(value) {
        Log.d("Swipe  mode","${!value}")
        enableSwipe = !value
        //viewPager.isUserInputEnabled = !value
        ( fragments.get(viewPager?.currentItem!!)  as PageFragment).isZooming = value
        field = value
    }

    var enableInserting : Boolean = false
    set(value) {
        ( fragments.get(viewPager?.currentItem!!)  as PageFragment).isInserting = value
        field = value
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun render(){

        val displayMetrics = DisplayMetrics()
        (context as Activity ).windowManager.defaultDisplay.getMetrics(displayMetrics)
        val height = displayMetrics.heightPixels
        val width = displayMetrics.widthPixels

        val render = PdfRenderer(ParcelFileDescriptor
            .open(File(filePath),
                ParcelFileDescriptor.MODE_READ_WRITE))

        val pages = render.pageCount
        var list = mutableListOf<PagePdf>()


        for (i in 0 until pages){
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444)
            val page = render.openPage(i)
            page.render(bitmap,null,null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            Log.d("Bitmap page","$bitmap")
            page.close()
            var f = PageFragment()
            f.page = PagePdf(i,page,bitmap)
            fragments.add(f)
            //list.add()
        }
        viewPager.adapter = PdfPageAdapter(lifeCycle!!,fragmentManager!!,fragments)
        render.close()

    }

    fun clearCanvas(){
        ( fragments.get(viewPager?.currentItem!!)  as PageFragment).clearCanvas()
    }

    fun getPages() : List<PagePdf>{
        return fragments.map {
            (it as PageFragment).page!!
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun createNewDocument(pages : List<PagePdf>){
        val document = PdfDocument()

        pages.forEach { pagePdf ->

            val width = pagePdf.page.width
            val height = pagePdf.page.height

            val pageInfo = PdfDocument.PageInfo.Builder(
                width,
                height,
                pagePdf.page.index + 1
            ).create()

            //val bitmap = BITMAP_RESIZER(pagePdf.bitmap,width,height)//Bitmap.createScaledBitmap(pagePdf.bitmap, width, height, true)

            val options = BitmapFactory.Options()
            options.inScaled = false
            options.inDither = false
            options.inPreferredConfig = Bitmap.Config.ARGB_8888
            //val source: Bitmap = BitmapFactory.decodeResource(a.getResources(), path, options)
            // Get cq
            val bitmap = pagePdf.bitmap//Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            val halfWidth = width/2
            val halfHeight = height/2

            val X = 0
            val Y = 0
            //val dstRectForRender = Rect( X - halfWidth, Y - halfHeight, X + halfWidth, Y + halfHeight )
            val dstRectForRender = Rect( 0,0,width, height )
            canvas.drawBitmap(bitmap, null,dstRectForRender, null)
            document.finishPage(page)
        }

        val path =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .toString() + "/pdfs/"
        val dir = File(path)
        if(!dir.exists()){
            dir.mkdirs()
        }
        val filePath = File(dir,"testpdfsigned.pdf")
        try {
            document.writeTo(FileOutputStream(filePath))
        }catch (e : Exception){

        }
        document.close()
    }


    private fun createScaledBitmap(bitmap: Bitmap, newWidth: Int, newHeight: Int): Bitmap? {
        val scaledBitmap = Bitmap.createBitmap(newWidth, newHeight, bitmap.config)
        val scaleX = newWidth / bitmap.width.toFloat()
        val scaleY = newHeight / bitmap.height.toFloat()
        val scaleMatrix = Matrix()
        scaleMatrix.setScale(scaleX, scaleY, 0f, 0f)
        val canvas = Canvas(scaledBitmap)
        canvas.setMatrix(scaleMatrix)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        paint.setAntiAlias(true)
        paint.setDither(true)
        paint.setFilterBitmap(true)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return scaledBitmap
    }

    fun BITMAP_RESIZER(bitmap: Bitmap, newWidth: Int, newHeight: Int): Bitmap? {
        val scaledBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888)
        val ratioX = newWidth / bitmap.width.toFloat()
        val ratioY = newHeight / bitmap.height.toFloat()
        val middleX = newWidth / 2.0f
        val middleY = newHeight / 2.0f
        val scaleMatrix = Matrix()
        scaleMatrix.setScale(ratioX, ratioY, middleX, middleY)
        val canvas = Canvas(scaledBitmap)
        canvas.setMatrix(scaleMatrix)
        canvas.drawBitmap(
            bitmap,
            middleX - bitmap.width / 2,
            middleY - bitmap.height / 2,
            Paint(Paint.FILTER_BITMAP_FLAG)
        )
        return scaledBitmap
    }

}