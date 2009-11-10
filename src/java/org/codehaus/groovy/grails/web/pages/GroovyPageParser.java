/*
 * Copyright 2004-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.web.pages;

import grails.util.Environment;
import grails.util.PluginBuildSettings;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.*;
import org.codehaus.groovy.grails.plugins.GrailsPluginUtils;
import org.codehaus.groovy.grails.plugins.PluginInfo;
import org.codehaus.groovy.grails.web.taglib.GrailsTagRegistry;
import org.codehaus.groovy.grails.web.taglib.GroovySyntaxTag;
import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException;
import org.codehaus.groovy.grails.web.util.StreamByteBuffer;
import org.codehaus.groovy.grails.web.util.StreamCharBuffer;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NOTE: Based on work done by the GSP standalone project
 * (https://gsp.dev.java.net/)
 * 
 * Parsing implementation for GSP files
 * 
 * @author Troy Heninger
 * @author Graeme Rocher
 * @author Lari Hotari
 * 
 *         Date: Jan 10, 2004
 *         GSP precompilation (GRAILS-2890) added: May 19, 2009
 * 
 */
public class GroovyPageParser implements Tokens {
	public static final Log LOG = LogFactory.getLog(GroovyPageParser.class);

	private static final Pattern PARA_BREAK = Pattern.compile(
			"/p>\\s*<p[^>]*>", Pattern.CASE_INSENSITIVE);
	private static final Pattern ROW_BREAK = Pattern.compile(
			"((/td>\\s*</tr>\\s*<)?tr[^>]*>\\s*<)?td[^>]*>",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern PARSE_TAG_FIRST_PASS = Pattern
			.compile("(\\s*(\\S+)\\s*=\\s*[\"]([^\"]*)[\"][\\s|>]{1}){1}");
	private static final Pattern PARSE_TAG_SECOND_PASS = Pattern
			.compile("(\\s*(\\S+)\\s*=\\s*[']([^']*)['][\\s|>]{1}){1}");
	private static final Pattern PAGE_DIRECTIVE_PATTERN = Pattern
			.compile("(\\w+)\\s*=\\s*\"([^\"]*)\"");

	public static final String CONSTANT_NAME_JSP_TAGS = "JSP_TAGS";
	public static final String CONSTANT_NAME_CONTENT_TYPE = "CONTENT_TYPE";
	public static final String CONSTANT_NAME_LAST_MODIFIED = "LAST_MODIFIED";

	private GroovyPageScanner scan;
	private GSPWriter out;
	private String className;
	private String packageName;
	private boolean finalPass = false;
	private int tagIndex;
	private Map tagContext;
	private Stack<TagMeta> tagMetaStack = new Stack<TagMeta>();
	private GrailsTagRegistry tagRegistry = GrailsTagRegistry.getInstance();
	private Environment environment;
	private List<String> htmlParts = new ArrayList<String>();
	private static SitemeshPreprocessor sitemeshPreprocessor = new SitemeshPreprocessor();
	
	Set<Integer> bodyVarsDefined=new HashSet<Integer>();
	Map<Integer, String> attrsVarsMapDefinition=new HashMap<Integer, String>();

	int closureLevel=0;

	/*
	 * Set to true when whitespace is currently being saved for later output if
	 * the next tag isn't set to swallow it
	 */
	private boolean currentlyBufferingWhitespace;

	/*
	 * Set to true if the last output was not whitespace, so that we can detect
	 * when a tag has illegal content before it
	 */
	private boolean previousContentWasNonWhitespace;

	private StringBuffer whitespaceBuffer = new StringBuffer();

	private String contentType = DEFAULT_CONTENT_TYPE;
	private boolean doNextScan = true;
	private int state;
	private static final String DEFAULT_CONTENT_TYPE = "text/html;charset=UTF-8";
	private int constantCount = 0;
	private Map<String,Integer> constantsToNumbers = new HashMap<String,Integer>();

	private final String pageName;
	public static final String[] DEFAULT_IMPORTS = new String[] {
            "org.codehaus.groovy.grails.plugins.metadata.GrailsPlugin",
			"org.codehaus.groovy.grails.web.pages.GroovyPage",
			"org.codehaus.groovy.grails.web.taglib.*",
			"org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException",
			"org.springframework.web.util.*", "grails.util.GrailsUtil" };
	private static final String CONFIG_PROPERTY_DEFAULT_CODEC = "grails.views.default.codec";
	private static final String CONFIG_PROPERTY_GSP_ENCODING = "grails.views.gsp.encoding";
	private static final String CONFIG_PROPERTY_GSP_KEEPGENERATED_DIR = "grails.views.gsp.keepgenerateddir";
	private static final String CONFIG_PROPERTY_GSP_SITEMESH_PREPROCESS = "grails.views.gsp.sitemesh.preprocess";

	private String codecClassName;
	private String codecName;
	private static final String IMPORT_DIRECTIVE = "import";
	private static final String CONTENT_TYPE_DIRECTIVE = "contentType";
	private static final String DEFAULT_CODEC_DIRECTIVE = "defaultCodec";
	private static final String PAGE_DIRECTIVE = "page";

	private static final String TAGLIB_DIRECTIVE = "taglib";
	private String gspEncoding;
    private String pluginAnnotation;
	public static final String GROOVY_SOURCE_CHAR_ENCODING = "UTF-8";
	private Map jspTags = new HashMap();
	private long lastModified;
	private boolean precompileMode;
	private boolean sitemeshPreprocessMode=false;
    private PluginBuildSettings pluginBuildSettings = GrailsPluginUtils.getPluginBuildSettings();

    public String getContentType() {
		return this.contentType;
	}

	public int getCurrentOutputLineNumber() {
		return scan.getLineNumberForToken();
	}

	public Map getJspTags() {
		return jspTags;
	}

	class TagMeta {
		String name;
		String namespace;
		Object instance;
		boolean isDynamic;
		boolean hasAttributes;
		int lineNumber;
		boolean emptyTag;
		int tagIndex;
		boolean bufferMode=false;
		int bufferPartNumber=-1;

		public String toString() {
			return "<" + namespace + ":" + name + ">";
		}
	}

	public GroovyPageParser(String name, String uri, String filename, InputStream in)
			throws IOException {
		Map config = ConfigurationHolder.getFlatConfig();
        PluginInfo info = pluginBuildSettings.getPluginInfoForSource(filename);
        if(info!=null) {
            pluginAnnotation = "@GrailsPlugin(name='"+info.getName()+"', version='"+info.getVersion()+"')";
        }

        // Get the GSP file encoding from Config, or fall back to system
		// file.encoding if none set
		Object gspEnc = config.get(CONFIG_PROPERTY_GSP_ENCODING);
		if ((gspEnc != null) && (gspEnc.toString().trim().length() > 0)) {
			gspEncoding = gspEnc.toString();
		} else {
			gspEncoding = System.getProperty("file.encoding", "us-ascii");
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("GSP file encoding set to: " + gspEncoding);
		}

		String gspSource = readStream(in);

        if(isSitemeshPreprocessingEnabled(config, uri)) {
			if(LOG.isDebugEnabled()) {
				LOG.debug("Preprocessing " + uri + " for sitemesh. Replacing head, title, meta and body elements with g:capture*.");
			}
			// GSP preprocessing for direct sitemesh integration: replace head -> g:captureHead, title -> g:captureTitle, meta -> g:captureMeta, body -> g:captureBody
			gspSource = sitemeshPreprocessor.addGspSitemeshCapturing(gspSource);
			sitemeshPreprocessMode=true;
		}
		scan = new GroovyPageScanner(gspSource);
		this.pageName = uri;
		this.environment = Environment.getCurrent();
		makeName(name);
		Object o = config.get(CONFIG_PROPERTY_DEFAULT_CODEC);
		lookupCodec(o);

	} // Parse()

    private boolean isSitemeshPreprocessingEnabled(Map config, String filename) {
        Object sitemeshPreprocessEnabled = config.get(CONFIG_PROPERTY_GSP_SITEMESH_PREPROCESS);
        return /*!filename.contains("/layouts/") &&*/ (sitemeshPreprocessEnabled == null || (sitemeshPreprocessEnabled instanceof Boolean && ((Boolean) sitemeshPreprocessEnabled).booleanValue()));
    }

    private void lookupCodec(Object o) {
		if (o != null) {
			this.codecName = o.toString();
			GrailsApplication app = ApplicationHolder.getApplication();
			if (app != null) {
				GrailsClass codecClass = app.getArtefactByLogicalPropertyName(
						CodecArtefactHandler.TYPE, codecName);
				if (codecClass == null)
					codecClass = app.getArtefactByLogicalPropertyName(
							CodecArtefactHandler.TYPE, codecName.toUpperCase());
				if (codecClass != null) {
					this.codecClassName = codecClass.getFullName();
				}
			}
		}
	}

	public int[] getLineNumberMatrix() {
		return out.getLineNumbers();
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public InputStream parse() {
		File keepGeneratedDirectory = resolveKeepGeneratedDirectory();

		StreamCharBuffer streamBuffer = new StreamCharBuffer(1024);
		StreamByteBuffer byteOutputBuffer = new StreamByteBuffer(1024,
				StreamByteBuffer.ReadMode.RETAIN_AFTER_READING);

		try {
			streamBuffer.connectTo(new OutputStreamWriter(byteOutputBuffer
					.getOutputStream(), GROOVY_SOURCE_CHAR_ENCODING), true);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(
					"Grails cannot run unless your environment supports UTF-8!");
		}

		File keepGeneratedFile = null;
		Writer keepGeneratedWriter = null;
		if (keepGeneratedDirectory != null) {
			keepGeneratedFile = new File(keepGeneratedDirectory, className);
			try {
				keepGeneratedWriter = new OutputStreamWriter(
						new FileOutputStream(keepGeneratedFile),
						GROOVY_SOURCE_CHAR_ENCODING);
			} catch (IOException e) {
				LOG
						.warn("Cannot open keepgenerated file for writing. File's absolute path is '"
								+ keepGeneratedFile.getAbsolutePath() + "'");
				keepGeneratedFile = null;
			}
			streamBuffer.connectTo(keepGeneratedWriter, true);
		}

		Writer target = streamBuffer.getWriter();
		try {
			generateGsp(target, false);

			if (LOG.isDebugEnabled()) {
				if (keepGeneratedFile != null) {
					LOG.debug("Compiled GSP into Groovy code. Source is in "
							+ keepGeneratedFile);
				} else {
					LOG.debug("Configure "
							+ CONFIG_PROPERTY_GSP_KEEPGENERATED_DIR
							+ " property to view generated source.");
				}
			}
			InputStream in = byteOutputBuffer.getInputStream();
			return in;
		} finally {
			IOUtils.closeQuietly(keepGeneratedWriter);
		}
	}

	private File resolveKeepGeneratedDirectory() {
		File keepGeneratedDirectory = null;

		Object keepDirObj = ConfigurationHolder.getFlatConfig().get(
				CONFIG_PROPERTY_GSP_KEEPGENERATED_DIR);
		if (keepDirObj instanceof File) {
			keepGeneratedDirectory = ((File) keepDirObj);
		} else if (keepDirObj != null) {
			keepGeneratedDirectory = new File(String.valueOf(keepDirObj));
		}
		if (keepGeneratedDirectory != null
				&& !keepGeneratedDirectory.isDirectory()) {
			LOG
					.warn("The directory specified with "
							+ CONFIG_PROPERTY_GSP_KEEPGENERATED_DIR
							+ " config parameter doesn't exist or isn't a readable directory. Absolute path: '"
							+ keepGeneratedDirectory.getAbsolutePath()
							+ "' Keepgenerated will be disabled.");
			keepGeneratedDirectory = null;
		}
		return keepGeneratedDirectory;
	}

	public void generateGsp(Writer target) {
		generateGsp(target, true);
	}

	public void generateGsp(Writer target, boolean precompileMode) {
		this.precompileMode = precompileMode;

		out = new GSPWriter(target, this);
		if (packageName != null && packageName.length() > 0) {
			out.println("package " + packageName);
			out.println();
		}
		page();
		finalPass = true;
		scan.reset();
		previousContentWasNonWhitespace = false;
		currentlyBufferingWhitespace = false;
		page();

		out.flush();
		scan = null;
	}

	public void writeHtmlParts(File filename) throws IOException {
		DataOutputStream dataOut = null;
		try {
			dataOut = new DataOutputStream(new BufferedOutputStream(
					new FileOutputStream(filename)));
			dataOut.writeInt(htmlParts.size());
			for (Iterator<String> i = htmlParts.iterator(); i.hasNext();) {
				dataOut.writeUTF(i.next());
			}
		} finally {
			IOUtils.closeQuietly(dataOut);
		}
	}

	public void writeLineNumbers(File filename) throws IOException {
		DataOutputStream dataOut = null;
		try {
			dataOut = new DataOutputStream(new BufferedOutputStream(
					new FileOutputStream(filename)));
			int lineNumbersCount = this.out.getCurrentLineNumber() - 1;
			int[] lineNumbers = this.out.getLineNumbers();
			dataOut.writeInt(lineNumbersCount);
			for (int i = 0; i < lineNumbersCount; i++) {
				dataOut.writeInt(lineNumbers[i]);
			}
		} finally {
			IOUtils.closeQuietly(dataOut);
		}
	}

	private void declare(boolean gsp) {
		if (finalPass)
			return;
		if (LOG.isDebugEnabled())
			LOG.debug("parse: declare");
		out.println();
		write(scan.getToken().trim(), gsp);
		out.println();
		out.println();
	} // declare()

	private void direct() {
		if (finalPass)
			return;
		if (LOG.isDebugEnabled())
			LOG.debug("parse: direct");
		String text = scan.getToken();
		text = text.trim();
		if (text.startsWith(PAGE_DIRECTIVE))
			directPage(text);
		else if (text.startsWith(TAGLIB_DIRECTIVE))
			directJspTagLib(text);
	} // direct()

	private void directPage(String text) {

		text = text.trim();
		// LOG.debug("directPage(" + text + ')');
		Matcher mat = PAGE_DIRECTIVE_PATTERN.matcher(text);
		for (int ix = 0;;) {
			if (!mat.find(ix))
				return;
			String name = mat.group(1);
			String value = mat.group(2);
			if (name.equals(IMPORT_DIRECTIVE))
				pageImport(value);
			if (name.equals(CONTENT_TYPE_DIRECTIVE))
				contentType(value);
			if (name.equals(DEFAULT_CODEC_DIRECTIVE))
				lookupCodec(value);
			ix = mat.end();
		}
	} // directPage()

	private void directJspTagLib(String text) {

		text = text.substring(TAGLIB_DIRECTIVE.length() + 1, text.length());
		Map attrs = new HashMap();
		populateMapWithAttributes(attrs, text + '>');

		String prefix = (String) attrs.get("\"prefix\"");
		String uri = (String) attrs.get("\"uri\"");

		if (uri != null && prefix != null) {

			final String namespace = prefix.substring(1, prefix.length() - 1);
			if (!GroovyPage.DEFAULT_NAMESPACE.equals(namespace)) {
				jspTags.put(namespace, uri.substring(1, uri.length() - 1));
			} else {
				LOG
						.error("You cannot override the default 'g' namespace with the directive <%@ taglib prefix=\"g\" %>. Please select another namespace.");
			}
		}
	}

	private void contentType(String value) {
		this.contentType = value;
	}

	private void scriptletExpr() {
		if (!finalPass)
			return;
		if (LOG.isDebugEnabled())
			LOG.debug("parse: expr");

		String text = scan.getToken().trim();
		out.printlnToResponse(text);
	}

	private void expr() {
		if (!finalPass)
			return;
		if (LOG.isDebugEnabled())
			LOG.debug("parse: expr");

		String text = scan.getToken().trim();
		text = getExpressionText(text);
		if (environment == Environment.DEVELOPMENT) {

		}
		if (codecClassName != null) {
			out.printlnToResponse("Codec.encode(" + text + ")");
		} else {
			out.printlnToResponse(text);
		}
	} // expr()

	/**
	 * Returns an expression text for the given expression
	 * 
	 * @param text
	 *            The text
	 * @return An expression text
	 */
	public String getExpressionText(String text) {
        boolean safeDereference = false;
        if (text.endsWith("?")) {
            text = text.substring(0, text.length() - 1);
            safeDereference = true;
        }
        if (!precompileMode
				&& (environment == Environment.DEVELOPMENT || environment == Environment.TEST)) {
			String escaped = escapeGroovy(text);
			text = "evaluate('" + escaped + "', "
					+ getCurrentOutputLineNumber() + ", it) { return " + text
					+ " }" + (safeDereference ? "?" : "");
		} else {
			// add extra parenthesis, see http://jira.codehaus.org/browse/GRAILS-4351 
			// or GroovyPagesTemplateEngineTests.testForEachInProductionMode

			text = "(" + text + ")"+ (safeDereference ? "?" : "");
		}
		return text;
	}

	private String escapeGroovy(String text) {
		return text.replace("\\", "\\\\").replace("'", "\\'").replace("\n",
				"\\n").replace("\r", "\\r");
	}

	/**
	 * Write to the outputstream ONLY if the string is not blank, else we hold
	 * it back in case it is to be swallowed between tags
	 */
	private void bufferedPrintlnToResponse(String s) {
		if (currentlyBufferingWhitespace) {
			whitespaceBuffer.append(s);
		} else {
			flushTagBuffering();
			out.printlnToResponse(s);
		}
	}
	
	private void htmlPartPrintlnToResponse(int partNumber) {
		if(!tagMetaStack.isEmpty()) {
			TagMeta tm=tagMetaStack.peek();
			if(tm.bufferMode && tm.bufferPartNumber==-1) {
				tm.bufferPartNumber=partNumber;
				return;
			}
		}
		
		flushTagBuffering();
		
		htmlPartPrintlnRaw(partNumber);
	}

	private void htmlPartPrintlnRaw(int partNumber) {
		out.print("printHtmlPart(");
		out.print(String.valueOf(partNumber));
		out.print(")");
		out.println();
	}

	public void flushTagBuffering() {
		if(!tagMetaStack.isEmpty()) {
			TagMeta tm=tagMetaStack.peek();
			if(tm.bufferMode) {
				writeTagBodyStart(tm);
				if(tm.bufferPartNumber != -1) {
					htmlPartPrintlnRaw(tm.bufferPartNumber);
				}
				tm.bufferMode=false;
			}
		}
	}

	private void html() {
		if (!finalPass)
			return;
		if (LOG.isDebugEnabled())
			LOG.debug("parse: html");
		String text = scan.getToken();
		if (text.length() == 0)
			return;

		// If we detect it is all whitespace, we need to keep it for later
		// If it is not whitespace, we need to flush any whitespace we do have
		boolean contentIsWhitespace = !Pattern.compile("\\S").matcher(text)
				.find();
		if (!contentIsWhitespace && currentlyBufferingWhitespace) {
			flushBufferedWhiteSpace();
		} else {
			currentlyBufferingWhitespace = contentIsWhitespace;
		}
		// We need to know if the last content output was not whitespace, for
		// tag safety checks
		previousContentWasNonWhitespace = !contentIsWhitespace;

		if(currentlyBufferingWhitespace) {
			whitespaceBuffer.append(text);
		} else {
			appendHtmlPart(text);
		}
	} // html()

	private void appendHtmlPart(String text) {
		// flush previous white space if any
		if(whitespaceBuffer.length() > 0) {
			if(text != null) {
				whitespaceBuffer.append(text);
			}
			text=whitespaceBuffer.toString();
			clearBufferedWhiteSpace();
		}
		
		// de-dupe constants
		Integer constantNumber = constantsToNumbers.get(text);
		if (constantNumber == null) {
			constantNumber = new Integer(constantCount++);
			constantsToNumbers.put(text, constantNumber);
			htmlParts.add(text);
		}
		htmlPartPrintlnToResponse(constantNumber);
	}

	private void makeName(String uri) {
		String name;
		int slash = uri.lastIndexOf('/');
		if (slash > -1) {
			name = uri.substring(slash + 1);
			uri = uri.substring(0, (uri.length() - 1) - name.length());
			while (uri.endsWith("/")) {
				uri = uri.substring(0, uri.length() - 1);
			}
			slash = uri.lastIndexOf('/');
			if (slash > -1) {
				name = uri.substring(slash + 1) + '_' + name;
			}
		} else {
			name = uri;
		}
		StringBuffer buf = new StringBuffer(name.length());
		for (int ix = 0, ixz = name.length(); ix < ixz; ix++) {
			char c = name.charAt(ix);
			if (c < '0' || (c > '9' && c < '@') || (c > 'Z' && c < '_')
					|| (c > '_' && c < 'a') || c > 'z')
				c = '_';
			else if (ix == 0 && c >= '0' && c <= '9')
				c = '_';
			buf.append(c);
		}
		className = buf.toString();
	} // makeName()

	private static boolean match(CharSequence pat, CharSequence text, int start) {
		int ix = start, ixz = text.length(), ixy = start + pat.length();
		if (ixz > ixy)
			ixz = ixy;
		if (pat.length() > ixz - start)
			return false;
		for (; ix < ixz; ix++) {
			if (Character.toLowerCase(text.charAt(ix)) != Character
					.toLowerCase(pat.charAt(ix - start))) {
				return false;
			}
		}
		return true;
	} // match()

	private static int match(Pattern pat, CharSequence text, int start) {
		Matcher mat = pat.matcher(text);
		if (mat.find(start) && mat.start() == start) {
			return mat.end();
		}
		return 0;
	} // match()

	private void page() {
		if (LOG.isDebugEnabled())
			LOG.debug("parse: page");
		if (finalPass) {
			out.println();
            if(pluginAnnotation!=null) {
                out.println(pluginAnnotation);
            }
			out.print("class ");
			out.print(className);
			out.println(" extends GroovyPage {");

			out.println("public String getGroovyPageFileName() { \""
					+ pageName.replaceAll("\\\\", "/") + "\" }");
			out.println("public Object run() {");
			out.println("def params = binding.params");
			out.println("def request = binding.request");
			out.println("def flash = binding.flash");
			out.println("def response = binding.response");
			out.println("def out = binding.out");
			if(sitemeshPreprocessMode) {
				out.println("registerSitemeshPreprocessMode(request)");
			}
			if (codecClassName != null) {
				out
						.println("request.setAttribute('org.codehaus.groovy.grails.GSP_CODEC', '"
								+ codecName + "')");
			}

		}

		loop: for (;;) {
			if (doNextScan)
				state = scan.nextToken();
			else
				doNextScan = true;

			// Flush any buffered whitespace if there's not a possibility of
			// more whitespace
			// or a new tag which will handle flushing as necessary
			if ((state != GSTART_TAG) && (state != HTML)) {
				flushBufferedWhiteSpace();
				previousContentWasNonWhitespace = false; // well, we don't know
			}

			switch (state) {
			case EOF:
				break loop;
			case HTML:
				html();
				break;
			case JEXPR:
				scriptletExpr();
				break;
			case JSCRIPT:
				script(false);
				break;
			case JDIRECT:
				direct();
				break;
			case JDECLAR:
				declare(false);
				break;
			case GEXPR:
				expr();
				break;
			case GSCRIPT:
				script(true);
				break;
			case GDIRECT:
				direct();
				break;
			case GDECLAR:
				declare(true);
				break;
			case GSTART_TAG:
				startTag();
				break;
			case GEND_EMPTY_TAG:
			case GEND_TAG:
				endTag();
				break;
			}

		}

		if (finalPass) {
			if (!tagMetaStack.isEmpty()) {
				throw new GrailsTagException("Grails tags were not closed! ["
						+ tagMetaStack + "] in GSP " + pageName + "", pageName,
						out.getCurrentLineNumber());
			}

			out.println("}");

			out.println("public static final Map " + CONSTANT_NAME_JSP_TAGS
					+ " = new HashMap()");
			if (jspTags != null && jspTags.size() > 0) {
				out.println("static {");
				for (Iterator iterator = jspTags.entrySet().iterator(); iterator
						.hasNext();) {
					Map.Entry entry = (Map.Entry) iterator.next();
					out.print("\t" + CONSTANT_NAME_JSP_TAGS + ".put('");
					out.print(escapeGroovy(String.valueOf(entry.getKey())));
					out.print("','");
					out.print(escapeGroovy(String.valueOf(entry.getValue())));
					out.println("')");
				}
				out.println("}");
			}

			out.println("protected void init() {");
			out.println("\tthis.jspTags = " + CONSTANT_NAME_JSP_TAGS);
			out.println("}");

			out.println("public static final String "
					+ CONSTANT_NAME_CONTENT_TYPE + " = '"
					+ escapeGroovy(contentType) + "'");

			out.println("public static final long "
					+ CONSTANT_NAME_LAST_MODIFIED + " = " + lastModified + "L");

			out.println("}");
		} else {
			for (int i = 0; i < DEFAULT_IMPORTS.length; i++) {
				out.print("import ");
				out.println(DEFAULT_IMPORTS[i]);

			}
			if (codecClassName != null) {
				out.print("import ");
				out.print(codecClassName);
				out.println(" as Codec");
			}
		}
	} // page()

	private void endTag() {
		if (!finalPass)
			return;

		String tagName = scan.getToken().trim();
		String ns = scan.getNamespace();

		if (tagMetaStack.isEmpty())
			throw new GrailsTagException(
					"Found closing Grails tag with no opening [" + tagName
							+ "]", pageName,
							out.getCurrentLineNumber());

		TagMeta tm = (TagMeta) tagMetaStack.pop();
		String lastInStack = tm.name;
		String lastNamespaceInStack = tm.namespace;

		// if the tag name is blank then it has been closed by the start tag ie
		// <tag />
		if (StringUtils.isBlank(tagName))
			tagName = lastInStack;

		if (!lastInStack.equals(tagName) || !lastNamespaceInStack.equals(ns)) {
			throw new GrailsTagException("Grails tag [" + lastNamespaceInStack
					+ ":" + lastInStack + "] was not closed", pageName,
					out.getCurrentLineNumber());
		}

		if (GroovyPage.DEFAULT_NAMESPACE.equals(ns)
				&& tagRegistry.isSyntaxTag(tagName)) {
			if (tm.instance instanceof GroovySyntaxTag) {
				GroovySyntaxTag tag = (GroovySyntaxTag) tm.instance;
				tag.doEndTag();
			} else {
				throw new GrailsTagException("Grails tag [" + tagName
						+ "] was not closed", pageName,
						out.getCurrentLineNumber());
			}
		} else {
			String bodyTagClosureName = "null";
			if (!tm.emptyTag && !tm.bufferMode) {
				bodyTagClosureName = "body" + tagIndex;
				out.println("}");
				closureLevel--;
			}
			
			if(tm.bufferMode && tm.bufferPartNumber != -1){
				if(!bodyVarsDefined.contains(tm.tagIndex)) {
					//out.print("def ");
					bodyVarsDefined.add(tm.tagIndex);
				}
				out
						.println("body"
								+ tm.tagIndex
								+ " = createClosureForHtmlPart(" + tm.bufferPartNumber + ")");
				bodyTagClosureName = "body" + tm.tagIndex;
				tm.bufferMode = false;
			}

			if (jspTags.containsKey(ns)) {
				String uri = (String) jspTags.get(ns);
				out.println("jspTag = tagLibraryResolver?.resolveTagLibrary('"
						+ uri + "')?.getTag('" + tagName + "')");
				out
						.println("if(!jspTag) throw new GrailsTagException('Unknown JSP tag "
								+ ns + ":" + tagName + "')");
				out.println("jspTag.doTag(out," + attrsVarsMapDefinition.get(tagIndex) + ", "
						+ bodyTagClosureName + ")");
			} else {
				if (tm.hasAttributes) {
					out.println("invokeTag('" + tagName + "','" + ns + "',"
							+ getCurrentOutputLineNumber() + "," + attrsVarsMapDefinition.get(tagIndex) +
							"," + bodyTagClosureName + ")");
				} else {
					out.println("invokeTag('" + tagName + "','" + ns + "',"
							+ getCurrentOutputLineNumber() + ",[:],"
							+ bodyTagClosureName + ")");
				}
			}
		}
		
		tm.bufferMode = false;
		tagIndex--;
	}

	private void startTag() {
		if (!finalPass)
			return;
		tagIndex++;

		String text;
		StringBuffer buf = new StringBuffer(scan.getToken());
		String ns = scan.getNamespace();

		boolean emptyTag = false;

		state = scan.nextToken();
		while (state != HTML && state != GEND_TAG && state != GEND_EMPTY_TAG
				&& state != EOF) {
			if (state == GTAG_EXPR) {
				buf.append("${");
				buf.append(scan.getToken().trim());
				buf.append("}");
			} else {
				buf.append(scan.getToken());
			}
			state = scan.nextToken();
		}
		if (state == GEND_EMPTY_TAG) {
			emptyTag = true;
		}

		doNextScan = false;

		text = buf.toString();

		String tagName;
		Map attrs = new TreeMap();
		text = text.replaceAll("[\r\n\t]", " "); // this line added TODO query
													// this

		if (text.indexOf(' ') > -1) { // ignores carriage returns and new lines
			int i = text.indexOf(' ');
			tagName = text.substring(0, i);
			String attrTokens = text.substring(i, text.length());
			attrTokens += '>'; // closing bracket marker
			populateMapWithAttributes(attrs, attrTokens);
		} else {
			tagName = text;
		}

		if (state == EOF) {
			throw new GrailsTagException(
					"Unexpected end of file encountered parsing Tag ["
							+ tagName + "] for " + className
							+ ". Are you missing a closing brace '}'?", pageName,
							out.getCurrentLineNumber());
		}
		
		flushTagBuffering();

		TagMeta tm = new TagMeta();
		tm.name = tagName;
		tm.namespace = ns;
		tm.hasAttributes = !attrs.isEmpty();
		tm.lineNumber = getCurrentOutputLineNumber();
		tm.emptyTag = emptyTag;
		tm.tagIndex = tagIndex;
		tagMetaStack.push(tm);

		if (GroovyPage.DEFAULT_NAMESPACE.equals(ns)
				&& tagRegistry.isSyntaxTag(tagName)) {
			if (this.tagContext == null) {
				this.tagContext = new HashMap();
				this.tagContext.put(GroovyPage.OUT, out);
				this.tagContext.put(GroovyPageParser.class, this);
			}
			GroovySyntaxTag tag = (GroovySyntaxTag) tagRegistry.newTag(tagName);
			tag.init(tagContext);
			tag.setAttributes(attrs);

			if (tag.isKeepPrecedingWhiteSpace() && currentlyBufferingWhitespace) {
				flushBufferedWhiteSpace();
			} else if (!tag.isAllowPrecedingContent()
					&& previousContentWasNonWhitespace) {
				throw new GrailsTagException(
						"Tag ["
								+ tag.getName()
								+ "] cannot have non-whitespace characters directly preceding it.", pageName,
								out.getCurrentLineNumber());
			} else {
				// If tag does not specify buffering of WS, we swallow it here
				clearBufferedWhiteSpace();
			}

			tag.doStartTag();
			tm.instance = tag;
		} else {
			// Custom taglibs have to always flush the whitespace, there's no
			// "allowPrecedingWhitespace" property on tags yet
			flushBufferedWhiteSpace();
			
			if (attrs.size() > 0) {
				FastStringWriter buffer=new FastStringWriter();
				buffer.print('[');
				for (Iterator i = attrs.keySet().iterator(); i.hasNext();) {
					String name = (String) i.next();
					String cleanedName=name;
					if(name.startsWith("\"") && name.endsWith("\"")) {
						cleanedName="'" + name.substring(1,name.length()-1) + "'";
					}
					buffer.print(cleanedName);
					buffer.print(':');

					buffer.print(getExpressionText(attrs.get(name).toString()));
					if (i.hasNext())
						buffer.print(',');
					else
						buffer.print(']');
				}
				attrsVarsMapDefinition.put(tagIndex, buffer.toString());
			}
			
			if(!emptyTag) {
				tm.bufferMode = true;
			}

		}
	}

	private void writeTagBodyStart(TagMeta tm) {
		if (tm.bufferMode) {
			tm.bufferMode = false;			
			if(!bodyVarsDefined.contains(tm.tagIndex)) {
				//out.print("def ");
				bodyVarsDefined.add(tm.tagIndex);
			}
			out
					.println("body"
							+ tm.tagIndex
							+ " = new GroovyPageTagBody(this,binding.webRequest) {");
			closureLevel++;
		}
	}

	private void clearBufferedWhiteSpace() {
		whitespaceBuffer.delete(0, whitespaceBuffer.length());
		currentlyBufferingWhitespace = false;
	}

	// Write out any whitespace we saved between tags
	private void flushBufferedWhiteSpace() {
		if (currentlyBufferingWhitespace) {
			appendHtmlPart(null);
		}
		currentlyBufferingWhitespace = false;
	}

	private void populateMapWithAttributes(Map attrs, String attrTokens) {
		// do first pass parse which retrieves double quoted attributes
		Matcher m = PARSE_TAG_FIRST_PASS.matcher(attrTokens);
		populateAttributesFromMatcher(m, attrs);

		// do second pass parse which retrieves single quoted attributes
		m = PARSE_TAG_SECOND_PASS.matcher(attrTokens);
		populateAttributesFromMatcher(m, attrs);
	}

	private void populateAttributesFromMatcher(Matcher m, Map attrs) {
		while (m.find()) {
			String name = m.group(2);
			String val = m.group(3);
			name = '\"' + name + '\"';
			if (val.startsWith("${") && val.endsWith("}")) {
				val = val.substring(2, val.length() - 1);
			} else if (!(val.startsWith("[") && val.endsWith("]"))) {
				val = '\"' + val + '\"';
			}
			attrs.put(name, val);
		}
	}

	private void pageImport(String value) {
		// LOG.debug("pageImport(" + value + ')');
		String[] imports = Pattern.compile(";").split(
				value.subSequence(0, value.length()));
		for (int ix = 0; ix < imports.length; ix++) {
			out.print("import ");
			out.print(imports[ix]);
			out.println();
		}
	} // pageImport()

	private String readStream(InputStream in) throws IOException {
		return IOUtils.toString(in, gspEncoding);
	}

	private void script(boolean gsp) {
		flushTagBuffering();
		if (!finalPass)
			return;
		if (LOG.isDebugEnabled())
			LOG.debug("parse: script");
		out.println();
		write(scan.getToken().trim(), gsp);
		out.println();
		out.println();
	} // script()

	private void write(CharSequence text, boolean gsp) {
		if (!gsp) {
			out.print(text);
			return;
		}
		for (int ix = 0, ixz = text.length(); ix < ixz; ix++) {
			char c = text.charAt(ix);
			String rep = null;
			if (Character.isWhitespace(c)) {
				for (ix++; ix < ixz; ix++) {
					if (Character.isWhitespace(text.charAt(ix)))
						continue;
					ix--;
					rep = " ";
					break;
				}
			} else if (c == '&') {
				if (match("&semi;", text, ix)) {
					rep = ";";
					ix += 5;
				} else if (match("&amp;", text, ix)) {
					rep = "&";
					ix += 4;
				} else if (match("&lt;", text, ix)) {
					rep = "<";
					ix += 3;
				} else if (match("&gt;", text, ix)) {
					rep = ">";
					ix += 3;
				}
			} else if (c == '<') {
				if (match("<br>", text, ix) || match("<hr>", text, ix)) {
					rep = "\n";
					//incrementLineNumber();
					ix += 3;
				} else {
					int end = match(PARA_BREAK, text, ix);
					if (end <= 0)
						end = match(ROW_BREAK, text, ix);
					if (end > 0) {
						rep = "\n";
						//incrementLineNumber();
						ix = end;
					}
				}
			}
			if (rep != null)
				out.print(rep);
			else
				out.print(c);
		}
	} // write()

	public long getLastModified() {
		return lastModified;
	}

	public void setLastModified(long lastModified) {
		this.lastModified = lastModified;
	}

	public List<String> getHtmlParts() {
		return htmlParts;
	}

	public String[] getHtmlPartsArray() {
		return htmlParts.toArray(new String[htmlParts.size()]);
	}

	public boolean isInClosure() {
		return closureLevel > 0;
	}
} // Parse
