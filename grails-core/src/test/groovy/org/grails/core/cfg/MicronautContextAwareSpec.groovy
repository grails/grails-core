/*
 * Copyright 2024 original authors
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
package org.grails.core.cfg

import grails.util.Environment
import grails.util.SupplierUtil
import io.micronaut.context.ApplicationContext
import io.micronaut.context.ApplicationContextBuilder
import io.micronaut.context.ApplicationContextConfigurer
import io.micronaut.context.annotation.ContextConfigurer
import io.micronaut.context.env.PropertySource
import io.micronaut.spring.context.factory.MicronautBeanFactoryConfiguration
import org.springframework.core.convert.ConversionService
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.PropertyResolver
import org.springframework.lang.NonNull

import java.util.function.Supplier

trait MicronautContextAwareSpec {

    ApplicationContext micronautContext
    String[] environments = new String[5]
    Supplier<ClassLoader> classLoader = SupplierUtil.memoized(() -> this.getClass().getClassLoader())

    ApplicationContextBuilder micronautContextBuilder() {
        if (Environment.current != null) {
            environments[0] = Environment.current.getName()
        }
        final ApplicationContextBuilder builder = ApplicationContext.builder()
                .classLoader(classLoader.get())
                .deduceEnvironment(false)
                .propertySources(PropertySource.of("grails-config", [(MicronautBeanFactoryConfiguration.PREFIX + ".bean-excludes"): (Object) beanExcludes]))
                .environments(environments)

        builder
    }

    ApplicationContext getMicronautContext() {
        if (micronautContext == null) {
            micronautContext = micronautContextBuilder().build()
        }
        micronautContext
    }

    ApplicationContext start() {
        if (micronautContext != null) {
            micronautContext.start()
        }
    }

    List<Class<?>> getBeanExcludes() {
        List beanExcludes = []
        beanExcludes.add(ConversionService.class)
        beanExcludes.add(org.springframework.core.env.Environment.class)
        beanExcludes.add(PropertyResolver.class)
        beanExcludes.add(ConfigurableEnvironment.class)
        def objectMapper = io.micronaut.core.reflect.ClassUtils.forName("com.fasterxml.jackson.databind.ObjectMapper", classLoader.get()).orElse(null)
        if (objectMapper != null) {
            beanExcludes.add(objectMapper)
        }
        beanExcludes
    }

    void setClassLoader(ClassLoader classLoader) {
        this.classLoader = SupplierUtil.memoized(() -> classLoader)
    }

}