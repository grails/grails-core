/**
 * Tests Grails data binding capabilities
 
 * @author Graeme Rocher
 * @since 1.0
  *
 * Created: Sep 12, 2007
 * Time: 11:50:39 AM
 * 
 */
package org.codehaus.groovy.grails.web.binding

import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests
import java.text.SimpleDateFormat

class DataBindingTests extends AbstractGrailsControllerTests {

    void onSetUp() {
        gcl.parseClass('''
class TestController {
    def index = {}
}
class Book {
    Long id
    Long version
    String title
    Author author
    URL site


}
class MyBean {
  Long id
  Long version
  Integer someIntProperty
  Integer someOtherIntProperty
  static constraints = {
    someIntProperty(min:1, nullable:true)
    someOtherIntProperty(max:99)
  }
}
class Author {
    Long id
    Long version
    String name
    String hairColour
    City placeOfBirth

    static constraints = {
        name(nullable:true)
    }
}
class City {
    Long id
    Long version
    String name
}
        ''')
    }

    void testThreeLevelDataBinding() {
        def c = ga.getControllerClass("TestController").newInstance()
        def b = ga.getDomainClass("Book").newInstance()

        def params = c.params
        params.title = "The Stand"
        params.'author.placeOfBirth.name' = 'Maine'
        params.'author.name' = "Stephen King"

        b.properties = params

        assertEquals "The Stand",b.title
        assertEquals "Maine", b.author.placeOfBirth.name
        assertEquals "Stephen King", b.author.name

    }
    void testBindBlankToNullWhenNullable() {
        def c = ga.getControllerClass("TestController").newInstance()
        def a = ga.getDomainClass("Author").newInstance()

        def params = c.params
        params.name =  ''
        params.hairColour = ''

        a.properties = params

        assertNull a.name
        assertEquals '', a.hairColour

    }

    void testTypeConversionErrorsWithNestedAssociations() {
        def c = ga.getControllerClass("TestController").newInstance()

        request.addParameter("author.name", "Stephen King")
        request.addParameter("author.hairColour", "Black")


        def params = c.params

        def b = ga.getDomainClass("Book").newInstance()

        b.properties = params

        def a = b.author

        assert !a.hasErrors()
        println b.errors
        assert !b.hasErrors()

    }
    void testTypeConversionErrors() {
        def c = ga.getControllerClass("TestController").newInstance()

        request.addParameter("site", "not_a_valid_URL")


        def params = c.params

        def b = ga.getDomainClass("Book").newInstance()

        b.properties = params

        println b.errors
        assert b.hasErrors()

        def error = b.errors.getFieldError('site')


    }

    void testValidationAfterBindingFails() {
        def c = ga.getControllerClass("TestController").newInstance()

        // binding should fail for this one...
        request.addParameter("someIntProperty", "foo")

        // validation should fail for this one...
        request.addParameter("someOtherIntProperty", "999")

        def params = c.params

        def myBean = ga.getDomainClass("MyBean").newInstance()

        myBean.properties = params

        assertEquals "wrong number of errors before validation", 1, myBean.errors.errorCount
        assertFalse 'validation should have failed', myBean.validate()
        assertEquals 'wrong number of errors after validation', 2, myBean.errors.errorCount
    }

    void testAssociationAutoCreation() {
        def c = ga.getControllerClass("TestController").newInstance()

        request.addParameter("title", "The Stand")
        request.addParameter("author.name", "Stephen King")

        def params = c.params

        assertEquals "The Stand", params.title

        def b = ga.getDomainClass("Book").newInstance()

        b.properties = params
        assertEquals "The Stand", b.title
        assertEquals "Stephen King", b.author?.name
    }

    void testNullAssociations() {
        def c = ga.getControllerClass("TestController").newInstance()

        request.addParameter("title", "The Stand")
        request.addParameter("author.id", "null")


        def params = c.params
        def b = ga.getDomainClass("Book").newInstance()

        b.properties = params
        assertEquals "Wrong 'title' property", "The Stand", b.title
        assertNull "Expected null for property 'author'", b.author
    }

    void testAssociationsBinding() {
        def c = ga.getControllerClass("TestController").newInstance()

        def authorClass = ga.getDomainClass("Author").getClazz()
        authorClass.metaClass.'static'.get = { id ->
            def result = authorClass.newInstance()
            result.id = id
            result.name = "Mocked ${id}"
            result
        }

        request.addParameter("title", "The Stand")
        request.addParameter("author.id", "5")

        def params = c.params

        def b = ga.getDomainClass("Book").newInstance()

        b.properties = params


        assertEquals "Wrong 'title' property", "The Stand", b.title
        assertNotNull "Association 'author' should be binded", b.author
        assertEquals 5, b.author.id
        assertEquals "Mocked 5", b.author.name
    }

    void testMultiDBinding() {
        def c = ga.getControllerClass("TestController").newInstance()

        request.addParameter("author.name", "Stephen King")
        request.addParameter("author.hairColour", "Black")
        request.addParameter("title", "The Stand")
        def params = c.params

        def a = ga.getDomainClass("Author").newInstance()

        assertEquals "Stephen King",params['author'].name
        println params['author']
        a.properties = params['author']
        assertEquals "Stephen King", a.name
        assertEquals "Black", a.hairColour


        def b = ga.getDomainClass("Book").newInstance()
        b.properties = params
        assertEquals "The Stand", b.title
        assertEquals "Stephen King", b.author?.name
                               
    }

}