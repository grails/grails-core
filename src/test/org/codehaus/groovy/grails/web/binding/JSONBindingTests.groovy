package org.codehaus.groovy.grails.web.binding

import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.codehaus.groovy.grails.web.mime.MimeType

/**
 * @author Graeme Rocher
 * @since 1.1
 */

public class JSONBindingTests extends AbstractGrailsControllerTests{

    protected void onSetUp() {
        def config = new ConfigSlurper().parse( """
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
        """)

        ConfigurationHolder.setConfig config

        gcl.parseClass('''
import grails.persistence.*

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

    def simple = {
    }
}
''')
    }


    public void tearDown() {
        super.tearDown();
        ConfigurationHolder.setConfig null
        MimeType.reset()
    }

    void testSimpleJSONBinding() {
        def controller = ga.getControllerClass("SiteController").newInstance()

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
        def controller = ga.getControllerClass("SiteController").newInstance()

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

        println site.errors
        
        assertFalse site.hasErrors()

        assertEquals "asite", site.description
        assertEquals "blah", site.mode.code
        assertEquals "blah2", site.unwanted1
    }


}