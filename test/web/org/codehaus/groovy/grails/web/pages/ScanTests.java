package org.codehaus.groovy.grails.web.pages;

import junit.framework.TestCase;


/**
 * Tests the GSP lexer (Scan class). 
 * 
 * @author a.shneyderman
 */
public class ScanTests extends TestCase {
	
	public void testTagsCustomNamespace() {
		String gsp = 
			"<tbody>\n" +
			"  <tt:form />\n" +
			"</tbody>";
		
		Scan s = new Scan (gsp);
		int next;
		while((next = s.nextToken()) != Tokens.EOF) {
			if (next == Tokens.GSTART_TAG ||
				next == Tokens.GEND_TAG) {
				assertEquals("tt", s.getNamespace());
			}
		}
	}
	
	public void testTagsDefaultNamespace() {
		String gsp = 
			"<tbody>\n" +
			"  <g:form />\n" +
			"</tbody>";
		
		Scan s = new Scan (gsp);
		int next;
		while((next = s.nextToken()) != Tokens.EOF) {
			if (next == Tokens.GSTART_TAG ||
				next == Tokens.GEND_TAG) {
				assertEquals(GroovyPage.DEFAULT_NAMESPACE, s.getNamespace());
			}
		}
	}
	
}
