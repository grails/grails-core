/*
 * Copyright 2012 the original author or authors.
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

package org.codehaus.groovy.grails.cli.support

import groovy.transform.CompileStatic

/**
 * Based on  http://stackoverflow.com/a/6424879/166062
 *
 * Loads classes from the child first, before trying parent
 *
 * @author Graeme Rocher
 *
 */
@CompileStatic
public class ChildFirstURLClassLoader extends GroovyClassLoader {
    private ClassLoader system

    ChildFirstURLClassLoader() {
        this( Thread.currentThread().contextClassLoader )
    }

    ChildFirstURLClassLoader(ClassLoader parent) {
        super(parent, null, false)
        system = ClassLoader.systemClassLoader
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // First, check if the class has already been loaded
        Class c = findLoadedClass(name)
        if (!c) {
            if (!system) {
                try {
                    // checking system: jvm classes, endorsed, cmd classpath,
                    // etc.
                    c = system.loadClass(name)
                }
                catch (ClassNotFoundException ignored) {
                }
            }
            if (!c) {
                try {
                    // checking local
                    c = findClass(name)
                }
                catch (ClassNotFoundException e) {
                    // checking parent
                    // This call to loadClass may eventually call findClass
                    // again, in case the parent doesn't find anything.
                    c = super.loadClass(name, resolve)
                }
            }
        }
        if (resolve) {
            resolveClass c
        }
        return c
    }

    @Override
    URL getResource(String name) {
        URL url = null
        if (!system) {
            url = system.getResource(name)
        }

        if (!url) {
            url = findResource(name)
            if (!url) {
                // This call to getResource may eventually call findResource
                // again, in case the parent doesn't find anything.
                url = super.getResource(name)
            }
        }
        return url
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        /**
         * Similar to super, but local resources are enumerated before parent
         * resources
         */
        Enumeration<URL> systemUrls = system ? system.getResources(name) : null
        Enumeration<URL> localUrls = findResources(name)
        Enumeration<URL> parentUrls = parent ? parent.getResources(name) : null

        final List<URL> urls = []
        if (systemUrls) {
            systemUrls.each { URL url ->
                urls << url
            }
        }
        if (localUrls) {
            localUrls.each { URL url ->
                urls << url
            }
        }
        if (parentUrls) {
            localUrls.each { URL url ->
                urls << url
            }
        }

        Iterator<URL> iter = urls.iterator()

        new Enumeration<URL>() {
            boolean hasMoreElements() { iter.hasNext() }

            public URL nextElement() { iter.next() }
        }
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        URL url = getResource(name)
        try {
            return url ? url.openStream() : null
        }
        catch (IOException e) {
            // ignore
        }
    }
}
