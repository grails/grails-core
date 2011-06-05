package org.codehaus.groovy.grails.web.pages;

import grails.util.Environment;
import groovy.lang.Binding;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;

/**
 * 
 * Script binding to be used as the top-level binding in GSP evaluation
 * 
 * @author Lari Hotari
 * 
 */
public class GroovyPageRequestBinding extends Binding {
	private static Log log=LogFactory.getLog(GroovyPageRequestBinding.class);
	private GrailsWebRequest webRequest;
	private HttpServletResponse response;
	private Map<String, Class<?>> cachedDomainsWithoutPackage;
	private static Map<String, LazyRequestBasedValue> lazyRequestBasedValuesMap = new HashMap<String, LazyRequestBasedValue>() {
		private static final long serialVersionUID = 1L;
		{
			put(GroovyPage.WEB_REQUEST, new LazyRequestBasedValue() {
				public Object evaluate(GrailsWebRequest webRequest,
						HttpServletResponse response) {
					return webRequest;
				}
			});
			put(GroovyPage.REQUEST, new LazyRequestBasedValue() {
				public Object evaluate(GrailsWebRequest webRequest,
						HttpServletResponse response) {
					return webRequest.getCurrentRequest();
				}
			});
			put(GroovyPage.RESPONSE, new LazyRequestBasedValue() {
				public Object evaluate(GrailsWebRequest webRequest,
						HttpServletResponse response) {
					return response;
				}
			});
			put(GroovyPage.FLASH, new LazyRequestBasedValue() {
				public Object evaluate(GrailsWebRequest webRequest,
						HttpServletResponse response) {
					return webRequest.getFlashScope();
				}
			});
			put(GroovyPage.SERVLET_CONTEXT, new LazyRequestBasedValue() {
				public Object evaluate(GrailsWebRequest webRequest,
						HttpServletResponse response) {
					return webRequest.getServletContext();
				}
			});
			put(GroovyPage.APPLICATION_CONTEXT, new LazyRequestBasedValue() {
				public Object evaluate(GrailsWebRequest webRequest,
						HttpServletResponse response) {
					return webRequest.getAttributes().getApplicationContext();
				}
			});
			put(GrailsApplication.APPLICATION_ID, new LazyRequestBasedValue() {
				public Object evaluate(GrailsWebRequest webRequest,
						HttpServletResponse response) {
					return webRequest.getAttributes().getGrailsApplication();
				}
			});
			put(GroovyPage.SESSION, new LazyRequestBasedValue() {
				public Object evaluate(GrailsWebRequest webRequest,
						HttpServletResponse response) {
					return webRequest.getSession();
				}
			});
			put(GroovyPage.PARAMS, new LazyRequestBasedValue() {
				public Object evaluate(GrailsWebRequest webRequest,
						HttpServletResponse response) {
					return webRequest.getParams();
				}
			});
			put(GroovyPage.ACTION_NAME, new LazyRequestBasedValue() {
				public Object evaluate(GrailsWebRequest webRequest,
						HttpServletResponse response) {
					return webRequest.getActionName();
				}
			});
			put(GroovyPage.CONTROLLER_NAME, new LazyRequestBasedValue() {
				public Object evaluate(GrailsWebRequest webRequest,
						HttpServletResponse response) {
					return webRequest.getControllerName();
				}
			});
			put(GrailsApplicationAttributes.CONTROLLER,
					new LazyRequestBasedValue() {
						public Object evaluate(GrailsWebRequest webRequest,
								HttpServletResponse response) {
							return webRequest.getAttributes().getController(
									webRequest.getCurrentRequest());
						}
					});
		}
	};

	public GroovyPageRequestBinding(GrailsWebRequest webRequest,
			HttpServletResponse response) {
		super();
		this.webRequest = webRequest;
		this.response = response;
	}

	@Override
	public Object getVariable(String name) {
		Object val = getVariables().get(name);
		if(val == null && !getVariables().containsKey(name)) {
			val = webRequest.getCurrentRequest().getAttribute(name);
	
			if (val == null) {
				LazyRequestBasedValue lazyValue = lazyRequestBasedValuesMap.get(name);
				if (lazyValue != null) {
					val = lazyValue.evaluate(webRequest, response);
				}
			}
			
			if (val == null && cachedDomainsWithoutPackage != null) {
				val = cachedDomainsWithoutPackage.get(name);
			}
			
			// warn about missing variables in development mode
			if(val == null && Environment.isDevelopmentMode()) {
				if(log.isWarnEnabled()) {
					log.warn("Variable '" + name + "' not found in binding or the value is null.");
				}
			}
		}
		return val;
	}

	public void setCachedDomainsWithoutPackage(
			Map<String, Class<?>> cachedDomainsWithoutPackage) {
		this.cachedDomainsWithoutPackage = cachedDomainsWithoutPackage;
	}

	private static interface LazyRequestBasedValue {
		public Object evaluate(GrailsWebRequest webRequest,
				HttpServletResponse response);
	}
}
