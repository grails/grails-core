package org.codehaus.groovy.grails.web.plugins.support;

import java.util.List;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;

public class WebInterceptorWiringTests extends
		AbstractDependencyInjectionSpringContextTests {

	protected String[] getConfigLocations() {
		return new String[] {
			"classpath:org/codehaus/groovy/grails/web/plugins/support/web-interceptor-wiring-tests.xml"
		};
	}


	public void testAssertInterceptorWiring() {
		BeanDefinition beanDefinition = applicationContext.getBeanFactory().getBeanDefinition("handlerMapping");

		Object list = beanDefinition.getPropertyValues().getPropertyValue("interceptors").getValue();

		assertTrue(list instanceof ManagedList);
		assertEquals(2, ((List)list).size());

	}

}
