package grails.web;

import java.util.Locale;

import org.apache.commons.lang.StringUtils;

import grails.util.GrailsNameUtils;

public class CamelCaseUrlConverter implements UrlConverter {

    public String toUrlElement(String propertyOrClassName) {
        if(StringUtils.isBlank(propertyOrClassName)) {
            return propertyOrClassName;
        }
        if (propertyOrClassName.length() > 1 && Character.isUpperCase(propertyOrClassName.charAt(0)) &&
                Character.isUpperCase(propertyOrClassName.charAt(1))) {
            return propertyOrClassName;
        }

        String uriElement = propertyOrClassName.substring(0,1).toLowerCase(Locale.ENGLISH) + propertyOrClassName.substring(1);
        return uriElement;
    }

}
