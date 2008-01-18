package org.codehaus.groovy.grails.web.pages;

import junit.framework.TestCase;
import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException;
import org.codehaus.groovy.grails.commons.ConfigurationHolder;

import java.io.*;

import groovy.util.ConfigSlurper;
import groovy.util.ConfigObject;


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

    protected String makeImports() {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < Parse.DEFAULT_IMPORTS.length; i++) {
            result.append( "import "+Parse.DEFAULT_IMPORTS[i]+"\n");
        }
        return result.toString();
    }

    public void testParse() throws Exception {
		String output = parseCode("myTest", "<div>hi</div>");
		String expected = makeImports() +
            "\n"+
			"class myTest extends GroovyPage {\n"+
			"public Object run() {\n"+
			"out.print('<div>hi</div>')\n"+
			"}\n"+
			"}";
		assertEquals(trimAndRemoveCR(expected), trimAndRemoveCR(output));
	}

    public void testParseWithUnclosedSquareBracket() throws Exception {
		String output = parseCode("myTest", "<g:message code=\"[\"/>");
		String expected = makeImports() +
			"\n"+
			"class myTest extends GroovyPage {\n"+
			"public Object run() {\n"+
            "attrs1 = [\"code\":\"[\"]\n" +
            "body1 = new GroovyPageTagBody(this,binding.webRequest) {\n" +
            "}\n" +
            "invokeTag('message','g',attrs1,body1)\n"+
			"}\n"+
			"}";

		assertEquals(trimAndRemoveCR(expected), trimAndRemoveCR(output));
	}

    public void testParseWithUnclosedGstringThrowsException() throws IOException {
        try{
            parseCode("myTest", "<g:message value=\"${boom\">");
        }catch(GrailsTagException e){
            assertEquals("Unexpected end of file encountered parsing Tag [message] for myTest. Are you missing a closing brace '}'?", e.getMessage());
            return;
        }
		fail("Expected parse exception not thrown");

    }

    public void testParseWithUTF8() throws IOException {
        // This is some unicode Chinese (who knows what it says!)
        String src = "Chinese text: \u3421\u3437\u343f\u3443\u3410\u3405\u38b3\u389a\u395e\u3947\u3adb\u3b5a\u3b67";
        // Sanity check the string loaded OK as unicode - it won't look right if you output it, default stdout is not UTF-8
        // on many OSes
        assertEquals(src.indexOf('?'), -1);


        ConfigObject config = new ConfigSlurper().parse("grails.views.gsp.encoding = \"UTF-8\"");

        ConfigurationHolder.setConfig( config);
        String output = null;
        try {
            output = parseCode("myTest", src);
        }
        finally {
            ConfigurationHolder.setConfig(null);
        }
        String expected = makeImports() +
            "\n"+
            "class myTest extends GroovyPage {\n"+
            "public Object run() {\n"+
            "out.print('"+src+"')\n"+
            "}\n"+
            "}";
        assertEquals(trimAndRemoveCR(expected), trimAndRemoveCR(output));

    }

    public void testParseWithLocalEncoding() throws IOException {
        String src = "This is just plain ASCII to make sure test works on all platforms";
        // Sanity check the string loaded OK as unicode - it won't look right if you output it, default stdout is not UTF-8
        // on many OSes
        assertEquals(src.indexOf('?'), -1);


        ConfigObject config = new ConfigSlurper().parse("grails.views.gsp.encoding = \"\"");

        ConfigurationHolder.setConfig( config);
        String output = null;
        try {
            output = parseCode("myTest", src);
        }
        finally {
            ConfigurationHolder.setConfig(null);
        }
        String expected = makeImports() +
            "\n"+
            "class myTest extends GroovyPage {\n"+
            "public Object run() {\n"+
            "out.print('"+src+"')\n"+
            "}\n"+
            "}";
        assertEquals(trimAndRemoveCR(expected), trimAndRemoveCR(output));

    }

    private void dumpCharValues(String str) {
        for (int i = 0; i < str.length(); i++) {
            System.out.println("char "+i+" is: "+(int) str.charAt(i));
        }
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
		while (((index = sb.toString().indexOf('\r')) != -1) || ((index = sb.toString().indexOf('\n')) != -1) ) {
			sb.deleteCharAt(index);
		}
		return sb.toString();
	}
	
	public String parseCode(String uri, String gsp) throws IOException {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);

        // Simulate what the parser does so we get it in the encoding expected
        Object enc = ConfigurationHolder.getFlatConfig().get("grails.views.gsp.encoding");
        if ((enc == null) || (enc.toString().trim().length() == 0)) {
            enc = System.getProperty("file.encoding", "us-ascii");
        }

        InputStream gspIn = new ByteArrayInputStream(gsp.getBytes(enc.toString()));
        Parse parse = new Parse(uri, uri, gspIn);
        InputStream in = parse.parse();
        send(in, pw, enc.toString());

		return sw.toString();
	}

 	public void testParseGTagsWithNamespaces() throws Exception {
 		String expected = makeImports() +
             "\n" +
             "class myTest extends GroovyPage {\n" +
             "public Object run() {\n" +
             "out.print(STATIC_HTML_CONTENT_0)\n" +
             "body1 = new GroovyPageTagBody(this,binding.webRequest) {\n" +
             "}\n" +
             "invokeTag('form','tt',[:],body1)\n" +
             "out.print(STATIC_HTML_CONTENT_1)\n" +
             "}\n" +
             "static final STATIC_HTML_CONTENT_0 = '''<tbody>'''\n" +
             "\n" +
             "static final STATIC_HTML_CONTENT_1 = '''</tbody>'''\n" +
             "\n" +
             "}";
 		String output = parseCode("myTest",
 		"<tbody>\n" +
 		"  <tt:form />\n" +
		"</tbody>");
 		System.out.println("|"+trimAndRemoveCR(output)+"|");
        System.out.println("|"+trimAndRemoveCR(expected)+"|");
        assertTrue( "should have call to tag with 'tt' namespace", output.indexOf("invokeTag('form','tt',[:],body1)") > -1);
 	}

    /**
     * Copy all of input to output.
     * @param in
     * @param out
     * @param encoding
     * @throws IOException
     */
    public static void send(InputStream in, Writer out, String encoding) throws IOException {
        try {
            Reader reader = new InputStreamReader(in, encoding);
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
