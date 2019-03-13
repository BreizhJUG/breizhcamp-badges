package org.breizhcamp.badge

import groovy.transform.Immutable
import groovy.transform.ToString

@Immutable
@ToString(includeNames = true)
class Badge {

    String id, lastname, firstname, email, company, ticketType, twitterAccount, noGoodies, tShirtFitting, tShirtSize

}
