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

import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.web.pages.GroovyPage;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.util.UrlPathHelper;

/**
 * @author Graeme Rocher
 */
public abstract class RequestContextTag implements GrailsTag {

    protected Writer out;
    @SuppressWarnings("rawtypes")
    protected Map attributes = new HashMap();
    protected ServletRequest request;
    protected String contextPath;
    protected UrlPathHelper urlPathHelper = new UrlPathHelper();
    protected GrailsTagRegistry registry;
    private boolean init;
    protected BeanWrapper bean;
    protected ServletContext servletContext;
    protected ServletResponse response;
    protected WebApplicationContext applicationContext;
    protected GrailsApplication grailsApplication;
    private String name;

    protected RequestContextTag(String name) {
        this.name = name;
        bean = new BeanWrapperImpl(this);
    }

    public String getName() {
        return name;
    }

    @SuppressWarnings("rawtypes")
    public void init(Map context) {
        Assert.notNull(context, "Argument 'context' cannot be null");
        out = (Writer)context.get(GroovyPage.OUT);
        request = (ServletRequest)context.get(GroovyPage.REQUEST);
        servletContext = (ServletContext)context.get(GroovyPage.SERVLET_CONTEXT);
        response = (ServletResponse)context.get(GroovyPage.RESPONSE);
        applicationContext = RequestContextUtils.getWebApplicationContext(request, servletContext);
        grailsApplication = (GrailsApplication)applicationContext.getBean(GrailsApplication.APPLICATION_ID);
        if (context.get(GroovyPage.ATTRIBUTES) == null) {
            attributes = new HashMap();
        }
        else {
            attributes = (Map)context.get(GroovyPage.ATTRIBUTES);
        }
        contextPath = urlPathHelper.getContextPath((HttpServletRequest)request);
        init = true;
    }

    @SuppressWarnings("unchecked")
    public void setAttribute(String name, Object value) {
        if (bean.isWritableProperty(name)) {
            bean.setPropertyValue(name,value);
        }
        else {
            attributes.put(name, value);
        }
    }

    public GrailsTagRegistry getRegistry() {
        return registry;
    }

    public void setWriter(Writer w) {
        out = w;
    }

    @SuppressWarnings("rawtypes")
    public void setAttributes(Map attributes) {
        this.attributes = attributes;
    }

    public final void doStartTag() {
        Assert.state(init, "Tag not initialised called 'init' first");
        doStartTagInternal();
    }

    protected abstract void doStartTagInternal();
    protected abstract void doEndTagInternal();

    public final void doEndTag()  {
        Assert.state(init, "Tag not initialised called 'init' first");
        doEndTagInternal();
    }
}
