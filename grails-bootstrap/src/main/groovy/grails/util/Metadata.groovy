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

import grails.config.ConfigMap
import grails.io.IOUtils
import groovy.transform.CompileStatic
import org.grails.config.NavigableMap
import org.grails.io.support.FileSystemResource
import org.grails.io.support.Resource
import org.grails.io.support.UrlResource
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor

import java.lang.ref.Reference
import java.lang.ref.SoftReference
import java.util.*

/**
 * Represents the application Metadata and loading mechanics.
 *
 * @author Graeme Rocher
 * @since 1.1
 */
@CompileStatic
class Metadata extends NavigableMap implements ConfigMap  {
    private static final long serialVersionUID = -582452926111226898L
    public static final String FILE = "application.yml"
    public static final String APPLICATION_VERSION = "info.app.version"
    public static final String APPLICATION_NAME = "info.app.name"
    public static final String APPLICATION_GRAILS_VERSION = "info.app.grailsVersion"
    public static final String SERVLET_VERSION = "info.app.servletVersion"
    public static final String WAR_DEPLOYED = "info.app.warDeployed"
    public static final String DEFAULT_SERVLET_VERSION = "3.0"

    private static Holder<Reference<Metadata>> holder = new Holder<Reference<Metadata>>("Metadata")
    public static final String BUILD_INFO_FILE = "META-INF/grails.build.info"

    private Resource metadataFile
    private boolean warDeployed
    private String servletVersion = DEFAULT_SERVLET_VERSION
    private Object source

    private Metadata() {
        loadFromDefault()
    }

    private Metadata(Resource res) {
        loadFromFile(res)
    }

    private Metadata(File f) {
        metadataFile = new FileSystemResource(f)
        loadFromFile(metadataFile)
    }

    private Metadata(InputStream inputStream) {
        loadFromInputStream(inputStream)
    }

    private Metadata(Map<String, String> properties) {
        merge(properties, true)
        afterLoading()
    }

    /**
     * @return The source of the metadata
     */
    Object getSource() {
        return source
    }

    Resource getMetadataFile() {
        return metadataFile
    }

    /**
     * Resets the current state of the Metadata so it is re-read.
     */
    static void reset() {
        Metadata m = getFromMap()
        if (m != null) {
            m.clear()
            m.afterLoading()
        }
    }

    private void afterLoading() {
        // allow override via system properties
        merge(new LinkedHashMap(System.properties).findAll { key, val -> val }, true)
        def value = get(WAR_DEPLOYED)
        warDeployed = value != null ? Boolean.valueOf(value.toString()) : false
    }

    /**
     * @return the metadata for the current application
     */
    static Metadata getCurrent() {
        Metadata m = getFromMap()
        if (m == null) {
            m = new Metadata()
            holder.set(new SoftReference<Metadata>(m))
        }
        return m
    }

    private void loadFromDefault() {
        try {
            def classLoader = Thread.currentThread().getContextClassLoader()
            URL url = classLoader.getResource(FILE)
            if (url == null) {
                url = getClass().getClassLoader().getResource(FILE)
            }
            if (url != null) {
                url.withInputStream { input ->
                    this.@source = loadYml(input)
                }
                this.metadataFile = new UrlResource(url)
            }

            url = classLoader.getResource(BUILD_INFO_FILE)
            if(url != null) {
                if(IOUtils.isWithinBinary(url) || !Environment.isDevelopmentEnvironmentAvailable()) {
                    url.withInputStream { input ->
                        loadAndMerge(input)
                    }
                }
            }
            else {
                // try WAR packaging resolve
                url = classLoader.getResource("../../" + BUILD_INFO_FILE)
                if(url != null) {
                    if(IOUtils.isWithinBinary(url) || !Environment.isDevelopmentEnvironmentAvailable()) {
                        url.withInputStream { input ->
                            loadAndMerge(input)
                        }
                    }
                }
            }
            afterLoading()
        }
        catch (Exception e) {
            throw new RuntimeException("Cannot load application metadata:" + e.getMessage(), e)
        }
    }

    private void loadAndMerge(InputStream input) {
        try {
            def props = new Properties()
            props.load(input)
            merge(props, true)
        } catch (Throwable e) {
            // ignore
        }
    }

    private Object loadYml(InputStream input) {
        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()))
        def loadedYaml = yaml.loadAll(input)
        List result = []
        for(Object yamlObject : loadedYaml) {
            if(yamlObject instanceof Map) { // problem here with CompileStatic
                result.add(yamlObject)
                merge((Map)yamlObject)
            }
        }

        return result
    }

    private void loadFromInputStream(InputStream inputStream) {
        loadYml(inputStream)
        afterLoading()
    }

    private void loadFromFile(Resource file) {
        if (file != null && file.exists()) {
            InputStream input = null
            try {
                input = file.getInputStream()
                loadYml(input)
                afterLoading()
            }
            catch (Exception e) {
                throw new RuntimeException("Cannot load application metadata:" + e.getMessage(), e)
            }
            finally {
                closeQuietly(input)
            }
        }
    }

    /**
     * Loads a Metadata instance from a Reader
     * @param inputStream The InputStream
     * @return a Metadata instance
     */
    static Metadata getInstance(InputStream inputStream) {
        Metadata m = new Metadata(inputStream)
        holder.set(new FinalReference<Metadata>(m))
        return m
    }

    /**
     * Loads and returns a new Metadata object for the given File.
     * @param file The File
     * @return A Metadata object
     */
    static Metadata getInstance(File file) {
        return getInstance(new FileSystemResource(file))
    }

    /**
     * Loads and returns a new Metadata object for the given File.
     * @param file The File
     * @return A Metadata object
     */
    static Metadata getInstance(Resource file) {
        Reference<Metadata> ref = holder.get()
        if (ref != null) {
            Metadata metadata = ref.get()
            if (metadata != null && metadata.getMetadataFile() != null && metadata.getMetadataFile().equals(file)) {
                return metadata
            }
            else {
                createAndBindNew(file)
            }
        }
        return createAndBindNew(file)
    }

    private static Metadata createAndBindNew(Resource file) {
        Metadata m = new Metadata(file)
        holder.set(new FinalReference<Metadata>(m))
        return m
    }

    /**
     * Reloads the application metadata.
     * @return The metadata object
     */
    static Metadata reload() {
        Resource f = getCurrent().getMetadataFile()
        if (f != null && f.exists()) {
            return getInstance(f)
        }

        return f == null ? new Metadata() : new Metadata(f)
    }

    /**
     * @return The application version
     */
    String getApplicationVersion() {
        return get(APPLICATION_VERSION)?.toString()
    }

    /**
     * @return The Grails version used to build the application
     */
    String getGrailsVersion() {
        return get(APPLICATION_GRAILS_VERSION)?.toString() ?: getClass().getPackage().getImplementationVersion()
    }

    /**
     * @return The environment the application expects to run in
     */
    String getEnvironment() {
        return get(Environment.KEY)?.toString()
    }

    /**
     * @return The application name
     */
    String getApplicationName() {
        return get(APPLICATION_NAME)?.toString()
    }


    /**
     * @return The version of the servlet spec the application was created for
     */
    String getServletVersion() {
        String servletVersion = get(SERVLET_VERSION)?.toString()
        if (servletVersion == null) {
            servletVersion = System.getProperty(SERVLET_VERSION) != null ? System.getProperty(SERVLET_VERSION) : this.servletVersion
            return servletVersion
        }
        return servletVersion
    }


    void setServletVersion(String servletVersion) {
        this.servletVersion = servletVersion
    }


    /**
     * @return true if this application is deployed as a WAR
     */
    boolean isWarDeployed() {
        Environment.isWarDeployed()
    }

    /**
     * @return True if the development sources are present
     */
    boolean isDevelopmentEnvironmentAvailable() {
        return Environment.isDevelopmentEnvironmentAvailable()
    }


    private static void closeQuietly(Closeable c) {
        if (c != null) {
            try {
                c.close()
            }
            catch (Exception ignored) {
                // ignored
            }
        }
    }

    private static Metadata getFromMap() {
        Reference<Metadata> metadata = holder.get()
        return metadata == null ? null : metadata.get()
    }



    static class FinalReference<T> extends SoftReference<T> {
        private final T ref

        FinalReference(T t) {
            super(t)
            ref = t
        }

        @Override
        T get() {
            return ref
        }
    }

    @Override
    <T> T getProperty(String key, Class<T> targetType) {
        return get(key)?.asType(targetType)
    }

    @Override
    <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
        def v = getProperty(key, targetType)
        if(v == null) {
            return defaultValue
        }
        return v
    }

    @Override
    <T> T getRequiredProperty(String key, Class<T> targetType) throws IllegalStateException {
        def value = get(key)
        if(value == null) {
            throw new IllegalStateException("Value for key ["+key+"] cannot be resolved")
        }
        return value.asType(targetType)
    }

    @Override
    Iterator<Map.Entry<String, Object>> iterator() {
        return entrySet().iterator()
    }
}
