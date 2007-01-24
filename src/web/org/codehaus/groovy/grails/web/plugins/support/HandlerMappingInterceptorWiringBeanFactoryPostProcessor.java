package org.codehaus.groovy.grails.web.plugins.support;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;

/**
 * <p>This class automagically wires all <code>HandlerInterceptors</code> and <code>WebRequestInterceptors</code>
 * (web interceptors) in a Spring <code>BeanFactory</code> to all <code>AbstractHandlerMappings</code>.
 *
 * <p>As to be expected such an implementation cannot handle each and every situation. The goal however
 * is to move this wiring logic out of Grails plugin implementations. To make this abstraction useful
 * for everyone writing plugins we make these assumptions:
 *
 * <ul>
 *   <li><code>HandlerInterceptors</code>, <code>WebRequestInterceptors</code> and
 *   <code>AbstractHandlerMappings</code> are not created by
 *   <code>FactoryBeans</code> or factory methods.</li>
 *   <li><code>HandlerInterceptors</code>, <code>WebRequestInterceptors</code> and
 *   <code>AbstractHandlerMappings</code> are not created by child bean definitions.
 *   This requirement is enforced.</li>
 *   <li><code>HandlerInterceptors</code> and <code>WebRequestInterceptors</code>
 *   should be registered as singletons. This requirement is enforced.<li>
 *   <li><code>HandlerInterceptors</code>, <code>WebRequestInterceptors</code> and
 *   <code>AbstractHandlerMappings</code> should not be registered with the
 *   <code>BeanFactory</code> without bean definition. This requirement cannot be enforced.
 *   Notice however that objects registered in this fashion will not be wired at all.</li>
 *   <li><code>HandlerInterceptors</code>, <code>WebRequestInterceptors</code> and
 *   <code>AbstractHandlerMappings</code> should not be registered as inner bean definitions.
 *   It's very unlikely someone would do this. This requirement could be enforced but
 *   isn't.</li>
 *   <li>It may make sense to register a web interceptor explicitly for one or more
 *   <code>AbstractHanlderMappings</code>. This wiring implementation however does
 *   not support this very well. First of all, all <code>HandlerInterceptors</code>
 *   and <code>WebRequestInterceptors</code> will be registered to all
 *   <code>AbstractHanlderMappings</code>. And secondly, aliases make it hard or
 *   even impossible to have a fail-proof protection against double registration of web interceptors with
 *   <code>WebRequestInterceptors</code>. To make things easy we don't accept any injection
 *   in the <code>interceptors</code> property at this time.</li>
 * </ul>
 *
 * <p>While these limitations are not expected to block plugin developers they are important to be aware
 * of when registering <code>HandlerInterceptors</code>, <code>WebRequestInterceptors</code> and
 * <code>AbstractHandlerMappings</code>. If you're experiencing difficulties by these restrictions
 * please do send a mail to the developer mailing list.
 *
 * <p>As a side-note, this is a pessimistic implementation that could be replaced by an optimistic one
 * using object-oriented design to select and refuse web interceptors.
 *
 * @author Steven Devijver
 * @since 2 jan 2007
 */
public class HandlerMappingInterceptorWiringBeanFactoryPostProcessor implements
		BeanFactoryPostProcessor {

	private static Log log = LogFactory.getLog(HandlerMappingInterceptorWiringBeanFactoryPostProcessor.class);
	private final static String INTERCEPTORS_PROPERTY = "interceptors";

	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
			throws BeansException {
		ManagedList managedListWithRuntimeReferencesForWebInterceptors =
			createManagedListWithRuntimeReferencesForWebInterceptors(beanFactory);
		String[] beanDefinitionNames = beanFactory.getBeanDefinitionNames();

		for (int i = 0; i < beanDefinitionNames.length; i++) {
			listenToBeanDefinition(
				beanDefinitionNames[i],
				beanFactory.getBeanDefinition(beanDefinitionNames[i]),
				managedListWithRuntimeReferencesForWebInterceptors);
		}
	}

	private void listenToBeanDefinition(String beanDefinitionName, BeanDefinition beanDefinition, ManagedList managedList) {
		Class beanDefinitionClass = getClassForBeanDefinition(beanDefinitionName, beanDefinition);
		if (beanDefinitionClass != null && AbstractHandlerMapping.class.isAssignableFrom(beanDefinitionClass)) {
			handleAbstractHandlerMappingBeanDefinition(beanDefinitionName, beanDefinition, managedList);
		}
	}

	private void handleAbstractHandlerMappingBeanDefinition(String beanDefinitionName, BeanDefinition beanDefinition, ManagedList managedList) {
		if (!(beanDefinition instanceof RootBeanDefinition)) {
			throw new RuntimeException(
				"Bean definition ["
				+ beanDefinitionName
				+ "] cannot have a parent.");
		}
		if (beanDefinition.isAbstract()) {
			throw new RuntimeException(
				"Bean definition ["
				+ beanDefinitionName
				+ "] cannot be abstract.");
		}

		if (beanDefinition.getPropertyValues().contains(INTERCEPTORS_PROPERTY)) {
			throw new RuntimeException(
				"Please remove the interceptors property from the ["
				+ beanDefinitionName + "] bean definition. Instead register your web interceptors" +
				" as singleton, non-abstract, non-child bean definitions. These will be automatically" +
				"wired to AbstractHandlerMapping instances.");
		} else {
			beanDefinition.getPropertyValues().addPropertyValue(INTERCEPTORS_PROPERTY, managedList);
		}
	}

	private ManagedList createManagedListWithRuntimeReferencesForWebInterceptors(ConfigurableListableBeanFactory beanFactory) {
		return createManagedListWithRuntimeReferences(getAllWebInterceptorBeanDefinitionNames(beanFactory));
	}

	private ManagedList createManagedListWithRuntimeReferences(String[] webInterceptorBeanDefinitionNames) {
		ManagedList managedList = new ManagedList();

		for (int i = 0; i < webInterceptorBeanDefinitionNames.length; i++) {
			managedList.add(
				new RuntimeBeanReference(webInterceptorBeanDefinitionNames[i])
			);
		}

		return managedList;
	}

	private String[] getAllWebInterceptorBeanDefinitionNames(ConfigurableListableBeanFactory beanFactory) {
		List webInterceptorBeanDefinitionNames = new ArrayList();
		String[] beanDefinitionNames = beanFactory.getBeanDefinitionNames();
		for (int i = 0; i < beanDefinitionNames.length; i++) {
			String beanDefinitionName = beanDefinitionNames[i];
			BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanDefinitionName);
			Class beanDefinitionClass = getClassForBeanDefinition(beanDefinitionName, beanDefinition);
			if (beanDefinitionClass != null
				&& (HandlerInterceptor.class.isAssignableFrom(beanDefinitionClass)
				|| WebRequestInterceptor.class.isAssignableFrom(beanDefinitionClass))) {
					if (!(beanDefinition instanceof RootBeanDefinition)) {
						log.warn("Web interceptor bean definition ["
							+ beanDefinitionName
							+ "] not selected for wiring to "
							+ "HandlerMappings because it is not of type RootBeanDefinition.");
						continue;
					}
					if (!beanDefinition.isSingleton()) {
						log.warn("Web interceptor bean definition ["
								+ beanDefinitionName
								+ "] not selected for wiring to "
								+ "HandlerMappings because it is not singleton.");
						continue;
					}
					if (beanDefinition.isAbstract()) {
						log.warn("Web interceptor bean definition ["
								+ beanDefinitionName
								+ "] not selected for wiring to "
								+ "HandlerMappings because it is abstract.");
						continue;
					}

					log.debug("Web interceptor bean definition ["
						+ beanDefinitionName
						+ "] selected for wiring.");
					webInterceptorBeanDefinitionNames.add(beanDefinitionName);
			}
		}
		return (String[]) webInterceptorBeanDefinitionNames.toArray(new String[webInterceptorBeanDefinitionNames.size()]);
	}

	private Class getClassForBeanDefinition(String beanDefinitionName, BeanDefinition beanDefinition) {
		if (StringUtils.hasText(beanDefinition.getBeanClassName())) {
			// try to load the class. Log a message when this fails.
			try {
				return ClassUtils.forName(beanDefinition.getBeanClassName());
			} catch (ClassNotFoundException e) {
				log.warn(
					"Could not determine bean definition class for ["
					+ beanDefinitionName
					+ "] because of error", e);
			} catch (NoClassDefFoundError e) {
				log.warn(
					"Could not determine bean definition class for ["
					+ beanDefinitionName
					+ "] because of error", e);
			} catch (LinkageError e) {
				log.warn(
					"Could not determine bean definition class for ["
					+ beanDefinitionName
					+ "] because of error", e);
			}
		}
		return null;
	}

}
