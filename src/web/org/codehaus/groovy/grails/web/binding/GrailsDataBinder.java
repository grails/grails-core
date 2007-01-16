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

import groovy.lang.GroovyObject;
import groovy.lang.GroovyRuntimeException;
import groovy.lang.MetaClass;
import groovy.lang.MissingMethodException;

import java.net.URI;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.metaclass.CreateDynamicMethod;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.springframework.beans.ConfigurablePropertyAccessor;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.beans.propertyeditors.LocaleEditor;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.ServletRequestParameterPropertyValues;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.support.ByteArrayMultipartFileEditor;
import org.springframework.web.multipart.support.StringMultipartFileEditor;
import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * A data binder that handles binding dates that are specified with a "struct"-like syntax in request parameters.
 * For example for a set of fields defined as:
 *
 * <code>
     * <input type="hidden" name="myDate_year" value="2005" />
     * <input type="hidden" name="myDate_month" value="6" />
     * <input type="hidden" name="myDate_day" value="12" />
     * <input type="hidden" name="myDate_hour" value="13" />
     * <input type="hidden" name="myDate_minute" value="45" />
 * </code>
 *
 * This would set the property "myDate" of type java.util.Date with the specified values.
 *
 * @author Graeme Rocher
 * @since 05-Jan-2006
 */
public class GrailsDataBinder extends ServletRequestDataBinder {
    private static final Log LOG = LogFactory.getLog(GrailsDataBinder.class);
	private static ConfigurablePropertyAccessor bean;

    public static final String[] GROOVY_DISALLOWED = new String[] { "metaClass", "properties" };
    public static final String[] DOMAINCLASS_DISALLOWED = new String[] { "id", "version" };
    public static final String[] GROOVY_DOMAINCLASS_DISALLOWED = new String[] { "metaClass", "properties", "id", "version" };

    /**
     * Create a new GrailsDataBinder instance.
     *
     * @param target     target object to bind onto
     * @param objectName objectName of the target object
     */
    public GrailsDataBinder(Object target, String objectName) {
        super(target, objectName);
        
        bean = ((BeanPropertyBindingResult)super.getBindingResult()).getPropertyAccessor();

        String[] disallowed = null;
        if (GrailsClassUtils.isDomainClass(target.getClass())) {
            if (target instanceof GroovyObject) {
                disallowed = GROOVY_DOMAINCLASS_DISALLOWED;
            } else {
                disallowed = DOMAINCLASS_DISALLOWED;
            }
        } else if (target instanceof GroovyObject) {
            disallowed = GROOVY_DISALLOWED;
        }
        if (disallowed != null) {
            setDisallowedFields(disallowed);
        }
    }

    /**
     * Utility method for creating a GrailsDataBinder instance
     *
     * @param target
     * @param objectName
     * @param request
     * @return A GrailsDataBinder instance
     */
    public static GrailsDataBinder createBinder(Object target, String objectName, HttpServletRequest request) {
        GrailsDataBinder binder = createBinder(target,objectName);
        binder.registerCustomEditor( Date.class, new CustomDateEditor(DateFormat.getDateInstance( DateFormat.SHORT, RequestContextUtils.getLocale(request) ),true) );
        return binder;
    }

    /**
     * Utility method for creating a GrailsDataBinder instance
     *
     * @param target
     * @param objectName
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
        
        
		return binder;
    }

    public void bind(ServletRequest request) {
        MutablePropertyValues mpvs = new ServletRequestParameterPropertyValues(request);

        checkStructuredDateDefinitions(request,mpvs);
        autoCreateIfPossible(mpvs);
        bindAssociations(mpvs); 
        if(request instanceof HttpServletRequestWrapper) {
            request = ((HttpServletRequestWrapper)request).getRequest();
        }
        
		if (request instanceof MultipartHttpServletRequest) {
			MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
			bindMultipartFiles(multipartRequest.getFileMap(), mpvs);
		}

        super.doBind(mpvs);
    }

    /**
     * Method that auto-creates the a type if it is null and is possible to auto-create
     *
     * @param mpvs A MutablePropertyValues instance
     */
    private void autoCreateIfPossible(MutablePropertyValues mpvs) {
        PropertyValue[] pvs = mpvs.getPropertyValues();
        for (int i = 0; i < pvs.length; i++) {
            PropertyValue pv = pvs[i];

            String propertyName = pv.getName();
            //super.
            
            if(propertyName.indexOf('.') > -1) {
                propertyName = propertyName.split("\\.")[0];
            }
            Class type = bean.getPropertyType(propertyName);
            LOG.debug("Checking if auto-create is possible for property ["+propertyName+"] and type ["+type+"]");
            if(type != null) {
                if(GroovyObject.class.isAssignableFrom(type)) {
                    if(bean.getPropertyValue(propertyName) == null) {
                        if(bean.isWritableProperty(propertyName)) {
                            try {
                                MetaClass mc = InvokerHelper
                                                    .getInstance()
                                                    .getMetaRegistry()
                                                    .getMetaClass(type);
                                if(mc!=null) {
                                    Object created = mc.invokeStaticMethod(type.getName(),CreateDynamicMethod.METHOD_NAME, new Object[0]);
                                    bean.setPropertyValue(propertyName,created);
                                }
                            }
                            catch(MissingMethodException mme) {
                                LOG.warn("Unable to auto-create type, 'create' method not found");
                            }
                            catch(GroovyRuntimeException gre) {
                                LOG.warn("Unable to auto-create type, Groovy Runtime error: " + gre.getMessage(),gre) ;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Interrogates the specified properties looking for properites that represent associations to other 
     * classes (e.g., 'author.id').  If such a property is found, this method attempts to load the specified 
     * instance of the association (by ID) and set it on the target object.  
     * 
     * @param mpvs the <code>MutablePropertyValues</code> object holding the parameters from the request
     */
    private void bindAssociations(MutablePropertyValues mpvs) {
        PropertyValue[] pvs = mpvs.getPropertyValues();
        for (int i = 0; i < pvs.length; i++) {
            PropertyValue pv = pvs[i];

            String propertyName = pv.getName();
            int index = propertyName.indexOf(".id");
            if (index > -1) {
                propertyName = propertyName.substring(0, index);
                if (bean.isReadableProperty(propertyName) && bean.isWritableProperty(propertyName)) {
                    Class type = bean.getPropertyType(propertyName);

                    // In order to load the association instance using InvokerHelper below, we need to 
                    // temporarily change this thread's ClassLoader to use the Grails ClassLoader.
                    // (Otherwise, we'll get a ClassNotFoundException.)
                    ClassLoader currentClassLoader = getClass().getClassLoader();
                    ClassLoader grailsClassLoader = getTarget().getClass().getClassLoader();
                    try {
                        Thread.currentThread().setContextClassLoader(grailsClassLoader);
						Object persisted = null;
                        	
                       	persisted = InvokerHelper.invokeStaticMethod(type, "get", pv.getValue());
                        
                        if (persisted != null) {
                            bean.setPropertyValue(propertyName, persisted);
                        }
                    } finally {
                        Thread.currentThread().setContextClassLoader(currentClassLoader);
                    }
                }
            }
        }
    }    

    private void checkStructuredDateDefinitions(ServletRequest request, MutablePropertyValues mpvs) {

        PropertyValue[] pvs = mpvs.getPropertyValues();
        for (int i = 0; i < pvs.length; i++) {
            PropertyValue pv = pvs[i];

            try {
                String propertyName = pv.getName();
                Class type = bean.getPropertyType(propertyName);
                // if its a date check that it hasn't got structured parameters in the request
                // this is used as an alternative to specifying the date format
                if(type == Date.class || type == Calendar.class) {
                    try {
                           // The request will always include the year value
                        int year = Integer.parseInt(request.getParameter(propertyName + "_year"));

                        // The request may not include the other date values, so be prepared to use the
                        // default values.  Default values --> month = January; day = 1st day of the month;
                        // hour = 00; minute = 00.
                        int month = Integer.parseInt(getParameterValue(request, propertyName + "_month","1"));
                        int day = Integer.parseInt(getParameterValue(request, propertyName + "_day","1"));
                        int hour = Integer.parseInt(getParameterValue(request, propertyName + "_hour","0"));
                        int minute = Integer.parseInt(getParameterValue(request, propertyName + "_minute","0"));

                        Calendar c = new GregorianCalendar(year,month - 1,day,hour,minute);
                        if(type == Date.class)
                            mpvs.setPropertyValueAt(new PropertyValue(propertyName,c.getTime()),i);
                        else
                            mpvs.setPropertyValueAt(new PropertyValue(propertyName,c),i);
                    }
                    catch(NumberFormatException nfe) {
                         LOG.warn("Unable to parse structured date from request for date ["+propertyName+"]",nfe);
                    }
                }
            }
            catch(InvalidPropertyException ipe) {
                // ignore
            }
        }
    }

    /**
     * Returns the value of the specified parameter from the specified request, or the specified default
     * value (if the parameter is not in the request).
     *
     * @param request the request from which to extract the parameter
     * @param propertyName the key used to fetch the parameter from the request
     * @param defaultPropertyValue the value to return if the parameter is not in the request
     * @return the requested value
     */
    private String getParameterValue(ServletRequest request, String propertyName, String defaultPropertyValue) {
        String parameterValue = request.getParameter(propertyName);
        return parameterValue != null ? parameterValue : defaultPropertyValue;
    }
}
