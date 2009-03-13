package org.codehaus.groovy.grails.commons;

import groovy.lang.Closure;

import java.util.ArrayList;
import java.util.List;

public class DefaultGrailsUrlMappingsClass extends AbstractGrailsClass implements GrailsUrlMappingsClass {

	public static final String URL_MAPPINGS = "UrlMappings";
	
	private static final String MAPPINGS_CLOSURE = "mappings";

    private static final String EXCLUDE_PATTERNS = "excludes";

    public DefaultGrailsUrlMappingsClass(Class clazz) {
		super(clazz, URL_MAPPINGS);
	}

	public Closure getMappingsClosure() {
		Closure result = (Closure) getPropertyOrStaticPropertyOrFieldValue(MAPPINGS_CLOSURE, Closure.class);
		if (result == null) {
			throw new RuntimeException(MAPPINGS_CLOSURE + " closure does not exists for class " +  getClazz().getName());
		}
		return result;
	}

    public List getExcludePatterns() {
        return  (List) getPropertyOrStaticPropertyOrFieldValue(EXCLUDE_PATTERNS, ArrayList.class);
    }

}
