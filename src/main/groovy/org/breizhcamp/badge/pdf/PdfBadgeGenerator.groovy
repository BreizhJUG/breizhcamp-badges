package org.breizhcamp.badge.pdf

import au.com.bytecode.opencsv.CSVWriter
import com.itextpdf.text.*
import com.itextpdf.text.pdf.BarcodeQRCode
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import com.itextpdf.text.pdf.qrcode.EncodeHintType
import org.breizhcamp.badge.Badge
import org.breizhcamp.badge.PageLayout
import org.breizhcamp.badge.pdf.ImageBackgroundEvent

class PdfBadgeGenerator {

    private final PageLayout pageLayout
    private final Document document

    private PdfPTable docTable
    private int labelCount

    private final cellBorderWidth

    Font nameFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, BaseColor.BLACK);
    Font ticketTypeFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, BaseColor.WHITE);

    Map backgrounds = [Team    : 'orange',
                       Conf    : 'blue',
                       Speaker : 'yellow',
                       Hacker  : 'violet',
                       Combo   : 'green',
                       Exposant: 'black']

    PdfBadgeGenerator(OutputStream outputStream, PageLayout pageLayout, boolean debug = false) {

        assert pageLayout != null, 'pageLayout must not be null'
        assert outputStream != null, 'outputStream must not be null'

        this.pageLayout = pageLayout

        document = [pageLayout.format, pageLayout.margins.collect(milliToPoints)].flatten() as Document
        PdfWriter.getInstance(document, outputStream)

        cellBorderWidth = debug ? 1f : 0f
    }

    void startDocument() {

        document.open()
        labelCount = 0

        docTable = new PdfPTable(pageLayout.columns)
        docTable.widthPercentage = 100
        docTable.widths = (0..<pageLayout.columns).collect { 1f } as float[]
    }

    void addBadge(Badge badge) {


        PdfPTable labelLayout = new PdfPTable(2)
        labelLayout.with {
            widthPercentage = 100
            widths = [1f, 1f] as float[]
        }

        // First and last name
        PdfPCell nameCell = new PdfPCell()
        nameCell.with {
            horizontalAlignment = ALIGN_LEFT
            verticalAlignment = ALIGN_TOP
            paddingLeft = 10
            fixedHeight = calculateLabelHeight()
            borderWidth = cellBorderWidth
        }
        def nameParagraph = new Paragraph(24, "${badge.firstname}\n${badge.lastname}", nameFont)
        nameParagraph.setSpacingAfter(32)
        nameCell.addElement(nameParagraph)
        nameCell.addElement(new Paragraph(16, "${badge.company}", nameFont))
        labelLayout.addCell(nameCell)

        // Right part
        PdfPTable rightSide = new PdfPTable(1)
        rightSide.with {
            widthPercentage = 100
            widths = [1f] as float[]
        }

        // Ticket type
        String ticketType = badge.ticketType
        PdfPCell ticketTypeCell = new PdfPCell(new Phrase(ticketType, ticketTypeFont))
        ticketTypeCell.with {
            horizontalAlignment = ALIGN_RIGHT
            paddingTop = 10
            paddingRight = 10
            borderWidth = cellBorderWidth
        }
        rightSide.addCell(ticketTypeCell)

        // QRCode
        StringWriter qrCodeTextWriter = new StringWriter()
        CSVWriter csvWriter = new CSVWriter(qrCodeTextWriter, ',' as char, '"' as char, '\\' as char)
        csvWriter.writeNext([badge.lastname, badge.firstname, badge.company, badge.email] as String[])
        csvWriter.flush()

        PdfPCell qrcodeCell = new PdfPCell(new BarcodeQRCode(qrCodeTextWriter.toString(),
                100, 100,
                [(EncodeHintType.CHARACTER_SET): 'UTF-8']).image)
        qrcodeCell.with {
            horizontalAlignment = ALIGN_CENTER
            verticalAlignment = ALIGN_MIDDLE
            borderWidth = cellBorderWidth
        }
        rightSide.addCell(qrcodeCell)

        PdfPCell rightSideWrapper = new PdfPCell(rightSide)
        rightSideWrapper.with {
            cellEvent = new ImageBackgroundEvent(Image.getInstance(this.class.getResource("/${backgrounds[badge.ticketType]}.png").toURI().toURL()))
            borderWidth = cellBorderWidth
        }
        labelLayout.addCell(rightSideWrapper)

        // Wrapper to remove label border
        def labelWrapper = new PdfPCell(labelLayout)
        labelWrapper.borderWidth = cellBorderWidth
        docTable.addCell(labelWrapper)

        ++labelCount
    }

    void addFiller() {
        def fillerCell = new PdfPCell()
        fillerCell.with {
            borderWidth = cellBorderWidth
            fixedHeight = calculateLabelHeight()
        }
        docTable.addCell(fillerCell)
    }

    void endDocument() {
        (labelCount % pageLayout.columns).times { addFiller() } // fillers nécessaire pour compléter la dernière ligne
        document.add(docTable)
        document.close()
    }

    private Closure milliToPoints = { float number ->
        return number * 28.35 / 10 as float
    }

    private double calculateLabelHeight() {
        (pageLayout.format.height - document.topMargin() - document.bottomMargin()) / pageLayout.rows
    }
}
