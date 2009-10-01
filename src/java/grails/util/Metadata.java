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
package grails.util;

import java.io.*;
import java.util.*;

/**
 * Represents the application Metadata and loading mechanics
 *
 * @author Graeme Rocher
 * @since 1.1
 * 
 * Created: Dec 12, 2008
 */

public class Metadata extends Properties {

    public static final String FILE = "application.properties";
    public static final String APPLICATION_VERSION = "app.version";
    public static final String APPLICATION_NAME = "app.name";
    public static final String APPLICATION_GRAILS_VERSION = "app.grails.version";
    public static final String SERVLET_VERSION = "app.servlet.version";
    public static final String WAR_DEPLOYED = "grails.war.deployed";
    public static final String DEFAULT_SERVLET_VERSION = "2.4";
    
    private boolean initialized;
    private static Metadata metadata = new Metadata();
    private static File metadataFile;

    private Metadata() {
        super();
    }

    /**
     * Resets the current state of the Metadata so it is re-read
     */
    public static void reset() {
    	metadata.clear();
    	metadata.initialized=false;
    }
    /**
     * @return Returns the metadata for the current application
     */
    public static Metadata getCurrent() {
        if(!metadata.initialized) {
        	InputStream input=null;
            try {
                input = Thread.currentThread().getContextClassLoader().getResourceAsStream(FILE);
                if(input == null) {
                    input = Metadata.class.getClassLoader().getResourceAsStream(FILE);
                }
                if(input != null) {
                    metadata.load(input);
                }
            }
            catch (Exception e) {
                throw new RuntimeException("Cannot load application metadata:" + e.getMessage(), e);
            }
            finally {
                try {
                    if(input!=null) input.close();
                }
                catch (IOException e) {
                    // ignore
                }
                metadata.initialized = true;
            }

        }

        return metadata;
    }

    /***
     * Loads a Metadata instance from a Reader
     * @param inputStream The InputStream
     * @return a Metadata instance
     */
    public static Metadata getInstance(InputStream inputStream) {
        metadata = new Metadata();

        try {
            metadata.load(inputStream);
            metadata.initialized=true;
        }
        catch (IOException e) {
            throw new RuntimeException("Cannot load application metadata:" + e.getMessage(), e);
        }
        return metadata;
    }

    /**
     * Loads and returns a new Metadata object for the given File
     * @param file The File
     * @return A Metadata object
     */
    public static Metadata getInstance(File file) {
        metadata = new Metadata();
        metadataFile = file;

        if(file!= null && file.exists()) {

            FileInputStream input = null;
            try {
                input = new FileInputStream(file);
                metadata.load(input);
                metadata.initialized = true;
            }
            catch (Exception e) {
                throw new RuntimeException("Cannot load application metadata:" + e.getMessage(), e);
            }
            finally {
                try {
                    if(input!=null) input.close();
                }
                catch (IOException e) {
                    // ignore
                }
            }
        }
        return metadata;
    }

    /**
     * Reloads the application metadata
     * @return The metadata object
     */
    public static Metadata reload() {
        if(metadataFile!=null)
            metadata = getInstance(metadataFile);
        return metadata;
    }

    /**
     * @return The application version
     */
    public String getApplicationVersion() {
        return (String) get(APPLICATION_VERSION);
    }

    /**
     * @return The Grails version used to build the application
     */
    public String getGrailsVersion() {
        return (String) get(APPLICATION_GRAILS_VERSION);
    }


    /**
     * @return The environment the application expects to run in
     */
    public String getEnvironment() {
        return (String) get(Environment.KEY);        
    }

    /**
     * @return The application name
     */
    public String getApplicationName() {
        return (String) get(APPLICATION_NAME);
    }

    /**
     * @return The version of the servlet spec the application was created for
     */
    public String getServletVersion() {
        final String servletVersion = (String) get(SERVLET_VERSION);
        if(servletVersion == null) {
            return DEFAULT_SERVLET_VERSION;
        }
        return servletVersion;
    }

    /**
     * Saves the current state of the Metadata object
     */
    public void persist() {

        if (propertiesHaveNotChanged())
	        return;	

        if(metadataFile != null) {
            FileOutputStream out = null;

            try {
                out = new FileOutputStream(metadataFile);
                store(out, "Grails Metadata file");
            }
            catch (Exception e) {
                throw new RuntimeException("Error persisting metadata to file ["+metadataFile+"]: " + e.getMessage(),e );
            }
            finally {
                try {
                    if(out != null) out.close();
                }
                catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    /**
     * @return Returns true if these properties have not changed since they were loaded
     */	
    public boolean propertiesHaveNotChanged(){
        Metadata transientMetadata = metadata;
        
        Metadata allStringValuesMetadata = new Metadata();
        Map<Object,Object> transientMap = (Map<Object,Object>)transientMetadata;
        for(Map.Entry<Object, Object> entry : transientMap.entrySet()) {
        	if(entry.getValue() != null) {
        		allStringValuesMetadata.put(entry.getKey().toString(), entry.getValue().toString());
        	}
        }

        Metadata persistedMetadata = Metadata.reload();	
        boolean result = allStringValuesMetadata.equals(persistedMetadata);
        metadata = transientMetadata;
        return result;
    }	

    /**
     * Overrides, called by the store method.
     */
    @SuppressWarnings("unchecked")
    public synchronized Enumeration keys() {
        Enumeration keysEnum = super.keys();
        Vector keyList = new Vector();
        while(keysEnum.hasMoreElements()){
            keyList.add(keysEnum.nextElement());
        }
        Collections.sort(keyList);
        return keyList.elements();
    }
    
    /**
     * @return Returns true if this application is deployed as a WAR
     */
    public boolean isWarDeployed() {
        Object val = get(WAR_DEPLOYED);
        return val != null && val.equals("true");
    }
}
