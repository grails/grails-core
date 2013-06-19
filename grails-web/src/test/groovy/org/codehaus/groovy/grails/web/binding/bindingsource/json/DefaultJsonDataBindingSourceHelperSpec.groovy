package org.codehaus.groovy.grails.web.binding.bindingsource.json

import org.grails.databinding.DataBindingSource

import spock.lang.Specification

class DefaultJsonDataBindingSourceHelperSpec extends Specification {
    
    void 'Test JSON parsing'() {
        given:
        def json = '''{
  "category": {"name":"laptop", "shouldBeNull": null, "shouldBeTrue": true, "shouldBeFalse": false, "someNumber": 42},
  "name": "MacBook"
}'''
      
        def bindingSource = new JsonDataBindingSourceCreator().createDataBindingSource(json)
        
        when:
        def propertyNames = bindingSource.propertyNames
        
        then:
        propertyNames.contains 'category'
        propertyNames.contains 'name'
        bindingSource.containsProperty 'category'
        bindingSource.containsProperty 'name'
        bindingSource['name'] == 'MacBook'
        bindingSource['category'] instanceof DataBindingSource
        bindingSource['category'].size() == 5
        bindingSource['category']['name'] == 'laptop'
        bindingSource['category']['shouldBeTrue'] == true
        bindingSource['category']['shouldBeFalse'] == false
        bindingSource['category']['someNumber'] == '42'
        bindingSource['category']['shouldBeNull'] == null
    }
}
