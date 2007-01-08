package org.codehaus.groovy.grails.web.taglib;

/**
 * Tests for the FormTagLib.groovy file which contains tags to help with the
 * creation of HTML forms
 *
 * @author Graeme
 * @author Marc Guillemot
 */
public class FormTagLib2Tests extends AbstractTagLibTests {
	def formTagLib
	
	protected void onSetUp()
	{
		super.onSetUp()
        def tagClass = grailsApplication.getTagLibClassForTag("textField");
        
		formTagLib = tagClass.newInstance()
		formTagLib.out = new StringBuffer()
	}

    public void testTextFieldTag() {
    	// use sorted map to be able to predict the order in which tag attributes are generated
    	def attributes = new TreeMap([name: "testField", value: "1"])
    	formTagLib.textField(attributes)

    	assertEquals '<input type=\'text\' id="testField" name="testField" value="1" />', formTagLib.out.toString()

    	formTagLib.out.length = 0
    	attributes.value = /foo > " & < '/
       	formTagLib.textField(attributes)
    	assertEquals '<input type=\'text\' id="testField" name="testField" value="foo &gt; &quot; &amp; &lt; \'" />', formTagLib.out.toString()
    }

    public void testTextAreaTag() {
    	// use sorted map to be able to predict the order in which tag attributes are generated
    	def attributes = new TreeMap([name: "testField", value: "1"])
    	formTagLib.textArea(attributes)
    	assertEquals '<textarea id="testField" name="testField" >1</textarea>', formTagLib.out.toString()
    }
    
    public void testHtmlEscapingTextAreaTag() {
    	// use sorted map to be able to predict the order in which tag attributes are generated
    	def attributes = new TreeMap([name: "testField", value: "<b>some text</b>"])
    	formTagLib.textArea(attributes)

    	assertEquals '<textarea id="testField" name="testField" >&lt;b&gt;some text&lt;/b&gt;</textarea>', formTagLib.out.toString()
    }
    
    public void testHiddenFieldTag() {
    	// use sorted map to be able to predict the order in which tag attributes are generated
    	def attributes = new TreeMap([name: "testField", value: "1"])
    	formTagLib.hiddenField(attributes)

    	assertEquals '<input type=\'hidden\' id="testField" name="testField" value="1" />', formTagLib.out.toString()
    }

    public void testRadioTag() {
    	// use sorted map to be able to predict the order in which tag attributes are generated
    	def attributes = new TreeMap([name: "testRadio", checked: "true", value: "1"])
    	formTagLib.radio(attributes)

    	assertEquals "<input type=\"radio\" name='testRadio' checked=\"checked\" value=\"1\"  ></input>", formTagLib.out.toString()

    	formTagLib.out.length = 0
    	attributes = new TreeMap([name: "testRadio", value: "2"])
    	formTagLib.radio(attributes)

    	assertEquals "<input type=\"radio\" name='testRadio' value=\"2\"  ></input>", formTagLib.out.toString()
    }

    public void testCheckboxTag() {
    	// use sorted map to be able to predict the order in which tag attributes are generated
    	def attributes = new TreeMap([name: "testCheck", extra: "1", value: "true"])
    	formTagLib.checkBox(attributes)

    	assertEquals '<input type="hidden" name="_testCheck" /><input type="checkbox" name=\'testCheck\' checked="checked" value=\'true\' extra="1"  />', formTagLib.out.toString()
    }
}
