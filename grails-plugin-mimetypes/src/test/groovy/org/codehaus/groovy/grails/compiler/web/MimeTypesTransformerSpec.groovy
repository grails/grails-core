package org.codehaus.groovy.grails.compiler.web

import org.codehaus.groovy.grails.compiler.injection.ClassInjector
import org.codehaus.groovy.grails.compiler.injection.GrailsAwareClassLoader
import spock.lang.Specification

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 25/03/2011
 * Time: 15:26
 * To change this template use File | Settings | File Templates.
 */
class MimeTypesTransformerSpec extends Specification{


    void "Test withFormat method injected at compile time"() {
        given:
            def gcl = new GrailsAwareClassLoader()
            def transformer = new MimeTypesTransformer() {
                @Override
                boolean shouldInject(URL url) {
                    return true;
                }

            }
            gcl.classInjectors = [transformer] as ClassInjector[]

        when:
            def cls = gcl.parseClass('''
import org.codehaus.groovy.grails.web.mime.*

class MimeTypesCompiledController {
    def request = new MyMockRequest()
    def index() {
        withFormat {
            html { "html" }
            xml { "xml" }
        }
    }
}
class MyMockRequest extends org.springframework.mock.web.MockHttpServletRequest {
    String getFormat() { "html" }

    void putAt(String name, val) {}
    def getAt(String name){}
}

''')
            def controller = cls.newInstance()
            def format = controller.index()


        then:
            format == "html"
    }
}
