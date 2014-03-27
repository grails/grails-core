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

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.springframework.core.OrderComparator
import org.springframework.core.io.UrlResource
import org.springframework.core.io.support.PropertiesLoaderUtils
import org.springframework.util.Assert
import org.springframework.util.ClassUtils

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

    private static ConcurrentMap<Integer, Map<String,String[]>> loadedPropertiesForClassLoader = new ConcurrentHashMap<Integer, Map<String,String[]>>()
    
    private static final Object[] NO_ARGUMENTS = [] as Object[]
    
    
    /**
     * Load the factory implementations of the given type from the default location,
     * using the given class loader.
     * <p>The returned factories are ordered in accordance with the {@link org.springframework.core.OrderComparator}.
     * @param factoryClass the interface or abstract class representing the factory
     * @param classLoader the ClassLoader to use for loading (can be {@code null} to use the default)
     */
    static <T> List<T> loadFactories(Class<T> factoryClass, ClassLoader classLoader) {
        (List<T>)loadFactoriesWithArguments(factoryClass, classLoader, NO_ARGUMENTS)
    }

    static <T> List<T> loadFactoriesWithArguments(Class<T> factoryClass, ClassLoader classLoader, Object[] arguments) {
        boolean hasArguments = !(arguments != null && arguments.length==0)
        List<T> results = new ArrayList<T>() 
        for(Class<? extends T> clazz : loadFactoryClasses(factoryClass, classLoader)) {
            results.add(hasArguments ? clazz.newInstance(arguments) : clazz.newInstance()) 
        }
        OrderComparator.sort(results)
        results
    }
    
    static <T> List<Class<T>> loadFactoryClasses(Class<T> factoryClass, ClassLoader classLoader = GrailsFactoriesLoader.class.classLoader) {
        Assert.notNull factoryClass, "'factoryClass' must not be null"
        
        def factoryNames = loadFactoryNames(factoryClass, classLoader)
        if (logger.traceEnabled) {
            logger.trace("Loaded [$factoryClass.name] names: $factoryNames" )
        }

        List<Class<T>> result = []
        for (String factoryName in factoryNames) {
            result.add loadFactoryClass(factoryName, factoryClass, classLoader)
        }
        return result
    }

    static String[] loadFactoryNames(Class<?> factoryClass, ClassLoader classLoader = GrailsFactoriesLoader.class.classLoader) {
        String factoryClassName = factoryClass.getName()
        try {
            Map<String,String[]> loadedProperties = loadedPropertiesForClassLoader.get(System.identityHashCode(classLoader))
            if( loadedProperties  == null) {
                Set<String> allKeys = [] as Set
                def urls = classLoader.getResources(FACTORIES_RESOURCE_LOCATION);
                Collection<Properties> allProperties = []
                urls.each { URL url ->
                    def properties = PropertiesLoaderUtils.loadProperties(new UrlResource(url))
                    allProperties << properties
                    allKeys.addAll((Set<String>)properties.keySet())
                }
                Map<String,String[]> mergedFactoryNames=[:]
                for (String propertyName : allKeys) {
                    Set<String> result = [] as Set
                    for (Properties props : allProperties) {
                        String factoryClassNames = props.getProperty(propertyName)
                        if (factoryClassNames) {
                            result.addAll factoryClassNames.split(',').toList()
                        }
                    }
                    mergedFactoryNames.put propertyName, result as String[]
                }
                loadedProperties = loadedPropertiesForClassLoader.putIfAbsent(System.identityHashCode(classLoader), mergedFactoryNames)
                if (loadedProperties == null) {
                    loadedProperties = mergedFactoryNames
                }
            }
            return loadedProperties.get(factoryClassName)
        }
        catch (IOException ex) {
            throw new IllegalArgumentException("Unable to load [$factoryClassName] factories from location [$FACTORIES_RESOURCE_LOCATION]", ex);
        }
    }

    private static <T> Class<? extends T> loadFactoryClass(String instanceClassName, Class<T> factoryClass, ClassLoader classLoader) {
        try {
            def instanceClass = ClassUtils.forName(instanceClassName, classLoader)
            if (!factoryClass.isAssignableFrom(instanceClass)) {
                throw new IllegalArgumentException(
                    "Class [$instanceClassName] is not assignable to [$factoryClass.name]")
            }
            return (Class<? extends T>) instanceClass
        }
        catch (Throwable ex) {
            throw new IllegalArgumentException("Cannot instantiate factory class: $factoryClass.name", ex);
        }
    }

    static <T> T loadFactory(Class<T> factoryClass, ClassLoader classLoader = GrailsFactoriesLoader.class.classLoader) {
        def all = loadFactories(factoryClass, classLoader)
        if(all) {
            return all.get(0)
        }
    }

    static <T> T loadFactory(Class<T> factoryClass, Object... arguments) {
        loadFactory(factoryClass, GrailsFactoriesLoader.class.classLoader, arguments)
    }

    static <T> T loadFactory(Class<T> factoryClass, ClassLoader classLoader, Object... arguments) {
        def all = loadFactoriesWithArguments(factoryClass, classLoader, arguments)
        if(all) {
            return (T)all.get(0)
        }
    }
}
