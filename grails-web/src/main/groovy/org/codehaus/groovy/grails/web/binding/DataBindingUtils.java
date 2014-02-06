/*
 * Copyright 2006-2007 Graeme Rocher
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
package org.codehaus.groovy.grails.web.binding;

import grails.util.Environment;
import grails.validation.ValidationErrors;
import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.CollectionUtils;
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.codehaus.groovy.grails.web.binding.bindingsource.DataBindingSourceRegistry;
import org.codehaus.groovy.grails.web.binding.bindingsource.DefaultDataBindingSourceRegistry;
import org.codehaus.groovy.grails.web.mime.MimeType;
import org.codehaus.groovy.grails.web.mime.MimeTypeResolver;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.grails.databinding.CollectionDataBindingSource;
import org.grails.databinding.DataBinder;
import org.grails.databinding.DataBindingSource;
import org.grails.databinding.bindingsource.InvalidRequestBodyException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.context.ApplicationContext;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Utility methods to perform data binding from Grails objects.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@SuppressWarnings("rawtypes")
public class DataBindingUtils {

    public static final String DATA_BINDER_BEAN_NAME = "grailsWebDataBinder";
    private static final String BLANK = "";
    private static final Map<Class, List> CLASS_TO_BINDING_INCLUDE_LIST = new ConcurrentHashMap<Class, List>();

    /**
     * Associations both sides of any bidirectional relationships found in the object and source map to bind
     *
     * @param object The object
     * @param source The source map
     * @param domainClass The DomainClass for the object
     */
    public static void assignBidirectionalAssociations(Object object, Map source, GrailsDomainClass domainClass) {
        if (source == null) {
            return;
        }

        for (Object key : source.keySet()) {
            String propertyName = key.toString();
            if (propertyName.indexOf('.') > -1) {
                propertyName = propertyName.substring(0, propertyName.indexOf('.'));
            }
            if (domainClass.hasPersistentProperty(propertyName)) {
                GrailsDomainClassProperty prop = domainClass.getPropertyByName(propertyName);
                if (prop != null && prop.isOneToOne() && prop.isBidirectional()) {
                    Object val = source.get(key);
                    GrailsDomainClassProperty otherSide = prop.getOtherSide();
                    if (val != null && otherSide != null) {
                        MetaClass mc = GroovySystem.getMetaClassRegistry().getMetaClass(val.getClass());
                        try {
                            mc.setProperty(val, otherSide.getName(), object);
                        }
                        catch (Exception e) {
                            // ignore
                        }
                    }
                }
            }
        }
    }

    /**
     * Binds the given source object to the given target object performing type conversion if necessary
     *
     * @param object The object to bind to
     * @param source The source object
     * @return A BindingResult or null if it wasn't successful
     */
    public static BindingResult bindObjectToInstance(Object object, Object source) {
        return bindObjectToInstance(object, source, getBindingIncludeList(object), Collections.EMPTY_LIST, null);
    }

    private static List getBindingIncludeList(final Object object) {
        List includeList = Collections.EMPTY_LIST;
        try {
            final Class<? extends Object> objectClass = object.getClass();
            if (CLASS_TO_BINDING_INCLUDE_LIST.containsKey(objectClass)) {
                includeList = CLASS_TO_BINDING_INCLUDE_LIST.get(objectClass);
            } else {
                final Field whiteListField = objectClass.getDeclaredField(DefaultASTDatabindingHelper.DEFAULT_DATABINDING_WHITELIST);
                if (whiteListField != null) {
                    if ((whiteListField.getModifiers() & Modifier.STATIC) != 0) {
                         final Object whiteListValue = whiteListField.get(objectClass);
                         if (whiteListValue instanceof List) {
                             includeList = (List)whiteListValue;
                         }
                    }
                }
                if (!Environment.getCurrent().isReloadEnabled()) {
                    CLASS_TO_BINDING_INCLUDE_LIST.put(objectClass, includeList);
                }
            }
        } catch (Exception e) {
        }
        return includeList;
    }

    /**
     * Binds the given source object to the given target object performing type conversion if necessary
     *
     * @param domain The GrailsDomainClass instance
     * @param object The object to bind to
     * @param source The source object
     *
     * @see org.codehaus.groovy.grails.commons.GrailsDomainClass
     *
     * @return A BindingResult or null if it wasn't successful
     */
    public static BindingResult bindObjectToDomainInstance(GrailsDomainClass domain, Object object, Object source) {
        return bindObjectToDomainInstance(domain,object, source, getBindingIncludeList(object), Collections.EMPTY_LIST, null);
    }

    /**
     * For each DataBindingSource provided by collectionBindingSource a new instance of targetType is created,
     * data binding is imposed on that instance with the DataBindingSource and the instance is added to the end of
     * collectionToPopulate
     *
     * @param targetType The type of objects to create, must be a concrete class
     * @param collectionToPopulate A collection to populate with new instances of targetType
     * @param collectionBindingSource A CollectionDataBindingSource
     * @since 2.3
     */
    public static <T> void bindToCollection(final Class<T> targetType, final Collection<T> collectionToPopulate, final CollectionDataBindingSource collectionBindingSource) throws InstantiationException, IllegalAccessException {
        final GrailsApplication application = GrailsWebRequest.lookupApplication();
        GrailsDomainClass domain = null;
        if (application != null) {
            domain = (GrailsDomainClass) application.getArtefact(DomainClassArtefactHandler.TYPE,targetType.getName());
        }
        final List<DataBindingSource> dataBindingSources = collectionBindingSource.getDataBindingSources();
        for(final DataBindingSource dataBindingSource : dataBindingSources) {
            final T newObject = targetType.newInstance();
            bindObjectToDomainInstance(domain, newObject, dataBindingSource, getBindingIncludeList(newObject), Collections.EMPTY_LIST, null);
            collectionToPopulate.add(newObject);
        }
    }

    public static <T> void bindToCollection(final Class<T> targetType, final Collection<T> collectionToPopulate, final ServletRequest request) throws InstantiationException, IllegalAccessException {
        final GrailsApplication grailsApplication = GrailsWebRequest.lookupApplication();
        final CollectionDataBindingSource collectionDataBindingSource = createCollectionDataBindingSource(grailsApplication, targetType, request);
        bindToCollection(targetType, collectionToPopulate, collectionDataBindingSource);
    }

    /**
     * Binds the given source object to the given target object performing type conversion if necessary
     *
     * @param object The object to bind to
     * @param source The source object
     * @param include The list of properties to include
     * @param exclude The list of properties to exclud
     * @param filter The prefix to filter by
     *
     * @return A BindingResult or null if it wasn't successful
     */
    public static BindingResult bindObjectToInstance(Object object, Object source, List include, List exclude, String filter) {
        if (include == null && exclude == null) {
            include = getBindingIncludeList(object);
        }
        GrailsApplication application = GrailsWebRequest.lookupApplication();
        GrailsDomainClass domain = null;
        if (application != null) {
            domain = (GrailsDomainClass) application.getArtefact(DomainClassArtefactHandler.TYPE,object.getClass().getName());
        }
        return bindObjectToDomainInstance(domain, object, source, include, exclude, filter);
    }

    /**
     * Binds the given source object to the given target object performing type conversion if necessary
     *
     * @param domain The GrailsDomainClass instance
     * @param object The object to bind to
     * @param source The source object
     * @param include The list of properties to include
     * @param exclude The list of properties to exclud
     * @param filter The prefix to filter by
     *
     * @see org.codehaus.groovy.grails.commons.GrailsDomainClass
     *
     * @return A BindingResult or null if it wasn't successful
     */
    @SuppressWarnings("unchecked")
    public static BindingResult bindObjectToDomainInstance(GrailsDomainClass domain, Object object,
            Object source, List include, List exclude, String filter) {
        BindingResult bindingResult = null;
        boolean useSpringBinder = false;
        GrailsApplication grailsApplication = null;
        if (domain != null) {
            grailsApplication = domain.getGrailsApplication();
        }
        if (grailsApplication == null) {
            grailsApplication = GrailsWebRequest.lookupApplication();
        }
        if (grailsApplication != null) {
            if (Boolean.TRUE.equals(grailsApplication.getFlatConfig().get("grails.databinding.useSpringBinder"))) {
                useSpringBinder = true;
            }
        }
        if (!useSpringBinder) {
            try {
                final DataBindingSource bindingSource = createDataBindingSource(grailsApplication, object.getClass(), source);
                final DataBinder grailsWebDataBinder = getGrailsWebDataBinder(grailsApplication);
                grailsWebDataBinder.bind(object, bindingSource, filter, include, exclude);
            } catch (InvalidRequestBodyException e) {
                String messageCode = "invalidRequestBody";
                Class objectType = object.getClass();
                String defaultMessage = "An error occurred parsing the body of the request";
                String[] codes = getMessageCodes(messageCode, objectType);
                bindingResult = new BeanPropertyBindingResult(object, objectType.getName());
                bindingResult.addError(new ObjectError(bindingResult.getObjectName(), codes, null, defaultMessage));
            } catch (Exception e) {
                bindingResult = new BeanPropertyBindingResult(object, object.getClass().getName());
                bindingResult.addError(new ObjectError(bindingResult.getObjectName(), e.getMessage()));
            }
        } else {
            if (source instanceof GrailsParameterMap) {
                GrailsParameterMap parameterMap = (GrailsParameterMap) source;
                HttpServletRequest request = parameterMap.getRequest();
                GrailsDataBinder dataBinder = createDataBinder(object, include, exclude, request);
                dataBinder.bind(parameterMap, filter);
                bindingResult = dataBinder.getBindingResult();
            } else if (source instanceof HttpServletRequest) {
                HttpServletRequest request = (HttpServletRequest) source;
                GrailsDataBinder dataBinder = createDataBinder(object, include, exclude, request);
                performBindFromRequest(dataBinder, request, filter);
                bindingResult = dataBinder.getBindingResult();
            } else if (source instanceof Map) {
                Map propertyMap = convertPotentialGStrings((Map) source);
                GrailsDataBinder binder = createDataBinder(object, include, exclude, null);
                performBindFromPropertyValues(binder, new MutablePropertyValues(propertyMap), filter);
                bindingResult = binder.getBindingResult();
            }

            else {
                GrailsWebRequest webRequest = (GrailsWebRequest) RequestContextHolder.getRequestAttributes();
                if (webRequest != null) {
                    GrailsDataBinder binder = createDataBinder(object, include, exclude, webRequest.getCurrentRequest());
                    HttpServletRequest request = webRequest.getCurrentRequest();
                    performBindFromRequest(binder, request, filter);
                    bindingResult = binder.getBindingResult();
                }
            }
        }

        if (domain != null && bindingResult != null) {
            BindingResult newResult = new ValidationErrors(object);
            for (Object error : bindingResult.getAllErrors()) {
                if (error instanceof FieldError) {
                    FieldError fieldError = (FieldError)error;
                    final boolean isBlank = BLANK.equals(fieldError.getRejectedValue());
                    if (!isBlank) {
                        newResult.addError(fieldError);
                    }
                    else if (domain.hasPersistentProperty(fieldError.getField())) {
                        final boolean isOptional = domain.getPropertyByName(fieldError.getField()).isOptional();
                        if (!isOptional) {
                            newResult.addError(fieldError);
                        }
                    }
                    else {
                        newResult.addError(fieldError);
                    }
                }
                else {
                    newResult.addError((ObjectError)error);
                }
            }
            bindingResult = newResult;
        }
        MetaClass mc = GroovySystem.getMetaClassRegistry().getMetaClass(object.getClass());
        if (mc.hasProperty(object, "errors")!=null && bindingResult!=null) {
            ValidationErrors errors = new ValidationErrors(object);
            errors.addAllErrors(bindingResult);
            mc.setProperty(object,"errors", errors);
        }
        return bindingResult;
    }

    protected static String[] getMessageCodes(String messageCode,
            Class objectType) {
        String[] codes = {objectType.getName() + "." + messageCode, messageCode};
        return codes;
    }

    public static DataBindingSourceRegistry getDataBindingSourceRegistry(GrailsApplication grailsApplication) {
        DataBindingSourceRegistry registry = null;
        if(grailsApplication != null) {
            ApplicationContext context = grailsApplication.getMainContext();
            if(context != null) {
                if(context.containsBean(DataBindingSourceRegistry.BEAN_NAME)) {
                    registry = context.getBean(DataBindingSourceRegistry.BEAN_NAME, DataBindingSourceRegistry.class);
                }
            }
        }
        if(registry == null) {
            registry = new DefaultDataBindingSourceRegistry();
        }

        return registry;
    }

    public static DataBindingSource createDataBindingSource(GrailsApplication grailsApplication, Class bindingTargetType, Object bindingSource) {
        final DataBindingSourceRegistry registry = getDataBindingSourceRegistry(grailsApplication);
        final MimeType mimeType = getMimeType(grailsApplication, bindingSource);
        return registry.createDataBindingSource(mimeType, bindingTargetType, bindingSource);
    }

    public static CollectionDataBindingSource createCollectionDataBindingSource(GrailsApplication grailsApplication, Class bindingTargetType, Object bindingSource) {
        final DataBindingSourceRegistry registry = getDataBindingSourceRegistry(grailsApplication);
        final MimeType mimeType = getMimeType(grailsApplication, bindingSource);
        return registry.createCollectionDataBindingSource(mimeType, bindingTargetType, bindingSource);
    }

    public static MimeType getMimeType(GrailsApplication grailsApplication,
            Object bindingSource) {
        final MimeTypeResolver mimeTypeResolver = getMimeTypeResolver(grailsApplication);
        return resolveMimeType(bindingSource, mimeTypeResolver);
    }

    public static MimeTypeResolver getMimeTypeResolver(
            GrailsApplication grailsApplication) {
        MimeTypeResolver mimeTypeResolver = null;
        if(grailsApplication != null) {
            ApplicationContext context = grailsApplication.getMainContext();
            if(context != null) {
                if(context.containsBean(MimeTypeResolver.BEAN_NAME)) {
                    mimeTypeResolver = context.getBean(MimeTypeResolver.BEAN_NAME, MimeTypeResolver.class);
                }
            }
        }
        return mimeTypeResolver;
    }

    public static MimeType resolveMimeType(Object bindingSource, MimeTypeResolver mimeTypeResolver) {
        final MimeType mimeType;
        if(mimeTypeResolver != null) {
            MimeType resolvedMimeType = mimeTypeResolver.resolveRequestMimeType();
            mimeType = resolvedMimeType != null ? resolvedMimeType : MimeType.ALL;
        }
        else if(bindingSource instanceof HttpServletRequest) {
            HttpServletRequest req = (HttpServletRequest) bindingSource;
            String contentType = req.getContentType();
            if(contentType != null) {
                mimeType = new MimeType(contentType);
            }
            else {
                mimeType = MimeType.ALL;
            }
        } else {
            mimeType = MimeType.ALL;
        }
        return mimeType;
    }

    private static DataBinder getGrailsWebDataBinder(final GrailsApplication grailsApplication) {
        DataBinder dataBinder = null;
        if (grailsApplication != null) {
            final ApplicationContext mainContext = grailsApplication.getMainContext();
            if (mainContext != null && mainContext.containsBean(DATA_BINDER_BEAN_NAME)) {
                dataBinder = mainContext.getBean(DATA_BINDER_BEAN_NAME, DataBinder.class);
            }
        }
        if (dataBinder == null) {
            // this should really never happen in the running app as the binder
            // should always be found in the context
            dataBinder = new GrailsWebDataBinder(grailsApplication);
        }
        return dataBinder;
    }

    private static void performBindFromPropertyValues(GrailsDataBinder binder, MutablePropertyValues mutablePropertyValues, String filter) {
        if (filter != null) {
            binder.bind(mutablePropertyValues,filter);
        }
        else {
            binder.bind(mutablePropertyValues);
        }
    }

    private static void performBindFromRequest(GrailsDataBinder binder, HttpServletRequest request,String filter) {
        if (filter != null) {
            binder.bind(request,filter);
        }
        else {
            binder.bind(request);
        }
    }

    private static GrailsDataBinder createDataBinder(Object object, List include, List exclude, HttpServletRequest request) {
        GrailsDataBinder binder;
        if (request == null) {
            binder = GrailsDataBinder.createBinder(object, object.getClass().getName());
        }
        else {
            binder = GrailsDataBinder.createBinder(object, object.getClass().getName(), request);
        }
        includeExcludeFields(binder, include, exclude);
        return binder;
    }

    @SuppressWarnings("unchecked")
    public static Map convertPotentialGStrings(Map<Object, Object> args) {
        Map newArgs = new HashMap(args.size());
        for (Map.Entry<Object, Object> entry : args.entrySet()) {
            newArgs.put(unwrapGString(entry.getKey()), unwrapGString(entry.getValue()));
        }
        return newArgs;
    }

    private static Object unwrapGString(Object value) {
        if (value instanceof CharSequence) {
            return value.toString();
        }
        return value;
    }

    private static void includeExcludeFields(GrailsDataBinder dataBinder, List allowed, List disallowed) {
        updateAllowed(dataBinder, allowed);
        updateDisallowed(dataBinder, allowed, disallowed);
    }

    @SuppressWarnings("unchecked")
    private static void updateAllowed(GrailsDataBinder binder, List allowed) {
        if (allowed == null) {
            return;
        }

        String[] currentAllowed = binder.getAllowedFields();
        List newAllowed = new ArrayList(allowed);
        CollectionUtils.addAll(newAllowed, currentAllowed);
        String[] value = new String[newAllowed.size()];
        newAllowed.toArray(value);
        binder.setAllowedFields(value);
    }

    @SuppressWarnings("unchecked")
    private static void updateDisallowed(GrailsDataBinder binder, List allowed, List disallowed) {
        if (disallowed == null) {
            return;
        }

        String[] currentDisallowed = binder.getDisallowedFields();
        List newDisallowed = new ArrayList(disallowed);
        if (allowed == null) {
            CollectionUtils.addAll(newDisallowed, currentDisallowed);
        } else {
            for (String s : currentDisallowed) {
                if (!allowed.contains(s)) {
                    newDisallowed.add(s);
                }
            }
        }
        String[] value = new String[newDisallowed.size()];
        newDisallowed.toArray(value);
        binder.setDisallowedFields(value);
    }
}
