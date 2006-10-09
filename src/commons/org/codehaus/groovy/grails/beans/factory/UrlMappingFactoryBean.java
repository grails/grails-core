/**
 * 
 */
package org.codehaus.groovy.grails.beans.factory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * A factory bean that creates the URL mappings, checking if there is a bean
 * called urlMap in the ctx and merging that with the mappings set explicitly
 * on this bean
 * 
 * @author Graeme Rocher
 * @since 0.3
 *
 */
public class UrlMappingFactoryBean extends AbstractFactoryBean implements ApplicationContextAware{

	private static final Log LOG = LogFactory.getLog(UrlMappingFactoryBean.class);
	private static final String URL_MAP_BEAN = "urlMappings";
	private ApplicationContext applicationContext;
	private Map mappings = new HashMap();

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.config.AbstractFactoryBean#createInstance()
	 */
	protected Object createInstance() throws Exception {
		if(applicationContext.containsBean(UrlMappingFactoryBean.URL_MAP_BEAN)) {
			Object o = applicationContext.getBean(UrlMappingFactoryBean.URL_MAP_BEAN);
			if(o instanceof Map) {
				mappings.putAll((Map)o);
			}
		}
		if(LOG.isDebugEnabled()) {
			LOG.debug("[UrlMappingFactoryBean] Creating URL mappings as...");
			for (Iterator i = mappings.keySet().iterator(); i.hasNext();) {
				Object key = (Object) i.next();
				LOG.debug("[UrlMappingFactoryBean] " + key + "="+mappings.get(key));
			}
		}
		return mappings;
	}

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.FactoryBean#getObjectType()
	 */
	public Class getObjectType() {
		return Map.class;
	}
	
	

	public void setMappings(Map mappings) {
		this.mappings = mappings;
	}

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

}
