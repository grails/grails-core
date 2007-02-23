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
package org.codehaus.groovy.grails.web.pages;

import groovy.lang.*;
import groovy.text.Template;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.web.pages.exceptions.GroovyPagesException;
import org.codehaus.groovy.grails.support.ResourceAwareTemplateEngine;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.support.ServletContextResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.beans.BeansException;
import grails.util.GrailsUtil;

/**
 * A GroovyPagesTemplateEngine based on (but not extending) the existing TemplateEngine implementations
 * within Groovy. It allows GSP pages to be re-used in different context using code like the below:
 *
 * <code>
 *      Template t = new GroovyPagesTemplateEngine()
 *                          .createTemplate(context,request,response);
 *      t.make()
 *       .writeTo(out);
 * </code>
 *
 * @author Graeme Rocher
 *
 * @since 0.1
 * 
 * Created: 12-Jan-2006
 */
public class GroovyPagesTemplateEngine  extends ResourceAwareTemplateEngine implements ApplicationContextAware {

    private static final String GSP_TEMPLATE_RESOURCE = "org.codehaus.groovy.grails.GSP_TEMPLATE_RESOURCE";

    private static final Log LOG = LogFactory.getLog(GroovyPagesTemplateEngine.class);
    private static Map pageCache = Collections.synchronizedMap(new HashMap());
    private GroovyClassLoader classLoader = new GroovyClassLoader();
    private int scriptNameCount;
    private ApplicationContext applicationContext;
    private ServletContextResourceLoader resourceLoader;


    public GroovyPagesTemplateEngine(ServletContext servletContext) {
        if(servletContext == null) throw new IllegalArgumentException("Argument [servletContext] cannot be null");
        this.resourceLoader = new ServletContextResourceLoader(servletContext);
    }

    /**
     * Sets the ClassLoader that the TemplateEngine should use to
     * @param classLoader The ClassLoader to use when compilation of Groovy Pages occurs
     */
    public void setClassLoader(GroovyClassLoader classLoader) {
        this.classLoader = classLoader;
    }


    /**
     * Retrieves a line number matrix for the specified page that can be used
     * to retrieve the actual line number within the GSP page if the line number within the
     * compiled GSP is known
     *
     * @param context The ServletContext instance
     * @param url The URL of the page
     * @return An array where the index is the line number witin the compiled GSP and the value is the line number within the source
     */
    public int[] calculateLineNumbersForPage(ServletContext context,String url) {
        try {
            Resource r = getResourceForUri(url);
            if(r != null) {
                GroovyPageMetaInfo metaInfo = buildPageMetaInfo(r.getInputStream());
                if(metaInfo!= null) {
                    return metaInfo.getLineNumbers();
                }
            }

        } catch (Exception e) {
            // ignore, non critical method used for retrieving debug info
            LOG.warn("Exception retrieving line numbers from GSP: " + url + ", message: " + e.getMessage());
            LOG.debug("Full stack trace of error", e);
        }

        return new int[0];
    }


    /**
     * Creates a Template for the given Spring Resource instance
     *
     * @param resource The Resource to create the Template for
     * @return The Template instance
     */
    public Template createTemplate(Resource resource) {
        if(resource == null) {
            GrailsWebRequest webRequest = getWebRequest();
            throw new GroovyPagesException("No Groovy page found for URI: " + getCurrentRequestUri(webRequest.getCurrentRequest()));
        }
        //if(!resource.exists()) throw new GroovyPagesException("No Groovy page found for ["+resource.getDescription()+"]");

        String name = establishPageName(resource);
        if(pageCache.containsKey(name)) {
            GroovyPageMetaInfo meta = (GroovyPageMetaInfo)pageCache.get(name);

            if(isGroovyPageReloadable(resource, meta)) {
                return createTemplateWithResource(resource);
            }
            else {
                return new GroovyPageTemplate(meta);
            }
        }
        else {
            return createTemplateWithResource(resource);
        }
    }

    /**
     * Creates a Template using the given URI.
     *
     * @param uri The URI of the page to create the template for
     * @return The Template instance
     * @throws CompilationFailedException
     */
    public Template createTemplate(String uri)  {
        return createTemplate(getResourceForUri(uri));
    }

    /**
     * Creates a Template using the given text for the Template and the given name. The name
     * of the template is required
     *
     * @param txt The URI of the page to create the template for
     * @param pageName The name of the page being parsed
     *
     * @return The Template instance
     * @throws CompilationFailedException

     */
    public Template createTemplate(String txt, String pageName)  {
        if(StringUtils.isBlank(txt)) throw new IllegalArgumentException("Argument [txt] cannot be null or blank");
        if(StringUtils.isBlank(pageName)) throw new IllegalArgumentException("Argument [pageName] cannot be null or blank");
        
        return createTemplate(new ByteArrayResource(txt.getBytes(), pageName));
    }
    /**
     * Creates a Template for the currently executing Request
     *
     * @return The Template for the currently executing request
     * @throws java.io.IOException    Thrown when an exception occurs Reading the Template
     * @throws ClassNotFoundException  Thrown when the class of the template was not found
     */
    public Template createTemplate() throws IOException, ClassNotFoundException {
        GrailsWebRequest webRequest = getWebRequest();
        String uri = getCurrentRequestUri(webRequest.getCurrentRequest());

        return createTemplate(uri);        
    }

    public Template createTemplate(File file) throws CompilationFailedException, ClassNotFoundException, IOException {
        return createTemplate(new FileSystemResource(file));
    }

    public Template createTemplate(URL url) throws CompilationFailedException, ClassNotFoundException, IOException {
        return createTemplate(new UrlResource(url));
    }

    /**
     * Create a Template for the given InputStream
     * @param inputStream The InputStream to create the Template for
     * @return The Template instance
     */
    public Template createTemplate(InputStream inputStream) {

        GroovyPageMetaInfo metaInfo = buildPageMetaInfo(inputStream);

        return new GroovyPageTemplate(metaInfo);
    }
    


    private boolean isGroovyPageReloadable(Resource resource, GroovyPageMetaInfo meta) {
        return GrailsUtil.isDevelopmentEnv() && (establishLastModified(resource) > meta.getLastModified());
    }


    public Resource getResourceForUri(String uri) {
        Resource r;
        r = getResourceWithinContext(uri);
        if(r == null || !r.exists()) {
            uri = getUriWithinGrailsViews(uri);
            return getResourceWithinContext(uri); 
        }
        return r;
    }

    private Resource getResourceWithinContext(String uri) {
        Resource r;
        if(this.applicationContext != null) {
            r = this.applicationContext.getResource(uri);
        }
        else {
            r = resourceLoader.getResource(uri);
        }
        return r;
    }


    private long establishLastModified(Resource resource) {
        long lastModified;
        try {
            URLConnection urlc = resource.getURL().openConnection();
            urlc.setDoInput(false);
            urlc.setDoOutput(false);

            lastModified = urlc.getLastModified();
        } catch (FileNotFoundException fnfe) {
            lastModified = -1;
        } catch (IOException e) {
            lastModified = -1;
        }
        return lastModified;
    }

    private Template createTemplateWithResource(Resource resource) {
        GrailsWebRequest request = getWebRequest();
        request.setAttribute(GSP_TEMPLATE_RESOURCE, resource, GrailsWebRequest.SCOPE_REQUEST);
        try {
            return createTemplate(resource.getInputStream());
        } catch (IOException e) {
            throw new GroovyPagesException("I/O reading Groovy page ["+resource.getDescription()+"]: " + e.getMessage(),e);
        }
    }

    private GroovyPageMetaInfo buildPageMetaInfo(InputStream inputStream) {
        Resource res = optainResourceFromRequest();
        String name = establishPageName(res);

        long lastModified = establishLastModified(res);

        Parse parse;
        try {
            parse = new Parse(name, inputStream);
        } catch (IOException e) {
            throw new GroovyPagesException("I/O parsing Groovy page ["+res.getDescription()+"]: " + e.getMessage(),e);
        }
        InputStream in = parse.parse();

        // Make a new metaInfo
        GroovyPageMetaInfo metaInfo = createPageMetaInfo(parse, lastModified, in);
        metaInfo.setPageClass( compileGroovyPage(in, name) );

        pageCache.put(name, metaInfo);
        return metaInfo;
    }

    private Class compileGroovyPage(InputStream in, String name) {
        // Compile the script into an object
        Class scriptClass;
        try {
            scriptClass =
                this.classLoader.parseClass(in, name.substring(1));
        } catch (CompilationFailedException e) {
        	LOG.error("Compilation error compiling GSP ["+name+"]:" + e.getMessage(), e);
            throw new GroovyPagesException("Could not parse script: " + name, e);
        }
        return scriptClass;
    }

    private GroovyPageMetaInfo createPageMetaInfo(Parse parse, long lastModified, InputStream in) {
        GroovyPageMetaInfo pageMeta = new GroovyPageMetaInfo();
        pageMeta.setContentType(parse.getContentType());
        pageMeta.setLineNumbers(parse.getLineNumberMatrix());
        pageMeta.setLastModified(lastModified);
            // just return groovy and don't compile if asked
        if (GrailsUtil.isDevelopmentEnv()) {
            pageMeta.setGroovySource(in);
        }

        return pageMeta;
    }

    private Resource optainResourceFromRequest() {
        GrailsWebRequest webRequest = getWebRequest();
        return (Resource)webRequest.getAttribute(GSP_TEMPLATE_RESOURCE, GrailsWebRequest.SCOPE_REQUEST);
    }

    private GrailsWebRequest getWebRequest() {
        return (GrailsWebRequest) RequestContextHolder.currentRequestAttributes();
    }

    private String establishPageName(Resource res) {
        String name;
        try {
            name = res.getFilename();
        } catch (IllegalStateException e) {
            name = generateTemplateName();
        }
        return name;
    }

    private String generateTemplateName() {
        return "gsp_script_"+ ++scriptNameCount;
    }


    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * Return the page identifier.
     * @param request The HttpServletRequest instance
     * @return The page id
     */
    protected String getCurrentRequestUri(HttpServletRequest request) {
        // Get the name of the Groovy script (intern the name so that we can
        // lock on it)
        Object includePath = request.getAttribute("javax.servlet.include.servlet_path");
        if (includePath != null) {
        	return ((String) includePath).intern();
        } else {
        	return request.getServletPath().intern();
        }
    }

    /**
     * Returns the path to the view of the relative URI within the Grails views directory
     *
     * @param relativeUri The relative URI
     * @return The path of the URI within the Grails view directory
     */
    protected String getUriWithinGrailsViews(String relativeUri) {
        StringBuffer buf = new StringBuffer();
        String[] tokens;
        if(relativeUri.startsWith("/"))
        		relativeUri = relativeUri.substring(1);


        if(relativeUri.indexOf('/')>-1)
            tokens = relativeUri.split("/");
        else
            tokens = new String[]{relativeUri};

        buf.append(GrailsApplicationAttributes.PATH_TO_VIEWS);
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            buf.append('/').append(token);

        }
        buf.append(GrailsApplicationAttributes.GSP_FILE_EXTENSION);
        return buf.toString();
    }

}
