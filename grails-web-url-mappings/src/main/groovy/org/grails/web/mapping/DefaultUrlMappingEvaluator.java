/*
 * Copyright 2004-2005 Graeme Rocher
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
package org.grails.web.mapping;

import grails.core.GrailsApplication;
import grails.core.GrailsControllerClass;
import grails.core.support.ClassLoaderAware;
import grails.gorm.validation.ConstrainedProperty;
import grails.gorm.validation.DefaultConstrainedProperty;
import grails.io.IOUtils;
import grails.plugins.GrailsPluginManager;
import grails.plugins.PluginManagerAware;
import grails.util.GrailsUtil;
import grails.web.mapping.UrlMapping;
import grails.web.mapping.UrlMappingData;
import grails.web.mapping.UrlMappingEvaluator;
import grails.web.mapping.UrlMappingParser;
import grails.web.mapping.exceptions.UrlMappingException;
import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import groovy.lang.GroovyObjectSupport;
import groovy.lang.Script;
import org.codehaus.groovy.runtime.IOGroovyMethods;
import org.grails.datastore.gorm.validation.constraints.builder.ConstrainedPropertyBuilder;
import org.grails.datastore.gorm.validation.constraints.eval.ConstraintsEvaluator;
import org.grails.datastore.gorm.validation.constraints.eval.DefaultConstraintEvaluator;
import org.grails.datastore.gorm.validation.constraints.registry.ConstraintRegistry;
import org.grails.datastore.gorm.validation.constraints.registry.DefaultConstraintRegistry;
import org.grails.datastore.mapping.keyvalue.mapping.config.KeyValueMappingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;

import jakarta.servlet.ServletContext;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>A UrlMapping evaluator that evaluates Groovy scripts that are in the form:</p>
 * <p/>
 * <pre>
 * <code>
 * mappings {
 *    /$post/$year?/$month?/$day?" {
 *       controller = "blog"
 *       action = "show"
 *       constraints {
 *           year(matches:/\d{4}/)
 *           month(matches:/\d{2}/)
 *       }
 *    }
 * }
 * </code>
 * </pre>
 *
 * @author Graeme Rocher
 * @since 0.5
 */
public class DefaultUrlMappingEvaluator implements UrlMappingEvaluator, ClassLoaderAware, PluginManagerAware {

    public static final String ACTION_CREATE = "create";
    public static final String ACTION_INDEX = "index";
    public static final String ACTION_SHOW = "show";
    public static final String ACTION_EDIT = "edit";
    public static final String ACTION_UPDATE = "update";
    public static final String ACTION_PATCH = "patch";
    public static final String ACTION_DELETE = "delete";
    public static final String ACTION_SAVE = "save";
    public static final List<String> DEFAULT_RESOURCES_INCLUDES = Arrays.asList(ACTION_INDEX, ACTION_CREATE, ACTION_SAVE, ACTION_SHOW, ACTION_EDIT, ACTION_UPDATE, ACTION_PATCH, ACTION_DELETE);
    public static final List<String> DEFAULT_RESOURCE_INCLUDES = Arrays.asList(ACTION_CREATE, ACTION_SAVE, ACTION_SHOW, ACTION_EDIT, ACTION_UPDATE, ACTION_PATCH, ACTION_DELETE);
    private static final Logger LOG = LoggerFactory.getLogger(UrlMappingBuilder.class);
    private GroovyClassLoader classLoader = new GroovyClassLoader();
    private UrlMappingParser urlParser = new DefaultUrlMappingParser();
    private static final String EXCEPTION = "exception";
    private static final String PARSE_REQUEST = "parseRequest";
    private static final String SINGLE = "single";
    private static final String RESOURCE = "resource";
    private static final String RESOURCES = "resources";

    private GrailsPluginManager pluginManager;
    private ApplicationContext applicationContext;
    private GrailsApplication grailsApplication;
    private final ConstraintRegistry constraintRegistry;
    private final ConstraintsEvaluator constraintsEvaluator;

    public DefaultUrlMappingEvaluator(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        if (applicationContext != null) {
            this.grailsApplication = applicationContext.getBean(GrailsApplication.class);

            ConstraintRegistry constraintRegistry;
            try {
                constraintRegistry = applicationContext.getBean(ConstraintRegistry.class);
            } catch (BeansException e) {
                constraintRegistry = new DefaultConstraintRegistry(applicationContext);
            }
            this.constraintRegistry = constraintRegistry;

            ConstraintsEvaluator constraintEvaluator;
            try {
                constraintEvaluator = applicationContext.getBean(ConstraintsEvaluator.class);
            } catch (BeansException e) {
                constraintEvaluator = new DefaultConstraintEvaluator(constraintRegistry, new KeyValueMappingContext("test"), Collections.<String, Object>emptyMap());
            }
            this.constraintsEvaluator = constraintEvaluator;
        }
        else {
            StaticMessageSource messageSource = new StaticMessageSource();
            this.constraintRegistry = new DefaultConstraintRegistry(messageSource);
            this.constraintsEvaluator = new DefaultConstraintEvaluator(messageSource);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public List evaluateMappings(Resource resource) {
        InputStream inputStream = null;
        try {
            inputStream = resource.getInputStream();
            return evaluateMappings(classLoader.parseClass(IOGroovyMethods.getText(inputStream, "UTF-8")));
        } catch (IOException e) {
            throw new UrlMappingException("Unable to read mapping file [" + resource.getFilename() + "]: " + e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<UrlMapping> evaluateMappings(Class theClass) {
        GroovyObject obj = (GroovyObject) BeanUtils.instantiateClass(theClass);

        if (obj instanceof Script) {
            Script script = (Script) obj;
            Binding b = new Binding();

            MappingCapturingClosure closure = new MappingCapturingClosure(script);
            b.setVariable("mappings", closure);
            script.setBinding(b);

            script.run();

            Closure mappings = closure.getMappings();

            Binding binding = script.getBinding();
            return evaluateMappings(script, mappings, binding);
        }

        throw new UrlMappingException("Unable to configure URL mappings for class [" + theClass +
                "]. A URL mapping must be an instance of groovy.lang.Script.");
    }

    private List<UrlMapping> evaluateMappings(GroovyObject go, Closure<?> mappings, Binding binding) {
        UrlMappingBuilder builder = new UrlMappingBuilder(binding);
        mappings.setDelegate(builder);
        mappings.call();
        builder.urlDefiningMode = false;

        return builder.getUrlMappings();
    }

    @SuppressWarnings("rawtypes")
    public List<UrlMapping> evaluateMappings(Closure closure) {
        UrlMappingBuilder builder = new UrlMappingBuilder((Binding) null);
        closure.setDelegate(builder);
        closure.setResolveStrategy(Closure.DELEGATE_FIRST);
        if (closure.getParameterTypes().length == 0) {
            closure.call();
        } else {
            closure.call(applicationContext);
        }
        builder.urlDefiningMode = false;
        return builder.getUrlMappings();
    }

    public void setClassLoader(ClassLoader classLoader) {
        Assert.isInstanceOf(GroovyClassLoader.class, classLoader,
                "Property [classLoader] must be an instance of GroovyClassLoader");
        this.classLoader = (GroovyClassLoader) classLoader;
    }

    public void setPluginManager(GrailsPluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    /**
     * A Closure that captures a call to a method that accepts a single closure
     */
    @SuppressWarnings("rawtypes")
    class MappingCapturingClosure extends Closure {

        private static final long serialVersionUID = 2108155626252742722L;
        private Closure<?> mappings;

        public Closure<?> getMappings() {
            return mappings;
        }

        public MappingCapturingClosure(Object o) {
            super(o);
        }

        @Override
        public Object call(Object... args) {
            if (args.length > 0 && (args[0] instanceof Closure)) {
                mappings = (Closure<?>) args[0];
            }
            return null;
        }
    }

    /**
     * <p>A modal builder that constructs a UrlMapping instances by executing a closure. The class overrides
     * getProperty(name) and allows the substitution of GString values with the * wildcard.
     * <p/>
     * <p>invokeMethod(methodName, args) is also overriden for the creation of each UrlMapping instance
     */
    @SuppressWarnings("rawtypes")
    class UrlMappingBuilder extends GroovyObjectSupport {
        private static final String CAPTURING_WILD_CARD = UrlMapping.CAPTURED_WILDCARD;
        private static final String SLASH = "/";
        private static final String CONSTRAINTS = "constraints";

        private boolean urlDefiningMode = true;
        private boolean inGroupConstraints = false;
        private List<ConstrainedProperty> previousConstraints = new ArrayList<>();
        private List<UrlMapping> urlMappings = new ArrayList<>();
        private Map<String, Object> parameterValues = new HashMap<>();
        private Binding binding;
        private Object exception;
        private Object parseRequest;
        private Deque<ParentResource> parentResources = new ArrayDeque<>();
        private Deque<MetaMappingInfo> mappingInfoDeque = new ArrayDeque<>();
        private boolean isInCollection;

        public UrlMappingBuilder(Binding binding) {
            this.binding = binding;
        }

        protected UrlMappingBuilder(UrlMappingBuilder parent) {
            this(parent.binding);
            urlDefiningMode = parent.urlDefiningMode;
            previousConstraints = parent.previousConstraints;
            inGroupConstraints = parent.inGroupConstraints;
            urlMappings = parent.urlMappings;
            parameterValues = parent.parameterValues;
            exception = parent.exception;
            parseRequest = parent.parseRequest;
            parentResources = parent.parentResources;
            mappingInfoDeque = parent.mappingInfoDeque;
        }

        public List<UrlMapping> getUrlMappings() {
            return urlMappings;
        }

        public ServletContext getServletContext() {
            final ApplicationContext ctx = getApplicationContext();
            if (ctx instanceof WebApplicationContext) {
                return ((WebApplicationContext) ctx).getServletContext();
            }
            return null;
        }

        public ApplicationContext getApplicationContext() {
            return applicationContext;
        }

        public GrailsApplication getGrailsApplication() {
            return grailsApplication;
        }

        @Override
        public Object getProperty(String name) {
            if (urlDefiningMode) {
                final ConstrainedProperty newConstrained = new DefaultConstrainedProperty(UrlMapping.class, name, String.class, DefaultUrlMappingEvaluator.this.constraintRegistry);
                previousConstraints.add(newConstrained);
                return CAPTURING_WILD_CARD;
            }
            return super.getProperty(name);
        }

        public Object getException() {
            return exception;
        }

        public void setException(Object exception) {
            this.exception = exception;
        }

        public Object getUri() {
            return getMetaMappingInfo().getUri();
        }

        public void setUri(Object uri) {
            getMetaMappingInfo().setUri(uri);
        }

        public void setAction(Object action) {
            getMetaMappingInfo().setAction(action);
        }

        public Object getAction() {
            return getMetaMappingInfo().getAction();
        }

        public void setController(Object controller) {
            getMetaMappingInfo().setController(controller);
        }

        public Object getController() {
            return getMetaMappingInfo().getController();
        }

        public void setRedirectInfo(Object redirectInfo) {
            getMetaMappingInfo().setRedirectInfo(redirectInfo);
        }

        public Object getRedirectInfo() {
            return getMetaMappingInfo().getRedirectInfo();
        }

        public void setPlugin(Object plugin) {
            getMetaMappingInfo().setPlugin(plugin);
        }

        public Object getPlugin() {
            return getMetaMappingInfo().getPlugin();
        }

        public void setNamespace(Object namespace) {
            getMetaMappingInfo().setNamespace(namespace);
        }

        public Object getNamespace() {
            return getMetaMappingInfo().getNamespace();
        }

        public Object getView() {
            return getMetaMappingInfo().getView();
        }

        public void setView(String viewName) {
            getMetaMappingInfo().setView(viewName);
        }

        public void setMethod(Object method) {
            getMetaMappingInfo().setHttpMethod(method.toString());
        }

        public Object getMethod() {
            return getMetaMappingInfo().getHttpMethod();
        }

        public void name(Map<String, UrlMapping> m) {
            for (Map.Entry<String, UrlMapping> entry : m.entrySet()) {
                entry.getValue().setMappingName(entry.getKey());
            }
        }

        /**
         * Define a group
         *
         * @param uri      The URI
         * @param mappings The mappings
         */
        public void group(String uri, Closure mappings) {
            try {
                ParentResource parentResource = new ParentResource(null, uri, true, true);
                parentResources.push(parentResource);
                pushNewMetaMappingInfo();
                UrlGroupMappingRecursionBuilder builder = new UrlGroupMappingRecursionBuilder(this, parentResource);
                mappings.setDelegate(builder);
                mappings.setResolveStrategy(Closure.DELEGATE_FIRST);
                mappings.call();
                inGroupConstraints = false;
            } finally {
                mappingInfoDeque.pop();
                parentResources.pop();
            }
        }

        @Override
        public Object invokeMethod(String methodName, Object arg) {
            if (binding == null) {
                return invokeMethodClosure(methodName, arg);
            }
            return invokeMethodScript(methodName, arg);
        }

        private Object invokeMethodScript(String methodName, Object arg) {
            return _invoke(methodName, arg, null);
        }

        private Object invokeMethodClosure(String methodName, Object arg) {
            return _invoke(methodName, arg, this);
        }

        void propertyMissing(String name, Object value) {
            parameterValues.put(name, value);
        }

        Object propertyMissing(String name) {
            return parameterValues.get(name);
        }

        /**
         * Matches the GET method
         *
         * @param arguments The arguments
         * @param uri The URI
         * @param callable the customizer
         * @return the UrlMapping
         */
        public UrlMapping get(Map arguments, String uri, Closure callable) {
            arguments.put(UrlMapping.HTTP_METHOD, HttpMethod.GET.toString());
            return (UrlMapping) _invoke(uri, new Object[]{ arguments, callable }, this);
        }
        public UrlMapping get(Map arguments, String uri) {
            return get(arguments, uri, null);
        }
        public UrlMapping get(RegexUrlMapping regexUrlMapping) {
            regexUrlMapping.httpMethod = HttpMethod.GET.toString();
            return regexUrlMapping;
        }


        /**
         * Matches the POST method
         *
         * @param arguments The arguments
         * @param uri The URI
         * @return the UrlMapping
         */
        public UrlMapping post(Map arguments, String uri, Closure callable) {
            arguments.put(UrlMapping.HTTP_METHOD, HttpMethod.POST);
            return (UrlMapping) _invoke(uri, new Object[]{ arguments, callable }, this);
        }
        public UrlMapping post(Map arguments, String uri) {
            return post(arguments, uri, null);
        }
        public UrlMapping post(RegexUrlMapping regexUrlMapping) {
            regexUrlMapping.httpMethod = HttpMethod.POST.toString();
            return regexUrlMapping;
        }

        /**
         * Matches the PUT method
         *
         * @param arguments The arguments
         * @param uri The URI
         * @return the UrlMapping
         */
        public UrlMapping put(Map arguments, String uri, Closure callable) {
            arguments.put(UrlMapping.HTTP_METHOD, HttpMethod.PUT);
            return (UrlMapping) _invoke(uri, new Object[]{ arguments, callable }, this);
        }
        public UrlMapping put(Map arguments, String uri) {
            return put(arguments, uri, null);
        }
        public UrlMapping put(RegexUrlMapping regexUrlMapping) {
            regexUrlMapping.httpMethod = HttpMethod.PUT.toString();
            return regexUrlMapping;
        }

        /**
         * Matches the PATCH method
         *
         * @param arguments The arguments
         * @param uri The URI
         * @return the UrlMapping
         */
        public UrlMapping patch(Map arguments, String uri, Closure callable) {
            arguments.put(UrlMapping.HTTP_METHOD, HttpMethod.PATCH);
            return (UrlMapping) _invoke(uri, new Object[]{ arguments, callable }, this);
        }
        public UrlMapping patch(Map arguments, String uri) {
            return patch(arguments, uri, null);
        }
        public UrlMapping patch(RegexUrlMapping regexUrlMapping) {
            regexUrlMapping.httpMethod = HttpMethod.PATCH.toString();
            return regexUrlMapping;
        }

        /**
         * Matches the DELETE method
         *
         * @param arguments The arguments
         * @param uri The URI
         * @return the UrlMapping
         */
        public UrlMapping delete(Map arguments, String uri, Closure callable) {
            arguments.put(UrlMapping.HTTP_METHOD, HttpMethod.DELETE);
            return (UrlMapping) _invoke(uri, new Object[]{ arguments, callable }, this);
        }
        public UrlMapping delete(Map arguments, String uri) {
            return delete(arguments, uri, null);
        }
        public UrlMapping delete(RegexUrlMapping regexUrlMapping) {
            regexUrlMapping.httpMethod = HttpMethod.DELETE.toString();
            return regexUrlMapping;
        }
        /**
         * Matches the HEAD method
         *
         * @param arguments The arguments
         * @param uri The URI
         * @return the UrlMapping
         */
        public UrlMapping head(Map arguments, String uri, Closure callable) {
            arguments.put(UrlMapping.HTTP_METHOD, HttpMethod.HEAD);
            return (UrlMapping) _invoke(uri, new Object[]{ arguments, callable }, this);
        }
        public UrlMapping head(Map arguments, String uri) {
            return head(arguments, uri, null);
        }
        public UrlMapping head(RegexUrlMapping regexUrlMapping) {
            regexUrlMapping.httpMethod = HttpMethod.HEAD.toString();
            return regexUrlMapping;
        }

        /**
         * Matches the OPTIONS method
         *
         * @param arguments The arguments
         * @param uri The URI
         * @return the UrlMapping
         */
        public UrlMapping options(Map arguments, String uri, Closure callable) {
            arguments.put(UrlMapping.HTTP_METHOD, HttpMethod.OPTIONS);
            return (UrlMapping) _invoke(uri, new Object[]{ arguments, callable }, this);
        }
        public UrlMapping options(Map arguments, String uri) {
            return options(arguments, uri, null);
        }
        public UrlMapping options(RegexUrlMapping regexUrlMapping) {
            regexUrlMapping.httpMethod = HttpMethod.OPTIONS.toString();
            return regexUrlMapping;
        }
        /**
         * Define Url mapping collections that are nested directly below the parent resource (without the id)
         *
         * @param callable The callable
         */
        public void collection(Closure callable) {
            boolean previousState = isInCollection;
            this.isInCollection = true;
            try {
                callable.setDelegate(this);
                callable.call();
            } finally {
                isInCollection = previousState ;
            }
        }

        /**
         * Define Url mapping members that are nested directly below  the parent resource and resource id
         *
         * @param callable The callable
         */
        public void members(Closure callable) {
            boolean previousState = isInCollection;
            this.isInCollection = false;
            try {
                callable.setDelegate(this);
                callable.call();
            } finally {
                isInCollection = previousState ;
            }
        }

        private Object _invoke(String methodName, Object arg, Object delegate) {
            try {
                MetaMappingInfo mappingInfo = pushNewMetaMappingInfo();
                List<ConstrainedProperty> currentConstraints = mappingInfo.getConstraints();
                Object[] args = (Object[]) arg;
                String mappedURI = establishFullURI(methodName, currentConstraints);
                final boolean isResponseCode = isResponseCode(mappedURI);
                if (mappedURI.startsWith(SLASH) || isResponseCode) {
                    // Create a new parameter map for this mapping.
                    parameterValues = new HashMap<String, Object>();
                    Map variables = binding != null ? binding.getVariables() : null;
                    boolean hasParent = !parentResources.isEmpty();
                    try {
                        if (!hasParent) {
                            urlDefiningMode = false;
                        }
                        args = args != null && args.length > 0 ? args : new Object[]{Collections.emptyMap()};
                        if (args[0] instanceof Closure) {
                            UrlMappingData urlData = createUrlMappingData(mappedURI, isResponseCode);

                            Closure callable = (Closure) args[0];
                            if (delegate != null) {
                                callable.setDelegate(delegate);
                            }
                            callable.call();

                            if (binding != null) {
                                mappingInfo.setController(variables.get(GrailsControllerClass.CONTROLLER));
                                mappingInfo.setAction(variables.get(GrailsControllerClass.ACTION));
                                mappingInfo.setView(variables.get(GrailsControllerClass.VIEW));
                                mappingInfo.setUri(variables.get(UrlMapping.URI));
                                mappingInfo.setPlugin(variables.get(UrlMapping.PLUGIN));
                                mappingInfo.setNamespace(variables.get(UrlMapping.NAMESPACE));
                                if (variables.containsKey(UrlMapping.HTTP_METHOD)) {
                                    mappingInfo.setHttpMethod(variables.get(UrlMapping.HTTP_METHOD).toString());
                                }
                            }

                            ConstrainedProperty[] constraints = currentConstraints.toArray(new ConstrainedProperty[currentConstraints.size()]);
                            UrlMapping urlMapping;
                            if (mappingInfo.getUri() != null) {
                                try {
                                    urlMapping = new RegexUrlMapping(urlData, new URI(mappingInfo.getUri().toString()), constraints, grailsApplication);
                                } catch (URISyntaxException e) {
                                    throw new UrlMappingException("Cannot map to invalid URI: " + e.getMessage(), e);
                                }
                            } else {
                                urlMapping = createURLMapping(urlData, isResponseCode, mappingInfo.getRedirectInfo(), mappingInfo.getController(), mappingInfo.getAction(), mappingInfo.getNamespace(), mappingInfo.getPlugin(), mappingInfo.getView(), mappingInfo.getHttpMethod(), null, constraints);
                            }

                            if (binding != null) {
                                Map bindingVariables = variables;
                                Object parse = getParseRequest(Collections.emptyMap(), bindingVariables);
                                if (parse instanceof Boolean) {
                                    urlMapping.setParseRequest((Boolean) parse);
                                }
                            }
                            configureUrlMapping(urlMapping);
                            return urlMapping;
                        }

                        if (args[0] instanceof Map) {
                            Map namedArguments = (Map) args[0];
                            String uri = mappedURI;
                            String version = null;

                            if (namedArguments.containsKey(UrlMapping.VERSION)) {
                                version = namedArguments.get(UrlMapping.VERSION).toString();
                            }
                            if (namedArguments.containsKey(UrlMapping.NAMESPACE)) {
                                mappingInfo.setNamespace(namedArguments.get(UrlMapping.NAMESPACE).toString());
                            }

                            if (namedArguments.containsKey(UrlMapping.PLUGIN)) {
                                mappingInfo.setPlugin(namedArguments.get(UrlMapping.PLUGIN).toString());
                            }

                            UrlMappingData urlData = createUrlMappingData(uri, isResponseCode);

                            if (namedArguments.containsKey(RESOURCE) || namedArguments.containsKey(SINGLE)) {
                                Object controller;
                                if (namedArguments.containsKey(RESOURCE)) {
                                    GrailsUtil.deprecated("The " + RESOURCE + " syntax is deprecated and will be removed in a future release. Use " + SINGLE + " instead.");
                                    controller = namedArguments.get(RESOURCE);
                                } else {
                                    controller = namedArguments.get(SINGLE);
                                }
                                String controllerName = controller.toString();
                                mappingInfo.setController(controllerName);
                                parentResources.push(new ParentResource(controllerName, uri, true));
                                try {
                                    invokeLastArgumentIfClosure(args);
                                } finally {
                                    parentResources.pop();
                                }
                                if (controller != null) {

                                    createSingleResourceRestfulMappings(controllerName, mappingInfo.getPlugin(), mappingInfo.getNamespace(), version, urlData, currentConstraints, calculateIncludes(namedArguments, DEFAULT_RESOURCE_INCLUDES));
                                }
                            } else if (namedArguments.containsKey(RESOURCES)) {
                                Object controller = namedArguments.get(RESOURCES);
                                String controllerName = controller.toString();
                                mappingInfo.setController(controllerName);
                                parentResources.push(new ParentResource(controllerName, uri, false));
                                try {
                                    urlDefiningMode = true;
                                    invokeLastArgumentIfClosure(args);
                                } finally {
                                    parentResources.pop();
                                    hasParent = !parentResources.isEmpty();
                                    if (!hasParent) {
                                        urlDefiningMode = false;
                                    }
                                }
                                if (controller != null) {
                                    createResourceRestfulMappings(controllerName, mappingInfo.getPlugin(), mappingInfo.getNamespace(), version, urlData, currentConstraints, calculateIncludes(namedArguments, DEFAULT_RESOURCES_INCLUDES));
                                }
                            }
                            else {
                                invokeLastArgumentIfClosure(args);
                                UrlMapping urlMapping = getURLMappingForNamedArgs(namedArguments, urlData, mappedURI, isResponseCode, currentConstraints);
                                configureUrlMapping(urlMapping);
                                return urlMapping;
                            }
                        }
                        return null;
                    } finally {
                        if (binding != null) {
                            variables.clear();
                        }
                        if (!hasParent) {
                            urlDefiningMode = true;
                        }
                    }
                } else if ((!urlDefiningMode || (parentResources.size() > 0 && parentResources.peek().isGroup)) && CONSTRAINTS.equals(mappedURI)) {
                    if (args.length > 0 && (args[0] instanceof Closure)) {

                        Closure callable = (Closure) args[0];
                        ConstrainedPropertyBuilder builder = constraintsEvaluator.newConstrainedPropertyBuilder(UrlMapping.class);
                        for (ConstrainedProperty constrainedProperty : currentConstraints) {
                            builder.getConstrainedProperties().put(constrainedProperty.getPropertyName(), constrainedProperty);
                        }
                        callable.setResolveStrategy(Closure.DELEGATE_FIRST);
                        callable.setDelegate(builder);
                        callable.call();
                        return builder.getConstrainedProperties();

                    }
                    return Collections.emptyMap();
                } else {
                    LOG.error("Mapping: '"+ mappedURI + "' does not start with " + SLASH + " or is response code.");
                    return super.invokeMethod(mappedURI, arg);
                }
            } finally {
                mappingInfoDeque.pop();
            }
        }

        private List<String> calculateIncludes(Map namedArguments, List<String> defaultResourcesIncludes) {
            List<String> includes = new ArrayList<String>(defaultResourcesIncludes);

            Object excludesObject = namedArguments.get("excludes");
            if (excludesObject != null) {
                if (excludesObject instanceof List) {

                    List excludeList = (List) excludesObject;
                    for (Object exc : excludeList) {
                        if (exc != null) {
                            String excStr = exc.toString().toLowerCase();
                            includes.remove(excStr);
                        }
                    }
                } else {
                    includes.remove(excludesObject.toString());
                }
            }
            Object includesObject = namedArguments.get("includes");
            if (includesObject != null) {
                if (includesObject instanceof List) {

                    List includeList = (List) includesObject;
                    includes.clear();
                    for (Object inc : includeList) {
                        if (inc != null) {
                            String incStr = inc.toString().toLowerCase();
                            includes.add(incStr);
                        }
                    }
                } else {
                    includes.clear();
                    includes.add(includesObject.toString());
                }
            }
            return includes;
        }

        private String establishFullURI(String uri, List<ConstrainedProperty> constrainedList) {
            if (parentResources.isEmpty() || inGroupConstraints) {
                return uri;
            }

            ParentResource parentResource = parentResources.peek();
            if (CONSTRAINTS.equals(uri) && parentResource.isGroup) {
                return uri;
            }

            inGroupConstraints = false;

            StringBuilder uriBuilder = new StringBuilder();

            if (parentResource.isSingle) {
                uriBuilder.append(parentResource.uri);
            } else {
                if (parentResource.controllerName != null) {
                    uriBuilder.append(parentResource.uri);

                    if(!isInCollection) {
                        uriBuilder.append(SLASH).append(CAPTURING_WILD_CARD);
                    }
                }
            }

            if(!SLASH.equals(uri)) {
                uriBuilder.append(uri);
            }
            return uriBuilder.toString();
        }

        private void invokeLastArgumentIfClosure(Object[] args) {
            if (args.length > 1 && args[1] instanceof Closure) {
                ((Closure) args[1]).call();
            }
        }

        protected void createResourceRestfulMappings(String controllerName, Object pluginName, Object namespace, String version, UrlMappingData urlData, List<ConstrainedProperty> constrainedList, List<String> includes) {
            ConstrainedProperty[] constraintArray = constrainedList.toArray(new ConstrainedProperty[constrainedList.size()]);

            if (includes.contains(ACTION_INDEX)) {
                // GET /$controller -> action:'index'
                UrlMapping listUrlMapping = createIndexActionResourcesRestfulMapping(controllerName, pluginName, namespace, version, urlData, constrainedList);
                configureUrlMapping(listUrlMapping);
            }

            if (includes.contains(ACTION_CREATE)) {
                // GET /$controller/create -> action:'create'
                UrlMapping createUrlMapping = createCreateActionResourcesRestfulMapping(controllerName, pluginName, namespace, version, urlData, constraintArray);
                configureUrlMapping(createUrlMapping);
            }

            if (includes.contains(ACTION_SAVE)) {
                // POST /$controller -> action:'save'
                UrlMapping saveUrlMapping = createSaveActionResourcesRestfulMapping(controllerName, pluginName, namespace, version, urlData, constrainedList);
                configureUrlMapping(saveUrlMapping);
            }

            if (includes.contains(ACTION_SHOW)) {
                // GET /$controller/$id -> action:'show'
                UrlMapping showUrlMapping = createShowActionResourcesRestfulMapping(controllerName, pluginName, namespace, version, urlData, constrainedList);
                configureUrlMapping(showUrlMapping);
            }

            if (includes.contains(ACTION_EDIT)) {
                // GET /$controller/$id/edit -> action:'edit'
                UrlMapping editUrlMapping = createEditActionResourcesRestfulMapping(controllerName, pluginName, namespace, version, urlData, constrainedList);
                configureUrlMapping(editUrlMapping);
            }

            if (includes.contains(ACTION_UPDATE)) {
                // PUT /$controller/$id -> action:'update'
                UrlMapping updateUrlMapping = createUpdateActionResourcesRestfulMapping(controllerName, pluginName, namespace, version, urlData, constrainedList);
                configureUrlMapping(updateUrlMapping);
            }

            if (includes.contains(ACTION_PATCH)) {
                // PATCH /$controller/$id -> action:'patch'
                UrlMapping patchUrlMapping = createPatchActionResourcesRestfulMapping(controllerName, pluginName, namespace, version, urlData, constrainedList);
                configureUrlMapping(patchUrlMapping);
            }

            if (includes.contains(ACTION_DELETE)) {
                // DELETE /$controller/$id -> action:'delete'
                UrlMapping deleteUrlMapping = createDeleteActionResourcesRestfulMapping(controllerName, pluginName, namespace, version, urlData, constrainedList);
                configureUrlMapping(deleteUrlMapping);
            }
        }

        protected UrlMapping createDeleteActionResourcesRestfulMapping(String controllerName, Object pluginName, Object namespace, String version, UrlMappingData urlData, List<ConstrainedProperty> constrainedList) {
            UrlMappingData deleteUrlMappingData = createRelativeUrlDataWithIdAndFormat(urlData);
            List<ConstrainedProperty> deleteUrlMappingConstraints = createConstraintsWithIdAndFormat(constrainedList);

            return new RegexUrlMapping(deleteUrlMappingData, controllerName, ACTION_DELETE, namespace, pluginName, null, HttpMethod.DELETE.toString(), version, deleteUrlMappingConstraints.toArray(new ConstrainedProperty[deleteUrlMappingConstraints.size()]), grailsApplication);
        }

        protected UrlMapping createUpdateActionResourcesRestfulMapping(String controllerName, Object pluginName, Object namespace, String version, UrlMappingData urlData, List<ConstrainedProperty> constrainedList) {
            UrlMappingData updateUrlMappingData = createRelativeUrlDataWithIdAndFormat(urlData);
            List<ConstrainedProperty> updateUrlMappingConstraints = createConstraintsWithIdAndFormat(constrainedList);

            return new RegexUrlMapping(updateUrlMappingData, controllerName, ACTION_UPDATE, namespace, pluginName, null, HttpMethod.PUT.toString(), version, updateUrlMappingConstraints.toArray(new ConstrainedProperty[updateUrlMappingConstraints.size()]), grailsApplication);
        }

        protected UrlMapping createPatchActionResourcesRestfulMapping(String controllerName, Object pluginName, Object namespace, String version, UrlMappingData urlData, List<ConstrainedProperty> constrainedList) {
            UrlMappingData patchUrlMappingData = createRelativeUrlDataWithIdAndFormat(urlData);
            List<ConstrainedProperty> patchUrlMappingConstraints = createConstraintsWithIdAndFormat(constrainedList);

            return new RegexUrlMapping(patchUrlMappingData, controllerName, ACTION_PATCH, namespace, pluginName, null, HttpMethod.PATCH.toString(), version, patchUrlMappingConstraints.toArray(new ConstrainedProperty[patchUrlMappingConstraints.size()]), grailsApplication);
        }

        protected UrlMapping createEditActionResourcesRestfulMapping(String controllerName, Object pluginName, Object namespace, String version, UrlMappingData urlData, List<ConstrainedProperty> constrainedList) {
            UrlMappingData editUrlMappingData = urlData.createRelative('/' + CAPTURING_WILD_CARD + "/edit");
            List<ConstrainedProperty> editUrlMappingConstraints = new ArrayList<ConstrainedProperty>(constrainedList);
            editUrlMappingConstraints.add(new DefaultConstrainedProperty(UrlMapping.class, "id", String.class, constraintRegistry));

            return new RegexUrlMapping(editUrlMappingData, controllerName, ACTION_EDIT, namespace, pluginName, null, HttpMethod.GET.toString(), version, editUrlMappingConstraints.toArray(new ConstrainedProperty[editUrlMappingConstraints.size()]), grailsApplication);
        }

        protected UrlMapping createShowActionResourcesRestfulMapping(String controllerName, Object pluginName, Object namespace, String version, UrlMappingData urlData, List<ConstrainedProperty> constrainedList) {
            UrlMappingData showUrlMappingData = createRelativeUrlDataWithIdAndFormat(urlData);
            List<ConstrainedProperty> showUrlMappingConstraints = createConstraintsWithIdAndFormat(constrainedList);

            return new RegexUrlMapping(showUrlMappingData, controllerName, ACTION_SHOW, namespace, pluginName, null, HttpMethod.GET.toString(), version, showUrlMappingConstraints.toArray(new ConstrainedProperty[showUrlMappingConstraints.size()]), grailsApplication);
        }

        private List<ConstrainedProperty> createConstraintsWithIdAndFormat(List<ConstrainedProperty> constrainedList) {
            List<ConstrainedProperty> showUrlMappingConstraints = new ArrayList<ConstrainedProperty>(constrainedList);
            showUrlMappingConstraints.add(new DefaultConstrainedProperty(UrlMapping.class, "id", String.class, constraintRegistry));
            ConstrainedProperty cp = new DefaultConstrainedProperty(UrlMapping.class, "format", String.class, constraintRegistry);
            cp.applyConstraint(ConstrainedProperty.NULLABLE_CONSTRAINT, true);
            showUrlMappingConstraints.add(cp);
            return showUrlMappingConstraints;
        }

        private UrlMappingData createRelativeUrlDataWithIdAndFormat(UrlMappingData urlData) {
            return urlData.createRelative('/' + CAPTURING_WILD_CARD + UrlMapping.OPTIONAL_EXTENSION_WILDCARD + UrlMapping.QUESTION_MARK);
        }

        private UrlMappingData createFormatOnlyUrlMappingData(UrlMappingData urlData) {
            return urlData.createRelative(UrlMapping.OPTIONAL_EXTENSION_WILDCARD + UrlMapping.QUESTION_MARK);
        }


        protected UrlMapping createSaveActionResourcesRestfulMapping(String controllerName, Object pluginName, Object namespace, String version, UrlMappingData urlData, List<ConstrainedProperty> constrainedList) {
            UrlMappingData saveActionUrlMappingData = urlData.createRelative(UrlMapping.OPTIONAL_EXTENSION_WILDCARD + UrlMapping.QUESTION_MARK);
            List<ConstrainedProperty> saveUrlMappingConstraints = createFormatOnlyConstraints(constrainedList);

            return new RegexUrlMapping(saveActionUrlMappingData, controllerName, ACTION_SAVE, namespace, pluginName, null, HttpMethod.POST.toString(), version, saveUrlMappingConstraints.toArray(new ConstrainedProperty[saveUrlMappingConstraints.size()]), grailsApplication);
        }

        protected UrlMapping createCreateActionResourcesRestfulMapping(String controllerName, Object pluginName, Object namespace, String version, UrlMappingData urlData, ConstrainedProperty[] constraintArray) {
            UrlMappingData createMappingData = urlData.createRelative("/create");
            return new RegexUrlMapping(createMappingData, controllerName, ACTION_CREATE, namespace, pluginName, null, HttpMethod.GET.toString(), version, constraintArray, grailsApplication);
        }

        protected UrlMapping createIndexActionResourcesRestfulMapping(String controllerName, Object pluginName, Object namespace, String version, UrlMappingData urlData, List<ConstrainedProperty> constrainedList) {
            UrlMappingData indexActionUrlMappingData = urlData.createRelative(UrlMapping.OPTIONAL_EXTENSION_WILDCARD + UrlMapping.QUESTION_MARK);
            List<ConstrainedProperty> indexUrlMappingConstraints = createFormatOnlyConstraints(constrainedList);

            return new RegexUrlMapping(indexActionUrlMappingData, controllerName, ACTION_INDEX, namespace, pluginName, null, HttpMethod.GET.toString(), version, indexUrlMappingConstraints.toArray(new ConstrainedProperty[indexUrlMappingConstraints.size()]), grailsApplication);
        }

        private List<ConstrainedProperty> createFormatOnlyConstraints(List<ConstrainedProperty> constrainedList) {
            List<ConstrainedProperty> indexUrlMappingConstraints = new ArrayList<ConstrainedProperty>(constrainedList);
            ConstrainedProperty cp = new DefaultConstrainedProperty(UrlMapping.class, "format", String.class, constraintRegistry);
            cp.applyConstraint(ConstrainedProperty.NULLABLE_CONSTRAINT, true);
            indexUrlMappingConstraints.add(cp);
            return indexUrlMappingConstraints;
        }

        /**
         * Takes a controller and creates the necessary URL mappings for a singular RESTful resource
         *
         * @param controllerName The controller name
         * @param pluginName     The name of the plugin
         * @param namespace
         * @param version
         * @param urlData        The urlData instance
         * @param includes
         */
        protected void createSingleResourceRestfulMappings(String controllerName, Object pluginName, Object namespace, String version, UrlMappingData urlData, List<ConstrainedProperty> constrainedList, List<String> includes) {

            ConstrainedProperty[] constraintArray = constrainedList.toArray(new ConstrainedProperty[constrainedList.size()]);

            if (includes.contains(ACTION_CREATE)) {
                // GET /$controller/create -> action: 'create'
                UrlMapping createUrlMapping = createCreateActionResourcesRestfulMapping(controllerName, pluginName, namespace, version, urlData, constraintArray);
                configureUrlMapping(createUrlMapping);
            }

            if (includes.contains(ACTION_SAVE)) {
                // POST /$controller -> action:'save'
                UrlMapping saveUrlMapping = createSaveActionResourcesRestfulMapping(controllerName, pluginName, namespace, version, urlData, constrainedList);
                configureUrlMapping(saveUrlMapping);
            }

            if (includes.contains(ACTION_SHOW)) {
                // GET /$controller -> action:'show'
                UrlMapping showUrlMapping = createShowActionResourceRestfulMapping(controllerName, pluginName, namespace, version, urlData, constrainedList);
                configureUrlMapping(showUrlMapping);
            }

            if (includes.contains(ACTION_EDIT)) {
                // GET /$controller/edit -> action:'edit'
                UrlMapping editUrlMapping = createEditActionResourceRestfulMapping(controllerName, pluginName, namespace, version, urlData, constraintArray);
                configureUrlMapping(editUrlMapping);
            }

            if (includes.contains(ACTION_UPDATE)) {
                // PUT /$controller -> action:'update'
                UrlMapping updateUrlMapping = createUpdateActionResourceRestfulMapping(controllerName, pluginName, namespace, version, urlData, constrainedList);
                configureUrlMapping(updateUrlMapping);
            }

            if (includes.contains(ACTION_PATCH)) {
                // PATCH /$controller -> action:'patch'
                UrlMapping patchUrlMapping = createPatchActionResourceRestfulMapping(controllerName, pluginName, namespace, version, urlData, constrainedList);
                configureUrlMapping(patchUrlMapping);
            }

            if (includes.contains(ACTION_DELETE)) {
                // DELETE /$controller -> action:'delete'
                UrlMapping deleteUrlMapping = createDeleteActionResourceRestfulMapping(controllerName, pluginName, namespace, version, urlData, constrainedList);
                configureUrlMapping(deleteUrlMapping);
            }
        }

        protected UrlMapping createDeleteActionResourceRestfulMapping(String controllerName, Object pluginName, Object namespace, String version, UrlMappingData urlData, List<ConstrainedProperty> constrainedList) {
            UrlMappingData deleteUrlMappingData = createFormatOnlyUrlMappingData(urlData);
            List<ConstrainedProperty> deleteUrlMappingConstraints = createFormatOnlyConstraints(constrainedList);

            return new RegexUrlMapping(deleteUrlMappingData, controllerName, ACTION_DELETE, namespace, pluginName, null, HttpMethod.DELETE.toString(), version, deleteUrlMappingConstraints.toArray(new ConstrainedProperty[deleteUrlMappingConstraints.size()]), grailsApplication);
        }

        protected UrlMapping createUpdateActionResourceRestfulMapping(String controllerName, Object pluginName, Object namespace, String version, UrlMappingData urlData, List<ConstrainedProperty> constrainedList) {
            UrlMappingData updateUrlMappingData = createFormatOnlyUrlMappingData(urlData);
            List<ConstrainedProperty> updateUrlMappingConstraints = createFormatOnlyConstraints(constrainedList);

            return new RegexUrlMapping(updateUrlMappingData, controllerName, ACTION_UPDATE, namespace, pluginName, null, HttpMethod.PUT.toString(), version, updateUrlMappingConstraints.toArray(new ConstrainedProperty[updateUrlMappingConstraints.size()]), grailsApplication);
        }

        protected UrlMapping createPatchActionResourceRestfulMapping(String controllerName, Object pluginName, Object namespace, String version, UrlMappingData urlData, List<ConstrainedProperty> constrainedList) {
            UrlMappingData patchUrlMappingData = createFormatOnlyUrlMappingData(urlData);
            List<ConstrainedProperty> patchUrlMappingConstraints = createFormatOnlyConstraints(constrainedList);

            return new RegexUrlMapping(patchUrlMappingData, controllerName, ACTION_PATCH, namespace, pluginName, null, HttpMethod.PATCH.toString(), version, patchUrlMappingConstraints.toArray(new ConstrainedProperty[patchUrlMappingConstraints.size()]), grailsApplication);
        }

        protected UrlMapping createEditActionResourceRestfulMapping(String controllerName, Object pluginName, Object namespace, String version, UrlMappingData urlData, ConstrainedProperty[] constraintArray) {
            UrlMappingData editMappingData = urlData.createRelative("/edit");
            return new RegexUrlMapping(editMappingData, controllerName, ACTION_EDIT, namespace, pluginName, null, HttpMethod.GET.toString(), version, constraintArray, grailsApplication);
        }

        protected UrlMapping createShowActionResourceRestfulMapping(String controllerName, Object pluginName, Object namespace, String version, UrlMappingData urlData, List<ConstrainedProperty> constrainedList) {
            UrlMappingData showUrlMappingData = createFormatOnlyUrlMappingData(urlData);
            List<ConstrainedProperty> showUrlMappingConstraints = createFormatOnlyConstraints(constrainedList);

            return new RegexUrlMapping(showUrlMappingData, controllerName, ACTION_SHOW, namespace, pluginName, null, HttpMethod.GET.toString(), version, showUrlMappingConstraints.toArray(new ConstrainedProperty[showUrlMappingConstraints.size()]), grailsApplication);
        }

        @SuppressWarnings("unchecked")
        private void configureUrlMapping(UrlMapping urlMapping) {
            if (binding != null) {
                Map<String, Object> vars = binding.getVariables();
                for (String key : vars.keySet()) {
                    if (isNotCoreMappingKey(key)) {
                        parameterValues.put(key, vars.get(key));
                    }
                }

                binding.getVariables().clear();
            }

            // Add the controller and action to the params map if
            // they are set. This ensures consistency of behaviour
            // for the application, i.e. "controller" and "action"
            // parameters will always be available to it.
            if (urlMapping.getControllerName() != null) {
                parameterValues.put("controller", urlMapping.getControllerName());
            }
            if (urlMapping.getActionName() != null) {
                parameterValues.put("action", urlMapping.getActionName());
            }

            urlMapping.setParameterValues(new LinkedHashMap(parameterValues));
            urlMappings.add(urlMapping);
        }

        private boolean isNotCoreMappingKey(Object key) {
            return !GrailsControllerClass.ACTION.equals(key) &&
                    !GrailsControllerClass.CONTROLLER.equals(key) &&
                    !GrailsControllerClass.VIEW.equals(key);
        }

        private UrlMappingData createUrlMappingData(String methodName, boolean responseCode) {
            if (!responseCode) {
                return urlParser.parse(methodName);
            }

            return new ResponseCodeMappingData(methodName);
        }

        private boolean isResponseCode(String s) {
            for (int count = s.length(), i = 0; i < count; i++) {
                if (!Character.isDigit(s.charAt(i))) return false;
            }

            return true;
        }

        private UrlMapping getURLMappingForNamedArgs(Map namedArguments,
                                                     UrlMappingData urlData, String mapping, boolean isResponseCode, List<ConstrainedProperty> constrainedList) {
            final Map bindingVariables = binding != null ? binding.getVariables() : null;
            Object controllerName = getControllerName(namedArguments, bindingVariables);
            Object actionName = getActionName(namedArguments, bindingVariables);
            Object pluginName = getPluginName(namedArguments, bindingVariables);
            Object httpMethod = getHttpMethod(namedArguments, bindingVariables);
            Object version = getVersion(namedArguments, bindingVariables);
            Object namespace = getNamespace(namedArguments, bindingVariables);
            Object redirectInfo = getRedirectInfo(namedArguments, bindingVariables);

            Object viewName = getViewName(namedArguments, bindingVariables);
            if (actionName != null && viewName != null) {
                viewName = null;
                LOG.warn("Both [action] and [view] specified in URL mapping [" + mapping + "]. The action takes precendence!");
            }

            Object uri = getURI(namedArguments, bindingVariables);
            ConstrainedProperty[] constraints = constrainedList.toArray(new ConstrainedProperty[constrainedList.size()]);

            UrlMapping urlMapping;
            if (uri != null) {
                try {
                    urlMapping = isResponseCode ?
                        new ResponseCodeUrlMapping(urlData, new URI(uri.toString()), constraints, grailsApplication) :
                        new RegexUrlMapping(urlData, new URI(uri.toString()), constraints, grailsApplication);
                } catch (URISyntaxException e) {
                    throw new UrlMappingException("Cannot map to invalid URI: " + e.getMessage(), e);
                }
            } else {
                urlMapping = createURLMapping(urlData, isResponseCode, redirectInfo, controllerName, actionName, namespace, pluginName, viewName, httpMethod != null ? httpMethod.toString() : null, version != null ? version.toString() : null, constraints);
            }

            Object exceptionArg = getException(namedArguments, bindingVariables);

            if (isResponseCode && exceptionArg != null) {
                if (exceptionArg instanceof Class) {
                    Class exClass = (Class) exceptionArg;
                    if (Throwable.class.isAssignableFrom(exClass)) {
                        ((ResponseCodeUrlMapping) urlMapping).setExceptionType(exClass);
                    } else {
                        LOG.error("URL mapping argument [exception] with value [" + exceptionArg + "] must be a subclass of java.lang.Throwable");
                    }
                } else {
                    LOG.error("URL mapping argument [exception] with value [" + exceptionArg + "] must be a valid class");
                }
            }

            Object parseRequest = getParseRequest(namedArguments, bindingVariables);
            if (parseRequest instanceof Boolean) {
                urlMapping.setParseRequest((Boolean) parseRequest);
            }


            return urlMapping;
        }

        private Object getVariableFromNamedArgsOrBinding(Map namedArguments, Map bindingVariables, String variableName, Object defaultValue) {

            Object returnValue;
            returnValue = namedArguments.get(variableName);
            if (returnValue == null) {
                returnValue = binding != null ? bindingVariables.get(variableName) : defaultValue;
            }
            return returnValue;
        }

        private Object getActionName(Map namedArguments, Map bindingVariables) {
            return getVariableFromNamedArgsOrBinding(namedArguments, bindingVariables, GrailsControllerClass.ACTION, getMetaMappingInfo().getAction());
        }

        private Object getParseRequest(Map namedArguments, Map bindingVariables) {
            return getVariableFromNamedArgsOrBinding(namedArguments, bindingVariables, PARSE_REQUEST, parseRequest);
        }

        private Object getControllerName(Map namedArguments, Map bindingVariables) {
            Object fromBinding = getVariableFromNamedArgsOrBinding(namedArguments, bindingVariables, GrailsControllerClass.CONTROLLER, getMetaMappingInfo().getController());
            if(fromBinding == null && !parentResources.isEmpty()) {
                return parentResources.peekLast().controllerName;
            }
            else {
                return fromBinding;
            }
        }

        private Object getPluginName(Map namedArguments, Map bindingVariables) {
            return getVariableFromNamedArgsOrBinding(namedArguments, bindingVariables, UrlMapping.PLUGIN, getMetaMappingInfo().getPlugin());
        }

        private Object getHttpMethod(Map namedArguments, Map bindingVariables) {
            return getVariableFromNamedArgsOrBinding(namedArguments, bindingVariables, UrlMapping.HTTP_METHOD, getMetaMappingInfo().getHttpMethod());
        }

        private Object getRedirectInfo(Map namedArguments, Map bindingVariables) {
            return getVariableFromNamedArgsOrBinding(namedArguments, bindingVariables, UrlMapping.REDIRECT_INFO, getMetaMappingInfo().getRedirectInfo());
        }

        private Object getVersion(Map namedArguments, Map bindingVariables) {
            return getVariableFromNamedArgsOrBinding(namedArguments, bindingVariables, UrlMapping.VERSION, getMetaMappingInfo().getView());
        }

        private Object getNamespace(Map namedArguments, Map bindingVariables) {
            return getVariableFromNamedArgsOrBinding(namedArguments, bindingVariables, UrlMapping.NAMESPACE, getMetaMappingInfo().getNamespace());
        }

        private Object getViewName(Map namedArguments, Map bindingVariables) {
            return getVariableFromNamedArgsOrBinding(namedArguments, bindingVariables, GrailsControllerClass.VIEW, getMetaMappingInfo().getView());
        }

        private Object getURI(Map namedArguments, Map bindingVariables) {
            return getVariableFromNamedArgsOrBinding(namedArguments, bindingVariables, UrlMapping.URI, getMetaMappingInfo().getUri());
        }

        private Object getException(Map namedArguments, Map bindingVariables) {
            return getVariableFromNamedArgsOrBinding(namedArguments, bindingVariables, EXCEPTION, exception);
        }

        private UrlMapping createURLMapping(UrlMappingData urlData, boolean isResponseCode, Object redirectInfo,
                                            Object controllerName, Object actionName, Object namespace, Object pluginName,
                                            Object viewName, String httpMethod, String version, ConstrainedProperty[] constraints) {
            if (!isResponseCode) {
                return new RegexUrlMapping(redirectInfo, urlData, controllerName, actionName, namespace, pluginName, viewName, httpMethod, version, constraints, grailsApplication);
            }

            return new ResponseCodeUrlMapping(urlData, controllerName, actionName, namespace, pluginName, viewName,
                    null, grailsApplication);
        }

        protected MetaMappingInfo pushNewMetaMappingInfo() {
            MetaMappingInfo mappingInfo = new MetaMappingInfo();
            MetaMappingInfo parentMappingInfo = mappingInfoDeque.peek();
            if (parentMappingInfo != null) {
                List<ConstrainedProperty> parentMappingConstraints = parentMappingInfo.getConstraints();
                if (parentMappingConstraints != null) {
                    mappingInfo.getConstraints().addAll(parentMappingConstraints);
                }
                ParentResource parentResource = parentResources.peek();
                if(parentResource != null && !parentResource.isSingle) {
                    if(!isInCollection) {
                        mappingInfo.getConstraints().add(new DefaultConstrainedProperty(UrlMapping.class, parentResource.controllerName + "Id", String.class, constraintRegistry));
                    }
                }
            }
            if (previousConstraints.size() > 0) {
                mappingInfo.getConstraints().addAll(previousConstraints);
                previousConstraints.clear();
            }

            mappingInfoDeque.push(mappingInfo);
            return mappingInfo;
        }

        protected MetaMappingInfo getMetaMappingInfo() {
            return mappingInfoDeque.peek();
        }

        class ParentResource {
            String controllerName;
            String uri;
            boolean isSingle;
            boolean isGroup;

            ParentResource(String controllerName, String uri, boolean single) {
                this.controllerName = controllerName;
                this.uri = uri;
                isSingle = single;
                isGroup = false;
            }

            ParentResource(String controllerName, String uri, boolean single, boolean group) {
                this.controllerName = controllerName;
                this.uri = uri;
                isSingle = single;
                isGroup = group;
            }
        }
    }

    class UrlGroupMappingRecursionBuilder extends UrlMappingBuilder {
        private ParentResource parentResource;
        public UrlGroupMappingRecursionBuilder(UrlMappingBuilder parent, ParentResource parentResource) {
            super(parent);

            this.parentResource = parentResource;
        }

        @Override
        public void group(String uri, Closure mappings) {
            if (parentResource != null) {
                uri = parentResource.uri.concat(uri);
            }

            super.group(uri, mappings);
        }
    }
}
