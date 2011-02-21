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
package org.codehaus.groovy.grails.web.taglib.jsp;

import groovy.lang.Closure;
import groovy.lang.GroovyObject;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.servlet.jsp.tagext.DynamicAttributes;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsTagLibClass;
import org.codehaus.groovy.grails.commons.TagLibArtefactHandler;
import org.codehaus.groovy.grails.web.metaclass.TagLibDynamicMethods;
import org.codehaus.groovy.grails.web.pages.FastStringWriter;
import org.codehaus.groovy.grails.web.pages.GroovyPage;
import org.codehaus.groovy.grails.web.servlet.DefaultGrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.context.ApplicationContext;
import org.springframework.web.util.ExpressionEvaluationUtils;

/**
 * A tag that invokes a tag defined in a the Grails dynamic tag library. Authors of Grails tags
 * who want their tags to work in JSP should sub-class this class and call "setTagName" to set
 * the tagName of the tag within the Grails taglib
 *
 * This tag can of course be used standalone to invoke a Grails tag from JSP:
 *
 * <code>
 *   <gr:invokeTag tagName="myTag" />
 * </code>
 *
 * @author Graeme Rocher
 * @since 16-Jan-2006
 */
public class JspInvokeGrailsTagLibTag extends BodyTagSupport implements DynamicAttributes {

    private static final long serialVersionUID = 4688821761801666631L;
    private static final String ZERO_ARGUMENTS = "zeroArgumentsFlag";
    private static final String GROOVY_DEFAULT_ARGUMENT = "it";
    private static final String NAME_ATTRIBUTE = "tagName";
    private static final Pattern ATTRIBUTE_MAP = Pattern.compile("(\\s*(\\S+)\\s*:\\s*(\\S+?)(,|$){1}){1}");

    private String tagName;
    private int invocationCount;
    private List<Object> invocationArgs = new ArrayList<Object>();
    private List<String> invocationBodyContent = new ArrayList<String>();
    private BeanWrapper bean;
    protected Map<String, Object> attributes = new HashMap<String, Object>();
    private FastStringWriter sw;
    private PrintWriter out;
    private JspWriter jspWriter;
    private GrailsApplicationAttributes grailsAttributes;
    private GrailsApplication application;
    private ApplicationContext appContext;
    private static final String TAG_LIBS_ATTRIBUTE = "org.codehaus.groovy.grails.TAG_LIBS";
	private static final String OUT_PROPERTY = "out";
    private String tagContent;
    private boolean bodyInvokation;

    public JspInvokeGrailsTagLibTag() {
        bean = new BeanWrapperImpl(this);
    }

    @Override
    public final int doStartTag() throws JspException {
        for (PropertyDescriptor pd : bean.getPropertyDescriptors()) {
            if (pd.getPropertyType() == String.class &&
                    !pd.getName().equals(NAME_ATTRIBUTE) &&
                    bean.isWritableProperty(pd.getName()) &&
                    bean.isReadableProperty(pd.getName())) {

                String propertyValue = (String)bean.getPropertyValue(pd.getName());

                if (propertyValue != null) {
                    String trimmed = propertyValue.trim();
                    if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                        trimmed = trimmed.substring(1,trimmed.length() - 1);
                        Matcher m = ATTRIBUTE_MAP.matcher(trimmed);
                        Map<String, Object> attributeMap = new HashMap<String, Object>();
                        while (m.find()) {
                            String attributeName = m.group(1);
                            String attributeValue = m.group(2);
                            if (ExpressionEvaluationUtils.isExpressionLanguage(attributeValue)) {
                                attributeMap.put(attributeName, ExpressionEvaluationUtils.evaluate(
                                        attributeName, attributeValue, Object.class, pageContext));
                            }
                            else {
                                attributeMap.put(attributeName, attributeValue);
                            }
                        }
                        attributes.put(pd.getName(), attributeMap);
                    }
                    else {
                        if (ExpressionEvaluationUtils.isExpressionLanguage(propertyValue)) {
                            attributes.put(pd.getName(), ExpressionEvaluationUtils.evaluate(
                                    pd.getName(), propertyValue, Object.class, pageContext));
                        }
                        else {
                            attributes.put(pd.getName(), propertyValue);
                        }
                    }
                }
            }
        }
        return doStartTagInternal();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private GroovyObject getTagLib(String name) {
        if (application == null) {
            initPageState();
        }

        Map tagLibs = (Map)pageContext.getAttribute(TAG_LIBS_ATTRIBUTE);
        if (tagLibs == null) {
            tagLibs = new HashMap();
            pageContext.setAttribute(TAG_LIBS_ATTRIBUTE, tagLibs);
        }
        GrailsTagLibClass tagLibClass = (GrailsTagLibClass) application.getArtefactForFeature(
                TagLibArtefactHandler.TYPE, GroovyPage.DEFAULT_NAMESPACE + ':' + name);

        GroovyObject tagLib;
        if (tagLibs.containsKey(tagLibClass.getFullName())) {
            tagLib = (GroovyObject)tagLibs.get(tagLibClass.getFullName());
        }
        else {
            tagLib = (GroovyObject)appContext.getBean(tagLibClass.getFullName());
            tagLibs.put(tagLibClass.getFullName(),tagLib);
        }
        return tagLib;
    }

    private void initPageState() {
        if (application == null) {
            grailsAttributes = new DefaultGrailsApplicationAttributes(pageContext.getServletContext());
            application = grailsAttributes.getGrailsApplication();
            appContext = grailsAttributes.getApplicationContext();
        }
    }

    @SuppressWarnings("rawtypes")
    protected int doStartTagInternal() {
        GroovyObject tagLib = getTagLib(getTagName());
        if (tagLib == null) {
            throw new GrailsTagException("Tag [" + getTagName() + "] does not exist. No tag library found.");
        }

        sw = new FastStringWriter();
        out = sw;
        tagLib.setProperty(OUT_PROPERTY, out);
        Object tagLibProp;
        final Map tagLibProperties = DefaultGroovyMethods.getProperties(tagLib);
        if (tagLibProperties.containsKey(getTagName())) {
            tagLibProp = tagLibProperties.get(getTagName());
        }
        else {
            throw new GrailsTagException("Tag [" + getTagName() + "] does not exist in tag library [" +
                    tagLib.getClass().getName() + "]");
        }

        if (!(tagLibProp instanceof Closure)) {
            throw new GrailsTagException("Tag [" + getTagName() + "] does not exist in tag library [" +
                    tagLib.getClass().getName() + "]");
        }

        Closure body = new Closure(this) {
            private static final long serialVersionUID = 1861498565854341886L;
            @SuppressWarnings("unused")
            public Object doCall() {
                return call();
            }
            @SuppressWarnings("unused")
            public Object doCall(Object o) {
                return call(new Object[]{o});
            }
            @SuppressWarnings("unused")
            public Object doCall(Object[] args) {
                return call(args);
            }
            @Override
            public Object call(Object[] args) {
                invocationCount++;
                if (args.length > 0) {
                    invocationArgs.add(args[0]);
                }
                else {
                    invocationArgs.add(ZERO_ARGUMENTS);
                }
                out.print("<jsp-body-gen"+invocationCount+">");
                return "";
            }
        };
        Closure tag = (Closure)tagLibProp;
        if (tag.getParameterTypes().length == 1) {
            tag.call(new Object[]{ attributes });
            if (body != null) {
                body.call();
            }
        }
        if (tag.getParameterTypes().length == 2) {
            tag.call(new Object[] { attributes, body });
        }

        Collections.reverse(invocationArgs);
        setCurrentArgument();
        return EVAL_BODY_BUFFERED;
    }

    private void setCurrentArgument() {
        if (invocationCount == 0) {
            return;
        }

        Object arg = invocationArgs.get(invocationCount - 1);
        if (arg.equals(ZERO_ARGUMENTS)) {
            pageContext.setAttribute(GROOVY_DEFAULT_ARGUMENT, null);
        }
        else {
            pageContext.setAttribute(GROOVY_DEFAULT_ARGUMENT, arg);
        }
    }

    @Override
    public int doAfterBody() throws JspException {

        BodyContent b = getBodyContent();
        if (invocationCount > 0) {
            if (b != null) {
                jspWriter = b.getEnclosingWriter();
                invocationBodyContent.add(b.getString());
                bodyInvokation = true;
                b.clearBody();
            }
        }

        invocationCount--;
        setCurrentArgument();
        if (invocationCount <= 0)  {
            tagContent = sw.toString();
            int i = 1;
            StringBuilder buf = new StringBuilder();
            for (String body : invocationBodyContent) {
                String replaceFlag = "<jsp-body-gen" + i + ">";
                int j = tagContent.indexOf(replaceFlag);
                if (j > -1) {
                    buf.append(tagContent.substring(0,j))
                       .append(body)
                       .append(tagContent.substring(j + replaceFlag.length(), tagContent.length()));
                    tagContent = buf.toString();
                    if (tagContent != null) {
                        try {
                            jspWriter.write(tagContent);
                            out.close();
                        }
                        catch (IOException e) {
                            throw new JspTagException("I/O error writing tag contents [" + tagContent +
                                    "] to response out");
                        }
                    }
                    buf.delete(0, buf.length());
                }
            }
            return SKIP_BODY;
        }

        return EVAL_BODY_BUFFERED;
    }

    @Override
    public int doEndTag() throws JspException {
        if (!bodyInvokation) {
            if (tagContent == null) {
                tagContent = sw.toString();
            }

            if (tagContent != null) {
                try {
                    jspWriter =  pageContext.getOut();
                    jspWriter.write(tagContent);
                    out.close();
                }
                catch (IOException e) {
                    throw new JspTagException("I/O error writing tag contents [" + tagContent +
                            "] to response out");
                }
            }
        }
        return SKIP_BODY;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public final void setDynamicAttribute(String uri, String localName, Object value) throws JspException {
        if (value instanceof String) {
            String stringValue = (String)value;
            String trimmed = stringValue.trim();
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                trimmed = trimmed.substring(1,trimmed.length() - 1);
                Matcher m = ATTRIBUTE_MAP.matcher(trimmed);
                Map<String, Object> attributeMap = new HashMap<String, Object>();
                while (m.find()) {
                    String attributeName = m.group(1);
                    String attributeValue = m.group(2);
                    if (ExpressionEvaluationUtils.isExpressionLanguage(attributeValue)) {
                        attributeMap.put(attributeName, ExpressionEvaluationUtils.evaluate(
                                attributeName, attributeValue, Object.class, pageContext));
                    }
                    else {
                        attributeMap.put(attributeName, attributeValue);
                    }
                }
                attributes.put(localName, attributeMap);
            }
            else {
                if (ExpressionEvaluationUtils.isExpressionLanguage(stringValue)) {
                    attributes.put(localName,ExpressionEvaluationUtils.evaluate(localName,stringValue,Object.class,pageContext));
                }
                else {
                    attributes.put(localName,value);
                }
            }
        }
        else {
            attributes.put(localName,value);
        }
    }
}
