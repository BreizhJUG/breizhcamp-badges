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
                    'Identifiant'   : 'id',
                    'Nom'           : 'lastname',
                    'Prénom'        : 'firstname',
                    'E-mail'        : 'email',
                    'Entreprise #11': 'company',
                    'Tarif'         : 'ticketType',
                    'Twitter #5739' : 'twitterAccount'
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
                                'Organisation'               : _ticketTypes[0],
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
                    firstname     : { String value -> bttfFormatter(value).toUpperCase() },
                    lastname      : { String value -> bttfFormatter(value).toLowerCase() }
            ]
        }
        cli {
            ticketTypes = _ticketTypes
        }
    }
    pdfwriter {
        valueformatters = [
                lastname : { s -> s ? ">${s}" : '' },
                firstname: { s -> "${s}<" }
        ]
    }
}
