package org.grails.databinding.converters.web

import spock.lang.Issue
import spock.lang.Specification

class LocaleAwareBigDecimalConverterSpec extends Specification {

    @Issue('https://github.com/grails/grails-core/issues/10554')
    def "While trying to convert from an empty string to BigDecimal we should not raise a NPE but return null instead"() {
        when:
        def converter = new LocaleAwareBigDecimalConverter()
        converter.targetType = BigDecimal
        def result = converter.convert("")

        then:
        noExceptionThrown()
        result == null
    }
}
