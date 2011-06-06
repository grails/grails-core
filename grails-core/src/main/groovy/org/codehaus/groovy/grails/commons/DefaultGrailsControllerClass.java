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
package org.codehaus.groovy.grails.commons;

import grails.util.GrailsNameUtils;
import grails.web.Action;
import groovy.lang.Closure;
import groovy.lang.GroovyObject;
import groovy.lang.MetaProperty;

import java.beans.FeatureDescriptor;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.springframework.util.AntPathMatcher;

/**
 * Evaluates the conventions contained within controllers to perform auto-configuration.
 *
 * @author Graeme Rocher
 * @author Steven Devijver
 *
 * @since 0.1
 */
@SuppressWarnings("rawtypes")
public class DefaultGrailsControllerClass extends AbstractInjectableGrailsClass implements GrailsControllerClass {

    public static final String CONTROLLER = "Controller";

    private static final String SLASH = "/";
    private static final String DEFAULT_CLOSURE_PROPERTY = "defaultAction";
    private static final String ALLOWED_HTTP_METHODS_PROPERTY = "allowedMethods";

    private static final String EXCEPT = "except";
    private static final String ONLY = "only";
    private static final String FLOW_SUFFIX = "Flow";

    private static final String ACTION = "action";
    private Map<String, String> uri2viewMap = new HashMap<String, String>();
    private Map<String, String> uri2closureMap = new HashMap<String, String>();
    private Map<String, String> viewNames = new HashMap<String, String>();
    private String[] uris;
    private String uri;

    private AntPathMatcher pathMatcher = new AntPathMatcher();

    private final Set commandObjectActions = new HashSet();
    private final Set commandObjectClasses = new HashSet();
    private Map<String, FeatureDescriptor> flows = new HashMap<String, FeatureDescriptor>();

    public void setDefaultActionName(String defaultActionName) {
        this.defaultActionName = defaultActionName;
        configureDefaultActionIfSet();
        configureURIsForCurrentState();
    }

    private String defaultActionName;
    private String controllerPath;

    public DefaultGrailsControllerClass(Class<?> clazz) {
        super(clazz, CONTROLLER);
        uri = SLASH + GrailsNameUtils.getPropertyNameRepresentation(getName());
        defaultActionName = getStaticPropertyValue(DEFAULT_CLOSURE_PROPERTY, String.class);
        if (defaultActionName == null) {
            defaultActionName = INDEX_ACTION;
        }
        Collection<String> actionNames = new ArrayList<String>();

        controllerPath = uri + SLASH;

        mixedStrategy(actionNames);

        configureDefaultActionIfSet();
        configureURIsForCurrentState();
    }

    private void mixedStrategy(Collection<String> actionNames) {
        closureStrategy(actionNames);
        methodStrategy(actionNames);
    }

    private void closureStrategy(Collection<String> closureNames) {

        for (PropertyDescriptor propertyDescriptor : getPropertyDescriptors()) {
            Method readMethod = propertyDescriptor.getReadMethod();
            if (readMethod != null && !Modifier.isStatic(readMethod.getModifiers())) {
                final Class<?> propertyType = propertyDescriptor.getPropertyType();
                if (propertyType == Object.class || propertyType == Closure.class) {
                    String closureName = propertyDescriptor.getName();
                    if (closureName.endsWith(FLOW_SUFFIX)) {
                        String flowId = closureName.substring(0, closureName.length()-FLOW_SUFFIX.length());
                        flows.put(flowId, propertyDescriptor);
                        closureName = flowId;
                    }
                    closureNames.add(closureName);

                    configureMappingForClosureProperty(closureName);
                }
            }
        }

        if (!isReadableProperty(defaultActionName) && closureNames.size() == 1) {
            defaultActionName = closureNames.iterator().next();
        }
    }

    private void methodStrategy(Collection<String> methodNames) {

        for (Method method : getClazz().getMethods()) {
            if (Modifier.isPublic(method.getModifiers())
                    && method.getAnnotation(Action.class) != null) {
                String methodName = method.getName();

                methodNames.add(methodName);

                configureMappingForClosureProperty(methodName);
            }
        }

        if (!isActionMethod(defaultActionName) && methodNames.size() == 1) {
            defaultActionName = methodNames.iterator().next();
        }
    }

    private void configureURIsForCurrentState() {
        uris = uri2closureMap.keySet().toArray(new String[uri2closureMap.keySet().size()]);
    }

    private void configureDefaultActionIfSet() {
        if (defaultActionName == null) {
            return;
        }

        uri2closureMap.put(uri, defaultActionName);
        uri2closureMap.put(controllerPath, defaultActionName);
        uri2viewMap.put(controllerPath, controllerPath + defaultActionName);
        uri2viewMap.put(uri, controllerPath +  defaultActionName);
        viewNames.put(defaultActionName, controllerPath + defaultActionName);
    }

    private void configureMappingForClosureProperty(String closureName) {
        String tmpUri = controllerPath + closureName;
        uri2closureMap.put(tmpUri,closureName);
        uri2closureMap.put(tmpUri + SLASH + "**",closureName);
        uri2viewMap.put(tmpUri, tmpUri);
        viewNames.put(closureName, tmpUri);
    }

    public String[] getURIs() {
        return uris;
    }

    public boolean mapsToURI(@SuppressWarnings("hiding") String uri) {
        for (int i = 0; i < uris.length; i++) {
            if (pathMatcher.match(uris[i], uri)) {
                return true;
            }
        }
        return false;
    }

    public String getViewByURI(@SuppressWarnings("hiding") String uri) {
        return uri2viewMap.get(uri);
    }

    public String getClosurePropertyName(@SuppressWarnings("hiding") String uri) {
        return uri2closureMap.get(uri);
    }

    public String getViewByName(String viewName) {
        if (viewNames.containsKey(viewName)) {
            return viewNames.get(viewName);
        }

        return uri + SLASH + viewName;
    }

    public boolean isInterceptedBefore(GroovyObject controller, String action) {
        return controller.getMetaClass().hasProperty(controller, BEFORE_INTERCEPTOR) != null &&
            isIntercepted(controller.getProperty(BEFORE_INTERCEPTOR), action);
    }

    private boolean isIntercepted(Object bip, String action) {
        if (bip instanceof Map) {
            Map bipMap = (Map)bip;
            if (bipMap.containsKey(EXCEPT)) {
                Object excepts = bipMap.get(EXCEPT);
                if (excepts instanceof String) {
                    if (!excepts.equals(action)) {
                        return true;
                    }
                }
                else if (excepts instanceof List) {
                    if (!((List)excepts).contains(action)) {
                        return true;
                    }
                }
            }
            else if (bipMap.containsKey(ONLY)) {
                Object onlys = bipMap.get(ONLY);
                if (onlys instanceof String) {
                    if (onlys.equals(action)) {
                        return true;
                    }
                }
                else if (onlys instanceof List) {
                    if (((List)onlys).contains(action)) {
                        return true;
                    }
                }
            }
            else {
                return true;
            }
        }
        else if (bip instanceof Closure) {
            return true;
        }
        return false;
    }

    public boolean isHttpMethodAllowedForAction(GroovyObject controller, final String httpMethod, String actionName) {
        boolean isAllowed = true;
        Object methodRestrictionsProperty = null;
        MetaProperty metaProp=controller.getMetaClass().getMetaProperty(ALLOWED_HTTP_METHODS_PROPERTY);
        if (metaProp != null) {
            methodRestrictionsProperty = metaProp.getProperty(controller);
        }
        if (methodRestrictionsProperty instanceof Map) {
            Map map = (Map)methodRestrictionsProperty;
            Object value = map.get(actionName);
            if (value instanceof List) {
                List listOfMethods = (List)value;
                isAllowed = CollectionUtils.exists(listOfMethods, new Predicate() {
                    public boolean evaluate(@SuppressWarnings("hiding") Object value) {
                        return httpMethod.equalsIgnoreCase(value.toString());
                    }
                });
            }
            else if (value instanceof String) {
                isAllowed = ((String) value).equalsIgnoreCase(httpMethod);
            }
        }
        return isAllowed;
    }

    public boolean isInterceptedAfter(GroovyObject controller, String action) {
        return controller.getMetaClass().hasProperty(controller, AFTER_INTERCEPTOR) != null &&
            isIntercepted(controller.getProperty(AFTER_INTERCEPTOR), action);
    }

    public Closure getBeforeInterceptor(GroovyObject controller) {
        if (isReadableProperty(BEFORE_INTERCEPTOR)) {
            return getInterceptor(controller.getProperty(BEFORE_INTERCEPTOR));
        }
        return null;
    }

    public Closure getAfterInterceptor(GroovyObject controller) {
        if (isReadableProperty(AFTER_INTERCEPTOR)) {
            return getInterceptor(controller.getProperty(AFTER_INTERCEPTOR));
        }
        return null;
    }

    private Closure getInterceptor(Object ip) {
        if (ip instanceof Map) {
            Map ipMap = (Map)ip;
            if (ipMap.containsKey(ACTION)) {
                return (Closure)ipMap.get(ACTION);
            }
        }
        else if (ip instanceof Closure) {
            return (Closure)ip;
        }
        return null;
    }

    /**
     * @deprecated This method is deprecated and will be removed in a future version of Grails
     */
    @Deprecated
    public Set getCommandObjectActions() {
        return commandObjectActions;
    }

    /**
     * @deprecated This method is deprecated and will be removed in a future version of Grails
     */
    @SuppressWarnings("unchecked")
    @Deprecated
    public Set getCommandObjectClasses() {
        return Collections.unmodifiableSet(commandObjectClasses);
    }

    public Map<String, Closure> getFlows() {
        Map<String, Closure> closureFlows = new HashMap<String, Closure>();
        for (String name : flows.keySet()) {
            Closure c = getPropertyValue(name + "Flow", Closure.class);
            if (c != null) {
                closureFlows.put(name, c);
            }
        }
        return closureFlows;
    }

    public boolean isFlowAction(String actionName) {
        return flows.containsKey(actionName);
    }

    public String getDefaultAction() {
        return defaultActionName;
    }

    public void registerMapping(String actionName) {
        configureMappingForClosureProperty(actionName);
        configureURIsForCurrentState();
    }
}
