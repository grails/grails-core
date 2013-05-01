package org.codehaus.groovy.grails.web.binding

import org.codehaus.groovy.grails.plugins.testing.GrailsMockHttpServletRequest
import org.codehaus.groovy.grails.plugins.testing.GrailsMockMultipartFile
import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests

 /**
 * Tests Grails data binding capabilities.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class DataBindingTests extends AbstractGrailsControllerTests {

    protected void onSetUp() {
        gcl.parseClass('''
package databindingtests

import grails.persistence.*

class TestController {
    def index = {}
}

@Entity
class Book {
    static instanceCount = 0
    Book() {
        instanceCount++
    }
    String title
    Author author
    URL site
}

@Entity
class BookReview {
    Book book
}

@Entity
class MyBean {
  Integer someIntProperty
  Integer someOtherIntProperty
  Integer thirdIntProperty
  Float someFloatProperty
  static constraints = {
    someIntProperty(min:1, nullable:true)
    someOtherIntProperty(max:99)
    thirdIntProperty nullable:false
  }
}
@Entity
class Author {
    String name
    String hairColour
    City placeOfBirth

    static constraints = {
        name(nullable:true)
    }
}
@Entity
class City {
    String name
}
@Entity
class Person {
    String name
    Date birthDate
    static constraints = {
        birthDate nullable: true
    }
}
@Entity
class Pet {
    String name
    Map detailMap
    Person owner
}

@Entity
class WithEncoding {

    EmbedDate eDate

    static constraints = {
    }

    static embedded = ['eDate']
}
class EmbedDate {

    Date aDate
    byte[] aFile

    static constraints = {
    }
}
@Entity
class AuthorCommand {
    List beans = []
    public AuthorCommand() {
        beans << new AuthorBean()
    }
}
@Entity
class AuthorBean {
    Integer[] integers
}
        ''')
    }

    void testBindingASinglePropertyWithSubscriptOperator() {
        def personClass = ga.getDomainClass('databindingtests.Person')
        def person = personClass.newInstance()

        person.properties['birthDate'] = '2013-04-15 21:26:31.973'
        
        assert person.birthDate instanceof Date   
        def cal = Calendar.instance
        cal.time = person.birthDate
        assert Calendar.APRIL == cal.get(Calendar.MONTH)
        assert 2013 == cal.get(Calendar.YEAR)     
    }

    void testBindintToNestedArray() {
        def authorCommandClass = ga.getDomainClass('databindingtests.AuthorCommand')
        def author = authorCommandClass.newInstance()
        def req = new GrailsMockHttpServletRequest()
        req.addParameter 'beans[0].integers[0]', '42'
        
        author.properties = req
        
        assert author.beans.size() == 1
        assert author.beans[0].integers.length == 1
        assert author.beans[0].integers[0] == 42
    }

    void testFieldErrorObjectName() {
        def myBeanClass = ga.getDomainClass('databindingtests.MyBean')
        def myBean = myBeanClass.newInstance()
        
        def req = new GrailsMockHttpServletRequest()
        req.addParameter 'someIntProperty', 'bad integer'
        myBean.properties = req
        def errors = myBean.errors
        def fieldError = errors.getFieldError('someIntProperty')

        assert myBean.someIntProperty == null
        assert fieldError.rejectedValue == 'bad integer'
        assert fieldError.objectName == 'databindingtests.MyBean'
    }
    
    void testBindingMalformedNumber() {
        // GRAILS-6766
        def myBeanClass = ga.getDomainClass('databindingtests.MyBean')
        def myBean = myBeanClass.newInstance()
        
        def req = new GrailsMockHttpServletRequest()
        req.addParameter 'someFloatProperty', '21.12Rush'
        myBean.properties = req
        
        def errors = myBean.errors
        def fieldError = errors.getFieldError('someFloatProperty')

        // these fail with GrailsDataBinder and pass with GormAwareDataBinder
        assert myBean.someFloatProperty == null
        assert fieldError.rejectedValue == '21.12Rush'
    }
    
    void testBinderDoesNotCreateExtraneousInstances() {
        // GRAILS-9914
        def bookReviewClass = ga.getDomainClass('databindingtests.BookReview')
        def bookClass = ga.getDomainClass('databindingtests.Book')
        def book = bookClass.clazz.newInstance(title: 'Some Book')
        
        assert 1 == bookClass.clazz.instanceCount
        
        def bookReview = bookReviewClass.clazz.newInstance(book: book)
        
        assert 'Some Book' == bookReview.book.title
        
        def req = new GrailsMockHttpServletRequest()
        req.addParameter 'book.title', 'Some New Book'
        bookReview.properties = req
        
        assert 'Some New Book' == bookReview.book.title
        
        // this fails with GrailsDataBinder and passes with GormAwareDataBinder
        assert 1 == bookClass.clazz.instanceCount
    }

    void testBindEmbeddedWithMultipartFileAndDate() {
        def withEncodingClass = ga.getDomainClass("databindingtests.WithEncoding")

        def e = withEncodingClass.newInstance()
        def multipartRequest = new GrailsMockHttpServletRequest()
        multipartRequest.addFile(new GrailsMockMultipartFile("eDate.aFile", "foo".bytes))
        multipartRequest.addParameter("eDate.aDate", "struct")
        multipartRequest.addParameter("eDate.aDate_year", "1980")
        multipartRequest.addParameter("eDate.aDate_month", "02")
        multipartRequest.addParameter("eDate.aDate_day", "03")

        e.properties = multipartRequest

        assert e.eDate.aFile != null
        assert e.eDate.aDate != null

    }
    void testBindingMapValue() {
        def petClass = ga.getDomainClass('databindingtests.Pet')
        def pet = petClass.newInstance()
        pet.properties = [name: 'lemur', detailMap: [first: 'one', second: 'two'], owner: [name: 'Jeff'], foo: 'bar', bar: [a: 'a', b: 'b']]

        assert pet.name == 'lemur'
        assert pet.detailMap.first == 'one'
        assert pet.detailMap.second == 'two'
        assert !pet.hasErrors()
    }

    void testBindingNullToANullableDateThatAlreadyHasAValue() {
        def c = ga.getControllerClass('databindingtests.TestController').newInstance()
        def person = ga.getDomainClass('databindingtests.Person').newInstance()

        def params = c.params
        params.name = 'Douglas Adams'
        params.birthDate_year = '1952'
        params.birthDate_day = '11'
        params.birthDate_month = '3'
        params.birthDate = 'struct'

        person.properties = params
        assertEquals 'Douglas Adams', person.name
        assertNotNull person.birthDate

        params.name = 'Douglas Adams'
        params.birthDate_year = ''
        params.birthDate_day = ''
        params.birthDate_month = ''
        params.birthDate = 'struct'

        person.properties = params
        assertEquals 'Douglas Adams', person.name
        assertNull person.birthDate
    }

    void testNamedBinding() {
        def c = ga.getControllerClass('databindingtests.TestController').newInstance()
        def author = ga.getDomainClass('databindingtests.Author').newInstance()

        def params = c.params
        params.name = 'Douglas Adams'
        params.hairColour = 'Grey'

        author.properties['name'] = params
        assertEquals 'Douglas Adams', author.name
        assertNull author.hairColour
    }

    void testNamedBindingWithMultipleProperties() {
        def c = ga.getControllerClass('databindingtests.TestController').newInstance()
        def author = ga.getDomainClass('databindingtests.Author').newInstance()

        def params = c.params
        params.name = 'Douglas Adams'
        params.hairColour = 'Grey'

        author.properties['name', 'hairColour'] = params
        assertEquals 'Douglas Adams', author.name
        assertEquals 'Grey', author.hairColour
    }

    void testThreeLevelDataBinding() {
        def c = ga.getControllerClass("databindingtests.TestController").newInstance()
        def b = ga.getDomainClass("databindingtests.Book").newInstance()

        def params = c.params
        params.title = "The Stand"
        params.author = [placeOfBirth: [name: 'Maine'], name: 'Stephen King']

        b.properties = params

        assertEquals "The Stand",b.title
        assertEquals "Maine", b.author.placeOfBirth.name
        assertEquals "Stephen King", b.author.name
    }

    void testBindBlankToNullWhenNullable() {
        def c = ga.getControllerClass("databindingtests.TestController").newInstance()
        def a = ga.getDomainClass("databindingtests.Author").newInstance()

        def params = c.params
        params.name =  ''
        params.hairColour = ''

        a.properties = params

        assertNull a.name
        assertEquals '', a.hairColour
    }

    void testTypeConversionErrorsWithNestedAssociations() {
        def c = ga.getControllerClass("databindingtests.TestController").newInstance()

        request.addParameter("author.name", "Stephen King")
        request.addParameter("author.hairColour", "Black")

        def params = c.params

        def b = ga.getDomainClass("databindingtests.Book").newInstance()

        b.properties = params

        def a = b.author

        assert !a.hasErrors()
        assert !b.hasErrors()
    }

    void testTypeConversionErrors() {
        def c = ga.getControllerClass("databindingtests.TestController").newInstance()

        request.addParameter("site", "not_a_valid_URL")

        def params = c.params

        def b = ga.getDomainClass("databindingtests.Book").newInstance()

        b.properties = params

        assert b.hasErrors()

        def error = b.errors.getFieldError('site')
    }

    void testValidationAfterBindingFails() {
        def c = ga.getControllerClass("databindingtests.TestController").newInstance()

        // binding should fail for this one
        request.addParameter("someIntProperty", "foo")

        // validation should fail for this one...
        request.addParameter("someOtherIntProperty", "999")

        // binding should fail for this one...
        request.addParameter("thirdIntProperty", "bar")
        
        request.addParameter("someFloatProperty", "21.12")

        def params = c.params

        def myBean = ga.getDomainClass("databindingtests.MyBean").newInstance()

        myBean.properties = params

        assertEquals "wrong number of errors before validation", 2, myBean.errors.errorCount
        assertFalse 'validation should have failed', myBean.validate()
        assertEquals 'wrong number of errors after validation', 4, myBean.errors.errorCount
    }

    void testAssociationAutoCreation() {
        def c = ga.getControllerClass("databindingtests.TestController").newInstance()

        request.addParameter("title", "The Stand")
        request.addParameter("author.name", "Stephen King")

        def params = c.params

        assertEquals "The Stand", params.title

        def b = ga.getDomainClass("databindingtests.Book").newInstance()

        b.properties = params
        assertEquals "The Stand", b.title
        assertEquals "Stephen King", b.author?.name
    }

    void testNullAssociations() {
        def c = ga.getControllerClass("databindingtests.TestController").newInstance()

        request.addParameter("title", "The Stand")
        request.addParameter("author.id", "null")

        def params = c.params
        def b = ga.getDomainClass("databindingtests.Book").newInstance()

        b.properties = params
        assertEquals "Wrong 'title' property", "The Stand", b.title
        assertNull "Expected null for property 'author'", b.author
    }

    void testAssociationsBinding() {
        def c = ga.getControllerClass("databindingtests.TestController").newInstance()

        def authorClass = ga.getDomainClass("databindingtests.Author").getClazz()

        authorClass.metaClass.static.get = { Serializable id ->
            def result = authorClass.newInstance()
            result.id = id as long
            result.name = "Mocked ${id}"
            result
        }

        request.addParameter("title", "The Stand")
        request.addParameter("author.id", "5")

        def params = c.params

        def b = ga.getDomainClass("databindingtests.Book").newInstance()

        b.properties = params

        assertEquals "Wrong 'title' property", "The Stand", b.title
        assertNotNull "Association 'author' should be bound", b.author
        assertEquals 5, b.author.id
        assertEquals "Mocked 5", b.author.name
    }

    void testMultiDBinding() {
        def c = ga.getControllerClass("databindingtests.TestController").newInstance()

        request.addParameter("author.name", "Stephen King")
        request.addParameter("author.hairColour", "Black")
        request.addParameter("title", "The Stand")
        def params = c.params

        def a = ga.getDomainClass("databindingtests.Author").newInstance()

        assertEquals "Stephen King",params['author'].name
        a.properties = params['author']
        assertEquals "Stephen King", a.name
        assertEquals "Black", a.hairColour

        def b = ga.getDomainClass("databindingtests.Book").newInstance()
        b.properties = params
        assertEquals "The Stand", b.title
        assertEquals "Stephen King", b.author?.name
    }
}
