package org.codehaus.groovy.grails.web.taglib;

class MessageTagTests extends AbstractGrailsTagTests {

    private StringWriter sw

    void setUp() {
        super.setUp();
        sw = new StringWriter();
    }

    void testMessageTagInTemplate() {
        def template = '<g:message code="test.code" />'
        messageSource.addMessage("test.code", new Locale("en"), "hello world!")
        assertOutputEquals 'hello world!', template
    }

    void testMessageTagWithMissingMessage() {

        withTag("message", sw) { tag ->
            Map attrs = [code:"test.code"]
            def result = tag.call( attrs )
            assertEquals "test.code", result
        }

    }

    void testMessageTagWithMessage() {

        withTag("message", sw) { tag ->
            Map attrs = [code:"test.code"]
            messageSource.addMessage("test.code", new Locale("en"), "hello world!")
            def result = tag.call( attrs )
            assertEquals "hello world!", result
        }

    }

    void testMessageTagWithArguments() {

        withTag("message", sw) { tag ->
            messageSource.addMessage("test.args", new Locale("en"), "hello {0}!")
            def attrs = [code:"test.args", args:["fred"]]
            def result = tag.call(attrs)
            assertEquals "hello fred!", result
        }

    }

    void testMessageTagWithCodec() {

        withTag("message", sw) { tag ->
            def attrs = [code:"test.code", encodeAs:'HTML']
            messageSource.addMessage("test.code", new Locale("en"), ">>&&")
            def result=tag.call( attrs )
            assertEquals "&gt;&gt;&amp;&amp;", result
        }
    }
}
