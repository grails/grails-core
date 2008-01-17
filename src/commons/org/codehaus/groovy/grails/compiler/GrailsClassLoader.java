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
import org.codehaus.groovy.grails.compiler.support.GrailsResourceLoader;
import org.codehaus.groovy.grails.compiler.injection.GrailsAwareClassLoader;
import org.codehaus.groovy.grails.exceptions.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * A GroovyClassLoader that supports reloading using inner class loaders
 *
 * @author Graeme Rocher
 * @since 0.6
 *
 *        <p/>
 *        Created: Jul 27, 2007
 *        Time: 11:53:02 AM
 */
public class GrailsClassLoader extends GroovyClassLoader {

    private ClassLoader parent;
    private GrailsResourceLoader grailsResourceLoader;
    private Map innerClassLoaderMap = new HashMap();

    public GrailsClassLoader(ClassLoader parent, CompilerConfiguration config, GrailsResourceLoader resourceLoader) {
        super(parent, config);
        this.parent = parent;
        this.grailsResourceLoader = resourceLoader;
    }

    public Class reloadClass(String name) {
        try {
            URL resourceURL = grailsResourceLoader.loadGroovySource(name);
            GroovyClassLoader innerLoader = new GrailsAwareClassLoader(parent);
            InputStream inputStream = null;

            try {
                inputStream = resourceURL.openStream();
                Class reloadedClass = innerLoader.parseClass(inputStream, name);
                innerClassLoaderMap.put(name, innerLoader);
                return reloadedClass;
            } catch (IOException e) {
                throw new CompilationFailedException("Error opening stream to class " + name + " with URL " + resourceURL, e);
            }
            finally {
                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } catch (IOException e) {
                   // ignore
                }
            }

        } catch (MalformedURLException e) {
            throw new CompilationFailedException("Error opening stream to class " + name + ":" + e.getMessage(), e);
        }
    }

    protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        GroovyClassLoader innerLoader = (GroovyClassLoader)innerClassLoaderMap.get(name);
        if(innerLoader != null) {
            return innerLoader.loadClass(name);
        }
        return super.loadClass(name, resolve);

    }

    public void setGrailsResourceLoader(GrailsResourceLoader resourceLoader) {
        this.grailsResourceLoader = resourceLoader;
    }
}
