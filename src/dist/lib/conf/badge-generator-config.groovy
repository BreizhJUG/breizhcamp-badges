import java.text.Normalizer

def titleCase = { s ->
    if (s == null) {
        return null
    }

    def titleCaseString = s.split(/(?i)[^\p{L}]/).collect {
        it.length() > 0 ? (it[0].toUpperCase() + (it.length() > 1 ? it[1..-1].toLowerCase() : '')) : it
    }.join(' ')

    def result = new StringBuilder(titleCaseString.length())

    titleCaseString.eachWithIndex { c, i ->
        def original = s[i]
        if (!(c ==~ /(?i)\p{L}/) && c != original) {
            result << original
        } else {
            result << c
        }
    }

    return result.toString().replaceAll(/\s+/, ' ')
}

def unsupportedChars = ['A', 'D', 'E', 'G', 'J', 'L', 'M', 'Q', 'R', 'T', 'X', 'Z']

def symbolsChars = [
        'al' : 'A',
        'b'  : 'B',
        'c'  : 'C',
        'db' : 'D',
        'eu' : 'E',
        'f'  : 'F',
        'ga' : 'G',
        'h'  : 'H',
        'i'  : 'I',
        'dy' : 'J',
        'k'  : 'K',
        'li' : 'L',
        'mg' : 'M',
        'n'  : 'N',
        'o'  : 'O',
        'p'  : 'P',
        'uuq': 'Q',
        'rb' : 'R',
        's'  : 'S',
        'ti' : 'T',
        'u'  : 'U',
        'v'  : 'V',
        'w'  : 'W',
        'xe' : 'X',
        'y'  : 'Y',
        'zn' : 'Z',
        'ni' : '0',
        'cu' : '1',
        'ge' : '2',
        'as' : '3',
        'se' : '4',
        'br' : '5',
        'ba' : '6',
        'kr' : '7',
        'sr' : '8',
        'zr' : '9',
        'ne' : '$',
        'pt' : '¢',
        'au' : '£',
        'tl' : '¥',
        'hg' : '¤',
        'sc' : '+',
        'mn' : '-',
        'ca' : '*',
        'co' : '/',
        'ru' : '=',
        'na' : '%',
        'be' : '#',
        'at' : '@',
        'si' : '&',
        'te' : '_',
        'cl' : '(',
        'ar' : ')',
        'cr' : ',',
        'fe' : '.',
        'mo' : ';',
        'nb' : ':',
        'sm' : '¿',
        'pd' : '?',
        'ir' : '¡',
        'he' : '!',
        'in' : '\\',
        'ta' : '|',
        'hf' : '{',
        're' : '}',
        'tc' : '<',
        'rh' : '>',
        'cd' : '[',
        'sn' : ']',
        'bi' : '§',
        'uup': '¶',
        'uut': 'µ',
        'cs' : '`',
        'sb' : '^',
        'os' : '~',
        'ag' : '©',
        'rf' : '®',
        'gd' : 'À',
        'tb' : 'Á',
        'ho' : 'Â',
        'ds' : 'Ã',
        'er' : 'Ä',
        'tm' : 'Å',
        'yb' : 'Æ',
        'lu' : 'Ç',
        'ac' : 'È',
        'th' : 'É',
        'pa' : 'Ê',
        'np' : 'Ë',
        'pu' : 'Ì',
        'am' : 'Í',
        'cm' : 'Î',
        'bk' : 'Ï',
        'es' : 'Ñ',
        'fm' : 'Ò',
        'md' : 'Ó',
        'no' : 'Ô',
        'lr' : 'Õ'
]

def breakingBadFormatter = { s ->
    def onlyLetters = s.replaceAll(/(?i)[^\p{L}]/, ' ') // replace everything but letters by a space
    def noAccents = Normalizer
            .normalize(onlyLetters, Normalizer.Form.NFD)
            .replaceAll(/\p{InCombiningDiacriticalMarks}+/, '') // remove all accents


    if (!unsupportedChars.find { noAccents.contains(it) }) { // if no unsupported char is found
        return noAccents
    } else {
        def lowerCased = noAccents.toLowerCase() // lower case original string

        def symbolIndex = symbolsChars.keySet().collectEntries {
            // find the chemistry symbol char sequence closest to the beginning of the string
            [(it): lowerCased.indexOf(it)]
        }.findAll {
            it.value != -1
        }.inject([:]) { result, symbol, index ->
            def values = result.values() as List
            if (values) {
                values[0] < index ? result : [(symbol): index]
            } else {
                [(symbol): index]
            }
        }

        if (symbolIndex) { // only one key value pair in symbolIndex
            def symbol = (symbolIndex.keySet() as List)[0]
            def index = (symbolIndex.values() as List)[0]
            def result = new StringBuilder()

            def i = 0
            while (i < lowerCased.length()) {
                if (i == index) {
                    result << symbolsChars[symbol]
                    i += symbol.length()
                } else {
                    result << lowerCased[i]
                    i++
                }
            }
            return result.toString()
        } else {
            return lowerCased
        }
    }
}

def _ticketTypes = ['Combo', 'Université', 'Exposant', 'Conf', 'Team']

badgegenerator {
    parser {
        csv {
            fieldmapping = [
                    'Identifiant'       : 'id',
                    'Nom participant'   : 'lastname',
                    'Prénom participant': 'firstname',
                    'E-mail Participant': 'email',
                    'Société'           : 'company',
                    'Tarif'             : 'ticketType',
                    'Twitter'           : 'twitterAccount'
            ]
            valueconverters = [
                    ticketType    : { value ->
                        return [
                                'Billets sponsor'            : _ticketTypes[0],
                                'Université (mercredi)'      : _ticketTypes[1],
                                'exposant'                   : _ticketTypes[2],
                                'Conférence (jeudi+vendredi)': _ticketTypes[3],
                                'Combo (3 jours)'            : _ticketTypes[0],
                                'Billet speaker'             : _ticketTypes[0],
                                'Billet organisateur'        : _ticketTypes[4],
                                'Supporter'                  : _ticketTypes[0],
                                'Early bird (3 jours)'       : _ticketTypes[0]
                        ][value]
                    },
                    twitterAccount: { String value ->
                        if (value) {
                            if (value.startsWith('http')) {
                                return '@' + value.substring(value.lastIndexOf('/') + 1)
                            }
                            if (!value.startsWith('@')) {
                                return '@' + value
                            }
                        }
                    },
                    firstname     : titleCase,
                    lastname      : titleCase
            ]
        }
        cli {
            ticketTypes = _ticketTypes
        }
    }
    pdfwriter {
        valueformatters = [
                lastname : breakingBadFormatter,
                firstname: breakingBadFormatter
        ]
    }
}