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
package org.codehaus.groovy.grails.web.binding;

import grails.util.Environment;
import grails.util.GrailsNameUtils;
import grails.validation.DeferredBindingActions;
import groovy.lang.GroovyObject;
import groovy.lang.GroovyRuntimeException;
import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;
import groovy.lang.MetaClassRegistry;
import groovy.lang.MetaProperty;
import groovy.lang.MissingMethodException;
import groovy.lang.MissingPropertyException;

import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TimeZone;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.AnnotationDomainClassArtefactHandler;
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.codehaus.groovy.grails.commons.GrailsDomainConfigurationUtil;
import org.codehaus.groovy.grails.commons.GrailsMetaClassUtils;
import org.codehaus.groovy.grails.commons.metaclass.CreateDynamicMethod;
import org.codehaus.groovy.grails.validation.ConstrainedProperty;
import org.codehaus.groovy.grails.web.json.JSONObject;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.codehaus.groovy.runtime.MetaClassHelper;
import org.codehaus.groovy.runtime.metaclass.ThreadManagedMetaBeanProperty;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.ConfigurablePropertyAccessor;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyAccessor;
import org.springframework.beans.PropertyAccessorUtils;
import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.beans.propertyeditors.LocaleEditor;
import org.springframework.context.ApplicationContext;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Validator;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.ServletRequestParameterPropertyValues;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.support.ByteArrayMultipartFileEditor;
import org.springframework.web.multipart.support.StringMultipartFileEditor;
import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * A data binder that handles binding dates that are specified with a "struct"-like syntax in request parameters.
 * For example for a set of fields defined as:
 * <p/>
 * <code>
 * <input type="hidden" name="myDate_year" value="2005" />
 * <input type="hidden" name="myDate_month" value="6" />
 * <input type="hidden" name="myDate_day" value="12" />
 * <input type="hidden" name="myDate_hour" value="13" />
 * <input type="hidden" name="myDate_minute" value="45" />
 * </code>
 * <p/>
 * This would set the property "myDate" of type java.util.Date with the specified values.
 *
 * @author Graeme Rocher
 */
@SuppressWarnings("rawtypes")
public class GrailsDataBinder extends ServletRequestDataBinder {

    private static final String BIND_EVENT_LISTENERS = "org.codehaus.groovy.grails.BIND_EVENT_LISTENERS";
    private static final String PROPERTY_EDITOR_REGISTRARS = "org.codehaus.groovy.grails.PROPERTY_EDITOR_REGISTRARS";
    private static final Log LOG = LogFactory.getLog(GrailsDataBinder.class);
    private static final String JSON_DATE_FORMAT = "yyyy-MM-dd'T'hh:mm:ss'Z'";

    protected BeanWrapper bean;

    public static final String[] GROOVY_DISALLOWED = new String[] { "metaClass", "properties" };
    public static final String[] DOMAINCLASS_DISALLOWED = new String[] { "id", "version" };
    public static final String[] GROOVY_DOMAINCLASS_DISALLOWED = new String[] { "metaClass", "properties", "id", "version" };
    public static final String NULL_ASSOCIATION = "null";
    private static final String PREFIX_SEPERATOR = ".";
    private static final String[] ALL_OTHER_FIELDS_ALLOWED_BY_DEFAULT = new String[0];
    private static final String CONSTRAINTS_PROPERTY = "constraints";
    private static final String BLANK = "";
    private static final String STRUCTURED_PROPERTY_SEPERATOR = "_";
    private static final char PATH_SEPARATOR = '.';
    private static final String IDENTIFIER_SUFFIX = ".id";
    private List<String> transients = Collections.emptyList();
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.S";
    private static final Object[] NO_HINTS = {};

    private GrailsDomainClass domainClass;
    private GrailsApplication grailsApplication;

    /**
     * Create a new GrailsDataBinder instance.
     *
     * @param target     target object to bind onto
     * @param objectName objectName of the target object
     */
    @SuppressWarnings("unchecked")
    public GrailsDataBinder(Object target, String objectName) {
        super(target, objectName);

        setAutoGrowNestedPaths(false);
        bean = (BeanWrapper)((BeanPropertyBindingResult)super.getBindingResult()).getPropertyAccessor();

        Object tmpTransients = GrailsClassUtils.getStaticPropertyValue(bean.getWrappedClass(), GrailsDomainClassProperty.TRANSIENT);
        if (tmpTransients instanceof List) {
            transients = (List)tmpTransients;
        }

        setDisallowedFields(GROOVY_DISALLOWED);
        setAllowedFields(ALL_OTHER_FIELDS_ALLOWED_BY_DEFAULT);
        setIgnoreInvalidFields(true);
    }

    /**
     * Collects all PropertyEditorRegistrars in the application context and
     * calls them to register their custom editors
     *
     * @param servletContext
     * @param registry The PropertyEditorRegistry instance
     */
    private static void registerCustomEditors(ServletContext servletContext, PropertyEditorRegistry registry) {
        if (servletContext == null) {
            return;
        }

        WebApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(servletContext);
        if (context == null) {
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, PropertyEditorRegistrar> editors = (Map<String, PropertyEditorRegistrar>)servletContext.getAttribute(PROPERTY_EDITOR_REGISTRARS);
        if (editors == null) {
            editors = context.getBeansOfType(PropertyEditorRegistrar.class);
            if (!Environment.isDevelopmentMode()) {
                servletContext.setAttribute(PROPERTY_EDITOR_REGISTRARS, editors);
            }
        }
        for (PropertyEditorRegistrar editorRegistrar : editors.values()) {
            editorRegistrar.registerCustomEditors(registry);
        }
    }

    /**
     * Utility method for creating a GrailsDataBinder instance
     *
     * @param target The target object to bind to
     * @param objectName The name of the object
     * @param request A request instance
     * @return A GrailsDataBinder instance
     */
    public static GrailsDataBinder createBinder(Object target, String objectName, HttpServletRequest request) {
        GrailsDataBinder binder = createBinder(target, objectName);
        final GrailsWebRequest webRequest = GrailsWebRequest.lookup(request);
        initializeFromWebRequest(binder, webRequest);

        Locale locale = RequestContextUtils.getLocale(request);
        registerCustomEditors(webRequest, binder, locale);
        return binder;
    }

    private static void initializeFromWebRequest(GrailsDataBinder binder, GrailsWebRequest webRequest) {
        if (webRequest == null) {
            return;
        }

        binder.setGrailsApplication(webRequest.getAttributes().getGrailsApplication());

        if (webRequest.getApplicationContext() != null && webRequest.getApplicationContext().containsBean("dataBindingValidator")) {
            Validator validator = webRequest.getApplicationContext().getBean("dataBindingValidator", Validator.class);
            if (binder.getTarget() != null && validator.supports(binder.getTarget().getClass())) {
                binder.setValidator(validator);
            }
        }
    }

    private void setGrailsApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication;
        String[] disallowed = new String[0];
        final Object target = getTarget();
        if (grailsApplication != null && grailsApplication.isArtefactOfType(
                DomainClassArtefactHandler.TYPE, target.getClass())) {
            if (target instanceof GroovyObject) {
                disallowed = GROOVY_DOMAINCLASS_DISALLOWED;
            }
            else {
                disallowed = DOMAINCLASS_DISALLOWED;
            }
            domainClass = (GrailsDomainClass) grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, target.getClass().getName());
        }
        else if (target instanceof GroovyObject) {
            disallowed = GROOVY_DISALLOWED;
        }
        setDisallowedFields(disallowed);
    }

    /**
     * Registers all known
     *
     * @param grailsWebRequest
     * @param registry
     * @param locale
     */
    public static void registerCustomEditors(GrailsWebRequest grailsWebRequest, PropertyEditorRegistry registry, Locale locale) {
        // Formatters for the different number types.
        NumberFormat floatFormat = NumberFormat.getInstance(locale);
        NumberFormat integerFormat = NumberFormat.getIntegerInstance(locale);

        DateFormat dateFormat = new SimpleDateFormat(DEFAULT_DATE_FORMAT, locale);

        registry.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat,true));
        registry.registerCustomEditor(BigDecimal.class, new CustomNumberEditor(BigDecimal.class, floatFormat, true));
        registry.registerCustomEditor(BigInteger.class, new CustomNumberEditor(BigInteger.class, floatFormat, true));
        registry.registerCustomEditor(Double.class, new CustomNumberEditor(Double.class, floatFormat, true));
        registry.registerCustomEditor(double.class, new CustomNumberEditor(Double.class, floatFormat, true));
        registry.registerCustomEditor(Float.class, new CustomNumberEditor(Float.class, floatFormat, true));
        registry.registerCustomEditor(float.class, new CustomNumberEditor(Float.class, floatFormat, true));
        registry.registerCustomEditor(Long.class, new CustomNumberEditor(Long.class, integerFormat, true));
        registry.registerCustomEditor(long.class, new CustomNumberEditor(Long.class, integerFormat, true));
        registry.registerCustomEditor(Integer.class, new CustomNumberEditor(Integer.class, integerFormat, true));
        registry.registerCustomEditor(int.class, new CustomNumberEditor(Integer.class, integerFormat, true));
        registry.registerCustomEditor(Short.class, new CustomNumberEditor(Short.class, integerFormat, true));
        registry.registerCustomEditor(short.class, new CustomNumberEditor(Short.class, integerFormat, true));
        registry.registerCustomEditor(Date.class, new CompositeEditor(new StructuredDateEditor(dateFormat,true), new CustomDateEditor(new SimpleDateFormat(JSON_DATE_FORMAT), true)));
        registry.registerCustomEditor(Calendar.class, new StructuredDateEditor(dateFormat,true));

        ServletContext servletContext = grailsWebRequest != null ? grailsWebRequest.getServletContext() : null;
        registerCustomEditors(servletContext, registry);
    }

    /**
     * Utility method for creating a GrailsDataBinder instance
     *
     * @param target The target object to bind to
     * @param objectName The name of the object
     * @return A GrailsDataBinder instance
     */
    public static GrailsDataBinder createBinder(Object target, String objectName) {
        GrailsDataBinder binder = new GrailsDataBinder(target, objectName);
        binder.registerCustomEditor(byte[].class, new ByteArrayMultipartFileEditor());
        binder.registerCustomEditor(String.class, new StringMultipartFileEditor());
        binder.registerCustomEditor(Currency.class, new CurrencyEditor());
        binder.registerCustomEditor(Locale.class, new LocaleEditor());
        binder.registerCustomEditor(TimeZone.class, new TimeZoneEditor());
        binder.registerCustomEditor(URI.class, new UriEditor());
//        GenericConversionService conversionService = new GenericConversionService();
//        conversionService.addConverter(new GenericConverter() {
//
//            @Override
//            public Set<ConvertiblePair> getConvertibleTypes() {
//                return Collections.singleton(new ConvertiblePair(Map.class, Object.class));
//            }
//
//            @Override
//            public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
//                Object obj = BeanUtils.instantiate(targetType.getObjectType());
//                createBinder(obj, obj.getClass().getName()).bind(new MutablePropertyValues((Map<?, ?>) source));
//                return obj;
//            }
//        });
//        binder.setConversionService(conversionService);

        final GrailsWebRequest webRequest = GrailsWebRequest.lookup();
        if (webRequest == null) {
            registerCustomEditors(null, binder);
        }
        else {
            initializeFromWebRequest(binder, webRequest);
            Locale locale = RequestContextUtils.getLocale(webRequest.getCurrentRequest());
            registerCustomEditors(webRequest, binder, locale);
        }

        return binder;
    }

    @Override
    public void bind(PropertyValues propertyValues) {
        bind(propertyValues, null);
    }

    /**
     * Binds from a GrailsParameterMap object
     *
     * @param params The GrailsParameterMap object
     */
    public void bind(GrailsParameterMap params) {
        bind(params, null);
    }

    public void bind(GrailsParameterMap params, String prefix) {
        Map paramsMap = params;
        if (prefix != null) {
            Object o = params.get(prefix);
            if (o instanceof Map) paramsMap = (Map) o;
        }
        bindWithRequestAndPropertyValues(params.getRequest(), new MutablePropertyValues(paramsMap));
    }

    public void bind(PropertyValues propertyValues, String prefix) {
        PropertyValues values = filterPropertyValues(propertyValues, prefix);
        if (propertyValues instanceof MutablePropertyValues) {
            MutablePropertyValues mutablePropertyValues = (MutablePropertyValues) propertyValues;
            preProcessMutablePropertyValues(mutablePropertyValues);
        }
        super.bind(values);
    }

    @Override
    public void bind(ServletRequest request) {
        bind(request, null);
    }

    public void bind(ServletRequest request, String prefix) {
        MutablePropertyValues mpvs;
        if (prefix != null) {
            mpvs = new ServletRequestParameterPropertyValues(request, prefix, PREFIX_SEPERATOR);
        }
        else {
            mpvs = new ServletRequestParameterPropertyValues(request);
        }

        bindWithRequestAndPropertyValues(request, mpvs);
    }

    private void bindWithRequestAndPropertyValues(ServletRequest request, MutablePropertyValues mpvs) {
        GrailsWebRequest webRequest = GrailsWebRequest.lookup((HttpServletRequest) request);
        if (webRequest != null) {
            final ApplicationContext applicationContext = webRequest.getApplicationContext();
            if (applicationContext != null) {
                ServletContext servletContext = webRequest.getServletContext();
                @SuppressWarnings("unchecked")
                Map<String, BindEventListener> bindEventListenerMap = (Map<String, BindEventListener>)servletContext.getAttribute(BIND_EVENT_LISTENERS);
                if (bindEventListenerMap==null) {
                    bindEventListenerMap = applicationContext.getBeansOfType(BindEventListener.class);
                    if (!Environment.isDevelopmentMode()) {
                        servletContext.setAttribute(BIND_EVENT_LISTENERS, bindEventListenerMap);
                    }
                }
                for (BindEventListener bindEventListener : bindEventListenerMap.values()) {
                    bindEventListener.doBind(getTarget(), mpvs, getTypeConverter());
                }
            }
        }
        preProcessMutablePropertyValues(mpvs);

        if (request instanceof MultipartHttpServletRequest) {
            MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
            bindMultipart(multipartRequest.getMultiFileMap(), mpvs);
        }
        doBind(mpvs);
    }

    private void preProcessMutablePropertyValues(MutablePropertyValues mpvs) {
        autoCreateIfPossible(mpvs);
        checkStructuredProperties(mpvs);
        bindAssociations(mpvs);
    }

    @Override
    protected void doBind(MutablePropertyValues mpvs) {
        filterNestedParameterMaps(mpvs);
        filterBlankValuesWhenTargetIsNullable(mpvs);
        super.doBind(mpvs);
        validate(NO_HINTS);
    }

    private void filterBlankValuesWhenTargetIsNullable(MutablePropertyValues mpvs) {
        Object target = getTarget();
        Map constrainedProperties = resolveConstrainedProperties(target, domainClass);
        if (constrainedProperties == null) {
            return;
        }

        PropertyValue[] valueArray = mpvs.getPropertyValues();
        for (PropertyValue propertyValue : valueArray) {
            if (BLANK.equals(propertyValue.getValue())) {
                ConstrainedProperty cp = getConstrainedPropertyForPropertyValue(constrainedProperties, propertyValue);
                if (shouldNullifyBlankString(propertyValue, cp)) {
                    propertyValue.setConvertedValue(null);
                }
            }
        }
    }

    private ConstrainedProperty getConstrainedPropertyForPropertyValue(Map constrainedProperties, PropertyValue propertyValue) {
        final String propertyName = propertyValue.getName();
        if (propertyName.indexOf(PATH_SEPARATOR) > -1) {
            String[] propertyNames = propertyName.split("\\.");
            Object target = getTarget();
            Object value = getPropertyValueForPath(target, propertyNames);
            if (value != null) {
                Map nestedConstrainedProperties = resolveConstrainedProperties(value);
                if (nestedConstrainedProperties != null) {
                    return (ConstrainedProperty)nestedConstrainedProperties.get(propertyNames[propertyNames.length-1]);
                }
            }
            return null;
        }

        return (ConstrainedProperty)constrainedProperties.get(propertyName);
    }

    private Map resolveConstrainedProperties(Object object) {
        return resolveConstrainedProperties(object, (grailsApplication != null)?((GrailsDomainClass) grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, object.getClass().getName())):null);
    }

    private Map resolveConstrainedProperties(Object object, GrailsDomainClass dc) {
        Map constrainedProperties = null;
        if (dc != null) {
            constrainedProperties = dc.getConstrainedProperties();
        }
        else {
            // is this dead code? , didn't remove in case it's used somewhere
            MetaClass mc = GroovySystem.getMetaClassRegistry().getMetaClass(object.getClass());
            MetaProperty metaProp = mc.getMetaProperty(CONSTRAINTS_PROPERTY);
            if (metaProp != null) {
                Object constrainedPropsObj = getMetaPropertyValue(metaProp, object);
                if (constrainedPropsObj instanceof Map) {
                    constrainedProperties = (Map)constrainedPropsObj;
                }
            }
        }
        return constrainedProperties;
    }

    /**
     * Hack because of bug in ThreadManagedMetaBeanProperty, http://jira.codehaus.org/browse/GROOVY-3723 , fixed since 1.6.5
     *
     * @param metaProperty
     * @param delegate
     * @return
     */
    private Object getMetaPropertyValue(MetaProperty metaProperty, Object delegate) {
        if (metaProperty instanceof ThreadManagedMetaBeanProperty) {
            return ((ThreadManagedMetaBeanProperty)metaProperty).getGetter().invoke(delegate, MetaClassHelper.EMPTY_ARRAY);
        }

        return metaProperty.getProperty(delegate);
    }

    private Object getPropertyValueForPath(Object target, String[] propertyNames) {
        BeanWrapper wrapper = new BeanWrapperImpl(target);
        Object obj = target;
        for (int i = 0; i < propertyNames.length-1; i++) {
            String propertyName = propertyNames[i];
            if (wrapper.isReadableProperty(propertyName)) {
                obj = wrapper.getPropertyValue(propertyName);
                if (obj == null) break;
                wrapper = new BeanWrapperImpl(obj);
            }
        }

        return obj;
    }

    private boolean shouldNullifyBlankString(PropertyValue propertyValue, ConstrainedProperty cp) {
        return cp != null && cp.isNullable() && BLANK.equals(propertyValue.getValue());
    }

    private void filterNestedParameterMaps(MutablePropertyValues mpvs) {
        for (PropertyValue pv : mpvs.getPropertyValues()) {
            final Object value = pv.getValue();
            if (JSONObject.NULL.getClass().isInstance(value)) {
                mpvs.removePropertyValue(pv);
            }
            if (!isCandidateForBinding(pv)) {
                mpvs.removePropertyValue(pv);
            }
        }
    }

    private boolean isCandidateForBinding(PropertyValue pv) {
        boolean isCandidate = true;
        final Object value = pv.getValue();
        if (value instanceof GrailsParameterMap || value instanceof JSONObject) {
            isCandidate = false;
        } else if (value instanceof Map) {
            isCandidate = false;
            final String propertyName = pv.getName();
            final PropertyDescriptor property = BeanUtils.getPropertyDescriptor(getTarget().getClass(), propertyName);
            if (property != null) {
                final Class<?> propertyType = property.getPropertyType();
                if (propertyType.isAssignableFrom(value.getClass())) {
                    isCandidate = true;
                }
            }
        }
        return isCandidate;
    }

    private PropertyValues filterPropertyValues(PropertyValues propertyValues, String prefix) {
        if (prefix == null || prefix.length() == 0) return propertyValues;

        PropertyValue[] valueArray = propertyValues.getPropertyValues();
        MutablePropertyValues newValues = new MutablePropertyValues();
        for (PropertyValue propertyValue : valueArray) {
            String name = propertyValue.getName();
            final String prefixWithDot = prefix + PREFIX_SEPERATOR;
            if (name.startsWith(prefixWithDot)) {
                name = name.substring(prefixWithDot.length(),name.length());
                newValues.addPropertyValue(name, propertyValue.getValue());
            }
        }
        return newValues;
    }

    /**
     * Auto-creates the a type if it is null and is possible to auto-create.
     *
     * @param mpvs A MutablePropertyValues instance
     */
    protected void autoCreateIfPossible(MutablePropertyValues mpvs) {
        PropertyValue[] pvs = mpvs.getPropertyValues();
        for (PropertyValue pv : pvs) {
            String propertyName = pv.getName();
            if (!isAllowed(propertyName)) continue;
            if (propertyName.indexOf(PATH_SEPARATOR) > -1) {
                String[] propertyNames = propertyName.split("\\.");
                BeanWrapper currentBean = bean;

                for (String name : propertyNames) {
                    Object created = autoCreatePropertyIfPossible(currentBean, name, pv.getValue());
                    if (created != null) {
                        currentBean = new BeanWrapperImpl(created);
                    }
                    else {
                        break;
                    }
                }
            }
            else {
                autoCreatePropertyIfPossible(bean, propertyName, pv.getValue());
            }
        }
    }

    @Override
    protected boolean isAllowed(String field) {
        int i = field.indexOf('[');
        if (i>-1) {
            field = field.substring(0,i);
        }
        return super.isAllowed(field);
    }

    @SuppressWarnings("unchecked")
    private Object autoCreatePropertyIfPossible(BeanWrapper wrapper, String propertyName, Object propertyValue) {

        propertyName = PropertyAccessorUtils.canonicalPropertyName(propertyName);
        int currentKeyStart = propertyName.indexOf(PropertyAccessor.PROPERTY_KEY_PREFIX_CHAR);
        int currentKeyEnd = propertyName.indexOf(PropertyAccessor.PROPERTY_KEY_SUFFIX_CHAR);
        String propertyNameWithIndex = propertyName;
        if (currentKeyStart > -1) {
            propertyName = propertyName.substring(0, currentKeyStart);
        }

        Class<?> type = wrapper.getPropertyType(propertyName);
        Object val = wrapper.isReadableProperty(propertyName) ? wrapper.getPropertyValue(propertyName) : null;

        LOG.debug("Checking if auto-create is possible for property ["+propertyName+"] and type ["+type+"]");
        if (type != null && val == null && (isDomainClass(type) || isEmbedded(wrapper, propertyName))) {
            if (!shouldPropertyValueSkipAutoCreate(propertyValue) && isNullAndWritableProperty(wrapper, propertyName)) {
                if (isDomainClass(type)) {
                    Object created = autoInstantiateDomainInstance(type);
                    if (created != null) {
                        val = created;
                        wrapper.setPropertyValue(propertyName, created);
                    }
                }
                else if (isEmbedded(wrapper, propertyName)) {
                    Object created = autoInstantiateEmbeddedInstance(type);
                    if (created != null) {
                        val = created;
                        wrapper.setPropertyValue(propertyName, created);
                    }
                }
            }
        }
        else {
            final Object beanInstance = wrapper.getWrappedInstance();
            if (type != null && Collection.class.isAssignableFrom(type)) {
                Collection<?> c = null;
                final Class<?> referencedType = getReferencedTypeForCollection(propertyName, beanInstance);

                if (isNullAndWritableProperty(wrapper, propertyName)) {
                    c = decorateCollectionForDomainAssociation(GrailsClassUtils.createConcreteCollection(type), referencedType);
                }
                else {
                    if (wrapper.isReadableProperty(propertyName)) {
                        c = decorateCollectionForDomainAssociation((Collection<?>) wrapper.getPropertyValue(propertyName), referencedType);
                    }
                }

                if (wrapper.isWritableProperty(propertyName) && c != null) {
                    wrapper.setPropertyValue(propertyName, c);
                }

                val = c;

                if (c != null && currentKeyStart > -1 && currentKeyEnd > -1) {
                    String indexString = propertyNameWithIndex.substring(currentKeyStart + 1, currentKeyEnd);
                    int index = Integer.parseInt(indexString);

                    // See if we have an instance in the collection. If so, that specific instance
                    // is the value to return for this indexed property.
                    Object instance = findIndexedValue(c, index);
                    if (instance != null) {
                        val = instance;
                    }
                    // If no value in the collection, this might be a domain class
                    else if (isDomainClass(referencedType)) {
                        instance = autoInstantiateDomainInstance(referencedType);
                        if (instance != null) {
                            val = instance;
                            if (index == c.size()) {
                                addAssociationToTarget(propertyName, beanInstance, instance);
                            }
                            else if (index > c.size()) {
                                while (index > c.size()) {
                                    addAssociationToTarget(propertyName, beanInstance, autoInstantiateDomainInstance(referencedType));
                                }

                                addAssociationToTarget(propertyName, beanInstance, instance);
                            }
                        }
                    }
                }
            }
            else if (type != null && Map.class.isAssignableFrom(type)) {
                Map<String, Object> map;
                if (isNullAndWritableProperty(wrapper, propertyName)) {
                    map = new HashMap<String, Object>();
                    wrapper.setPropertyValue(propertyName,map);
                }
                else {
                    map = (Map)wrapper.getPropertyValue(propertyName);
                }
                val = map;
                wrapper.setPropertyValue(propertyName, val);

                if (currentKeyStart > -1 && currentKeyEnd > -1) {
                    String indexString = propertyNameWithIndex.substring(currentKeyStart + 1, currentKeyEnd);
                    Class<?> referencedType = getReferencedTypeForCollection(propertyName, beanInstance);
                    if (isDomainClass(referencedType)) {
                        final Object domainInstance = autoInstantiateDomainInstance(referencedType);
                        val = domainInstance;
                        map.put(indexString, domainInstance);
                    }
                }
            }
        }

        return val;
    }

    private boolean isDomainClass(final Class<?> clazz) {
        return DomainClassArtefactHandler.isDomainClass(clazz) || AnnotationDomainClassArtefactHandler.isJPADomainClass(clazz);
    }

    private boolean isEmbedded(BeanWrapper wrapper, String propertyName) {
        Object embedded = GrailsClassUtils.getStaticPropertyValue(wrapper.getWrappedClass(), GrailsDomainClassProperty.EMBEDDED);
        return embedded instanceof List && ((List)embedded).contains(propertyName);
    }

    private boolean shouldPropertyValueSkipAutoCreate(Object propertyValue) {
        return (propertyValue instanceof Map) || ((propertyValue instanceof String) && StringUtils.isBlank((String) propertyValue));
    }

    private Collection decorateCollectionForDomainAssociation(Collection c, final Class referencedType) {
        if (canDecorateWithListOrderedSet(c, referencedType)) {
            c = ListOrderedSet.decorate((Set) c);
        }
        return c;
    }

    private boolean canDecorateWithListOrderedSet(Collection c, Class referencedType) {
        return (c instanceof Set) && !(c instanceof ListOrderedSet) && !(c instanceof SortedSet) && isDomainClass(referencedType);
    }

    private Object findIndexedValue(Collection c, int index) {
        if (c instanceof List) {
            // If we have a list, try to return the element by index. For data binding, it's very possible
            // that this may be a LazyList so we'll just go right for the index instead of checking the
            // size first.
            try {
                return ((List)c).get(index);
            }
            catch( IndexOutOfBoundsException ignored) {
            }
        }
        else {
            int j = 0;
            for (Object o : c) {
                j++;
                if (j == index) return o;
            }
        }

        return null;
    }

    private Object autoInstantiateDomainInstance(Class<?> type) {
        Object created = null;
        try {
            MetaClass mc = GroovySystem.getMetaClassRegistry().getMetaClass(type);
            if (mc != null) {
                created = mc.invokeStaticMethod(type, CreateDynamicMethod.METHOD_NAME, new Object[0]);
            }
        }
        catch (MissingMethodException mme) {
            LOG.warn("Unable to auto-create type, 'create' method not found");
        }
        catch (GroovyRuntimeException gre) {
            LOG.warn("Unable to auto-create type, Groovy Runtime error: " + gre.getMessage(), gre);
        }
        return created;
    }

    private Object autoInstantiateEmbeddedInstance(Class<?> type) {
        Object created = null;
        try {
            created = type.newInstance();
        }
        catch (InstantiationException e) {
            LOG.error(String.format("Unable to auto-create type %s, %s thrown in constructor", type, e.getClass()));
        }
        catch (IllegalAccessException e) {
            LOG.error(String.format("Unable to auto-create type %s, cannot access constructor", type));
        }
        return created;
    }

    private boolean isNullAndWritableProperty(ConfigurablePropertyAccessor accessor, String propertyName) {
        return accessor.isWritableProperty(propertyName) && (accessor.isReadableProperty(propertyName) && accessor.getPropertyValue(propertyName) == null);
    }

    /**
     * Interrogates the specified properties looking for properites that represent associations to other
     * classes (e.g., 'author.id').  If such a property is found, this method attempts to load the specified
     * instance of the association (by ID) and set it on the target object.
     *
     * @param mpvs the <code>MutablePropertyValues</code> object holding the parameters from the request
     */
    protected void bindAssociations(MutablePropertyValues mpvs) {
        for (PropertyValue pv : mpvs.getPropertyValues()) {
            String propertyName = pv.getName();
            String propertyNameToCheck = propertyName;
            final int i = propertyName.indexOf('.');
            if (i >-1) {
                propertyNameToCheck = propertyName.substring(0,i);
            }

            if (!isAllowed(propertyNameToCheck)) continue;

            if (propertyName.endsWith(IDENTIFIER_SUFFIX)) {
                propertyName = propertyName.substring(0, propertyName.length() - 3);
                if (!isAllowed(propertyName)) continue;
                if (isReadableAndPersistent(propertyName) && bean.isWritableProperty(propertyName)) {
                    if (NULL_ASSOCIATION.equals(pv.getValue())) {
                        bean.setPropertyValue(propertyName, null);
                        mpvs.removePropertyValue(pv);
                    }
                    else {
                        Class<?> type = getPropertyTypeForPath(propertyName);

                        final Object persisted = getPersistentInstance(type, pv.getValue());

                        if (persisted != null) {
                            bean.setPropertyValue(propertyName, persisted);

                            if (domainClass != null) {
                                GrailsDomainClassProperty property = domainClass.getPersistentProperty(propertyName);
                                if (property != null) {
                                    final GrailsDomainClassProperty otherSide = property.getOtherSide();
                                    if (otherSide != null && List.class.isAssignableFrom(otherSide.getType()) && !property.isOptional()) {
                                        DeferredBindingActions.addBindingAction(
                                            new Runnable() {
                                                public void run() {
                                                    if (otherSide.isOneToMany()) {
                                                        Collection collection = GrailsMetaClassUtils.getPropertyIfExists(persisted, otherSide.getName(), Collection.class);
                                                        if (collection != null && !collection.contains(getTarget())) {
                                                            String methodName = "addTo" + GrailsNameUtils.getClassName(otherSide.getName());
                                                            GrailsMetaClassUtils.invokeMethodIfExists(persisted, methodName, new Object[]{getTarget()});
                                                        }
                                                    }
                                                }
                                            }
                                        );
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else {
                if (isReadableAndPersistent(propertyName)) {
                    Class<?> type = getPropertyTypeForPath(propertyName);
                    if (type != null) {
                        if (Collection.class.isAssignableFrom(type)) {
                            bindCollectionAssociation(mpvs, pv);
                        }
                    }
                }
            }
        }
    }

    private Class<?> getPropertyTypeForPath(String propertyName) {
        Class<?> type = bean.getPropertyType(propertyName);
        if (type == null) {
            // type not available via BeanWrapper - this happens with e.g. empty list indexes - so
            // find type by examining GrailsDomainClass
            Object target = bean.getWrappedInstance();
            String path = propertyName.replaceAll("\\[.+?\\]", "");
            if (path.indexOf(PATH_SEPARATOR) > -1) {
                // transform x.y.z into value of x.y and path z
                String nestedProp = StringUtils.substringBeforeLast(propertyName, ".");
                target = bean.getPropertyValue(nestedProp);
                path = StringUtils.substringAfterLast(path, ".");
            }
            if(target != null) {
                type = getReferencedTypeForCollection(path, target);
            }
        }
        return type;
    }

    private boolean isReadableAndPersistent(String propertyName) {
        return bean.isReadableProperty(propertyName) && !transients.contains(propertyName);
    }

    private Object getPersistentInstance(Class<?> type, Object id) {
        Object persisted;
        try {
            persisted = InvokerHelper.invokeStaticMethod(type, "get", id);
        }
        catch (MissingMethodException e) {
            return null; // GORM not installed, continue to operate as normal
        }
        catch (IllegalStateException e) {
            return null; // GORM not installed, continue to operate as normal
        }
        return persisted;
    }

    @SuppressWarnings("unchecked")
    private void bindCollectionAssociation(MutablePropertyValues mpvs, PropertyValue pv) {
        Object v = pv.getValue();
        final boolean isArray = v != null && v.getClass().isArray();

        if (!isArray && !(v instanceof String)) return;

        Collection collection = (Collection) bean.getPropertyValue(pv.getName());
        collection.clear();
        final Class associatedType = getReferencedTypeForCollection(pv.getName(), getTarget());
        final PropertyEditor propertyEditor = findCustomEditor(collection.getClass(), pv.getName());
        if (propertyEditor == null) {
            if (isDomainAssociation(associatedType)) {
                if (isArray) {
                    Object[] identifiers = (Object[])v;
                    for (Object id : identifiers) {
                        if (id != null) {
                            associateObjectForId(pv, id,associatedType);
                        }
                    }
                    mpvs.removePropertyValue(pv);
                }
                else if (v instanceof String) {
                    associateObjectForId(pv,v, associatedType);
                    mpvs.removePropertyValue(pv);
                }
            }
            else if (GrailsDomainConfigurationUtil.isBasicType(associatedType)) {
                Object[] values = null;
                if(isArray) {
                    values = (Object[])v;
                } else if(v instanceof String) {
                    values = new String[]{(String)v};
                }

                if (values != null) {
                    List list = collection instanceof List ? (List)collection : null;
                    for (int i = 0; i < values.length; i++) {
                        Object value = values[i];
                        try {
                            Object newValue = getTypeConverter().convertIfNecessary(value, associatedType);
                            if (list != null) {
                                if (i > list.size() - 1) {
                                    list.add(i, newValue);
                                }
                                else {
                                    list.set(i, newValue);
                                }
                            }
                            else {
                                collection.add(newValue);
                            }
                        }
                        catch (TypeMismatchException e) {
                            // ignore
                        }
                    }
                    mpvs.removePropertyValue(pv);
                }
            }
        }
    }

    private void associateObjectForId(PropertyValue pv, Object id, Class<?> associatedType) {
        final Object target = getTarget();
        final Object obj = getPersistentInstance(associatedType, id);
        addAssociationToTarget(pv.getName(), target, obj);
    }

    private boolean isDomainAssociation(Class<?> associatedType) {
        return associatedType != null && isDomainClass(associatedType);
    }

    private void addAssociationToTarget(String name, Object target, Object obj) {
        if (obj == null) {
            return;
        }

        MetaClassRegistry reg = GroovySystem.getMetaClassRegistry();
        MetaClass mc = reg.getMetaClass(target.getClass());
        final String addMethodName = "addTo" + GrailsNameUtils.getClassNameRepresentation(name);
        mc.invokeMethod(target, addMethodName,obj);
    }

    private Class<?> getReferencedTypeForCollection(String name, Object target) {
        if (grailsApplication != null) {
            GrailsDomainClass dc = (GrailsDomainClass) grailsApplication.getArtefact(
                    DomainClassArtefactHandler.TYPE, target.getClass().getName());
            if (dc != null) {
                GrailsDomainClassProperty domainProperty = dc.getPersistentProperty(name);
                if (domainProperty != null) {
                    return domainProperty.getReferencedPropertyType();
                }
            }
        }
        return null;
    }

    private String getNameOf(PropertyValue propertyValue) {
        String name = propertyValue.getName();
        if (name.indexOf(STRUCTURED_PROPERTY_SEPERATOR) == -1) {
            return name;
        }
        return name.substring(0, name.indexOf(STRUCTURED_PROPERTY_SEPERATOR));
    }

    private boolean isStructured(PropertyValue propertyValue) {
        String name = propertyValue.getName();
        return name.indexOf(STRUCTURED_PROPERTY_SEPERATOR) != -1;
    }

    /**
     * Checks for structured properties. Structured properties are properties with a name
     * containg a "_".
     *
     * @param propertyValues
     */
    private void checkStructuredProperties(MutablePropertyValues propertyValues) {
        Map<String, PropertyValue> valuesByName = new HashMap<String, PropertyValue>();
        List<String> valueNames = new ArrayList<String>();
        mapPropertyValues(propertyValues.getPropertyValues(), valuesByName, valueNames);

        while (!valueNames.isEmpty()) {
            String name = valueNames.remove(0);
            PropertyValue propertyValue = valuesByName.get(name);

            if (!isStructured(propertyValue)) {
                continue;
            }

            String propertyName = getNameOf(propertyValue);
            Class<?> type = bean.getPropertyType(propertyName);
            if (type == null) {
                continue;
            }

            PropertyEditor editor = findCustomEditor(type, propertyName);

            if (editor instanceof CompositeEditor) {
                CompositeEditor composite = (CompositeEditor) editor;
                List<PropertyEditor> propertyEditors = composite.getPropertyEditors();
                for (PropertyEditor propertyEditor : propertyEditors) {
                    if (null == propertyEditor || !StructuredPropertyEditor.class.isAssignableFrom(propertyEditor.getClass())) {
                        continue;
                    }

                    StructuredPropertyEditor structuredEditor = (StructuredPropertyEditor) propertyEditor;
                    processStructuredProperty(structuredEditor, propertyName, type, valueNames, propertyValues);
                }
            }
            else {
                if (null == editor || !StructuredPropertyEditor.class.isAssignableFrom(editor.getClass())) {
                    continue;
                }

                StructuredPropertyEditor structuredEditor = (StructuredPropertyEditor) editor;
                processStructuredProperty(structuredEditor, propertyName, type, valueNames, propertyValues);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void processStructuredProperty(StructuredPropertyEditor structuredEditor, String propertyName, Class<?> type,
            List<String> valueNames, MutablePropertyValues propertyValues) {

        List requiredFields = structuredEditor.getRequiredFields();
        List<String> fields = new ArrayList<String>();
        fields.addAll(requiredFields);
        fields.addAll(structuredEditor.getOptionalFields());

        Map<String, String> fieldValues = new HashMap<String, String>();
        try {

            String firstRequiredField = null;
            for (String field : fields) {
                String fullName = propertyName + STRUCTURED_PROPERTY_SEPERATOR + field;

                // don't re-process related properties
                valueNames.remove(fullName);

                if (firstRequiredField != null) {
                    continue;
                }

                PropertyValue partialStructValue = propertyValues.getPropertyValue(fullName);
                if (partialStructValue == null) {
                    if (requiredFields.contains(field)) {
                        firstRequiredField = field;
                    }
                }
                else {
                    fieldValues.put(field, getStringValue(partialStructValue));
                }
            }

            // set to null since it either won't be created because of problem, or will be overwritten
            propertyValues.removePropertyValue(propertyName);

            if (firstRequiredField != null) {
                throw new MissingPropertyException(
                        "Required structured property is missing [" + firstRequiredField + "]");
            }

            try {
                Object value = structuredEditor.assemble(type, fieldValues);
                for (String field : fields) {
                    PropertyValue partialStructValue = propertyValues.getPropertyValue(
                            propertyName + STRUCTURED_PROPERTY_SEPERATOR + field);
                    if (null != partialStructValue) {
                        partialStructValue.setConvertedValue(getStringValue(partialStructValue));
                    }
                }
                propertyValues.addPropertyValue(new PropertyValue(propertyName, value));
            }
            catch (IllegalArgumentException e) {
                LOG.warn("Unable to parse structured date from request for date [" + propertyName + "]", e);
            }
        }
        catch (InvalidPropertyException ignored) {
            // ignored
        }
    }

    private void mapPropertyValues(PropertyValue[] pvs,
            Map<String, PropertyValue> valuesByName, List<String> valueNames) {
        for (PropertyValue pv : pvs) {
            String propertyName = pv.getName();
            if (!isAllowed(propertyName)) continue;
            valuesByName.put(propertyName, pv);
            valueNames.add(propertyName);
        }
    }

    private String getStringValue(PropertyValue yearProperty) {
        Object value = yearProperty.getValue();
        if (value == null) return null;

        if (value.getClass().isArray()) {
            return ((String[])value)[0];
        }

        return (String)value;
    }

    /**
     * This overrides the method from WebDataBinder to allow for nested checkbox handling, so property paths such as
     * a._b will result in the boolean b on object a getting set to false.
     */
    @Override protected void checkFieldMarkers(MutablePropertyValues mpvs) {
        if (getFieldMarkerPrefix() == null) {
            return;
        }

        String fieldMarkerPrefix = getFieldMarkerPrefix();
        PropertyValue[] pvArray = mpvs.getPropertyValues();
        for (PropertyValue pv : pvArray) {
            // start of variation from superclass method
            if (propertyStartsWithFieldMarkerPrefix(pv, fieldMarkerPrefix)) {
                String field = stripFieldMarkerPrefix(pv.getName(), fieldMarkerPrefix);
                // end of variation from superclass method
                if (getPropertyAccessor().isWritableProperty(field) && !mpvs.contains(field)) {
                    Class<?> fieldType = getPropertyAccessor().getPropertyType(field);
                    mpvs.add(field, getEmptyValue(field, fieldType));
                }
                mpvs.removePropertyValue(pv);
            }
        }
    }

    private boolean propertyStartsWithFieldMarkerPrefix(PropertyValue pv, String fieldMarkerPrefix) {
        String propertyName = pv.getName().indexOf(PATH_SEPARATOR) > -1 ? StringUtils.substringAfterLast(pv.getName(), ".") : pv.getName();
        return propertyName.startsWith(fieldMarkerPrefix);
    }

    private String stripFieldMarkerPrefix(String path, String fieldMarkerPrefix) {
        String[] pathElements = StringUtils.split(path, PATH_SEPARATOR);
        for (int i = 0; i < pathElements.length; i++) {
            if (pathElements[i].startsWith(fieldMarkerPrefix)) {
                pathElements[i] = pathElements[i].substring(fieldMarkerPrefix.length());
            }
        }
        return StringUtils.join(pathElements, PATH_SEPARATOR);
    }
}
