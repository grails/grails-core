/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package grails.boot.config

import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import jakarta.annotation.Resource

import org.springframework.aot.AotDetector
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.context.annotation.AnnotationConfigUtils
import org.springframework.context.support.GenericApplicationContext
import spock.lang.Specification

import org.apache.grails.core.aot.GeneratedBeanDefinitions

/**
 * Covers the processors that read the injection annotations being restored when running on
 * artifacts generated ahead of time.
 *
 * <p>The generator resolves the annotations itself for a bean whose implementation it can see, but a
 * bean contributed as an interface built by a supplier hides that implementation, and the annotated
 * members on it are then injected by nobody.</p>
 */
class GrailsApplicationPostProcessorAnnotationConfigSpec extends Specification {

    private static final String AOT_KEY = 'spring.aot.enabled'

    /** What injecting and initializing an {@link InjectedFixture} calls, each once. */
    private static final Map<String, Integer> INJECTED_AND_INITIALIZED =
            [collaborator: 1, overridableCollaborator: 1, resourceCollaborator: 1, initialize: 1].asImmutable()

    BeanDefinitionRegistry registry = new DefaultListableBeanFactory()

    /** Restores the property, so the environment other specs observe is unchanged. */
    private void withGeneratedArtifacts(boolean enabled, Closure body) {
        String previous = System.getProperty(AOT_KEY)
        try {
            enabled ? System.setProperty(AOT_KEY, 'true') : System.clearProperty(AOT_KEY)
            body.call()
        }
        finally {
            previous == null ? System.clearProperty(AOT_KEY) : System.setProperty(AOT_KEY, previous)
        }
    }

    private void register() {
        GrailsApplicationPostProcessor.registerAnnotationConfigProcessorsForGeneratedArtifacts(registry)
    }

    void 'the injection processors are registered when running on generated artifacts'() {
        when:
            withGeneratedArtifacts(true) { register() }

        then:
            registry.containsBeanDefinition(AnnotationConfigUtils.AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME)
            registry.containsBeanDefinition(AnnotationConfigUtils.COMMON_ANNOTATION_PROCESSOR_BEAN_NAME)
    }

    void 'the injection processors are created without reflection'() {
        when:
            withGeneratedArtifacts(true) { register() }

        then: 'they are registered while running on generated artifacts, which includes a native image'
            [AnnotationConfigUtils.AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME, AnnotationConfigUtils.COMMON_ANNOTATION_PROCESSOR_BEAN_NAME].every {
                ((RootBeanDefinition) registry.getBeanDefinition(it)).instanceSupplier != null
            }
    }

    void 'the processor that reads configuration classes is not'() {
        when:
            withGeneratedArtifacts(true) { register() }

        then: 'reading them again in a context whose configuration is already generated makes a ' +
                'second definition for beans the generated code has contributed'
            !registry.containsBeanDefinition(AnnotationConfigUtils.CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME)
    }

    void 'nothing is registered without generated artifacts'() {
        when:
            withGeneratedArtifacts(false) { register() }

        then:
            !registry.containsBeanDefinition(AnnotationConfigUtils.AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME)
            !registry.containsBeanDefinition(AnnotationConfigUtils.COMMON_ANNOTATION_PROCESSOR_BEAN_NAME)
    }

    void 'a context that already has them keeps what it has'() {
        given:
            RootBeanDefinition existing = new RootBeanDefinition(String)
            registry.registerBeanDefinition(
                    AnnotationConfigUtils.AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME, existing)

        when:
            withGeneratedArtifacts(true) { register() }

        then:
            registry.getBeanDefinition(
                    AnnotationConfigUtils.AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME).is(existing)
    }

    void 'a bean the generated code created is injected, initialized and destroyed once, and one declared later still is'() {
        given: 'a bean the generated code created, injected and given its lifecycle methods by that code'
            def context = new GenericApplicationContext()
            context.registerBean('collaborator', CollaboratorFixture)
            context.registerBeanDefinition('generated', generatedDefinition { new InjectedFixture() })
            GeneratedBeanDefinitions.record(context.beanFactory)

        and: 'a bean declared after it, as a plugin declares one'
            context.registerBean('declaredLater', InjectedFixture)

        when:
            refreshOnGeneratedArtifacts(context)
            def generated = context.getBean('generated', InjectedFixture)
            def declaredLater = context.getBean('declaredLater', InjectedFixture)

        then: 'injected twice, a side-effecting autowired method runs twice'
            generated.calls == INJECTED_AND_INITIALIZED

        and: 'the bean the generated code did not create is injected by the processors'
            declaredLater.calls == INJECTED_AND_INITIALIZED

        when:
            context.close()

        then: 'the container calls the destruction method the generated code recorded, and the processor does not'
            generated.calls.shutdown == 1
            declaredLater.calls.shutdown == 1

        cleanup:
            context.close()
    }

    void 'a bean the generated code created as an interface is injected and initialized by the processors'() {
        given: 'a bean the generated code created from a factory method declaring an interface, as the link ' +
                'generator is created, so the code saw none of the members of the class it returns'
            def context = new GenericApplicationContext()
            context.registerBean('collaborator', CollaboratorFixture)
            context.registerBeanDefinition('declaredAsInterface', new RootBeanDefinition(InjectedType).tap {
                instanceSupplier = { new InjectedFixture() }
            })
            GeneratedBeanDefinitions.record(context.beanFactory)

        when:
            refreshOnGeneratedArtifacts(context)

        then: 'nobody else injects it: left alone, the link generator rendered no link'
            context.getBean('declaredAsInterface', InjectedFixture).calls == INJECTED_AND_INITIALIZED

        cleanup:
            context.close()
    }

    void 'a bean the generated code created as a superclass is injected once through it, and through the rest by the processors'() {
        given: 'a bean the generated code created from a factory method declaring a superclass of the class it ' +
                'returns, so the code injected it through the members of that superclass and recorded its lifecycle methods'
            def context = new GenericApplicationContext()
            context.registerBean('collaborator', CollaboratorFixture)
            context.registerBeanDefinition('declaredAsSuperclass', generatedDefinition { new SubclassFixture() })
            GeneratedBeanDefinitions.record(context.beanFactory)

        when:
            refreshOnGeneratedArtifacts(context)
            def bean = context.getBean('declaredAsSuperclass', SubclassFixture)

        then: 'what the generated code injected is not injected again, nor is the override of it that code called, ' +
                'and what only the subclass declares, which that code never saw, is injected by the processors'
            bean.calls == INJECTED_AND_INITIALIZED + [subclassCollaborator: 1]

        when:
            context.close()

        then:
            bean.calls.shutdown == 1

        cleanup:
            context.close()
    }

    void 'a bean the generated code created as a generic superclass is injected once through the override of its setter'() {
        given: 'a factory method declaring a generic superclass, whose subclass overrides the annotated setter with ' +
                'the actual type, so the generated code called that override through the bridge to it'
            def context = new GenericApplicationContext()
            context.registerBean('collaborator', CollaboratorFixture)
            context.registerBeanDefinition('declaredAsGenericSuperclass', new RootBeanDefinition(GenericSuperclassFixture).tap {
                instanceSupplier = { new GenericSubclassFixture().tap { setDependency(new CollaboratorFixture()) } }
            })
            GeneratedBeanDefinitions.record(context.beanFactory)

        when:
            refreshOnGeneratedArtifacts(context)

        then:
            context.getBean('declaredAsGenericSuperclass', GenericSubclassFixture).calls == [dependency: 1]

        cleanup:
            context.close()
    }

    /**
     * A definition as the generated code contributes one for a factory method declaring {@link InjectedFixture}:
     * the bean it creates is injected through the members of that type, and the type's lifecycle methods are
     * recorded on the definition.
     */
    private static RootBeanDefinition generatedDefinition(Closure<InjectedFixture> factoryMethod) {
        new RootBeanDefinition(InjectedFixture).tap {
            instanceSupplier = {
                factoryMethod.call().tap {
                    def collaborator = new CollaboratorFixture()
                    setCollaborator(collaborator)
                    setOverridableCollaborator(collaborator)
                    setResourceCollaborator(collaborator)
                }
            }
            initMethodNames = ['initialize'] as String[]
            destroyMethodNames = ['shutdown'] as String[]
        }
    }

    private void refreshOnGeneratedArtifacts(GenericApplicationContext context) {
        withGeneratedArtifacts(true) {
            GrailsApplicationPostProcessor.registerAnnotationConfigProcessorsForGeneratedArtifacts(context)
        }
        context.refresh()
    }

    static class CollaboratorFixture {
    }

    static interface InjectedType {
    }

    static class InjectedFixture implements InjectedType {

        /** How many times each member was called. */
        final Map<String, Integer> calls = [:].withDefault { 0 }

        @Autowired
        void setCollaborator(CollaboratorFixture collaborator) {
            calls.collaborator++
        }

        @Autowired
        void setOverridableCollaborator(CollaboratorFixture collaborator) {
            calls.overridableCollaborator++
        }

        @Resource
        void setResourceCollaborator(CollaboratorFixture collaborator) {
            calls.resourceCollaborator++
        }

        @PostConstruct
        void initialize() {
            calls.initialize++
        }

        @PreDestroy
        void shutdown() {
            calls.shutdown++
        }
    }

    static class GenericSuperclassFixture<T> {

        /** How many times each member was called. */
        final Map<String, Integer> calls = [:].withDefault { 0 }

        @Autowired
        void setDependency(T dependency) {
            calls.dependency++
        }
    }

    static class GenericSubclassFixture extends GenericSuperclassFixture<CollaboratorFixture> {

        @Override
        @Autowired
        void setDependency(CollaboratorFixture dependency) {
            super.setDependency(dependency)
        }
    }

    static class SubclassFixture extends InjectedFixture {

        @Override
        @Autowired
        void setOverridableCollaborator(CollaboratorFixture collaborator) {
            super.setOverridableCollaborator(collaborator)
        }

        @Autowired
        void setSubclassCollaborator(CollaboratorFixture collaborator) {
            calls.subclassCollaborator++
        }
    }
}
