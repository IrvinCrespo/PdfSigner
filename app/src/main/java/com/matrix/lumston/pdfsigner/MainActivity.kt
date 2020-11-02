package com.matrix.lumston.pdfsigner

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.drawToBitmap
import com.github.barteksc.pdfviewer.listener.OnDrawListener
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.pdf_view.view.*
import java.io.File


class MainActivity : AppCompatActivity(), OnDrawListener, DrawingListener {

    init {

    }

    //val contentStream : PdfPro

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //Manifest.permission.READ_EXTERNAL_STORAGE,
        actionBar?.setDisplayShowHomeEnabled(true)
        actionBar?.setIcon(R.drawable.ic_launcher_background)
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),102)
                //requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),101)
            }
        }
        else
            pdfview.fromFile(File("/storage/emulated/0/Download/82_Reglamento_de_la_Ley_Federal_de_Radio_y_Television_01.pdf"))
                    .onDraw(this)
                    .enableSwipe(false)
                    //.load()

        signview.drawListener = this
        pdfdraw.fragmentManager = supportFragmentManager
        pdfdraw.lifeCycle = lifecycle
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            pdfdraw.render()
        }
    }
    
    var mainMenu : Menu? = null
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        mainMenu = menu
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu_toolbar,menu)
        return true
    }

    var isEditing = false
    var isZooming = false
    var isInsertingSign = false

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if(item.itemId == R.id.zoom){
            isZooming = !isZooming

        }else if(item.itemId == R.id.edit){
            isEditing = !isEditing

            mainMenu?.findItem(R.id.delete)?.isVisible = isEditing

        }else if(item.itemId == R.id.save){

            val pages = pdfdraw.getPages()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                pdfdraw.createNewDocument(pages)
                Log.d("Document created", "OK")
            }

        }else if(item.itemId == R.id.delete){
            pdfdraw.clearCanvas()
        }
        else if(item.itemId == R.id.sign){
            isInsertingSign = !isInsertingSign
        }

        pdfdraw.currentPage?.isEditing = isEditing
        pdfdraw.enableZooming = isZooming
        pdfdraw.enableInserting = isInsertingSign
        pdfdraw.enableSwipe = !isEditing

        return super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == 102){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
                pdfview.fromFile(File("/storage/emulated/0/Download/82_Reglamento_de_la_Ley_Federal_de_Radio_y_Television_01.pdf"))
                        .onDraw(this)
                        .enableSwipe(false)
                        //.load()
        }
    }

    var paint = Paint()
    override fun onLayerDrawn(canvas: Canvas?, pageWidth: Float, pageHeight: Float, displayedPage: Int) {
        Log.d("pdf draw","drawing")
        Log.d("pdf canvas","${canvas}")

        paint.color = Color.BLACK
        paint.strokeWidth = 10f
        paint.style = Paint.Style.STROKE

        if(mPath != null && mPaint != null){
            canvas?.drawBitmap(mBitmap!!,0f,0f,mBitmapPaint)
            canvas?.drawPath( mPath!!,  mPaint!!)
            //canvas?.draw
        }
        /*
        canvas?.drawLine(0f, 0f, pageWidth, 0f, paint)
        canvas?.drawLine(0f, pageHeight, pageWidth, pageHeight, paint)
        canvas?.drawLine(0f, 0f, 0f, pageHeight, paint)
        canvas?.drawLine(pageWidth, 0f, pageWidth, pageHeight, paint)
        */
        canvas?.save()
        canvas?.restore()
        //pdfview.draw(canvas)

    }

    override fun onDrawSign(canvas: Canvas?) {
        pdfview.draw(canvas)
    }

    var mPath : Path? = null
    var mPaint : Paint? = null
    var mBitmapPaint: Paint? = null
    var mBitmap : Bitmap? = null
    override fun onDrawPath(path: Path, paint: Paint, bitmapPaint: Paint,bitmap:Bitmap) {
        mPaint = paint
        mPath = path
        mBitmapPaint = bitmapPaint
        mBitmap = bitmap
    }

}
