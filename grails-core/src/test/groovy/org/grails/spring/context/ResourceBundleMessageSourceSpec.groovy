package org.grails.spring.context

import org.grails.spring.context.support.ReloadableResourceBundleMessageSource
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.core.io.Resource
import spock.lang.Specification

class ResourceBundleMessageSourceSpec extends Specification {
    Resource messages
    Resource other 
    void setup(){
        messages = new TestResource('messages.properties','''\
            foo=bar
        '''.stripIndent().getBytes('UTF-8'))
         
        other = new TestResource('other.properties','''\
            bar=foo
        '''.stripIndent().getBytes('UTF-8'))
    }
    
    void 'Check method to retrieve bundle codes per messagebundle'(){
        given:
            def messageSource = new ReloadableResourceBundleMessageSource(
                resourceLoader: new DefaultResourceLoader(){
                    Resource getResourceByPath(String path){
                        path.startsWith('messages') ? messages:other
                    }
                }
            )
            messageSource.setBasenames('messages','other')
            def locale = Locale.default
        expect:
            messageSource.getBundleCodes(locale,'messages') == (['foo'] as Set)
            messageSource.getBundleCodes(locale,'other') == (['bar'] as Set)
            messageSource.getBundleCodes(locale,'messages','other') == (['foo','bar'] as Set)
    }
    
    class TestResource extends ByteArrayResource{
        String filename

        TestResource(String filename, byte[] byteArray) {
            super(byteArray)
            this.filename=filename
        }
        
    }
    
}
