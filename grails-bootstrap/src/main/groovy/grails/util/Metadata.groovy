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
package grails.util

import org.grails.config.CodeGenConfig
import groovy.transform.CompileStatic

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.*;

/**
 * Represents the application Metadata and loading mechanics.
 *
 * @author Graeme Rocher
 * @since 1.1
 */
@CompileStatic
public class Metadata extends CodeGenConfig  {
    private static final long serialVersionUID = -582452926111226898L;
    public static final String FILE = "application.yml";
    public static final String APPLICATION_VERSION = "info.app.version";
    public static final String APPLICATION_NAME = "info.app.name";
    public static final String APPLICATION_GRAILS_VERSION = "info.app.grailsVersion";
    public static final String SERVLET_VERSION = "info.app.servletVersion";
    public static final String WAR_DEPLOYED = "info.app.warDeployed";
    public static final String DEFAULT_SERVLET_VERSION = "2.5";

    private static Holder<Reference<Metadata>> holder = new Holder<Reference<Metadata>>("Metadata");
    public static final String BUILD_INFO_FILE = "META-INF/grails.build.info"

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

    private Metadata(Map<String, String> properties) {
        getConfigMap().putAll(properties);
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
            m.getConfigMap().clear();
            m.afterLoading();
        }
    }

    private void afterLoading() {
        def flatConfig = configMap.toFlatConfig()
        configMap.putAll(flatConfig)
        def map = [:]
        // allow override via system properties
        map.putAll(System.properties.findAll { it.value })
        configMap.putAll( map )
        warDeployed = getProperty(WAR_DEPLOYED, Boolean)
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
                loadYml(input);
            }

            input = Metadata.class.getClassLoader().getResourceAsStream(BUILD_INFO_FILE);
            if(input != null) {
                try {
                    def props = new Properties()
                    props.load(input)
                    mergeMap(props, true)
                } catch (Throwable e) {
                    // ignore
                }
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
        loadYml(inputStream);
        afterLoading();
    }

    private void loadFromFile(File file) {
        if (file != null && file.exists()) {
            FileInputStream input = null;
            try {
                input = new FileInputStream(file);
                loadYml(input);
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
        return getProperty(APPLICATION_VERSION, String.class);
    }

    /**
     * @return The Grails version used to build the application
     */
    public String getGrailsVersion() {
        return getProperty(APPLICATION_GRAILS_VERSION, String.class) ?: getClass().getPackage().getImplementationVersion();
    }

    /**
     * @return The environment the application expects to run in
     */
    public String getEnvironment() {
        return getProperty(Environment.KEY, String.class);
    }

    /**
     * @return The application name
     */
    public String getApplicationName() {
        return getProperty(APPLICATION_NAME, String.class);
    }


    /**
     * @return The version of the servlet spec the application was created for
     */
    public String getServletVersion() {
        String servletVersion = getProperty(SERVLET_VERSION, String.class);
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
     * @return true if this application is deployed as a WAR
     */
    public boolean isWarDeployed() {
        def loadedLocation = getClass().getClassLoader().getResource(FILE);
        if(loadedLocation && loadedLocation.path.contains('/WEB-INF/classes')) {
            return true
        }
        return false
    }

    /**
     * @return True if the development sources are present
     */
    boolean isDevelopmentEnvironmentAvailable() {
        return BuildSettings.GRAILS_APP_DIR_PRESENT;
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
