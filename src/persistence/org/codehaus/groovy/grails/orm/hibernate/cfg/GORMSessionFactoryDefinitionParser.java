/* Copyright 2004-2005 Graeme Rocher
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.orm.hibernate.cfg;

import grails.persistence.Entity;
import groovy.lang.GroovyClassLoader;
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;
import org.codehaus.groovy.grails.orm.hibernate.ConfigurableLocalSessionFactoryBean;
import org.codehaus.groovy.grails.orm.hibernate.support.SpringLobHandlerDetectorFactoryBean;
import org.codehaus.groovy.grails.orm.hibernate.validation.HibernateDomainClassValidator;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.MethodInvokingFactoryBean;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.beans.factory.parsing.Location;
import org.springframework.beans.factory.parsing.Problem;
import org.springframework.beans.factory.support.*;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.JdkVersion;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.orm.hibernate3.HibernateTransactionManager;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * A BeanDefinitionParser that will scan for GORM entities to configure and automatically setup an
 * appropriate Hibernate SessionFactory for usage with GORM. Example in XML:
 *
 * <pre><code>
 * <gorm:sessionFactory
 *          data-source-ref="myDataSource"
 *          base-package="my.company.domain"
 *          lob-handler-ref="lobHandler" <!--optional-->
 *          config-location="classpath:hibernate.cfg.xml" <!--optional-->
 *          config-class="org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsAnnotationConfiguration" <!--optional-->
 * >
 *      <property name="hibernateProperties"></property>
 * </gorm:sessionFactory>
 *
 * </code></pre>
 *
 * @see org.springframework.beans.factory.xml.BeanDefinitionParser
 *
 * @author Graeme Rocher
 * @since 1.1
 *        <p/>
 *        Created: Jan 16, 2009
 */
public class GORMSessionFactoryDefinitionParser implements BeanDefinitionParser {

    private static final String ID_ATTRIBUTE = "id";
    private static final String DATA_SOURCE_ATTRIBUTE = "data-source-ref";
    private static final String MESSAGE_SOURCE_ATTRIBUTE = "message-source-ref";
    private static final String LOB_HANDLER_ATTRIBUTE = "lob-handler-ref";
    private static final String BASE_PACKAGE_ATTRIBUTE = "base-package";
    private static final String TRANSACTION_MANAGER_ATTRIBUTE = "transaction-manager-ref";

    public BeanDefinition parse(Element element, ParserContext parserContext) {

        final XmlReaderContext readerContext = parserContext
                                                    .getReaderContext();
        final ClassLoader beanClassLoader = readerContext
                                                .getBeanClassLoader() != null ?
                                            readerContext.getBeanClassLoader() :
                                            Thread.currentThread().getContextClassLoader();

        String[] basePackages =
                StringUtils.commaDelimitedListToStringArray(element.getAttribute(BASE_PACKAGE_ATTRIBUTE));


        String dataSourceId = element.getAttribute(DATA_SOURCE_ATTRIBUTE);
        if(!StringUtils.hasText(dataSourceId)) {
            throw new BeanDefinitionParsingException(new Problem("Attribute ["+DATA_SOURCE_ATTRIBUTE+"] of tag <gorm:sessionFactory> must be specified!", new Location(readerContext.getResource())));
        }


        // Actually scan for bean definitions and register them.
        BeanDefinitionRegistry targetRegistry = parserContext.getRegistry();

        // setup the GrailsApplication instance
        parseGrailsApplication(element,parserContext, readerContext, beanClassLoader, basePackages);

        GenericBeanDefinition postProccessingBeanDef = new GenericBeanDefinition();
        postProccessingBeanDef.setBeanClass(GORMEnhancingBeanPostProcessor.class);

        targetRegistry.registerBeanDefinition("gormEnhancingPostProcessor", postProccessingBeanDef);

        return parseSessionFactory(element, dataSourceId, targetRegistry, parserContext);
    }

    private void parseGrailsApplication(Element element, ParserContext parserContext, XmlReaderContext readerContext, ClassLoader beanClassLoader, String[] basePackages) {
        BeanDefinitionRegistry simpleRegistry = new SimpleBeanDefinitionRegistry();
        ClassPathBeanDefinitionScanner scanner = configureScanner(parserContext, simpleRegistry);

        scanner.scan(basePackages);

        BeanDefinitionRegistry targetRegistry = parserContext.getRegistry();
        AbstractBeanDefinition grailsApplicationBean = new GenericBeanDefinition();
        grailsApplicationBean.setBeanClass(DefaultGrailsApplication.class);
        grailsApplicationBean.setInitMethodName("initialise");
        ConstructorArgumentValues constructorArgs = grailsApplicationBean.getConstructorArgumentValues();

        Set<Class> classes = new HashSet<Class>();


        for(String beanName : simpleRegistry.getBeanDefinitionNames()) {
            BeanDefinition beanDef = simpleRegistry.getBeanDefinition(beanName);
            try {
                Class entityClass = beanClassLoader
                                        .loadClass(beanDef.getBeanClassName());

                classes.add(entityClass);

                registerDomainBean(entityClass, targetRegistry, element.getAttribute(MESSAGE_SOURCE_ATTRIBUTE));
            }
            catch (ClassNotFoundException e) {
                throw new BeanDefinitionParsingException(new Problem("Unable to load class whilst configuring GORM: " + e.getMessage(), new Location(readerContext.getResource()),null, e));
            }
        }

        constructorArgs.addGenericArgumentValue(classes.toArray(new Class[classes.size()]));
        constructorArgs.addGenericArgumentValue(new GroovyClassLoader(beanClassLoader));
        targetRegistry.registerBeanDefinition(GrailsApplication.APPLICATION_ID, grailsApplicationBean);
    }

    private void registerDomainBean(final Class entityClass, BeanDefinitionRegistry targetRegistry, String messageSourceRef) {
        GenericBeanDefinition beanDef = new GenericBeanDefinition();
        beanDef.setBeanClass(entityClass);
        beanDef.setScope("prototype");

        RootBeanDefinition domainDef = new RootBeanDefinition(MethodInvokingFactoryBean.class);
        
        domainDef.getPropertyValues().addPropertyValue("targetObject", new RuntimeBeanReference(GrailsApplication.APPLICATION_ID));
        domainDef.getPropertyValues().addPropertyValue("targetMethod", "getArtefact");
        domainDef.getPropertyValues().addPropertyValue("arguments", new ArrayList() {{
            add(DomainClassArtefactHandler.TYPE);
            add(entityClass.getName());
        }});


        final String domainRef = entityClass.getName() + "Domain";
        if(StringUtils.hasText(messageSourceRef)) {
            GenericBeanDefinition validatorDef = new GenericBeanDefinition();
            validatorDef.setBeanClass(HibernateDomainClassValidator.class);
            validatorDef.getPropertyValues().addPropertyValue("messageSource", new RuntimeBeanReference(messageSourceRef));
            validatorDef.getPropertyValues().addPropertyValue("domainClass", new RuntimeBeanReference(domainRef));
            targetRegistry.registerBeanDefinition(entityClass.getName()+"Validator",validatorDef);
        }

        targetRegistry.registerBeanDefinition(entityClass.getName(), beanDef);
        targetRegistry.registerBeanDefinition(domainRef, domainDef);


    }

    private AbstractBeanDefinition parseSessionFactory(Element element, String dataSourceId, BeanDefinitionRegistry targetRegistry, ParserContext parserContext) {
        String sessionFactoryId = StringUtils.hasText(element.getAttribute(ID_ATTRIBUTE)) ? element.getAttribute(ID_ATTRIBUTE) : "sessionFactory";
        AbstractBeanDefinition sessionFactoryBean = new GenericBeanDefinition();
        sessionFactoryBean.setBeanClass(ConfigurableLocalSessionFactoryBean.class);


        MutablePropertyValues propertyValues = sessionFactoryBean.getPropertyValues();
        final RuntimeBeanReference dataSourceRef = new RuntimeBeanReference(dataSourceId);
        propertyValues.addPropertyValue("dataSource", dataSourceRef);
        if(JdkVersion.isAtLeastJava15()) {
            propertyValues.addPropertyValue("configClass", GrailsAnnotationConfiguration.class);
        }
        else {
            propertyValues.addPropertyValue("configClass", DefaultGrailsDomainConfiguration.class);
        }
        propertyValues.addPropertyValue(GrailsApplication.APPLICATION_ID, new RuntimeBeanReference(GrailsApplication.APPLICATION_ID));

        targetRegistry.registerBeanDefinition(sessionFactoryId,sessionFactoryBean);

        final String lobHandlerRef = element.getAttribute(LOB_HANDLER_ATTRIBUTE);
        if(StringUtils.hasText(lobHandlerRef)) {
           propertyValues.addPropertyValue("lobHandler", new RuntimeBeanReference(lobHandlerRef));
        }
        else {
            GenericBeanDefinition lobHandler = new GenericBeanDefinition();
            lobHandler.setBeanClass(SpringLobHandlerDetectorFactoryBean.class);
            lobHandler.getPropertyValues().addPropertyValue("pooledConnection", true);
            lobHandler.getPropertyValues().addPropertyValue("dataSource", dataSourceRef);
            propertyValues.addPropertyValue("lobHandler", lobHandler);
        }

        String transactionManagerRef = element.getAttribute(TRANSACTION_MANAGER_ATTRIBUTE);

        if(StringUtils.hasText(transactionManagerRef)) {
            targetRegistry.registerAlias("transactionManager", transactionManagerRef);
        }
        else {
            GenericBeanDefinition transactionManagerBean = new GenericBeanDefinition();
            transactionManagerBean.setBeanClass(HibernateTransactionManager.class);
            transactionManagerBean.getPropertyValues().addPropertyValue("sessionFactory", new RuntimeBeanReference(sessionFactoryId));

            targetRegistry.registerBeanDefinition("transactionManager", transactionManagerBean);

        }

        parserContext.getDelegate().parsePropertyElements(element, sessionFactoryBean);       
        return sessionFactoryBean;
    }

    
    private ClassPathBeanDefinitionScanner configureScanner(ParserContext parserContext, BeanDefinitionRegistry registry) {
		XmlReaderContext readerContext = parserContext.getReaderContext();
        // Delegate bean definition registration to scanner class.
        ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(registry, false);
        scanner.setIncludeAnnotationConfig(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
        
        scanner.setResourceLoader(readerContext.getResourceLoader());
        scanner.setBeanDefinitionDefaults(parserContext.getDelegate().getBeanDefinitionDefaults());
        scanner.setAutowireCandidatePatterns(parserContext.getDelegate().getAutowireCandidatePatterns());

        return scanner;

    }


}
