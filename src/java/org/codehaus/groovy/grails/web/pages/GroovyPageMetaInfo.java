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

import grails.util.Environment;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.ApplicationHolder;
import org.codehaus.groovy.grails.commons.CodecArtefactHandler;
import org.codehaus.groovy.grails.commons.ConfigurationHolder;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsClass;
import org.codehaus.groovy.grails.web.pages.exceptions.GroovyPagesException;
import org.codehaus.groovy.grails.web.pages.ext.jsp.TagLibraryResolver;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.ReflectionUtils;

/**
 * Encapsulates the information necessary to describe a GSP.
 *
 * @author Graeme Rocher
 * @author Lari Hotari
 * @since 0.5
 */
class GroovyPageMetaInfo {

    private static final Log LOG=LogFactory.getLog(GroovyPageMetaInfo.class);
    private TagLibraryLookup tagLibraryLookup;
    private TagLibraryResolver jspTagLibraryResolver;

    private boolean precompiledMode=false;
    private Class<?> pageClass;
    private long lastModified;
    private InputStream groovySource;
    private String contentType;
    private int[] lineNumbers;
    private String[] htmlParts;
    @SuppressWarnings("rawtypes")
    private Map jspTags = Collections.EMPTY_MAP;
    private GroovyPagesException compilationException;
    private String codecName;
    private Class<?> codecClass;

    public static final String HTML_DATA_POSTFIX = "_html.data";
    public static final String LINENUMBERS_DATA_POSTFIX = "_linenumbers.data";
    
    private long latestLastModifiedCheck=0L;
    public static final long LASTMODIFIED_CHECK_INTERVAL =  Long.getLong("grails.gsp.reload.interval", 5000).longValue();
    private static final long LASTMODIFIED_CHECK_GRANULARITY =  Long.getLong("grails.gsp.reload.granularity", 2000).longValue();

    public GroovyPageMetaInfo() {
    	latestLastModifiedCheck=System.currentTimeMillis();
    }

    @SuppressWarnings("rawtypes")
    public GroovyPageMetaInfo(Class<?> pageClass) {
        precompiledMode=true;
        this.pageClass = pageClass;
        contentType = (String)ReflectionUtils.getField(ReflectionUtils.findField(pageClass, GroovyPageParser.CONSTANT_NAME_CONTENT_TYPE), null);
        jspTags = (Map)ReflectionUtils.getField(ReflectionUtils.findField(pageClass, GroovyPageParser.CONSTANT_NAME_JSP_TAGS), null);
        lastModified = (Long)ReflectionUtils.getField(ReflectionUtils.findField(pageClass, GroovyPageParser.CONSTANT_NAME_LAST_MODIFIED), null);
        codecName = (String)ReflectionUtils.getField(ReflectionUtils.findField(pageClass, GroovyPageParser.CONSTANT_NAME_DEFAULT_CODEC), null);
        initCodec();

        try {
            readHtmlData();
        }
        catch (IOException e) {
            throw new RuntimeException("Problem reading html data for page class " + pageClass, e);
        }
    }

    @SuppressWarnings("rawtypes")
    public void initCodec() {
        if (codecName == null) {
            Map config = ConfigurationHolder.getFlatConfig();
            if (config != null) {
                Object o = config.get(GroovyPageParser.CONFIG_PROPERTY_DEFAULT_CODEC);
                if (o != null) {
                    codecName = o.toString();
                }
            }
        }

        GrailsClass codecGrailsClass = null;
        GrailsApplication app = ApplicationHolder.getApplication();
        if (codecName != null) {
            if (app != null) {
                codecGrailsClass = app.getArtefactByLogicalPropertyName(
                        CodecArtefactHandler.TYPE, codecName);
                if (codecGrailsClass == null)
                    codecGrailsClass = app.getArtefactByLogicalPropertyName(
                            CodecArtefactHandler.TYPE, codecName.toUpperCase());
            }
        }

        if (codecGrailsClass == null && StringUtils.isNotBlank(codecName) && !"none".equalsIgnoreCase(codecName)) {
            LOG.warn("Couldn't initialize Codec by name '" + codecName + "' , pageClass=" + pageClass.getName());
        }
        if (codecGrailsClass!=null) {
            codecClass = codecGrailsClass.getClazz();
        }
    }

    /**
     * Reads the static html parts from a file stored in a separate file in the same package as the precompiled GSP class
     *
     * @throws IOException
     */
    private void readHtmlData() throws IOException {
        String dataResourceName = resolveDataResourceName(HTML_DATA_POSTFIX);

        DataInputStream input = null;
        try {
            input = new DataInputStream(pageClass.getResourceAsStream(dataResourceName));
            int arrayLen = input.readInt();
            htmlParts = new String[arrayLen];
            for(int i = 0; i < arrayLen; i++) {
                htmlParts[i] = input.readUTF();
            }
        }
        finally {
            IOUtils.closeQuietly(input);
        }
    }

    /**
     * reads the linenumber mapping information from a separate file that has been generated at precompile time
     *
     * @throws IOException
     */
    private void readLineNumbers() throws IOException {
        String dataResourceName = resolveDataResourceName(LINENUMBERS_DATA_POSTFIX);

        DataInputStream input = null;
        try {
            input = new DataInputStream(pageClass.getResourceAsStream(dataResourceName));
            int arrayLen = input.readInt();
            lineNumbers = new int[arrayLen];
            for (int i = 0; i < arrayLen; i++) {
                lineNumbers[i] = input.readInt();
            }
        }
        finally {
            IOUtils.closeQuietly(input);
        }
    }

    /**
     * resolves the file name for html and linenumber data files
     * the file name is the classname + POSTFIX
     *
     * @param postfix
     * @return
     */
    private String resolveDataResourceName(String postfix) {
        String dataResourceName = pageClass.getName();
        int pos = dataResourceName.lastIndexOf('.');
        if (pos > -1) {
            dataResourceName = dataResourceName.substring(pos+1);
        }
        dataResourceName += postfix;
        return dataResourceName;
    }

    public TagLibraryLookup getTagLibraryLookup() {
        return tagLibraryLookup;
    }

    public void setTagLibraryLookup(TagLibraryLookup tagLibraryLookup) {
        this.tagLibraryLookup = tagLibraryLookup;
    }

    public TagLibraryResolver getJspTagLibraryResolver() {
        return jspTagLibraryResolver;
    }

    public void setJspTagLibraryResolver(TagLibraryResolver jspTagLibraryResolver) {
        this.jspTagLibraryResolver = jspTagLibraryResolver;
    }

    public Class<?> getPageClass() {
        return pageClass;
    }

    public void setPageClass(Class<?> pageClass) {
        this.pageClass = pageClass;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public InputStream getGroovySource() {
        return groovySource;
    }

    public void setGroovySource(InputStream groovySource) {
        this.groovySource = groovySource;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public int[] getLineNumbers() {
        if (precompiledMode) {
            return getPrecompiledLineNumbers();
        }

        return lineNumbers;
    }

    private synchronized int[] getPrecompiledLineNumbers() {
        if (lineNumbers == null) {
            try {
                readLineNumbers();
            }
            catch (IOException e) {
                LOG.warn("Problem reading precompiled linenumbers", e);
            }
        }
        return lineNumbers;
    }

    public void setLineNumbers(int[] lineNumbers) {
        this.lineNumbers = lineNumbers;
    }

    @SuppressWarnings("rawtypes")
    public void setJspTags(Map jspTags) {
        this.jspTags = jspTags != null ? jspTags : Collections.EMPTY_MAP;
    }

    @SuppressWarnings("rawtypes")
    public Map getJspTags() {
        return jspTags;
    }

    public void setCompilationException(GroovyPagesException e) {
        compilationException = e;
    }

    public GroovyPagesException getCompilationException() {
        return compilationException;
    }

    public String[] getHtmlParts() {
        return htmlParts;
    }

    public void setHtmlParts(String[] htmlParts) {
        this.htmlParts = htmlParts;
    }

    public Class<?> getCodecClass() {
        return codecClass;
    }

    public String getCodecName() {
        return codecName;
    }

    public void setCodecName(String codecName) {
        this.codecName = codecName;
    }

    public void applyLastModifiedFromResource(Resource resource) {
    	this.lastModified = establishLastModified(resource);
    }
    
    /**
     * Attempts to establish what the last modified date of the given resource is. If the last modified date cannot
     * be etablished -1 is returned
     *
     * @param resource The Resource to evaluate
     * @return The last modified date or -1
     */
    private long establishLastModified(Resource resource) {
        if (resource == null) return -1;
        
        if(resource instanceof FileSystemResource) {
    		return ((FileSystemResource)resource).getFile().lastModified();
        }
        
        long lastModified;
        URLConnection urlc = null;

        try {
            URL url=resource.getURL();
            if("file".equals(url.getProtocol())) {
            	File file=new File(url.getFile());
            	if(file.exists()) {
            		return file.lastModified();
            	}
            }
        	urlc = url.openConnection();
            urlc.setDoInput(false);
            urlc.setDoOutput(false);
            lastModified = urlc.getLastModified();
        }
        catch (FileNotFoundException fnfe) {
            lastModified = -1;
        }
        catch (IOException e) {
            lastModified = -1;
        }
        finally {
            if (urlc != null) {
                try {
                    InputStream is = urlc.getInputStream();
                    if (is != null) {
                        is.close();
                    }
                }
                catch (IOException e) {
                    // ignore
                }
            }
        }

        return lastModified;
    }

	/**
	 * Checks if this GSP has expired and should be reloaded (there is a newer source gsp available)
	 * PrivilegedAction is used so that locating the Resource is lazily evaluated.
	 * 
	 * lastModified checking is done only when enough time has expired since the last check. This setting is controlled by the grails.gsp.reload.interval System property,
	 * by default it's value is 5000 (ms).
	 * 
	 * @param resourceCallable call back that resolves the source gsp lazily
	 * @return true if the available gsp source file is newer than the loaded one.
	 */
	public synchronized boolean shouldReload(PrivilegedAction<Resource> resourceCallable) {
		long now=System.currentTimeMillis();
		if(Environment.isDevelopmentMode() || now - latestLastModifiedCheck > LASTMODIFIED_CHECK_INTERVAL) {
			latestLastModifiedCheck=now;
			Resource resource=resourceCallable.run();
			if(resource != null && resource.exists()) {
				long currentLastmodified=establishLastModified(resource);
				// granularity is required since lastmodified information is rounded some where in copying & war (zip) file information
				// usually the lastmodified time is 1000L apart in files and in files extracted from the zip (war) file
				if(currentLastmodified > 0 && Math.abs(currentLastmodified-lastModified) > LASTMODIFIED_CHECK_GRANULARITY) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean isPrecompiledMode() {
		return precompiledMode;
	}
    
}
