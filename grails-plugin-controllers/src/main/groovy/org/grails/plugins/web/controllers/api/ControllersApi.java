/*
 * Copyright 2011 SpringSource
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
package org.grails.plugins.web.controllers.api;

import grails.async.Promise;
import grails.converters.JSON;
import grails.core.GrailsControllerClass;
import grails.databinding.CollectionDataBindingSource;
import grails.databinding.DataBindingSource;
import grails.io.IOUtils;
import grails.util.*;
import grails.web.JSONBuilder;
import grails.web.http.HttpHeaders;
import grails.web.mapping.ResponseRedirector;
import grails.web.mime.MimeType;
import grails.web.mime.MimeUtility;
import groovy.lang.*;

import java.io.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import groovy.text.Template;
import groovy.util.slurpersupport.GPathResult;
import groovy.xml.StreamingMarkupBuilder;

import org.grails.core.artefact.ControllerArtefactHandler;
import org.grails.io.support.GrailsResourceUtils;
import org.grails.io.support.SpringIOUtils;

import grails.plugins.GrailsPlugin;

import org.codehaus.groovy.grails.web.json.JSONElement;
import org.codehaus.groovy.grails.web.metaclass.*;
import org.grails.web.pages.GroovyPageTemplate;
import org.grails.web.servlet.view.GroovyPageView;
import org.grails.web.sitemesh.GrailsLayoutDecoratorMapper;
import org.grails.web.sitemesh.GrailsLayoutView;
import org.grails.web.sitemesh.GroovyPageLayoutFinder;
import org.grails.core.artefact.DomainClassArtefactHandler;

import grails.core.GrailsDomainClassProperty;

import org.grails.compiler.web.ControllerActionTransformer;

import grails.plugins.GrailsPluginManager;
import grails.web.databinding.DataBindingUtils;

import org.grails.plugins.web.controllers.ControllerExceptionHandlerMetaData;

import grails.web.mapping.LinkGenerator;

import org.grails.plugins.support.WebMetaUtils;

import grails.web.util.GrailsApplicationAttributes;

import org.grails.plugins.web.controllers.metaclass.ChainMethod;
import org.grails.plugins.web.controllers.metaclass.ForwardMethod;
import org.grails.plugins.web.controllers.metaclass.RenderDynamicMethod;
import org.grails.plugins.web.controllers.metaclass.WithFormMethod;
import org.grails.web.servlet.mvc.ActionResultTransformer;
import org.grails.web.servlet.mvc.GrailsWebRequest;

import grails.web.mapping.mvc.RedirectEventListener;
import grails.web.mapping.mvc.exceptions.CannotRedirectException;

import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.grails.web.api.CommonWebApi;
import org.grails.web.converters.Converter;
import org.grails.web.servlet.mvc.exceptions.ControllerExecutionException;
import org.grails.web.support.ResourceAwareTemplateEngine;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.validation.Errors;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.support.RequestDataValueProcessor;

import static org.grails.plugins.web.controllers.metaclass.RenderDynamicMethod.*;
/**
 * API for each controller in a Grails application.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
@SuppressWarnings("rawtypes")
public class ControllersApi extends CommonWebApi {

    private static final String INCLUDE_MAP_KEY = "include";
    private static final String EXCLUDE_MAP_KEY = "exclude";

    private static final long serialVersionUID = 1;

    protected static final String RENDER_METHOD_NAME = "render";
    protected static final String BIND_DATA_METHOD = "bindData";
    protected static final String SLASH = "/";
    protected transient RenderDynamicMethod render;
    protected transient WithFormMethod withFormMethod;
    protected transient ForwardMethod forwardMethod;
    private Collection<RedirectEventListener> redirectListeners;
    private LinkGenerator linkGenerator;
    private RequestDataValueProcessor requestDataValueProcessor;
    private boolean useJessionId = false;
    private MimeUtility mimeUtility;
    private Collection<ActionResultTransformer> actionResultTransformers;
    private String gspEncoding = DEFAULT_ENCODING;

    public ControllersApi() {
        this(null);
    }

    public ControllersApi(GrailsPluginManager pluginManager) {
        super(pluginManager);
        render = new RenderDynamicMethod();
        withFormMethod = new WithFormMethod();
        forwardMethod = new ForwardMethod();
    }

    public static ApplicationContext getStaticApplicationContext() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (!(requestAttributes instanceof GrailsWebRequest)) {
            return ContextLoader.getCurrentWebApplicationContext();
        }
        return ((GrailsWebRequest)requestAttributes).getApplicationContext();
    }

    public void setGspEncoding(String gspEncoding) {
       this.gspEncoding = gspEncoding;
    }

    public void setRedirectListeners(Collection<RedirectEventListener> redirectListeners) {
        this.redirectListeners = redirectListeners;
    }

    public void setUseJessionId(boolean useJessionId) {
        this.useJessionId = useJessionId;
    }

    public void setLinkGenerator(LinkGenerator linkGenerator) {
        this.linkGenerator = linkGenerator;
    }

    /**
     * Constructor used by controllers
     *
     * @param instance The instance
     */
    public static void initialize(Object instance) {
        ApplicationContext applicationContext = getStaticApplicationContext();
        if (applicationContext == null) {
            return;
        }

        applicationContext.getAutowireCapableBeanFactory().autowireBeanProperties(
                instance, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);

        if (Environment.getCurrent() == Environment.TEST) {
            GrailsWebRequest webRequest = GrailsWebRequest.lookup();
            if (webRequest != null) {
                // GRAILS-10929 - If the class name ends with $$..., then it's a proxy and we want to remove that from the name
				String className = instance.getClass().getName().replaceAll("\\$\\$.*$", "");
                webRequest.setControllerName(GrailsNameUtils.getLogicalPropertyName(className, ControllerArtefactHandler.TYPE));
            }
        }
    }

    /**
     * Returns the URI of the currently executing action
     *
     * @return The action URI
     */
    public String getActionUri(Object instance) {
        return SLASH + getControllerName(instance) + SLASH + getActionName(instance);
    }

    /**
     * Returns the URI of the currently executing controller
     * @return The controller URI
     */
    public String getControllerUri(Object instance) {
        return SLASH + getControllerName(instance);
    }

    /**
     * Obtains a URI of a template by name
     *
     * @param name The name of the template
     * @return The template URI
     */
    public String getTemplateUri(Object instance, String name) {
        return getGrailsAttributes(instance).getTemplateUri(name, getRequest(instance));
    }

    /**
     * Obtains a URI of a view by name
     *
     * @param name The name of the view
     * @return The template URI
     */
    public String getViewUri(Object instance, String name) {
        return getGrailsAttributes(instance).getViewUri(name, getRequest(instance));
    }

    /**
     * Sets the errors instance of the current controller
     *
     * @param errors The error instance
     */
    public void setErrors(Object instance, Errors errors) {
        currentRequestAttributes().setAttribute(GrailsApplicationAttributes.ERRORS, errors, 0);
    }

    /**
     * Obtains the errors instance for the current controller
     *
     * @return The Errors instance
     */
    public Errors getErrors(Object instance) {
        return (Errors)currentRequestAttributes().getAttribute(GrailsApplicationAttributes.ERRORS, 0);
    }

    /**
     * Sets the ModelAndView of the current controller
     *
     * @param mav The ModelAndView
     */
    public void setModelAndView(Object instance, ModelAndView mav) {
        currentRequestAttributes().setAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW, mav, 0);
    }

    /**
     * Obtains the ModelAndView for the currently executing controller
     *
     * @return The ModelAndView
     */
    public ModelAndView getModelAndView(Object instance) {
        return (ModelAndView)currentRequestAttributes().getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW, 0);
    }

    /**
     * Obtains the chain model which is used to chain request attributes from one request to the next via flash scope
     * @return The chainModel
     */
    public Map getChainModel(Object instance) {
        return (Map)getFlash(instance).get("chainModel");
    }

    /**
     * Return true if there are an errors
     * @return true if there are errors
     */
    public boolean hasErrors(Object instance) {
        final Errors errors = getErrors(instance);
        return errors != null && errors.hasErrors();
    }

    /**
     * Redirects for the given arguments.
     *
     * @param argMap The arguments
     * @return null
     */
    public Object redirect(Object target, Map argMap) {

        if (argMap.isEmpty()) {
            throw new MissingMethodException("redirect",target.getClass(),new Object[]{ argMap });
        }

        GrailsWebRequest webRequest = (GrailsWebRequest)RequestContextHolder.currentRequestAttributes();

        if(target instanceof GroovyObject) {
            GroovyObject controller = (GroovyObject)target;

            // if there are errors add it to the list of errors
            Errors controllerErrors = (Errors)controller.getProperty(ControllerDynamicMethods.ERRORS_PROPERTY);
            Errors errors = (Errors)argMap.get(ControllerDynamicMethods.ERRORS_PROPERTY);
            if (controllerErrors != null && errors != null) {
                controllerErrors.addAllErrors(errors);
            }
            else {
                controller.setProperty(ControllerDynamicMethods.ERRORS_PROPERTY, errors);
            }
            Object action = argMap.get(GrailsControllerClass.ACTION);
            if (action != null) {
                argMap.put(GrailsControllerClass.ACTION, establishActionName(action,controller));
            }
            if (!argMap.containsKey(GrailsControllerClass.NAMESPACE_PROPERTY)) {
                // this could be made more efficient if we had a reference to the GrailsControllerClass object, which
                // has the namespace property accessible without needing reflection
                argMap.put(GrailsControllerClass.NAMESPACE_PROPERTY, GrailsClassUtils.getStaticFieldValue(controller.getClass(), GrailsControllerClass.NAMESPACE_PROPERTY));
            }
        }

        ResponseRedirector redirector = new ResponseRedirector(getLinkGenerator(webRequest));
        redirector.setRedirectListeners(redirectListeners);
        redirector.setRequestDataValueProcessor(initRequestDataValueProcessor());
        redirector.setUseJessionId(useJessionId);
        redirector.redirect(webRequest.getRequest(), webRequest.getResponse(), argMap);
        return null;
    }

    /**
     * Redirects for the given arguments.
     *
     * @param object A domain class
     * @return null
     */
    @SuppressWarnings("unchecked")
    public Object redirect(Object instance,Object object) {
        if(object != null) {

            Class<?> objectClass = object.getClass();
            boolean isDomain = DomainClassArtefactHandler.isDomainClass(objectClass) && object instanceof GroovyObject;
            if(isDomain) {

                Object id = ((GroovyObject)object).getProperty(GrailsDomainClassProperty.IDENTITY);
                if(id != null) {
                    Map args = new HashMap();
                    args.put(LinkGenerator.ATTRIBUTE_RESOURCE, object);
                    args.put(LinkGenerator.ATTRIBUTE_METHOD, HttpMethod.GET.toString());
                    return redirect(instance, args);
                }
            }
        }
        throw new CannotRedirectException("Cannot redirect for object ["+object+"] it is not a domain or has no identifier. Use an explicit redirect instead ");
    }

    /**
     * Invokes the chain method for the given arguments
     *
     * @param instance The instance
     * @param args The arguments
     * @return Result of the redirect call
     */
    public Object chain(Object instance, Map args) {
        return ChainMethod.invoke(instance, args);
    }

    // the render method
    public Object render(Object instance, Object o) {
        return invokeRender(instance, DefaultGroovyMethods.inspect(o));
    }

    public Object render(Object instance, String txt) {
        return invokeRender(instance, txt);
    }

    public Object render(Object instance, Map args) {
        return invokeRender(instance, args);
    }

    public Object render(Object instance, Closure c) {
        return invokeRender(instance, c);
    }

    public Object render(Object instance, Map args, Closure c) {
        return invokeRender(instance, args, c);
    }

    protected Object invokeRender(Object target, Object... arguments) {
        if (arguments.length == 0) {
            throw new MissingMethodException(METHOD_SIGNATURE, target.getClass(), arguments);
        }

        GrailsWebRequest webRequest = (GrailsWebRequest) RequestContextHolder.currentRequestAttributes();
        HttpServletResponse response = webRequest.getCurrentResponse();

        boolean renderView = true;
        GroovyObject controller = (GroovyObject) target;

        String explicitSiteMeshLayout = null;

        final Object renderArgument = arguments[0];
        if (renderArgument instanceof Converter<?>) {
            renderView = renderConverter((Converter<?>)renderArgument, response);
        } else if (renderArgument instanceof Writable) {
            applyContentType(response, null, renderArgument);
            Writable writable = (Writable)renderArgument;
            renderView = renderWritable(writable, response);
        } else if (renderArgument instanceof CharSequence) {
            applyContentType(response, null, renderArgument);
            CharSequence text = (CharSequence)renderArgument;
            renderView = renderText(text, response);
        }
        else {
            final Object renderObject = arguments[arguments.length - 1];
            if (renderArgument instanceof Closure) {
                setContentType(response, TEXT_HTML, DEFAULT_ENCODING, true);
                Closure closure = (Closure) renderObject;
                renderView = renderMarkup(closure, response);
            }
            else if (renderArgument instanceof Map) {
                Map argMap = (Map) renderArgument;

                if (argMap.containsKey(ARGUMENT_LAYOUT)) {
                    explicitSiteMeshLayout = String.valueOf(argMap.get(ARGUMENT_LAYOUT));
                }

                boolean statusSet = false;
                if (argMap.containsKey(ARGUMENT_STATUS)) {
                    Object statusObj = argMap.get(ARGUMENT_STATUS);
                    if (statusObj != null) {
                        try {
                            final int statusCode = statusObj instanceof Number ? ((Number)statusObj).intValue() : Integer.parseInt(statusObj.toString());
                            response.setStatus(statusCode);
                            statusSet = true;
                        }
                        catch (NumberFormatException e) {
                            throw new ControllerExecutionException(
                                    "Argument [status] of method [render] must be a valid integer.");
                        }
                    }
                }

                if (renderObject instanceof Writable) {
                    Writable writable = (Writable) renderObject;
                    applyContentType(response, argMap, renderObject);
                    renderView = renderWritable(writable, response);
                }
                else if (renderObject instanceof Closure) {
                    Closure callable = (Closure) renderObject;
                    applyContentType(response, argMap, renderObject);
                    if (BUILDER_TYPE_JSON.equals(argMap.get(ARGUMENT_BUILDER)) || isJSONResponse(response)) {
                        renderView = renderJSON(callable, response);
                    }
                    else {
                        renderView = renderMarkup(callable, response);
                    }
                }
                else if (renderObject instanceof CharSequence) {
                    applyContentType(response, argMap, renderObject);
                    CharSequence text = (CharSequence) renderObject;
                    renderView = renderText(text, response);
                }
                else if (argMap.containsKey(ARGUMENT_TEXT)) {
                    Object textArg = argMap.get(ARGUMENT_TEXT);
                    applyContentType(response, argMap, textArg);
                    if (textArg instanceof Writable) {
                        Writable writable = (Writable) textArg;
                        renderView = renderWritable(writable, response);
                    } else {
                        CharSequence text = (textArg instanceof CharSequence) ? ((CharSequence)textArg) : textArg.toString();
                        renderView = renderText(text, response);
                    }
                }
                else if (argMap.containsKey(ARGUMENT_VIEW)) {
                    renderView(webRequest, argMap, target, controller);
                }
                else if (argMap.containsKey(ARGUMENT_TEMPLATE)) {
                    applyContentType(response, argMap, null, false);
                    renderView = renderTemplate(target, controller, webRequest, argMap, explicitSiteMeshLayout);
                }
                else if (argMap.containsKey(ARGUMENT_FILE)) {
                    renderView = false;

                    Object o = argMap.get(ARGUMENT_FILE);
                    Object fnO = argMap.get(ARGUMENT_FILE_NAME);
                    String fileName = fnO != null ? fnO.toString() : ((o instanceof File) ? ((File)o).getName(): null );
                    if (o != null) {
                        boolean hasContentType = applyContentType(response, argMap, null, false);
                        if (fileName != null) {
                            if(!hasContentType) {
                                hasContentType = detectContentTypeFromFileName(webRequest, response, argMap, fileName);
                            }
                            if (fnO != null) {
                                response.setHeader(HttpHeaders.CONTENT_DISPOSITION, DISPOSITION_HEADER_PREFIX + fileName);
                            }
                        }
                        if (!hasContentType) {
                            throw new ControllerExecutionException(
                                    "Argument [file] of render method specified without valid [contentType] argument");
                        }

                        InputStream input = null;
                        try {
                            if (o instanceof File) {
                                File f = (File) o;
                                input = IOUtils.openStream(f);
                            }
                            else if (o instanceof InputStream) {
                                input = (InputStream) o;
                            }
                            else if (o instanceof byte[]) {
                                input = new ByteArrayInputStream((byte[])o);
                            }
                            else {
                                input = IOUtils.openStream(new File(o.toString()));
                            }
                            SpringIOUtils.copy(input, response.getOutputStream());
                        } catch (IOException e) {
                            throw new ControllerExecutionException(
                                    "I/O error copying file to response: " + e.getMessage(), e);

                        }
                        finally {
                            if (input != null) {
                                try {
                                    input.close();
                                } catch (IOException e) {
                                    // ignore
                                }
                            }
                        }
                    }
                }
                else if (statusSet) {
                    // GRAILS-6711 nothing to render, just setting status code, so don't render the map
                    renderView = false;
                }
                else {
                    Object object = renderArgument;
                    if (object instanceof JSONElement) {
                        renderView = renderJSON((JSONElement)object, response);
                    }
                    else{
                        try {
                            renderView = renderObject(object, response.getWriter());
                        }
                        catch (IOException e) {
                            // ignore
                        }
                    }
                }
            }
            else {
                throw new MissingMethodException(METHOD_SIGNATURE, target.getClass(), arguments);
            }
        }
        applySiteMeshLayout(webRequest.getCurrentRequest(), renderView, explicitSiteMeshLayout);
        webRequest.setRenderView(renderView);
        return null;
    }

    public Object bindData(Object instance, Object target, Object bindingSource, final List excludes) {
        return bindData(instance, target, bindingSource, CollectionUtils.newMap(EXCLUDE_MAP_KEY, excludes), null);
    }

    public Object bindData(Object instance, Object target, Object bindingSource, final List excludes, String filter) {
        return bindData(instance, target, bindingSource, CollectionUtils.newMap(EXCLUDE_MAP_KEY, excludes), filter);
    }

    public Object bindData(Object instance, Object target, Object bindingSource, Map includeExclude) {
        return bindData(instance, target, bindingSource, includeExclude, null);
    }

    public Object bindData(Object instance, Object target, Object bindingSource, String filter) {
        return bindData(instance, target, bindingSource, Collections.EMPTY_MAP, filter);
    }

    public Object bindData(Object instance, Object target, Object bindingSource) {
        return bindData(instance, target, bindingSource, Collections.EMPTY_MAP, null);
    }

    public Object bindData(Object instance, Object target, Object bindingSource, Map includeExclude, String filter) {
        List include = convertToListIfString(includeExclude.get(INCLUDE_MAP_KEY));
        List exclude = convertToListIfString(includeExclude.get(EXCLUDE_MAP_KEY));
        DataBindingUtils.bindObjectToInstance(target, bindingSource, include, exclude, filter);
        return target;
    }

    public <T> void bindData(Object instance, Class<T> targetType, Collection<T> collectionToPopulate, ServletRequest request) throws Exception {
        DataBindingUtils.bindToCollection(targetType, collectionToPopulate, request);
    }

    public <T> void bindData(Object instance, Class<T> targetType, Collection<T> collectionToPopulate, CollectionDataBindingSource collectionBindingSource) throws Exception {
        DataBindingUtils.bindToCollection(targetType, collectionToPopulate, collectionBindingSource);
    }

    /**
     * Sets a response header for the given name and value
     *
     * @param instance The instance
     * @param headerName The header name
     * @param headerValue The header value
     */
    public void header(Object instance, String headerName, Object headerValue) {
        if (headerValue == null) {
            return;
        }

        final HttpServletResponse response = getResponse(instance);
        if (response != null) {
            response.setHeader(headerName, headerValue.toString());
        }
    }

    /**
     * Used the synchronizer token pattern to avoid duplicate form submissions
     *
     * @param instance The instance
     * @param callable The closure to execute
     * @return The result of the closure execution
     */
    public Object withForm(Object instance, Closure callable) {
        return withFormMethod.withForm(getWebRequest(instance), callable);
    }

    /**
     * Forwards a request for the given parameters using the RequestDispatchers forward method
     *
     * @param instance The instance
     * @param params The parameters
     * @return The forwarded URL
     */
    public String forward(Object instance, Map params) {
        return forwardMethod.forward(getRequest(instance), getResponse(instance), params);
    }

    /**
     * Initializes a command object.
     *
     * If type is a domain class and the request body or parameters include an id, the id is used to retrieve
     * the command object instance from the database, otherwise the no-arg constructor on type is invoke.  If
     * an attempt is made to retrieve the command object instance from the database and no corresponding
     * record is found, null is returned.
     *
     * The command object is then subjected to data binding and dependency injection before being returned.
     *
     *
     * @param controllerInstance The controller instance
     * @param type The type of the command object
     * @return the initialized command object or null if the command object is a domain class, the body or
     * parameters included an id and no corresponding record was found in the database.
     */
    public Object initializeCommandObject(final Object controllerInstance, final Class type, final String commandObjectParameterName) throws Exception {
        final HttpServletRequest request = getRequest(controllerInstance);
        final DataBindingSource dataBindingSource = DataBindingUtils.createDataBindingSource(getGrailsApplication(controllerInstance), type, request);
        final DataBindingSource commandObjectBindingSource = WebMetaUtils.getCommandObjectBindingSourceForPrefix(commandObjectParameterName, dataBindingSource);
        Object commandObjectInstance = null;
        Object entityIdentifierValue = null;
        final boolean isDomainClass = DomainClassArtefactHandler.isDomainClass(type);
        if(isDomainClass) {
            entityIdentifierValue = commandObjectBindingSource.getIdentifierValue();
            if(entityIdentifierValue == null) {
                final GrailsWebRequest webRequest = GrailsWebRequest.lookup(request);
                entityIdentifierValue = webRequest != null ? webRequest.getParams().getIdentifier() : null;
            }
        }
        if(entityIdentifierValue instanceof String) {
            entityIdentifierValue = ((String)entityIdentifierValue).trim();
            if("".equals(entityIdentifierValue) || "null".equals(entityIdentifierValue)) {
                entityIdentifierValue = null;
            }
        }

        final HttpMethod requestMethod = HttpMethod.valueOf(request.getMethod());

        if(entityIdentifierValue != null) {
            commandObjectInstance = InvokerHelper.invokeStaticMethod(type, "get", entityIdentifierValue);
        } else if(requestMethod == HttpMethod.POST || !isDomainClass){
            commandObjectInstance = type.newInstance();
        }

        if(commandObjectInstance != null) {
            final boolean shouldDoDataBinding;

            if(entityIdentifierValue != null) {
                switch(requestMethod) {
                    case PATCH:
                    case POST:
                    case PUT:
                        shouldDoDataBinding = true;
                        break;
                    default:
                        shouldDoDataBinding = false;
                }
            } else {
                shouldDoDataBinding = true;
            }

            if(shouldDoDataBinding) {
                bindData(controllerInstance, commandObjectInstance, commandObjectBindingSource, Collections.EMPTY_MAP, null);
            }

            final ApplicationContext applicationContext = getApplicationContext(controllerInstance);
            final AutowireCapableBeanFactory autowireCapableBeanFactory = applicationContext.getAutowireCapableBeanFactory();
            autowireCapableBeanFactory.autowireBeanProperties(commandObjectInstance, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);
        }
        return commandObjectInstance;
    }

    @SuppressWarnings("unchecked")
    public Method getExceptionHandlerMethodFor(final Object controllerInstance, final Class<? extends Exception> exceptionType) throws Exception {
        if(!Exception.class.isAssignableFrom(exceptionType)) {
            throw new IllegalArgumentException("exceptionType [" + exceptionType.getName() + "] argument must be Exception or a subclass of Exception");
        }
        Method handlerMethod = null;
        final List<ControllerExceptionHandlerMetaData> exceptionHandlerMetaDataInstances = (List<ControllerExceptionHandlerMetaData>) GrailsClassUtils.getStaticFieldValue(controllerInstance.getClass(), ControllerActionTransformer.EXCEPTION_HANDLER_META_DATA_FIELD_NAME);
        if(exceptionHandlerMetaDataInstances != null && exceptionHandlerMetaDataInstances.size() > 0) {

            // find all of the handler methods which could accept this exception type
            final List<ControllerExceptionHandlerMetaData> matches = (List<ControllerExceptionHandlerMetaData>) DefaultGroovyMethods.findAll(exceptionHandlerMetaDataInstances, new Closure(this) {
                @Override
                public Object call(Object object) {
                    ControllerExceptionHandlerMetaData md = (ControllerExceptionHandlerMetaData) object;
                    return md.getExceptionType().isAssignableFrom(exceptionType);
                }
            });

            if(matches.size() > 0) {
                ControllerExceptionHandlerMetaData theOne = matches.get(0);

                // if there are more than 1, find the one that is farthest down the inheritance hierarchy
                for(int i = 1; i < matches.size(); i++) {
                    final ControllerExceptionHandlerMetaData nextMatch = matches.get(i);
                    if(theOne.getExceptionType().isAssignableFrom(nextMatch.getExceptionType())) {
                        theOne = nextMatch;
                    }
                }
                handlerMethod = controllerInstance.getClass().getMethod(theOne.getMethodName(), theOne.getExceptionType());
            }
        }

        return handlerMethod;
    }

    private LinkGenerator getLinkGenerator(GrailsWebRequest webRequest) {
        if (linkGenerator == null) {
            ApplicationContext applicationContext = webRequest.getApplicationContext();
            if (applicationContext != null) {
                linkGenerator = applicationContext.getBean("grailsLinkGenerator", LinkGenerator.class);
            }
        }

        return linkGenerator;
    }

    /*
     * Figures out the action name from the specified action reference (either a string or closure)
     */
    private String establishActionName(Object actionRef, Object target) {
        String actionName = null;
        if (actionRef instanceof String) {
            actionName = (String)actionRef;
        }
        else if (actionRef instanceof CharSequence) {
            actionName = actionRef.toString();
        }
        else if (actionRef instanceof Closure) {
            GrailsUtil.deprecated("Using a closure reference in the 'action' argument of the 'redirect' method is deprecated. Please change to use a String.");
            actionName = GrailsClassUtils.findPropertyNameForValue(target, actionRef);
        }
        return actionName;
    }


    @SuppressWarnings("unchecked")
    private List convertToListIfString(Object o) {
        if (o instanceof String) {
            List list = new ArrayList();
            list.add(o);
            o = list;
        }
        return (List) o;
    }



    /**
     * getter to obtain RequestDataValueProcessor from
     */
    private RequestDataValueProcessor initRequestDataValueProcessor() {
        GrailsWebRequest webRequest = (GrailsWebRequest)RequestContextHolder.currentRequestAttributes();
        ApplicationContext applicationContext = webRequest.getApplicationContext();
        if (requestDataValueProcessor == null && applicationContext.containsBean("requestDataValueProcessor")) {
            requestDataValueProcessor = applicationContext.getBean("requestDataValueProcessor", RequestDataValueProcessor.class);
        }
        return requestDataValueProcessor;
    }

    private void applySiteMeshLayout(HttpServletRequest request, boolean renderView, String explicitSiteMeshLayout) {
        if(explicitSiteMeshLayout == null && request.getAttribute(GrailsLayoutDecoratorMapper.LAYOUT_ATTRIBUTE) != null) {
            // layout has been set already
            return;
        }
        String siteMeshLayout = explicitSiteMeshLayout != null ? explicitSiteMeshLayout : (renderView ? null : GrailsLayoutDecoratorMapper.NONE_LAYOUT);
        if(siteMeshLayout != null) {
            request.setAttribute(GrailsLayoutDecoratorMapper.LAYOUT_ATTRIBUTE, siteMeshLayout);
        }
    }

    private boolean renderConverter(Converter<?> converter, HttpServletResponse response) {
        converter.render(response);
        return false;
    }

    private String resolveContentTypeBySourceType(final Object renderArgument, String defaultEncoding) {
        return renderArgument instanceof GPathResult ? APPLICATION_XML : defaultEncoding;
    }

    private boolean applyContentType(HttpServletResponse response, Map argMap, Object renderArgument) {
        return applyContentType(response, argMap, renderArgument, true);
    }

    private boolean applyContentType(HttpServletResponse response, Map argMap, Object renderArgument, boolean useDefault) {
        boolean contentTypeIsDefault = true;
        String contentType = resolveContentTypeBySourceType(renderArgument, useDefault ? TEXT_HTML : null);
        String encoding = DEFAULT_ENCODING;
        if (argMap != null) {
            if(argMap.containsKey(ARGUMENT_CONTENT_TYPE)) {
                contentType = argMap.get(ARGUMENT_CONTENT_TYPE).toString();
                contentTypeIsDefault = false;
            }
            if(argMap.containsKey(ARGUMENT_ENCODING)) {
                encoding = argMap.get(ARGUMENT_ENCODING).toString();
                contentTypeIsDefault = false;
            }
        }
        if(contentType != null) {
            setContentType(response, contentType, encoding, contentTypeIsDefault);
            return true;
        }
        return false;
    }

    private boolean renderJSON(JSONElement object, HttpServletResponse response) {
        response.setContentType(GrailsWebUtil.getContentType("application/json", DEFAULT_ENCODING));
        return renderWritable(object, response);
    }

    private boolean detectContentTypeFromFileName(GrailsWebRequest webRequest, HttpServletResponse response, Map argMap, String fileName) {
        MimeUtility mimeUtility = lookupMimeUtility(webRequest);
        if (mimeUtility != null) {
            MimeType mimeType = mimeUtility.getMimeTypeForExtension(GrailsStringUtils.getFilenameExtension(fileName));
            if (mimeType != null) {
                String contentType = mimeType.getName();
                Object encodingObj = argMap.get(ARGUMENT_ENCODING);
                String encoding = encodingObj != null ? encodingObj.toString() : DEFAULT_ENCODING;
                setContentType(response, contentType, encoding);
                return true;
            }
        }
        return false;
    }

    private MimeUtility lookupMimeUtility(GrailsWebRequest webRequest) {
        if (mimeUtility == null) {
            ApplicationContext applicationContext = webRequest.getApplicationContext();
            if (applicationContext != null) {
                mimeUtility = applicationContext.getBean("grailsMimeUtility", MimeUtility.class);
            }
        }
        return mimeUtility;
    }

    private boolean renderTemplate(Object target, GroovyObject controller, GrailsWebRequest webRequest,
                                   Map argMap, String explicitSiteMeshLayout) {
        boolean renderView;
        boolean hasModel = argMap.containsKey(ARGUMENT_MODEL);
        Object modelObject = null;
        if(hasModel) {
            modelObject = argMap.get(ARGUMENT_MODEL);
        }
        String templateName = argMap.get(ARGUMENT_TEMPLATE).toString();
        String contextPath = getContextPath(webRequest, argMap);

        String var = null;
        if (argMap.containsKey(ARGUMENT_VAR)) {
            var = String.valueOf(argMap.get(ARGUMENT_VAR));
        }

        // get the template uri
        String templateUri = webRequest.getAttributes().getTemplateURI(controller, templateName);

        // retrieve gsp engine
        ResourceAwareTemplateEngine engine = webRequest.getAttributes().getPagesTemplateEngine();
        try {
            Template t = engine.createTemplateForUri(new String[]{
                    GrailsResourceUtils.appendPiecesForUri(contextPath, templateUri),
                    GrailsResourceUtils.appendPiecesForUri(contextPath, "/grails-app/views/", templateUri)});

            if (t == null) {
                throw new ControllerExecutionException("Unable to load template for uri [" +
                        templateUri + "]. Template not found.");
            }

            if (t instanceof GroovyPageTemplate) {
                ((GroovyPageTemplate)t).setAllowSettingContentType(true);
            }

            GroovyPageView gspView = new GroovyPageView();
            gspView.setTemplate(t);
            try {
                gspView.afterPropertiesSet();
            } catch (Exception e) {
                throw new RuntimeException("Problem initializing view", e);
            }

            View view = gspView;
            boolean renderWithLayout = (explicitSiteMeshLayout != null || webRequest.getCurrentRequest().getAttribute(GrailsLayoutDecoratorMapper.LAYOUT_ATTRIBUTE) != null);
            if(renderWithLayout) {
                applySiteMeshLayout(webRequest.getCurrentRequest(), false, explicitSiteMeshLayout);
                try {
                    GroovyPageLayoutFinder groovyPageLayoutFinder = webRequest.getApplicationContext().getBean("groovyPageLayoutFinder", GroovyPageLayoutFinder.class);
                    view = new GrailsLayoutView(groovyPageLayoutFinder, gspView);
                } catch (NoSuchBeanDefinitionException e) {
                    // ignore
                }
            }

            Map binding = new HashMap();

            if (argMap.containsKey(ARGUMENT_BEAN)) {
                Object bean = argMap.get(ARGUMENT_BEAN);
                if (hasModel) {
                    if (modelObject instanceof Map) {
                        setTemplateModel(webRequest, binding, (Map) modelObject);
                    }
                }
                renderTemplateForBean(webRequest, view, binding, bean, var);
            }
            else if (argMap.containsKey(ARGUMENT_COLLECTION)) {
                Object colObject = argMap.get(ARGUMENT_COLLECTION);
                if (hasModel) {
                    if (modelObject instanceof Map) {
                        setTemplateModel(webRequest, binding, (Map)modelObject);
                    }
                }
                renderTemplateForCollection(webRequest, view, binding, colObject, var);
            }
            else if (hasModel) {
                if (modelObject instanceof Map) {
                    setTemplateModel(webRequest, binding, (Map)modelObject);
                }
                renderViewForTemplate(webRequest, view, binding);
            }
            else {
                renderViewForTemplate(webRequest, view, binding);
            }
            renderView = false;
        }
        catch (GroovyRuntimeException gre) {
            throw new ControllerExecutionException("Error rendering template [" + templateName + "]: " + gre.getMessage(), gre);
        }
        catch (IOException ioex) {
            throw new ControllerExecutionException("I/O error executing render method for arguments [" + argMap + "]: " + ioex.getMessage(), ioex);
        }
        return renderView;
    }

    protected void renderViewForTemplate(GrailsWebRequest webRequest, View view, Map binding) {
        try {
            view.render(binding, webRequest.getCurrentRequest(), webRequest.getResponse());
        }
        catch (Exception e) {
            throw new ControllerExecutionException(e.getMessage(), e);
        }
    }

    protected Collection<ActionResultTransformer> getActionResultTransformers(GrailsWebRequest webRequest) {
        if (actionResultTransformers == null) {

            ApplicationContext applicationContext = webRequest.getApplicationContext();
            if (applicationContext != null) {
                actionResultTransformers = applicationContext.getBeansOfType(ActionResultTransformer.class).values();
            }
            if (actionResultTransformers == null) {
                actionResultTransformers = Collections.emptyList();
            }
        }

        return actionResultTransformers;
    }

    private void setTemplateModel(GrailsWebRequest webRequest, Map binding, Map modelObject) {
        Map modelMap = modelObject;
        webRequest.setAttribute(GrailsApplicationAttributes.TEMPLATE_MODEL, modelMap, RequestAttributes.SCOPE_REQUEST);
        binding.putAll(modelMap);
    }

    private String getContextPath(GrailsWebRequest webRequest, Map argMap) {
        Object cp = argMap.get(ARGUMENT_CONTEXTPATH);
        String contextPath = (cp != null ? cp.toString() : "");

        Object pluginName = argMap.get(ARGUMENT_PLUGIN);
        if (pluginName != null) {
            ApplicationContext applicationContext = webRequest.getApplicationContext();
            GrailsPluginManager pluginManager = (GrailsPluginManager) applicationContext.getBean(GrailsPluginManager.BEAN_NAME);
            GrailsPlugin plugin = pluginManager.getGrailsPlugin(pluginName.toString());
            if (plugin != null && !plugin.isBasePlugin()) contextPath = plugin.getPluginPath();
        }
        return contextPath;
    }

    private void setContentType(HttpServletResponse response, String contentType, String encoding) {
        setContentType(response, contentType, encoding, false);
    }

    private void setContentType(HttpServletResponse response, String contentType, String encoding, boolean contentTypeIsDefault) {
        if (!contentTypeIsDefault || response.getContentType()==null) {
            response.setContentType(GrailsWebUtil.getContentType(contentType, encoding));
        }
    }

    private boolean renderObject(Object object, Writer out) {
        boolean renderView;
        try {
            out.write(DefaultGroovyMethods.inspect(object));
            renderView = false;
        }
        catch (IOException e) {
            throw new ControllerExecutionException("I/O error obtaining response writer: " + e.getMessage(), e);
        }
        return renderView;
    }

    private void renderTemplateForCollection(GrailsWebRequest webRequest, View view, Map binding, Object colObject, String var) throws IOException {
        if (colObject instanceof Iterable) {
            Iterable c = (Iterable) colObject;
            for (Object o : c) {
                if (GrailsStringUtils.isBlank(var)) {
                    binding.put(DEFAULT_ARGUMENT, o);
                }
                else {
                    binding.put(var, o);
                }
                renderViewForTemplate(webRequest, view, binding);
            }
        }
        else {
            if (GrailsStringUtils.isBlank(var)) {
                binding.put(DEFAULT_ARGUMENT, colObject);
            }
            else {
                binding.put(var, colObject);
            }

            renderViewForTemplate(webRequest, view, binding);
        }
    }

    private void renderTemplateForBean(GrailsWebRequest webRequest, View view, Map binding, Object bean, String varName) throws IOException {
        if (GrailsStringUtils.isBlank(varName)) {
            binding.put(DEFAULT_ARGUMENT, bean);
        }
        else {
            binding.put(varName, bean);
        }
        renderViewForTemplate(webRequest, view, binding);
    }

    private void renderView(GrailsWebRequest webRequest, Map argMap, Object target, GroovyObject controller) {
        String viewName = argMap.get(ARGUMENT_VIEW).toString();
        String viewUri = webRequest.getAttributes().getNoSuffixViewURI((GroovyObject) target, viewName);
        String contextPath = getContextPath(webRequest, argMap);
        if(contextPath != null) {
            viewUri = contextPath + viewUri;
        }
        Object modelObject = argMap.get(ARGUMENT_MODEL);
        if (modelObject != null) {
            modelObject = argMap.get(ARGUMENT_MODEL);
            boolean isPromise = modelObject instanceof Promise;
            Collection<ActionResultTransformer> resultTransformers = getActionResultTransformers(webRequest);
            for (ActionResultTransformer resultTransformer : resultTransformers) {
                modelObject = resultTransformer.transformActionResult(webRequest,viewUri, modelObject);
            }
            if (isPromise) return;
        }

        applyContentType(webRequest.getCurrentResponse(), argMap, null);

        Map model;
        if (modelObject instanceof Map) {
            model = (Map) modelObject;
        }
        else {
            model = new HashMap();
        }

        controller.setProperty(ControllerDynamicMethods.MODEL_AND_VIEW_PROPERTY, new ModelAndView(viewUri, model));
    }

    private boolean renderJSON(Closure callable, HttpServletResponse response) {
        boolean renderView = true;
        JSONBuilder builder = new JSONBuilder();
        JSON json = builder.build(callable);
        json.render(response);
        renderView = false;
        return renderView;
    }

    private boolean renderMarkup(Closure closure, HttpServletResponse response) {
        StreamingMarkupBuilder b = new StreamingMarkupBuilder();
        b.setEncoding(response.getCharacterEncoding());
        Writable markup = b.bind(closure);
        return renderWritable(markup, response);
    }

    private boolean renderText(CharSequence text, HttpServletResponse response) {
        try {
            PrintWriter writer = response.getWriter();
            return renderText(text, writer);
        }
        catch (IOException e) {
            throw new ControllerExecutionException(e.getMessage(), e);
        }
    }

    private boolean renderWritable(Writable writable, HttpServletResponse response) {
        try {
            PrintWriter writer = response.getWriter();
            writable.writeTo(writer);
            writer.flush();
        }
        catch (IOException e) {
            throw new ControllerExecutionException(e.getMessage(), e);
        }
        return false;
    }

    private boolean renderText(CharSequence text, Writer writer) {
        try {
            if (writer instanceof PrintWriter) {
                ((PrintWriter)writer).print(text);
            }
            else {
                writer.write(text.toString());
            }
            writer.flush();
            return false;
        }
        catch (IOException e) {
            throw new ControllerExecutionException(e.getMessage(), e);
        }
    }

    private boolean isJSONResponse(HttpServletResponse response) {
        String contentType = response.getContentType();
        return contentType != null && (contentType.indexOf("application/json") > -1 ||
                contentType.indexOf("text/json") > -1);
    }
}
