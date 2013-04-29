/*
 * Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.plugins.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * Adapter implementation of {@link BeanPostProcessor}.
 *
 * @author Steven Devijver
 * @since 0.2
 */
public class BeanPostProcessorAdapter implements BeanPostProcessor {

    /**
     * @param bean
     * @param beanName
     * @return The specified bean
     * @throws BeansException
     * @see BeanPostProcessor#postProcessBeforeInitialization(Object, String)
     */
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    /**
     * @param bean
     * @param beanName
     * @return The specified bean
     * @throws BeansException
     * @see BeanPostProcessor#postProcessAfterInitialization(Object, String)
     */
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
}
