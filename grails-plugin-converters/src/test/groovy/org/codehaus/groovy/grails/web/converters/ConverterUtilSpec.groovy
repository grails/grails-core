package org.codehaus.groovy.grails.web.converters


import spock.lang.Specification

class ConverterUtilSpec extends Specification {

    void 'Test converting an array to an interface type'() {
        given:
            def converterUtil = new ConverterUtil()
            
        when:
            def someArray = ['One', 'Two', 'One', 'Three'] as String[]
            def someSet = converterUtil.invokeOriginalAsTypeMethod(someArray, Set)
            
        then:
            someSet instanceof Set
    }
}
