/* Copyright (C) 2010 SpringSource
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
package org.codehaus.groovy.grails.web.servlet.mvc;

import groovy.lang.Closure;
import groovy.lang.GroovyObject;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.codehaus.groovy.grails.commons.ControllerArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.plugins.support.BeanPostProcessorAdapter;
import org.codehaus.groovy.grails.web.plugins.support.WebMetaUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * @author Graeme Rocher
 * @since 1.3.2
 */
public class CommandObjectEnablingPostProcessor extends BeanPostProcessorAdapter implements ApplicationContextAware {

    private GrailsApplication grailsApplication;
    private Closure commandObjectBindingAction;
    private Collection processedControllerNames = new ConcurrentLinkedQueue();
    private ApplicationContext applicationContext;

    public CommandObjectEnablingPostProcessor(ApplicationContext applicationContext) {
        setApplicationContext(applicationContext);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (shouldPostProcessController(bean, beanName)) {
            if (grailsApplication.getArtefact(ControllerArtefactHandler.TYPE, bean.getClass().getName()) != null) {
                GroovyObject controller = (GroovyObject) bean;
                Map<String, Object> props = (Map<String, Object>) controller.getProperty("properties");
                for (String propName : props.keySet()) {
                    Object value = props.get(propName);
                    if (value instanceof Closure) {
                        final Closure callable = (Closure) value;
                        if (WebMetaUtils.isCommandObjectAction(callable)) {
                            WebMetaUtils.prepareCommandObjectBindingAction(commandObjectBindingAction,callable, propName, controller, applicationContext);
                        }
                    }
                }
                processedControllerNames.add(beanName);
            }
        }
        return super.postProcessBeforeInitialization(bean, beanName);
    }

    private boolean shouldPostProcessController(Object bean, String beanName) {
        return beanName.endsWith(ControllerArtefactHandler.TYPE) &&
               !processedControllerNames.contains(beanName) &&
               grailsApplication != null && bean != null && (bean instanceof GroovyObject);
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        grailsApplication = applicationContext.getBean(GrailsApplication.class);
        commandObjectBindingAction = WebMetaUtils.createCommandObjectBindingAction(applicationContext);
        this.applicationContext = applicationContext;
    }
}
