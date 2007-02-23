package org.codehaus.groovy.grails.web.pages;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.io.IOException;

import junit.framework.TestCase;


/**
 * Tests the GSP parser.  This can detect issues caused by improper 
 * GSP->Groovy conversion.  Normally, to compare the code, you can
 * run the page with a showSource parameter specified.
 * 
 * The methods parseCode() and trimAndRemoveCR() have been added 
 * to simplify test case code.
 * 
 * @author Daiji
 *
 */
public class ParseTests extends TestCase {

	public void testParse() throws Exception {
		String output = parseCode("myTest", "<div>hi</div>");
		String expected = 
			"import org.codehaus.groovy.grails.web.pages.GroovyPage\n" +
			"import org.codehaus.groovy.grails.web.taglib.*\n"+
			"\n"+
			"class myTest extends GroovyPage {\n"+
			"public Object run() {\n"+
			"out.print('<div>hi</div>')\n"+
			"}\n"+
			"}";

		System.out.println(output);
		assertEquals(expected, trimAndRemoveCR(output));
	}
	
	/**
	 * Eliminate potential issues caused by operating system differences
	 * and minor output differences that we don't care about.
	 * 
	 * Note: this code is inefficient and could stand to be optimized.
	 */
	public String trimAndRemoveCR(String s) {
		int index;
		StringBuffer sb = new StringBuffer(s.trim());
		while ((index = sb.toString().indexOf('\r')) != -1) {
			sb.deleteCharAt(index);
		}
		return sb.toString();
	}
	
	public String parseCode(String uri, String gsp) throws IOException {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		InputStream gspIn = new ByteArrayInputStream(gsp.getBytes());
        Parse parse = new Parse(uri, gspIn);
        InputStream in = parse.parse();
        send(in, pw);

		return sw.toString();
	}

    /**
     * Copy all of input to output.
     * @param in
     * @param out
     * @throws IOException
     */
    public static void send(InputStream in, Writer out) throws IOException {
        try {
            Reader reader = new InputStreamReader(in);
            char[] buf = new char[8192];
            for (;;) {
                int read = reader.read(buf);
                if (read <= 0) break;
                out.write(buf, 0, read);
            }
        } finally {
            out.close();
            in.close();
        }
    } // writeInputStreamToResponse()
}
