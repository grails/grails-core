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
package org.codehaus.groovy.grails.compiler.injection;

import groovy.lang.GroovyResourceLoader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.grails.commons.GrailsResourceUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.Assert;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * A Groovy compiler injection operation that uses a specified array of ClassInjector instances to
 * attempt AST injection.
 *
 * @author Graeme Rocher
 * @since 0.6
 */
public class GrailsAwareInjectionOperation extends CompilationUnit.PrimaryClassNodeOperation  {

    private static final Log LOG = LogFactory.getLog(GrailsAwareInjectionOperation.class);
    private static final String INJECTOR_SCAN_PACKAGE = "org.codehaus.groovy.grails.compiler";

    private GroovyResourceLoader grailsResourceLoader;
    private static ClassInjector[] classInjectors = null;
    private ClassInjector[] localClassInjectors;

    public GrailsAwareInjectionOperation(GroovyResourceLoader resourceLoader) {
        Assert.notNull(resourceLoader, "The argument [resourceLoader] is required!");
        this.grailsResourceLoader = resourceLoader;
        initializeState();
    }

    public GrailsAwareInjectionOperation(GroovyResourceLoader resourceLoader, ClassInjector[] classInjectors) {
        Assert.notNull(resourceLoader, "The argument [resourceLoader] is required!");
        this.grailsResourceLoader = resourceLoader;
        this.localClassInjectors = classInjectors;
    }

    public static ClassInjector[] getClassInjectors() {
        if(classInjectors == null) {
            initializeState();
        }
        return classInjectors;
    }

    public ClassInjector[] getLocalClassInjectors() {
        if(localClassInjectors == null) {
            return getClassInjectors();
        }
        return localClassInjectors;
    }

    private static void initializeState() {
        if (classInjectors == null) {
            BeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
            ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(registry);
            scanner.setResourceLoader(new DefaultResourceLoader(Thread.currentThread().getContextClassLoader()));
            scanner.addIncludeFilter(new AnnotationTypeFilter(AstTransformer.class));
            scanner.scan(INJECTOR_SCAN_PACKAGE);

            List<ClassInjector> classInjectorList = new ArrayList<ClassInjector>();
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            for(String beanName : registry.getBeanDefinitionNames()) {
                try {
                    Class<?> injectorClass = classLoader.loadClass(registry.getBeanDefinition(beanName).getBeanClassName());
                    if(ClassInjector.class.isAssignableFrom(injectorClass))
                        classInjectorList.add((ClassInjector) injectorClass.newInstance());
                } catch (ClassNotFoundException e) {
                    // ignore
                } catch (InstantiationException e) {
                    // ignore
                } catch (IllegalAccessException e) {
                    // ignore
                }
            }
            classInjectors = classInjectorList.toArray(new ClassInjector[classInjectorList.size()]);
        }
    }

    @Override
    public void call(SourceUnit source, GeneratorContext context, ClassNode classNode) throws CompilationFailedException {
        for (ClassInjector classInjector : getLocalClassInjectors()) {
            try {
                URL url;
                if (GrailsResourceUtils.isGrailsPath(source.getName())) {
                    url = grailsResourceLoader.loadGroovySource(GrailsResourceUtils.getClassName(source.getName()));
                } else {
                    url = grailsResourceLoader.loadGroovySource(source.getName());
                }

                if (classInjector.shouldInject(url)) {
                    classInjector.performInjection(source, context, classNode);
                }
            } catch (MalformedURLException e) {
                LOG.error("Error loading URL during addition of compile time properties: " + e.getMessage(), e);
                throw new CompilationFailedException(Phases.CONVERSION, source, e);
            }
        }
    }
}
