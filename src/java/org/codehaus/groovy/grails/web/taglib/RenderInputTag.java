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

import groovy.text.Template;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;

/**
 * A tag that attempts to render an input for a bean property into an appropriate component based on the type.
 * It uses the templates defined in "grails-app/views/scaffolding" to achieve this by looking up
 * the template by type.
 *
 * Example:
 * <code>
 *      <gr:renderInput bean="myBean" property="firstName" />
 * </code>
 * Produces for Example (unless customised):
 * <code>
 *      <input type="text" name="firstName" value="Bob" />
 * </code>
 * @author Graeme Rocher
 * @since 12-Jan-2006
 */
public class RenderInputTag extends RequestContextTag {

    public static final String PATH_PREFIX = "/WEB-INF/grails-app/views/scaffolding/";
    public static final String PATH_SUFFIX = ".gsp";
    public static final String TAG_NAME = "renderInput";

    private static final Log LOG = LogFactory.getLog(RenderInputTag.class);

    private static final String BEAN_PROPERTY = "bean";

    @SuppressWarnings("hiding")
    private Object bean;
    private String property;
    private BeanWrapper beanWrapper;
    @SuppressWarnings("unchecked")
    private Map constrainedProperties = Collections.EMPTY_MAP;
    private Map<Class<?>, String> cachedUris = new ConcurrentHashMap<Class<?>, String>();

    protected RenderInputTag() {
        super(TAG_NAME);
    }

    @Override
    protected void doStartTagInternal() {

        GrailsDomainClass domainClass = (GrailsDomainClass) grailsApplication.getArtefact(
                DomainClassArtefactHandler.TYPE, bean.getClass().getName());
        if (domainClass != null) {
            constrainedProperties = domainClass.getConstrainedProperties();
        }
        beanWrapper = new BeanWrapperImpl(bean);
        PropertyDescriptor pd = null;
        try {
            pd = beanWrapper.getPropertyDescriptor(property);
        }
        catch (BeansException e) {
            throw new GrailsTagException("Property [" + property +
                    "] is not a valid bean property in tag [renderInput]:" + e.getMessage(),e);
        }
        GroovyPagesTemplateEngine engine = (GroovyPagesTemplateEngine)servletContext.getAttribute(
                GrailsApplicationAttributes.GSP_TEMPLATE_ENGINE);

        Template t = null;
        try {
            String uri = findUriForType(pd.getPropertyType());
            t = engine.createTemplate(uri);
            if (t == null) {
                throw new GrailsTagException("Type [" + pd.getPropertyType() +
                        "] is unsupported by tag [scaffold]. No template found.");
            }

            Map<String, Object> binding = new HashMap<String, Object>();
            binding.put("name", pd.getName());
            binding.put("value",beanWrapper.getPropertyValue(property));
            if (constrainedProperties.containsKey(property)) {
                binding.put("constraints",constrainedProperties.get(property));
            }
            else {
                binding.put("constraints",null);
            }
            t.make(binding).writeTo(out);
        }
        catch (IOException e) {
            throw new GrailsTagException("I/O error writing tag [" + getName() +
                    "] to writer: " + e.getMessage(),e);
        }
    }

    @Override
    protected void doEndTagInternal() {
        // do nothing
    }

    public boolean isDynamicAttribute(String attr) {
        return BEAN_PROPERTY.equals(attr);
    }

    public Object getBean() {
        return bean;
    }

    public void setBean(Object bean) {
        this.bean = bean;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String findUriForType(Class<?> type) throws MalformedURLException {

        if (LOG.isTraceEnabled()) {
            LOG.trace("[JspRenderInputTag] Attempting to retrieve template for type ["+type+"]");
        }
        String templateUri;
        if (cachedUris.containsKey(type)) {
            templateUri = cachedUris.get(type);
        }
        else {
            templateUri = locateTemplateUrl(type);
            cachedUris.put(type, templateUri);
        }
        return templateUri;
    }

    private String locateTemplateUrl(Class<?> type) throws MalformedURLException {
        if (type == Object.class) {
            return null;
        }

        String uri = PATH_PREFIX + type.getName() + PATH_SUFFIX;
        URL returnUrl = servletContext.getResource(uri);
        if (returnUrl == null) {
            return locateTemplateUrl(type.getSuperclass());
        }
        return uri;
    }
}
