import org.breizhcamp.badge.PageLayout
import org.breizhcamp.badge.pdf.PdfBadgeGenerator
import org.breizhcamp.badge.parser.*

import java.util.function.Consumer
import java.util.function.Supplier

def cli = new CliBuilder(usage: '''breizhcamp-badge [-d] [-o <path>] [-sc <char>] [-qc <char>] [-ec <char>] [-cc <string>] csv-file
[-d] -o <path>
-h''', width: 120, header: 'Options description:')
cli.with {
    d(args: 0, longOpt: 'debug', required: false, 'Debug mode')
    h(args: 0, longOpt: 'help', required: false, 'Show usage information')
    p(args: 1, argName: 'int', longOpt: 'position', required: false, '''Position of first badge on the label sheet.
The label on the upper left corner is the one with position 0, the one on its right is at position 1 and so on.''')
    o(args: 1, argName: 'path', longOpt: 'output', required: false, '''Ouput PDF file path (optional in CSV mode).
If not specified in CSV mode, the PDF file will be generated in the same directory as the CSV and have the same name, except for the extension.
In any case, if the file already exists, it is overwritten.''')
    sc(args: 1, argName: 'char', longOpt: 'separator', required: false, 'Separator character used in the input CSV file. Defaults to \',\'.')
    qc(args: 1, argName: 'char', longOpt: 'quote', required: false, 'Quote character used in the input CSV file. Defaults to \'"\'.')
    ec(args: 1, argName: 'char', longOpt: 'escape', required: false, 'Escape character used in the input CSV file. Defaults to \'\\\'.')
    cc(args: 1, argName: 'string', longOpt: 'charset', required: false, 'Charset used in the CSV file. Defaults to UTF-8.')
}

def options = cli.parse(args)

if (options.h) {
    showMessageUsageAndQuit(cli)
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

// CSV file?
def csvFile = arguments[0] ? new File(arguments[0]) : null

// PDF file
def pdfPath
if (csvFile) {
    pdfPath = options.o ?: new File(csvFile.parentFile.path, csvFile.name.replaceAll(/\.\w+$/, '.pdf'))
} else {
    if (!options.o) showMessageUsageAndQuit(cli, 'error: Missing required option: o')
    pdfPath = options.o
}

// Parser factory initialization
BadgeParserFactory factory = BadgeParserFactory.create([accept: { Builder builder ->
    builder.register(null, [get: { new CLIBadgeParser() }] as Supplier<BadgeParser>)
    builder.register('csv', [get: {
        new CSVBadgeParser(csvFile.newInputStream(), [separator    : separator,
                                                      quoteChar    : quoteChar,
                                                      escapeChar   : escapeChar,
                                                      readFirstLine: !hasHeader,
                                                      encoding     : charset])
    }] as Supplier<BadgeParser>)
}] as Consumer<Builder>);

BadgeParser badges = factory.build(csvFile ? 'csv' : null) // TODO use file extension maybe?

PageLayout pageLayout = PageLayout.L4745REV

PdfBadgeGenerator generator = new PdfBadgeGenerator(new FileOutputStream(pdfPath), pageLayout, debug)

generator.startDocument()

(position).times {
    generator.addFiller()
}

badges.each { badge ->
    generator.addBadge(badge)
}

generator.endDocument()


private void showMessageUsageAndQuit(CliBuilder cli, String message = null) {
    if (message) {
        println message
    }
    cli.usage()
    return
}