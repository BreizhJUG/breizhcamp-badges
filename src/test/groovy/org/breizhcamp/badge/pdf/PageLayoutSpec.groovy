package org.breizhcamp.badge.pdf

import com.itextpdf.text.PageSize
import org.breizhcamp.badge.pdf.PageLayout
import spock.lang.Specification


class PageLayoutSpec extends Specification {

    PageLayout layout

    def 'test constructor with wrong arguments'() {

        when:
        layout = new PageLayout(format, margins, columns, rows)

        then:
        thrown(IllegalArgumentException)

        where:
        format  | margins                             | columns | rows
        null    | [9, 9, 21.5, 21.5] as float[]       | 2       | 4
        'WRONG' | [9, 9, 21.5, 21.5] as float[]       | 2       | 4
        'A4'    | null                                | 2       | 4
        'A4'    | [9, 21.5] as float[]                | 2       | 4
        'A4'    | [9, 9, 21.5, 21.5, 21.5] as float[] | 2       | 4
        'A4'    | [9, -1, 21.5, 21.5] as float[]      | 2       | 4
        'A4'    | [9, 9, 21.5, 21.5] as float[]       | 0       | 4
        'A4'    | [9, 9, 21.5, 21.5] as float[]       | 2       | 0
        'A4'    | [9, 9, 21.5, 21.5] as float[]       | 0       | 0
    }

    def 'test constructor in nominal case'() {

        when:
        layout = new PageLayout(format, margins, columns, rows)

        then:
        layout.format == PageSize.getRectangle(format)
        layout.margins == margins
        layout.columns == columns
        layout.rows == rows

        where:
        format | margins                       | columns | rows
        'A4'   | [9, 9, 21.5, 21.5] as float[] | 2       | 4
    }
}
