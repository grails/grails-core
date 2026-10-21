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
package grails.boot.config;

import org.springframework.aot.AotDetector;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import org.apache.grails.core.aot.GeneratedBeanDefinitions;

/**
 * Registers the {@link GrailsEarlyPluginRegistrationPostProcessor} on the context via
 * {@code addBeanFactoryPostProcessor}, which guarantees it runs ahead of Spring Boot's
 * registry-discovered post-processors (including {@code ConfigurationClassPostProcessor},
 * which expands auto-configuration). That ordering is what lets plugin beans contributed via
 * {@code doWithSpring} be present in the registry before Boot's {@code @ConditionalOnMissingBean}
 * guards are evaluated, so auto-configured defaults back off in favour of plugin beans.
 *
 * <p>On generated artifacts it also records the bean definitions the generated code contributed. Spring Boot
 * adds the generated initializer ahead of the application's own and sorts them stably, so it runs before this
 * one, which has no order, but after any initializer that has one. The record relies on none of those
 * registering a bean definition, and Spring Boot's own only set the parent context and register a singleton;
 * a definition one did register would be taken for a generated one. No plugin has declared a bean yet either,
 * so what the context holds here is what was generated. See {@link GeneratedBeanDefinitions}.</p>
 *
 * @since 8.0
 */
public class GrailsPluginLifecycleInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        if (AotDetector.useGeneratedArtifacts()) {
            GeneratedBeanDefinitions.record(applicationContext.getBeanFactory());
        }
        applicationContext.addBeanFactoryPostProcessor(new GrailsEarlyPluginRegistrationPostProcessor(applicationContext));
    }
}
