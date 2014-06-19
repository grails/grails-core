/*
 * Copyright 2006-2007 Graeme Rocher
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
package org.grails.compiler.injection;

import groovy.lang.GroovyResourceLoader;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.SourceUnit;
import org.grails.core.io.GrailsResource;
import org.codehaus.groovy.grails.io.support.FileSystemResource;
import org.codehaus.groovy.grails.io.support.PathMatchingResourcePatternResolver;
import org.codehaus.groovy.grails.io.support.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * A Groovy compiler injection operation that uses a specified array of
 * ClassInjector instances to attempt AST injection.
 *
 * @author Graeme Rocher
 * @since 0.6
 */
public class GrailsAwareInjectionOperation extends CompilationUnit.PrimaryClassNodeOperation {

    private static final String INJECTOR_SCAN_PACKAGE = "org.grails.compiler";
    private static final String INJECTOR_CODEHAUS_SCAN_PACKAGE = "org.codehaus.groovy.grails.compiler";

    private static ClassInjector[] classInjectors;
    private ClassInjector[] localClassInjectors;

    public GrailsAwareInjectionOperation() {
        initializeState();
    }

    public GrailsAwareInjectionOperation(ClassInjector[] classInjectors) {
        this();
        localClassInjectors = classInjectors;
    }

    /**
     * @deprecated Custom resource loader no longer supported
     */
    @Deprecated
    public GrailsAwareInjectionOperation(GroovyResourceLoader resourceLoader, ClassInjector[] classInjectors) {
        localClassInjectors = classInjectors;
    }

    public static ClassInjector[] getClassInjectors() {
        if (classInjectors == null) {
            initializeState();
        }
        return classInjectors;
    }

    public ClassInjector[] getLocalClassInjectors() {
        if (localClassInjectors == null) {
            return getClassInjectors();
        }
        return localClassInjectors;
    }

    private static void initializeState() {
        if (classInjectors != null) {
            return;
        }


        String pattern = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
                ClassUtils.convertClassNameToResourcePath(INJECTOR_CODEHAUS_SCAN_PACKAGE) + "/**/*.class";

        String pattern2 = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
                ClassUtils.convertClassNameToResourcePath(INJECTOR_SCAN_PACKAGE) + "/**/*.class";

        ClassLoader classLoader = GrailsAwareInjectionOperation.class.getClassLoader();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(classLoader);
        Resource[] resources;
        try {
            resources = scanForPatterns(resolver, pattern2, pattern);
            if(resources.length == 0) {
                classLoader = Thread.currentThread().getContextClassLoader();
                resolver = new PathMatchingResourcePatternResolver(classLoader);
                resources = scanForPatterns(resolver, pattern2, pattern);
            }
            List<ClassInjector> injectors = new ArrayList<ClassInjector>();
            Set<Class> injectorClasses = new HashSet<Class>();
            CachingMetadataReaderFactory readerFactory = new CachingMetadataReaderFactory(classLoader);
            for (org.codehaus.groovy.grails.io.support.Resource resource : resources) {
                try {

                    MetadataReader metadataReader = readerFactory.getMetadataReader(new GrailsResource(resource));
                    if(metadataReader.getAnnotationMetadata().hasAnnotation(AstTransformer.class.getName())) {
                        String className = metadataReader.getClassMetadata().getClassName();
                        Class<?> injectorClass = classLoader.loadClass(className);
                        if(injectorClasses.contains(injectorClass)) continue;
                        if (ClassInjector.class.isAssignableFrom(injectorClass)) {

                            injectorClasses.add(injectorClass);
                            injectors.add((ClassInjector) injectorClass.newInstance());
                        }
                    }
                } catch (ClassNotFoundException e) {
                    // ignore
                } catch (InstantiationException e) {
                    // ignore
                } catch (IllegalAccessException e) {
                    // ignore
                } catch (IOException e) {
                    // ignore
                }

            }
            Collections.sort(injectors, new Comparator<ClassInjector>() {
                @SuppressWarnings({ "unchecked", "rawtypes" })
                public int compare(ClassInjector classInjectorA, ClassInjector classInjectorB) {
                    if (classInjectorA instanceof Comparable) {
                        return ((Comparable)classInjectorA).compareTo(classInjectorB);
                    }
                    return 0;
                }
            });
            classInjectors = injectors.toArray(new ClassInjector[injectors.size()]);
        } catch (IOException e) {
            // ignore
        }


    }

    private static Resource[] scanForPatterns(PathMatchingResourcePatternResolver resolver, String...patterns) throws IOException {
        List<Resource> results = new ArrayList<Resource>();
        for(String pattern : patterns) {
            results.addAll( Arrays.asList(resolver.getResources(pattern)) );
        }
        return results.toArray(new Resource[results.size()]);
    }

    @Override
    public void call(SourceUnit source, GeneratorContext context, ClassNode classNode) throws CompilationFailedException {

        URL url = null;
        final String filename = source.getName();
        Resource resource = new FileSystemResource(filename);
        if (resource.exists()) {
            try {
                url = resource.getURL();
            } catch (IOException e) {
                // ignore
            }
        }

        ClassInjector[] classInjectors1 = getLocalClassInjectors();
        if (classInjectors1 == null || classInjectors1.length == 0) {
            classInjectors1 = getClassInjectors();
        }
        for (ClassInjector classInjector : classInjectors1) {
            if (classInjector.shouldInject(url)) {
                classInjector.performInjection(source, context, classNode);
            }
        }
    }
}
