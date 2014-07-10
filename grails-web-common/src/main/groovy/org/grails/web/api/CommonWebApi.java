/*
 * Copyright 2010 the original author or authors.
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
package org.grails.web.api;

import java.io.Serializable;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import grails.core.GrailsApplication;
import grails.plugins.GrailsPluginManager;
import grails.core.support.GrailsApplicationAware;
import org.grails.support.encoding.CodecLookupHelper;
import org.grails.support.encoding.Encoder;
import grails.web.mvc.FlashScope;
import grails.web.util.GrailsApplicationAttributes;
import grails.web.servlet.mvc.GrailsParameterMap;
import org.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * API shared by controllers, tag libraries and any other web artifact.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
public class CommonWebApi implements GrailsApplicationAware, ServletContextAware, ApplicationContextAware, Serializable{
    private static final long serialVersionUID = 1;
    public static final String RAW_CODEC_NAME = "org.grails.plugins.codecs.RawCodec";

    private transient GrailsPluginManager pluginManager;
    private transient GrailsApplication grailsApplication;
    private transient ServletContext servletContext;
    private transient ApplicationContext applicationContext;
    private transient Encoder rawEncoder;

    public CommonWebApi(GrailsPluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    public CommonWebApi() {
    }

    /**
     * Marks the given value to be output in raw form without encoding
     *
     * @param instance The instance
     * @param value The value
     * @return The raw unencoded value
     * @since 2.3
     */
    public Object raw(Object instance, Object value) {
        Encoder encoder = getRawEncoder(instance);
        if(encoder != null) {
            return encoder.encode(value);
        }
        else {
            return InvokerHelper.invokeMethod(value, "encodeAsRaw", null);
        }
    }

    private Encoder getRawEncoder(GrailsApplication application) {
        if(application != null) {
            return CodecLookupHelper.lookupEncoder(application, "Raw");
        }
        return null;
    }
    private Encoder getRawEncoder(Object instance) {
        if(rawEncoder == null) {
            GrailsApplication application = getGrailsApplication(instance);
            rawEncoder = getRawEncoder(application);
        }
        return rawEncoder;
    }

    /**
     * Obtains the Grails parameter map
     *
     * @return The GrailsParameterMap instance
     */
    public GrailsParameterMap getParams(Object instance) {
        return currentRequestAttributes().getParams();
    }

    /**
     * Obtains the Grails FlashScope instance
     *
     * @return The FlashScope instance
     */
    public FlashScope getFlash(Object instance) {
        return currentRequestAttributes().getFlashScope();
    }

    /**
     * Obtains the HttpSession instance
     *
     * @return The HttpSession instance
     */
    public HttpSession getSession(Object instance) {
        return currentRequestAttributes().getSession();
    }

    /**
     * Obtains the HttpServletRequest instance
     *
     * @return The HttpServletRequest instance
     */
    public HttpServletRequest getRequest(Object instance) {
        return currentRequestAttributes().getCurrentRequest();
    }

    /**
     * Obtains the ServletContext instance
     *
     * @return The ServletContext instance
     */
    public ServletContext getServletContext(Object instance) {
        if (servletContext == null) {
            servletContext = currentRequestAttributes().getServletContext();
        }
        return servletContext;
    }

    /**
     * Obtains the HttpServletResponse instance
     *
     * @return The HttpServletResponse instance
     */
    public HttpServletResponse getResponse(Object instance) {
        return currentRequestAttributes().getCurrentResponse();
    }

    /**
     * Obtains the GrailsApplicationAttributes instance
     *
     * @return The GrailsApplicationAttributes instance
     */
    public GrailsApplicationAttributes getGrailsAttributes(Object instance) {
        return currentRequestAttributes().getAttributes();
    }

    /**
     * Obtains the GrailsApplication instance
     * @return The GrailsApplication instance
     */
    public GrailsApplication getGrailsApplication(Object instance) {
        if (grailsApplication == null) {
            grailsApplication = getGrailsAttributes(instance).getGrailsApplication();
        }
        return grailsApplication;
    }

    /**
     * Obtains the ApplicationContext instance
     * @return The ApplicationContext instance
     */
    public ApplicationContext getApplicationContext(Object instance) {
        if (applicationContext == null) {
            applicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext(instance));
        }
        return applicationContext;
    }

    /**
     * Obtains the currently executing action name
     * @return The action name
     */
    public String getActionName(Object instance) {
        return currentRequestAttributes().getActionName();
    }

    /**
     * Obtains the currently executing controller name
     * @return The controller name
     */
    public String getControllerName(Object instance) {
        return currentRequestAttributes().getControllerName();
    }

    /**
     * Obtains the currently executing controller namespace
     * @return The controller name
     */
    public String getControllerNamespace(Object instance) {
        return currentRequestAttributes().getControllerNamespace();
    }

    /**
     * Obtains the currently executing controllerClass
     * @return The controller class
     */
    public Object getControllerClass(Object instance) {
        return currentRequestAttributes().getControllerClass();
    }

    /**
     * Obtains the currently executing web request
     *
     * @return The GrailsWebRequest instance
     */
    public GrailsWebRequest getWebRequest(Object instance) {
        return currentRequestAttributes();
    }

    /**
     * Obtains the pluginContextPath
     *
     * @param delegate The object the method is being invoked on
     * @return The plugin context path
     */
    public String getPluginContextPath(Object delegate) {
        GrailsPluginManager manager = getPluginManagerInternal(delegate);
        final String pluginPath = manager != null ? manager.getPluginPathForInstance(delegate) : null;
        return pluginPath !=null ? pluginPath : "";
    }

    private GrailsPluginManager getPluginManagerInternal(Object delegate) {
        if (pluginManager == null) {
            ApplicationContext ctx = getApplicationContext(delegate);
            pluginManager = ctx != null ? ctx.getBean(GrailsPluginManager.BEAN_NAME, GrailsPluginManager.class) : null;
        }
        return pluginManager;
    }

    public void setGrailsApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication;
        this.rawEncoder = getRawEncoder(grailsApplication);
    }

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    protected GrailsWebRequest currentRequestAttributes() {
        return (GrailsWebRequest)RequestContextHolder.currentRequestAttributes();
    }
}
