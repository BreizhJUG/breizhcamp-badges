package org.breizhcamp.badge.pdf

import au.com.bytecode.opencsv.CSVWriter
import com.itextpdf.text.*
import com.itextpdf.text.pdf.*
import com.itextpdf.text.pdf.qrcode.EncodeHintType
import org.breizhcamp.badge.Badge
import org.breizhcamp.badge.PageLayout

import java.lang.IllegalArgumentException as IAE

class PdfBadgeGenerator {

    private final PageLayout pageLayout
    private final Document document

    private PdfPTable docTable
    private int labelCount

    private final cellBorderWidth

    private final BaseFont badgeFont, symbolFont
    private final Font nameFont, ticketTypeFont, twitterFont

    private final Map backgrounds

    PdfBadgeGenerator(OutputStream outputStream, PageLayout pageLayout, boolean debug = false) {

        if (pageLayout == null) throw new IAE('pageLayout must not be null')
        if (outputStream == null) throw new IAE('outputStream must not be null')

        badgeFont = BaseFont.createFont('/fonts/nunito/Nunito-Light.ttf', BaseFont.IDENTITY_H, true)
        symbolFont = BaseFont.createFont('/fonts/fontawesome/fontawesome-webfont.ttf', BaseFont.IDENTITY_H, BaseFont.EMBEDDED)
        nameFont = new Font(badgeFont, 16, Font.BOLD, BaseColor.BLACK)
        ticketTypeFont = new Font(badgeFont, 14, Font.BOLD, BaseColor.WHITE)
        twitterFont = new Font(symbolFont, 16, Font.NORMAL, new BaseColor(58, 170, 225))

        backgrounds = [Team    : 'orange',
                       Conf    : 'blue',
                       Speaker : 'yellow',
                       Hacker  : 'violet',
                       Combo   : 'green',
                       Exposant: 'black']

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

        // Label left side
        labelLayout.addCell(generateLeftSide(badge))

        // label right side
        labelLayout.addCell(generateRightSide(badge))

        // Wrapper to remove label border
        def labelWrapper = new PdfPCell(labelLayout)
        labelWrapper.borderWidth = cellBorderWidth
        docTable.addCell(labelWrapper)

        ++labelCount
    }

    private PdfPCell generateLeftSide(Badge badge) {

        PdfPTable leftSide = generateSideContentTable()

        // Name and company
        PdfPCell nameAndCompanyCell = new PdfPCell()
        nameAndCompanyCell.with {
            horizontalAlignment = ALIGN_LEFT
            verticalAlignment = ALIGN_TOP
            paddingLeft = 10
            borderWidth = cellBorderWidth
        }
        def nameParagraph = new Paragraph(24, "${badge.firstname}\n${badge.lastname}", nameFont)
        nameParagraph.setSpacingAfter(32)
        nameAndCompanyCell.addElement(nameParagraph)
        nameAndCompanyCell.addElement(new Paragraph(16, "${badge.company}", nameFont))
        leftSide.addCell(nameAndCompanyCell)

        // Twitter symbol
        PdfPCell twitterCell = new PdfPCell(new Paragraph("\uF099", twitterFont))
        twitterCell.with {
            borderWidth = cellBorderWidth
            paddingLeft = 10
            paddingBottom = 10
            verticalAlignment = ALIGN_BOTTOM
        }
        leftSide.addCell(twitterCell)

        PdfPCell fixedHeightWrapper = new PdfPCell(leftSide)
        fixedHeightWrapper.with {
            fixedHeight = calculateLabelHeight()
            borderWidth = cellBorderWidth
        }
        return fixedHeightWrapper
    }

    private PdfPCell generateRightSide(Badge badge) {

        PdfPTable rightSide = generateSideContentTable()

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

        // QRCode content
        StringWriter qrCodeTextWriter = new StringWriter()
        CSVWriter csvWriter = new CSVWriter(qrCodeTextWriter, ',' as char, '"' as char, '\\' as char)
        csvWriter.writeNext([badge.lastname, badge.firstname, badge.company, badge.email] as String[])
        csvWriter.flush()

        // QRCode cell
        PdfPCell qrcodeCell = new PdfPCell(new BarcodeQRCode(qrCodeTextWriter.toString(),
                100, 100,
                [(EncodeHintType.CHARACTER_SET): 'UTF-8']).image)
        qrcodeCell.with {
            horizontalAlignment = ALIGN_CENTER
            verticalAlignment = ALIGN_MIDDLE
            borderWidth = cellBorderWidth
        }
        rightSide.addCell(qrcodeCell)

        PdfPCell backgroundWrapper = new PdfPCell(rightSide)
        backgroundWrapper.with {
            cellEvent = new ImageBackgroundEvent(Image.getInstance(this.class.getResource("/images/${backgrounds[badge.ticketType]}.png").toURI().toURL()))
            borderWidth = cellBorderWidth
        }
        return backgroundWrapper
    }

    private PdfPTable generateSideContentTable() {
        PdfPTable sideContent = new PdfPTable(1)
        sideContent.with {
            widthPercentage = 100
            widths = [1f] as float[]
        }
        sideContent
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
