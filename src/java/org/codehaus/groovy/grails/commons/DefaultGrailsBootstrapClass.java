package org.codehaus.groovy.grails.commons;

import grails.util.Environment;
import groovy.lang.Closure;

import javax.servlet.ServletContext;

public class DefaultGrailsBootstrapClass extends AbstractGrailsClass implements GrailsBootstrapClass {

	
	public static final String BOOT_STRAP = "BootStrap";
	
	private static final String INIT_CLOSURE = "init";
	private static final String DESTROY_CLOSURE = "destroy";
	private static final Closure BLANK_CLOSURE = new Closure(DefaultGrailsBootstrapClass.class) {
		public Object call(Object[] args) {
			return null;
		}		
	};

	
	public DefaultGrailsBootstrapClass(Class clazz) {
		super(clazz, BOOT_STRAP);
	}

	public Closure getInitClosure() {
		Object obj = getPropertyValueObject(INIT_CLOSURE);
		if(obj instanceof Closure) {
			return (Closure)obj;
		}
		return BLANK_CLOSURE;
	}

	public Closure getDestroyClosure() {
		Object obj = getPropertyValueObject(DESTROY_CLOSURE);
		if(obj instanceof Closure) {
			return (Closure)obj;
		}
		return BLANK_CLOSURE;
	}

	public void callInit(ServletContext servletContext) {
		Closure init = getInitClosure();
        if(init != null) {
            init = init.curry(new Object[]{servletContext});
            Environment.executeForCurrentEnvironment(init);
        }
	}

	public void callDestroy() {
		Closure destroy = getDestroyClosure();
        if(destroy!=null) {
            Environment.executeForCurrentEnvironment(destroy);
        }
	}
}
