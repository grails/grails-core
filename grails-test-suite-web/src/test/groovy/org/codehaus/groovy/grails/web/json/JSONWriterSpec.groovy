package org.codehaus.groovy.grails.web.json

import spock.lang.Ignore
import spock.lang.Issue
import spock.lang.Specification

class JSONWriterSpec extends Specification {
    
    @Issue('GRAILS-10823')
    @Ignore
    void 'Test rendering a forward slash'() {
        given:
        def writer = new StringWriter()
        def jsonWriter = new JSONWriter(writer)
        
        when:
        jsonWriter.object().key('namespace').value('alpha/beta').endObject()
        
        then:
        '{"namespace":"alpha/beta"}' == writer.toString()
    }

}
