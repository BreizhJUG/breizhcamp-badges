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
}