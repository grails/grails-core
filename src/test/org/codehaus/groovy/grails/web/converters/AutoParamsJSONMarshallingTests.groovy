/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Nov 27, 2007
 */
package org.codehaus.groovy.grails.web.converters

import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import groovy.util.ConfigSlurper
import org.codehaus.groovy.grails.web.mime.MimeType

class AutoParamsJSONMarshallingTests extends AbstractGrailsControllerTests {      

    public void onSetUp() {
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

        gcl.parseClass '''

import grails.persistence.*

class TestController {
    def create = {
        [book:new AutoParamsJSONMarshallingBook(params['book'])]
    }
}

@Entity
class AutoParamsJSONMarshallingBook {
    String title
    Date releaseDate
    static belongsTo = [author:AutoParamsJSONMarshallingAuthor]
}

@Entity
class AutoParamsJSONMarshallingAuthor {
    String name

    // mocked get method
    static get(Object id) {
        new AutoParamsJSONMarshallingAuthor(id:id.toLong())
    }

}
'''
    }

    public void tearDown() {
        super.tearDown();
        ConfigurationHolder.setConfig null
        MimeType.reset()
    }



    void testJSONMarshallingIntoParamsObject() {
        def controller = ga.getControllerClass("TestController").newInstance()

        controller.request.contentType = "application/json"
        controller.request.content = '{"id":1,"class":"Book","author":{"id":1,"class":"Author","name":"Stephen King"},"releaseDate":new Date(1196179518015),"title":"The Stand"}'.bytes

		webRequest.informParameterCreationListeners()
        def model = controller.create()
        
        assert model
        assert model.book
        assertEquals "The Stand", model.book.title
        assertEquals 1, model.book.author.id
        assertEquals 'Stephen King', model.book.author.name

        // "id" should not bind because we are binding to a domain class.
        assertNull model.book.id
    }


}
