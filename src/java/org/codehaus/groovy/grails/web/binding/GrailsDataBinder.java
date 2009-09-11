/* Copyright 2004-2005 the original author or authors.
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

import grails.util.GrailsNameUtils;
import groovy.lang.*;
import org.apache.commons.collections.Factory;
import org.apache.commons.collections.list.LazyList;
import org.apache.commons.collections.set.ListOrderedSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.grails.commons.*;
import org.codehaus.groovy.grails.commons.metaclass.CreateDynamicMethod;
import org.codehaus.groovy.grails.validation.ConstrainedProperty;
import org.codehaus.groovy.grails.web.context.ServletContextHolder;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.springframework.beans.*;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.beans.propertyeditors.LocaleEditor;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.ServletRequestParameterPropertyValues;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.support.ByteArrayMultipartFileEditor;
import org.springframework.web.multipart.support.StringMultipartFileEditor;
import org.springframework.web.servlet.support.RequestContextUtils;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.beans.PropertyEditor;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;

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
 * @since 05-Jan-2006
 */
public class GrailsDataBinder extends ServletRequestDataBinder {
    private static final Log LOG = LogFactory.getLog(GrailsDataBinder.class);

    protected BeanWrapper bean;

    public static final String[] GROOVY_DISALLOWED = new String[] { "metaClass", "properties" };
    public static final String[] DOMAINCLASS_DISALLOWED = new String[] { "id", "version" };
    public static final String[] GROOVY_DOMAINCLASS_DISALLOWED = new String[] { "metaClass", "properties", "id", "version" };
    public static final String   NULL_ASSOCIATION = "null";
    private static final String PREFIX_SEPERATOR = ".";
    private static final String[] ALL_OTHER_FIELDS_ALLOWED_BY_DEFAULT = new String[0];
    private static final String CONSTRAINTS_PROPERTY = "constraints";
    private static final String BLANK = "";
    private static final String STRUCTURED_PROPERTY_SEPERATOR = "_";
    private static final char PATH_SEPARATOR = '.';
    private static final String IDENTIFIER_SUFFIX = ".id";
    private List transients = Collections.EMPTY_LIST;
    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.S";

    /**
     * Create a new GrailsDataBinder instance.
     *
     * @param target     target object to bind onto
     * @param objectName objectName of the target object
     */
    public GrailsDataBinder(Object target, String objectName) {
        super(target, objectName);

        bean = (BeanWrapper)((BeanPropertyBindingResult)super.getBindingResult()).getPropertyAccessor();

        Object tmpTransients = GrailsClassUtils.getStaticPropertyValue(bean.getWrappedClass(), GrailsDomainClassProperty.TRANSIENT);
        if(tmpTransients instanceof List) {
            this.transients = (List) tmpTransients;
        }
        String[] disallowed = new String[0];
        GrailsApplication grailsApplication = ApplicationHolder.getApplication();
        if (grailsApplication!=null && grailsApplication.isArtefactOfType(DomainClassArtefactHandler.TYPE, target.getClass())) {
            if (target instanceof GroovyObject) {
                disallowed = GROOVY_DOMAINCLASS_DISALLOWED;
            }
            else {
                disallowed = DOMAINCLASS_DISALLOWED;
            }
        }
        else if (target instanceof GroovyObject) {
            disallowed = GROOVY_DISALLOWED;
        }
        setDisallowedFields(disallowed);
        setAllowedFields(ALL_OTHER_FIELDS_ALLOWED_BY_DEFAULT);
        setIgnoreInvalidFields(true);

    }

    /**
     * Collects all PropertyEditorRegistrars in the application context and
     * calls them to register their custom editors
     *
     * @param registry The PropertyEditorRegistry instance
     */
    private static void registerCustomEditors(PropertyEditorRegistry registry) {
        final ServletContext servletContext = ServletContextHolder.getServletContext();
        if(servletContext != null) {
            WebApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(servletContext);
            if(context != null) {
                Map editors = context.getBeansOfType(PropertyEditorRegistrar.class);
                for (Object o : editors.entrySet()) {
                    PropertyEditorRegistrar editorRegistrar = (PropertyEditorRegistrar) ((Map.Entry) o).getValue();
                    editorRegistrar.registerCustomEditors(registry);
                }
            }
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
        GrailsDataBinder binder = createBinder(target,objectName);
        Locale locale = RequestContextUtils.getLocale(request);
        registerCustomEditors(binder, locale);


        return binder;
    }


    /**
     * Registers all known
     *
     * @param registry
     * @param locale
     */
    public static void registerCustomEditors(PropertyEditorRegistry registry, Locale locale) {
        // Formatters for the different number types.
        NumberFormat floatFormat = NumberFormat.getInstance(locale);
        NumberFormat integerFormat = NumberFormat.getIntegerInstance(locale);

        DateFormat dateFormat = new SimpleDateFormat(DEFAULT_DATE_FORMAT, locale);

        registry.registerCustomEditor( Date.class, new CustomDateEditor(dateFormat,true) );
        registry.registerCustomEditor( BigDecimal.class, new CustomNumberEditor(BigDecimal.class, floatFormat, true));
        registry.registerCustomEditor( BigInteger.class, new CustomNumberEditor(BigInteger.class, floatFormat, true));
        registry.registerCustomEditor( Double.class, new CustomNumberEditor(Double.class, floatFormat, true));
        registry.registerCustomEditor( double.class, new CustomNumberEditor(Double.class, floatFormat, true));
        registry.registerCustomEditor( Float.class, new CustomNumberEditor(Float.class, floatFormat, true));
        registry.registerCustomEditor( float.class, new CustomNumberEditor(Float.class, floatFormat, true));
        registry.registerCustomEditor( Long.class, new CustomNumberEditor(Long.class, integerFormat, true));
        registry.registerCustomEditor( long.class, new CustomNumberEditor(Long.class, integerFormat, true));
        registry.registerCustomEditor( Integer.class, new CustomNumberEditor(Integer.class, integerFormat, true));
        registry.registerCustomEditor( int.class, new CustomNumberEditor(Integer.class, integerFormat, true));
        registry.registerCustomEditor( Short.class, new CustomNumberEditor(Short.class, integerFormat, true));
        registry.registerCustomEditor( short.class, new CustomNumberEditor(Short.class, integerFormat, true));
        registry.registerCustomEditor( Date.class, new StructuredDateEditor(dateFormat,true));
        registry.registerCustomEditor( Calendar.class, new StructuredDateEditor(dateFormat,true));

        registerCustomEditors(registry);
    }

    /**
     * Utility method for creating a GrailsDataBinder instance
     *
     * @param target The target object to bind to
     * @param objectName The name of the object
     * @return A GrailsDataBinder instance
     */
    public static GrailsDataBinder createBinder(Object target, String objectName) {
        GrailsDataBinder binder = new GrailsDataBinder(target,objectName);
        binder.registerCustomEditor( byte[].class, new ByteArrayMultipartFileEditor());
        binder.registerCustomEditor( String.class, new StringMultipartFileEditor());
        binder.registerCustomEditor( Currency.class, new CurrencyEditor());
        binder.registerCustomEditor( Locale.class, new LocaleEditor());
        binder.registerCustomEditor( TimeZone.class, new TimeZoneEditor());
        binder.registerCustomEditor( URI.class, new UriEditor());

        registerCustomEditors(binder);

		return binder;
    }

    public void bind(PropertyValues propertyValues) {
        bind(propertyValues, null);
    }

    /**
     * Binds from a GrailsParameterMap object
     *
     * @param params The GrailsParameterMap object
     */
    public void bind(GrailsParameterMap params) {
        bind(params,null);
    }

    public void bind(GrailsParameterMap params, String prefix) {
        Map paramsMap = params;
        if(prefix != null) {
            Object o = params.get(prefix);
            if(o instanceof Map) paramsMap = (Map) o;
        }
        bindWithRequestAndPropertyValues(params.getRequest(), new MutablePropertyValues(paramsMap));
    }


    public void bind(PropertyValues propertyValues, String prefix) {
        PropertyValues values = filterPropertyValues(propertyValues, prefix);
        if(propertyValues instanceof MutablePropertyValues) {
            MutablePropertyValues mutablePropertyValues = (MutablePropertyValues) propertyValues;
            preProcessMutablePropertyValues(mutablePropertyValues);
        }
        super.bind(values);
    }

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
        preProcessMutablePropertyValues(mpvs);

        if (request instanceof MultipartHttpServletRequest) {
			MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
			bindMultipartFiles(multipartRequest.getFileMap(), mpvs);
		}
        doBind(mpvs);
    }

    private void preProcessMutablePropertyValues(MutablePropertyValues mpvs) {
        checkStructuredProperties(mpvs);
        autoCreateIfPossible(mpvs);
        bindAssociations(mpvs);
    }

    protected void doBind(MutablePropertyValues mpvs) {
        filterNestedParameterMaps(mpvs);
        filterBlankValuesWhenTargetIsNullable(mpvs);
        super.doBind(mpvs);
    }

    private void filterBlankValuesWhenTargetIsNullable(MutablePropertyValues mpvs) {
        Object target = getTarget();
        MetaClass mc = GroovySystem.getMetaClassRegistry().getMetaClass(target.getClass());
        if(mc.hasProperty(target, CONSTRAINTS_PROPERTY) != null) {
            Map constrainedProperties = (Map)mc.getProperty(target, CONSTRAINTS_PROPERTY);
            PropertyValue[] valueArray = mpvs.getPropertyValues();
            for (PropertyValue propertyValue : valueArray) {
                ConstrainedProperty cp = getConstrainedPropertyForPropertyValue(constrainedProperties, propertyValue);
                if (shouldNullifyBlankString(propertyValue, cp)) {
                    propertyValue.setConvertedValue(null);
                }
            }
        }
    }

    private ConstrainedProperty getConstrainedPropertyForPropertyValue(Map constrainedProperties, PropertyValue propertyValue) {

        final String propertyName = propertyValue.getName();
        if(propertyName.indexOf(PATH_SEPARATOR) > -1) {
            String[] propertyNames = propertyName.split("\\.");
            Object target = getTarget();
            Object value = getPropertyValueForPath(target, propertyNames);
            if(value != null) {
                MetaClass mc = GroovySystem.getMetaClassRegistry().getMetaClass(value.getClass());
                if(mc.hasProperty(value, CONSTRAINTS_PROPERTY) != null) {
                    Map nestedConstrainedProperties = (Map)mc.getProperty(value, CONSTRAINTS_PROPERTY);
                    return (ConstrainedProperty)nestedConstrainedProperties.get(propertyNames[propertyNames.length-1]);
                }
            }

        }
        else {
            return (ConstrainedProperty)constrainedProperties.get(propertyName);
        }
        return null;
    }

    private Object getPropertyValueForPath(Object target, String[] propertyNames) {
        BeanWrapper bean = new BeanWrapperImpl(target);
        Object obj = target;
        for (int i = 0; i < propertyNames.length-1; i++) {

            String propertyName = propertyNames[i];
            if(bean.isReadableProperty(propertyName)) {
                obj = bean.getPropertyValue(propertyName);
                if(obj == null) break;
                bean = new BeanWrapperImpl(obj);
            }
        }

        return obj;
    }

    private boolean shouldNullifyBlankString(PropertyValue propertyValue, ConstrainedProperty cp) {
        return cp!= null && cp.isNullable() && BLANK.equals(propertyValue.getValue());
    }

    private void filterNestedParameterMaps(MutablePropertyValues mpvs) {
        PropertyValue[] values = mpvs.getPropertyValues();
        for (PropertyValue pv : values) {
            if (pv.getValue() instanceof GrailsParameterMap) {
                mpvs.removePropertyValue(pv);
            }
        }
    }

    private PropertyValues filterPropertyValues(PropertyValues propertyValues, String prefix) {
        if(prefix == null || prefix.length() == 0) return propertyValues;
        PropertyValue[] valueArray = propertyValues.getPropertyValues();
        MutablePropertyValues newValues = new MutablePropertyValues();
        for (PropertyValue propertyValue : valueArray) {
            if (propertyValue.getName().startsWith(prefix + PREFIX_SEPERATOR)) {
                newValues.addPropertyValue(propertyValue.getName().replaceFirst(prefix + PREFIX_SEPERATOR, ""), propertyValue.getValue());
            }
        }
        return newValues;
    }

    /**
     * Method that auto-creates the a type if it is null and is possible to auto-create
     *
     * @param mpvs A MutablePropertyValues instance
     */
    protected void autoCreateIfPossible(MutablePropertyValues mpvs) {
        PropertyValue[] pvs = mpvs.getPropertyValues();
        for (PropertyValue pv : pvs) {
            String propertyName = pv.getName();
            //super.

            if (propertyName.indexOf(PATH_SEPARATOR) > -1) {
                String[] propertyNames = propertyName.split("\\.");
                BeanWrapper currentBean = bean;

                for (String name : propertyNames) {
                    Object created = autoCreatePropertyIfPossible(currentBean, name, pv.getValue());
                    if (created != null)
                        currentBean = new BeanWrapperImpl(created);
                    else
                        break;
                }

            }
            else {

                autoCreatePropertyIfPossible(bean, propertyName, pv.getValue());
            }
        }
    }

    private Object autoCreatePropertyIfPossible(BeanWrapper bean, String propertyName, Object propertyValue) {

        propertyName = PropertyAccessorUtils.canonicalPropertyName(propertyName);
        int currentKeyStart = propertyName.indexOf(PropertyAccessor.PROPERTY_KEY_PREFIX_CHAR);
        int currentKeyEnd = propertyName.indexOf(PropertyAccessor.PROPERTY_KEY_SUFFIX_CHAR);
        String propertyNameWithIndex = propertyName;
        if(currentKeyStart>-1) {
            propertyName = propertyName.substring(0, currentKeyStart);
        }


        Class type = bean.getPropertyType(propertyName);
        Object val = bean.isReadableProperty(propertyName) ? bean.getPropertyValue(propertyName) : null;
        
        LOG.debug("Checking if auto-create is possible for property ["+propertyName+"] and type ["+type+"]");
        if(type != null && val == null && DomainClassArtefactHandler.isDomainClass(type)) {
            if(!shouldPropertyValueSkipAutoCreate(propertyValue) && isNullAndWritableProperty(bean, propertyName)) {

                Object created = autoInstantiateDomainInstance(type);

                if(created!=null)  {
                    val = created;
                    bean.setPropertyValue(propertyName,created);
                }
            }
        }
        else {
            final Object beanInstance = bean.getWrappedInstance();
            if(type!= null && Collection.class.isAssignableFrom(type)) {
               Collection c;
               final Class referencedType = getReferencedTypeForCollection(propertyName, beanInstance);

               if(isNullAndWritableProperty(bean, propertyName)) {
                   c = decorateCollectionForDomainAssociation(GrailsClassUtils.createConcreteCollection(type), referencedType);
               }
                else {
                   c = decorateCollectionForDomainAssociation((Collection) bean.getPropertyValue(propertyName), referencedType);
               }
               bean.setPropertyValue(propertyName, c);
               val = c;


               if(c!= null && currentKeyStart > -1 && currentKeyEnd > -1) {
                   String indexString = propertyNameWithIndex.substring(currentKeyStart+1, currentKeyEnd);
                   int index = Integer.parseInt(indexString);


                   if(DomainClassArtefactHandler.isDomainClass(referencedType)) {
                       Object instance = findIndexedValue(c, index);
                       if(instance != null) {
                           val = instance;
                       }
                       else {
                           instance = autoInstantiateDomainInstance(referencedType);
                           if(instance !=null) {
                               val = instance;
                               if(index  == c.size()) {
                                   addAssociationToTarget(propertyName, beanInstance, instance);
                               }
                               else if(index > c.size()) {
                                   while(index > c.size()) {
                                       addAssociationToTarget(propertyName, beanInstance, autoInstantiateDomainInstance(referencedType));
                                   }

                                   addAssociationToTarget(propertyName, beanInstance, instance);
                               }
                           }
                       }
                   }

               }

            }
            else if(type!=null && Map.class.isAssignableFrom(type)) {
                Map map;

                if(isNullAndWritableProperty(bean, propertyName)) {
                    map = new HashMap();
                    bean.setPropertyValue(propertyName,map);
                }
                else {
                    map = (Map) bean.getPropertyValue(propertyName);
                }
                val = map;
                bean.setPropertyValue(propertyName, val);

                if(currentKeyStart > -1 && currentKeyEnd > -1) {
                    String indexString = propertyNameWithIndex.substring(currentKeyStart+1, currentKeyEnd);
                    Class referencedType = getReferencedTypeForCollection(propertyName, beanInstance);
                    if(DomainClassArtefactHandler.isDomainClass(referencedType)) {
                        final Object domainInstance = autoInstantiateDomainInstance(referencedType);
                        val = domainInstance;
                        map.put(indexString, domainInstance);
                    }
                }
            }
        }

        return val;
    }

    private boolean shouldPropertyValueSkipAutoCreate(Object propertyValue) {
        return (propertyValue instanceof Map) || ((propertyValue instanceof String) && StringUtils.isBlank((String) propertyValue));
    }

    private Collection decorateCollectionForDomainAssociation(Collection c, final Class referencedType) {
        if(canDecorateWithLazyList(c, referencedType)) {
            c = LazyList.decorate((List)c, new Factory() {
                public Object create() {
                    return autoInstantiateDomainInstance(referencedType);
                }
            });
        }
        else if(canDecorateWithListOrderedSet(c, referencedType)) {
            c = ListOrderedSet.decorate((Set) c);
        }
        return c;
    }

    private boolean canDecorateWithListOrderedSet(Collection c, Class referencedType) {
        return (c instanceof Set) && !(c instanceof ListOrderedSet) && !(c instanceof SortedSet) && DomainClassArtefactHandler.isDomainClass(referencedType);
    }

    private boolean canDecorateWithLazyList(Collection c, Class referencedType) {
        return (c instanceof List) && !(c instanceof LazyList) && DomainClassArtefactHandler.isDomainClass(referencedType);
    }

    private Object findIndexedValue(Collection c, int index) {
        if(index < c.size()) {
            if(c instanceof List) {
                return ((List)c).get(index);
            }
            else {
                int j =0;
                for (Iterator i = c.iterator(); i.hasNext();j++) {
                    Object o = i.next();
                    if(j == index) return o;
                    
                }
            }
        }
        return null;
    }

    private Object autoInstantiateDomainInstance(Class type) {
        Object created = null;
        try {
            MetaClass mc = GroovySystem.getMetaClassRegistry()
                    .getMetaClass(type);
            if(mc!=null) {
                created = mc.invokeStaticMethod(type, CreateDynamicMethod.METHOD_NAME, new Object[0]);
            }
        }
        catch(MissingMethodException mme) {
            LOG.warn("Unable to auto-create type, 'create' method not found");
        }
        catch(GroovyRuntimeException gre) {
            LOG.warn("Unable to auto-create type, Groovy Runtime error: " + gre.getMessage(),gre) ;
        }
        return created;
    }

    private boolean isNullAndWritableProperty(ConfigurablePropertyAccessor bean, String propertyName) {
        return bean.getPropertyValue(propertyName) == null && bean.isWritableProperty(propertyName);
    }

    /**
     * Interrogates the specified properties looking for properites that represent associations to other 
     * classes (e.g., 'author.id').  If such a property is found, this method attempts to load the specified 
     * instance of the association (by ID) and set it on the target object.  
     * 
     * @param mpvs the <code>MutablePropertyValues</code> object holding the parameters from the request
     */
    protected void bindAssociations(MutablePropertyValues mpvs) {
        PropertyValue[] pvs = mpvs.getPropertyValues();
        for (PropertyValue pv : pvs) {
            String propertyName = pv.getName();

            if (propertyName.endsWith(IDENTIFIER_SUFFIX)) {
                propertyName = propertyName.substring(0, propertyName.length() - 3);
                if (isReadableAndPersistent(propertyName) && bean.isWritableProperty(propertyName)) {
                    if (NULL_ASSOCIATION.equals(pv.getValue())) {
                        bean.setPropertyValue(propertyName, null);
                        mpvs.removePropertyValue(pv);
                    }
                    else {
                        Class type = bean.getPropertyType(propertyName);

                        Object persisted = getPersistentInstance(type, pv.getValue());
                        if (persisted != null) {
                            bean.setPropertyValue(propertyName, persisted);
                        }

                    }
                }
            }
            else {
                if (isReadableAndPersistent(propertyName)) {
                    Class type = bean.getPropertyType(propertyName);
                    if (Collection.class.isAssignableFrom(type)) {
                        bindCollectionAssociation(mpvs, pv);
                    }
                }
            }
        }
    }

    private boolean isReadableAndPersistent(String propertyName) {        
        return bean.isReadableProperty(propertyName) && !transients.contains(propertyName);
    }

    private Object getPersistentInstance(Class type, Object id) {
        Object persisted;// In order to load the association instance using InvokerHelper below, we need to
        // temporarily change this thread's ClassLoader to use the Grails ClassLoader.
        // (Otherwise, we'll get a ClassNotFoundException.)
        ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader grailsClassLoader = getTarget().getClass().getClassLoader();
        try {
            try {
                Thread.currentThread().setContextClassLoader(grailsClassLoader);
            }
            catch (java.security.AccessControlException e) {
                // container doesn't allow, probably related to WAR deployment on AppEngine. proceed.
            }


            try {
                persisted = InvokerHelper.invokeStaticMethod(type, "get", id);
            }
            catch (MissingMethodException e) {
                return null; // GORM not installed, continue to operate as normal
            }

        }
        finally {
            try {
                Thread.currentThread().setContextClassLoader(currentClassLoader);
            }
            catch (java.security.AccessControlException e) {
                // container doesn't allow, probably related to WAR deployment on AppEngine. proceed.
            }            
        }
        return persisted;
    }

    private void bindCollectionAssociation(MutablePropertyValues mpvs, PropertyValue pv) {
        Object v = pv.getValue();

        Collection collection = (Collection) bean.getPropertyValue(pv.getName());
        collection.clear();
        final Class associatedType = getReferencedTypeForCollection(pv.getName(), getTarget());
        final boolean isArray = v != null && v.getClass().isArray();
        if(isDomainAssociation(associatedType)) {
            if(isArray) {

                Object[] identifiers = (Object[])v;
                for (Object id : identifiers) {
                    if (id != null) {
                        associateObjectForId(pv, id,associatedType);
                    }
                }

                mpvs.removePropertyValue(pv);
            }
            else if(v!=null && (v instanceof String)) {
                associateObjectForId(pv,v, associatedType);
                mpvs.removePropertyValue(pv);
            }
        }
        else if(GrailsDomainConfigurationUtil.isBasicType(associatedType)) {
            if(isArray) {
                Object[] values = (Object[])v;
                List list = collection instanceof List ? (List)collection : null;
                for (int i = 0; i < values.length; i++) {
                    Object value = values[i];
                    try {
                        Object newValue = getTypeConverter().convertIfNecessary(value, associatedType);
                        if(list!=null) {
                            if(i>list.size()-1) {
                                list.add(i,newValue);
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
            }
        }
    }

    private void associateObjectForId(PropertyValue pv, Object id, Class associatedType) {
        final Object target = getTarget();
        final Object obj = getPersistentInstance(associatedType, id);
        addAssociationToTarget(pv.getName(), target, obj);        
    }

    private boolean isDomainAssociation(Class associatedType) {
        return associatedType != null && DomainClassArtefactHandler.isDomainClass(associatedType);
    }

    private void addAssociationToTarget(String name, Object target, Object obj) {
        if(obj!=null) {
            MetaClassRegistry reg = GroovySystem.getMetaClassRegistry();
            MetaClass mc = reg.getMetaClass(target.getClass());
            final String addMethodName = "addTo" + GrailsNameUtils.getClassNameRepresentation(name);
            mc.invokeMethod(target, addMethodName,obj);
        }
    }

    private Class getReferencedTypeForCollection(String name, Object target) {
        final GrailsApplication grailsApplication = ApplicationHolder.getApplication();
        if(grailsApplication!=null) {
            GrailsDomainClass domainClass = (GrailsDomainClass) grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, target.getClass().getName());
            if(domainClass!=null) {
                GrailsDomainClassProperty domainProperty = domainClass.getPropertyByName(name);
                if(domainProperty!=null) {
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
     * containg a "_"
     *
     * @param propertyValues
     */
    private void checkStructuredProperties(MutablePropertyValues propertyValues) {
        PropertyValue[] pvs = propertyValues.getPropertyValues();
        for (PropertyValue propertyValue : pvs) {
            if (!isStructured(propertyValue)) {
                continue;
            }
            String propertyName = getNameOf(propertyValue);
            Class type = bean.getPropertyType(propertyName);
            if (type != null) {
                PropertyEditor editor = findCustomEditor(type, propertyName);
                if (null != editor && StructuredPropertyEditor.class.isAssignableFrom(editor.getClass())) {
                    StructuredPropertyEditor structuredEditor = (StructuredPropertyEditor) editor;
                    List fields = new ArrayList();
                    fields.addAll(structuredEditor.getRequiredFields());
                    fields.addAll(structuredEditor.getOptionalFields());
                    Map<String, String> fieldValues = new HashMap<String, String>();
                    try {
                        for (Object fld : fields) {
                            String field = (String) fld;
                            PropertyValue partialStructValue = propertyValues.getPropertyValue(propertyName + STRUCTURED_PROPERTY_SEPERATOR + field);
                            if (partialStructValue == null && structuredEditor.getRequiredFields().contains(field)) {
                                throw new MissingPropertyException("Required structured property is missing [" + field + "]");
                            }
                            else if (partialStructValue == null)
                                continue;
                            fieldValues.put(field, getStringValue(partialStructValue));
                        }
                        try {
                            Object value = structuredEditor.assemble(type, fieldValues);
                            for (Object fld : fields) {
                                String field = (String) fld;
                                PropertyValue partialStructValue = propertyValues.getPropertyValue(propertyName + STRUCTURED_PROPERTY_SEPERATOR + field);
                                if (null != partialStructValue) {
                                    partialStructValue.setConvertedValue(getStringValue(partialStructValue));
                                    }
                                }
                                propertyValues.addPropertyValue(new PropertyValue(propertyName, value));
                            }
                        catch (IllegalArgumentException iae) {
                            LOG.warn("Unable to parse structured date from request for date [" + propertyName + "]", iae);
                        }
                    }
                    catch (InvalidPropertyException ipe) {
                        // ignore
                    }
                }
            }
        }
    }

    private String getStringValue(PropertyValue yearProperty) {
        Object value = yearProperty.getValue();
        if(value == null) return null;
        else if(value.getClass().isArray()) {
            return ((String[])value)[0];
        }
        return (String)value ;
    }

}
