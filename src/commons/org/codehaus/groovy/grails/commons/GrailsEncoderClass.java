package org.codehaus.groovy.grails.commons;

import groovy.lang.Closure;

/**
 * 
 * @author Jeff Brown
 * @since 0.4
 */
public interface GrailsEncoderClass extends InjectableGrailsClass {

	Closure getEncodeMethod();

	Closure getDecodeMethod();

}
