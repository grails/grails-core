package org.codehaus.groovy.grails.support;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

public class MockApplicationContext implements ApplicationContext {

	Date startupDate = new Date();
	Map beans = new HashMap();
	
	public void registerMockBean(String name, Object instance) {
		beans.put(name,instance);
	}
	public ApplicationContext getParent() {
		throw new UnsupportedOperationException("Method not supported by implementation");	
	}

	public String getDisplayName() {
		throw new UnsupportedOperationException("Method not supported by implementation");
	}

	public long getStartupDate() {
		return startupDate.getTime();
	}

	public void publishEvent(ApplicationEvent event) {
		// do nothing
	}

	public boolean containsBeanDefinition(String beanName) {	
		return beans.containsKey(beanName);
	}

	public int getBeanDefinitionCount() {
		return beans.size();
	}

	public String[] getBeanDefinitionNames() {
		return (String[])beans.keySet().toArray(new String[beans.keySet().size()]);
	}

	public String[] getBeanDefinitionNames(Class type) {
		throw new UnsupportedOperationException("Method not supported by implementation");
	}

	public String[] getBeanNamesForType(Class type) {
		List beanNames = new ArrayList();
		for (Iterator i = beans.keySet().iterator(); i.hasNext();) {
			String beanName = (String)i.next();
			if(type.isAssignableFrom( beans.get(beanName).getClass() )) {
				beanNames.add(beanName);
			}			
		}
		return (String[])beanNames.toArray(new String[beanNames.size()]);
	}

	public String[] getBeanNamesForType(Class type, boolean includePrototypes,
			boolean includeFactoryBeans) {
		throw new UnsupportedOperationException("Method not supported by implementation");
	}

	public Map getBeansOfType(Class type) throws BeansException {
		return Collections.EMPTY_MAP;
	}

	public Map getBeansOfType(Class type, boolean includePrototypes,
			boolean includeFactoryBeans) throws BeansException {
		throw new UnsupportedOperationException("Method not supported by implementation");
	}

	public Object getBean(String name) throws BeansException {
		if(!beans.containsKey(name))throw new NoSuchBeanDefinitionException(name);
		return beans.get(name);
	}

	public Object getBean(String name, Class requiredType)
			throws BeansException {
		if(!beans.containsKey(name))throw new NoSuchBeanDefinitionException( name);
		if(requiredType != null && beans.get(name).getClass() != requiredType)throw new NoSuchBeanDefinitionException(name);
		
		return beans.get(name);
	}

	public boolean containsBean(String name) {
		return beans.containsKey(name);
	}

	public boolean isSingleton(String name)
			throws NoSuchBeanDefinitionException {
		throw new UnsupportedOperationException("Method not supported by implementation");
	}

	public Class getType(String name) throws NoSuchBeanDefinitionException {
		if(!beans.containsKey(name))throw new NoSuchBeanDefinitionException(name);
		
		return beans.get(name).getClass();
	}

	public String[] getAliases(String name)
			throws NoSuchBeanDefinitionException {
		throw new UnsupportedOperationException("Method not supported by implementation");
	}

	public BeanFactory getParentBeanFactory() {
		return null;
	}

	public String getMessage(String code, Object[] args, String defaultMessage,
			Locale locale) {
		MessageSource messageSource = (MessageSource)getBean("messageSource");
		if(messageSource == null) throw new BeanCreationException("No bean [messageSource] found in MockApplicationContext");
		return messageSource.getMessage(code, args, defaultMessage, locale);
		
	}

	public String getMessage(String code, Object[] args, Locale locale)
			throws NoSuchMessageException {
		MessageSource messageSource = (MessageSource)getBean("messageSource");
		if(messageSource == null) throw new BeanCreationException("No bean [messageSource] found in MockApplicationContext");
		return messageSource.getMessage(code, args, locale);
	}

	public String getMessage(MessageSourceResolvable resolvable, Locale locale)
			throws NoSuchMessageException {
		MessageSource messageSource = (MessageSource)getBean("messageSource");
		if(messageSource == null) throw new BeanCreationException("No bean [messageSource] found in MockApplicationContext");
		return messageSource.getMessage(resolvable, locale);
	}

	public Resource[] getResources(String locationPattern) throws IOException {
		throw new UnsupportedOperationException("Method not supported by implementation");
	}

	public Resource getResource(String location) {
		return new ClassPathResource(location);
	}
	public boolean containsLocalBean(String arg0) {
		throw new UnsupportedOperationException("Method not supported by implementation");
	}
	public AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException {
		return new DefaultListableBeanFactory();
	}
	public ClassLoader getClassLoader() {
		return getClass().getClassLoader();
	}

}
