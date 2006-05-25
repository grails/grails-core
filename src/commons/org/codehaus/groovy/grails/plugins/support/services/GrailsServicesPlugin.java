package org.codehaus.groovy.grails.plugins.support.services;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsServiceClass;
import org.codehaus.groovy.grails.plugins.GrailsPlugin;
import org.codehaus.groovy.grails.plugins.support.OrderedAdapter;
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.util.Assert;
import org.springframework.transaction.interceptor.TransactionProxyFactoryBean;

import java.util.Properties;

public class GrailsServicesPlugin extends OrderedAdapter implements GrailsPlugin {
    public void doWithGenericApplicationContext(GenericApplicationContext applicationContext, GrailsApplication application) {
        GrailsServiceClass[] serviceClasses = application.getGrailsServiceClasses();
        for (int i = 0; i < serviceClasses.length; i++) {
            RootBeanDefinition factoryBean = new RootBeanDefinition(GrailsServiceFactory.class);
            MutablePropertyValues mpv = new MutablePropertyValues();
            mpv.addPropertyValue("serviceFullname", serviceClasses[i].getFullName());
            factoryBean.setPropertyValues(mpv);

            String factoryBeanDefinitionName = serviceClasses[i].getFullName() + "Class";
            applicationContext.registerBeanDefinition(factoryBeanDefinitionName, factoryBean);

            RootBeanDefinition factoryMethod = new RootBeanDefinition();
            factoryMethod.setFactoryBeanName(factoryBeanDefinitionName);
            factoryMethod.setFactoryMethodName("getObject");
            if (serviceClasses[i].byName()) {
                factoryMethod.setAutowireMode(AutowireCapableBeanFactory.AUTOWIRE_BY_NAME);
            }
            if (serviceClasses[i].byType()) {
                factoryMethod.setAutowireMode(AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE);
            }

            String serviceName = serviceClasses[i].getFullName() + "Service";
            if (serviceClasses[i].isTransactional()) {
                RootBeanDefinition transactionProxy = new RootBeanDefinition(TransactionProxyFactoryBean.class);
                MutablePropertyValues mpvTx = new MutablePropertyValues();
                mpvTx.addPropertyValue("target", factoryMethod);
                Properties transactionAttributes = new Properties();
                transactionAttributes.setProperty("*", "PROPAGATION_REQUIRED");
                mpvTx.addPropertyValue("transactionAttributes", transactionAttributes);
                mpvTx.addPropertyValue("proxyTargetClass", "true");
                mpvTx.addPropertyValue("transactionManager", new RuntimeBeanReference("transactionManager"));
                transactionProxy.setPropertyValues(mpvTx);

                applicationContext.registerBeanDefinition(serviceName, transactionProxy);
            } else {
                applicationContext.registerBeanDefinition(serviceName, factoryMethod);
            }
        }
    }

    public static class GrailsServiceFactory implements GrailsApplicationAware {
        private GrailsApplication grailsApplication;
        private String serviceFullname;

        public void setGrailsApplication(GrailsApplication grailsApplication) {
            this.grailsApplication = grailsApplication;
        }

        public void setServiceFullname(String serviceFullname) {
            this.serviceFullname = serviceFullname;
        }

        public Object getObject() {
            Assert.notNull(serviceFullname, "Service full name is required!");
            Assert.notNull(grailsApplication, "GrailsApplication instance is required!");

            GrailsServiceClass serviceClass = grailsApplication.getGrailsServiceClass(serviceFullname);

            return serviceClass.newInstance();
        }
    }
}
