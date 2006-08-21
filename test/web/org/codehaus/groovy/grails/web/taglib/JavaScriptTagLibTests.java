package org.codehaus.groovy.grails.web.taglib;

import groovy.lang.Closure;
import groovy.lang.GroovyObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

import org.codehaus.groovy.grails.web.servlet.GrailsHttpServletRequest;

public class JavaScriptTagLibTests extends AbstractTagLibTests {

	public void testPrototypeRemoteFunction() throws Exception {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		
		Closure tag = getTag("remoteFunction",pw);
		GroovyObject tagLibrary = (GroovyObject)tag.getOwner();
		
		GrailsHttpServletRequest request = (GrailsHttpServletRequest)tagLibrary.getProperty("request");
		List includedLibrary = new ArrayList();
		includedLibrary.add("prototype");
		request.setAttribute("org.codehaus.grails.INCLUDED_JS_LIBRARIES", includedLibrary);
		
		Map attrs = new HashMap();
		attrs.put("action","action");
		attrs.put("controller","test");
		
		tag.call(new Object[]{attrs});

		assertEquals("new Ajax.Request('/test/action',{asynchronous:true,evalScripts:true});",sw.toString());
		
		attrs.put("action","action");
		attrs.put("controller","test");
		Map update = new HashMap();
		update.put("success", "updateMe");
		attrs.put("update",update);
		
		sw.getBuffer().delete(0,sw.getBuffer().length());
		
		tag.call(new Object[]{attrs});
		
		System.out.println(sw.toString());
		
		assertEquals("new Ajax.Updater({success:'updateMe'},'/test/action',{asynchronous:true,evalScripts:true});",sw.toString());
	}
	
	public void testDojoRemoteFunction() throws Exception {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		
		Closure tag = getTag("remoteFunction",pw);
		GroovyObject tagLibrary = (GroovyObject)tag.getOwner();
		
		GrailsHttpServletRequest request = (GrailsHttpServletRequest)tagLibrary.getProperty("request");
		List includedLibrary = new ArrayList();
		includedLibrary.add("dojo");
		request.setAttribute("org.codehaus.grails.INCLUDED_JS_LIBRARIES", includedLibrary);
		
		Map attrs = new HashMap();
		attrs.put("action","action");
		attrs.put("controller","test");
		
		tag.call(new Object[]{attrs});
		System.out.println(sw.toString());
		assertEquals("dojo.io.bind({url:'/test/action',load:function(type,data,evt) {},error:function(type,error) { }});",sw.toString());
		
		attrs.put("action","action");
		attrs.put("controller","test");
		Map update = new HashMap();
		update.put("success", "updateMe");
		attrs.put("update",update);
		
		sw.getBuffer().delete(0,sw.getBuffer().length());
		
		tag.call(new Object[]{attrs});
		
		System.out.println(sw.toString());
		
		assertEquals("dojo.io.bind({url:'/test/action',load:function(type,data,evt) {dojo.html.textContent( dojo.byId('updateMe'),data);},error:function(type,error) { }});",sw.toString());
	}	
	
	public void testYahooRemoteFunction() throws Exception {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		
		Closure tag = getTag("remoteFunction",pw);
		GroovyObject tagLibrary = (GroovyObject)tag.getOwner();
		
		GrailsHttpServletRequest request = (GrailsHttpServletRequest)tagLibrary.getProperty("request");
		List includedLibrary = new ArrayList();
		includedLibrary.add("yahoo");
		request.setAttribute("org.codehaus.grails.INCLUDED_JS_LIBRARIES", includedLibrary);
		
		Map attrs = new HashMap();
		attrs.put("action","action");
		attrs.put("controller","test");
		
		tag.call(new Object[]{attrs});
		System.out.println(sw.toString());
		assertEquals("YAHOO.util.Connect.asyncRequest('GET','/test/action',{ },null);",sw.toString());
		
		attrs.put("action","action");
		attrs.put("controller","test");
		Map update = new HashMap();
		update.put("success", "updateMe");
		attrs.put("update",update);
		
		sw.getBuffer().delete(0,sw.getBuffer().length());
		
		tag.call(new Object[]{attrs});
		
		System.out.println(sw.toString());
		
		assertEquals("YAHOO.util.Connect.asyncRequest('GET','/test/action',{ success: function(o) { YAHOO.util.Dom.get('updateMe').innerHTML = o.responseText; }, failure: function(o) {}},null);",sw.toString());
	}


    public void testEscapeJavascript() throws Exception {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);

		Closure tag = getTag("escapeJavascript",pw);

        tag.call(new Object[]{ "This is some \"text\" to be 'escaped'", Collections.EMPTY_MAP });
        assertEquals("This is some \\\"text\\\" to be \\'escaped\\'",sw.toString());
    }
}
