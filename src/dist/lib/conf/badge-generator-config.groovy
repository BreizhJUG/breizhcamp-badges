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

def codeBustersFormatter = { s ->
    def onlyLetters = s.replaceAll(/(?i)[^\p{L}]/, ' ') // replace everything but letters by a space
    def noAccents = Normalizer
            .normalize(onlyLetters, Normalizer.Form.NFD)
            .replaceAll(/\p{InCombiningDiacriticalMarks}+/, '') // remove all accents
}

def _ticketTypes = ['Combo', 'Mercredi', 'Exposant', 'Conf', 'Speaker', 'Jeudi', 'Vendredi', 'Staff']

badgegenerator {
    parser {
        csv {
            fieldmapping = [
                    'id'            : 'id',
                    'lastname'      : 'lastname',
                    'firstname'     : 'firstname',
                    'email'         : 'email',
                    'company'       : 'company',
                    'ticketType'    : 'ticketType',
                    'twitterAccount': 'twitterAccount'
            ]
            valueconverters = [
                    ticketType    : { value ->
                        return [
                                'Combo (3 jours)'                : _ticketTypes[0],
                                'Combo Sponsors (3 jours)'       : _ticketTypes[0],
                                'Conférences (jeudi et vendredi)': _ticketTypes[3],
                                'Exposant'                       : _ticketTypes[2],
                                'Staff'                          : _ticketTypes[7],
                                'Speaker'                        : _ticketTypes[4],
                                'Mercredi'                       : _ticketTypes[1],
                                'Jeudi'                          : _ticketTypes[5],
                                'Vendredi'                       : _ticketTypes[6]
                        ][value]
                    },
                    twitterAccount: { String value ->
                        if (value && value ==~ /[@A-Za-z]+/) {
                            // font chosen for the twitter handle does not support numbers 😕
                            if (value.startsWith('http')) {
                                return value.substring(value.lastIndexOf('/') + 1).toUpperCase()
                            }
                            if (value.startsWith('@')) {
                                return value.substring(1).toUpperCase()
                            } else {
                                return value.toUpperCase()
                            }
                        }
                    },
                    firstname     : { String value -> codeBustersFormatter(value).toUpperCase() },
                    lastname      : { String value -> codeBustersFormatter(value).toUpperCase() }
            ]
        }
        cli {
            ticketTypes = _ticketTypes
        }
    }
    pdfwriter {
        valueformatters = [
                lastname : { s -> s ? s : '' },
                firstname: { s -> s }
        ]
    }
}
