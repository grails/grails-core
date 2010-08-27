/* Copyright 2006-2007 Graeme Rocher
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
package org.codehaus.groovy.grails.compiler;

import groovy.lang.GroovyClassLoader;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.IOUtils;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.grails.compiler.injection.GrailsAwareClassLoader;
import org.codehaus.groovy.grails.compiler.support.GrailsResourceLoader;
import org.codehaus.groovy.grails.exceptions.CompilationFailedException;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

/**
 * A GroovyClassLoader that supports reloading using inner class loaders
 *
 * @author Graeme Rocher
 * @since 0.6
 */
public class GrailsClassLoader extends GroovyClassLoader {

    private GrailsResourceLoader grailsResourceLoader;
    private Map<String, GroovyClassLoader> innerClassLoaderMap = new ConcurrentHashMap<String, GroovyClassLoader>();
    private Map<String,MultipleCompilationErrorsException> compilationErrors = new ConcurrentHashMap<String,MultipleCompilationErrorsException>();

    public GrailsClassLoader() {
        // default
    }

    public GrailsClassLoader(ClassLoader parent, CompilerConfiguration config, GrailsResourceLoader resourceLoader) {
        super(parent, config);
        this.grailsResourceLoader = resourceLoader;
    }

    public boolean hasCompilationErrors() {
        return !compilationErrors.isEmpty();
    }

    public MultipleCompilationErrorsException getCompilationError() {
        if (hasCompilationErrors()) {
            return compilationErrors.values().iterator().next();
        }
        return null;
    }

    public Class<?> reloadClass(String name)  {
        try {
            Resource resourceURL = loadGroovySource(name);
            if(resourceURL != null) {

                GroovyClassLoader innerLoader = new GrailsAwareClassLoader(this);
                InputStream inputStream = null;
                clearCache();

                try {
                    inputStream = resourceURL.getInputStream();
                    Class<?> reloadedClass = innerLoader.parseClass(
                            DefaultGroovyMethods.getText(inputStream), name);
                    compilationErrors.remove(name);
                    innerClassLoaderMap.put(name, innerLoader);
                    return reloadedClass;
                }
                catch (MultipleCompilationErrorsException e) {
                    compilationErrors.put(name, e);
                    throw e;
                }
                catch (IOException e) {
                    throw new CompilationFailedException("Error opening stream to class " + name + " with URL " + resourceURL, e);
                }
                finally {
                    IOUtils.closeQuietly(inputStream);
                }
            }
            return null;
        }
        catch (MalformedURLException e) {
            throw new CompilationFailedException("Error opening stream to class " + name + ":" + e.getMessage(), e);
        }
    }

    protected Resource loadGroovySource(String name) throws MalformedURLException {
        URL resourceURL = grailsResourceLoader.loadGroovySource(name);
        if(resourceURL != null) {
            return new UrlResource(resourceURL);
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        GroovyClassLoader innerLoader = innerClassLoaderMap.get(name);
        if (innerLoader != null) {
            return innerLoader.loadClass(name);
        }
        return super.loadClass(name, resolve);
    }

    public void setGrailsResourceLoader(GrailsResourceLoader resourceLoader) {
        this.grailsResourceLoader = resourceLoader;
    }
}
