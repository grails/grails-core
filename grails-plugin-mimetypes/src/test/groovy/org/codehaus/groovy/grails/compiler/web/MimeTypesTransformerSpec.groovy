package org.codehaus.groovy.grails.compiler.web

import grails.util.GrailsWebUtil
import javax.servlet.http.HttpServletResponse
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.metaclass.MetaClassEnhancer
import org.codehaus.groovy.grails.compiler.injection.ClassInjector
import org.codehaus.groovy.grails.compiler.injection.GrailsAwareClassLoader
import org.codehaus.groovy.grails.plugins.web.api.ResponseMimeTypesApi
import org.codehaus.groovy.grails.plugins.web.mimes.MimeTypesFactoryBean
import org.springframework.web.context.request.RequestContextHolder
import spock.lang.Specification

class MimeTypesTransformerSpec extends Specification {

    void setup() {
        MetaClassEnhancer responseEnhancer = new MetaClassEnhancer()
        responseEnhancer.addApi responseMimeTypesAPi
        responseEnhancer.enhance HttpServletResponse.metaClass
        GrailsWebUtil.bindMockWebRequest()
    }

    void cleanup() {
        GroovySystem.metaClassRegistry.removeMetaClass(HttpServletResponse)
        RequestContextHolder.setRequestAttributes(null)
    }

    void "Test withFormat method injected at compile time"() {
        given:
            def gcl = new GrailsAwareClassLoader()
            def transformer = new MimeTypesTransformer() {
                @Override
                boolean shouldInject(URL url) { true }
            }
            gcl.classInjectors = [transformer] as ClassInjector[]

        when:
            def cls = getControllerClass(gcl)
            def controller = cls.newInstance()
            def format = controller.index()

        then:
            format == "html"
    }

    private Class getControllerClass(GrailsAwareClassLoader gcl) {
        return gcl.parseClass('''
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
    def getAt(String name) {}
}

''')
    }

    private ResponseMimeTypesApi getResponseMimeTypesAPi() {
        final application = new DefaultGrailsApplication()
        application.config = config
        def mimeTypesFactory = new MimeTypesFactoryBean()
        mimeTypesFactory.grailsApplication = application
        mimeTypesFactory.afterPropertiesSet()

        return new ResponseMimeTypesApi(application, mimeTypesFactory.getObject())
    }

    private getConfig() {
        def s = new ConfigSlurper()

        s.parse '''
grails.mime.file.extensions = true // enables the parsing of file extensions from URLs into the request format
grails.mime.use.accept.header = true
grails.mime.types = [ html: ['text/html','application/xhtml+xml'],
                      xml: ['text/xml', 'application/xml'],
                      text: 'text/plain',
                      js: 'text/javascript',
                      rss: 'application/rss+xml',
                      atom: 'application/atom+xml',
                      css: 'text/css',
                      csv: 'text/csv',
                      all: '*/*',
                      json: ['application/json','text/json'],
                      form: 'application/x-www-form-urlencoded',
                      multipartForm: 'multipart/form-data'
                    ]
'''
    }
}
