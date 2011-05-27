package org.codehaus.groovy.grails.web.converters

import org.codehaus.groovy.grails.web.mime.MimeType
import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests
import org.springframework.web.context.request.RequestContextHolder
import org.codehaus.groovy.grails.web.converters.configuration.ConvertersConfigurationHolder

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class AutoParamsXmlMarshallingTests extends AbstractGrailsControllerTests {

    protected void onSetUp() {
        ConvertersConfigurationHolder.clear()
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
        """, "Config")

        gcl.parseClass '''
import grails.persistence.*

class TestController {
    def create = {
        [book:new AutoParamsXmlMarshallingBook(params['book'])]
    }
}

@Entity
class AutoParamsXmlMarshallingBook {
    String title
    Date releaseDate

    static belongsTo = [author:AutoParamsXmlMarshallingAuthor]
}

@Entity
class AutoParamsXmlMarshallingAuthor {
    String name

    // mocked get method
    static get(Serializable id) {
        new AutoParamsXmlMarshallingAuthor(id:id.toLong())
    }
}
'''
    }

    protected void tearDown() {
        super.tearDown()
        MimeType.reset()
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
