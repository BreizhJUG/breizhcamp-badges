package org.breizhcamp.badge.parser

import java.util.function.Supplier

interface Builder {

    void register(String name, Supplier<BadgeParser> supplier);
}
