package org.breizhcamp.badge

import com.xlson.groovycsv.CsvIterator
import com.xlson.groovycsv.CsvParser
import com.xlson.groovycsv.PropertyMapper

class CSVBadgeParser implements Iterator<Badge> {

    private CsvIterator linesIterator

    CSVBadgeParser(InputStream input, Map options = [:]) {

        assert input != null, 'input (InputStream) parameter must not be null'
        assert options != null, 'options Map parameter must not be null'

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
}
