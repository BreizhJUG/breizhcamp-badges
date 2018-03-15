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

def bttfFormatter = { s ->
    def onlyLetters = s.replaceAll(/(?i)[^\p{L}]/, ' ') // replace everything but letters by a space
    def noAccents = Normalizer
            .normalize(onlyLetters, Normalizer.Form.NFD)
            .replaceAll(/\p{InCombiningDiacriticalMarks}+/, '') // remove all accents
}

def _ticketTypes = ['Combo', 'Mercredi', 'Exposant', 'Conf', 'Speaker', 'Jeudi', 'Vendredi']

badgegenerator {
    parser {
        csv {
            fieldmapping = [
                    'Identifiant'        : 'id',
                    'Nom participant'    : 'lastname',
                    'Prénom participant' : 'firstname',
                    'E-mail Participant' : 'email',
                    'Societe Participant': 'company',
                    'Tarif'              : 'ticketType',
                    'twitter'            : 'twitterAccount'
            ]
            valueconverters = [
                    ticketType    : { value ->
                        return [
                                'bénévoles'                  : _ticketTypes[0],
                                'Combo 3 jours'              : _ticketTypes[0],
                                'Conférence (jeudi+vendredi)': _ticketTypes[3],
                                'exposant'                   : _ticketTypes[2],
                                'Fanboy (3 jours)'           : _ticketTypes[0],
                                'last minute'                : _ticketTypes[0],
                                'organisateur'               : _ticketTypes[0],
                                'Speaker'                    : _ticketTypes[4],
                                'sponsor'                    : _ticketTypes[0],
                                'Université (mercredi)'      : _ticketTypes[1],
                                'Jeudi'                      : _ticketTypes[5],
                                'Vendredi'                   : _ticketTypes[6]
                        ][value]
                    },
                    twitterAccount: { String value ->
                        if (value) {
                            if (value.startsWith('http')) {
                                return '@' + value.substring(value.lastIndexOf('/') + 1)
                            }
                            if (value.startsWith('@')) {
                                return value
                            } else {
                                return '@' + value
                            }
                        }
                    },
                    firstname     : { String value -> value.toUpperCase() },
                    lastname      : { String value -> value.toLowerCase() }
            ]
        }
        cli {
            ticketTypes = _ticketTypes
        }
    }
    pdfwriter {
        valueformatters = [
                lastname : { s -> s ? "\u00A0>${s}" : '' },
                firstname: { s -> "${s}<" }
        ]
    }
}
