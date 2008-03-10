/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Nov 27, 2007
 */
package org.codehaus.groovy.grails.web.converters

import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests
import org.codehaus.groovy.grails.commons.ConfigurationHolder

class AutoParamsXmlMarshallingTests extends AbstractGrailsControllerTests {

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
class TestController {
    def create = {
        [book:new Book(params['book'])]
    }
}
class Book {
    Long id
    Long version
    String title
    Date releaseDate
    Author author
    static belongsTo = Author
}
class Author {
    Long id
    Long version
    String name

    // mocked get method
    static get(Object id) {
        new Author(id:id.toLong())
    }

}
'''
    }

    public void tearDown() {
        super.tearDown();
        ConfigurationHolder.setConfig null
    }



    void testXmlMarshallingIntoParamsObject() {
        def controller = ga.getControllerClass("TestController").newInstance()

        controller.request.contentType = "text/xml"
        controller.request.content = '''<?xml version="1.0" encoding="ISO-8859-1"?>
<book id="1">
  <author id="1">
     <name>Stephen King</name>
  </author>
  <releaseDate>2007-11-27 11:52:53.447</releaseDate>
  <title>The Stand</title>
</book>
'''.bytes

        def model = controller.create()

        assert model

        assert model.book
        assertEquals "The Stand", model.book.title
        assertEquals 1, model.book.author.id
        assertEquals 'Stephen King', model.book.author.name
        
    }


}
