import au.com.bytecode.opencsv.CSVWriter
import com.itextpdf.text.*
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import com.xlson.groovycsv.CsvParser

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
def readFirstLine = false

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
def ticketTypeFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);

PdfPTable mainLayout = new PdfPTable(2)
mainLayout.widthPercentage = 100
mainLayout.widths = [1f, 1f] as float[]

def cellBorderWidth = debug ? 1f : 0f

def ticketTypeColors = ['Speaker': new BaseColor(228, 26, 28), // red
        'Hackerspace': new BaseColor(55, 126, 184), // blue
        'Conférence': new BaseColor(77, 175, 74), // green
        'Combo': new BaseColor(152, 78, 163)] //purple

csvFile.withReader('UTF-8') { reader ->

    def lines = CsvParser.parseCsv([separator: separator,
            quoteChar: quoteChar,
            escapeChar: escapeChar,
            readFirstLine: readFirstLine],
            reader)

    lines.each { cells ->

        ++participantsCount

        PdfPTable labelLayout = new PdfPTable(2)
        labelLayout.with {
            widthPercentage = 100
            widths = [0.4f, 0.6f] as float[]
        }

        // QRCode
        StringWriter qrCodeTextWriter = new StringWriter()
        CSVWriter csvWriter = new CSVWriter(qrCodeTextWriter, separator, quoteChar, escapeChar)
        csvWriter.writeNext([cells.lastname, cells.firstname, cells.company, cells.email] as String[])
        csvWriter.flush()

        PdfPCell qrcodeCell = new PdfPCell(Image.getInstance("https://chart.googleapis.com/chart?cht=qr&chs=100x100&chl=${URLEncoder.encode(qrCodeTextWriter.toString(), 'UTF-8')}"))
        qrcodeCell.with {
            rowspan = 3
            fixedHeight = milliToPoints(63.5)
            horizontalAlignment = ALIGN_CENTER
            verticalAlignment = ALIGN_MIDDLE
            borderWidth = cellBorderWidth
        }
        labelLayout.addCell(qrcodeCell)

        // Ticket type
        def ticketType = cells.ticketType
        PdfPCell ticketTypeCell = new PdfPCell(new Phrase(ticketType, ticketTypeFont))
        ticketTypeCell.with {
            horizontalAlignment = ALIGN_CENTER
            backgroundColor = ticketTypeColors[ticketType]
            borderWidth = cellBorderWidth
        }
        labelLayout.addCell(ticketTypeCell)

        // Name
        PdfPCell nameCell = new PdfPCell(new Phrase("${cells.lastname}\n${cells.firstname}", nameFont))
        nameCell.with {
            horizontalAlignment = ALIGN_CENTER
            verticalAlignment = ALIGN_MIDDLE
            borderWidth = cellBorderWidth
        }
        labelLayout.addCell(nameCell)

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