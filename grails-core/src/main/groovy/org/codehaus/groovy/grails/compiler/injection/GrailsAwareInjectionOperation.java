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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.SourceUnit;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.type.filter.AnnotationTypeFilter;

/**
 * A Groovy compiler injection operation that uses a specified array of ClassInjector instances to
 * attempt AST injection.
 *
 * @author Graeme Rocher
 * @since 0.6
 */
public class GrailsAwareInjectionOperation extends CompilationUnit.PrimaryClassNodeOperation  {

    private static final String INJECTOR_SCAN_PACKAGE = "org.codehaus.groovy.grails.compiler";

    private static ClassInjector[] classInjectors = null;
    private ClassInjector[] localClassInjectors;

    public GrailsAwareInjectionOperation() {
        initializeState();
    }

    public GrailsAwareInjectionOperation(ClassInjector[] classInjectors) {
        initializeState();
        this.localClassInjectors = classInjectors;
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
        if (classInjectors == null) {
            BeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
            ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(registry);
            scanner.setResourceLoader(new DefaultResourceLoader(Thread.currentThread().getContextClassLoader()));
            scanner.addIncludeFilter(new AnnotationTypeFilter(AstTransformer.class));
            scanner.scan(INJECTOR_SCAN_PACKAGE);

            List<ClassInjector> classInjectorList = new ArrayList<ClassInjector>();
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            for (String beanName : registry.getBeanDefinitionNames()) {
                try {
                    Class<?> injectorClass = classLoader.loadClass(registry.getBeanDefinition(beanName).getBeanClassName());
                    if (ClassInjector.class.isAssignableFrom(injectorClass))
                        classInjectorList.add((ClassInjector) injectorClass.newInstance());
                } catch (ClassNotFoundException e) {
                    // ignore
                } catch (InstantiationException e) {
                    // ignore
                } catch (IllegalAccessException e) {
                    // ignore
                }
            }
            Collections.sort(classInjectorList, new Comparator<ClassInjector>() {
                @SuppressWarnings({ "unchecked", "rawtypes" })
                public int compare(ClassInjector classInjectorA, ClassInjector classInjectorB) {
                    if (classInjectorA instanceof Comparable) {
                        return ((Comparable)classInjectorA).compareTo(classInjectorB);
                    }
                    return 0;
                }
            });
            classInjectors = classInjectorList.toArray(new ClassInjector[classInjectorList.size()]);
        }
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
        for (ClassInjector classInjector : getLocalClassInjectors()) {
            if (classInjector.shouldInject(url)) {
                classInjector.performInjection(source, context, classNode);
            }
        }
    }
}
