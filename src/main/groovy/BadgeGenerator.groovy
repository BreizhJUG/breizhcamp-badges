import org.breizhcamp.badge.parser.*
import org.breizhcamp.badge.pdf.PageLayout
import org.breizhcamp.badge.pdf.PdfBadgeGenerator

import java.util.function.Consumer
import java.util.function.Supplier

def cli = new CliBuilder(usage: '''breizhcamp-badge [-d] [-m <string>] [-o <path>] [-sc <char>] [-qc <char>] [-ec <char>] [-cc <string>] csv-file
[-d] [-m <string>] -o <path>
-h''', width: 120, header: 'Options description:')
cli.with {
    d(args: 0, longOpt: 'debug', required: false, 'Debug mode')
    h(args: 0, longOpt: 'help', required: false, 'Show usage information')
    p(args: 1, argName: 'int', longOpt: 'position', required: false, '''Position of first badge on the label sheet.
The label on the upper left corner is the one with position 0, the one on its right is at position 1 and so on.''')
    o(args: 1, argName: 'path', longOpt: 'output', required: false, '''Ouput PDF file path (optional in CSV mode).
If not specified in CSV mode, the PDF file will be generated in the same directory as the CSV and have the same name, except for the extension.
In any case, if the file already exists, it is overwritten.''')
    m(args: 1, argName: 'string', longOpt: 'margins', required: false, '''Output PDF page margins in mm. Defaults to 9 mm for horizontal margins and 21.5 mm for vertical margins.
String argument may contain 1, 2, 3 or 4 margin values separated by a space.
Examples:
-m 9                   -> left, right, top and bottom margins will be 9 mm.
-m "21.5 9"            -> top and bottom margins will be 21.5 mm. Left and right margins will be 9 mm.
-m "21.6 9 21.4"       -> top margin will be 21.6 mm, bottom will be 21.4 mm. Left and right margins will be 9 mm.
-m "21.6 8.8 21.4 9.2" -> top, right, bottom and left margins will respectively be 21.6 mm, 8.8 mm, 21.4 mm and 9.2 mm.
Partially based on CSS syntax: https://developer.mozilla.org/en-US/docs/Web/CSS/margin#Syntax''')
    sc(args: 1, argName: 'char', longOpt: 'separator', required: false, 'Separator character used in the input CSV file. Defaults to \',\'.')
    qc(args: 1, argName: 'char', longOpt: 'quote', required: false, 'Quote character used in the input CSV file. Defaults to \'"\'.')
    ec(args: 1, argName: 'char', longOpt: 'escape', required: false, 'Escape character used in the input CSV file. Defaults to \'\\\'.')
    cc(args: 1, argName: 'string', longOpt: 'charset', required: false, 'Charset used in the CSV file. Defaults to UTF-8.')
}

def options = cli.parse(args)

if (options.h) {
    showMessageUsageAndQuit(cli)
}

ConfigObject config = new ConfigSlurper().parse(this.class.getResource('/badge-generator-config.groovy'))

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

// Margins
def margins = options.m ? parseMargins(options.m, cli) : null

// Parser factory initialization
BadgeParserFactory factory = BadgeParserFactory.create([accept: { Builder builder ->
    builder.register(null, [get: {
        new CLIBadgeParser(config.badgegenerator.parser.cli.ticketTypes)
    }] as Supplier<BadgeParser>)
    builder.register('csv', [get: {
        new CSVBadgeParser(csvFile.newInputStream(),
                config.badgegenerator.parser.csv.fieldmapping, [separator    : separator,
                                                                quoteChar    : quoteChar,
                                                                escapeChar   : escapeChar,
                                                                readFirstLine: !hasHeader,
                                                                encoding     : charset],
                config.badgegenerator.parser.csv.valueconverters)
    }] as Supplier<BadgeParser>)
}] as Consumer<Builder>);

BadgeParser badges = factory.build(csvFile ? 'csv' : null) // TODO use file extension maybe?

PageLayout pageLayout = PageLayout.L4745REV

// Adjust margins
if (margins) {
    pageLayout.margins = margins
}

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
    System.exit(0)
}

private float[] parseMargins(String marginString, CliBuilder cli) {

    try {
        def parts = marginString.split(/\s+/)
        switch (parts.length) {
            case 1:
                def margin = parts[0] as float
                return [margin, margin, margin, margin]
            case 2:
                def vertMargin = parts[0] as float
                def horizMargin = parts[1] as float
                return [horizMargin, horizMargin, vertMargin, vertMargin]
            case 3:
                def topMargin = parts[0] as float
                def horizMargin = parts[1] as float
                def bottomMargin = parts[2] as float
                return [horizMargin, horizMargin, topMargin, bottomMargin]
            case 4:
                def topMargin = parts[0] as float
                def rightMargin = parts[1] as float
                def bottomMargin = parts[2] as float
                def leftMargin = parts[2] as float
                return [leftMargin, rightMargin, topMargin, bottomMargin]
            default:
                showMessageUsageAndQuit(cli, "error: option m cannot be interpreted: $marginString")
        }
    } catch (NumberFormatException e) {
        showMessageUsageAndQuit(cli, "error: option m cannot be interpreted: $marginString")
    }
}