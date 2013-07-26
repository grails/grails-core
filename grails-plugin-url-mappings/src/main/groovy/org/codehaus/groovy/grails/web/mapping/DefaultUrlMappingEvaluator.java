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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsControllerClass;
import org.codehaus.groovy.grails.commons.GrailsMetaClassUtils;
import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.codehaus.groovy.grails.plugins.PluginManagerAware;
import org.codehaus.groovy.grails.plugins.support.aware.ClassLoaderAware;
import org.codehaus.groovy.grails.validation.ConstrainedProperty;
import org.codehaus.groovy.grails.validation.ConstrainedPropertyBuilder;
import org.codehaus.groovy.grails.web.mapping.exceptions.UrlMappingException;
import org.codehaus.groovy.grails.web.plugins.support.WebMetaUtils;
import org.codehaus.groovy.runtime.IOGroovyMethods;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

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
    public static final String ACTION_DELETE = "delete";
    public static final String ACTION_SAVE = "save";
    public static final List<String> DEFAULT_RESOURCES_INCLUDES = Arrays.asList(ACTION_INDEX, ACTION_CREATE, ACTION_SAVE,ACTION_SHOW,ACTION_EDIT, ACTION_UPDATE, ACTION_DELETE);
    public static final List<String> DEFAULT_RESOURCE_INCLUDES = Arrays.asList(ACTION_CREATE,ACTION_SAVE,ACTION_SHOW, ACTION_EDIT, ACTION_UPDATE, ACTION_DELETE);
    private static final Log LOG = LogFactory.getLog(UrlMappingBuilder.class);
    private GroovyClassLoader classLoader = new GroovyClassLoader();
    private UrlMappingParser urlParser = new DefaultUrlMappingParser();
    private ServletContext servletContext;
    private static final String EXCEPTION = "exception";
    private static final String PARSE_REQUEST = "parseRequest";
    private static final String RESOURCE = "resource";
    private static final String RESOURCES = "resources";

    private GrailsPluginManager pluginManager;
    private WebApplicationContext applicationContext;


    /**
     * @deprecated Used DefaultUrLMappingsEvaluator(ApplicationContext) instead
     * @param servletContext The servlet context
     */
    @Deprecated
    public DefaultUrlMappingEvaluator(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public DefaultUrlMappingEvaluator(WebApplicationContext applicationContext) {
        if (applicationContext != null) {
            this.servletContext = applicationContext.getServletContext();
        }
        this.applicationContext = applicationContext;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public List evaluateMappings(Resource resource) {
        InputStream inputStream = null;
        try {
            inputStream = resource.getInputStream();
            return evaluateMappings(classLoader.parseClass(IOGroovyMethods.getText(inputStream)));
        }
        catch (IOException e) {
            throw new UrlMappingException("Unable to read mapping file [" + resource.getFilename() + "]: " + e.getMessage(), e);
        }
        finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    @SuppressWarnings({"unchecked","rawtypes"})
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
        UrlMappingBuilder builder = new UrlMappingBuilder(binding, servletContext);
        mappings.setDelegate(builder);
        mappings.call();
        builder.urlDefiningMode = false;

        configureUrlMappingDynamicObjects(go);

        return builder.getUrlMappings();
    }

    @SuppressWarnings("rawtypes")
    public List<UrlMapping> evaluateMappings(Closure closure) {
        UrlMappingBuilder builder = new UrlMappingBuilder(null, servletContext);
        closure.setDelegate(builder);
        closure.setResolveStrategy(Closure.DELEGATE_FIRST);
        if (closure.getParameterTypes().length == 0) {
            closure.call();
        }
        else {
            closure.call(applicationContext);
        }
        builder.urlDefiningMode = false;
        configureUrlMappingDynamicObjects(closure);
        return builder.getUrlMappings();
    }

    private void configureUrlMappingDynamicObjects(Object object) {
        if (pluginManager != null) {
            WebMetaUtils.registerCommonWebProperties(GrailsMetaClassUtils.getExpandoMetaClass(object.getClass()), null);
        }
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
        private List<ConstrainedProperty> previousConstraints = new ArrayList<ConstrainedProperty>();
        private List<UrlMapping> urlMappings = new ArrayList<UrlMapping>();
        private Map<String, Object> parameterValues = new HashMap<String, Object>();
        private Binding binding;
        private Object actionName = null;
        private Object pluginName = null;
        private Object namespace = null;
        private Object controllerName = null;
        private Object viewName = null;
        private String httpMethod;
        private ServletContext sc;
        private Object exception;
        private Object parseRequest;
        private Object uri;
        private Object redirectInfo;
        private Deque<ParentResource> parentResources = new ArrayDeque<ParentResource>();

        public UrlMappingBuilder(Binding binding, ServletContext servletContext) {
            this.binding = binding;
            sc = servletContext;
        }

        public List<UrlMapping> getUrlMappings() {
            return urlMappings;
        }

        public ServletContext getServletContext() {
            return sc;
        }

        public ApplicationContext getApplicationContext() {
            return WebApplicationContextUtils.getRequiredWebApplicationContext(sc);
        }

        public GrailsApplication getGrailsApplication() {
            return getApplicationContext().getBean(GrailsApplication.APPLICATION_ID, GrailsApplication.class);
        }

        @Override
        public Object getProperty(String name) {
            if (urlDefiningMode) {
                getCurrentConstraints().add(new ConstrainedProperty(UrlMapping.class, name, String.class));
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

        public void setRedirectInfo(Object redirectInfo) {
            this.redirectInfo = redirectInfo;
        }
        
        public Object getRedirectInfo() {
            return redirectInfo;
        }
        
        public void setPlugin(Object plugin) {
            pluginName = plugin;
        }

        public Object getPlugin() {
            return pluginName;
        }

        public void setNamespace(Object namespace) {
            this.namespace = namespace;
        }

        public Object getNamespace() {
            return namespace;
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

        /**
         * Define a group
         *
         * @param uri The URI
         * @param mappings The mappings
         */
        public void group(String uri, Closure mappings) {

            try {
                parentResources.push(new ParentResource(null, uri,previousConstraints, true));
                mappings.call();
            } finally {
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

        private Object _invoke(String methodName, Object arg, Object delegate) {
            Object[] args = (Object[]) arg;
            String mappedURI = establishFullURI(methodName);
            final boolean isResponseCode = isResponseCode(mappedURI);
            if (mappedURI.startsWith(SLASH) || isResponseCode) {
                // Create a new parameter map for this mapping.
                parameterValues = new HashMap<String, Object>();
                Map variables = binding != null ? binding.getVariables() : null;
                try {
                    urlDefiningMode = false;
                    args = args != null && args.length > 0 ? args : new Object[]{Collections.EMPTY_MAP};
                    if (args[0] instanceof Closure) {
                        UrlMappingData urlData = createUrlMappingData(mappedURI, isResponseCode);

                        Closure callable = (Closure) args[0];
                        if (delegate != null) {
                            callable.setDelegate(delegate);
                        }
                        callable.call();

                        Object controllerName;
                        Object actionName;
                        Object pluginName;
                        String httpMethod = null;
                        String version = null;
                        Object namespace;
                        Object viewName;
                        Object uri;
                        Object redirectInfo = null;

                        if (binding != null) {
                            controllerName = variables.get(GrailsControllerClass.CONTROLLER);
                            actionName = variables.get(GrailsControllerClass.ACTION);
                            viewName = variables.get(GrailsControllerClass.VIEW);
                            uri = variables.get(UrlMapping.URI);
                            pluginName = variables.get(UrlMapping.PLUGIN);
                            namespace = variables.get(UrlMapping.NAMESPACE);
                            if (variables.containsKey(UrlMapping.HTTP_METHOD)) {
                                httpMethod = variables.get(UrlMapping.HTTP_METHOD).toString();
                            }

                        } else {
                            controllerName = this.controllerName;
                            redirectInfo = this.redirectInfo;
                            actionName = this.actionName;
                            pluginName = this.pluginName;
                            namespace = this.namespace;
                            viewName = this.viewName;
                            uri = this.uri;
                            httpMethod = this.httpMethod;
                        }

                        ConstrainedProperty[] constraints = getCurrentConstraints().toArray(new ConstrainedProperty[getCurrentConstraints().size()]);
                        UrlMapping urlMapping;
                        if (uri != null) {
                            try {
                                urlMapping = new RegexUrlMapping(urlData, new URI(uri.toString()), constraints, sc);
                            } catch (URISyntaxException e) {
                                throw new UrlMappingException("Cannot map to invalid URI: " + e.getMessage(), e);
                            }
                        } else {
                            urlMapping = createURLMapping(urlData, isResponseCode, redirectInfo, controllerName, actionName, namespace, pluginName, viewName, httpMethod, version,constraints);
                        }

                        if (binding != null) {
                            Map bindingVariables = variables;
                            Object parse = getParseRequest(Collections.EMPTY_MAP, bindingVariables);
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
                        UrlMappingData urlData = createUrlMappingData(uri, isResponseCode);

                        if (namedArguments.containsKey(RESOURCE)) {
                            Object controller = namedArguments.get(RESOURCE);
                            String controllerName = controller.toString();
                            parentResources.push(new ParentResource(controllerName, uri, getCurrentConstraints(), true));
                            try {
                                invokeLastArgumentIfClosure(args);
                            } finally {
                                parentResources.pop();
                            }
                            if (controller != null) {

                                createSingleResourceRestfulMappings(controllerName, pluginName, namespace, version, urlData, getCurrentConstraints(), calculateIncludes(namedArguments, DEFAULT_RESOURCE_INCLUDES));
                            }
                        } else if (namedArguments.containsKey(RESOURCES)) {
                            Object controller = namedArguments.get(RESOURCES);
                            String controllerName = controller.toString();
                            parentResources.push(new ParentResource(controllerName, uri, getCurrentConstraints(), false));
                            try {
                                invokeLastArgumentIfClosure(args);
                            } finally {
                                parentResources.pop();
                            }
                            if (controller != null) {
                                createResourceRestfulMappings(controllerName, pluginName, namespace,version,urlData, getCurrentConstraints(), calculateIncludes(namedArguments, DEFAULT_RESOURCES_INCLUDES));
                            }
                        } else {

                            invokeLastArgumentIfClosure(args);
                            UrlMapping urlMapping = getURLMappingForNamedArgs(namedArguments, urlData, mappedURI, isResponseCode);
                            configureUrlMapping(urlMapping);
                            return urlMapping;
                        }
                    }
                    return null;
                } finally {
                    if (binding != null) {
                        variables.clear();
                    } else {
                        controllerName = null;
                        redirectInfo = null;
                        actionName = null;
                        viewName = null;
                        pluginName = null;
                        namespace = null;
                    }
                    if (parentResources.isEmpty()) {
                        previousConstraints.clear();
                        urlDefiningMode = true;
                    }
                }
            } else if (!urlDefiningMode && CONSTRAINTS.equals(mappedURI)) {
                ConstrainedPropertyBuilder builder = new ConstrainedPropertyBuilder(this);
                if (args.length > 0 && (args[0] instanceof Closure)) {

                    Closure callable = (Closure) args[0];
                    callable.setDelegate(builder);
                    for (ConstrainedProperty constrainedProperty : getCurrentConstraints()) {
                        builder.getConstrainedProperties().put(constrainedProperty.getPropertyName(), constrainedProperty);
                    }
                    callable.call();
                }
                return builder.getConstrainedProperties();
            } else {
                return super.invokeMethod(mappedURI, arg);
            }
        }


        private List<String> calculateIncludes(Map namedArguments, List<String> defaultResourcesIncludes) {
            List<String> includes = new ArrayList<String>(defaultResourcesIncludes);

            Object excludesObject = namedArguments.get("excludes");
            if (excludesObject != null ) {
                if (excludesObject instanceof List) {

                    List excludeList = (List) excludesObject;
                    for (Object exc : excludeList) {
                        if (exc != null) {
                            String excStr = exc.toString().toLowerCase();
                            includes.remove(excStr);
                        }
                    }
                }
                else {
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
                }
                else {
                    includes.clear();
                    includes.add(includesObject.toString());
                }
            }
            return includes;
        }

        private String establishFullURI(String uri) {
            if (parentResources.isEmpty()) {
                return uri;
            }

            StringBuilder uriBuilder = new StringBuilder();
            ParentResource parentResource = parentResources.peek();
            if (parentResource.isSingle) {
                uriBuilder.append(parentResource.uri);
            }
            else {
                if (parentResource.controllerName != null) {
                    uriBuilder.append(parentResource.uri).append(SLASH).append(CAPTURING_WILD_CARD);
                    getCurrentConstraints().add(new ConstrainedProperty(UrlMapping.class, parentResource.controllerName + "Id", String.class));
                }
            }

            uriBuilder.append(uri);
            return uriBuilder.toString();
        }

        private void invokeLastArgumentIfClosure(Object[] args) {
            if (args.length > 1 && args[1] instanceof Closure) {
                ((Closure) args[1]).call();
            }
        }

        protected void createResourceRestfulMappings(String controllerName, Object pluginName, Object namespace, String version, UrlMappingData urlData, List<ConstrainedProperty> previousConstraints, List<String> includes) {
            ConstrainedProperty[] constraintArray = previousConstraints.toArray(new ConstrainedProperty[previousConstraints.size()]);

            if (includes.contains(ACTION_INDEX)) {
                // GET /$controller -> action:'index'
                UrlMapping listUrlMapping = createIndexActionResourcesRestfulMapping(controllerName, pluginName, namespace,version,urlData, previousConstraints);
                configureUrlMapping(listUrlMapping);
            }

            if (includes.contains(ACTION_CREATE)) {
                // GET /$controller/create -> action:'create'
                UrlMapping createUrlMapping = createCreateActionResourcesRestfulMapping(controllerName, pluginName,namespace, version, urlData, constraintArray);
                configureUrlMapping(createUrlMapping);
            }

            if (includes.contains(ACTION_SAVE)) {
                // POST /$controller -> action:'save'
                UrlMapping saveUrlMapping = createSaveActionResourcesRestfulMapping(controllerName, pluginName, namespace,version, urlData, previousConstraints);
                configureUrlMapping(saveUrlMapping);
            }

            if (includes.contains(ACTION_SHOW)) {
                // GET /$controller/$id -> action:'show'
                UrlMapping showUrlMapping = createShowActionResourcesRestfulMapping(controllerName, pluginName, namespace,version,urlData, previousConstraints);
                configureUrlMapping(showUrlMapping);
            }

            if (includes.contains(ACTION_EDIT)) {
                // GET /$controller/$id/edit -> action:'edit'
                UrlMapping editUrlMapping = createEditActionResourcesRestfulMapping(controllerName, pluginName, namespace,version,urlData, previousConstraints);
                configureUrlMapping(editUrlMapping);
            }

            if (includes.contains(ACTION_UPDATE)) {
                // PUT /$controller/$id -> action:'update'
                UrlMapping updateUrlMapping = createUpdateActionResourcesRestfulMapping(controllerName, pluginName, namespace,version,urlData, previousConstraints);
                configureUrlMapping(updateUrlMapping);
            }

            if (includes.contains(ACTION_DELETE)) {
                // DELETE /$controller/$id -> action:'delete'
                UrlMapping deleteUrlMapping = createDeleteActionResourcesRestfulMapping(controllerName, pluginName, namespace,version,urlData, previousConstraints);
                configureUrlMapping(deleteUrlMapping);
            }
        }

        protected UrlMapping createDeleteActionResourcesRestfulMapping(String controllerName, Object pluginName, Object namespace, String version, UrlMappingData urlData, List<ConstrainedProperty> previousConstraints) {
            UrlMappingData deleteUrlMappingData = createRelativeUrlDataWithIdAndFormat(urlData);
            List<ConstrainedProperty> deleteUrlMappingConstraints = createConstraintsWithIdAndFormat(previousConstraints);

            return new RegexUrlMapping(deleteUrlMappingData,controllerName, ACTION_DELETE, null, pluginName, namespace, HttpMethod.DELETE.toString(), version,deleteUrlMappingConstraints.toArray(new ConstrainedProperty[deleteUrlMappingConstraints.size()]) , servletContext);
        }

        protected UrlMapping createUpdateActionResourcesRestfulMapping(String controllerName, Object pluginName, Object namespace, String version, UrlMappingData urlData, List<ConstrainedProperty> previousConstraints) {
            UrlMappingData updateUrlMappingData = createRelativeUrlDataWithIdAndFormat(urlData);
            List<ConstrainedProperty> updateUrlMappingConstraints = createConstraintsWithIdAndFormat(previousConstraints);

            return new RegexUrlMapping(updateUrlMappingData,controllerName, ACTION_UPDATE, null, pluginName, namespace, HttpMethod.PUT.toString(),version,updateUrlMappingConstraints.toArray(new ConstrainedProperty[updateUrlMappingConstraints.size()]) , servletContext);
        }

        protected UrlMapping createEditActionResourcesRestfulMapping(String controllerName, Object pluginName, Object namespace, String version, UrlMappingData urlData, List<ConstrainedProperty> previousConstraints) {
            UrlMappingData editUrlMappingData = urlData.createRelative('/' + CAPTURING_WILD_CARD + "/edit");
            List<ConstrainedProperty> editUrlMappingConstraints = new ArrayList<ConstrainedProperty>(previousConstraints);
            editUrlMappingConstraints.add(new ConstrainedProperty(UrlMapping.class, "id", String.class));

            return new RegexUrlMapping(editUrlMappingData,controllerName, ACTION_EDIT,null, pluginName, namespace, HttpMethod.GET.toString(), version,editUrlMappingConstraints.toArray(new ConstrainedProperty[editUrlMappingConstraints.size()]) , servletContext);
        }

        protected UrlMapping createShowActionResourcesRestfulMapping(String controllerName, Object pluginName, Object namespace, String version, UrlMappingData urlData, List<ConstrainedProperty> previousConstraints) {
            UrlMappingData showUrlMappingData = createRelativeUrlDataWithIdAndFormat(urlData);
            List<ConstrainedProperty> showUrlMappingConstraints = createConstraintsWithIdAndFormat(previousConstraints);

            return new RegexUrlMapping(showUrlMappingData,controllerName, ACTION_SHOW,null, pluginName, namespace, HttpMethod.GET.toString(), version,showUrlMappingConstraints.toArray(new ConstrainedProperty[showUrlMappingConstraints.size()]) , servletContext);
        }

        private List<ConstrainedProperty> createConstraintsWithIdAndFormat(List<ConstrainedProperty> previousConstraints) {
            List<ConstrainedProperty> showUrlMappingConstraints = new ArrayList<ConstrainedProperty>(previousConstraints);
            showUrlMappingConstraints.add(new ConstrainedProperty(UrlMapping.class, "id", String.class));
            ConstrainedProperty cp = new ConstrainedProperty(UrlMapping.class, "format", String.class);
            cp.setNullable(true);
            showUrlMappingConstraints.add(cp);
            return showUrlMappingConstraints;
        }

        private UrlMappingData createRelativeUrlDataWithIdAndFormat(UrlMappingData urlData) {
            return urlData.createRelative('/' + CAPTURING_WILD_CARD + UrlMapping.OPTIONAL_EXTENSION_WILDCARD + UrlMapping.QUESTION_MARK);
        }
        private UrlMappingData createFormatOnlyUrlMappingData(UrlMappingData urlData) {
            return urlData.createRelative(UrlMapping.OPTIONAL_EXTENSION_WILDCARD + UrlMapping.QUESTION_MARK);
        }


        protected UrlMapping createSaveActionResourcesRestfulMapping(String controllerName, Object pluginName, Object namespace, String version, UrlMappingData urlData, List<ConstrainedProperty> constraints) {
            UrlMappingData saveActionUrlMappingData = urlData.createRelative(UrlMapping.OPTIONAL_EXTENSION_WILDCARD + UrlMapping.QUESTION_MARK);
            List<ConstrainedProperty> saveUrlMappingConstraints = createFormatOnlyConstraints(constraints);

            return new RegexUrlMapping(saveActionUrlMappingData,controllerName, ACTION_SAVE,null, pluginName, namespace, HttpMethod.POST.toString(),version,saveUrlMappingConstraints.toArray(new ConstrainedProperty[saveUrlMappingConstraints.size()]), servletContext);
        }

        protected UrlMapping createCreateActionResourcesRestfulMapping(String controllerName, Object pluginName, Object namespace, String version, UrlMappingData urlData, ConstrainedProperty[] constraintArray) {
            UrlMappingData createMappingData = urlData.createRelative("/create");
            return new RegexUrlMapping(createMappingData,controllerName, ACTION_CREATE, null, pluginName, namespace, HttpMethod.GET.toString(), version,constraintArray, servletContext);
        }

        protected UrlMapping createIndexActionResourcesRestfulMapping(String controllerName, Object pluginName, Object namespace, String version, UrlMappingData urlData, List<ConstrainedProperty> constraints) {
            UrlMappingData indexActionUrlMappingData = urlData.createRelative(UrlMapping.OPTIONAL_EXTENSION_WILDCARD + UrlMapping.QUESTION_MARK);
            List<ConstrainedProperty> indexUrlMappingConstraints = createFormatOnlyConstraints(constraints);

            return new RegexUrlMapping(indexActionUrlMappingData, controllerName, ACTION_INDEX, null, pluginName, namespace, HttpMethod.GET.toString(), version,indexUrlMappingConstraints.toArray(new ConstrainedProperty[indexUrlMappingConstraints.size()]), servletContext);
        }

        private List<ConstrainedProperty> createFormatOnlyConstraints(List<ConstrainedProperty> constraints) {
            List<ConstrainedProperty> indexUrlMappingConstraints = new ArrayList<ConstrainedProperty>(constraints);
            ConstrainedProperty cp = new ConstrainedProperty(UrlMapping.class, "format", String.class);
            cp.setNullable(true);
            indexUrlMappingConstraints.add(cp);
            return indexUrlMappingConstraints;
        }

        /**
         * Takes a controller and creates the necessary URL mappings for a singular RESTful resource
         *
         * @param controllerName The controller name
         * @param pluginName The name of the plugin
         * @param namespace
         * @param version
         * @param urlData   The urlData instance
         * @param previousConstraints Any constraints
         * @param includes
         */
        protected void createSingleResourceRestfulMappings(String controllerName, Object pluginName, Object namespace, String version, UrlMappingData urlData, List<ConstrainedProperty> previousConstraints, List<String> includes) {

            ConstrainedProperty[] constraintArray = previousConstraints.toArray(new ConstrainedProperty[previousConstraints.size()]);

            if (includes.contains(ACTION_CREATE)) {
                // GET /$controller/create -> action: 'create'
                UrlMapping createUrlMapping = createCreateActionResourcesRestfulMapping(controllerName, pluginName, namespace,version, urlData, constraintArray);
                configureUrlMapping(createUrlMapping);
            }

            if (includes.contains(ACTION_SAVE)) {
                // POST /$controller -> action:'save'
                UrlMapping saveUrlMapping = createSaveActionResourcesRestfulMapping(controllerName, pluginName, namespace,version,urlData, previousConstraints);
                configureUrlMapping(saveUrlMapping);
            }

            if (includes.contains(ACTION_SHOW)) {
                // GET /$controller -> action:'show'
                UrlMapping showUrlMapping = createShowActionResourceRestfulMapping(controllerName, pluginName, namespace,version,urlData, previousConstraints);
                configureUrlMapping(showUrlMapping);
            }

            if (includes.contains(ACTION_EDIT)) {
                // GET /$controller/edit -> action:'edit'
                UrlMapping editUrlMapping = createEditctionResourceRestfulMapping(controllerName, pluginName, namespace,version,urlData, constraintArray);
                configureUrlMapping(editUrlMapping);
            }

            if (includes.contains(ACTION_UPDATE)) {
                // PUT /$controller -> action:'update'
                UrlMapping updateUrlMapping = createUpdateActionResourceRestfulMapping(controllerName, pluginName, namespace,version,urlData, previousConstraints);
                configureUrlMapping(updateUrlMapping);
            }

            if (includes.contains(ACTION_DELETE)) {
                // DELETE /$controller -> action:'delete'
                UrlMapping deleteUrlMapping = createDeleteActionResourceRestfulMapping(controllerName, pluginName, namespace,version,urlData, previousConstraints);
                configureUrlMapping(deleteUrlMapping);
            }
        }

        protected UrlMapping createDeleteActionResourceRestfulMapping(String controllerName, Object pluginName, Object namespace, String version, UrlMappingData urlData, List<ConstrainedProperty> previousConstraints) {
            UrlMappingData deleteUrlMappingData = createFormatOnlyUrlMappingData(urlData);
            List<ConstrainedProperty> deleteUrlMappingConstraints = createFormatOnlyConstraints(previousConstraints);

            return new RegexUrlMapping(deleteUrlMappingData,controllerName, ACTION_DELETE, null, pluginName, null, HttpMethod.DELETE.toString(), version, deleteUrlMappingConstraints.toArray(new ConstrainedProperty[deleteUrlMappingConstraints.size()]) , servletContext);
        }

        protected UrlMapping createUpdateActionResourceRestfulMapping(String controllerName, Object pluginName, Object namespace, String version, UrlMappingData urlData, List<ConstrainedProperty> previousConstraints) {
            UrlMappingData updateUrlMappingData = createFormatOnlyUrlMappingData(urlData);
            List<ConstrainedProperty> updateUrlMappingConstraints = createFormatOnlyConstraints(previousConstraints);

            return new RegexUrlMapping(updateUrlMappingData,controllerName, ACTION_UPDATE, null, pluginName, null, HttpMethod.PUT.toString(),version, updateUrlMappingConstraints.toArray(new ConstrainedProperty[updateUrlMappingConstraints.size()]) , servletContext);
        }

        protected UrlMapping createEditctionResourceRestfulMapping(String controllerName, Object pluginName, Object namespace, String version, UrlMappingData urlData, ConstrainedProperty[] constraintArray) {
            UrlMappingData editMappingData = urlData.createRelative("/edit");
            return new RegexUrlMapping(editMappingData,controllerName,ACTION_EDIT, null, pluginName, null, HttpMethod.GET.toString(),version, constraintArray, servletContext);
        }

        protected UrlMapping createShowActionResourceRestfulMapping(String controllerName, Object pluginName, Object namespace, String version, UrlMappingData urlData, List<ConstrainedProperty> previousConstraints) {
            UrlMappingData showUrlMappingData = createFormatOnlyUrlMappingData(urlData);
            List<ConstrainedProperty> showUrlMappingConstraints = createFormatOnlyConstraints(previousConstraints);

            return new RegexUrlMapping(showUrlMappingData,controllerName, ACTION_SHOW,null, pluginName, null, HttpMethod.GET.toString(), version, showUrlMappingConstraints.toArray(new ConstrainedProperty[showUrlMappingConstraints.size()]) , servletContext);
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
            for (int count = s.length(), i = 0; i < count; i++) {
                if (!Character.isDigit(s.charAt(i))) return false;
            }

            return true;
        }

        private UrlMapping getURLMappingForNamedArgs(Map namedArguments,
                UrlMappingData urlData, String mapping, boolean isResponseCode) {
            Object controllerName;
            Object actionName;
            final Map bindingVariables = binding != null ? binding.getVariables() : null;
            controllerName = getControllerName(namedArguments, bindingVariables);
            actionName = getActionName(namedArguments, bindingVariables);
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
            ConstrainedProperty[] constraints = getCurrentConstraints().toArray(new ConstrainedProperty[getCurrentConstraints().size()]);

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
                urlMapping = createURLMapping(urlData, isResponseCode, redirectInfo, controllerName, actionName, namespace, pluginName, viewName, httpMethod != null ? httpMethod.toString() : null, version != null ? version.toString() : null, constraints);
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
                    LOG.error("URL mapping argument [exception] with value [" + exceptionArg + "] must be a valid class");
                }
            }

            Object parseRequest = getParseRequest(namedArguments,bindingVariables);
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
            return getVariableFromNamedArgsOrBinding(namedArguments, bindingVariables,GrailsControllerClass.ACTION, actionName);
        }

        private Object getParseRequest(Map namedArguments, Map bindingVariables) {
            return getVariableFromNamedArgsOrBinding(namedArguments, bindingVariables,PARSE_REQUEST, parseRequest);
        }

        private Object getControllerName(Map namedArguments, Map bindingVariables) {
            return getVariableFromNamedArgsOrBinding(namedArguments, bindingVariables,GrailsControllerClass.CONTROLLER, controllerName);
        }

        private Object getPluginName(Map namedArguments, Map bindingVariables) {
            return getVariableFromNamedArgsOrBinding(namedArguments, bindingVariables, UrlMapping.PLUGIN, pluginName);
        }

        private Object getHttpMethod(Map namedArguments, Map bindingVariables) {
            return getVariableFromNamedArgsOrBinding(namedArguments, bindingVariables, UrlMapping.HTTP_METHOD, pluginName);
        }

        private Object getRedirectInfo(Map namedArguments, Map bindingVariables) {
            return getVariableFromNamedArgsOrBinding(namedArguments, bindingVariables, UrlMapping.REDIRECT_INFO, redirectInfo);
        }
        
        private Object getVersion(Map namedArguments, Map bindingVariables) {
            return getVariableFromNamedArgsOrBinding(namedArguments, bindingVariables, UrlMapping.VERSION, pluginName);
        }

        private Object getNamespace(Map namedArguments, Map bindingVariables) {
            return getVariableFromNamedArgsOrBinding(namedArguments, bindingVariables, UrlMapping.NAMESPACE, namespace);
        }

        private Object getViewName(Map namedArguments, Map bindingVariables) {
            return getVariableFromNamedArgsOrBinding(namedArguments, bindingVariables,GrailsControllerClass.VIEW, viewName);
        }

        private Object getURI(Map namedArguments, Map bindingVariables) {
            return getVariableFromNamedArgsOrBinding(namedArguments, bindingVariables, UrlMapping.URI, uri);
        }

        private Object getException(Map namedArguments, Map bindingVariables) {
            return getVariableFromNamedArgsOrBinding(namedArguments, bindingVariables,EXCEPTION, exception);
        }

        private UrlMapping createURLMapping(UrlMappingData urlData, boolean isResponseCode, Object redirectInfo,
                                            Object controllerName, Object actionName, Object namespace, Object pluginName,
                                            Object viewName, String httpMethod, String version, ConstrainedProperty[] constraints) {
            if (!isResponseCode) {
                return new RegexUrlMapping(redirectInfo, urlData, controllerName, actionName, namespace, pluginName, viewName, httpMethod,  version, constraints, sc);
            }

            return new ResponseCodeUrlMapping(urlData, controllerName, actionName, namespace, pluginName, viewName,
                    null, sc);
        }

        public List<ConstrainedProperty> getCurrentConstraints() {
            ParentResource parentResource = parentResources.peek();
            return parentResource == null ? previousConstraints : parentResource.constraints;
        }

        class ParentResource {
            String controllerName;
            String uri;
            List<ConstrainedProperty> constraints;
            boolean isSingle;

            ParentResource(String controllerName, String uri, List<ConstrainedProperty> constraints, boolean single) {
                this.controllerName = controllerName;
                this.uri = uri;
                this.constraints = new ArrayList<ConstrainedProperty>(constraints);
                isSingle = single;
            }
        }
    }
}
