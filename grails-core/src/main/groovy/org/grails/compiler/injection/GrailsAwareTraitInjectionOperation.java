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
package org.grails.compiler.injection;

import grails.compiler.traits.TraitInjector;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.trait.TraitComposer;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.type.filter.AssignableTypeFilter;

/**
 *
 * @author Jeff Brown
 * @since 3.0
 *
 */
public class GrailsAwareTraitInjectionOperation extends
        CompilationUnit.PrimaryClassNodeOperation {

    protected CompilationUnit unit;
    protected static List<TraitInjector> traitInjectors;
    private static final String PACKAGE_TO_SCAN = "grails.compiler.traits";

    public GrailsAwareTraitInjectionOperation(CompilationUnit unit) {
        this.unit = unit;
        initializeState();
    }

    public void setTraitInjectors(List<TraitInjector> i) {
        traitInjectors = i;
    }

    @Override
    public void call(SourceUnit source, GeneratorContext context,
            ClassNode classNode) throws CompilationFailedException {

        URL url = null;
        final String filename = source.getName();
        Resource resource = new FileSystemResource(filename);
        if (resource.exists()) {
            try {
                url = resource.getURL();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        List<TraitInjector> injectorsToUse = new ArrayList<TraitInjector>();
        for (TraitInjector injector : traitInjectors) {
            if (injector.shouldInject(url)) {
                injectorsToUse.add(injector);
            }
        }
        performTraitInjection(source, classNode, injectorsToUse);
    }

    public void performTraitInjection(SourceUnit source, ClassNode classNode,
            List<TraitInjector> injectorsToUse) {
        for (TraitInjector injector : injectorsToUse) {
            Class<?> trait = injector.getTrait();
            ClassNode traitClassNode = ClassHelper.make(trait);
            if (!classNode.implementsInterface(traitClassNode)) {
                classNode.addInterface(traitClassNode);
            }
        }
        if(unit.getPhase() != CompilePhase.SEMANTIC_ANALYSIS.getPhaseNumber()) {
            TraitComposer.doExtendTraits(classNode, source, unit);
        }
    }

    public List<TraitInjector> getTraitInjectors() {
        return Collections.unmodifiableList(traitInjectors);
    }

    protected static void initializeState() {
        if (traitInjectors != null) {
            return;
        }

        traitInjectors = new ArrayList<TraitInjector>();

        BeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
        ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(
                registry, false);
        scanner.setIncludeAnnotationConfig(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(TraitInjector.class));

        ClassLoader classLoader = Thread.currentThread()
                .getContextClassLoader();
        scanner.setResourceLoader(new DefaultResourceLoader(classLoader));
        scanner.scan(PACKAGE_TO_SCAN);

        // fallback to current classloader for special cases (e.g. gradle
        // classloader isolation with useAnt=false)
        if (registry.getBeanDefinitionCount() == 0) {
            classLoader = GrailsAwareInjectionOperation.class.getClassLoader();
            scanner.setResourceLoader(new DefaultResourceLoader(classLoader));
            scanner.scan(PACKAGE_TO_SCAN);
        }

        for (String beanName : registry.getBeanDefinitionNames()) {
            try {
                Class<?> injectorClass = classLoader.loadClass(registry
                        .getBeanDefinition(beanName).getBeanClassName());
                if (TraitInjector.class.isAssignableFrom(injectorClass))
                    traitInjectors.add((TraitInjector) injectorClass
                            .newInstance());
            } catch (Exception e) {
                // ignore
            }
        }
    }
}
