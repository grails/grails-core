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

import grails.util.GrailsUtil;
import groovy.lang.GroovyClassLoader;
import groovy.text.Template;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.grails.commons.GrailsResourceUtils;
import org.codehaus.groovy.grails.support.ResourceAwareTemplateEngine;
import org.codehaus.groovy.grails.web.pages.exceptions.GroovyPagesException;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.runtime.metaclass.ConcurrentReaderHashMap;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.*;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.support.ServletContextResourceLoader;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

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
public class GroovyPagesTemplateEngine  extends ResourceAwareTemplateEngine implements ApplicationContextAware, ServletContextAware {


    private static final Log LOG = LogFactory.getLog(GroovyPagesTemplateEngine.class);
    private static Map pageCache = new ConcurrentReaderHashMap();
    private GroovyClassLoader classLoader = new GroovyClassLoader();
    private int scriptNameCount;
    private ResourceLoader resourceLoader;
    
    public static final String BEAN_ID = "groovyPagesTemplateEngine";
    public static final String RESOURCE_LOADER_BEAN_ID = "groovyPagesResourceLoader";
    private boolean reloadEnabled;
    private ServletContext servletContext;
    private ServletContextResourceLoader servletContextLoader;

    public GroovyPagesTemplateEngine() {
    }

    public GroovyPagesTemplateEngine(ServletContext servletContext) {
        if(servletContext == null) throw new IllegalArgumentException("Argument [servletContext] cannot be null");
        this.resourceLoader = new ServletContextResourceLoader(servletContext);
        this.servletContext = servletContext;
        this.servletContextLoader = new ServletContextResourceLoader(servletContext);

    }

    /**
     * Sets the ClassLoader that the TemplateEngine should use to
     * @param classLoader The ClassLoader to use when compilation of Groovy Pages occurs
     */
    public void setClassLoader(GroovyClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Sets a custom ResourceLoader that will be used to load GSPs for URIs
     * 
     * @param resourceLoader The ResourceLoader instance
     */
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
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
                InputStream inputStream = r.getInputStream();
                try {
                    GroovyPageMetaInfo metaInfo = buildPageMetaInfo(inputStream,r, null);
                    if(metaInfo!= null) {
                        return metaInfo.getLineNumbers();
                    }
                } finally {
                    inputStream.close();
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
        String name = establishPageName(resource, null);
        if(pageCache.containsKey(name)) {
            GroovyPageMetaInfo meta = (GroovyPageMetaInfo)pageCache.get(name);

            if(isGroovyPageReloadable(resource, meta)) {
                try {
                    return createTemplateWithResource(resource);
                } catch (IOException e) {
                    throw new GroovyPagesException("I/O error reading stream for resource ["+resource+"]: " + e.getMessage(),e);
                }
            }
            else {
                return new GroovyPageTemplate(meta);
            }
        }
        else {
            try {
                return createTemplateWithResource(resource);
            } catch (IOException e) {
                throw new GroovyPagesException("I/O error reading stream for resource ["+resource+"]: " + e.getMessage(),e);
            }
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
     * @throws java.io.IOException Thrown if an IO exception occurs creating the Template

     */
    public Template createTemplate(String txt, String pageName) throws IOException {
        if(StringUtils.isBlank(txt)) throw new IllegalArgumentException("Argument [txt] cannot be null or blank");
        if(StringUtils.isBlank(pageName)) throw new IllegalArgumentException("Argument [pageName] cannot be null or blank");
        
        return createTemplate(new ByteArrayResource(txt.getBytes(), pageName), pageName);
    }

    private Template createTemplate(Resource resource, String pageName) throws IOException {
        InputStream in = resource.getInputStream();
        try {
            return createTemplate(in, resource, pageName);
        }
        finally {
           in.close();
        }
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

    /**
     * Creates a Template for the given file
     *
     * @param file The File to use to construct the template with
     * @return A Groovy Template instance
     *
     * @throws CompilationFailedException When an error occured compiling the Template
     * @throws ClassNotFoundException When a Class cannot be found within the given Template
     * @throws IOException When a I/O Exception occurs reading the Template
     */
    public Template createTemplate(File file) throws CompilationFailedException, ClassNotFoundException, IOException {
        return createTemplate(new FileSystemResource(file));
    }
    /**
     * Creates a Template for the given URL
     *
     * @param url The URL to use to construct the template with
     * @return A Groovy Template instance
     *
     * @throws CompilationFailedException When an error occured compiling the Template
     * @throws ClassNotFoundException When a Class cannot be found within the given Template
     * @throws IOException When a I/O Exception occurs reading the Template
     */
    public Template createTemplate(URL url) throws CompilationFailedException, ClassNotFoundException, IOException {
        return createTemplate(new UrlResource(url));
    }

    /**
     * Create a Template for the given InputStream
     * @param inputStream The InputStream to create the Template for
     * @return The Template instance
     */
    public Template createTemplate(InputStream inputStream) {

        GroovyPageMetaInfo metaInfo = buildPageMetaInfo(inputStream, null, null);

        return new GroovyPageTemplate(metaInfo);
    }

    /**
     * Creates a Template for the given Spring Resource instance
     *
     * @param resource The Spring resource instance
     * @return A Groovy Template
     * @throws java.io.IOException Thrown when an error occurs reading the template
     */
    private Template createTemplateWithResource(Resource resource) throws IOException {
        InputStream in = resource.getInputStream();
        try {
            return createTemplate(in, resource, null);
        }
        finally {
            in.close();
        }
    }

    /**
     * Constructs a Groovy Template from the given InputStream and Spring Resource object
     *
     * @param inputStream The InputStream to use
     * @param resource The Resource to use
     * @param pageName The name of the page
     * @return The Groovy Template
     */
    protected Template createTemplate(InputStream inputStream, Resource resource, String pageName) {
        GroovyPageMetaInfo metaInfo = buildPageMetaInfo(inputStream, resource, pageName);
        return new GroovyPageTemplate(metaInfo);
    }

    /**
     * Establishes whether a Groovy page is reloadable. A GSP is only reloadable in the development environment.
     *
     * @param resource The Resource to check.
     * @param meta The current GroovyPageMetaInfo instance
     * @return True if it is reloadable
     */
    private boolean isGroovyPageReloadable(Resource resource, GroovyPageMetaInfo meta) {
        return isReloadEnabled() && (establishLastModified(resource) > meta.getLastModified());
    }

    /**
     * Return whether reload is enabled for the GroovyPagesTemplateEngine
     *
     * @return True if it is
     */
    public boolean isReloadEnabled() {
        return this.reloadEnabled;
    }

    /**
     * Sets whether reloading is enabled
     *
     * @param b True if it is enabled
     */
    public void setReloadEnabled(boolean b) {
        this.reloadEnabled = true;
    }

    /**
     * Attempts to retrieve a reference to a GSP as a Spring Resource instance for the given URI.
     *
     * @param uri The URI to check
     * @return A Resource instance
     */
    public Resource getResourceForUri(String uri) {
        Resource r;
        r = getResourceWithinContext(uri);
        if(r == null || !r.exists()) {
            // try plugin
            String pluginUri = GrailsResourceUtils.WEB_INF + uri;
            r = getResourceWithinContext(pluginUri);
            if(r == null || !r.exists()) {                
                uri = getUriWithinGrailsViews(uri);
                return getResourceWithinContext(uri);
            }
        }
        return r;
    }

    private Resource getResourceWithinContext(String uri) {
        if(resourceLoader == null) throw new IllegalStateException("TemplateEngine not initialised correctly, no [resourceLoader] specified!");
        Resource r = servletContextLoader.getResource(uri);
        if(r.exists()) return r;
        return resourceLoader.getResource(uri);
    }


    /**
     * Attempts to establish what the last modified date of the given resource is. If the last modified date cannot
     * be etablished -1 is returned
     *
     * @param resource The Resource to evaluate
     * @return The last modified date or -1
     */
    private long establishLastModified(Resource resource) {
        if(resource ==null)return -1;
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


    /**
     * Constructs a GroovyPageMetaInfo instance which holds the script class, modified date and so on
     *
     * @param inputStream The InputStream to construct the GroovyPageMetaInfo instance from
     * @param res The Spring Resource to construct the MetaInfo from
     * @param pageName The name of the page (can be null, in which case method responsible for calculating appropriate alternative)
     * @return The GroovyPageMetaInfo instance
     */
    protected GroovyPageMetaInfo buildPageMetaInfo(InputStream inputStream, Resource res, String pageName) {
        String name = establishPageName(res, pageName);

        long lastModified = establishLastModified(res);

        Parse parse;
        try {
            parse = new Parse(name, res.getDescription(), inputStream);
        } catch (IOException e) {
            throw new GroovyPagesException("I/O parsing Groovy page ["+(res != null ? res.getDescription() : name)+"]: " + e.getMessage(),e);
        }
        InputStream in = parse.parse();

        // Make a new metaInfo
        GroovyPageMetaInfo metaInfo = createPageMetaInfo(parse, lastModified, in);
        metaInfo.setPageClass( compileGroovyPage(in, name) );

        pageCache.put(name, metaInfo);

        return metaInfo;
    }

    /**
     * Attempts to compile the given InputStream into a Groovy script using the given name
     * @param in The InputStream to read the Groovy code from
     * @param name The name of the class to use
     *
     * @return The compiled java.lang.Class, which is an instance of groovy.lang.Script
     */
    private Class compileGroovyPage(InputStream in, String name) {
        // Compile the script into an object
        Class scriptClass;
        try {
            scriptClass =
                this.classLoader.parseClass(in, name);
        } catch (CompilationFailedException e) {
        	LOG.error("Compilation error compiling GSP ["+name+"]:" + e.getMessage(), e);
            throw new GroovyPagesException("Could not parse script [" + name + "]: " + e.getMessage(), e);
        }
        return scriptClass;
    }

    /**
     * Creates a GroovyPageMetaInfo instance from the given Parse object, and initialises it with the the specified
     * last modifed date and InputStream
     *
     * @param parse The Parse object
     * @param lastModified The last modified date
     * @param in The InputStream instance
     * @return A GroovyPageMetaInfo instance
     */
    private GroovyPageMetaInfo createPageMetaInfo(Parse parse, long lastModified, InputStream in) {
        GroovyPageMetaInfo pageMeta = new GroovyPageMetaInfo();
        pageMeta.setContentType(parse.getContentType());
        pageMeta.setLineNumbers(parse.getLineNumberMatrix());
        pageMeta.setLastModified(lastModified);
            // just return groovy and don't compile if asked
        if (isReloadEnabled() || GrailsUtil.isDevelopmentEnv()) {
            pageMeta.setGroovySource(in);
        }

        return pageMeta;
    }


    private GrailsWebRequest getWebRequest() {
        return (GrailsWebRequest) RequestContextHolder.currentRequestAttributes();
    }

    /**
     * Establishes the name to use for the given resource
     *
     * @param res The Resource to calculate the name for
     * @param pageName The name of the page, can be null, in which case method responsible for calculation
     * 
     * @return  The name as a String
     */
    protected String establishPageName(Resource res, String pageName) {
        if(res == null) {
            return generateTemplateName();
        }
        String name;
        try {
            name = pageName != null ? pageName : res.getURL().getPath();
            // As the name take the first / off and then replace all characters that aren't
            // a word character or a digit with an underscore
            if(name.startsWith("/")) name = name.substring(1);
            name = name.replaceAll("[^\\w\\d]", "_");

        } catch (IllegalStateException e) {
            name = generateTemplateName();
        }
        catch (IOException ioex) {
            name = generateTemplateName();
        }
        return name;
    }

    /**
     * Generates the template name to use if it cannot be established from the Resource
     *
     * @return The template name
     */
    private String generateTemplateName() {
        return "gsp_script_"+ ++scriptNameCount;
    }

    /**
     * Sets the ResourceLoader from the ApplicationContext
     *
     * @param applicationContext The ApplicationContext
     * @throws BeansException Thrown when an error occurs with the ApplicationContext
     */
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if(this.resourceLoader == null) {
            this.resourceLoader = applicationContext;
        }
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
        if(!relativeUri.endsWith(GroovyPage.EXTENSION))
            buf.append(GroovyPage.EXTENSION);
        return buf.toString();
    }

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
        this.servletContextLoader = new ServletContextResourceLoader(servletContext);        
        if(this.resourceLoader == null)
            this.resourceLoader = new ServletContextResourceLoader(servletContext);
    }

    /**
     * Clears the page cache. Views will be re-compiled.
     */
    public void clearPageCache() {
        pageCache.clear();
    }
}
