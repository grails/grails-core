package org.grails.web.beans

import grails.databinding.DataBinder;
import grails.util.Environment
import groovy.transform.CompileStatic

import org.grails.web.binding.CompositeEditor
import org.grails.web.binding.StructuredDateEditor
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.beans.PropertyEditorRegistrar
import org.springframework.beans.PropertyEditorRegistry
import org.springframework.beans.propertyeditors.CustomDateEditor
import org.springframework.beans.propertyeditors.CustomNumberEditor
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.context.support.WebApplicationContextUtils

import javax.servlet.ServletContext
import java.text.NumberFormat
import java.text.SimpleDateFormat

/**
 * @author Graeme Rocher
 * @since 2.4
 */
@CompileStatic
class PropertyEditorRegistryUtils {

    private static final String PROPERTY_EDITOR_REGISTRARS = "org.codehaus.groovy.grails.PROPERTY_EDITOR_REGISTRARS";
    private static final String JSON_DATE_FORMAT = "yyyy-MM-dd'T'hh:mm:ss'Z'";

    /**
     * Registers all known
     *
     * @param grailsWebRequest
     * @param registry
     * @param locale
     */
    public static void registerCustomEditors(GrailsWebRequest grailsWebRequest, PropertyEditorRegistry registry, Locale locale) {
        // Formatters for the different number types.
        def floatFormat = NumberFormat.getInstance(locale)
        def integerFormat = NumberFormat.getIntegerInstance(locale)

        def dateFormat = new SimpleDateFormat(DataBinder.DEFAULT_DATE_FORMAT, locale)

        registry.registerCustomEditor(Date, new CustomDateEditor(dateFormat,true));
        registry.registerCustomEditor(BigDecimal, new CustomNumberEditor(BigDecimal.class, floatFormat, true));
        registry.registerCustomEditor(BigInteger, new CustomNumberEditor(BigInteger.class, floatFormat, true));
        registry.registerCustomEditor(Double, new CustomNumberEditor(Double.class, floatFormat, true));
        registry.registerCustomEditor(double.class, new CustomNumberEditor(Double.class, floatFormat, true));
        registry.registerCustomEditor(Float, new CustomNumberEditor(Float.class, floatFormat, true));
        registry.registerCustomEditor(float.class, new CustomNumberEditor(Float.class, floatFormat, true));
        registry.registerCustomEditor(Long, new CustomNumberEditor(Long.class, integerFormat, true));
        registry.registerCustomEditor(long.class, new CustomNumberEditor(Long.class, integerFormat, true));
        registry.registerCustomEditor(Integer, new CustomNumberEditor(Integer.class, integerFormat, true));
        registry.registerCustomEditor(int.class, new CustomNumberEditor(Integer.class, integerFormat, true));
        registry.registerCustomEditor(Short, new CustomNumberEditor(Short.class, integerFormat, true));
        registry.registerCustomEditor(short.class, new CustomNumberEditor(Short.class, integerFormat, true));
        registry.registerCustomEditor(Date, new CompositeEditor(new StructuredDateEditor(dateFormat,true), new CustomDateEditor(new SimpleDateFormat(JSON_DATE_FORMAT), true)));
        registry.registerCustomEditor(Calendar, new StructuredDateEditor(dateFormat,true));

        ServletContext servletContext = grailsWebRequest != null ? grailsWebRequest.getServletContext() : null;
        registerCustomEditorsFromContext(servletContext, registry);
    }

    /**
     * Collects all PropertyEditorRegistrars in the application context and
     * calls them to register their custom editors
     *
     * @param servletContext
     * @param registry The PropertyEditorRegistry instance
     */
    private static void registerCustomEditorsFromContext(ServletContext servletContext, PropertyEditorRegistry registry) {
        if (servletContext == null) {
            return;
        }

        WebApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(servletContext);
        if (context == null) {
            return;
        }

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
}
