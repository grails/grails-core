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

import grails.util.Environment;
import grails.util.GrailsNameUtils;
import grails.util.Pair;
import grails.util.Triple;
import grails.web.Action;
import grails.web.UrlConverter;
import groovy.lang.Closure;
import groovy.lang.GroovyObject;
import groovy.lang.MetaProperty;

import java.beans.FeatureDescriptor;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.springframework.context.ApplicationContext;
import org.springframework.util.AntPathMatcher;

/**
 * Evaluates the conventions contained within controllers to perform auto-configuration.
 *
 * @author Graeme Rocher
 * @author Steven Devijver
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
    private Map<String, String> uri2methodMap = new HashMap<String, String>();
    private Map<String, String> viewNames = new HashMap<String, String>();
    private String[] uris;
    private String uri;

    private AntPathMatcher pathMatcher = new AntPathMatcher();

    private Map<String, FeatureDescriptor> flows = new HashMap<String, FeatureDescriptor>();
    private UrlConverter urlConverter;

    private final boolean developerMode = Environment.isDevelopmentMode();
    private Map<Pair<Class<?>, String>, Boolean> isInterceptedBeforeCache = new ConcurrentHashMap<Pair<Class<?>, String>, Boolean>();
    private Map<Pair<Class<?>, String>, Boolean> isInterceptedAfterCache = new ConcurrentHashMap<Pair<Class<?>, String>, Boolean>();
    private Map<Triple<Class<?>, String, String>, Boolean> isHttpMethodAllowedCache = new ConcurrentHashMap<Triple<Class<?>, String, String>, Boolean>();

    public void setDefaultActionName(String defaultActionName) {
        this.defaultActionName = defaultActionName;
        configureDefaultActionIfSet();
        configureURIsForCurrentState();
    }

    private String defaultActionName;
    private String namespace;
    private String controllerPath;

    public DefaultGrailsControllerClass(Class<?> clazz) {
        super(clazz, CONTROLLER);
        namespace = getStaticPropertyValue(NAMESPACE_PROPERTY, String.class);
        defaultActionName = getStaticPropertyValue(DEFAULT_CLOSURE_PROPERTY, String.class);
        if (defaultActionName == null) {
            defaultActionName = INDEX_ACTION;
        }
    }

    public void initialize() {
        ApplicationContext mainContext = grailsApplication.getMainContext();
        urlConverter = mainContext.getBean("grailsUrlConverter", UrlConverter.class);
        uri = SLASH + urlConverter.toUrlElement(getName());
        controllerPath = uri + SLASH;

        Collection<String> actionNames = new HashSet<String>();
        flowStrategy(actionNames);
        methodStrategy(actionNames);

        configureDefaultActionIfSet();
        configureURIsForCurrentState();
    }

    //Todo change flow resolution
    private void flowStrategy(Collection<String> closureNames) {

        for (PropertyDescriptor propertyDescriptor : getPropertyDescriptors()) {
            Method readMethod = propertyDescriptor.getReadMethod();
            if (readMethod != null && !Modifier.isStatic(readMethod.getModifiers())) {
                final Class<?> propertyType = propertyDescriptor.getPropertyType();
                if ((propertyType == Object.class || propertyType == Closure.class) && propertyDescriptor.getName().endsWith(FLOW_SUFFIX)) {
                    String closureName = propertyDescriptor.getName();
                    String flowId = closureName.substring(0, closureName.length() - FLOW_SUFFIX.length());
                    flows.put(flowId, propertyDescriptor);
                    closureNames.add(flowId);
                    configureMappingForMethodAction(flowId);
                }
            }
        }
        if (!isReadableProperty(defaultActionName) && closureNames.size() == 1) {
            defaultActionName = closureNames.iterator().next();
        }
    }

    private void methodStrategy(Collection<String> methodNames) {

        Class superClass = getClazz();

        while (superClass != null && superClass != Object.class && superClass != GroovyObject.class) {
            for (Method method : superClass.getMethods()) {
                if (Modifier.isPublic(method.getModifiers()) && method.getAnnotation(Action.class) != null) {
                    String methodName = method.getName();

                    if (!methodName.endsWith(FLOW_SUFFIX)) {
                        methodNames.add(methodName);
                        configureMappingForMethodAction(methodName);
                    }
                }
            }
            superClass = superClass.getSuperclass();
        }

        if (!isActionMethod(defaultActionName) && methodNames.size() == 1 && !isReadableProperty("scaffold")) {
            defaultActionName = methodNames.iterator().next();
        }
    }

    private void configureURIsForCurrentState() {
        uris = uri2methodMap.keySet().toArray(new String[uri2methodMap.keySet().size()]);
    }

    private void configureDefaultActionIfSet() {
        if (defaultActionName == null) {
            return;
        }

        final String defaultViewPath = SLASH + GrailsNameUtils.getPropertyNameRepresentation(getName()) + SLASH + defaultActionName;

        uri2methodMap.put(uri, defaultActionName);
        uri2methodMap.put(controllerPath, defaultActionName);
        uri2viewMap.put(controllerPath, defaultViewPath);
        uri2viewMap.put(uri, defaultViewPath);
        viewNames.put(defaultActionName, defaultViewPath);
    }

    private void configureMappingForMethodAction(String closureName) {
        String tmpUri = controllerPath + urlConverter.toUrlElement(closureName);
        uri2methodMap.put(tmpUri, closureName);
        uri2methodMap.put(tmpUri + SLASH + "**", closureName);

        String viewPath = SLASH + GrailsNameUtils.getPropertyNameRepresentation(getName()) + SLASH + closureName;
        uri2viewMap.put(tmpUri, viewPath);
        viewNames.put(closureName, viewPath);
    }

    public String[] getURIs() {
        return uris;
    }

    public boolean mapsToURI(String uri) {
        for (String uri1 : uris) {
            if (pathMatcher.match(uri1, uri)) {
                return true;
            }
        }
        return false;
    }

    public String getViewByURI(String uri) {
        return uri2viewMap.get(uri);
    }

    public String getMethodActionName(String uri) {
        return uri2methodMap.get(uri);
    }

    public String getViewByName(String viewName) {
        if (viewNames.containsKey(viewName)) {
            return viewNames.get(viewName);
        }

        return uri + SLASH + viewName;
    }

    public boolean isInterceptedBefore(GroovyObject controller, String action) {
        Pair<Class<?>, String> key = new Pair<Class<?>, String>(controller.getClass(), action);
        Boolean interceptedBefore = isInterceptedBeforeCache.get(key);
        if (interceptedBefore == null) {
            interceptedBefore = controller.getMetaClass().hasProperty(controller, BEFORE_INTERCEPTOR) != null &&
                    isIntercepted(controller.getProperty(BEFORE_INTERCEPTOR), action);
            if (!developerMode) {
                isInterceptedBeforeCache.put(key, interceptedBefore);
            }
        }
        return interceptedBefore;
    }

    private boolean isIntercepted(Object bip, String action) {
        if (bip instanceof Closure) {
            return true;
        }

        if (!(bip instanceof Map)) {
            return false;
        }

        Map bipMap = (Map) bip;
        if (bipMap.containsKey(EXCEPT)) {
            Object excepts = bipMap.get(EXCEPT);
            if (excepts instanceof String) {
                if (!excepts.equals(action)) {
                    return true;
                }
            } else if (excepts instanceof List) {
                if (!((List) excepts).contains(action)) {
                    return true;
                }
            }
        } else if (bipMap.containsKey(ONLY)) {
            Object onlys = bipMap.get(ONLY);
            if (onlys instanceof String) {
                if (onlys.equals(action)) {
                    return true;
                }
            } else if (onlys instanceof List) {
                if (((List) onlys).contains(action)) {
                    return true;
                }
            }
        }
        else {
            return true;
        }

        return false;
    }

    public boolean isHttpMethodAllowedForAction(GroovyObject controller, final String httpMethod, String actionName) {
        Triple<Class<?>, String, String> key = new Triple<Class<?>, String, String>(controller.getClass(), actionName, httpMethod);
        Boolean httpMethodAllowed = isHttpMethodAllowedCache.get(key);
        if (httpMethodAllowed == null) {
            httpMethodAllowed = doCheckIsHttpMethodAllowedForAction(controller, httpMethod, actionName);
            if (!developerMode) {
                isHttpMethodAllowedCache.put(key, httpMethodAllowed);
            }
        }
        return httpMethodAllowed;
    }

    protected boolean doCheckIsHttpMethodAllowedForAction(GroovyObject controller, final String httpMethod, String actionName) {
        Object methodRestrictionsProperty = null;
        MetaProperty metaProp = controller.getMetaClass().getMetaProperty(ALLOWED_HTTP_METHODS_PROPERTY);
        if (metaProp != null) {
            methodRestrictionsProperty = metaProp.getProperty(controller);
        }
        if (!(methodRestrictionsProperty instanceof Map)) {
            return true;
        }

        Map map = (Map) methodRestrictionsProperty;
        Object value = map.get(actionName);

        if (value instanceof String) {
            return ((String) value).equalsIgnoreCase(httpMethod);
        }

        if (!(value instanceof List)) {
            return true;
        }

        return CollectionUtils.exists((List)value, new Predicate() {
            public boolean evaluate(Object method) {
                return httpMethod.equalsIgnoreCase(method.toString());
            }
        });
    }

    public boolean isInterceptedAfter(GroovyObject controller, String action) {
        Pair<Class<?>, String> key = new Pair<Class<?>, String>(controller.getClass(), action);
        Boolean interceptedAfter = isInterceptedAfterCache.get(key);
        if (interceptedAfter == null) {
            interceptedAfter = controller.getMetaClass().hasProperty(controller, AFTER_INTERCEPTOR) != null &&
                    isIntercepted(controller.getProperty(AFTER_INTERCEPTOR), action);
            if (!developerMode) {
                isInterceptedAfterCache.put(key, interceptedAfter);
            }
        }
        return interceptedAfter;
    }

    public Closure getBeforeInterceptor(GroovyObject controller) {
        if (isReadableProperty(BEFORE_INTERCEPTOR)) {
            return getInterceptor(controller, controller.getProperty(BEFORE_INTERCEPTOR));
        }
        return null;
    }

    public Closure getAfterInterceptor(GroovyObject controller) {
        if (isReadableProperty(AFTER_INTERCEPTOR)) {
            return getInterceptor(controller, controller.getProperty(AFTER_INTERCEPTOR));
        }
        return null;
    }

    private Closure getInterceptor(GroovyObject controller, Object ip) {
        Closure interceptor = null;
        if (ip instanceof Map) {
            Map ipMap = (Map)ip;
            if (ipMap.containsKey(ACTION)) {
                interceptor = (Closure)ipMap.get(ACTION);
            }
        } else if (ip instanceof Closure) {
            interceptor = (Closure)ip;
        }
        if (interceptor != null && interceptor.getDelegate() != controller) {
            interceptor = (Closure)interceptor.clone();
            interceptor.setDelegate(controller);
            interceptor.setResolveStrategy(Closure.DELEGATE_FIRST);
        }
        return interceptor;
    }

    /**
     * @return EMPTY_SET until the method is removed
     * @deprecated This method is deprecated and will be removed in a future version of Grails
     */
    @Deprecated
    public Set getCommandObjectActions() {
        return Collections.EMPTY_SET;
    }

    /**
     * @return EMPTY_SET until the method is removed
     * @deprecated This method is deprecated and will be removed in a future version of Grails
     */
    @Deprecated
    public Set getCommandObjectClasses() {
        return Collections.EMPTY_SET;
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

    public String getNamespace() {
        return namespace;
    }

    public void registerMapping(String actionName) {
        configureMappingForMethodAction(actionName);
        configureURIsForCurrentState();
    }
}
