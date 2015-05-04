import au.com.bytecode.opencsv.CSVWriter
import com.itextpdf.text.*
import com.itextpdf.text.pdf.BarcodeQRCode
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import com.itextpdf.text.pdf.qrcode.EncodeHintType
import com.xlson.groovycsv.CsvParser
import org.breizhcamp.badge.ImageBackgroundEvent

def cli = new CliBuilder(usage: 'badge-generator -i <path> [-o <path>] [-sc <char>] [-qc <char>] [-ec <char>] [-d]')
cli.with {
    d(args: 0, longOpt: 'debug', required: false, 'Debug mode')
    i(args: 1, argName: 'path', longOpt: 'input', required: true, 'Input CSV file path')
    o(args: 1, argName: 'path', longOpt: 'output', required: false, '''Ouput PDF file path (optional).
If not specified, the PDF file will be generated in the same directory as the CSV and have the same name, except for the extension.
If the file already exists, it is overwritten.''')
    sc(args: 1, argName: 'char', longOpt: 'separator', required: false, 'Separator character used in the input CSV file. Defaults to \',\'.')
    qc(args: 1, argName: 'char', longOpt: 'quote', required: false, 'Quote character used in the input CSV file. Defaults to \'"\'.')
    ec(args: 1, argName: 'char', longOpt: 'escape', required: false, 'Escape character used in the input CSV file. Defaults to \'\\\'.')
}

def options = cli.parse(args)

if (!options) {
    return
}

// CSV parser parameters
def separator = (options.sc ?: ',') as char
def quoteChar = (options.qc ?: '"') as char
def escapeChar = (options.ec ?: '\\') as char
def hasHeader = true

def debug = options.d as boolean

def csvFile = new File(options.i)
def pdfFile = options.o ?: new File(csvFile.parentFile.path, csvFile.name.replaceAll(/\.\w+$/, '.pdf'))

private float milliToPoints(number) {
    return number * 28.35 / 10
}

def participantsCount = 0

def widthMargin = milliToPoints(9) // Margins for the labels sheet format L4745REV (63.5 x 96)
def heightMargin = milliToPoints(21.5)
def document = new Document(PageSize.A4, widthMargin, widthMargin, heightMargin, heightMargin)
PdfWriter.getInstance(document, new FileOutputStream(pdfFile))
document.open()

// Fonts definition
def nameFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, BaseColor.BLACK);
def ticketTypeFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, BaseColor.WHITE);

def backgrounds = [Team    : 'orange',
                   Conf    : 'blue',
                   Speaker : 'yellow',
                   Hacker  : 'violet',
                   Combo   : 'green',
                   Exposant: 'black']

PdfPTable mainLayout = new PdfPTable(2)
mainLayout.widthPercentage = 100
mainLayout.widths = [1f, 1f] as float[]

def cellBorderWidth = debug ? 1f : 0f

csvFile.withReader('UTF-8') { reader ->

    def lines = CsvParser.parseCsv([separator    : separator,
                                    quoteChar    : quoteChar,
                                    escapeChar   : escapeChar,
                                    readFirstLine: !hasHeader],
            reader)

    lines.each { cells ->

        ++participantsCount

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
            fixedHeight = milliToPoints(63.5)
            borderWidth = cellBorderWidth
        }
        def nameParagraph = new Paragraph(24, "${cells.firstname}\n${cells.lastname}", nameFont)
        nameParagraph.setSpacingAfter(32)
        nameCell.addElement(nameParagraph)
        nameCell.addElement(new Paragraph(16, "${cells.company}", nameFont))
        labelLayout.addCell(nameCell)

        // Right part
        PdfPTable rightSide = new PdfPTable(1)
        rightSide.with {
            widthPercentage = 100
            widths = [1f] as float[]
        }

        // Ticket type
        String ticketType = cells.ticketType
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
        CSVWriter csvWriter = new CSVWriter(qrCodeTextWriter, separator, quoteChar, escapeChar)
        csvWriter.writeNext([cells.lastname, cells.firstname, cells.company, cells.email] as String[])
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
            cellEvent = new ImageBackgroundEvent(Image.getInstance(this.class.getResource("${backgrounds[cells.ticketType]}.png").toURI().toURL()))
            borderWidth = cellBorderWidth
        }
        labelLayout.addCell(rightSideWrapper)

        // Wrapper to remove label border
        def labelWrapper = new PdfPCell(labelLayout)
        labelWrapper.borderWidth = cellBorderWidth
        mainLayout.addCell(labelWrapper)
    }
}

(participantsCount % 2).times { // complète la dernière ligne pour obtenir la dernière ligne du tableau
    def fillerCell = new PdfPCell()
    fillerCell.borderWidth = cellBorderWidth
    mainLayout.addCell(fillerCell)
}

document.add(mainLayout)
document.close()
