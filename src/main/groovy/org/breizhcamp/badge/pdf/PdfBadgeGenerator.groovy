package org.breizhcamp.badge.pdf

import au.com.bytecode.opencsv.CSVWriter
import com.itextpdf.text.*
import com.itextpdf.text.pdf.*
import com.itextpdf.text.pdf.qrcode.EncodeHintType
import org.breizhcamp.badge.Badge

import java.lang.IllegalArgumentException as IAE

import static com.itextpdf.text.pdf.BaseFont.*

class PdfBadgeGenerator {

    private final PageLayout pageLayout
    private final Document document

    private PdfPTable docTable
    private int labelCount

    private final cellBorderWidth

    private final BaseFont badgeFont, symbolFont
    private final Font nameFont, ticketTypeFont, twitterFont, twitterAccountFont
    private final PdfWriter writer

    PdfBadgeGenerator(OutputStream outputStream, PageLayout pageLayout, boolean debug = false) {

        if (pageLayout == null) throw new IAE('pageLayout must not be null')
        if (outputStream == null) throw new IAE('outputStream must not be null')

        badgeFont = createFont('/fonts/nunito/Nunito-Light.ttf', IDENTITY_H, EMBEDDED)
        symbolFont = createFont('/fonts/fontawesome/fontawesome-webfont.ttf', IDENTITY_H, EMBEDDED)
        nameFont = new Font(badgeFont, 16, Font.BOLD, BaseColor.BLACK)
        twitterAccountFont = new Font(badgeFont, 12, Font.BOLD, BaseColor.BLACK)
        ticketTypeFont = new Font(badgeFont, 14, Font.BOLD, BaseColor.WHITE)
        twitterFont = new Font(symbolFont, 16, Font.NORMAL, new BaseColor(58, 170, 225)) // Twitter color

        this.pageLayout = pageLayout

        document = [pageLayout.format, pageLayout.margins.collect(milliToPoints)].flatten() as Document
        writer = PdfWriter.getInstance(document, outputStream)

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
        def nameParagraph = new Paragraph(24, "${titleCase(badge.firstname)}\n${titleCase(badge.lastname)}", nameFont)
        nameParagraph.spacingAfter = 30
        nameAndCompanyCell.addElement(nameParagraph)
        nameAndCompanyCell.addElement(new Paragraph(16, "${badge.company}", nameFont))
        leftSide.addCell(nameAndCompanyCell)

        // Twitter symbol and bar code
        PdfPCell twitterAndBarCodeCell = new PdfPCell()
        twitterAndBarCodeCell.with {
            borderWidth = cellBorderWidth
            paddingLeft = 10
            paddingRight = 10
            verticalAlignment = ALIGN_BOTTOM
        }
        Paragraph twitterParagraph = new Paragraph()
        twitterParagraph.add(new Phrase("\uF099", twitterFont))
        if (badge.twitterAccount) {
            def twitterAccount = badge.twitterAccount.startsWith('@') ? badge.twitterAccount : '@' + badge.twitterAccount
            twitterParagraph.add(new Phrase(twitterAccount, twitterAccountFont))
        }
        twitterParagraph.spacingAfter = 10
        twitterAndBarCodeCell.addElement(twitterParagraph)

        Barcode codeEAN = new Barcode39()
        codeEAN.with {
            code = badge.id
            font = null // no text below the barcode
            barHeight = 3f
        }
        Image barcode = codeEAN.createImageWithBarcode(writer.getDirectContent(), null, null)
        twitterAndBarCodeCell.addElement(barcode)

        leftSide.addCell(twitterAndBarCodeCell)

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
        String ticketType = titleCase(badge.ticketType)
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
        csvWriter.writeNext([titleCase(badge.lastname), titleCase(badge.firstname), badge.company, badge.email] as String[])
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
            URL backgroundURL = (this.class.getResource("/images/background-${badge.ticketType?.toLowerCase()}.png") ?:
                this.class.getResource("/images/background-default.png"))?.toURI()?.toURL()
            if (backgroundURL) {
                cellEvent = new ImageBackgroundEvent(Image.getInstance(backgroundURL))
            }
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
        (labelCount % pageLayout.columns).times { addFiller() } // fillers nécessaires pour compléter la dernière ligne
        document.add(docTable)
        document.close()
    }


    private Closure milliToPoints = { float number ->
        return number * 28.35 / 10 as float
    }

    private double calculateLabelHeight() {
        (pageLayout.format.height - document.topMargin() - document.bottomMargin()) / pageLayout.rows
    }

    private static String titleCase(String s) {

        if (s == null) {
            return null
        }

        // TODO implémentation bancale. Améliorer si possible ou supprimer.
        def titleCaseString = s.split(/(?i)[^a-z]/).collect {
            it.length() > 0 ? (it[0].toUpperCase() + (it.length() > 1 ? it[1..-1].toLowerCase() : '')) : it
        }.join(' ')

        def result = new StringBuilder(titleCaseString.length())

        titleCaseString.eachWithIndex { c, i ->
            def original = s[i]
            if (!(c ==~ /(?i)[a-z]/) && c != original) {
                result << original
            } else {
                result << c
            }
        }

        return result.toString().replaceAll(/\s+/, ' ')
    }
}
