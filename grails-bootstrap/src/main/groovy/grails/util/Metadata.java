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

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

/**
 * Represents the application Metadata and loading mechanics.
 *
 * @author Graeme Rocher
 * @since 1.1
 */
public class Metadata extends Properties {
    private static final long serialVersionUID = -582452926111226898L;
    public static final String FILE = "application.properties";
    public static final String APPLICATION_VERSION = "app.version";
    public static final String APPLICATION_NAME = "app.name";
    public static final String APPLICATION_GRAILS_VERSION = "app.grails.version";
    public static final String SERVLET_VERSION = "app.servlet.version";
    public static final String WAR_DEPLOYED = "grails.war.deployed";
    public static final String DEFAULT_SERVLET_VERSION = "2.5";

    private static Holder<Reference<Metadata>> holder = new Holder<Reference<Metadata>>("Metadata");

    private File metadataFile;
    private boolean warDeployed;
    private String servletVersion = DEFAULT_SERVLET_VERSION;

    private Metadata() {
        loadFromDefault();
    }

    private Metadata(File f) {
        metadataFile = f;
        loadFromFile(f);
    }

    private Metadata(InputStream inputStream) {
        loadFromInputStream(inputStream);
    }

    private Metadata(Map<?, ?> properties) {
        putAll(properties);
        afterLoading();
    }

    public File getMetadataFile() {
        return metadataFile;
    }

    /**
     * Resets the current state of the Metadata so it is re-read.
     */
    public static void reset() {
        Metadata m = getFromMap();
        if (m != null) {
            m.clear();
            m.afterLoading();
        }
    }

    private void afterLoading() {
        warDeployed = "true".equals(get(WAR_DEPLOYED));
    }

    /**
     * @return the metadata for the current application
     */
    public static Metadata getCurrent() {
        Metadata m = getFromMap();
        if (m == null) {
            m = new Metadata();
            holder.set(new SoftReference<Metadata>(m));
        }
        return m;
    }

    private void loadFromDefault() {
        InputStream input = null;
        try {
            input = Thread.currentThread().getContextClassLoader().getResourceAsStream(FILE);
            if (input == null) {
                input = Metadata.class.getClassLoader().getResourceAsStream(FILE);
            }
            if (input != null) {
                load(input);
            }
            afterLoading();
        }
        catch (Exception e) {
            throw new RuntimeException("Cannot load application metadata:" + e.getMessage(), e);
        }
        finally {
            closeQuietly(input);
        }
    }

    private void loadFromInputStream(InputStream inputStream) {
        try {
            load(inputStream);
            afterLoading();
        }
        catch (IOException e) {
            throw new RuntimeException("Cannot load application metadata:" + e.getMessage(), e);
        }
    }

    private void loadFromFile(File file) {
        if (file != null && file.exists()) {
            FileInputStream input = null;
            try {
                input = new FileInputStream(file);
                load(input);
                afterLoading();
            }
            catch (Exception e) {
                throw new RuntimeException("Cannot load application metadata:" + e.getMessage(), e);
            }
            finally {
                closeQuietly(input);
            }
        }
    }

    /**
     * Loads a Metadata instance from a Reader
     * @param inputStream The InputStream
     * @return a Metadata instance
     */
    public static Metadata getInstance(InputStream inputStream) {
        Metadata m = new Metadata(inputStream);
        holder.set(new FinalReference<Metadata>(m));
        return m;
    }

    /**
     * Loads and returns a new Metadata object for the given File.
     * @param file The File
     * @return A Metadata object
     */
    public static Metadata getInstance(File file) {
        Reference<Metadata> ref = holder.get();
        if (ref != null) {
            Metadata metadata = ref.get();
            if (metadata != null && metadata.getMetadataFile() != null && metadata.getMetadataFile().equals(file)) {
                return metadata;
            }
            createAndBindNew(file);
        }
        return createAndBindNew(file);
    }

    private static Metadata createAndBindNew(File file) {
        Metadata m = new Metadata(file);
        holder.set(new FinalReference<Metadata>(m));
        return m;
    }

    /**
     * Reloads the application metadata.
     * @return The metadata object
     */
    public static Metadata reload() {
        File f = getCurrent().metadataFile;
        if (f != null && f.exists()) {
            return getInstance(f);
        }

        return f == null ? new Metadata() : new Metadata(f);
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
    public void setGrailsVersion(String version) {
        put(APPLICATION_GRAILS_VERSION, version);
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
     * Obtains a map (name->version) of installed plugins specified in the project metadata
     * @return A map of installed plugins
     */
    public Map<String, String> getInstalledPlugins() {
        Map<String, String> newMap = new LinkedHashMap<String, String>();

        for (Map.Entry<Object, Object> entry : entrySet()) {
            String key = entry.getKey().toString();
            Object val = entry.getValue();
            if (key.startsWith("plugins.") && val != null) {
                newMap.put(key.substring(8), val.toString());
            }
        }
        return newMap;
    }

    /**
     * @return The version of the servlet spec the application was created for
     */
    public String getServletVersion() {
        String servletVersion = (String) get(SERVLET_VERSION);
        if (servletVersion == null) {
            servletVersion = System.getProperty(SERVLET_VERSION) != null ? System.getProperty(SERVLET_VERSION) : this.servletVersion;
            return servletVersion;
        }
        return servletVersion;
    }


    public void setServletVersion(String servletVersion) {
        this.servletVersion = servletVersion;
    }

    /**
     * Saves the current state of the Metadata object.
     */
    public void persist() {

        if (propertiesHaveNotChanged() || metadataFile == null) {
            return;
        }

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(metadataFile);
            store(out, "Grails Metadata file");
        }
        catch (Exception e) {
            throw new RuntimeException("Error persisting metadata to file [" + metadataFile + "]: " + e.getMessage(), e);
        }
        finally {
            closeQuietly(out);
        }
    }

    /**
     * @return Returns true if these properties have not changed since they were loaded
     */
    public boolean propertiesHaveNotChanged() {
        Metadata allStringValuesMetadata = new Metadata();
        for (Map.Entry<Object, Object> entry : entrySet()) {
            if (entry.getValue() != null) {
                allStringValuesMetadata.put(entry.getKey().toString(), entry.getValue().toString());
            }
        }

        return allStringValuesMetadata.equals(new Metadata(metadataFile));
    }

    /**
     * Overrides, called by the store method.
     */
    @Override
    @SuppressWarnings({"unchecked","rawtypes"})
    public synchronized Enumeration keys() {
        Enumeration keysEnum = super.keys();
        Vector keyList = new Vector();
        while (keysEnum.hasMoreElements()) {
            keyList.add(keysEnum.nextElement());
        }
        Collections.sort(keyList);
        return keyList.elements();
    }

    /**
     * @return true if this application is deployed as a WAR
     */
    public boolean isWarDeployed() {
        return warDeployed;
    }

    @Override
    public synchronized Object put(Object key, Object o2) {
        try {
            return super.put(key, o2);
        } finally {
            afterLoading();
        }
    }

    private static void closeQuietly(Closeable c) {
        if (c != null) {
            try {
                c.close();
            }
            catch (Exception ignored) {
                // ignored
            }
        }
    }

    private static Metadata getFromMap() {
        Reference<Metadata> metadata = holder.get();
        return metadata == null ? null : metadata.get();
    }

    static class FinalReference<T> extends SoftReference<T> {
        private T ref;
        public FinalReference(T t) {
            super(t);
            ref = t;
        }

        @Override
        public T get() {
            return ref;
        }
    }
}
