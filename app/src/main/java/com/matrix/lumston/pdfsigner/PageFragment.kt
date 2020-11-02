package com.matrix.lumston.pdfsigner

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import kotlinx.android.synthetic.main.fragment_page.*

/**
 * A simple [Fragment] subclass.
 */
class PageFragment : Fragment() {

    var page : PagePdf? = null
    var theview : View? = null

    var isEditing: Boolean = false
    set(value) {
        image_pdf.inEditMode = value
        field = value
        if(!field){
            val newBitmap = getNewBitmap()
            page?.bitmap = newBitmap!!
            image_pdf.setImageBitmap(page?.bitmap)
            image_pdf.clear()
            //image_pdf
        }
    }

    var isZooming : Boolean = false
    set(value) {
        field = value
        image_pdf.inZoomMode = value
    }

    var isInserting : Boolean = false
    set(value) {
        field = value
        image_pdf.inInsertingMode = value
    }

    lateinit var scaleDetector : ScaleGestureDetector
    private var scaleFactor = 1f

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        theview = inflater.inflate(R.layout.fragment_page, container, false)

        val pdf = PageView(context!!)
        //theview. pagePdf = page
        return theview
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        image_pdf.setImageBitmap(page?.bitmap!!) //setImage(ImageSource.bitmap())

        image_pdf.insertBitmap = AppCompatResources.getDrawable(requireContext(),R.drawable.ic_launcher_background )?.toBitmap()
        //image_pdf.insertBitmap = BitmapFactory.decodeResource(resources,R.drawable.ic_launcher_background)
        //pageview.pagePdf = page
        //pageview.parentGroup =
       // page_image.setImageBitmap(page?.bitmap)
    }

    fun getNewBitmap() : Bitmap?{
        return ( image_pdf?.drawable as (BitmapDrawable)).bitmap
    }

    fun clearCanvas(){
        image_pdf.clear()
    }
}


