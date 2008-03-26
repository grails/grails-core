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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.*;
import org.codehaus.groovy.grails.web.taglib.GrailsTagRegistry;
import org.codehaus.groovy.grails.web.taglib.GroovySyntaxTag;
import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException;

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
public class Parse implements Tokens {
    public static final Log LOG = LogFactory.getLog(Parse.class);

    private static final Pattern PARA_BREAK = Pattern.compile("/p>\\s*<p[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ROW_BREAK = Pattern.compile("((/td>\\s*</tr>\\s*<)?tr[^>]*>\\s*<)?td[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern PARSE_TAG_FIRST_PASS = Pattern.compile("(\\s*(\\S+)\\s*=\\s*[\"]([^\"]*)[\"][\\s|>]{1}){1}");
    private static final Pattern PARSE_TAG_SECOND_PASS = Pattern.compile("(\\s*(\\S+)\\s*=\\s*[']([^']*)['][\\s|>]{1}){1}");

    private Scan scan;
    private GSPWriter out;
    private String className;
    private boolean finalPass = false;
    private int tagIndex;
    private Map tagContext;
    private List tagMetaStack = new ArrayList();
    private GrailsTagRegistry tagRegistry = GrailsTagRegistry.getInstance();
    private boolean bufferWhiteSpace ;

    private StringBuffer whiteSpaceBuffer = new StringBuffer();
    private int currentOutputLine = 1;
    private String contentType = DEFAULT_CONTENT_TYPE;
    private boolean doNextScan = true;
    private int state;
    private static final String START_MULTILINE_STRING = "'''";
    private static final String END_MULTILINE_STRING = "'''";
    private static final String DEFAULT_CONTENT_TYPE = "text/html;charset=UTF-8";
    private Map constants = new TreeMap();
    private int constantCount = 0;

    private final String pageName;
    private static final String EMPTY_MULTILINE_STRING = "''''''";
    public static final String[] DEFAULT_IMPORTS = new String[] {
        "org.codehaus.groovy.grails.web.pages.GroovyPage",
        "org.codehaus.groovy.grails.web.taglib.*",
        "org.springframework.web.util.*",
        "grails.util.GrailsUtil"
    };
    private static final String CONFIG_PROPERTY_DEFAULT_CODEC = "grails.views.default.codec";
    private static final String CONFIG_PROPERTY_GSP_ENCODING = "grails.views.gsp.encoding";

    private String codecName;
    private static final String IMPORT_DIRECTIVE = "import";
    private static final String CONTENT_TYPE_DIRECTIVE = "contentType";
    private static final String DEFAULT_CODEC_DIRECTIVE = "defaultCodec";
    private String gspEncoding;
    public static final String GROOVY_SOURCE_CHAR_ENCODING = "UTF-8";

    public String getContentType() {
        return this.contentType;
    }

    public int getCurrentOutputLineNumber() {
        return currentOutputLine;
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

    public Parse(String name, String filename, InputStream in) throws IOException {
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
        
        scan = new Scan(readStream(in));
        this.pageName = filename;
        makeName(name);
        Object o = config.get(CONFIG_PROPERTY_DEFAULT_CODEC);
        lookupCodec(o);

    } // Parse()

    private void lookupCodec(Object o) {
        if(o!=null) {
            String codecName = o.toString();
            GrailsApplication app = ApplicationHolder.getApplication();
            if(app != null) {
                GrailsClass codecClass = app.getArtefactByLogicalPropertyName(CodecArtefactHandler.TYPE, codecName);
                if(codecClass == null) codecClass = app.getArtefactByLogicalPropertyName(CodecArtefactHandler.TYPE, codecName.toUpperCase());
                if(codecClass != null) {
                    this.codecName = codecClass.getFullName();
                }
            }
        }
    }

    public int[] getLineNumberMatrix() {
        return out.getLineNumbers();
    }
    public InputStream parse() {

        StringWriter sw = new StringWriter();
        out = new GSPWriter(sw,this);
        page();
        finalPass = true;
        scan.reset();
        page();

        // This gets bytes in system's default encoding
        InputStream in = null;
        try {
            in = new ByteArrayInputStream(sw.toString().getBytes(GROOVY_SOURCE_CHAR_ENCODING));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Grails cannot run unless your environment supports UTF-8!");
        }
        //System.out.println("Compiled GSP into Groovy code: " + sw.toString());
        if(LOG.isDebugEnabled()) {
            LOG.debug("Compiled GSP into Groovy code: " + sw.toString());
        }
        scan = null;
        return in;
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
        directPage(text);
    } // direct()

    private void directPage(String text) {
        text = text.trim();
//		LOG.debug("directPage(" + text + ')');
        Pattern pat = Pattern.compile("(\\w+)\\s*=\\s*\"([^\"]*)\"");
        Matcher mat = pat.matcher(text);
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
        if(codecName != null) {
            out.printlnToResponse("Codec.encode("+text+")");            
        }
        else {
            out.printlnToResponse(text);
        }
    } // expr()


    private void html() {
        if (!finalPass) return;
        if (LOG.isDebugEnabled()) LOG.debug("parse: html");
        String text = scan.getToken();
        if(Pattern.compile("\\S").matcher(text).find())
            bufferWhiteSpace = false;
        
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        String[] lines = text.split("\\n");

        if(lines.length == 1 && !StringUtils.isBlank(lines[0])) {
            out.printlnToResponse('\'' + escapeGroovy(lines[0]) + '\'');
        }
        else {

            pw.print(START_MULTILINE_STRING);
            boolean hasContent = false;
            boolean firstLine = true;
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                final String content = escapeGroovy(line);
                if(!StringUtils.isEmpty(content)) {
                    if(!hasContent) {
                        hasContent = true;
                        break;
                    }
                }
            }
            if(hasContent) {
                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i];
                    final String content = escapeGroovy(line);
                    if(firstLine) {
                        pw.print(content);
                        firstLine = false;
                    }
                    else {
                        pw.println();
                        pw.print(content);
                    }

                }
            }
            pw.print(END_MULTILINE_STRING);
            pw.println();
            
            if(hasContent && !bufferWhiteSpace) {
                final String constantValue = sw.toString();
                final String constantName = "STATIC_HTML_CONTENT_" + constantCount++;
                constants.put(constantName, constantValue);
                out.printlnToResponse(constantName);
            }
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
            out.println("public Object run() {");            

        }

        loop: for (;;) {
            if(doNextScan)
                state = scan.nextToken();
            else
                doNextScan = true;

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
                throw new GrailsTagException("Grails tags were not closed! ["+tagMetaStack+"] in GSP "+pageName+"", pageName, tag.lineNumber);
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
            if(codecName != null) {
                out.print("import ");
                out.print(codecName);
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
               if(tag.isBufferWhiteSpace())
                    bufferWhiteSpace = true;
               tag.doEndTag();
           }
           else {
              throw new GrailsTagException("Grails tag ["+tagName+"] was not closed");
           }
       }
       else {
          out.println("}");
          if(tm.hasAttributes) {
               out.println("invokeTag('"+tagName+"','"+ns+"',attrs"+tagIndex+",body"+tagIndex+")");
          }
          else {
               out.println("invokeTag('"+tagName+"','"+ns+"',[:],body"+tagIndex+")");
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

               // do first pass parse which retrieves double quoted attributes
                Matcher m = PARSE_TAG_FIRST_PASS.matcher(attrTokens);
                populateAttributesFromMatcher(m,attrs);

               // do second pass parse which retrieves single quoted attributes
               m = PARSE_TAG_SECOND_PASS.matcher(attrTokens);
               populateAttributesFromMatcher(m,attrs);
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
            }
            GroovySyntaxTag tag = (GroovySyntaxTag)tagRegistry.newTag(tagName);
            tag.init(tagContext);
            tag.setAttributes(attrs);
            if(!tag.hasPrecedingContent() && !bufferWhiteSpace) {
                throw new GrailsTagException("Tag ["+tag.getName()+"] cannot have non-whitespace characters directly preceding it.");
            }
            else if(!tag.hasPrecedingContent() && bufferWhiteSpace) {
                whiteSpaceBuffer.delete(0,whiteSpaceBuffer.length());
                bufferWhiteSpace = false;
            } else {
                if(whiteSpaceBuffer.length() > 0) {
                    out.printlnToResponse(whiteSpaceBuffer.toString());
                    whiteSpaceBuffer.delete(0,whiteSpaceBuffer.length());
                }
                bufferWhiteSpace = false;
            }
            tag.doStartTag();
            tm.instance = tag;
        }
        else {
            if(attrs.size() > 0) {
                out.print("attrs"+tagIndex+" = [");
                for (Iterator i = attrs.keySet().iterator(); i.hasNext();) {
                    String name = (String) i.next();
                    out.print(name);
                    out.print(':');
                    out.print(attrs.get(name));
                    if(i.hasNext())
                        out.print(',');
                    else
                        out.println(']');
                }
            }
            out.println("body"+tagIndex+" = new GroovyPageTagBody(this,binding.webRequest) {" );
        }
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
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            byte[] buf = new byte[8192];
            for (;;) {
                int read = in.read(buf);
                if (read <= 0) break;
                out.write(buf, 0, read);
            }
            return out.toString( gspEncoding);
        } finally {
            out.close();
            in.close();
        }
    } // readStream()

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
