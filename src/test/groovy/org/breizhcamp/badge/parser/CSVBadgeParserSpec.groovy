package org.breizhcamp.badge.parser

import org.breizhcamp.badge.Badge
import spock.lang.Specification
import spock.lang.Unroll

class CSVBadgeParserSpec extends Specification {

    CSVBadgeParser parser

    @Unroll
    def 'test constructing CSVBadgeParser with input=#inputStream and options=#options'() {

        when: 'passing wrong parameters'
        parser = new CSVBadgeParser(inputStream, options)

        then: 'an error is thrown'
        thrown(java.lang.IllegalArgumentException)

        where:
        inputStream       | options
        null              | [:]
        Mock(InputStream) | null
    }

    def 'test constructing CSVBadgeParser'() {

        given:
        def input = getClass().getResourceAsStream('nominal.csv')

        when:
        parser = new CSVBadgeParser(input, [:])

        then:
        parser.linesIterator.hasNext()
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

    def 'test encoding traps'() {
        given:
        def input = getClass().getResourceAsStream(csvFile)
        parser = new CSVBadgeParser(input, [encoding: wrongEncoding])

        expect:
        parser.hasNext()

        when:
        Badge content = parser.next()

        then: 'characters are not interpreted correctly'
        !content.firstname.startsWith(expected)

        where:
        csvFile                    | wrongEncoding || expected
        'utf-8-encoded.csv'        | 'ISO-8859-1'  || 'Œ'
        'windows-1252-encoded.csv' | 'UTF-8'       || 'Œ'
    }

    def 'test remove() is not supported'() {
        given:
        def input = getClass().getResourceAsStream('nominal.csv')
        parser = new CSVBadgeParser(input, [:])

        when:
        parser.remove()

        then:
        thrown(UnsupportedOperationException)
    }
}
