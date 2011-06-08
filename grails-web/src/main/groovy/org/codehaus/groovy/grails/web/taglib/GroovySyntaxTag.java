/* Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.web.taglib;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.grails.web.pages.GroovyPage;
import org.codehaus.groovy.grails.web.pages.GroovyPageParser;
import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException;

/**
 * <p>A tag type that gets translated directly into Groovy syntax by the GSP parser.</p>
 *
 * <p>This is used for Java-implemented internal tags that the Parse class uses to directly inject code into the
 * generated GSP source. These tags can do more than custom taglibs as the operate at the code level, rather than at
 * the runtime view rendering level</p>
 *
 * @author Graeme Rocher
 */
public abstract class GroovySyntaxTag implements GrailsTag {

    private static final String ERROR_NO_VAR_WITH_STATUS = "When using <g:each> with a [status] attribute, you must also define a [var]. eg. <g:each var=\"myVar\">";
    protected static final String ATTRIBUTE_IN = "in";
    protected static final String ATTRIBUTE_VAR = "var";
    protected static final String ATTRIBUTES_STATUS = "status";
    @SuppressWarnings("rawtypes")
    protected Map tagContext;
    protected PrintWriter out;
    protected Map<String, String> attributes = new HashMap<String, String>();
    protected GroovyPageParser parser;

    @SuppressWarnings("rawtypes")
    public void init(Map context) {
        tagContext = context;
        parser = (GroovyPageParser) context.get(GroovyPageParser.class);
        Object outObj = context.get(GroovyPage.OUT);
        if (outObj instanceof PrintWriter) {
            out = (PrintWriter)context.get(GroovyPage.OUT);
        }
    }

    public void setWriter(Writer w) {
        if (!(w instanceof PrintWriter)) {
            throw new IllegalArgumentException("A GroovySynax tag requires a java.io.PrintWriter instance");
        }
        out = (PrintWriter)w;
    }

    @SuppressWarnings("rawtypes")
    public void setAttributes(Map attributes) {
        for (Iterator i = attributes.keySet().iterator(); i.hasNext();) {
            String attrName = (String) i.next();
            setAttribute(attrName,attributes.get(attrName));
        }
    }

    public void setAttribute(String name, Object value) {
        if (!(value instanceof String)) {
            throw new IllegalArgumentException("A GroovySynax tag requires only string valued attributes");
        }

        String stringValue = (String)value;
        if (stringValue.startsWith("${") && stringValue.endsWith("}")) {
            stringValue = stringValue.substring(2,stringValue.length() -1);
        }

        attributes.put(name.substring(1,name.length()-1), stringValue);
    }

    /**
     * <p>Tags must return the correct value to indicate whether or not whitespace before this tag should be kept in the output.</p>
     * <p>This is for tags that must follow other tags, such as g:else or g:elseif that do not allow content between them and the
     * previous tag, and need to swallow the whitespace between them.</p>
     * @return True if any whitespace immediately before the tag should be kept in the output - false if it is to be discarded
     */
    public abstract boolean isKeepPrecedingWhiteSpace();

    /**
     * <p>Tags must return the correct value to indicate whether or not non-whitespace content is permitted before this tag.</p>
     * <p>This is for tags that must follow other tags, such as g:else or g:elseif that do not allow content between them and the
     * previous tag. It is simply used as a safety mechanism to trap incorrect usage of tags.</p>
     * @todo rework this and combine with isKeepPrecedingWhiteSpace as really they are used in the same situations
     * @return True if any content is allowed immediately before the tag - false if it is an error to have such content before it
     */
    public abstract boolean isAllowPrecedingContent();

    protected String calculateExpression(String expr) {
        if (StringUtils.isBlank(expr)) {
            throw new IllegalArgumentException("Argument [expr] cannot be null or blank");
        }

        expr = expr.trim();
        if (expr.startsWith("\"") && expr.endsWith("\"")) {
            expr = expr.substring(1,expr.length()-1);
            expr = expr.trim();
        }
        if (expr.startsWith("${") && expr.endsWith("}")) {
            expr = expr.substring(2,expr.length()-1);
            expr = expr.trim();
        }
        return expr;
    }

    /**
     * @param in
     */
    protected void doEachMethod(String in) {
        String var = attributes.get(ATTRIBUTE_VAR);
        String status = attributes.get(ATTRIBUTES_STATUS);
        var = extractAttributeValue(var);
        status = extractAttributeValue(status);

        boolean hasStatus = !StringUtils.isBlank(status);
        boolean hasVar = !StringUtils.isBlank(var);
        if (hasStatus && !hasVar) {
            throw new GrailsTagException(ERROR_NO_VAR_WITH_STATUS);
        }

        if (var.equals(status) && (hasStatus)) {
            throw new GrailsTagException("Attribute [" + ATTRIBUTE_VAR +
                    "] cannot have the same value as attribute [" + ATTRIBUTES_STATUS + "]");
        }

        if (hasStatus) {
            out.println("FOR:{");
            out.println("int "+ status +" = 0" );
        }
        out.print("for( " + (hasVar ? var : "it"));
        out.print(" in "); // dot de-reference
        out.print(parser != null ? parser.getExpressionText(in, false) : extractAttributeValue(in));  // object
        out.print(" )"); // dot de-reference
        out.print(" {"); // start closure


        out.println();
    }

    protected void endEachMethod(){
        String status = attributes.get(ATTRIBUTES_STATUS);
        status = extractAttributeValue(status);
        boolean hasStatus = !StringUtils.isBlank(status);

        if (hasStatus) {
            out.println(status +"++" );
            out.println("}");
        }
        out.println("}");
    }

    private String extractAttributeValue(String attr) {
        if (StringUtils.isBlank(attr)) {
            return "";
        }
        if (attr.startsWith("\"") && attr.endsWith("\"") && attr.length() > 1) {
            attr = attr.substring(1,attr.length()-1);
        }
         if (attr.endsWith("?") && attr.length() > 1) {
            attr = attr.substring(0,attr.length()-1);
        }
        return attr;
    }
}
