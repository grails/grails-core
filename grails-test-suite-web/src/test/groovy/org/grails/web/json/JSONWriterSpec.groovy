package org.grails.web.json

import groovy.transform.CompileStatic
import spock.lang.Issue
import spock.lang.Specification

class JSONWriterSpec extends Specification {
    
    @Issue('GRAILS-10823')
    void 'Test rendering a forward slash'() {
        given:
        def writer = new StringWriter()
        def jsonWriter = new JSONWriter(writer)
        
        when:
        jsonWriter.object().key('namespace').value('alpha/beta').endObject()
        
        then:
        '{"namespace":"alpha/beta"}' == writer.toString()
    }

    void 'should handle nulls'() {
        given:
        def writer = new StringWriter()
        def jsonWriter = new JSONWriter(writer)
        when:
        jsonWriter.array()
        writeNumber(jsonWriter, null)
        writeObject(jsonWriter, null)
        jsonWriter.endArray()
        then:
        '[{"key":null},{"key":null}]' == writer.toString()
    }

    @CompileStatic
    private writeNumber(JSONWriter jsonWriter, Number n) {
        jsonWriter.object().key('key').value(n).endObject()
    }

    @CompileStatic
    private writeObject(JSONWriter jsonWriter, Object o) {
        jsonWriter.object().key('key').value(o).endObject()
    }
}
