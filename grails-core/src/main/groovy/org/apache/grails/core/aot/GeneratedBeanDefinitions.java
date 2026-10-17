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

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * The bean definitions the code generated ahead of time contributed, recorded before anything else is
 * declared.
 *
 * <p>An application running on generated artifacts has the processors that inject through annotations
 * registered again, for the beans the generated code could not see into. The generated code has already
 * injected every bean it created and recorded its initialization and destruction methods, so those processors
 * leave such a bean alone; one injected twice runs a side-effecting {@code @Autowired} method twice, which
 * registered every executor's metrics a second time.</p>
 *
 * <p>The definitions are recorded by identity as the context is initialized, after the generated initializer
 * has run and before any plugin declares a bean. A definition a plugin declares later, or declares again in
 * place of a generated one, is a different object and is processed as usual.</p>
 *
 * @since 9.0
 */
public final class GeneratedBeanDefinitions {

    /** The singleton the definitions are recorded in. */
    public static final String BEAN_NAME = "grailsGeneratedBeanDefinitions";

    private final Set<BeanDefinition> definitions = Collections.newSetFromMap(new IdentityHashMap<>());

    private GeneratedBeanDefinitions() {
    }

    /**
     * Records every bean definition the bean factory holds as generated.
     *
     * @param beanFactory a bean factory the generated code has just contributed its definitions to
     */
    public static void record(ConfigurableListableBeanFactory beanFactory) {
        if (beanFactory.containsSingleton(BEAN_NAME)) {
            return;
        }
        GeneratedBeanDefinitions generated = new GeneratedBeanDefinitions();
        for (String name : beanFactory.getBeanDefinitionNames()) {
            generated.definitions.add(beanFactory.getBeanDefinition(name));
        }
        beanFactory.registerSingleton(BEAN_NAME, generated);
    }

    /**
     * Whether the bean is still the one the generated code contributed.
     *
     * @param beanFactory the bean factory the bean is in
     * @param beanName the bean's name
     * @return {@code true} when generated definitions were recorded and the bean's definition is one of them
     */
    public static boolean isGenerated(@Nullable BeanFactory beanFactory, String beanName) {
        if (!(beanFactory instanceof ConfigurableListableBeanFactory listable) ||
                !listable.containsSingleton(BEAN_NAME) || !listable.containsBeanDefinition(beanName)) {
            return false;
        }
        Object generated = listable.getSingleton(BEAN_NAME);
        return generated instanceof GeneratedBeanDefinitions recorded &&
                recorded.definitions.contains(listable.getBeanDefinition(beanName));
    }

}
