package org.grails.web.pages;

import junit.framework.TestCase;
import org.grails.web.pages.GroovyPage;
import org.grails.web.pages.GroovyPageScanner;
import org.grails.web.pages.Tokens;

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

        GroovyPageScanner s = new GroovyPageScanner(gsp);
        int next;
        while ((next = s.nextToken()) != Tokens.EOF) {
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

        GroovyPageScanner s = new GroovyPageScanner(gsp);
        int next;
        while ((next = s.nextToken()) != Tokens.EOF) {
            if (next == Tokens.GSTART_TAG ||
                next == Tokens.GEND_TAG) {
                assertEquals(GroovyPage.DEFAULT_NAMESPACE, s.getNamespace());
            }
        }
    }
    
    public void testMaxHtmlLength() {
        String gsp = "0123456789ABCDEFGHIJK";
        GroovyPageScanner scanner = new GroovyPageScanner(gsp);
        scanner.setMaxHtmlLength(10);
        assertEquals(GroovyPageScanner.HTML, scanner.nextToken());
        assertEquals("0123456789", scanner.getToken());
        assertEquals(GroovyPageScanner.HTML, scanner.nextToken());
        assertEquals("ABCDEFGHIJ", scanner.getToken());
        assertEquals(GroovyPageScanner.HTML, scanner.nextToken());
        assertEquals("K", scanner.getToken());
        assertEquals(GroovyPageScanner.EOF, scanner.nextToken());
    }
}
