package org.breizhcamp.badge.parser

import com.xlson.groovycsv.CsvIterator
import com.xlson.groovycsv.CsvParser
import com.xlson.groovycsv.PropertyMapper
import org.breizhcamp.badge.Badge

import java.lang.IllegalArgumentException as IAE
import java.util.function.Consumer

class CSVBadgeParser implements BadgeParser {

    private CsvIterator linesIterator

    private final Map fieldMappings

    private final Map valueConverters

    CSVBadgeParser(InputStream input, Map fieldMappings = [:], Map options = [:], Map converters = [:]) {

        if (input == null) throw new IAE('input (InputStream) parameter must not be null')
        if (fieldMappings == null) throw new IAE('fieldMappings Map parameter must not be null')
        if (options == null) throw new IAE('options Map parameter must not be null')
        if (converters == null) throw new IAE('valueConverters Map parameter must not be null')

        this.linesIterator = new CsvParser().parse(options, new InputStreamReader(input, options?.encoding ?: 'UTF-8'))
        this.fieldMappings = new HashMap(fieldMappings)
        this.valueConverters = new HashMap(converters)
    }

    @Override
    boolean hasNext() {
        return linesIterator.hasNext()
    }

    @Override
    Badge next() {
        PropertyMapper mapper = linesIterator.next()

        return new Badge(fieldMappings.collectEntries { entry ->
            def convertor = valueConverters[entry.value]
            if (convertor) {
                return [(entry.value): convertor(mapper[entry.key])]
            } else {
                return [(entry.value): mapper[entry.key] ?: null]
            }
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
