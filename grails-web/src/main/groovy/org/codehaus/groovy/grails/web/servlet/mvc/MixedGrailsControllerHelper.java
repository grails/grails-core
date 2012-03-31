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

import grails.web.Action;
import groovy.lang.Closure;
import groovy.lang.GroovyObject;
import groovy.lang.MissingPropertyException;

import java.io.IOException;
import java.lang.reflect.Method;

import javax.servlet.http.HttpServletResponse;

import org.codehaus.groovy.grails.web.servlet.mvc.exceptions.ControllerExecutionException;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Implements action invocation through Closure.
 *
 * @author Stephane Maldini
 * @since 2.0
 */
public class MixedGrailsControllerHelper extends AbstractGrailsControllerHelper {

    @Override
    protected Object retrieveAction(GroovyObject controller, String actionName, HttpServletResponse response) {

        Class<?> controllerClass = AopProxyUtils.ultimateTargetClass(controller);

        Method mAction = ReflectionUtils.findMethod(controllerClass, actionName, MethodGrailsControllerHelper.NOARGS);
        if (mAction != null) {
            ReflectionUtils.makeAccessible(mAction);
            if (mAction.getAnnotation(Action.class) != null) {
                return mAction;
            }
        }

        try {
            return controller.getProperty(actionName);
        } catch (MissingPropertyException mpe) {
            try {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return null;
            } catch (IOException e) {
                throw new ControllerExecutionException("I/O error sending 404 error", e);
            }
        }
    }

    @Override
    protected Object invoke(GroovyObject controller, Object action) {
        try {
            if (action.getClass() == Method.class) {
                return ((Method) action).invoke(controller);
            }
            return ((Closure<?>) action).call();
        } catch (Exception e) {
            throw new ControllerExecutionException("Runtime error executing action", e);
        }
    }
}
