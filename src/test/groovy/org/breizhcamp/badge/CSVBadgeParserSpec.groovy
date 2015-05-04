package org.breizhcamp.badge

import spock.lang.Specification
import spock.lang.Unroll

class CSVBadgeParserSpec extends Specification {

    CSVBadgeParser parser

    @Unroll
    def 'test constructing CSVBadgeParser with input=#inputStream and options=#options'() {

        when: 'passing wrong parameters'
        parser = new CSVBadgeParser(inputStream, options)

        then: 'an error is thrown'
        thrown(java.lang.AssertionError)

        where:
        inputStream       | options
        null              | [:]
        Mock(InputStream) | null
    }

    def 'test constructing CSVBadgeParser'() {

        given:
        def input = new ByteArrayInputStream("""lastname,firstname,email,company,ticketType
,Œdipe,œdipe@mythologie.gr,Complex & Associates,Team
""".getBytes('UTF-8'))

        when:
        parser = new CSVBadgeParser(input, options)

        then:
        parser.linesIterator.hasNext()

        where:
        options << [[:], [encoding: 'ISO-8859-1']]
    }

    def 'test next() with wrong headers in CSV'() {

        given:
        def input = getClass().getResourceAsStream(csvFile)
        parser = new CSVBadgeParser(input, [separator: ','])

        expect:
        parser.hasNext()

        when:
        parser.next()

        then:
        thrown(MissingPropertyException)

        where:
        csvFile << ['wrong-headers.csv', 'unsupported-header.csv']
    }

    def 'test next() in nominal cases'() {
        given:
        def input = getClass().getResourceAsStream(csvFile)
        parser = new CSVBadgeParser(input, [separator: ','])

        expect:
        parser.hasNext()

        when:
        Badge content = parser.next()

        then:
        content == expected
        !parser.hasNext()

        where:
        csvFile              || expected
        'nominal.csv'        || new Badge(lastname: 'Snow', firstname: 'Jon', email: 'jon.snow@castle-black.wes', company: 'The Night\'s Watch', ticketType: 'Speaker')
        'missing-header.csv' || new Badge(lastname: 'Snow', firstname: 'Jon', email: 'jon.snow@castle-black.wes', company: 'The Night\'s Watch', ticketType: null)
    }
}
