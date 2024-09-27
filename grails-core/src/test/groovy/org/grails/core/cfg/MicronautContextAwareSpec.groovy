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