package org.codehaus.groovy.grails.web.taglib;

import groovy.lang.Closure;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.XPath;
import org.dom4j.xpath.DefaultXPath;

import org.springframework.web.servlet.support.RequestContextUtils as RCU;

/**
 * Tests for the FormTagLib.groovy file which contains tags to help with the
 * creation of HTML forms
 *
 * @author Graeme
 *
 */
public class FormTagLib3Tests extends AbstractGrailsTagTests {

    /** The name used for the datePicker tags created in the test cases. */
    private static final String DATE_PICKER_TAG_NAME = "testDatePicker";
    private static final def SELECT_TAG_NAME = "testSelect";

    private static final Collection DATE_PRECISIONS_INCLUDING_MINUTE = Collections.unmodifiableCollection(Arrays.asList( ["minute", null] as String[] ))
    private static final Collection DATE_PRECISIONS_INCLUDING_HOUR = Collections.unmodifiableCollection(Arrays.asList(["hour", "minute",null] as String[] ))
    private static final Collection DATE_PRECISIONS_INCLUDING_DAY = Collections.unmodifiableCollection(Arrays.asList(["day", "hour", "minute", null] as String[] ))
    private static final Collection DATE_PRECISIONS_INCLUDING_MONTH = Collections.unmodifiableCollection(Arrays.asList(["month", "day", "hour", "minute", null] as String[] ))

    
    public void testHiddenFieldTag() {
    	final StringWriter sw = new StringWriter();
    	final PrintWriter pw = new PrintWriter(sw);

		withTag("hiddenField", pw) { tag ->
	    	// use sorted map to be able to predict the order in which tag attributes are generated
    		def attributes = new TreeMap([name: "testField", value: "1"])
    		tag.call(attributes)
	
    		assertEquals '<input type="hidden" id="testField" name="testField" value="1" />', sw.toString()
		}
    }

    public void testRadioTag() {
    	StringWriter sw = new StringWriter();
    	PrintWriter pw = new PrintWriter(sw);

    	withTag("radio", pw) { tag ->
	    	// use sorted map to be able to predict the order in which tag attributes are generated
    		def attributes = new TreeMap([name: "testRadio", checked: "true", value: "1"])
    		tag.call(attributes)

	    	assertEquals "<input type=\"radio\" name=\"testRadio\" checked=\"checked\" value=\"1\"  />", sw.toString()
    	}

    	sw = new StringWriter();
    	pw = new PrintWriter(sw);

    	withTag("radio", pw) { tag ->
	    	// use sorted map to be able to predict the order in which tag attributes are generated
    		def attributes = new TreeMap([name: "testRadio", value: "2"])
    		tag.call(attributes)

    		assertEquals "<input type=\"radio\" name=\"testRadio\" value=\"2\"  />", sw.toString()
    	}
    }

    public void testRadioGroupTagWithLabels() {
           StringWriter sw = new StringWriter();
           PrintWriter pw = new PrintWriter(sw);
          withTag("radioGroup", pw) { tag ->
               def attributes = new TreeMap([name: "testRadio", labels:['radio.1', 'radio.2', 'radio.3'],
                                            values:[1,2,3], value: "1"])
               tag.call(attributes, {"<p><g:message code=\"${it.label}\" /> ${it.radio}</p>"})
               def lineSep = System.getProperty("line.separator")
               assertEquals ("<p><g:message code=\"radio.1\" /> <input type=\"radio\" name=\"testRadio\" checked=\"checked\" value=\"1\" /></p>"
                + lineSep + "<p><g:message code=\"radio.2\" /> <input type=\"radio\" name=\"testRadio\" value=\"2\" /></p>"
                + lineSep + "<p><g:message code=\"radio.3\" /> <input type=\"radio\" name=\"testRadio\" value=\"3\" /></p>"
                + lineSep , sw.toString())
           }
       }

        public void testRadioGroupTagWithoutLabelsAndInvalidValue() {
           StringWriter sw = new StringWriter();
           PrintWriter pw = new PrintWriter(sw);
           withTag("radioGroup", pw) { tag ->
               def attributes = new TreeMap([name: "testRadio2",
                                            values:[3,2], value: "1"])
               tag.call(attributes, {"<p><g:message code=\"${it.label}\" /> ${it.radio}</p>"})
               def lineSep = System.getProperty("line.separator")
               assertEquals ("<p><g:message code=\"Radio 3\" /> <input type=\"radio\" name=\"testRadio2\" value=\"3\" /></p>"
                + lineSep + "<p><g:message code=\"Radio 2\" /> <input type=\"radio\" name=\"testRadio2\" value=\"2\" /></p>"
                + lineSep , sw.toString())
           }
       }


    public void testSelectTag() {
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

        def sel = 5

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

    public void testSelectTagWithNoSelectionSet() {
    	final StringWriter sw = new StringWriter();
    	final PrintWriter pw = new PrintWriter(sw);

        def range = ['a', 'b', 'c', 'd', 'e']

    	withTag("select", pw) { tag ->
	    	// use sorted map to be able to predict the order in which tag attributes are generated
    		def attributes = new TreeMap([name: SELECT_TAG_NAME, noSelection:['?':'NONE'], from: range ])
    		tag.call(attributes)
    	}


        def doc = DocumentHelper.parseText( sw.toString() )
        assertNotNull( doc)

        assertSelectFieldPresentWithValueAndText( doc, SELECT_TAG_NAME, '?', 'NONE' )
        range.each() {
            assertSelectFieldPresentWithValue( doc, SELECT_TAG_NAME, it.toString() )
        }


    	sw = new StringWriter();
    	pw = new PrintWriter(sw);

    	withTag("select", pw) { tag ->
	    	// use sorted map to be able to predict the order in which tag attributes are generated
    		def attributes = new TreeMap([name: SELECT_TAG_NAME, value: '', noSelection:['':'NONE'], from: range ])
    		tag.call(attributes)
    	}


        doc = DocumentHelper.parseText( sw.toString() )
        assertNotNull( doc)

        assertSelectFieldPresentWithSelectedValue( doc, SELECT_TAG_NAME, '')
        range.each() {
            assertSelectFieldPresentWithValue( doc, SELECT_TAG_NAME, it.toString() )
        }


    }
    
    public void testSelectTagWithValueMessagePrefixSet() {
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

    public void testCheckboxTag() {
    	final StringWriter sw = new StringWriter();
    	final PrintWriter pw = new PrintWriter(sw);

    	withTag("checkBox", pw) { tag ->    	
	    	// use sorted map to be able to predict the order in which tag attributes are generated
    		def attributes = new TreeMap([name: "testCheck", extra: "1", value: "true"])
    		tag.call(attributes)
	
    		assertEquals '<input type="hidden" name="_testCheck" /><input type="checkbox" name="testCheck" checked="checked" value="true" extra="1"  />', sw.toString()
    	}
    }

    void testRenderingNoSelectionOption() {
    	final StringWriter sw = new StringWriter();
    	final PrintWriter pw = new PrintWriter(sw);

        // This isn't really a tag...
    	withTag("renderNoSelectionOption", pw) { tag ->
    	    tag.call( '', '', null)

	        println "SW: "+sw.toString()
	        assertEquals '<option value=""></option>', sw.toString()
        }
    }

    public void testNoHtmlEscapingTextAreaTag() throws Exception {
    	final StringWriter sw = new StringWriter();
    	final PrintWriter pw = new PrintWriter(sw);

    	withTag("textArea", pw) { tag ->
	
	        assertNotNull(tag);
	
	        final Map attrs = new HashMap();
	        attrs.put("name","testField");
	        attrs.put("escapeHtml","false");
	        attrs.put("value", "<b>some text</b>");
	
	        tag.call(attrs);
	
	        final String result = sw.toString();
	        // need to inspect this as raw text so the DocumentHelper doesn't
	        // unescape anything...
	        assertTrue(result.indexOf("<b>some text</b>") >= 0);
	
	        final Document document = DocumentHelper.parseText(sw.toString());
	        assertNotNull(document);
	
	        final Element inputElement = document.getRootElement();
	        final Attribute escapeHtmlAttribute = inputElement.attribute("escapeHtml");
	        assertNull("escapeHtml attribute should not exist", escapeHtmlAttribute);
    	
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

    private void assertSelectFieldNotPresent(Document document, String fieldName) {
        XPath xpath = new DefaultXPath("//select[@name='" + fieldName + "']");
        assertFalse(xpath.booleanValueOf(document));
    }
    

}

