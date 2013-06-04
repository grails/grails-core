package org.codehaus.groovy.grails.web.binding.bindingsource.hal.json

import org.codehaus.groovy.grails.web.binding.bindingsource.HalJsonDataBindingSourceCreator

import spock.lang.Specification

class HalJsonDataBindingSourceCreatorSpec extends Specification {

    void 'Test JSON parsing'() {
        given:
        def json = '''
            {
    "name": "Douglas",
    "age": "42",
    "_embedded" : {
        "homeAddress" : { "state": "Missouri", "city": "O'Fallon"},
        "workAddress" : { "state": "California", "city": "San Mateo"}
    }
}
'''
        def inputStream = new ByteArrayInputStream(json.bytes)
        def bindingSource = new HalJsonDataBindingSourceCreator().createBindingSource(inputStream)

        when:
        def propertyNames = bindingSource.propertyNames

        then:
        propertyNames.contains 'age'
        propertyNames.contains 'name'
        propertyNames.contains 'homeAddress'
        propertyNames.contains 'workAddress'
        bindingSource.containsProperty 'name'
        bindingSource.containsProperty 'age'
        bindingSource.containsProperty 'homeAddress'
        bindingSource.containsProperty 'workAddress'

        bindingSource['name'] == 'Douglas'
        bindingSource['age'] == '42'
        bindingSource['homeAddress']['state'] == 'Missouri'
        bindingSource['homeAddress']['city'] == "O'Fallon"
        bindingSource['workAddress']['state'] == 'California'
        bindingSource['workAddress']['city'] == 'San Mateo'
    }
}
