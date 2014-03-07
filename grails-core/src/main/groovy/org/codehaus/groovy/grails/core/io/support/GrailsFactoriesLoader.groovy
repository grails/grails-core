/*
 * Copyright 2014 the original author or authors.
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

package org.codehaus.groovy.grails.core.io.support

import groovy.transform.CompileStatic
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.springframework.core.OrderComparator
import org.springframework.core.io.UrlResource
import org.springframework.core.io.support.PropertiesLoaderUtils
import org.springframework.util.Assert
import org.springframework.util.ClassUtils

import java.util.concurrent.ConcurrentLinkedQueue

/**
 * General purpose factory loading mechanism for internal use within the framework.
 *
 * <p>Based on {@link org.springframework.core.io.support.SpringFactoriesLoader}, but applies caching and uses a different file name</p>
 *
 * @since 2.4
 * @author Graeme Rocher
 */
@CompileStatic
class GrailsFactoriesLoader {


    /** The location to look for the factories. Can be present in multiple JAR files. */
    static final String FACTORIES_RESOURCE_LOCATION = "META-INF/grails.factories"

    private static final Log logger = LogFactory.getLog(GrailsFactoriesLoader)


    private static Collection<Properties> loadedProperties
    /**
     * Load the factory implementations of the given type from the default location,
     * using the given class loader.
     * <p>The returned factories are ordered in accordance with the {@link org.springframework.core.OrderComparator}.
     * @param factoryClass the interface or abstract class representing the factory
     * @param classLoader the ClassLoader to use for loading (can be {@code null} to use the default)
     */
    static <T> List<T> loadFactories(Class<T> factoryClass, ClassLoader classLoader = GrailsFactoriesLoader.class.classLoader) {

        Assert.notNull factoryClass, "'factoryClass' must not be null"

        def factoryNames = loadFactoryNames(factoryClass, classLoader)
        if (logger.traceEnabled) {
            logger.trace("Loaded [$factoryClass.name] names: $factoryNames" )
        }

        List<T> result = []
        for (String factoryName in factoryNames) {
            result.add instantiateFactory(factoryName, factoryClass, classLoader)
        }
        OrderComparator.sort(result)
        return result
    }

    static List<String> loadFactoryNames(Class<?> factoryClass, ClassLoader classLoader = GrailsFactoriesLoader.class.classLoader) {
        def factoryClassName = factoryClass.getName()
        try {
            List<String> result = []

            if( loadedProperties  == null) {
                loadedProperties = new ConcurrentLinkedQueue<Properties>()
                def urls = classLoader.getResources(FACTORIES_RESOURCE_LOCATION);
                urls.each { URL url ->
                    def properties = PropertiesLoaderUtils.loadProperties(new UrlResource(url))
                    loadedProperties << properties
                }
            }

            for(Properties properties in loadedProperties) {
                String factoryClassNames = properties.getProperty(factoryClassName)
                result.addAll factoryClassNames.split(',').toList()
            }
            return result
        }
        catch (IOException ex) {
            throw new IllegalArgumentException("Unable to load [$factoryClassName] factories from location [$FACTORIES_RESOURCE_LOCATION]", ex);
        }
    }

    private static <T> T instantiateFactory(String instanceClassName, Class<T> factoryClass, ClassLoader classLoader) {
        try {
            def instanceClass = ClassUtils.forName(instanceClassName, classLoader)
            if (!factoryClass.isAssignableFrom(instanceClass)) {
                throw new IllegalArgumentException(
                    "Class [$instanceClassName] is not assignable to [$factoryClass.name]")
            }
            return (T) instanceClass.newInstance()
        }
        catch (Throwable ex) {
            throw new IllegalArgumentException("Cannot instantiate factory class: $factoryClass.name", ex);
        }
    }
}
