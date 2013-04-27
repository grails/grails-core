package org.codehaus.groovy.grails.web.pages

import grails.test.mixin.TestMixin
import grails.test.mixin.web.ControllerUnitTestMixin

@TestMixin(ControllerUnitTestMixin)
class GSPResponseWriterSpec extends spock.lang.Specification {
    GSPResponseWriter writer
    
    def setupSpec() {
        enableContentLength = true
    }
    
    def setup() {
        writer = GSPResponseWriter.getInstance(response)
    }
    
    def cleanupSpec() {
        enableContentLength = false
    }
    
    void setEnableContentLength(boolean value) {
        System.setProperty("GSPResponseWriter.enableContentLength",String.valueOf(value));
    }
    
    def "GSPResponseWriter should support Content-Length calculation"() {
        when:
            writer.print("Hello")
            writer.close()
        then:
            response.getContentLength() == 5
            response.contentAsString == 'Hello'
    }
    
    def "GSPResponseWriter should support Content-Length calculation for UTF"() {
        when:
            response.setCharacterEncoding("UTF-8")
            writer.print("Hellå")
            writer.close()
        then:
            response.getContentLength() == 6
            response.contentAsString == 'Hellå'
    }
    
    def "GSPResponseWriter should support Content-Length calculation for ISO-8859-1"() {
        when:
            response.setCharacterEncoding("ISO-8859-1")
            writer.print("Hellå")
            writer.close()
        then:
            response.getContentLength() == 5
            response.contentAsString == 'Hellå'
    }
    
    void "GSPResponseWriter should support filtering encoding that forces safe output"() {
        when:
            webRequest.filteringCodec = 'html'
            response.setCharacterEncoding("UTF-8")
            writer.print("<script>alert('hello')</script>")
            writer.flush();
            writer.close()
        then:
            response.getContentLength() == 51
            response.contentAsString == '&lt;script&gt;alert(&#39;hello&#39;)&lt;/script&gt;'
            
    }

}
