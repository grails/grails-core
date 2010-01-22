package org.codehaus.groovy.grails.web.pages;

import groovy.util.ConfigObject;
import groovy.util.ConfigSlurper;
import junit.framework.TestCase;

import org.apache.commons.io.IOUtils;
import org.codehaus.groovy.grails.commons.ConfigurationHolder;
import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException;

import java.io.*;


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
	
	class ParsedResult {
		String generatedGsp;
		GroovyPageParser parser;
		String[] htmlParts;
		
		public String toString() { return generatedGsp; }
	}
	
	protected static final String GSP_FOOTER = "public static final Map JSP_TAGS = new HashMap()\n"
			+ "protected void init() {\n"
			+ "\tthis.jspTags = JSP_TAGS\n"
			+ "}\n"
			+ "public static final String CONTENT_TYPE = 'text/html;charset=UTF-8'\n"
			+ "public static final long LAST_MODIFIED = 0L\n"
			+ "public static final String DEFAULT_CODEC = null\n"+ "}\n";

    protected String makeImports() {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < GroovyPageParser.DEFAULT_IMPORTS.length; i++) {
            result.append( "import "+ GroovyPageParser.DEFAULT_IMPORTS[i]+"\n");
        }
        return result.toString();
    }
    
    private void configureKeepgen() {
		File tempdir=new File(System.getProperty("java.io.tmpdir"),"gspgen");
        tempdir.mkdir();
        ConfigObject config = new ConfigSlurper().parse("grails.views.gsp.keepgenerateddir = \"" + tempdir.getAbsolutePath() + "\"");
        ConfigurationHolder.setConfig( config);        
    }

    public void testParse() throws Exception {
		ParsedResult result = parseCode("myTest1", "<div>hi</div>");
		String expected = makeImports() +
            "\n"+
			"class myTest1 extends GroovyPage {\n"+
            "public String getGroovyPageFileName() { \"myTest1\" }\n"+
			"public Object run() {\n"+
            "def params = binding.params\n"+
            "def request = binding.request\n"+            
            "def flash = binding.flash\n"+
            "def response = binding.response\n"+
            "def out = binding.out\n"+
            "registerSitemeshPreprocessMode(request)\n"+

			"printHtmlPart(0)\n"+
			"}\n"+ GSP_FOOTER;
		assertEquals(trimAndRemoveCR(expected), trimAndRemoveCR(result.generatedGsp));
		assertEquals("<div>hi</div>", result.htmlParts[0]);
	}

    public void testParseWithUnclosedSquareBracket() throws Exception {
		String output = parseCode("myTest2", "<g:message code=\"[\"/>").generatedGsp;
		String expected = makeImports() +
			"\n"+
			"class myTest2 extends GroovyPage {\n"+
            "public String getGroovyPageFileName() { \"myTest2\" }\n"+
			"public Object run() {\n"+
            "def params = binding.params\n"+
            "def request = binding.request\n"+
            "def flash = binding.flash\n"+
            "def response = binding.response\n"+
            "def out = binding.out\n"+
            "registerSitemeshPreprocessMode(request)\n"+

            "invokeTag('message','g',1,['code':evaluate('\"[\"', 1, it) { return \"[\" }] as GroovyPageAttributes,null)\n"+
			"}\n" + GSP_FOOTER;

		assertEquals(trimAndRemoveCR(expected), trimAndRemoveCR(output));
	}

    public void testParseWithUnclosedGstringThrowsException() throws IOException {
        try{
            parseCode("myTest3", "<g:message value=\"${boom\">");
        }catch(GrailsTagException e){
            assertEquals("Unexpected end of file encountered parsing Tag [message] for myTest3. Are you missing a closing brace '}'? at myTest3:17", e.getMessage());
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
        ParsedResult output = null;
        try {
            output = parseCode("myTest4", src);
        }
        finally {
            ConfigurationHolder.setConfig(null);
        }
        String expected = makeImports() +
            "\n"+
            "class myTest4 extends GroovyPage {\n"+
            "public String getGroovyPageFileName() { \"myTest4\" }\n"+
            "public Object run() {\n"+
            "def params = binding.params\n"+
            "def request = binding.request\n"+
            "def flash = binding.flash\n"+
            "def response = binding.response\n"+
            "def out = binding.out\n"+
            "registerSitemeshPreprocessMode(request)\n"+
            "printHtmlPart(0)\n"+
            "}\n" + GSP_FOOTER;;
        assertEquals(trimAndRemoveCR(expected), trimAndRemoveCR(output.generatedGsp));
        assertEquals(src, output.htmlParts[0]);

    }

    public void testParseWithLocalEncoding() throws IOException {
        String src = "This is just plain ASCII to make sure test works on all platforms";
        // Sanity check the string loaded OK as unicode - it won't look right if you output it, default stdout is not UTF-8
        // on many OSes
        assertEquals(src.indexOf('?'), -1);


        ConfigObject config = new ConfigSlurper().parse("grails.views.gsp.encoding = \"\"");

        ConfigurationHolder.setConfig( config);
        ParsedResult output = null;
        try {
            output = parseCode("myTest5", src);
        }
        finally {
            ConfigurationHolder.setConfig(null);
        }
        String expected = makeImports() +
            "\n"+
            "class myTest5 extends GroovyPage {\n"+
            "public String getGroovyPageFileName() { \"myTest5\" }\n"+
            "public Object run() {\n"+

            "def params = binding.params\n"+
            "def request = binding.request\n"+
            "def flash = binding.flash\n"+
            "def response = binding.response\n"+
            "def out = binding.out\n"+
            "registerSitemeshPreprocessMode(request)\n"+
            "printHtmlPart(0)\n"+
            "}\n" + GSP_FOOTER;;
        assertEquals(trimAndRemoveCR(expected), trimAndRemoveCR(output.generatedGsp));
        assertEquals(src, output.htmlParts[0]);

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
	
	public ParsedResult parseCode(String uri, String gsp) throws IOException {
        // Simulate what the parser does so we get it in the encoding expected
        Object enc = ConfigurationHolder.getFlatConfig().get("grails.views.gsp.encoding");
        if ((enc == null) || (enc.toString().trim().length() == 0)) {
            enc = System.getProperty("file.encoding", "us-ascii");
        }

        InputStream gspIn = new ByteArrayInputStream(gsp.getBytes(enc.toString()));
        GroovyPageParser parse = new GroovyPageParser(uri, uri, uri, gspIn);
        InputStream in = parse.parse();
        ParsedResult result=new ParsedResult();
        result.parser=parse;
        result.generatedGsp = IOUtils.toString(in, enc.toString());
        result.htmlParts = parse.getHtmlPartsArray();
        return result;
	}

 	public void testParseGTagsWithNamespaces() throws Exception {
 		String output = parseCode("myTest6",
 		"<tbody>\n" +
 		"  <tt:form />\n" +
		"</tbody>").generatedGsp;
         System.out.println("output = " + output);
        assertTrue( "should have call to tag with 'tt' namespace", output.indexOf("invokeTag('form','tt',2,[:],null)") > -1);
 	}

    public void testParseWithWhitespaceNotEaten() throws Exception {
        String expected = makeImports() +
            "\n" +
            "class myTest7 extends GroovyPage {\n" +
            "public String getGroovyPageFileName() { \"myTest7\" }\n"+                
            "public Object run() {\n" +
            "def params = binding.params\n"+
            "def request = binding.request\n"+
            "def flash = binding.flash\n"+
            "def response = binding.response\n"+
            "def out = binding.out\n"+
            "registerSitemeshPreprocessMode(request)\n"+
            "printHtmlPart(0)\n" +
            "out.print(Codec.encode(evaluate('uri', 3, it) { return uri }))\n"+
            "printHtmlPart(1)\n" +
            "}\n" + GSP_FOOTER;


        ParsedResult output = parseCode("myTest7",
        "Please click the link below to confirm your email address:\n" +
        "\n" +
        "${uri}\n" +
        "\n"+
        "\n"+
        "Thanks");

        System.out.println("Output: "+output.generatedGsp);
        System.out.println("Expect: "+expected);
        assertEquals(expected, output.generatedGsp);
        assertEquals("Please click the link below to confirm your email address:\n\n", output.htmlParts[0]);
        assertEquals("\n\n\nThanks", output.htmlParts[1]);
        
    }

    public void testBodyWithGStringAttribute() throws Exception {
		ParsedResult result = parseCode("GRAILS5598", "<body class=\"${page.name} ${page.group.name.toLowerCase()}\">text</body>");
		String expected = makeImports() +
            "\n"+
			"class GRAILS5598 extends GroovyPage {\n"+
            "public String getGroovyPageFileName() { \"GRAILS5598\" }\n"+
			"public Object run() {\n"+
            "def params = binding.params\n"+
            "def request = binding.request\n"+            
            "def flash = binding.flash\n"+
            "def response = binding.response\n"+
            "def out = binding.out\n"+
            "registerSitemeshPreprocessMode(request)\n"+
            "body1 = createClosureForHtmlPart(0)\n"+
            "invokeTag('captureBody','sitemesh',1,['class':evaluate('\"${page.name} ${page.group.name.toLowerCase()}\"', 1, it) { return \"${page.name} ${page.group.name.toLowerCase()}\" }] as GroovyPageAttributes,body1)\n"+            
			"}\n"+ GSP_FOOTER;
		assertEquals(trimAndRemoveCR(expected), trimAndRemoveCR(result.generatedGsp));
		assertEquals("text", result.htmlParts[0]);
	}

    public void testMetaWithGStringAttribute() throws Exception {
		ParsedResult result = parseCode("GRAILS5605", "<html><head><meta name=\"SomeName\" content='${grailsApplication.config.myFirstConfig}/something/${someVar}' /></head></html>");
		String expected = makeImports() +
            "\n"+
			"class GRAILS5605 extends GroovyPage {\n"+
            "public String getGroovyPageFileName() { \"GRAILS5605\" }\n"+
			"public Object run() {\n"+
            "def params = binding.params\n"+
            "def request = binding.request\n"+            
            "def flash = binding.flash\n"+
            "def response = binding.response\n"+
            "def out = binding.out\n"+
            "registerSitemeshPreprocessMode(request)\n"+
            "printHtmlPart(0)\n"+
            "body1 = new GroovyPageTagBody(this,binding.webRequest) {\n"+
            "invokeTag('captureMeta','sitemesh',1,['gsp_sm_xmlClosingForEmptyTag':evaluate('\"/\"', 1, it) { return \"/\" },'name':evaluate('\"SomeName\"', 1, it) { return \"SomeName\" },'content':evaluate('\"${grailsApplication.config.myFirstConfig}/something/${someVar}\"', 1, it) { return \"${grailsApplication.config.myFirstConfig}/something/${someVar}\" }] as GroovyPageAttributes,null)\n"+
            "}\n"+            
            "invokeTag('captureHead','sitemesh',1,[:],body1)\n"+
            "printHtmlPart(1)\n"+
			"}\n"+ GSP_FOOTER;
		assertEquals(trimAndRemoveCR(expected), trimAndRemoveCR(result.generatedGsp));
	}
}
