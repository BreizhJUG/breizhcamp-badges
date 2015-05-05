import org.breizhcamp.badge.CSVBadgeParser
import org.breizhcamp.badge.PageLayout
import org.breizhcamp.badge.PdfBadgeGenerator

def cli = new CliBuilder(usage: 'badge-generator [-dh] [-o <path>] [[-sc <char>] [-qc <char>] [-ec <char>] [-cc <string>] csv-file]', width: 120)
cli.with {
    d(args: 0, longOpt: 'debug', required: false, 'Debug mode')
    h(args: 0, longOpt: 'help', required: false, 'Show usage information')
    p(args: 1, argName: 'int', longOpt: 'position', required: false, '''Position of first badge on the label sheet.
The label on the upper left corner is the one with position 0, the one on its right is at position 1 and so on.''')
    o(args: 1, argName: 'path', longOpt: 'output', required: false, '''Ouput PDF file path (optional).
If not specified, the PDF file will be generated in the same directory as the CSV and have the same name, except for the extension.
If the file already exists, it is overwritten.''')
    sc(args: 1, argName: 'char', longOpt: 'separator', required: false, 'Separator character used in the input CSV file. Defaults to \',\'.')
    qc(args: 1, argName: 'char', longOpt: 'quote', required: false, 'Quote character used in the input CSV file. Defaults to \'"\'.')
    ec(args: 1, argName: 'char', longOpt: 'escape', required: false, 'Escape character used in the input CSV file. Defaults to \'\\\'.')
    cc(args: 1, argName: 'string', longOpt: 'charset', required: false, 'Charset used in the CSV file. Defaults to UTF-8.')
}

def options = cli.parse(args)

if (!options || !options.arguments() || options.h) {
    cli.usage()
    return
}

// position of first label
int position = (options.p ?: 0) as int

// CSV parser parameters
def separator = (options.sc ?: ',') as char
def quoteChar = (options.qc ?: '"') as char
def escapeChar = (options.ec ?: '\\') as char
def charset = options.cc ?: 'UTF-8'
def hasHeader = true

// Debug mode
def debug = options.d as boolean

List<String> arguments = options.arguments()

// input CSV file
def csvFile = new File(arguments[0])
def pdfFile = options.o ?: new File(csvFile.parentFile.path, csvFile.name.replaceAll(/\.\w+$/, '.pdf'))

def badges = new CSVBadgeParser(csvFile.newInputStream(), [separator    : separator,
                                                           quoteChar    : quoteChar,
                                                           escapeChar   : escapeChar,
                                                           readFirstLine: !hasHeader,
                                                           encoding     : charset])

PageLayout pageLayout = PageLayout.L4745REV

PdfBadgeGenerator generator = new PdfBadgeGenerator(new FileOutputStream(pdfFile), pageLayout, debug)

generator.startDocument()

(position).times {
    generator.addFiller()
}

badges.each { badge ->
    generator.addBadge(badge)
}

generator.endDocument()