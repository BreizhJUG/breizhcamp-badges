package org.breizhcamp.badge.pdf

import spock.lang.Specification

class PdfBadgeGeneratorSpec extends Specification {

    def 'test titleCase()'() {

        expect:
        PdfBadgeGenerator.titleCase(s) == expected

        where:
        s                      || expected
        null                   || null
        ''                     || ''
        'a'                    || 'A'
        'a b c'                || 'A B C'
        'Eddard\nStark'        || 'Eddard Stark'      // line break is a space character
        'Eddard\u00A0Stark'    || 'Eddard\u00A0Stark' // unbreakable space is not a space character
        'EDDARD STARK'         || 'Eddard Stark'
        'eDDARD sTARK'         || 'Eddard Stark'
        'arstan barbe-blanche' || 'Arstan Barbe-Blanche'
    }
}
