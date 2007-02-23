/* Copyright 2004-2005 Graeme Rocher
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

import grails.util.GrailsUtil;
import groovy.lang.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.web.metaclass.ControllerDynamicMethods;
import org.codehaus.groovy.grails.web.metaclass.GetParamsDynamicProperty;
import org.codehaus.groovy.grails.web.metaclass.GetSessionDynamicProperty;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * An instance of groovy.lang.Writable that writes itself to the specified
 * writer, typically the response writer
 *
 * @author Graeme Rocher
 * @since 0.5
 * 
 *        <p/>
 *        Created: Feb 23, 2007
 *        Time: 11:36:44 AM
 */
class GroovyPageWritable implements Writable {

    private static final Log LOG = LogFactory.getLog(GroovyPageWritable.class);
    
    private HttpServletResponse response;
    private HttpServletRequest request;
    private GroovyPageMetaInfo metaInfo;
    private boolean showSource;

    private ServletContext context;
    private Map additionalBinding = new HashMap();
    private static final String GROOVY_SOURCE_CONTENT_TYPE = "text/plain";

    public GroovyPageWritable(GroovyPageMetaInfo metaInfo) {
        GrailsWebRequest webRequest = (GrailsWebRequest) RequestContextHolder.currentRequestAttributes();
        this.request = webRequest.getCurrentRequest();
        this.response = webRequest.getCurrentResponse();
        this.context = webRequest.getServletContext();
        this.showSource = request.getParameter("showSource") != null && GrailsUtil.isDevelopmentEnv();
        this.metaInfo = metaInfo;
    }

    /**
     * This sets any additional variables that need to be placed in the Binding of the GSP page.
     *
     * @param binding The additional variables
     */
    public void setBinding(Map binding) {
        if(binding != null)
            this.additionalBinding = binding;
    }

    /**
     * Writes the template to the specified Writer
     *
     * @param out The Writer to write to, normally the HttpServletResponse
     * @return Returns the passed Writer
     * @throws IOException
     */
    public Writer writeTo(Writer out) throws IOException {
        if (showSource) {
            // Set it to TEXT
            response.setContentType(GROOVY_SOURCE_CONTENT_TYPE); // must come before response.getOutputStream()
            writeInputStreamToResponse(metaInfo.getGroovySource(), out);
            metaInfo.setGroovySource(null);
        } else {
            // Set it to HTML by default
            if(LOG.isDebugEnabled() && !response.isCommitted()) {
                LOG.debug("Writing response with content type: " + metaInfo.getContentType());
            }
            if(!response.isCommitted())
                response.setContentType(metaInfo.getContentType()); // must come before response.getWriter()

            Binding binding = formulateBinding(request, response, out);
            Script page = InvokerHelper.createScript(metaInfo.getPageClass(), binding);
            page.run();
        }
        return out;
    }

    /**
     * Copy all of input to output.
     * @param in The input stream to writeInputStreamToResponse from
     * @param out The output to write to
     * @throws IOException When an error occurs writing to the response Writer
     */
    protected void writeInputStreamToResponse(InputStream in, Writer out) throws IOException {
        try {
            Reader reader = new InputStreamReader(in);
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
    }

    /**
     * Prepare Bindings before instantiating page.
     * @param request The HttpServletRequest instance
     * @param response The HttpServletResponse instance
     * @param out The response out
     * @return the Bindings
     */
    protected Binding formulateBinding(HttpServletRequest request, HttpServletResponse response, Writer out)
            throws IOException {
        // Set up the script context
        Binding binding = new Binding();

        GroovyObject controller = (GroovyObject)request.getAttribute(GrailsApplicationAttributes.CONTROLLER);

        if(controller!=null) {
            try {
                formulateBindingFromController(binding, controller, out);
            }
            catch(MissingPropertyException mpe) {
                formulateBindingFromWebRequest(binding, request, response, out);
            }
        }
        else {
            formulateBindingFromWebRequest(binding, request, response, out);
        }
        populateViewModel(request, binding);


        return binding;
    }

    protected void populateViewModel(HttpServletRequest request, Binding binding) {
        // Go through request attributes and add them to the binding as the model
        for (Enumeration attributeEnum =  request.getAttributeNames(); attributeEnum.hasMoreElements();) {
            String key = (String) attributeEnum.nextElement();
            if(!binding.getVariables().containsKey(key)) {
                binding.setVariable( key, request.getAttribute(key) );
            }
        }
        for (Iterator i = additionalBinding.keySet().iterator(); i.hasNext();) {
            String key =  (String)i.next();
            binding.setVariable(key, additionalBinding.get(key));
        }
    }


    private void formulateBindingFromWebRequest(Binding binding, HttpServletRequest request, HttpServletResponse response, Writer out) {
        // if there is no controller in the request configure using existing attributes, creating objects where necessary
        GrailsWebRequest webRequest = (GrailsWebRequest) RequestContextHolder.currentRequestAttributes();

        binding.setVariable(GroovyPage.REQUEST, request);
        binding.setVariable(GroovyPage.RESPONSE, response);
        binding.setVariable(GroovyPage.FLASH, webRequest.getFlashScope());
        binding.setVariable(GroovyPage.SERVLET_CONTEXT, context);

        ApplicationContext appCtx = webRequest.getAttributes().getApplicationContext();
        binding.setVariable(GroovyPage.APPLICATION_CONTEXT, appCtx);
        if(appCtx!=null)
                binding.setVariable(GrailsApplication.APPLICATION_ID, webRequest.getAttributes().getGrailsApplication());
        binding.setVariable(GroovyPage.SESSION, webRequest.getSession());
        binding.setVariable(GroovyPage.PARAMS, webRequest.getParams());
        binding.setVariable(GroovyPage.OUT, out);
    }

    private void formulateBindingFromController(Binding binding, GroovyObject controller, Writer out) {
        binding.setVariable(GroovyPage.REQUEST, controller.getProperty(ControllerDynamicMethods.REQUEST_PROPERTY));
        binding.setVariable(GroovyPage.RESPONSE, controller.getProperty(ControllerDynamicMethods.RESPONSE_PROPERTY));
        binding.setVariable(GroovyPage.FLASH, controller.getProperty(ControllerDynamicMethods.FLASH_SCOPE_PROPERTY));
        binding.setVariable(GroovyPage.SERVLET_CONTEXT, context);
        ApplicationContext appContext = (ApplicationContext)context.getAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT);
        binding.setVariable(GroovyPage.APPLICATION_CONTEXT, appContext);
        binding.setVariable(GrailsApplication.APPLICATION_ID, appContext.getBean(GrailsApplication.APPLICATION_ID));
        binding.setVariable(GrailsApplicationAttributes.CONTROLLER, controller);
        binding.setVariable(GroovyPage.SESSION, controller.getProperty(GetSessionDynamicProperty.PROPERTY_NAME));
        binding.setVariable(GroovyPage.PARAMS, controller.getProperty(GetParamsDynamicProperty.PROPERTY_NAME));
        binding.setVariable(GroovyPage.PLUGIN_CONTEXT_PATH, controller.getProperty(GroovyPage.PLUGIN_CONTEXT_PATH));
        binding.setVariable(GroovyPage.OUT, out);
    }
}
