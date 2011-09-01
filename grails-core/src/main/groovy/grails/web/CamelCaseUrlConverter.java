package grails.web;

import org.apache.commons.lang.StringUtils;

import grails.util.GrailsNameUtils;

public class CamelCaseUrlConverter implements UrlConverter {

    public String toUrlElement(String propertyOrClassName) {
        if(StringUtils.isBlank(propertyOrClassName)) {
            return propertyOrClassName;
        }
        return GrailsNameUtils.getPropertyNameRepresentation(propertyOrClassName);
    }

}
