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
import org.grails.core.io.support.GrailsFactoriesLoader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/**
 *
 * @author Jeff Brown
 * @since 3.0
 *
 */
public class GrailsAwareTraitInjectionOperation extends
        CompilationUnit.PrimaryClassNodeOperation {

    protected CompilationUnit unit;
    protected List<TraitInjector> traitInjectors;

    public GrailsAwareTraitInjectionOperation(CompilationUnit unit) {
        this.unit = unit;
        traitInjectors = GrailsFactoriesLoader.loadFactories(TraitInjector.class);
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
        boolean traitsAdded = false;
        for (TraitInjector injector : injectorsToUse) {
            Class<?> trait = injector.getTrait();
            ClassNode traitClassNode = ClassHelper.make(trait);
            boolean implementsTrait = false;
            boolean traitNotLoaded = false;
            try {
                implementsTrait = classNode.implementsInterface(traitClassNode);
            } catch (Throwable e) {
                // if we reach this point, the trait injector could not be loaded due to missing dependencies (for example missing servlet-api). This is ok, as we want to be able to compile against non-servlet environments.
                traitNotLoaded = true;
            }
            if (!implementsTrait && !traitNotLoaded) {
                System.out.println("traitClassNode = " + traitClassNode);
                classNode.addInterface(traitClassNode);
                traitsAdded = true;
            }
        }
        if(traitsAdded && 
           unit.getPhase() != CompilePhase.SEMANTIC_ANALYSIS.getPhaseNumber()) {
            TraitComposer.doExtendTraits(classNode, source, unit);
        }
    }

    public List<TraitInjector> getTraitInjectors() {
        return Collections.unmodifiableList(traitInjectors);
    }
}
