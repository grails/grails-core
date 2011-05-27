/* Copyright 2004-2005 Graeme Rocher
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
package org.codehaus.groovy.grails.web.mapping;

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import groovy.lang.GroovyObjectSupport;
import groovy.lang.Script;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsControllerClass;
import org.codehaus.groovy.grails.commons.GrailsMetaClassUtils;
import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.codehaus.groovy.grails.plugins.PluginManagerAware;
import org.codehaus.groovy.grails.plugins.support.aware.ClassLoaderAware;
import org.codehaus.groovy.grails.validation.ConstrainedProperty;
import org.codehaus.groovy.grails.validation.ConstrainedPropertyBuilder;
import org.codehaus.groovy.grails.web.mapping.exceptions.UrlMappingException;
import org.codehaus.groovy.grails.web.plugins.support.WebMetaUtils;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.springframework.beans.BeanUtils;
import org.springframework.core.io.Resource;

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

    private static final Log LOG = LogFactory.getLog(UrlMappingBuilder.class);

    private GroovyClassLoader classLoader = new GroovyClassLoader();
    private UrlMappingParser urlParser = new DefaultUrlMappingParser();
    private ServletContext servletContext;
    private static final String EXCEPTION = "exception";
    private static final String PARSE_REQUEST = "parseRequest";
    private static final String RESOURCE = "resource";
    private GrailsPluginManager pluginManager;

    public DefaultUrlMappingEvaluator(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @SuppressWarnings("rawtypes")
    public List evaluateMappings(Resource resource) {
        InputStream inputStream = null;
        try {
            inputStream = resource.getInputStream();
            return evaluateMappings(classLoader.parseClass(DefaultGroovyMethods.getText(inputStream)));
        }
        catch (IOException e) {
            throw new UrlMappingException("Unable to read mapping file [" + resource.getFilename() + "]: " + e.getMessage(), e);
        }
        finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    @SuppressWarnings({"unchecked","rawtypes"})
    public List evaluateMappings(Class theClass) {
        GroovyObject obj = (GroovyObject) BeanUtils.instantiateClass(theClass);

        if (obj instanceof Script) {
            Script script = (Script) obj;
            Binding b = new Binding();

            MappingCapturingClosure closure = new MappingCapturingClosure(script);
            b.setVariable("mappings", closure);
            script.setBinding(b);

            script.run();

            Closure mappings = closure.getMappings();

            UrlMappingBuilder builder = new UrlMappingBuilder(script.getBinding(), servletContext);
            mappings.setDelegate(builder);
            mappings.call();
            builder.urlDefiningMode = false;

            configureUrlMappingDynamicObjects(script);

            return builder.getUrlMappings();
        }

        throw new UrlMappingException("Unable to configure URL mappings for class [" + theClass +
                "]. A URL mapping must be an instance of groovy.lang.Script.");
    }

    @SuppressWarnings("rawtypes")
    public List evaluateMappings(Closure closure) {
        UrlMappingBuilder builder = new UrlMappingBuilder(null, servletContext);
        closure.setDelegate(builder);
        closure.setResolveStrategy(Closure.DELEGATE_FIRST);
        closure.call();
        builder.urlDefiningMode = false;
        List mappings = builder.getUrlMappings();
        configureUrlMappingDynamicObjects(closure);
        return mappings;
    }

    private void configureUrlMappingDynamicObjects(Script script) {
        if (pluginManager != null) {
            WebMetaUtils.registerCommonWebProperties(GrailsMetaClassUtils.getExpandoMetaClass(script.getClass()), null);
        }
    }

    private void configureUrlMappingDynamicObjects(Object object) {
        if (pluginManager != null) {
            WebMetaUtils.registerCommonWebProperties(GrailsMetaClassUtils.getExpandoMetaClass(object.getClass()), null);
        }
    }

    public void setClassLoader(ClassLoader classLoader) {
        if (classLoader instanceof GroovyClassLoader) {
            this.classLoader = (GroovyClassLoader) classLoader;
        }
        else {
            throw new IllegalArgumentException("Property [classLoader] must be an instance of GroovyClassLoader");
        }
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
        private static final String CAPTURING_WILD_CARD = "(*)";
        private static final String SLASH = "/";
        private static final String CONSTRAINTS = "constraints";

        private boolean urlDefiningMode = true;
        private List<ConstrainedProperty> previousConstraints = new ArrayList<ConstrainedProperty>();
        private List<UrlMapping> urlMappings = new ArrayList<UrlMapping>();
        private Map<String, Object> parameterValues = new HashMap<String, Object>();
        private Binding binding;
        private Object actionName = null;
        private Object controllerName = null;
        private Object viewName = null;
        private ServletContext sc;
        private Object exception;
        private Object parseRequest;
        private Object uri;

        public UrlMappingBuilder(Binding binding, ServletContext servletContext) {
            this.binding = binding;
            sc = servletContext;
        }

        public List<UrlMapping> getUrlMappings() {
            return urlMappings;
        }

        @Override
        public Object getProperty(String name) {
            if (urlDefiningMode) {
                previousConstraints.add(new ConstrainedProperty(UrlMapping.class, name, String.class));
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
            return uri;
        }

        public void setUri(Object uri) {
            this.uri = uri;
        }

        public void setAction(Object action) {
            actionName = action;
        }

        public Object getAction() {
            return actionName;
        }

        public void setController(Object controller) {
            controllerName = controller;
        }

        public Object getController() {
            return controllerName;
        }

        public Object getView() {
            return viewName;
        }

        public void setView(String viewName) {
            this.viewName = viewName;
        }

        public void name(Map<String, UrlMapping> m) {
            for (Map.Entry<String, UrlMapping> entry: m.entrySet()) {
                entry.getValue().setMappingName(entry.getKey());
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

        private Object _invoke(String methodName, Object arg, Object delegate) {
            Object[] args = (Object[]) arg;
            final boolean isResponseCode = isResponseCode(methodName);
            if (methodName.startsWith(SLASH) || isResponseCode) {
                // Create a new parameter map for this mapping.
                parameterValues = new HashMap<String, Object>();
                try {
                    urlDefiningMode = false;
                    args = args != null && args.length > 0 ? args : new Object[]{Collections.EMPTY_MAP};
                    if (args[0] instanceof Closure) {
                        UrlMappingData urlData = createUrlMappingData(methodName, isResponseCode);

                        Closure callable = (Closure) args[0];
                        if (delegate != null) callable.setDelegate(delegate);
                        callable.call();

                        @SuppressWarnings("hiding")
                        Object controllerName;
                        @SuppressWarnings("hiding")
                        Object actionName;
                        @SuppressWarnings("hiding")
                        Object viewName;
                        @SuppressWarnings("hiding")
                        Object uri;

                        if (binding != null) {
                            controllerName = binding.getVariables().get(GrailsControllerClass.CONTROLLER);
                            actionName = binding.getVariables().get(GrailsControllerClass.ACTION);
                            viewName = binding.getVariables().get(GrailsControllerClass.VIEW);
                            uri = binding.getVariables().get("uri");
                        }
                        else {
                            controllerName = this.controllerName;
                            actionName = this.actionName;
                            viewName = this.viewName;
                            uri = this.uri;
                        }

                        ConstrainedProperty[] constraints = previousConstraints.toArray(new ConstrainedProperty[previousConstraints.size()]);
                        UrlMapping urlMapping;
                        if (uri != null) {
                            try {
                                urlMapping = new RegexUrlMapping(urlData, new URI(uri.toString()), constraints, sc);
                            }
                            catch (URISyntaxException e) {
                                throw new UrlMappingException("Cannot map to invalid URI: " + e.getMessage(), e);
                            }
                        }
                        else {
                            urlMapping = createURLMapping(urlData, isResponseCode, controllerName, actionName, viewName, constraints);
                        }

                        configureUrlMapping(urlMapping);
                        return urlMapping;
                    }

                    if (args[0] instanceof Map) {
                        Map namedArguments = (Map) args[0];
                        UrlMappingData urlData = createUrlMappingData(methodName, isResponseCode);
                        if (args.length > 1 && args[1] instanceof Closure) {
                            Closure callable = (Closure) args[1];
                            callable.call();
                        }

                        UrlMapping urlMapping = getURLMappingForNamedArgs(namedArguments, urlData, methodName, isResponseCode);
                        configureUrlMapping(urlMapping);
                        return urlMapping;
                    }
                    return null;
                }
                finally {
                    if (binding != null) {
                        binding.getVariables().clear();
                    }
                    else {
                        controllerName = null;
                        actionName = null;
                        viewName = null;
                    }
                    previousConstraints.clear();
                    urlDefiningMode = true;
                }
            }
            else if (!urlDefiningMode && CONSTRAINTS.equals(methodName)) {
                ConstrainedPropertyBuilder builder = new ConstrainedPropertyBuilder(this);
                if (args.length > 0 && (args[0] instanceof Closure)) {

                    Closure callable = (Closure) args[0];
                    callable.setDelegate(builder);
                    for (ConstrainedProperty constrainedProperty : previousConstraints) {
                        builder.getConstrainedProperties().put(constrainedProperty.getPropertyName(), constrainedProperty);
                    }
                    callable.call();
                }
                return builder.getConstrainedProperties();
            }
            else {
                return super.invokeMethod(methodName, arg);
            }
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

            urlMapping.setParameterValues(parameterValues);
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
            for (int i = 0; i < s.length(); i++) {
                if (!Character.isDigit(s.charAt(i))) return false;
            }

            return true;
        }

        private UrlMapping getURLMappingForNamedArgs(Map namedArguments,
                UrlMappingData urlData, String mapping, boolean isResponseCode) {
            @SuppressWarnings("hiding")
            Object controllerName;
            @SuppressWarnings("hiding")
            Object actionName;
            final Map bindingVariables = binding != null ? binding.getVariables() : null;
            boolean restRequest = false;
            if (namedArguments.containsKey(RESOURCE)) {
                controllerName = namedArguments.get(RESOURCE);
                actionName = DEFAULT_REST_MAPPING;
                restRequest = true;
            }
            else {
                controllerName = getControllerName(namedArguments, bindingVariables);
                actionName = getActionName(namedArguments, bindingVariables);
            }

            @SuppressWarnings("hiding")
            Object viewName = getViewName(namedArguments, bindingVariables);
            if (actionName != null && viewName != null) {
                viewName = null;
                LOG.warn("Both [action] and [view] specified in URL mapping [" + mapping + "]. The action takes precendence!");
            }

            @SuppressWarnings("hiding")
            Object uri = getURI(namedArguments, bindingVariables);
            ConstrainedProperty[] constraints = previousConstraints.toArray(new ConstrainedProperty[previousConstraints.size()]);

            UrlMapping urlMapping;
            if (uri != null) {
                try {
                    urlMapping = new RegexUrlMapping(urlData, new URI(uri.toString()), constraints, sc);
                }
                catch (URISyntaxException e) {
                    throw new UrlMappingException("Cannot map to invalid URI: " + e.getMessage(), e);
                }
            }
            else {
                urlMapping = createURLMapping(urlData, isResponseCode, controllerName, actionName, viewName, constraints);
            }

            Object exceptionArg = getException(namedArguments, bindingVariables);

            if (isResponseCode && exceptionArg != null) {
                if (exceptionArg instanceof Class) {
                    Class exClass = (Class) exceptionArg;
                    if (Throwable.class.isAssignableFrom(exClass)) {
                        ((ResponseCodeUrlMapping)urlMapping).setExceptionType(exClass);
                    }
                    else {
                        LOG.error("URL mapping argument [exception] with value ["+ exceptionArg +"] must be a subclass of java.lang.Throwable");
                    }
                }
                else {
                    LOG.error("URL mapping argument [exception] with value ["+ exceptionArg +"] must be a valid class");
                }
            }

            if (restRequest) {
                urlMapping.setParseRequest(true);
                urlMapping.setRestfulMapping(true);
            }
            else {
                @SuppressWarnings("hiding")
                Object parseRequest = getParseRequest(namedArguments,bindingVariables);
                if (parseRequest instanceof Boolean) {
                    urlMapping.setParseRequest((Boolean) parseRequest);
                }
            }

            if (actionName instanceof Map) {
                urlMapping.setRestfulMapping(true);
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
            return getVariableFromNamedArgsOrBinding(namedArguments, bindingVariables,GrailsControllerClass.ACTION, actionName);
        }

        private Object getParseRequest(Map namedArguments, Map bindingVariables) {
            return getVariableFromNamedArgsOrBinding(namedArguments, bindingVariables,PARSE_REQUEST, parseRequest);
        }

        private Object getControllerName(Map namedArguments, Map bindingVariables) {
            return getVariableFromNamedArgsOrBinding(namedArguments, bindingVariables,GrailsControllerClass.CONTROLLER, controllerName);
        }

        private Object getViewName(Map namedArguments, Map bindingVariables) {
            return getVariableFromNamedArgsOrBinding(namedArguments, bindingVariables,GrailsControllerClass.VIEW, viewName);
        }

        private Object getURI(Map namedArguments, Map bindingVariables) {
            return getVariableFromNamedArgsOrBinding(namedArguments, bindingVariables,"uri", uri);
        }

        private Object getException(Map namedArguments, Map bindingVariables) {
            return getVariableFromNamedArgsOrBinding(namedArguments, bindingVariables,EXCEPTION, exception);
        }

        private UrlMapping createURLMapping(UrlMappingData urlData, boolean isResponseCode,
                @SuppressWarnings("hiding") Object controllerName,
                @SuppressWarnings("hiding") Object actionName,
                @SuppressWarnings("hiding") Object viewName, ConstrainedProperty[] constraints) {
            if (!isResponseCode) {
                return new RegexUrlMapping(urlData, controllerName, actionName, viewName,
                        constraints, sc);
            }

            return new ResponseCodeUrlMapping(urlData, controllerName, actionName, viewName,
                    null, sc);
        }
    }
}
