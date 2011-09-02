package grails.web;

public interface UrlConverter {
    String BEAN_NAME = "grailsUrlConverter";

    String toUrlElement(String propertyOrClassName);
}
