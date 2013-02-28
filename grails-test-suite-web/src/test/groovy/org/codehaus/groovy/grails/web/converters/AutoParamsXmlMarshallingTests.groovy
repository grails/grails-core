package org.codehaus.groovy.grails.web.converters

import grails.persistence.Entity

import org.codehaus.groovy.grails.web.converters.configuration.ConvertersConfigurationHolder
import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests
import org.springframework.web.context.request.RequestContextHolder

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class AutoParamsXmlMarshallingTests extends AbstractGrailsControllerTests {

    protected void onSetUp() {
        ConvertersConfigurationHolder.clear()
        RequestContextHolder.setRequestAttributes(null)
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
'''
    }

    @Override
    protected Collection<Class> getControllerClasses() {
        [TestConverterController]
    }

    @Override
    protected Collection<Class> getDomainClasses() {
        [AutoParamsXmlMarshallingAuthor, AutoParamsXmlMarshallingBook]
    }

    void testXmlMarshallingIntoParamsObject() {
        def controller = ga.getControllerClass(TestConverterController.name).newInstance()

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
    void testXmlMarshallingIntoParamsObjectWithBindableId() {
        def controller = ga.getControllerClass(TestConverterController.name).newInstance()

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
        def model = controller.createWithBindableId()

        assert model
        assert model.book
        assertEquals "The Stand", model.book.title
        assertEquals 1, model.book.author.id
        assertEquals 'Stephen King', model.book.author.name
        assertEquals 1, model.book.id
    }
}

class TestConverterController {
    def create = {
        [book:new AutoParamsXmlMarshallingBook(params['book'])]
    }
    def createWithBindableId = {
        [book:new AutoParamsXmlMarshallingBookWithBindableId(params['book'])]
    }
}

@Entity
class AutoParamsXmlMarshallingBook {
    String title
    Date releaseDate

    static belongsTo = [author:AutoParamsXmlMarshallingAuthor]
}

@Entity
class AutoParamsXmlMarshallingBookWithBindableId {
    String title
    Date releaseDate

    static belongsTo = [author:AutoParamsXmlMarshallingAuthor]
    static constraints = {
        id bindable: true
    }
}

@Entity
class AutoParamsXmlMarshallingAuthor {
    String name
    
    static constraints = {
        id bindable: true
    }

    // mocked get method
    static get(Serializable id) {
        new AutoParamsXmlMarshallingAuthor(id:id.toLong())
    }
}
