package com.matrix.lumston.pdfsigner

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer

data class PagePdf (
    var index : Int,
    var page:PdfRenderer.Page,
    var bitmap:Bitmap
)