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

    private final BaseFont badgeBaseFont, symbolBaseFont, nameBaseFont
    private final Font idFont, nameFont, companyFont, ticketTypeFont, twitterFont, twitterAccountFont
    private final PdfWriter writer

    private final Map formatters

    private final Random angleRandom = new Random()

    PdfBadgeGenerator(OutputStream outputStream, PageLayout pageLayout, boolean debug = false, Map formatters = [:]) {

        if (pageLayout == null) throw new IAE('pageLayout must not be null')
        if (outputStream == null) throw new IAE('outputStream must not be null')
        if (formatters == null) throw new IAE('formatters must not be null')

        badgeBaseFont = createFont('/fonts/nunito/Nunito-Light.ttf', IDENTITY_H, EMBEDDED)
        symbolBaseFont = createFont('/fonts/fontawesome/fontawesome-webfont.ttf', IDENTITY_H, EMBEDDED)
        nameBaseFont = createFont('/fonts/BTTF/BTTF.ttf', IDENTITY_H, EMBEDDED)
        idFont = new Font(badgeBaseFont, 8, Font.NORMAL, BaseColor.GRAY)
        nameFont = new Font(nameBaseFont, 13, Font.NORMAL, BaseColor.BLACK)
        companyFont = new Font(badgeBaseFont, 12, Font.NORMAL, BaseColor.GRAY)
        twitterAccountFont = new Font(badgeBaseFont, 12, Font.BOLD, BaseColor.BLACK)
        ticketTypeFont = new Font(badgeBaseFont, 14, Font.BOLD, BaseColor.WHITE)
        twitterFont = new Font(symbolBaseFont, 16, Font.NORMAL, new BaseColor(58, 170, 225)) // Twitter color

        this.pageLayout = pageLayout

        document = [pageLayout.format, pageLayout.margins.collect(milliToPoints)].flatten() as Document
        writer = PdfWriter.getInstance(document, outputStream)
        this.formatters = formatters

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

        // Badge ID
        PdfPCell idCell = new PdfPCell(new Phrase(badge.id, idFont))
        idCell.with {
            horizontalAlignment = ALIGN_RIGHT
            verticalAlignment = ALIGN_TOP
            paddingRight = 5
            borderWidth = cellBorderWidth
        }
        leftSide.addCell(idCell)

        // Name and company
        PdfPCell nameAndCompanyCell = new PdfPCell()
        nameAndCompanyCell.with {
            horizontalAlignment = ALIGN_LEFT
            verticalAlignment = ALIGN_TOP
            paddingLeft = 10
            borderWidth = cellBorderWidth
        }
        def firstname = formatValue(badge.firstname, 'firstname')
        def lastname = formatValue(badge.lastname, 'lastname')
        def nameParagraph = new Paragraph(16, "${firstname}\n${lastname}", nameFont)
        nameParagraph.spacingAfter = 8
        nameAndCompanyCell.addElement(nameParagraph)
        nameAndCompanyCell.addElement(new Paragraph(12, badge.company, companyFont))
        leftSide.addCell(nameAndCompanyCell)

        // QRCode content
        StringWriter qrCodeTextWriter = new StringWriter()
        CSVWriter csvWriter = new CSVWriter(qrCodeTextWriter, ',' as char, '"' as char, '\\' as char)
        csvWriter.writeNext([badge.lastname, badge.firstname, badge.company, badge.email] as String[])
        csvWriter.flush()

        // QRCode cell
        def qrCodeImage = new BarcodeQRCode(qrCodeTextWriter.toString(),
                110, 110,
                [(EncodeHintType.CHARACTER_SET): 'UTF-8']).image
        qrCodeImage.alignment = Image.TEXTWRAP
        PdfPCell qrcodeCell = new PdfPCell(qrCodeImage)
        qrcodeCell.with {
            paddingTop = 0
            horizontalAlignment = ALIGN_CENTER
            verticalAlignment = ALIGN_BOTTOM
            borderWidth = cellBorderWidth
        }
        leftSide.addCell(qrcodeCell)

        // Twitter symbol and bar code
/*        PdfPCell twitterCell = new PdfPCell()
        twitterCell.with {
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
        twitterCell.addElement(twitterParagraph)

        leftSide.addCell(twitterCell)*/

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
            paddingTop = 45
            paddingRight = 10
            borderWidth = cellBorderWidth
        }
        rightSide.addCell(ticketTypeCell)

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

    private formatValue(String value, String key) {
        def formatter = formatters[key]
        return formatter ? formatter(value) : value
    }
}
