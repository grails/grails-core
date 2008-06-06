package org.codehaus.groovy.grails.web.taglib

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.XPath;
import org.dom4j.xpath.DefaultXPath;

import org.springframework.web.servlet.support.RequestContextUtils as RCU;

/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Jan 25, 2008
 */
class SelectTagTests extends AbstractGrailsTagTests {
    private static final def SELECT_TAG_NAME = "testSelect";

    void testSelectUsesExpressionForDisable() {
        def template = '<g:set var="flag" value="${true}"/><g:select disabled="${flag}" name="foo" id="foo" from="[1,2,3]" />'
        assertOutputContains('disabled="disabled"', template)
        template = '<g:set var="flag" value="${false}"/><g:select disabled="${flag}" name="foo" id="foo" from="[1,2,3]" />'
        assertOutputContains('<select name="foo" id="foo" >', template)
        template = '<g:select disabled="true" name="foo" id="foo" from="[1,2,3]" />'
        assertOutputContains('disabled="disabled"', template)
        template = '<g:select disabled="false" name="foo" id="foo" from="[1,2,3]" />'
        assertOutputContains('<select name="foo" id="foo" >', template)
    }

    void testSelectWithBigDecimal() {
        def template = '<g:set var="value" value="${2.4}"/><g:select name="foo" from="[1,2,3]" value="${value}" />'
        assertOutputContains('<option value="2" selected="selected" >2</option>', template)
    }

    void testSimpleSelect() {
        def template = '<g:select name="foo" from="[1,2,3]" value="1" />'
        assertOutputContains('<option value="1" selected="selected" >1</option>', template)
        assertOutputContains('<option value="2" >2</option>', template)
        assertOutputContains('<option value="3" >3</option>', template)
    }


    void testMultiSelect() {
        def template = '<g:select name="foo" from="[1,2,3]" value="[2,3]" />'

        assertOutputContains('<select name="foo" id="foo" multiple="multiple" >', template)
        assertOutputContains('<option value="1" >1</option>', template)
        assertOutputContains('<option value="2" selected="selected" >2</option>', template)
        assertOutputContains('<option value="3" selected="selected" >3</option>', template)
    }

    void testSelectWithCustomOptionKeyAndValue() {

        def list = [new SelectTestObject(id:1L, name:"Foo"),new SelectTestObject(id:2L, name:"Bar")]

        def template = '<g:select optionKey="id" optionValue="name" name="foo" from="${objList}" value="2" />'
        assertOutputContains('<option value="2" selected="selected" >Bar</option>', template,[objList:list])
        assertOutputContains('<option value="1" >Foo</option>', template,[objList:list])

    }

    void testSelectWithCustomOptionKeyAndValueAsClosure() {
        def list = [new SelectTestObject(id:1L, name:"Foo"),new SelectTestObject(id:2L, name:"Bar")]

        def template = '<g:select optionKey="id" optionValue="${{it.name?.toUpperCase()}}" name="foo" from="${objList}" value="2" />'

        
        assertOutputContains('<option value="2" selected="selected" >BAR</option>', template,[objList:list])
        assertOutputContains('<option value="1" >FOO</option>', template,[objList:list])

    }

    void testSelectTag() {
    	final StringWriter sw = new StringWriter();
    	final PrintWriter pw = new PrintWriter(sw);

        def range = 1..10

    	withTag("select", pw) { tag ->
	    	// use sorted map to be able to predict the order in which tag attributes are generated
    		def attributes = new TreeMap([name: SELECT_TAG_NAME, from: range ])
    		tag.call(attributes)
    	}

        def doc = DocumentHelper.parseText( sw.toString() )
        assertNotNull( doc)

        range.each() {
            assertSelectFieldPresentWithValue( doc, SELECT_TAG_NAME, it.toString() )
        }

    	sw = new StringWriter();
    	pw = new PrintWriter(sw);

        def sel = '5'

    	withTag("select", pw) { tag ->
	    	// use sorted map to be able to predict the order in which tag attributes are generated
    		def attributes = new TreeMap([name: SELECT_TAG_NAME, value: sel, from: range ])
    		tag.call(attributes)
    	}


        doc = DocumentHelper.parseText( sw.toString() )
        assertNotNull( doc)

        range.each() {
            if (it != sel) {
                assertSelectFieldPresentWithValue( doc, SELECT_TAG_NAME, it.toString() )
            } else {
                assertSelectFieldPresentWithSelectedValue( doc, SELECT_TAG_NAME, it.toString() )
            }
        }


    }

    void testSelectTagWithNoSelectionSet() {
    	final StringWriter sw = new StringWriter();
    	final PrintWriter pw = new PrintWriter(sw);

        def range = ['a', 'b', 'c', 'd', 'e']

    	withTag("select", pw) { tag ->
	    	// use sorted map to be able to predict the order in which tag attributes are generated
    		def attributes = new TreeMap([name: SELECT_TAG_NAME, noSelection:['?':'NONE'], from: range ])
    		tag.call(attributes)
    	}


        println "SELECT = $sw"

        def xml = new XmlSlurper().parseText(sw.toString())

        assertEquals "testSelect", xml.@name?.toString()
        assertEquals "testSelect", xml.@id?.toString()
        assertEquals "NONE", xml.option[0].text()
        assertEquals "?", xml.option[0].@value.toString()

        range.eachWithIndex { e, i ->
            assertEquals e, xml.option[i+1].text()
            assertEquals e, xml.option[i+1].@value.toString()
        }


    	sw = new StringWriter();
    	pw = new PrintWriter(sw);

    	withTag("select", pw) { tag ->
	    	// use sorted map to be able to predict the order in which tag attributes are generated
    		def attributes = new TreeMap([name: SELECT_TAG_NAME, value: '', noSelection:['':'NONE'], from: range ])
    		tag.call(attributes)
    	}


        def doc = DocumentHelper.parseText( sw.toString() )
        assertNotNull( doc)

        assertSelectFieldPresentWithSelectedValue( doc, SELECT_TAG_NAME, '')
        range.each() {
            assertSelectFieldPresentWithValue( doc, SELECT_TAG_NAME, it.toString() )
        }


    }

    void testSelectTagWithValueMessagePrefixSet() {
    	final StringWriter sw = new StringWriter();
    	final PrintWriter pw = new PrintWriter(sw);

        def categoryMap = ['M':'Mystery' , 'T':'Thriller', 'F':'Fantasy']
        def categoryList = categoryMap.keySet()

    	def valueMessagePrefix = "book.category"

		// test without messages set; value will be used as text

    	withTag("select", pw) { tag ->
	    	// use sorted map to be able to predict the order in which tag attributes are generated
    		def attributes = new TreeMap([name: SELECT_TAG_NAME, valueMessagePrefix: valueMessagePrefix, from: categoryList])
    		tag.call(attributes)
    	}

        def doc = DocumentHelper.parseText( sw.toString() )
        assertNotNull( doc )

		// assert select field uses value for both the value as the text (as there is no text found within messages)
        categoryMap.each() { value, text ->
	        assertSelectFieldPresentWithValueAndText( doc, SELECT_TAG_NAME, value, value )
        }


        // test with messages set

        categoryMap.each() { value, text ->
        	messageSource.addMessage(valueMessagePrefix + "." + value, RCU.getLocale(request), text)
        }

        sw = new StringWriter();
    	pw = new PrintWriter(sw);

    	withTag("select", pw) { tag ->
	    	// use sorted map to be able to predict the order in which tag attributes are generated
    		def attributes = new TreeMap([name: SELECT_TAG_NAME, valueMessagePrefix: valueMessagePrefix, from: categoryList])
    		tag.call(attributes)
    	}

        doc = DocumentHelper.parseText( sw.toString() )
        assertNotNull( doc )

		// assert select field uses value and text
        categoryMap.each() { value, text ->
            assertSelectFieldPresentWithValueAndText( doc, SELECT_TAG_NAME, value, text )
        }


    }

    void testMultipleSelect() {
    	final StringWriter sw = new StringWriter();
    	final PrintWriter pw = new PrintWriter(sw);

        def categories = [
                new Expando(code: 'M', label: 'Mystery'),
                new Expando(code: 'T', label: 'Thriller'),
                new Expando(code: 'F', label: 'Fantasy'),
                new Expando(code: 'SF', label: 'Science Fiction'),
                new Expando(code: 'C', label: 'Crime') ]
        def selected = [ 'T', 'C']

        // Execute the tag.
        withTag("select", pw) { tag ->
	    	// use sorted map to be able to predict the order in which tag attributes are generated
    		def attributes = new TreeMap(
                    name: SELECT_TAG_NAME,
                    from: categories,
                    value: selected,
                    optionKey: 'code',
                    optionValue: 'label')
            tag.call(attributes)
    	}

        def doc = DocumentHelper.parseText( sw.toString() )
        assertNotNull( doc )

        // Make sure that the "multiple" attribute is there.
        XPath xpath = new DefaultXPath("//select[@name='" + SELECT_TAG_NAME + "']/@multiple");
        assertEquals("multiple", xpath.valueOf(doc));

        // assert select field uses value for both the value as the text (as there is no text found within messages)
        categories.each() { cat ->
            if (selected.contains(cat)) {
                assertSelectFieldPresentWithSelectedValueAndText( doc, SELECT_TAG_NAME, cat.code, cat.label )
            }
            else {
                assertSelectFieldPresentWithValueAndText( doc, SELECT_TAG_NAME, cat.code, cat.label )
            }
        }
    }

    private void assertSelectFieldPresentWithSelectedValue(Document document, String fieldName, String value) {
        XPath xpath = new DefaultXPath("//select[@name='" + fieldName + "']/option[@selected='selected' and @value='" + value + "']");
        assertTrue(xpath.booleanValueOf(document));
    }

    private void assertSelectFieldPresentWithValue(Document document, String fieldName, String value) {
        XPath xpath = new DefaultXPath("//select[@name='" + fieldName + "']/option[@value='" + value + "']");
        assertTrue(xpath.booleanValueOf(document));
    }

    private void assertSelectFieldPresentWithValueAndText(Document document, String fieldName, String value, String label) {
        XPath xpath = new DefaultXPath("//select[@name='" + fieldName + "']/option[@value='" + value + "' and text()='"+label+"']");
        assertTrue(xpath.booleanValueOf(document));
    }

    private void assertSelectFieldPresentWithSelectedValueAndText(Document document, String fieldName, String value, String label) {
        XPath xpath = new DefaultXPath("//select[@name='" + fieldName + "']/option[@selected='selected' and @value='" + value + "' and text()='"+label+"']");
        assertTrue(xpath.booleanValueOf(document));
    }

    private void assertSelectFieldNotPresent(Document document, String fieldName) {
        XPath xpath = new DefaultXPath("//select[@name='" + fieldName + "']");
        assertFalse(xpath.booleanValueOf(document));
    }
}
class SelectTestObject {
    Long id
    String name
}