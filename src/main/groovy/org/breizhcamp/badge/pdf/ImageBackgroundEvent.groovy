package org.breizhcamp.badge.pdf

import com.itextpdf.text.DocumentException
import com.itextpdf.text.ExceptionConverter
import com.itextpdf.text.Image
import com.itextpdf.text.Rectangle
import com.itextpdf.text.pdf.PdfContentByte
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPCellEvent
import com.itextpdf.text.pdf.PdfPTable

class ImageBackgroundEvent implements PdfPCellEvent {

    protected Image image
    private boolean fitToRectangle


    public ImageBackgroundEvent(Image image,
                                boolean fitToRectangle = true) {
        this.image = image
        this.fitToRectangle = fitToRectangle
    }

    public void cellLayout(PdfPCell cell, Rectangle position, PdfContentByte[] canvases) {
        try {
            PdfContentByte cb = canvases[PdfPTable.BACKGROUNDCANVAS]
            if (fitToRectangle) {
                image.scaleAbsolute(position)
            }
            image.setAbsolutePosition(position.getLeft(), position.getBottom())
            cb.addImage(image)
        } catch (DocumentException e) {
            throw new ExceptionConverter(e)
        }
    }
}
