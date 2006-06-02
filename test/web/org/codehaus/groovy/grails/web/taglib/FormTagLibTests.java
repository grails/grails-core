package org.codehaus.groovy.grails.web.taglib;

import groovy.lang.Closure;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
/**
 * Tests for the FormTagLib.groovy file which contains tags to help with the
 * creation of HTML forms
 * 
 * @author Graeme
 *
 */
public class FormTagLibTests extends AbstractTagLibTests {

	public void testRadioTag() throws Exception {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		
		Closure tag = getTag("radio",pw);
		
		assertNotNull(tag);
		
		Map attrs = new HashMap();
		attrs.put("name","testRadio");
		attrs.put("checked", "true");
		attrs.put("value", "1");
		
		tag.call(new Object[]{attrs});
		
		Document document = DocumentHelper.parseText(sw.toString());
		assertNotNull(document);
		
		Element inputElement = document.getRootElement();
		assertEquals("input",inputElement.getName());
		
		assertEquals("testRadio",inputElement.attributeValue("name"));
		assertEquals("checked",inputElement.attributeValue("checked"));
		assertEquals("1",inputElement.attributeValue("value"));
		
		sw.getBuffer().delete(0,sw.getBuffer().length());
		
		attrs.remove("checked");
		attrs.put("name","testRadio");
		attrs.put("value","2");
		
		tag.call(new Object[]{attrs});
		
		document = DocumentHelper.parseText(sw.toString());
		assertNotNull(document);
		
		System.out.println(sw.toString());
		inputElement = document.getRootElement();
		assertEquals("input",inputElement.getName());
		
		assertEquals("testRadio",inputElement.attributeValue("name"));
		assertNull(inputElement.attributeValue("checked"));
		assertEquals("2",inputElement.attributeValue("value"));		
	}
	
	public void testCheckboxTag() throws Exception {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		
		Closure tag = getTag("checkBox",pw);
		
		Map attrs = new HashMap();
		attrs.put("name","testCheck");
		attrs.put("value", "true");		
		attrs.put("extra","1");
		
		assertNotNull(tag);
		
		tag.call(new Object[]{attrs});
		
		String enclosed  = "<test>" + sw.toString() + "</test>";
		
		System.out.println(enclosed);
		Document document = DocumentHelper.parseText(enclosed);
		assertNotNull(document);
		
		Element root = document.getRootElement();
		
		List els = root.elements();
		assertEquals(2, els.size());
		
		Element hidden = (Element)els.get(0);
		Element checkbox = (Element)els.get(1);
		
		assertEquals("hidden", hidden.attributeValue("type"));
		assertEquals("_testCheck", hidden.attributeValue("name"));
		
		assertEquals("checkbox", checkbox.attributeValue("type"));
		assertEquals("testCheck", checkbox.attributeValue("name"));
		assertEquals("checked", checkbox.attributeValue("checked"));
		assertEquals("true", checkbox.attributeValue("value"));
		assertEquals("1", checkbox.attributeValue("extra"));
		
		
		
	}
}
