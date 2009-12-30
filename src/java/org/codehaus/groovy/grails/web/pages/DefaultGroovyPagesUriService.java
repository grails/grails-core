package org.codehaus.groovy.grails.web.pages;

import groovy.lang.GroovyObject;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Provides services for resolving URIs
 * 
 * caches lookups in an internal ConcurrentHashMap cache
 * 
 * @author Lari Hotari , Sagire Software Oy
 *
 */
public class DefaultGroovyPagesUriService extends GroovyPagesUriSupport implements GroovyPagesUriService {
	ConcurrentHashMap<TupleStringKey, String> templateURICache=new ConcurrentHashMap<TupleStringKey, String>();
	ConcurrentHashMap<TupleStringKey, String> deployedViewURICache=new ConcurrentHashMap<TupleStringKey, String>();
	ConcurrentHashMap<ControllerObjectKey, String> controllerNameCache=new ConcurrentHashMap<ControllerObjectKey, String>();
	ConcurrentHashMap<TupleStringKey, String> noSuffixViewURICache=new ConcurrentHashMap<TupleStringKey, String>();
	
	private class TupleStringKey {
		String keyPart1;
		String keyPart2;
		
	    public TupleStringKey(String keyPart1, String keyPart2) {
			this.keyPart1 = keyPart1;
			this.keyPart2 = keyPart2;
		}

		public boolean equals(final Object that) {
	    	if (this == that) {
	    		return true;
	    	}
	    	if(that == null) {
	    		return false;
	    	}
	    	if(this.getClass() != that.getClass()) {
	    		return false;
	    	}
	    	TupleStringKey thatKey=(TupleStringKey)that;
	        return new EqualsBuilder().append(keyPart1, thatKey.keyPart1).append(keyPart2, thatKey.keyPart2).isEquals();
	    }

	    public int hashCode() {
	        return new HashCodeBuilder().append(keyPart1).append(keyPart2).toHashCode();
	    }		
	}
	
	private class ControllerObjectKey {
		private long controllerHashCode;
		private String controllerClassName;
		
		public ControllerObjectKey(GroovyObject controller) {
			this.controllerHashCode = System.identityHashCode(controller.getClass());
			this.controllerClassName = controller.getClass().getName();
		}
		
		public boolean equals(final Object that) {
	    	if (this == that) {
	    		return true;
	    	}
	    	if(that == null) {
	    		return false;
	    	}
	    	if(this.getClass() != that.getClass()) {
	    		return false;
	    	}
	    	ControllerObjectKey thatKey=(ControllerObjectKey)that;
	        return new EqualsBuilder().append(controllerHashCode, thatKey.controllerHashCode).append(controllerClassName, thatKey.controllerClassName).isEquals();
	    }

	    public int hashCode() {
	        return new HashCodeBuilder().append(controllerHashCode).append(controllerClassName).toHashCode();
	    }		
	}
	
	/* (non-Javadoc)
	 * @see org.codehaus.groovy.grails.web.pages.GroovyPagesUriService#getTemplateURI(java.lang.String, java.lang.String)
	 */
	public String getTemplateURI(String controllerName, String templateName) {
		TupleStringKey key=new TupleStringKey(controllerName, templateName);
		String uri=templateURICache.get(key);
		if(uri==null) {
			uri=super.getTemplateURI(controllerName, templateName);
			String prevuri=templateURICache.putIfAbsent(key, uri);
			if(prevuri != null) {
				return prevuri;
			}
		}
		return uri;
	}
	
	/* (non-Javadoc)
	 * @see org.codehaus.groovy.grails.web.pages.GroovyPagesUriService#getDeployedViewURI(java.lang.String, java.lang.String)
	 */
	public String getDeployedViewURI(String controllerName, String viewName) {
		TupleStringKey key=new TupleStringKey(controllerName, viewName);
		String uri=deployedViewURICache.get(key);
		if(uri==null) {
			uri=super.getDeployedViewURI(controllerName, viewName);
			String prevuri=deployedViewURICache.putIfAbsent(key, uri);
			if(prevuri != null) {
				return prevuri;
			}			
		}
		return uri;
	}
	
	public String getLogicalControllerName(GroovyObject controller) {
		ControllerObjectKey key=new ControllerObjectKey(controller);
		String name=controllerNameCache.get(key);
		if(name==null) {
			name=super.getLogicalControllerName(controller);
			String prevname=controllerNameCache.putIfAbsent(key, name);
			if(prevname != null) {
				return prevname;
			}
		}
		return name;
	}
	
	/* (non-Javadoc)
	 * @see org.codehaus.groovy.grails.web.pages.GroovyPagesUriService#getNoSuffixViewURI(groovy.lang.GroovyObject, java.lang.String)
	 */
	public String getNoSuffixViewURI(GroovyObject controller, String viewName) {
        if(controller == null) throw new IllegalArgumentException("Argument [controller] cannot be null");
        return getNoSuffixViewURI(getLogicalControllerName(controller),viewName);
	}

	/* (non-Javadoc)
	 * @see org.codehaus.groovy.grails.web.pages.GroovyPagesUriService#getNoSuffixViewURI(java.lang.String, java.lang.String)
	 */
	public String getNoSuffixViewURI(String controllerName,
			String viewName) {
		TupleStringKey key=new TupleStringKey(controllerName, viewName);
		String uri=noSuffixViewURICache.get(key);
		if(uri==null) {
			uri=super.getNoSuffixViewURI(controllerName, viewName);
			String prevuri=noSuffixViewURICache.putIfAbsent(key, uri);
			if(prevuri != null) {
				return prevuri;
			}
		}
		return uri;
	}

	/* (non-Javadoc)
	 * @see org.codehaus.groovy.grails.web.pages.GroovyPagesUriService#getTemplateURI(groovy.lang.GroovyObject, java.lang.String)
	 */
	public String getTemplateURI(GroovyObject controller, String templateName) {
		return getTemplateURI(getLogicalControllerName(controller), templateName);
	}

	/* (non-Javadoc)
	 * @see org.codehaus.groovy.grails.web.pages.GroovyPagesUriService#clear()
	 */
	public void clear() {
		templateURICache.clear();
		deployedViewURICache.clear();
		controllerNameCache.clear();
		noSuffixViewURICache.clear();
	}
}
