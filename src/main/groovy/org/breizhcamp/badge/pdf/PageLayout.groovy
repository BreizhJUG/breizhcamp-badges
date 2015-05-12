package org.breizhcamp.badge.pdf

import com.itextpdf.text.PageSize
import com.itextpdf.text.Rectangle
import groovy.transform.ToString

import java.lang.IllegalArgumentException as IAE

@ToString(includeNames = true)
class PageLayout {

    public static final L4745REV = new PageLayout('A4', [8.5f, 8.5f, 20.5f, 23.5f] as float[], 2, 4)

    /**
     * Page size (A4, A5...)
     */
    Rectangle format

    /**
     * Four document margins in mm. In order:
     * <ol>
     *   <li>left</li>
     *   <li>right</li>
     *   <li>top</li>
     *   <li>bottom</li>
     * </ol>
     */
    float[] margins

    /**
     * Number of columns of labels
     */
    int columns

    /**
     * Number of rows of labels
     */
    int rows

    PageLayout(String format, float[] margins, int columns, int rows) {

        try {
            this.format = PageSize.getRectangle(format)
        } catch (RuntimeException e) {
            throw new IAE("format '${format}' is not supported")
        }
        if (!(margins && margins.length == 4 && margins.every { it >= 0 }))
            throw new IAE('left, right, top and bottom margins must all be set and they all must be greater than or equal to 0')
        if (![columns, rows].every { it > 0 }) throw new IAE('columns and rows parameters must be greater than 0')

        this.margins = margins
        this.columns = columns
        this.rows = rows
    }
}
