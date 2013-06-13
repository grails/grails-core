/*
 * Copyright 2011 the original author or authors.
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

import grails.util.Pair;
import grails.web.Action;
import groovy.lang.Closure;
import groovy.lang.GroovyObject;
import groovy.lang.MetaProperty;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletResponse;

import org.codehaus.groovy.grails.web.servlet.mvc.exceptions.ControllerExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Implements action invocation through Closure.
 * 
 * @author Stephane Maldini
 * @since 2.0
 */
public class MixedGrailsControllerHelper extends AbstractGrailsControllerHelper {
    private static final Class<?>[] NOARGS = {};
    private static final Logger log = LoggerFactory.getLogger(MixedGrailsControllerHelper.class);
    Map<Pair<Class<?>, String>, Method> controllerToActionMethodCache = new ConcurrentHashMap<Pair<Class<?>, String>, Method>();
    private static final Method NULL_METHOD_HOLDER = ReflectionUtils.findMethod(Object.class, "toString");

    Map<Pair<Class<?>, String>, MetaProperty> controllerToMetaPropertyCache = new ConcurrentHashMap<Pair<Class<?>, String>, MetaProperty>();
    private static final MetaProperty NULL_META_PROPERTY_HOLDER = new MetaProperty("null", Void.class) {
        @Override
        public void setProperty(Object object, Object newValue) {
        }

        @Override
        public Object getProperty(Object object) {
            return null;
        }
    };

    public MixedGrailsControllerHelper() {
        super();
    }

    @Override
    protected Object retrieveAction(GroovyObject controller, String actionName, HttpServletResponse response) {
        Pair<Class<?>, String> key = new Pair<Class<?>, String>(controller.getClass(), actionName);

        Method mAction = controllerToActionMethodCache.get(key);

        if (mAction != null && mAction != NULL_METHOD_HOLDER) {
            return mAction;
        }

        MetaProperty metaProperty = controllerToMetaPropertyCache.get(key);

        if (metaProperty == null) {
            Class<?> controllerClass = AopProxyUtils.ultimateTargetClass(controller);

            mAction = ReflectionUtils.findMethod(controllerClass, actionName, NOARGS);
            if (mAction != null) {
                ReflectionUtils.makeAccessible(mAction);
                if (mAction.getAnnotation(Action.class) != null) {
                    if (!developmentMode) {
                        controllerToActionMethodCache.put(key, mAction);
                    }
                    return mAction;
                }
                else if (!developmentMode) {
                    controllerToActionMethodCache.put(key, NULL_METHOD_HOLDER);
                }
            }
        }

        if (metaProperty == null) {
            metaProperty = controller.getMetaClass().getMetaProperty(actionName);
            if (!developmentMode) {
                if (metaProperty != null) {
                    controllerToMetaPropertyCache.put(key, metaProperty);
                }
                else {
                    controllerToMetaPropertyCache.put(key, NULL_META_PROPERTY_HOLDER);
                }
            }
        }

        Object closureAction = null;

        if (metaProperty != null && metaProperty != NULL_META_PROPERTY_HOLDER) {
            if (metaProperty.getType() == Object.class || Closure.class.isAssignableFrom(metaProperty.getType())) {
                closureAction = metaProperty.getProperty(controller);
            }
            else {
                log.error("Invalid type for " + actionName + " in " + controller.getClass().getName() + ". type is "
                        + metaProperty.getType());
                if (!developmentMode) {
                    controllerToMetaPropertyCache.put(key, NULL_META_PROPERTY_HOLDER);
                }
            }
        }

        if (!(closureAction instanceof Closure)) {
            try {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return null;
            }
            catch (IOException e) {
                throw new ControllerExecutionException("I/O error sending 404 error", e);
            }
        }

        return closureAction;
    }

    @Override
    protected Object invoke(GroovyObject controller, Object action) {
        try {
            if (action.getClass() == Method.class) {
                return ((Method)action).invoke(controller);
            }
            return ((Closure<?>)action).call();
        }
        catch (Exception e) {
            throw new ControllerExecutionException("Runtime error executing action", e);
        }
    }
}
