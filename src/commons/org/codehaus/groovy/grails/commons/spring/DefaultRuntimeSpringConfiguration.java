/*
 * Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.commons.spring;

import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.util.Assert;

import java.util.*;

/**
 * A programmable runtime Spring configuration that allows a spring ApplicationContext
 * to be constructed at runtime
 *
 * Credit must go to Solomon Duskis and the
 * article: http://jroller.com/page/Solomon?entry=programmatic_configuration_in_spring
 *
 * @author Graeme
 * @since 0.3
 *
 */
public class DefaultRuntimeSpringConfiguration implements
        RuntimeSpringConfiguration {

    private static final Log LOG = LogFactory.getLog(DefaultRuntimeSpringConfiguration.class);
    protected GenericApplicationContext context;
    private Map beanConfigs = new HashMap();
    private Map beanDefinitions = new HashMap();
    private List beanNames = new ArrayList();
    protected ApplicationContext parent;
    protected ClassLoader classLoader;
    private Map aliases = new HashMap();

    public DefaultRuntimeSpringConfiguration() {
        super();
    }

    /**
     * Creates the ApplicationContext instance. Subclasses can override to customise the used ApplicationContext
     *
     * @param parent The parent ApplicationContext instance. Can be null.
     *
     * @return An instance of GenericApplicationContext
     */
    protected GenericApplicationContext createApplicationContext(ApplicationContext parent) {
        if(parent != null) return new GrailsApplicationContext(parent);
        return new GrailsApplicationContext();
    }

    public DefaultRuntimeSpringConfiguration(ApplicationContext parent) {
        this(parent, null);
    }

    public DefaultRuntimeSpringConfiguration(ApplicationContext parent, ClassLoader cl) {
        super();
        this.parent = parent;
        this.classLoader = cl;
    }


    private void trySettingClassLoaderOnContextIfFoundInParent(ApplicationContext parent) {
        if(parent.containsBean(GrailsRuntimeConfigurator.CLASS_LOADER_BEAN)) {
            Object classLoader = parent.getBean(GrailsRuntimeConfigurator.CLASS_LOADER_BEAN);
            if(classLoader instanceof ClassLoader){
                ClassLoader cl = (ClassLoader) classLoader;
                setClassLoaderOnContext(cl);
            }
        }
    }

    private void setClassLoaderOnContext(ClassLoader cl) {
        this.context.setClassLoader(cl);
        this.context.getBeanFactory().setBeanClassLoader(cl);
    }

    /**
     * Initialises the ApplicationContext instance
     */
    protected void initialiseApplicationContext() {
        if(this.context == null) {
            this.context = createApplicationContext(this.parent);
            if(parent != null && classLoader == null){
                trySettingClassLoaderOnContextIfFoundInParent(parent);
            }
            else if(classLoader != null)  {
                setClassLoaderOnContext(classLoader);
            }

            Assert.notNull(context);
        }
    }


    public BeanConfiguration addSingletonBean(String name, Class clazz) {
        BeanConfiguration bc = new DefaultBeanConfiguration(name,clazz);
        registerBeanConfiguration(name, bc);
        return bc;
    }

    public BeanConfiguration addPrototypeBean(String name, Class clazz) {
        BeanConfiguration bc = new DefaultBeanConfiguration(name,clazz,true);
        registerBeanConfiguration(name, bc);
        return bc;
    }

    public ApplicationContext getApplicationContext() {
        initialiseApplicationContext();
        registerBeansWithContext(context);
        context.refresh();
        return context;
    }

    public ApplicationContext getUnrefreshedApplicationContext() {
        initialiseApplicationContext();
        return context;
    }
    
    public BeanConfiguration addSingletonBean(String name) {
        BeanConfiguration bc = new DefaultBeanConfiguration(name);
        registerBeanConfiguration(name, bc);
        return bc;
    }

    public BeanConfiguration createSingletonBean(Class clazz) {
        return new DefaultBeanConfiguration(clazz);
    }

    public BeanConfiguration addSingletonBean(String name, Class clazz, Collection args) {
        BeanConfiguration bc = new DefaultBeanConfiguration(name,clazz,args);
        registerBeanConfiguration(name, bc);
        return bc;
    }

    public BeanConfiguration addPrototypeBean(String name) {
        BeanConfiguration bc = new DefaultBeanConfiguration(name,true);
        registerBeanConfiguration(name, bc);
        return bc;
    }

    private void registerBeanConfiguration(String name, BeanConfiguration bc) {
        beanConfigs.put(name,bc);
        beanNames.add(name);
    }

    public BeanConfiguration createSingletonBean(Class clazz, Collection constructorArguments) {
        return new DefaultBeanConfiguration(clazz, constructorArguments);
    }


    public BeanConfiguration createPrototypeBean(String name) {
        return new DefaultBeanConfiguration(name,true);
    }

    public BeanConfiguration createSingletonBean(String name) {
        return new DefaultBeanConfiguration(name);
    }

    public void addBeanConfiguration(String beanName, BeanConfiguration beanConfiguration) {
        beanConfiguration.setName(beanName);
        registerBeanConfiguration(beanName, beanConfiguration);
    }

    public void addBeanDefinition(String name, BeanDefinition bd) {
        beanDefinitions.put(name,bd);
        beanNames.add(name);
    }

    public boolean containsBean(String name) {
        return beanNames .contains(name);
    }

    public BeanConfiguration getBeanConfig(String name) {
        return (BeanConfiguration)beanConfigs.get(name);
    }

    public AbstractBeanDefinition createBeanDefinition(String name) {
        if(containsBean(name)) {
            if(beanDefinitions.containsKey(name))
                return (AbstractBeanDefinition)beanDefinitions.get(name);
            else if(beanConfigs.containsKey(name))
                return ((BeanConfiguration)beanConfigs.get(name)).getBeanDefinition();
        }
        return null;
    }

    public void registerPostProcessor(BeanFactoryPostProcessor processor) {
        initialiseApplicationContext();
        this.context.addBeanFactoryPostProcessor(processor);
    }



    public List getBeanNames() {
        return beanNames;
    }

    public void registerBeansWithContext(GenericApplicationContext applicationContext) {
        for (Iterator i = beanConfigs.values().iterator(); i.hasNext();) {
            BeanConfiguration bc = (BeanConfiguration) i.next();
            String beanName = bc.getName();
            if(LOG.isDebugEnabled()) {
                LOG.debug("[RuntimeConfiguration] Registering bean [" + beanName + "]");
                if(LOG.isTraceEnabled()) {
                    PropertyValue[] pvs = bc.getBeanDefinition()
                                            .getPropertyValues()
                                            .getPropertyValues();
                    for (int j = 0; j < pvs.length; j++) {
                        PropertyValue pv = pvs[j];
                        LOG.trace("[RuntimeConfiguration] With property [" + pv.getName() + "] set to ["+pv.getValue()+"]");
                    }
                }
            }

            
            if(applicationContext.containsBeanDefinition(beanName)) {
                removeBeanDefinition(applicationContext, beanName);
            }

            applicationContext.registerBeanDefinition(beanName,
                                                bc.getBeanDefinition()	);

            registerBeanAliases(applicationContext, beanName);
        }
        for (Iterator i = beanDefinitions.keySet().iterator(); i.hasNext();) {
            Object key = i.next();
            BeanDefinition bd = (BeanDefinition)beanDefinitions.get(key) ;
            if(LOG.isDebugEnabled()) {
                LOG.debug("[RuntimeConfiguration] Registering bean [" + key + "]");
                if(LOG.isTraceEnabled()) {
                    PropertyValue[] pvs = bd.getPropertyValues().getPropertyValues();
                    for (int j = 0; j < pvs.length; j++) {
                        PropertyValue pv = pvs[j];
                        LOG.trace("[RuntimeConfiguration] With property [" + pv.getName() + "] set to ["+pv.getValue()+"]");
                    }
                }
            }
            final String beanName = key.toString();
            if(applicationContext.containsBean(beanName)) {
                removeBeanDefinition(applicationContext, beanName);
            }

            applicationContext.registerBeanDefinition(beanName, bd);
            
            registerBeanAliases(applicationContext, beanName);
        }

    }

    private void registerBeanAliases(GenericApplicationContext applicationContext, String beanName) {
        List beanAliases = (List)aliases.get(beanName);
        if(beanAliases != null && !beanAliases.isEmpty()) {
            for (Iterator j = beanAliases.iterator(); j.hasNext();) {
                String alias = (String) j.next();
                applicationContext.registerAlias(beanName, alias);
            }
        }
    }

    private void removeBeanDefinition(GenericApplicationContext applicationContext, String beanName) {
        MetaClass mc = GroovySystem.getMetaClassRegistry().getMetaClass(applicationContext.getClass());
        if(mc.respondsTo(applicationContext, "removeBeanDefinition").size()>0) {
            mc.invokeMethod(applicationContext,"removeBeanDefinition",new Object[]{beanName});
        }
    }

    /**
     * Adds an abstract bean and returns the BeanConfiguration instance
     *
     * @param name The name of the bean
     * @return The BeanConfiguration object
     */
    public BeanConfiguration addAbstractBean(String name) {
        BeanConfiguration bc = new DefaultBeanConfiguration(name);
        bc.setAbstract(true);
        registerBeanConfiguration(name, bc);

        return bc;
    }

    public void addAlias(String alias, String beanName) {
        List beanAliases = (List)this.aliases.get(beanName);
        if(beanAliases == null) {
            beanAliases = new ArrayList();
            this.aliases.put(beanName, beanAliases);
        }
        beanAliases.add(alias);
    }

    public BeanDefinition getBeanDefinition(String beanName) {
        return (BeanDefinition) this.beanDefinitions.get(beanName);
    }
}
