package org.codehaus.groovy.grails.web.binding.bindingsource.json

import org.codehaus.groovy.grails.web.binding.bindingsource.JsonDataBindingSourceHelper
import org.grails.databinding.DataBindingSource

import spock.lang.Specification

class JsonDataBindingSourceHelperSpec extends Specification {
    
    void 'Test JSON parsing'() {
        given:
        def json = '''{
  "category": {"name":"laptop", "shouldBeNull": null, "shouldBeTrue": true, "shouldBeFalse": false, "someNumber": 42},
  "name": "MacBook",
  "languages" : [ {"name": "Groovy", "company": "GoPivotal"}, {"name": "Java", "company": "Oracle"}]
}'''

        def inputStream = new ByteArrayInputStream(json.bytes)      
        def bindingSource = new JsonDataBindingSourceHelper().createBindingSource(inputStream)
        
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
        bindingSource['languages[0]'] instanceof DataBindingSource
        bindingSource['languages[1]'] instanceof DataBindingSource
        bindingSource['languages[0]']['name'] == 'Groovy'
        bindingSource['languages[0]']['company'] == 'GoPivotal'
        bindingSource['languages[1]']['name'] == 'Java'
        bindingSource['languages[1]']['company'] == 'Oracle'
    }
}
