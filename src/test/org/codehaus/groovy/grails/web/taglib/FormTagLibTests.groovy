package org.codehaus.groovy.grails.web.taglib

/**
 * Tests for the FormTagLib.groovy file which contains tags to help with the                                         l
 * creation of HTML forms
 *
 * @author Graeme
 */
class FormTagLibTests extends AbstractGrailsTagTests {

    void testFormTagWithAlternativeMethod() {
        def template = '<g:form url="/foo/bar" method="delete"></g:form>'
        assertOutputEquals('<form action="/foo/bar" method="post" ><input type="hidden" name="_method" value="DELETE" id="_method" /></form>', template)
    }

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

    void testTextFieldTag() {
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
        final StringWriter sw = new StringWriter()

        withTag("form", new PrintWriter(sw)) { tag ->
            // use sorted map to be able to predict the order in which tag attributes are generated
            def attributes = new TreeMap([url:[controller:'con', action:'action'], id:'formElementId'])
            tag.call(attributes, { "" })
            assertEquals '<form action="/con/action" method="post" id="formElementId" ></form>', sw.toString().trim()
        }
    }

    void testActionSubmitWithoutAction() {
        final StringWriter sw = new StringWriter()

        withTag("actionSubmit", new PrintWriter(sw)) { tag ->
            // use sorted map to be able to predict the order in which tag attributes are generated
            def attributes = new TreeMap([value:'Edit'])
            tag.call(attributes)
            assertEquals '<input type="submit" name="_action_Edit" value="Edit" />', sw.toString() // NO TRIM, TEST WS!
        }
    }

    void testActionSubmitWithAction() {
        final StringWriter sw = new StringWriter()

        withTag("actionSubmit", new PrintWriter(sw)) { tag ->
            // use sorted map to be able to predict the order in which tag attributes are generated
            def attributes = new TreeMap([action:'Edit', value:'Some label for editing'])
            tag.call(attributes)
            assertEquals '<input type="submit" name="_action_Edit" value="Some label for editing" />', sw.toString() // NO TRIM, TEST WS!
        }
    }

    /**
     * GRAILS-454 - Make sure that the 'name' attribute is ignored.
     */
    void testActionSubmitWithName() {
        final StringWriter sw = new StringWriter()

        withTag("actionSubmit", new PrintWriter(sw)) { tag ->
            // use sorted map to be able to predict the order in which tag attributes are generated
            def attributes = new TreeMap([action:'Edit', value:'Some label for editing', name:'customName'])
            tag.call(attributes)
            assertEquals '<input type="submit" name="_action_Edit" value="Some label for editing" />', sw.toString() // NO TRIM, TEST WS!
        }
    }

    void testActionSubmitWithAdditionalAttributes() {
        final StringWriter sw = new StringWriter()

        withTag("actionSubmit", new PrintWriter(sw)) { tag ->
            // use sorted map to be able to predict the order in which tag attributes are generated
            def attributes = new TreeMap([action:'Edit', value:'Some label for editing', style:'width: 200px;'])
            tag.call(attributes)
            assertEquals '<input type="submit" name="_action_Edit" value="Some label for editing" style="width: 200px;" />', sw.toString() // NO TRIM, TEST WS!
        }
    }

    void testActionSubmitImageWithoutAction() {
        final StringWriter sw = new StringWriter()

        withTag("actionSubmitImage", new PrintWriter(sw)) { tag ->
            // use sorted map to be able to predict the order in which tag attributes are generated
            def attributes = new TreeMap([src:'edit.gif', value:'Edit'])
            tag.call(attributes)
            assertEquals '<input type="image" name="_action_Edit" value="Edit" src="edit.gif" />', sw.toString() // NO TRIM, TEST WS!
        }
    }

    void testActionSubmitImageWithAction() {
        final StringWriter sw = new StringWriter()

        withTag("actionSubmitImage", new PrintWriter(sw)) { tag ->
            // use sorted map to be able to predict the order in which tag attributes are generated
            def attributes = new TreeMap([src:'edit.gif', action:'Edit', value:'Some label for editing'])
            tag.call(attributes)
            assertEquals '<input type="image" name="_action_Edit" value="Some label for editing" src="edit.gif" />', sw.toString() // NO TRIM, TEST WS!
        }
    }

    void testActionSubmitImageWithAdditionalAttributes() {
        final StringWriter sw = new StringWriter()

        withTag("actionSubmitImage", new PrintWriter(sw)) { tag ->
            // use sorted map to be able to predict the order in which tag attributes are generated
            def attributes = new TreeMap([src:'edit.gif', action:'Edit', value:'Some label for editing', style:'border-line: 0px;'])
            tag.call(attributes)
            assertEquals '<input type="image" name="_action_Edit" value="Some label for editing" src="edit.gif" style="border-line: 0px;" />', sw.toString() // NO TRIM, TEST WS!
        }
    }

    void testHtmlEscapingTextAreaTag() {
        final StringWriter sw = new StringWriter()

        withTag("textArea", new PrintWriter(sw)) { tag ->
            def attributes = [name: "testField", value: "<b>some text</b>"]
            tag.call(attributes,{})
            assertEquals '<textarea name="testField" id="testField" >&lt;b&gt;some text&lt;/b&gt;</textarea>', sw.toString()
        }
    }

    void testTextAreaTag() {
        final StringWriter sw = new StringWriter()

        withTag("textArea", new PrintWriter(sw)) { tag ->
            def attributes = [name: "testField", value: "1"]
            tag.call(attributes,{})
            assertEquals '<textarea name="testField" id="testField" >1</textarea>', sw.toString()
        }
    }
}
