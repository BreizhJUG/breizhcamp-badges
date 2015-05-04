package org.breizhcamp.badge

import groovy.transform.Immutable
import groovy.transform.ToString

@Immutable
@ToString(includeNames = true)
class Badge {

    String lastname, firstname, email, company, ticketType

}
