package org.breizhcamp.badge.parser

import java.util.function.Consumer
import java.util.function.Supplier

class BadgeParserFactory {

    private Map<String, Supplier<BadgeParser>> map

    BadgeParserFactory() {
        this.map = [:]
    }

    BadgeParser build(String name) {
        return map[name].get()
    }

    static BadgeParserFactory create(Consumer<Builder> consumer) {

        BadgeParserFactory factory = new BadgeParserFactory()

        consumer.accept([register: factory.map.&put] as Builder)

        return factory
    }
}
