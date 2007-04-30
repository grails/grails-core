package org.codehaus.groovy.grails.web.taglib;

import groovy.lang.Closure;
import groovy.lang.GroovyObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

import org.codehaus.groovy.grails.web.servlet.GrailsHttpServletRequest;

public class JavaScriptTagLibTests extends AbstractGrailsTagTests {

	public void testPrototypeRemoteFunction() throws Exception {
		StringWriter sw = new StringWriter()
		PrintWriter pw = new PrintWriter(sw)
		
		withTag("remoteFunction",pw) { tag ->
			GroovyObject tagLibrary = (GroovyObject)tag.getOwner()
			GrailsHttpServletRequest request = (GrailsHttpServletRequest)tagLibrary.getProperty("request")
			def includedLibrary = ['prototype']
			request.setAttribute("org.codehaus.grails.INCLUDED_JS_LIBRARIES", includedLibrary)
			
			def attrs = [action:'action',controller:'test']
			tag.call(attrs)
			assertEquals("new Ajax.Request('/test/action',{asynchronous:true,evalScripts:true});",sw.toString())
			
			sw.getBuffer().delete(0,sw.getBuffer().length())
			attrs = [action:'action',controller:'test',update:[success:'updateMe'],options:[insertion:'Insertion.Bottom']]
			tag.call(attrs)
			assertEquals("new Ajax.Updater({success:'updateMe'},'/test/action',{asynchronous:true,evalScripts:true,insertion:Insertion.Bottom});",sw.toString())
		}
	}
	
	public void testDojoRemoteFunction() throws Exception {
		StringWriter sw = new StringWriter()
		PrintWriter pw = new PrintWriter(sw)
		
		withTag("remoteFunction",pw) { tag ->
			GroovyObject tagLibrary = (GroovyObject)tag.getOwner()
			GrailsHttpServletRequest request = (GrailsHttpServletRequest)tagLibrary.getProperty("request")
			def includedLibrary = ['dojo']
			request.setAttribute("org.codehaus.grails.INCLUDED_JS_LIBRARIES", includedLibrary)
			
			def attrs = [action:'action',controller:'test']
			tag.call(attrs)
			assertEquals("dojo.io.bind({url:'/test/action',load:function(type,data,evt) {},error:function(type,error) { }});",sw.toString());
			
			sw.getBuffer().delete(0,sw.getBuffer().length())
			attrs = [action:'action',controller:'test',update:[success:'updateMe']]
			tag.call(attrs)
			assertEquals("dojo.io.bind({url:'/test/action',load:function(type,data,evt) {dojo.html.textContent( dojo.byId('updateMe'),data);},error:function(type,error) { }});",sw.toString())
		}
	}	
	
	public void testYahooRemoteFunction() throws Exception {
		StringWriter sw = new StringWriter()
		PrintWriter pw = new PrintWriter(sw)
		
		withTag("remoteFunction",pw) { tag ->
			GroovyObject tagLibrary = (GroovyObject)tag.getOwner()
			GrailsHttpServletRequest request = (GrailsHttpServletRequest)tagLibrary.getProperty("request")
			def includedLibrary = ['yahoo']
			request.setAttribute("org.codehaus.grails.INCLUDED_JS_LIBRARIES", includedLibrary)
			
			def attrs = [action:'action',controller:'test']
			tag.call(attrs)
			assertEquals("YAHOO.util.Connect.asyncRequest('GET','/test/action',{ success: function(o) {  }, failure: function(o) {}},null);",sw.toString());
			
			sw.getBuffer().delete(0,sw.getBuffer().length());
			attrs = [action:'action',controller:'test',update:[success:'updateMe']]
			tag.call(attrs);
			assertEquals("YAHOO.util.Connect.asyncRequest('GET','/test/action',{ success: function(o) { YAHOO.util.Dom.get('updateMe').innerHTML = o.responseText; }, failure: function(o) {}},null);",sw.toString());
		}
	}

    void testRemoteField() {
        // <g:remoteField action="changeTitle" update="titleDiv"  name="title" value="${book?.title}"/>
        StringWriter sw = new StringWriter()
        PrintWriter pw = new PrintWriter(sw)

        withTag("remoteField",pw) { tag ->
            GroovyObject tagLibrary = (GroovyObject)tag.getOwner()
            GrailsHttpServletRequest request = (GrailsHttpServletRequest)tagLibrary.getProperty("request")
            def includedLibrary = ['prototype']
            request.setAttribute("org.codehaus.grails.INCLUDED_JS_LIBRARIES", includedLibrary)

            def attrs = [controller:'test',action:'changeTitle',update:'titleDiv',name:'title',value:'testValue']
            tag.call(attrs) { "body" }
            assertEquals("<input type=\"text\" name=\"title\" value=\"testValue\" onkeyup=\"new Ajax.Updater('titleDiv','/test/changeTitle',{asynchronous:true,evalScripts:true,parameters:'value='+this.value});\" />",sw.toString())
        }

    }


    public void testEscapeJavascript() throws Exception {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);

		withTag("escapeJavascript",pw) { tag ->

	        tag.call( "This is some \"text\" to be 'escaped'", Collections.EMPTY_MAP );
	        assertEquals("This is some \\\"text\\\" to be \\'escaped\\'",sw.toString());
		}
    }
}
