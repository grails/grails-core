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
package org.grails.core;

import grails.core.GrailsControllerClass;
import grails.web.Action;
import groovy.lang.GroovyObject;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Evaluates the conventions contained within controllers to perform auto-configuration.
 *
 * @author Graeme Rocher
 * @author Steven Devijver
 * @since 0.1
 */
@SuppressWarnings("rawtypes")
public class DefaultGrailsControllerClass extends AbstractInjectableGrailsClass implements GrailsControllerClass, org.codehaus.groovy.grails.commons.GrailsControllerClass {

    public static final String CONTROLLER = "Controller";

    private static final String DEFAULT_CLOSURE_PROPERTY = "defaultAction";
    public static final String ALLOWED_HTTP_METHODS_PROPERTY = "allowedMethods";
    private Map<String, Method> actions = new HashMap<String, Method>();
    private String defaultActionName;
    private String namespace;

    public DefaultGrailsControllerClass(Class<?> clazz) {
        super(clazz, CONTROLLER);
        namespace = getStaticPropertyValue(NAMESPACE_PROPERTY, String.class);
        defaultActionName = getStaticPropertyValue(DEFAULT_CLOSURE_PROPERTY, String.class);
        if (defaultActionName == null) {
            defaultActionName = INDEX_ACTION;
        }
        methodStrategy(actions);
    }

    public void initialize() {}

    @Override
    public Set<String> getActions() {
        return actions.keySet();
    }

    public String getNamespace() {
        return namespace;
    }

    @Override
    public String getDefaultAction() {
        return this.defaultActionName;
    }

    private void methodStrategy(Map<String, Method> methodNames) {
        Class superClass = getClazz();
        while (superClass != null && superClass != Object.class && superClass != GroovyObject.class) {
            for (Method method : superClass.getMethods()) {
                if (Modifier.isPublic(method.getModifiers()) && method.getAnnotation(Action.class) != null) {
                    String methodName = method.getName();

                    ReflectionUtils.makeAccessible(method);
                    methodNames.put(methodName, method);
                }
            }
            superClass = superClass.getSuperclass();
        }

        if (!isActionMethod(defaultActionName) && methodNames.size() == 1 && !isReadableProperty("scaffold")) {
            defaultActionName = methodNames.keySet().iterator().next();
        }
    }

    @Override
    public boolean mapsToURI(String uri) {
        if(uri.startsWith("/")) {
            String[] tokens = uri.substring(1).split("\\/");
            if(tokens.length>0) {
                String controllerName = tokens[0];
                if(getLogicalPropertyName().equals(controllerName)) {
                    if(tokens.length>1) {
                        String actionName = tokens[1];
                        if(actions.containsKey(actionName) || defaultActionName.equals(actionName)) {
                            return true;
                        }
                    }
                    else {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Invokes the controller action for the given name on the given controller instance
     *
     * @param controller The controller instance
     * @param action The action name
     * @return The result of the action
     * @throws Throwable
     */
    @Override
    public Object invoke(Object controller, String action) throws Throwable {
        if(action == null) action = this.defaultActionName;
        Method handle = actions.get(action);
        if(handle == null) throw new IllegalArgumentException("Invalid action name: " + action);
        return ReflectionUtils.invokeMethod(handle, controller);
    }

}
