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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.*;
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
 * NOTE: Based on work done by the GSP standalone project (https://gsp.dev.java.net/)
 *
 * Parsing implementation for GSP files
 *
 * @author Troy Heninger
 * @author Graeme Rocher
 *
 * Date: Jan 10, 2004
 *
 */
public class GroovyPageParser implements Tokens {
    public static final Log LOG = LogFactory.getLog(GroovyPageParser.class);

    private static final Pattern PARA_BREAK = Pattern.compile("/p>\\s*<p[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ROW_BREAK = Pattern.compile("((/td>\\s*</tr>\\s*<)?tr[^>]*>\\s*<)?td[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern PARSE_TAG_FIRST_PASS = Pattern.compile("(\\s*(\\S+)\\s*=\\s*[\"]([^\"]*)[\"][\\s|>]{1}){1}");
    private static final Pattern PARSE_TAG_SECOND_PASS = Pattern.compile("(\\s*(\\S+)\\s*=\\s*[']([^']*)['][\\s|>]{1}){1}");
    private static final Pattern PAGE_DIRECTIVE_PATTERN = Pattern.compile("(\\w+)\\s*=\\s*\"([^\"]*)\"");

    private GroovyPageScanner scan;
    private GSPWriter out;
    private String className;
    private boolean finalPass = false;
    private int tagIndex;
    private Map tagContext;
    private List tagMetaStack = new ArrayList();
    private GrailsTagRegistry tagRegistry = GrailsTagRegistry.getInstance();
    private Environment environment;

    /* Set to true when whitespace is currently being saved for later output if the next tag isn't set to swallow it */
    private boolean currentlyBufferingWhitespace;

    /* Set to true if the last output was not whitespace, so that we can detect when a tag has illegal content before it */
    private boolean previousContentWasNonWhitespace;

    private StringBuffer whitespaceBuffer = new StringBuffer();

    private int currentOutputLine = 1;
    private String contentType = DEFAULT_CONTENT_TYPE;
    private boolean doNextScan = true;
    private int state;
    private static final String START_MULTILINE_STRING = "'''";
    private static final String END_MULTILINE_STRING = "'''";
    private static final String DEFAULT_CONTENT_TYPE = "text/html;charset=UTF-8";
    private Map constants = new TreeMap();
    private int constantCount = 0;
    private Map constantsToNames = new HashMap();

    private final String pageName;
    private static final String EMPTY_MULTILINE_STRING = "''''''";
    public static final String[] DEFAULT_IMPORTS = new String[] {
        "org.codehaus.groovy.grails.web.pages.GroovyPage",
        "org.codehaus.groovy.grails.web.taglib.*",
        "org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException",
        "org.springframework.web.util.*",
        "grails.util.GrailsUtil"
    };
    private static final String CONFIG_PROPERTY_DEFAULT_CODEC = "grails.views.default.codec";
    private static final String CONFIG_PROPERTY_GSP_ENCODING = "grails.views.gsp.encoding";
    private static final String CONFIG_PROPERTY_GSP_KEEPGENERATED_DIR = "grails.views.gsp.keepgenerateddir";

    private String codecClassName;
    private String codecName;
    private static final String IMPORT_DIRECTIVE = "import";
    private static final String CONTENT_TYPE_DIRECTIVE = "contentType";
    private static final String DEFAULT_CODEC_DIRECTIVE = "defaultCodec";
    private static final String PAGE_DIRECTIVE = "page";

    private static final String TAGLIB_DIRECTIVE = "taglib";
    private String gspEncoding;
    public static final String GROOVY_SOURCE_CHAR_ENCODING = "UTF-8";
    private Map jspTags = new HashMap();
    
    private File keepGeneratedDirectory;

    public String getContentType() {
        return this.contentType;
    }

    public int getCurrentOutputLineNumber() {
        return currentOutputLine;
    }

    public Map getJspTags() {
        return jspTags;
    }

    class TagMeta  {
        String name;
        String namespace;
        Object instance;
        boolean isDynamic;
        boolean hasAttributes;
        int lineNumber;

        public String toString() {
            return "<"+namespace+":"+name+">";
        }
    }

    public GroovyPageParser(String name, String filename, InputStream in) throws IOException {
        Map config = ConfigurationHolder.getFlatConfig();

        // Get the GSP file encoding from Config, or fall back to system file.encoding if none set
        Object gspEnc = config.get(CONFIG_PROPERTY_GSP_ENCODING);
        if ((gspEnc != null) && (gspEnc.toString().trim().length() > 0)) {
            gspEncoding = gspEnc.toString();
        } else {
            gspEncoding = System.getProperty("file.encoding", "us-ascii");
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("GSP file encoding set to: " + gspEncoding);
        }
        
        scan = new GroovyPageScanner(readStream(in));
        this.pageName = filename;
        this.environment = Environment.getCurrent();
        makeName(name);
        Object o = config.get(CONFIG_PROPERTY_DEFAULT_CODEC);
        lookupCodec(o);
        
        Object keepDirObj = config.get(CONFIG_PROPERTY_GSP_KEEPGENERATED_DIR);
        if(keepDirObj instanceof File) {
        	keepGeneratedDirectory=((File)keepDirObj);
        } else if (keepDirObj != null) {
        	keepGeneratedDirectory=new File(String.valueOf(keepDirObj));
        }
        if(keepGeneratedDirectory != null && !keepGeneratedDirectory.isDirectory()) {
        	LOG.warn("The directory specified with " + CONFIG_PROPERTY_GSP_KEEPGENERATED_DIR + " config parameter doesn't exist or isn't a readable directory. Absolute path: '" + keepGeneratedDirectory.getAbsolutePath() + "' Keepgenerated will be disabled.");
        	keepGeneratedDirectory=null;
        }
    } // Parse()

    private void lookupCodec(Object o) {
        if(o!=null) {
            this.codecName = o.toString();
            GrailsApplication app = ApplicationHolder.getApplication();
            if(app != null) {
                GrailsClass codecClass = app.getArtefactByLogicalPropertyName(CodecArtefactHandler.TYPE, codecName);
                if(codecClass == null) codecClass = app.getArtefactByLogicalPropertyName(CodecArtefactHandler.TYPE, codecName.toUpperCase());
                if(codecClass != null) {
                    this.codecClassName = codecClass.getFullName();
                }
            }
        }
    }

    public int[] getLineNumberMatrix() {
        return out.getLineNumbers();
    }
    
    public InputStream parse() {
    	StreamCharBuffer streamBuffer=new StreamCharBuffer(1024);
    	StreamByteBuffer byteOutputBuffer=new StreamByteBuffer(1024, StreamByteBuffer.ReadMode.RETAIN_AFTER_READING);
    	
    	try {
    		streamBuffer.connectTo(new OutputStreamWriter(byteOutputBuffer.getOutputStream(), GROOVY_SOURCE_CHAR_ENCODING), true);
	    } catch (UnsupportedEncodingException e) {
	        throw new RuntimeException("Grails cannot run unless your environment supports UTF-8!");
	    }
	    
	    File keepGeneratedFile = null;
    	Writer keepGeneratedWriter = null;
    	if(keepGeneratedDirectory != null) {
    		keepGeneratedFile = new File(keepGeneratedDirectory, className);
	    	try {
	    		keepGeneratedWriter = new OutputStreamWriter(new FileOutputStream(keepGeneratedFile), GROOVY_SOURCE_CHAR_ENCODING);
	    	} catch (IOException e) {
	    		LOG.warn("Cannot open keepgenerated file for writing. File's absolute path is '" + keepGeneratedFile.getAbsolutePath() + "'");
	    		keepGeneratedFile=null;
	    	}
	    	streamBuffer.connectTo(keepGeneratedWriter, true);
	    }

    	try {
		    out = new GSPWriter(streamBuffer.getWriter(),this);
	        page();
	        finalPass = true;
	        scan.reset();
	        previousContentWasNonWhitespace = false;
	        currentlyBufferingWhitespace = false;
	        page();
	        
	        out.flush();	
	        
	        if(LOG.isDebugEnabled()) {
	        	if(keepGeneratedFile != null) {
	        		LOG.debug("Compiled GSP into Groovy code. Source is in " + keepGeneratedFile);
	        	}  else {
	        		LOG.debug("Configure " + CONFIG_PROPERTY_GSP_KEEPGENERATED_DIR + " property to view generated source.");
	        	}
	        }
	        scan = null;
	        InputStream in = byteOutputBuffer.getInputStream();
	        return in;
    	} finally {
    		IOUtils.closeQuietly(keepGeneratedWriter);
    	}
    } 


    private void declare(boolean gsp) {
        if (finalPass) return;
        if (LOG.isDebugEnabled()) LOG.debug("parse: declare");
        out.println();
        write(scan.getToken().trim(), gsp);
        out.println();
        out.println();
    } // declare()

    private void direct() {
        if (finalPass) return;
        if (LOG.isDebugEnabled()) LOG.debug("parse: direct");
        String text = scan.getToken();
        text = text.trim();
        if(text.startsWith(PAGE_DIRECTIVE))
            directPage(text);
        else if(text.startsWith(TAGLIB_DIRECTIVE))
            directJspTagLib(text);
    } // direct()


    private void directPage(String text) {

        text = text.trim();
//		LOG.debug("directPage(" + text + ')');
        Matcher mat = PAGE_DIRECTIVE_PATTERN.matcher(text);
        for (int ix = 0;;) {
            if (!mat.find(ix)) return;
            String name = mat.group(1);
            String value = mat.group(2);
            if (name.equals(IMPORT_DIRECTIVE)) pageImport(value);
            if (name.equals(CONTENT_TYPE_DIRECTIVE)) contentType(value);
            if (name.equals(DEFAULT_CODEC_DIRECTIVE)) lookupCodec(value);
            ix = mat.end();
        }
    } // directPage()

    private void directJspTagLib(String text) {

        text = text.substring(TAGLIB_DIRECTIVE.length()+1,text.length());
        Map attrs = new HashMap();
        populateMapWithAttributes(attrs, text+'>');

        String prefix = (String) attrs.get("\"prefix\"");
        String uri = (String) attrs.get("\"uri\"");

        if(uri!= null && prefix !=null) {

            final String namespace = prefix.substring(1, prefix.length() - 1);
            if(!GroovyPage.DEFAULT_NAMESPACE.equals(namespace)) {
                jspTags.put(namespace, uri.substring(1,uri.length()-1));
            }
            else {
                LOG.error("You cannot override the default 'g' namespace with the directive <%@ taglib prefix=\"g\" %>. Please select another namespace.");
            }
        }
    }

    private void contentType(String value) {
        this.contentType = value;
    }

    private void scriptletExpr() {
        if (!finalPass) return;
        if (LOG.isDebugEnabled()) LOG.debug("parse: expr");

        String text = scan.getToken().trim();
        out.printlnToResponse(text);
    }

    private void expr() {
        if (!finalPass) return;
        if (LOG.isDebugEnabled()) LOG.debug("parse: expr");

        String text = scan.getToken().trim();
        text = getExpressionText(text);
        if(environment == Environment.DEVELOPMENT) {
            
        }
        if(codecClassName != null) {
            out.printlnToResponse("Codec.encode("+text+")");            
        }
        else {
            out.printlnToResponse(text);
        }
    } // expr()

    /**
     * Returns an expression text for the given expression
     *
     * @param text The text
     * @return An expression text
     */
    public String getExpressionText(String text) {
        if(environment == Environment.DEVELOPMENT || environment == Environment.TEST) {
            boolean safeDereference = false;
            if(text.endsWith("?")) {
                text = text.substring(0, text.length()-1);
                safeDereference = true;
            }
            String escaped = escapeGroovy(text);
            text = "evaluate('"+ escaped +"', "+getCurrentOutputLineNumber()+", it) { return "+text+" }" + (safeDereference ? "?" : "");
        }
        return text;
    }

    private String escapeGroovy(String text) {
        return text.replace("\\","\\\\").replace("'", "\\'").replace("\n","\\n").replace("\r", "\\r");
    }

    /**
     * Split the input text on new lines, but keeping blank entries for blank lines unlike String.split
     * @param text
     * @return
     */
    private String[] splitLinesKeepingBlanks(String text) {
        ArrayList results = new ArrayList();
        final int len = text.length();
        int pos = 0;
        while (pos < len) {
            int EOLpos = text.indexOf('\n', pos);
            if (EOLpos >= 0) {
                results.add( text.substring(pos, EOLpos));
                pos = EOLpos+1;
            } else {
                results.add( text.substring(pos, len));
                pos = len;
            }
        }
        return (String[])results.toArray(new String[results.size()]);
    }

    /**
     * Write to the outputstream ONLY if the string is not blank, else we hold it back
     * in case it is to be swallowed between tags
     */
    private void bufferedPrintlnToResponse(String s) {
        if (currentlyBufferingWhitespace) {
            whitespaceBuffer.append(s);
        } else {
            out.printlnToResponse(s);
        }
    }

    private void html() {
        if (!finalPass) return;
        if (LOG.isDebugEnabled()) LOG.debug("parse: html");
        String text = scan.getToken();
        if (text.length() == 0) return;
        
        // If we detect it is all whitespace, we need to keep it for later
        // If it is not whitespace, we need to flush any whitespace we do have
        boolean contentIsWhitespace = !Pattern.compile("\\S").matcher(text).find();
        if (!contentIsWhitespace && currentlyBufferingWhitespace) {
            flushBufferedWhiteSpace();
        } else {
            currentlyBufferingWhitespace = contentIsWhitespace;
        }
        // We need to know if the last content output was not whitespace, for tag safety checks
        previousContentWasNonWhitespace = !contentIsWhitespace;

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        String[] lines = splitLinesKeepingBlanks(text);

        if (lines.length == 0) return;

        // Single lines are output directly
        if(lines.length == 1) {
            if (lines[0].length() == 0) lines[0] = "\n";
            bufferedPrintlnToResponse('\'' + escapeGroovy((CharSequence) lines[0]) + '\'');
        }
        else {
            // Multiple lines are bunched up into a multiline string constant
            pw.print(START_MULTILINE_STRING);
            String prevcontent = null;
            boolean firstLine = true;
            for (String line : lines) {
                // Preserve pure blank lines as \n to avoid problems with
                // final blank line being on same line as closing multi-line quote
                // losing us a blank line
                if (line.length() == 0) {
                    line = "\n";
                }
                final String content = escapeGroovy((CharSequence) line);
                if (firstLine) {
                    pw.print(content);
                    firstLine = false;
                }
                else {
                    // Don't put printlns in if the line is blank i.e. \\n else they double up!
                    if (!"\\n".equals(prevcontent)) {
                        incrementLineNumber();
                        pw.println();
                    }
                    pw.print(content);
                }
                prevcontent = content;


            }
            pw.print(END_MULTILINE_STRING);
            pw.println();
            
            // Write it out as a println of a STATIC and cache the string for later rendering
            final String constantValue = sw.toString();

            // de-dupe constants
            String constantName = (String)constantsToNames.get(constantValue);
            if (constantName == null) {
                constantName = "STATIC_HTML_CONTENT_" + constantCount++;
                constants.put(constantName, constantValue);
                constantsToNames.put( constantValue, constantName);
            }
            bufferedPrintlnToResponse(constantName);
        }

    } // html()

    private void makeName(String uri) {
        String name;
        int slash = uri.lastIndexOf('/');
        if (slash > -1) {
            name = uri.substring(slash + 1);
            uri = uri.substring(0,(uri.length() - 1) - name.length());
            while(uri.endsWith("/")) {
                uri = uri.substring(0,uri.length() -1);
            }
            slash = uri.lastIndexOf('/');
            if(slash > -1) {
                    name = uri.substring(slash + 1) + '_' + name;
            }
        }
        else {
            name = uri;
        }
        StringBuffer buf = new StringBuffer(name.length());
        for (int ix = 0, ixz = name.length(); ix < ixz; ix++) {
            char c = name.charAt(ix);
            if (c < '0' || (c > '9' && c < '@') || (c > 'Z' && c < '_') || (c > '_' && c < 'a') || c > 'z') c = '_';
            else if (ix == 0 && c >= '0' && c <= '9') c = '_';
            buf.append(c);
        }
        className = buf.toString();
    } // makeName()

    private static boolean match(CharSequence pat, CharSequence text, int start) {
        int ix = start, ixz = text.length(), ixy = start + pat.length();
        if (ixz > ixy) ixz = ixy;
        if (pat.length() > ixz - start) return false;
        for (; ix < ixz; ix++) {
            if (Character.toLowerCase(text.charAt(ix)) != Character.toLowerCase(pat.charAt(ix - start))) {
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
        if (LOG.isDebugEnabled()) LOG.debug("parse: page");
        if (finalPass) {
            out.println();
            out.print("class ");
            out.print(className);
            out.println(" extends GroovyPage {");
            
            out.println("public String getGroovyPageFileName() { \""+ pageName.replaceAll("\\\\","/") +"\" }");
            out.println("public Object run() {");
            out.println("def params = binding.params");
            out.println("def request = binding.request");
            out.println("def flash = binding.flash");
            out.println("def response = binding.response");
            if(codecClassName !=null) {
                out.println("request.setAttribute('org.codehaus.groovy.grails.GSP_CODEC', '"+ codecName +"')");
            }

        }

        loop: for (;;) {
            if(doNextScan)
                state = scan.nextToken();
            else
                doNextScan = true;

            // Flush any buffered whitespace if there's not a possibility of more whitespace
            // or a new tag which will handle flushing as necessary
            if ((state != GSTART_TAG) && (state != HTML)) {
                flushBufferedWhiteSpace();
                previousContentWasNonWhitespace = false; // well, we don't know
            }

            switch (state) {
                case EOF: break loop;
                case HTML: html(); break;
                case JEXPR: scriptletExpr(); break;
                case JSCRIPT: script(false); break;
                case JDIRECT: direct(); break;
                case JDECLAR: declare(false); break;
                case GEXPR: expr(); break;
                case GSCRIPT: script(true); break;
                case GDIRECT: direct(); break;
                case GDECLAR: declare(true); break;
                case GSTART_TAG: startTag(); break;
                case GEND_TAG: endTag(); break;
            }
        }

        if (finalPass) {
            if(!tagMetaStack.isEmpty()) {
                TagMeta tag = (TagMeta)tagMetaStack.iterator().next();
                throw new GrailsTagException("Grails tags were not closed! ["+tagMetaStack+"] in GSP "+pageName+"", pageName, out.getCurrentLineNumber());
            }

            out.println("}");
            for (Iterator i = constants.keySet().iterator(); i.hasNext();) {
                String name = (String) i.next();
                out.println("static final " + name + " = " + constants.get(name));
            }
            out.println("}");
        }
        else {
            for (int i = 0; i < DEFAULT_IMPORTS.length; i++) {
                out.print("import ");
                out.println(DEFAULT_IMPORTS[i]);

            }
            if(codecClassName != null) {
                out.print("import ");
                out.print(codecClassName);
                out.println(" as Codec");
            }            
        }
    } // page()


    private void endTag() {
        if (!finalPass) return;

       String tagName = scan.getToken().trim();
       String ns = scan.getNamespace();
       
       if(tagMetaStack.isEmpty())
             throw new GrailsTagException("Found closing Grails tag with no opening ["+tagName+"]");

       TagMeta tm = (TagMeta)tagMetaStack.remove(this.tagMetaStack.size() - 1);
       String lastInStack = tm.name;
       String lastNamespaceInStack = tm.namespace;

       // if the tag name is blank then it has been closed by the start tag ie <tag />
       if(StringUtils.isBlank(tagName))
               tagName = lastInStack;

       if(!lastInStack.equals(tagName) ||
    	  !lastNamespaceInStack.equals(ns)) {
           throw new GrailsTagException("Grails tag ["+lastNamespaceInStack+":"+lastInStack+"] was not closed");
       }

       if(GroovyPage.DEFAULT_NAMESPACE.equals(ns) && tagRegistry.isSyntaxTag(tagName)) {
           if(tm.instance instanceof GroovySyntaxTag) {
               GroovySyntaxTag tag = (GroovySyntaxTag)tm.instance;
               tag.doEndTag();
           }
           else {
              throw new GrailsTagException("Grails tag ["+tagName+"] was not closed");
           }
       }
       else {
          out.println("}");

          if(jspTags.containsKey(ns)) {
              String uri = (String) jspTags.get(ns);
              out.println("jspTag = tagLibraryResolver?.resolveTagLibrary('"+uri+"')?.getTag('"+tagName+"')");
              out.println("if(!jspTag) throw new GrailsTagException('Unknown JSP tag " + ns + ":" + tagName + "')");
              out.println("jspTag.doTag(out,attrs"+tagIndex+", body"+tagIndex+")");
          }
          else {
              if(tm.hasAttributes) {
                  out.println("invokeTag('"+tagName+"','"+ns+"',"+getCurrentOutputLineNumber()+",attrs"+tagIndex+",body"+tagIndex+")");
              }
              else {
                  out.println("invokeTag('"+tagName+"','"+ns+"',"+getCurrentOutputLineNumber()+",[:],body"+tagIndex+")");
              }
          }
       }
       tagIndex--;
    }

    private void startTag() {
        if (!finalPass) return;
        tagIndex++;

        String text;
        StringBuffer buf = new StringBuffer( scan.getToken().trim() );
        String ns = scan.getNamespace();
        
        state = scan.nextToken();
        while(state != HTML && state != GEND_TAG && state != EOF) {
            if(state == GTAG_EXPR) {
                buf.append("${");
                buf.append(scan.getToken().trim());
                buf.append("}");
            }
            else {
                buf.append(scan.getToken().trim());
            }
            state = scan.nextToken();
        }

        doNextScan = false;

        text = buf.toString();

        String tagName;
        Map attrs = new TreeMap();
        text = text.replaceAll("[\r\n\t]", " ");  // this line added TODO query this
        
        if(text.indexOf(' ') > -1) { // ignores carriage returns and new lines
               int i = text.indexOf(' ');
               tagName = text.substring(0,i);
               String attrTokens = text.substring(i,text.length());
               attrTokens += '>'; // closing bracket marker
               populateMapWithAttributes(attrs, attrTokens);
        }
        else {
            tagName = text;
        }

        if(state == EOF){
            throw new GrailsTagException("Unexpected end of file encountered parsing Tag [" + tagName + "] for " + className + ". Are you missing a closing brace '}'?");
        }

        TagMeta tm = new TagMeta();
        tm.name = tagName;
        tm.namespace = ns;
        tm.hasAttributes = !attrs.isEmpty();
        tm.lineNumber = getCurrentOutputLineNumber();
        tagMetaStack.add(tm);

        if (GroovyPage.DEFAULT_NAMESPACE.equals(ns) && tagRegistry.isSyntaxTag(tagName)) {
            if(this.tagContext == null) {
                this.tagContext = new HashMap();
                this.tagContext.put(GroovyPage.OUT,out);
                this.tagContext.put(GroovyPageParser.class, this);
            }
            GroovySyntaxTag tag = (GroovySyntaxTag)tagRegistry.newTag(tagName);
            tag.init(tagContext);
            tag.setAttributes(attrs);

            if (tag.isKeepPrecedingWhiteSpace() && currentlyBufferingWhitespace) {
                flushBufferedWhiteSpace();
            } else if(!tag.isAllowPrecedingContent() && previousContentWasNonWhitespace) {
                throw new GrailsTagException("Tag ["+tag.getName()+"] cannot have non-whitespace characters directly preceding it.");
            } else {
                // If tag does not specify buffering of WS, we swallow it here
                clearBufferedWhiteSpace();
            }

            tag.doStartTag();
            tm.instance = tag;
        }
        else {
            // Custom taglibs have to always flush the whitespace, there's no "allowPrecedingWhitespace" property on tags yet
            flushBufferedWhiteSpace();
            if(attrs.size() > 0) {
                out.print("attrs"+tagIndex+" = [");
                for (Iterator i = attrs.keySet().iterator(); i.hasNext();) {
                    String name = (String) i.next();
                    out.print(name);
                    out.print(':');

                    out.print(getExpressionText(attrs.get(name).toString()));
                    if(i.hasNext())
                        out.print(',');
                    else
                        out.println(']');
                }
            }
            out.println("body"+tagIndex+" = new GroovyPageTagBody(this,binding.webRequest) {" );
        }
    }

    private void clearBufferedWhiteSpace() {
        whitespaceBuffer.delete(0, whitespaceBuffer.length());
        currentlyBufferingWhitespace = false;
    }

    // Write out any whitespace we saved between tags
    private void flushBufferedWhiteSpace() {
        if (currentlyBufferingWhitespace) {
            out.printlnToResponse(whitespaceBuffer.toString());
            clearBufferedWhiteSpace();
        }
        currentlyBufferingWhitespace = false;
    }

    private void populateMapWithAttributes(Map attrs, String attrTokens) {
        // do first pass parse which retrieves double quoted attributes
        Matcher m = PARSE_TAG_FIRST_PASS.matcher(attrTokens);
        populateAttributesFromMatcher(m,attrs);

        // do second pass parse which retrieves single quoted attributes
        m = PARSE_TAG_SECOND_PASS.matcher(attrTokens);
        populateAttributesFromMatcher(m,attrs);
    }

    private void populateAttributesFromMatcher(Matcher m, Map attrs) {
        while(m.find()) {
            String name = m.group(2);
            String val = m.group(3);
            name = '\"' + name + '\"';
            if(val.startsWith("${") && val.endsWith("}")) {
                val = val.substring(2,val.length() -1);
            }
            else if(!(val.startsWith("[") && val.endsWith("]"))) {
                val = '\"' + val + '\"';
            }
            attrs.put(name,val);
        }
    }

    private void pageImport(String value) {
//		LOG.debug("pageImport(" + value + ')');
        String[] imports = Pattern.compile(";").split(value.subSequence(0, value.length()));
        for (int ix = 0; ix < imports.length; ix++) {
            out.print("import ");
            out.print(imports[ix]);
            out.println();
        }
    } // pageImport()


    private String escapeGroovy(CharSequence text) {
        StringBuffer buf = new StringBuffer();
        for (int ix = 0, ixz = text.length(); ix < ixz; ix++) {
            char c = text.charAt(ix);
            String rep = null;
            if (c == '\n') {
                incrementLineNumber();
                rep = "\\n";
            }
            else if (c == '\r') rep = "\\r";
            else if (c == '\t') rep = "\\t";
            else if (c == '\'') rep = "\\'";
            else if (c == '\\') rep = "\\\\";
            if (rep != null) buf.append(rep);
            else buf.append(c);
        }
        return buf.toString();
    }

    private String readStream(InputStream in) throws IOException {
    	return IOUtils.toString(in, gspEncoding);
    }

    private void script(boolean gsp) {
        if (!finalPass) return;
        if (LOG.isDebugEnabled()) LOG.debug("parse: script");
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
                    if (Character.isWhitespace(text.charAt(ix))) continue;
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
                    incrementLineNumber();
                    ix += 3;
                } else {
                    int end = match(PARA_BREAK, text, ix);
                    if (end <= 0) end = match(ROW_BREAK, text, ix);
                    if (end > 0) {
                        rep = "\n";
                        incrementLineNumber();
                        ix = end;
                    }
                }
            }
            if (rep != null) out.print(rep);
            else out.print(c);
        }
    } // write()

    private void incrementLineNumber() {
        currentOutputLine++;
    }
} // Parse
