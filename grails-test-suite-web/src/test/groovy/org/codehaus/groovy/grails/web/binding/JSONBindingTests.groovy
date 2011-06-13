package org.codehaus.groovy.grails.web.binding

import java.util.Collection;

import grails.persistence.Entity

import org.codehaus.groovy.grails.web.mime.MimeType
import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests
import org.springframework.web.context.request.RequestContextHolder

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class JSONBindingTests extends AbstractGrailsControllerTests {

    protected void onSetUp() {
        RequestContextHolder.setRequestAttributes(null)
        MimeType.reset()

        gcl.parseClass("""
grails.mime.types = [ html: ['text/html','application/xhtml+xml'],
                      xml: ['text/xml', 'application/xml'],
                      text: 'text/plain',
                      js: 'text/javascript',
                      rss: 'application/rss+xml',
                      atom: 'application/atom+xml',
                      css: 'text/css',
                      cvs: 'text/csv',
                      all: '*/*',
                      json: 'application/json'
                    ]
        """, 'Config')
    }
    
    @Override
    protected Collection<Class> getControllerClasses() {
        [SiteController]
    }
    
    @Override
    protected Collection<Class> getDomainClasses() {
        [Site, SiteMode]
    }

    protected void tearDown() {
        super.tearDown()
        MimeType.reset()
    }

    void testSimpleJSONBinding() {
        def controller = new SiteController()

        controller.request.contentType = "application/json"
        controller.request.content = '''\
 {"foo": { "bar": "baz" } }
'''.bytes

        webRequest.informParameterCreationListeners()

        controller.simple()

        assertEquals "baz", controller.params.foo.bar
        assertEquals "baz", controller.params['foo.bar']
    }

    void testJSONBindingWithAssociation() {
        def controller = new SiteController()

        controller.request.contentType = "application/json"
        controller.request.content = '''\
{"class":"Site",
 "id":1,
 "activated":new Date(1252073561495),
 "description":"asite",
 "mode":{"class":"SiteMode","code":"blah"},
 "unwanted1":"blah2",
 "unwanted2":"blah1"}
'''.bytes

        webRequest.informParameterCreationListeners()

        def site = controller.put()
        assertFalse site.hasErrors()

        assertEquals "asite", site.description
        assertEquals "blah", site.mode.code
        assertEquals "blah2", site.unwanted1
    }
}


@Entity
class Site {
    String description
    Date activated
    SiteMode mode
    String unwanted1 ="blah"
    String unwanted2 = "blah"

    static constraints = {
        description(blank:false)
        activated(nullable:true)
    }
}
@Entity
class SiteMode {
    String code
    String description
}

class SiteController {
    def put = {
        def s = new Site()
        s.properties = params['site']
        return s
    }

    def simple = {}
}
