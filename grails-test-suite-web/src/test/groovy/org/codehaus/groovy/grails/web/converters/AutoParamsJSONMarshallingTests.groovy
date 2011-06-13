package org.codehaus.groovy.grails.web.converters

import java.util.Collection;

import grails.persistence.Entity

import org.codehaus.groovy.grails.web.mime.MimeType
import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests

 /**
 * @author Graeme Rocher
 * @since 1.0
 */
class AutoParamsJSONMarshallingTests extends AbstractGrailsControllerTests {

    protected void onSetUp() {

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
        """, "Config")
    }

    protected void tearDown() {
        super.tearDown()
        MimeType.reset()
    }

    @Override
    protected Collection<Class> getControllerClasses() {
        [TestBookController]
    }
    
    @Override
    protected Collection<Class> getDomainClasses() {
        [AutoParamsJSONMarshallingBook, AutoParamsJSONMarshallingAuthor]
    }
    
    void testJSONMarshallingIntoParamsObject() {
            def controller = ga.getControllerClass(TestBookController.name).newInstance()

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

class TestBookController {
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
    static get(Serializable id) {
        new AutoParamsJSONMarshallingAuthor(id:id.toLong())
    }
}

