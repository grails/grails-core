package org.codehaus.groovy.grails.web.taglib

import grails.util.MockRequestDataValueProcessor

import org.codehaus.groovy.grails.plugins.web.taglib.FormTagLib
import org.codehaus.groovy.grails.support.MockApplicationContext
import org.springframework.web.servlet.support.RequestDataValueProcessor

/**
 * Tests for the FormTagLib.groovy file which contains tags to help with the                                         l
 * creation of HTML forms
 *
 * @author Graeme
 */
class FormTagLibTests extends AbstractGrailsTagTests {

    void registerRequestDataValueProcessor() {
        RequestDataValueProcessor requestDataValueProcessor = new MockRequestDataValueProcessor();
        MockApplicationContext applicationContext = (MockApplicationContext)ctx;
        applicationContext.registerMockBean("requestDataValueProcessor",requestDataValueProcessor);
    }
    void unRegisterRequestDataValueProcessor() {
        MockApplicationContext applicationContext = (MockApplicationContext)ctx;
        applicationContext.registerMockBean("requestDataValueProcessor",null);
    }

    void testFormTagWithAlternativeMethod() {
        def template = '<g:form url="/foo/bar" method="delete"></g:form>'
        assertOutputEquals('<form action="/foo/bar" method="post" ><input type="hidden" name="_method" value="DELETE" id="_method" /></form>', template)
    }

    void testFormTagWithAlternativeMethodAndRequestDataValueProcessor() {
        this.registerRequestDataValueProcessor()
        def template = '<g:form url="/foo/bar" method="delete"></g:form>'
        assertOutputEquals('<form action="/foo/bar" method="post" ><input type="hidden" name="_method" value="DELETE_PROCESSED_" id="_method" /><input type="hidden" name="requestDataValueProcessorHiddenName" value="hiddenValue" />\n</form>', template)
        this.unRegisterRequestDataValueProcessor()
    }

    // test for GRAILS-3865
    void testHiddenFieldWithZeroValue() {
        def template = '<g:hiddenField name="index" value="${0}" />'
        assertOutputContains 'value="0"', template
    }
    void testHiddenFieldWithZeroValueAndRequestDataValueProcessor() {
        this.registerRequestDataValueProcessor();
        def template = '<g:hiddenField name="index" value="${0}" />'
        assertOutputContains 'value="0_PROCESSED_"', template
        this.unRegisterRequestDataValueProcessor();
    }

    void testFormTagWithStringURL() {
        def template = '<g:form url="/foo/bar"></g:form>'
        assertOutputEquals('<form action="/foo/bar" method="post" ></form>', template)
    }

    void testFormTagWithStringURLAndRequestDataValueProcessor() {
        this.registerRequestDataValueProcessor();
        
        def template = '<g:form url="/foo/bar"></g:form>'
        assertOutputEquals('<form action="/foo/bar" method="post" ><input type="hidden" name="requestDataValueProcessorHiddenName" value="hiddenValue" />\n</form>', template)
        this.unRegisterRequestDataValueProcessor();
    }

    void testFormTagWithTrueUseToken() {
        def template = '<g:form url="/foo/bar" useToken="true"></g:form>'
        assertOutputContains('<form action="/foo/bar" method="post" >', template)
        assertOutputContains('<input type="hidden" name="org.codehaus.groovy.grails.SYNCHRONIZER_TOKEN" value="', template)
        assertOutputContains('<input type="hidden" name="org.codehaus.groovy.grails.SYNCHRONIZER_URI" value="', template)

        template = '<g:form url="/foo/bar" useToken="${2 * 3 == 6}"></g:form>'
        assertOutputContains('<form action="/foo/bar" method="post" >', template)
        assertOutputContains('<input type="hidden" name="org.codehaus.groovy.grails.SYNCHRONIZER_TOKEN" value="', template)
        assertOutputContains('<input type="hidden" name="org.codehaus.groovy.grails.SYNCHRONIZER_URI" value="', template)
    }

    void testFormTagWithTrueUseTokenAndRequestDataValueProcessor() {
        this.registerRequestDataValueProcessor();

        def template = '<g:form url="/foo/bar" useToken="true"></g:form>'
        assertOutputContains('<form action="/foo/bar" method="post" >', template)
        assertOutputContains('<input type="hidden" name="org.codehaus.groovy.grails.SYNCHRONIZER_TOKEN" value="', template)
        assertOutputContains('<input type="hidden" name="org.codehaus.groovy.grails.SYNCHRONIZER_URI" value="', template)
        assertOutputContains('<input type="hidden" name="requestDataValueProcessorHiddenName" value="hiddenValue" />',template)
        assertOutputContains('_PROCESSED_',template)

        template = '<g:form url="/foo/bar" useToken="${2 * 3 == 6}"></g:form>'
        assertOutputContains('<form action="/foo/bar" method="post" >', template)
        assertOutputContains('<input type="hidden" name="org.codehaus.groovy.grails.SYNCHRONIZER_TOKEN" value="', template)
        assertOutputContains('<input type="hidden" name="org.codehaus.groovy.grails.SYNCHRONIZER_URI" value="', template)
        assertOutputContains('<input type="hidden" name="requestDataValueProcessorHiddenName" value="hiddenValue" />',template)
        assertOutputContains('<input type="hidden" name="requestDataValueProcessorHiddenName" value="hiddenValue" />',template)
        assertOutputContains('_PROCESSED_',template)
        this.unRegisterRequestDataValueProcessor();
    }

    void testFormTagWithNonTrueUseToken() {
        def template = '<g:form url="/foo/bar" useToken="false"></g:form>'
        assertOutputContains('<form action="/foo/bar" method="post" >', template)
        assertOutputNotContains('SYNCHRONIZER_TOKEN', template)
        assertOutputNotContains('SYNCHRONIZER_URI', template)

        template = '<g:form url="/foo/bar" useToken="someNonTrueValue"></g:form>'
        assertOutputContains('<form action="/foo/bar" method="post" >', template)
        assertOutputNotContains('SYNCHRONIZER_TOKEN', template)
        assertOutputNotContains('SYNCHRONIZER_URI', template)

        template = '<g:form url="/foo/bar" useToken="${42 * 2112 == 3}"></g:form>'
        assertOutputContains('<form action="/foo/bar" method="post" >', template)
        assertOutputNotContains('SYNCHRONIZER_TOKEN', template)
        assertOutputNotContains('SYNCHRONIZER_URI', template)
    }

    void testTextFieldTag() {
        def template = '<g:textField name="testField" value="1" />'
        assertOutputEquals('<input type="text" name="testField" value="1" id="testField" />', template)

        template = '<g:textField name="testField" value="${value}" />'
        assertOutputEquals('<input type="text" name="testField" value="foo &gt; &quot; &amp; &lt; &#39;" id="testField" />', template, [value:/foo > " & < '/])
    }

    void testTextFieldTagWithRequestDataValueProcessor() {
        this.registerRequestDataValueProcessor()
        def template = '<g:textField name="testField" value="1" />'
        assertOutputEquals('<input type="text" name="testField" value="1_PROCESSED_" id="testField" />', template)

        template = '<g:textField name="testField" value="${value}" />'
        assertOutputEquals('<input type="text" name="testField" value="foo &gt; &quot; &amp; &lt; &#39;_PROCESSED_" id="testField" />', template, [value:/foo > " & < '/])
        this.unRegisterRequestDataValueProcessor()
    }

    void testTextAreaWithBody() {
        def template = '<g:textArea name="test">This is content</g:textArea>'
        assertOutputEquals '<textarea name="test" id="test" >This is content</textarea>', template        
    }

    void testTextAreaWithBodyAndRequestDataValueProcessor() {
        this.registerRequestDataValueProcessor()
        def template = '<g:textArea name="test">This is content</g:textArea>'
        assertOutputEquals '<textarea name="test" id="test" >This is content_PROCESSED_</textarea>', template
        this.unRegisterRequestDataValueProcessor()
    }

    void testPasswordTag() {
        def template = '<g:passwordField name="myPassword" value="foo"/>'
        assertOutputEquals('<input type="password" name="myPassword" value="foo" id="myPassword" />', template)
    }

    void testPasswordTagWithRequestDataValueProcessor() {
        this.registerRequestDataValueProcessor()
        def template = '<g:passwordField name="myPassword" value="foo"/>'
        assertOutputEquals('<input type="password" name="myPassword" value="foo_PROCESSED_" id="myPassword" />', template)
        this.unRegisterRequestDataValueProcessor()
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

    void testFormWithURLAndRequestDataValueProcessor() {
        this.registerRequestDataValueProcessor()
        final StringWriter sw = new StringWriter()

        withTag("form", new PrintWriter(sw)) { tag ->
            // use sorted map to be able to predict the order in which tag attributes are generated
            def attributes = new TreeMap([url:[controller:'con', action:'action'], id:'formElementId'])
            tag.call(attributes, { "" })
            assertEquals '<form action="/con/action" method="post" id="formElementId" ><input type="hidden" name="requestDataValueProcessorHiddenName" value="hiddenValue" />\n</form>', sw.toString().trim()
        }
        this.unRegisterRequestDataValueProcessor()
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

    void testActionSubmitWithoutActionAndWithRequestDataValueProcessor() {
        this.registerRequestDataValueProcessor()
        final StringWriter sw = new StringWriter()

        withTag("actionSubmit", new PrintWriter(sw)) { tag ->
            // use sorted map to be able to predict the order in which tag attributes are generated
            def attributes = new TreeMap([value:'Edit'])
            tag.call(attributes)
            assertEquals '<input type="submit" name="_action_Edit" value="Edit_PROCESSED_" />', sw.toString() // NO TRIM, TEST WS!
        }
        this.unRegisterRequestDataValueProcessor()
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

    void testActionSubmitWithActionAndRequestDataValueProcessor() {
        this.registerRequestDataValueProcessor()
        final StringWriter sw = new StringWriter()

        withTag("actionSubmit", new PrintWriter(sw)) { tag ->
            // use sorted map to be able to predict the order in which tag attributes are generated
            def attributes = new TreeMap([action:'Edit', value:'Some label for editing'])
            tag.call(attributes)
            assertEquals '<input type="submit" name="_action_Edit" value="Some label for editing_PROCESSED_" />', sw.toString() // NO TRIM, TEST WS!
        }
        this.unRegisterRequestDataValueProcessor()
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

    void testActionSubmitImageWithoutActionAndWithRequestDataValueProcessor() {
        this.registerRequestDataValueProcessor()
        final StringWriter sw = new StringWriter()

        withTag("actionSubmitImage", new PrintWriter(sw)) { tag ->
            // use sorted map to be able to predict the order in which tag attributes are generated
            def attributes = new TreeMap([src:'edit.gif', value:'Edit'])
            tag.call(attributes)
            assertEquals '<input type="image" name="_action_Edit" value="Edit_PROCESSED_" src="edit.gif?requestDataValueProcessorParamName=paramValue" />', sw.toString() // NO TRIM, TEST WS!
        }
        this.unRegisterRequestDataValueProcessor()
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
    void testPassingTheSameMapToTextField() {
        // GRAILS-8250
        StringWriter sw = new StringWriter()

        def attributes = [name: 'A']
        withTag("textField", new PrintWriter(sw)) { tag ->
            tag.call(attributes)
            assertEquals '<input type="text" name="A" value="" id="A" />', sw.toString()
        }

        sw = new StringWriter()
        attributes.name = 'B'
        withTag("textField", new PrintWriter(sw)) { tag ->
            tag.call(attributes)
            assertEquals '<input type="text" name="B" value="" id="B" />', sw.toString()
        }
    }

    void testFieldImplDoesNotApplyAttributesFromPreviousInvocation() {
        // GRAILS-8250
        def attrs = [:]
        def out = new StringBuilder()
        attrs.name = 'A'
        attrs.type = 'text'
        attrs.tagName = 'textField'

        def tag = new FormTagLib()
        tag.fieldImpl out, attrs
        assert '<input type="text" name="A" value="" id="A" />' == out.toString()

        out = new StringBuilder()
        attrs.name = 'B'
        attrs.type = 'text'
        attrs.tagName = 'textField'

        tag = new FormTagLib()
        tag.fieldImpl out, attrs
        assert '<input type="text" name="B" value="" id="B" />' == out.toString()
    }

    private void doTestBoolean(def attributes, String expected) {
        def sw = new StringWriter()
        withTag('textField', new PrintWriter(sw)) { tag ->
            tag.call(attributes)
            assertEquals expected, sw.toString()
        }
    }

    void testBooleanAttributes() {
        // GRAILS-3468
        // Test readonly for string as boolean true
        def attributes = [name: 'myfield', value: '1', readonly: 'true']
        doTestBoolean(attributes, '<input type="text" name="myfield" value="1" readonly="readonly" id="myfield" />')

        // Test readonly for string as boolean false
        attributes = [name: 'myfield', value: '1', readonly: 'false']
        doTestBoolean(attributes, '<input type="text" name="myfield" value="1" id="myfield" />')

        // Test readonly for real boolean true
        attributes = [name: 'myfield', value: '1', readonly: true]
        doTestBoolean(attributes, '<input type="text" name="myfield" value="1" readonly="readonly" id="myfield" />')

        // Test readonly for real boolean false
        attributes = [name: 'myfield', value: '1', readonly: false]
        doTestBoolean(attributes, '<input type="text" name="myfield" value="1" id="myfield" />')

        // Test readonly for its default value
        attributes = [name: 'myfield', value: '1', readonly: 'readonly']
        doTestBoolean(attributes, '<input type="text" name="myfield" value="1" readonly="readonly" id="myfield" />')

        // Test readonly for a value different from the defined in the spec
        attributes = [name: 'myfield', value: '1', readonly: 'other value']
        doTestBoolean(attributes, '<input type="text" name="myfield" value="1" readonly="other value" id="myfield" />')

        // Test readonly for null value
        attributes = [name: 'myfield', value: '1', readonly: null]
        doTestBoolean(attributes, '<input type="text" name="myfield" value="1" id="myfield" />')

        // Test disabled for string as boolean true
        attributes = [name: 'myfield', value: '1', disabled: 'true']
        doTestBoolean(attributes, '<input type="text" name="myfield" value="1" disabled="disabled" id="myfield" />')

        // Test disabled for string as boolean false
        attributes = [name: 'myfield', value: '1', disabled: 'false']
        doTestBoolean(attributes, '<input type="text" name="myfield" value="1" id="myfield" />')

        // Test disabled for real boolean true
        attributes = [name: 'myfield', value: '1', disabled: true]
        doTestBoolean(attributes, '<input type="text" name="myfield" value="1" disabled="disabled" id="myfield" />')

        // Test disabled for real boolean false
        attributes = [name: 'myfield', value: '1', disabled: false]
        doTestBoolean(attributes, '<input type="text" name="myfield" value="1" id="myfield" />')

        // Test disabled for its default value
        attributes = [name: 'myfield', value: '1', disabled: 'disabled']
        doTestBoolean(attributes, '<input type="text" name="myfield" value="1" disabled="disabled" id="myfield" />')

        // Test disabled for a value different from the defined in the spec
        attributes = [name: 'myfield', value: '1', disabled: 'other value']
        doTestBoolean(attributes, '<input type="text" name="myfield" value="1" disabled="other value" id="myfield" />')

        // Test disabled for null value
        attributes = [name: 'myfield', value: '1', disabled: null]
        doTestBoolean(attributes, '<input type="text" name="myfield" value="1" id="myfield" />')
    }
}
