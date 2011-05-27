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

import grails.web.Action;
import groovy.lang.Closure;
import groovy.lang.GroovyObject;
import org.codehaus.groovy.grails.commons.ControllerArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.plugins.support.BeanPostProcessorAdapter;
import org.codehaus.groovy.grails.web.plugins.support.WebMetaUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author Graeme Rocher
 * @since 1.3.2
 */
public class CommandObjectEnablingPostProcessor extends BeanPostProcessorAdapter implements ApplicationContextAware {

    private GrailsApplication grailsApplication;
    private Closure<?> commandObjectBindingAction;
    private Collection<String> processedControllerNames = new ConcurrentLinkedQueue<String>();
    private ApplicationContext applicationContext;

    public CommandObjectEnablingPostProcessor(ApplicationContext applicationContext) {
        setApplicationContext(applicationContext);
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (shouldPostProcessController(bean, beanName)) {
            if (grailsApplication.getArtefact(ControllerArtefactHandler.TYPE, bean.getClass().getName()) != null) {
                GroovyObject controller = (GroovyObject) bean;
                scanClosureActions(controller);
                scanMethodActions(controller);
                processedControllerNames.add(beanName);
            }
        }
        return super.postProcessBeforeInitialization(bean, beanName);
    }

    @SuppressWarnings("unchecked")
    private void scanClosureActions(GroovyObject controller) {
        Map<String, Object> props = (Map<String, Object>) controller.getProperty("properties");
        for (String propName : props.keySet()) {
            Object value = props.get(propName);
            if (value instanceof Closure) {
                final Closure<?> callable = (Closure<?>) value;
                if (WebMetaUtils.isCommandObjectAction(callable)) {
                    WebMetaUtils.prepareCommandObjectBindingAction(commandObjectBindingAction, callable, propName, controller, applicationContext);
                }
            }
        }
    }

    private void scanMethodActions(GroovyObject controller) {
        Method[] methods = ReflectionUtils.getAllDeclaredMethods(controller.getClass());
        Action actionAnn = null;
        for (Method method : methods) {
            actionAnn = method.getAnnotation(Action.class);
            Class<?>[] commandObjectClasses = actionAnn != null ? actionAnn.commandObjects() : null;
            if (commandObjectClasses != null && commandObjectClasses.length > 0) {
                WebMetaUtils.prepareCommandObjectBindingAction(method, commandObjectClasses, applicationContext);
            }
        }
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
