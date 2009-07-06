package org.codehaus.groovy.grails.support;

import groovy.lang.GroovyObjectSupport;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.*;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockServletContext;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.ServletContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class MockApplicationContext extends GroovyObjectSupport implements WebApplicationContext {

	Date startupDate = new Date();
	Map beans = new HashMap();
	List resources = new ArrayList();
    List ignoredClassLocations = new ArrayList();
    PathMatcher pathMatcher = new AntPathMatcher();

	public void registerMockBean(String name, Object instance) {
		beans.put(name,instance);
	}

	/**
	 * Registers a mock resource. Path separator: "/"
	 * @param location the location of the resource. Example: /WEB-INF/grails-app/i18n/messages.properties
	 */
	public void registerMockResource(String location) {
		resources.add(new ClassPathResource(StringUtils.removeStart(location, "/")));
	}

	/**
	 * Registers a mock resource. Path separator: "/"
	 * @param location the location of the resource. Example: /WEB-INF/grails-app/i18n/messages.properties
	 */
	public void registerMockResource(String location, String contents) {
		resources.add(new MockResource(StringUtils.removeStart(location, "/"), contents));
	}

	/**
	 * Unregisters a mock resource. Path separator: "/"
	 * @param location the location of the resource. Example: /WEB-INF/grails-app/i18n/messages.properties
	 */
	public void unregisterMockResource(String location) {
        for (Iterator it = resources.iterator(); it.hasNext();) {
            MockResource mockResource = (MockResource) it.next();
            if (mockResource.location.equals(location)) {
                it.remove();
            }
        }
	}

    /**
     * Registers a resource that should not be found on the classpath. Path separator: "/"
     * @param location the location of the resource. Example: /WEB-INF/grails-app/i18n/messages.properties
     */
    public void registerIgnoredClassPathLocation(String location) {
        ignoredClassLocations.add(location);
    }

    /**
     * Unregisters a resource that should not be found on the classpath. Path separator: "/"
     * @param location the location of the resource. Example: /WEB-INF/grails-app/i18n/messages.properties
     */
    public void unregisterIgnoredClassPathLocation(String location) {
        ignoredClassLocations.remove(location);
    }

    public ApplicationContext getParent() {
		throw new UnsupportedOperationException("Method not supported by implementation");
	}

    public String getId() {
        return "MockApplicationContext";
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
        return getBeanNamesForType(type);
    }

	public Map getBeansOfType(Class type) throws BeansException {
        String[] beanNames = getBeanNamesForType(type);
        Map newMap = new HashMap();
        for (int i = 0; i < beanNames.length; i++) {
            String beanName = beanNames[i];
            newMap.put(beanName, getBean(beanName));

        }
        return newMap;
	}

	public Map getBeansOfType(Class type, boolean includePrototypes,
			boolean includeFactoryBeans) throws BeansException {
        String[] beanNames = getBeanNamesForType(type);
        Map newMap = new HashMap();
        for (int i = 0; i < beanNames.length; i++) {
            String beanName = beanNames[i];
            newMap.put(beanName, getBean(beanName));

        }
        return newMap;
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

    public Object getBean(String name, Object[] args) throws BeansException {
        return getBean(name);
    }


    public Object getProperty(String name) {
        if(beans.containsKey(name)) return beans.get(name);
        else
            return super.getProperty(name);
    }

    public boolean containsBean(String name) {
		return beans.containsKey(name);
	}

	public boolean isSingleton(String name)
			throws NoSuchBeanDefinitionException {
		throw new UnsupportedOperationException("Method not supported by implementation");
	}

    public boolean isPrototype(String s) throws NoSuchBeanDefinitionException {
        throw new UnsupportedOperationException("Method not supported by implementation");
    }

    public boolean isTypeMatch(String s, Class aClass) throws NoSuchBeanDefinitionException {
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
		if (locationPattern.startsWith("classpath:") || locationPattern.startsWith("file:"))
			throw new UnsupportedOperationException("Location patterns 'classpath:' and 'file:' not supported by implementation");

		locationPattern = StringUtils.removeStart(locationPattern, "/"); // starting with "**/" is OK
		List result = new ArrayList();
		for (Iterator i = resources.iterator(); i.hasNext();) {
			Resource res = (Resource)i.next();
            String path = res instanceof ClassPathResource ? ((ClassPathResource) res).getPath() : res.getDescription();
            if (pathMatcher.match(locationPattern, path)) {
				result.add(res);
			}
		}
		return (Resource[])result.toArray(new Resource[0]);
	}

	public Resource getResource(String location) {
        for (Iterator i = resources.iterator(); i.hasNext();) {
            Resource mockResource = (Resource) i.next();
            if (pathMatcher.match(mockResource.getDescription(), StringUtils.removeStart(location, "/"))) {
                return mockResource;
            }

        }
        // Check for ignored resources and return null instead of a classpath resource in that case.
        for (Iterator i = ignoredClassLocations.iterator(); i.hasNext();) {
            String resourceLocation = (String) i.next();
            if (pathMatcher.match(
                    StringUtils.removeStart(location, "/"),
                    StringUtils.removeStart(resourceLocation, "/"))) {
                return null;
            }

        }
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

    public ServletContext getServletContext() {
        return new MockServletContext();
    }

    public class MockResource extends AbstractResource {

        private String contents = "";
        private String location;


        public MockResource(String location) {
            this.location = location;
        }

        public MockResource(String location, String contents) {
            this(location);
            this.contents = contents;
        }

        public boolean exists() {
            return true;
        }

        public String getDescription() {
            return location;
        }

        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(contents.getBytes());
        }

    }
}
