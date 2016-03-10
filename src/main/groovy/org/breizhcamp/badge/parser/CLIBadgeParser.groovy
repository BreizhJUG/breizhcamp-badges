package org.breizhcamp.badge.parser

import org.breizhcamp.badge.Badge

import java.util.function.Consumer

class CLIBadgeParser implements BadgeParser {

    boolean hasNext

    List<String> ticketTypes

    CLIBadgeParser(List<String> ticketTypes) {
        this.ticketTypes = ticketTypes
        hasNext = true
    }

    @Override
    boolean hasNext() {
        return hasNext
    }

    @Override
    Badge next() {
        Map<String, String> badgeParams = [:]

        new Scanner(System.in).with { scanner ->
            readParam('ID ? ', 'id', badgeParams, scanner)
            readParam('Nom ? ', 'lastname', badgeParams, scanner)
            readParam('Prénom ? ', 'firstname', badgeParams, scanner)
            readParam('Email ? ', 'email', badgeParams, scanner)
            readParam('Entreprise ? ', 'company', badgeParams, scanner)
            readParam('Compte Twitter ? ', 'twitterAccount', badgeParams, scanner)
            readParam("Type de ticket (${ticketTypes.join(', ')}) ? ", 'ticketType', badgeParams, scanner)

            hasNext = askForNext(scanner)
        }

        return badgeParams as Badge
    }

    @Override
    void remove() {
        throw new UnsupportedOperationException()
    }

    @Override
    void forEachRemaining(Consumer<? super Badge> action) {
        super.forEachRemaining(action)
    }

    private readParam(String prompt, String paramName, Map params, Scanner scanner) {
        print prompt
        String line = scanner.nextLine()
        params[paramName] = line
    }

    private boolean askForNext(Scanner scanner) {
        String line = askForNextPrompt(scanner)
        while (line && line != 'y' && line != 'n') {
            line = askForNextPrompt(scanner)
        }
        return line == 'y'
    }

    private String askForNextPrompt(Scanner scanner) {
        print '\nNouveau badge ? [y/N]'
        return scanner.nextLine()?.toLowerCase()
    }
}
