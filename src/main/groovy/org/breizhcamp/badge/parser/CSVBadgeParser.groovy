package org.breizhcamp.badge.parser

import com.xlson.groovycsv.CsvIterator
import com.xlson.groovycsv.CsvParser
import com.xlson.groovycsv.PropertyMapper
import org.breizhcamp.badge.Badge

import java.lang.IllegalArgumentException as IAE
import java.util.function.Consumer

class CSVBadgeParser implements BadgeParser {

    private CsvIterator linesIterator

    CSVBadgeParser(InputStream input, Map options = [:]) {

        if (input == null) throw new IAE('input (InputStream) parameter must not be null')
        if (options == null) throw new IAE('options Map parameter must not be null')

        linesIterator = new CsvParser().parse(options, new InputStreamReader(input, options?.encoding ?: 'UTF-8'))
    }

    @Override
    boolean hasNext() {
        return linesIterator.hasNext()
    }

    @Override
    Badge next() {
        PropertyMapper mapper = linesIterator.next()
        def values = mapper.values as List
        return new Badge(mapper.columns.collectEntries {
            [(it.key): values[it.value]]
        })
    }

    @Override
    void remove() {
        throw new UnsupportedOperationException()
    }

    @Override
    void forEachRemaining(Consumer<? super Badge> action) {
        super.forEachRemaining(action)
    }
}
