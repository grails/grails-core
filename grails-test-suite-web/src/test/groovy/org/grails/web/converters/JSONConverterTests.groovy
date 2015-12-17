package org.grails.web.converters

import static org.junit.Assert.assertEquals
import grails.artefact.Artefact
import grails.converters.JSON
import grails.persistence.Entity
import grails.test.mixin.Mock
import grails.test.mixin.TestFor

import org.grails.web.servlet.mvc.HibernateProxy
import org.grails.web.servlet.mvc.LazyInitializer
import org.grails.buffer.StreamCharBuffer
import org.junit.Test

/**
 * Tests for the JSON converter.
 *
 * @author Graeme Rocher
 * @since 0.4
 */
@TestFor(JSONConverterController)
@Mock(Book)
class JSONConverterTests {

    @Test
    void testNullJSONValues() {
        controller.testNullValues()

        assertEquals('{}', response.contentAsString)
    }

    @Test
    void testJSONConverter() {
        controller.test()

        // @todo this test is fragile and depends on runtime environment because
        // of hash key ordering variations
        assertEquals("""{"author":"Stephen King","title":"The Stand"}""".toString(), response.contentAsString)
    }

    @Test
    void testConvertErrors() {
        controller.testErrors()

        // @todo this test is fragile and depends on runtime environment because
        // of hash key ordering variations
        def json = JSON.parse(response.contentAsString)
        def titleError = json.errors.find { it.field == 'title' }

        assertEquals "Property [title] of class [class ${Book.name}] cannot be null".toString(), titleError.message
        def authorError = json.errors.find { it.field == 'author' }
        assertEquals "Property [author] of class [class ${Book.name}] cannot be null".toString(), authorError.message
    }

    @Test
    void testProxiedDomainClassWithJSONConverter() {

        def obj = new Book()
        obj.title = "The Stand"
        obj.author = "Stephen King"

        def hibernateInitializer = [getImplementation:{obj}] as LazyInitializer
        def proxy = [getHibernateLazyInitializer:{hibernateInitializer}] as HibernateProxy
        params.b = proxy

        controller.testProxy()

        // @todo this test is fragile and depends on runtime environment because
        // of hash key ordering variations
        assertEquals("""{"author":"Stephen King","title":"The Stand"}""".toString(), response.contentAsString)
    }

    @Test
    void testJSONEnumConverting() {
        def enumInstance = Role.HEAD
        params.e = enumInstance
        controller.testEnum()

        // @todo this test is fragile and depends on runtime environment because
        // of hash key ordering variations
        assertEquals('{"enumType":"org.grails.web.converters.Role","name":"HEAD"}', response.contentAsString)
    }

    // GRAILS-11513
    @Test
    void testStringsWithQuotes() {
        def json = [quotedString: "I contain a \"Quote\"!", nonquotedString: "I don't!"] as JSON
        assertEquals('{"quotedString":"I contain a \\"Quote\\"!","nonquotedString":"I don\'t!"}', json.toString())
    }

    @Test
    void testGStringsWithQuotes() {
        def json = [quotedString: "I contain a \"${'Quote'}\"!", nonquotedString: "I ${'don'}'t!"] as JSON
        assertEquals('{"quotedString":"I contain a \\"Quote\\"!","nonquotedString":"I don\'t!"}', json.toString())
    }

    @Test
    void testStreamCharBufferWithQuotes() {
        def quotedBuffer = new StreamCharBuffer()
        quotedBuffer.writer << "I contain a \"Quote\"!"
        def nonquotedBuffer = new StreamCharBuffer()
        nonquotedBuffer.writer << "I don't!"
        def json = [quotedString: quotedBuffer, nonquotedString: nonquotedBuffer] as JSON
        assertEquals('{"quotedString":"I contain a \\"Quote\\"!","nonquotedString":"I don\'t!"}', json.toString())
    }

    @Test
    void testObjectWithQuotes() {
        def json = [quotedString: new CustomCharSequence("I contain a \"Quote\"!"), nonquotedString: new CustomCharSequence("I don't!")] as JSON
        assertEquals('{"quotedString":"I contain a \\"Quote\\"!","nonquotedString":"I don\'t!"}', json.toString())
    }

    // GRAILS-11515
    @Test
    void testJsonMultilineSerialization() {
        String multiLine = "first line \n second line"
        def object = [ line: multiLine ]
        def result = object as JSON

        assertEquals('{"line":"first line \\n second line"}', result.toString())
    }

    // GRAILS-11530
    @Test
    void testMoreStringsWithQuotes() {
        def str = 'Hi, this is my "test"'
        def json = new grails.converters.JSON([a:str])
        assertEquals('{"a":"Hi, this is my \\"test\\""}', json.toString())

    }

    // GRAILS-11517
    @Test
    void testMoreStringsWithQuotes2() {
        assertEquals('{"key":"<a href=\\"#\\" class=\\"link\\">link<\\u002fa>"}',(['key': '<a href="#" class="link">link</a>'] as JSON).toString())
    }
}
enum Role { HEAD, DISPATCHER, ADMIN }

@Artefact("Controller")
class JSONConverterController {
    def test = {
       def b = new Book(title:'The Stand', author:'Stephen King')
       render b as JSON
    }

    def testProxy = {
       render params.b as JSON
    }

    def testErrors = {
        def b = new Book()
        b.validate()
        render b.errors as JSON
    }

   def testEnum = {
       render params.e as JSON
   }

    def testNullValues = {
        def descriptors = [:]
        descriptors.put(null,null)
        render descriptors as JSON
    }
}

@Entity
class Book {
   Long id
   Long version
   String title
   String author
}

class CustomCharSequence implements CharSequence {
    String source

    CustomCharSequence(String source) {
        this.source = source
    }

    @Override
    public int length() {
        source.length()
    }

    @Override
    public char charAt(int index) {
        source.charAt(index)
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        source.subSequence(start, end)
    }

    @Override
    public String toString() {
        source
    }
}
