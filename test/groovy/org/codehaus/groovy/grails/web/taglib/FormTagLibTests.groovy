package org.codehaus.groovy.grails.web.taglib;

/**
 * Tests for the FormTagLib.groovy file which contains tags to help with the                                         l
 * creation of HTML forms
 *
 * @author Graeme
 *
 */
public class FormTagLibTests extends AbstractGrailsTagTests {

    /** The name used for the datePicker tags created in the test cases. */
    private static final String DATE_PICKER_TAG_NAME = "testDatePicker";
    private static final def SELECT_TAG_NAME = "testSelect";

    private static final Collection DATE_PRECISIONS_INCLUDING_MINUTE = Collections.unmodifiableCollection(Arrays.asList( ["minute", null] as String[] ))
    private static final Collection DATE_PRECISIONS_INCLUDING_HOUR = Collections.unmodifiableCollection(Arrays.asList(["hour", "minute",null] as String[] ))
    private static final Collection DATE_PRECISIONS_INCLUDING_DAY = Collections.unmodifiableCollection(Arrays.asList(["day", "hour", "minute", null] as String[] ))
    private static final Collection DATE_PRECISIONS_INCLUDING_MONTH = Collections.unmodifiableCollection(Arrays.asList(["month", "day", "hour", "minute", null] as String[] ))


    // test for GRAILS-3865
    void testHiddenFieldWithZeroValue() {
        def template = '<g:hiddenField name="index" value="${0}" />'

        assertOutputContains 'value="0"', template 
    }
    void testFormTagWithStringURL() {
        def template = '<g:form url="/foo/bar"></g:form>'
        assertOutputEquals('<form action="/foo/bar" method="post" ></form>', template)
    }

    void testFormTagWithSynchronizedToken() {
        def template = '<g:form url="/foo/bar" useToken="true"></g:form>'
        assertOutputContains('<form action="/foo/bar" method="post" >', template)
        assertOutputContains('<input type="hidden" name="org.codehaus.groovy.grails.SYNCHRONIZER_TOKEN" value="', template)
        assertOutputContains('<input type="hidden" name="org.codehaus.groovy.grails.SYNCHRONIZER_URI" value="', template)
    }

    public void testTextFieldTag() {
        def template = '<g:textField name="testField" value="1" />'

        assertOutputEquals('<input type="text" name="testField" value="1" id="testField" />', template)

        template = '<g:textField name="testField" value="${value}" />'

        assertOutputEquals('<input type="text" name="testField" value="foo &gt; &quot; &amp; &lt; \'" id="testField" />', template, [value:/foo > " & < '/])
	}

    void testTextAreaWithBody() {
        def template = '<g:textArea name="test">This is content</g:textArea>'

        assertOutputEquals '<textarea name="test" id="test" >This is content</textarea>', template 
    }

    void testPasswordTag() {
        def template = '<g:passwordField name="myPassword" value="foo"/>'
        assertOutputEquals('<input type="password" name="myPassword" value="foo" id="myPassword" />', template)
    }
    

    void testFormWithURL() {
    	final StringWriter sw = new StringWriter();
    	final PrintWriter pw = new PrintWriter(sw);

    	withTag("form", pw) { tag ->
    	    // use sorted map to be able to predict the order in which tag attributes are generated
	    	def attributes = new TreeMap([url:[controller:'con', action:'action'], id:'formElementId'])
    	    tag.call(attributes, { "" })
    	    assertEquals '<form action="/con/action" method="post" id="formElementId" ></form>', sw.toString().trim()
    	}
    }

    void testActionSubmitWithoutAction() {
    	final StringWriter sw = new StringWriter();
    	final PrintWriter pw = new PrintWriter(sw);

    	withTag("actionSubmit", pw) { tag ->
    	    // use sorted map to be able to predict the order in which tag attributes are generated
	    	def attributes = new TreeMap([value:'Edit'])
    	    tag.call(attributes)
    	    assertEquals '<input type="submit" name="_action_Edit" value="Edit" />', sw.toString() // NO TRIM, TEST WS!
    	}
    }
    
    void testActionSubmitWithAction() {
    	final StringWriter sw = new StringWriter();
    	final PrintWriter pw = new PrintWriter(sw);

    	withTag("actionSubmit", pw) { tag ->
    	    // use sorted map to be able to predict the order in which tag attributes are generated
	    	def attributes = new TreeMap([action:'Edit', value:'Some label for editing'])
    	    tag.call(attributes)
    	    assertEquals '<input type="submit" name="_action_Edit" value="Some label for editing" />', sw.toString() // NO TRIM, TEST WS!
    	}
    }
    
    void testActionSubmitWithAdditionalAttributes() {
    	final StringWriter sw = new StringWriter();
    	final PrintWriter pw = new PrintWriter(sw);

    	withTag("actionSubmit", pw) { tag ->
    	    // use sorted map to be able to predict the order in which tag attributes are generated
	    	def attributes = new TreeMap([action:'Edit', value:'Some label for editing', style:'width: 200px;'])
    	    tag.call(attributes)
    	    assertEquals '<input type="submit" name="_action_Edit" value="Some label for editing" style="width: 200px;" />', sw.toString() // NO TRIM, TEST WS!
    	}
    }

    void testActionSubmitImageWithoutAction() {
    	final StringWriter sw = new StringWriter();
    	final PrintWriter pw = new PrintWriter(sw);

    	withTag("actionSubmitImage", pw) { tag ->
    	    // use sorted map to be able to predict the order in which tag attributes are generated
	    	def attributes = new TreeMap([src:'edit.gif', value:'Edit'])
    	    tag.call(attributes)
    	    assertEquals '<input type="image" name="_action_Edit" value="Edit" src="edit.gif" />', sw.toString() // NO TRIM, TEST WS!
    	}
    }

    void testActionSubmitImageWithAction() {
    	final StringWriter sw = new StringWriter();
    	final PrintWriter pw = new PrintWriter(sw);

    	withTag("actionSubmitImage", pw) { tag ->
    	    // use sorted map to be able to predict the order in which tag attributes are generated
	    	def attributes = new TreeMap([src:'edit.gif', action:'Edit', value:'Some label for editing'])
    	    tag.call(attributes)
    	    assertEquals '<input type="image" name="_action_Edit" value="Some label for editing" src="edit.gif" />', sw.toString() // NO TRIM, TEST WS!
    	}
    }
    
    void testActionSubmitImageWithAdditionalAttributes() {
    	final StringWriter sw = new StringWriter();
    	final PrintWriter pw = new PrintWriter(sw);

    	withTag("actionSubmitImage", pw) { tag ->
    	    // use sorted map to be able to predict the order in which tag attributes are generated
	    	def attributes = new TreeMap([src:'edit.gif', action:'Edit', value:'Some label for editing', style:'border-line: 0px;'])
    	    tag.call(attributes)
    	    assertEquals '<input type="image" name="_action_Edit" value="Some label for editing" src="edit.gif" style="border-line: 0px;" />', sw.toString() // NO TRIM, TEST WS!
    	}
    }

    public void testHtmlEscapingTextAreaTag() {
    	final StringWriter sw = new StringWriter();
    	final PrintWriter pw = new PrintWriter(sw);

    	withTag("textArea", pw) { tag ->
    	    // use sorted map to be able to predict the order in which tag attributes are generated
	    	def attributes = new TreeMap([name: "testField", value: "<b>some text</b>"])
    	    tag.call(attributes,{})
    	    assertEquals '<textarea id="testField" name="testField" >&lt;b&gt;some text&lt;/b&gt;</textarea>', sw.toString()
    	}
    }
    

    public void testTextAreaTag() {
    	final StringWriter sw = new StringWriter();
    	final PrintWriter pw = new PrintWriter(sw);

    	withTag("textArea", pw) { tag ->
    		// use sorted map to be able to predict the order in which tag attributes are generated
    		def attributes = new TreeMap([name: "testField", value: "1"])
    		tag.call(attributes,{})
    		assertEquals '<textarea id="testField" name="testField" >1</textarea>', sw.toString()
    	}
    }
    

}

