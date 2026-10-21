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
package org.apache.grails.core.aot;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.util.ClassUtils;

/**
 * The bean definitions the code generated ahead of time contributed, recorded before anything else is
 * declared, with the type each one declares.
 *
 * <p>An application running on generated artifacts has the processors that inject through annotations
 * registered again, for the beans the generated code could not see into. The generated code injects a bean
 * through the annotated members of the type its definition declares, and records that type's initialization
 * and destruction methods, so those processors leave a bean of exactly that type alone; one injected twice
 * runs a side-effecting {@code @Autowired} method twice, which registered every executor's metrics a second
 * time.</p>
 *
 * <p>A bean of a subclass of that type has members the generated code never saw, so those processors inject it
 * too, but only through them: the members the generated code injected are first marked on the bean's
 * definition as configured from outside, the way Spring keeps two processors from injecting one member. A
 * factory method declaring an interface, as the link generator's does, leaves every annotated member of the
 * class it returns to the processors, and a bean whose class could not be told at all is processed as
 * usual.</p>
 *
 * <p>The definitions are recorded by identity as the context is initialized, after the generated initializer
 * has run and before any plugin declares a bean. A definition a plugin declares later, or declares again in
 * place of a generated one, is a different object and is processed as usual.</p>
 *
 * @since 8.0.1
 */
public final class GeneratedBeanDefinitions {

    /** The singleton the definitions are recorded in. */
    public static final String BEAN_NAME = "grailsGeneratedBeanDefinitions";

    /** Each generated definition, and the type it declares. */
    private final Map<BeanDefinition, Class<?>> declaredTypes = new IdentityHashMap<>();

    private GeneratedBeanDefinitions() {
    }

    /**
     * Records every bean definition the bean factory holds as generated, with the type it declares.
     *
     * @param beanFactory a bean factory the generated code has just contributed its definitions to
     */
    public static void record(ConfigurableListableBeanFactory beanFactory) {
        if (beanFactory.containsSingleton(BEAN_NAME)) {
            return;
        }
        GeneratedBeanDefinitions generated = new GeneratedBeanDefinitions();
        for (String name : beanFactory.getBeanDefinitionNames()) {
            BeanDefinition definition = beanFactory.getBeanDefinition(name);
            Class<?> declaredType = definition.getResolvableType().resolve();
            if (declaredType != null) {
                generated.declaredTypes.put(definition, declaredType);
            }
        }
        beanFactory.registerSingleton(BEAN_NAME, generated);
    }

    /**
     * Whether the generated code has already injected the bean: its definition is still the one the generated
     * code contributed, and the bean is of the type that definition declares -- or a subclass Spring generated
     * for it, as it does for a configuration class.
     *
     * @param beanFactory the bean factory the bean is in
     * @param beanName the bean's name
     * @param beanClass the class of the bean
     * @return {@code true} when generated definitions were recorded, the bean's definition is one of them and
     *         the bean is of the type it declares
     */
    public static boolean isInjectedByGeneratedCode(@Nullable BeanFactory beanFactory, String beanName,
            Class<?> beanClass) {
        Class<?> declaredType = getDeclaredType(beanFactory, beanName);
        return declaredType != null && declaredType == ClassUtils.getUserClass(beanClass);
    }

    /**
     * Marks the members the generated code injected as configured from outside, on the definition of a bean it
     * created of a subclass of the type that definition declares: the members the given processor finds on the
     * declared type, and the methods of the bean's class overriding them, which are the ones the generated code
     * called. A processor of that kind then injects the bean only through the members its class adds.
     *
     * @param beanFactory the bean factory the bean is in
     * @param beanName the bean's name
     * @param beanType the class of the bean
     * @param beanDefinition the bean's merged definition, before the processor of that kind processes it
     * @param generatingProcessor a processor of the kind and configuration the code was generated with, which
     *        is given a definition of its own to process
     */
    public static void markMembersInjectedByGeneratedCode(@Nullable BeanFactory beanFactory, String beanName,
            Class<?> beanType, RootBeanDefinition beanDefinition, MergedBeanDefinitionPostProcessor generatingProcessor) {
        Class<?> declaredType = getDeclaredType(beanFactory, beanName);
        Class<?> beanClass = ClassUtils.getUserClass(beanType);
        if (declaredType == null || declaredType == beanClass || !declaredType.isAssignableFrom(beanClass)) {
            return;
        }
        RootBeanDefinition generated = new RootBeanDefinition(declaredType);
        generatingProcessor.postProcessMergedBeanDefinition(generated, declaredType, beanName);
        for (Member member : generated.getExternallyManagedConfigMembers()) {
            beanDefinition.registerExternallyManagedConfigMember(member);
            if (member instanceof Method method) {
                beanDefinition.registerExternallyManagedConfigMember(ClassUtils.getMostSpecificMethod(method, beanClass));
            }
        }
    }

    @Nullable
    private static Class<?> getDeclaredType(@Nullable BeanFactory beanFactory, String beanName) {
        if (!(beanFactory instanceof ConfigurableListableBeanFactory listable) ||
                !listable.containsSingleton(BEAN_NAME) || !listable.containsBeanDefinition(beanName)) {
            return null;
        }
        if (!(listable.getSingleton(BEAN_NAME) instanceof GeneratedBeanDefinitions recorded)) {
            return null;
        }
        return recorded.declaredTypes.get(listable.getBeanDefinition(beanName));
    }

}
