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

                import java.util.*;

                import javax.servlet.ServletContext;

                import org.apache.commons.logging.Log;
                import org.apache.commons.logging.LogFactory;
                import org.springframework.beans.PropertyValue;
                import org.springframework.beans.factory.config.BeanDefinition;
                import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
                import org.springframework.context.ApplicationContext;
                import org.springframework.web.context.WebApplicationContext;
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
                    private GrailsWebApplicationContext context;
                    private Map beanConfigs = new HashMap();
                    private Map beanDefinitions = new HashMap();
                    private List beanNames = new ArrayList();

                    public DefaultRuntimeSpringConfiguration() {
                        super();
                        this.context = new GrailsWebApplicationContext();
                    }

                    public DefaultRuntimeSpringConfiguration(ApplicationContext parent) {
                        super();
                        this.context = new GrailsWebApplicationContext(parent);
                    }

                    public BeanConfiguration addSingletonBean(String name, Class clazz) {
                        BeanConfiguration bc = new DefaultBeanConfiguration(name,clazz);
                        beanConfigs.put(name,bc);
                        beanNames.add(name);
                        return bc;
                    }

                    public BeanConfiguration addPrototypeBean(String name, Class clazz) {
                        BeanConfiguration bc = new DefaultBeanConfiguration(name,clazz,true);
                        beanConfigs.put(name,bc);
                        beanNames.add(name);
                        return bc;
                    }

                    public WebApplicationContext getApplicationContext() {
                        for (Iterator i = beanConfigs.values().iterator(); i.hasNext();) {
                            BeanConfiguration bc = (BeanConfiguration) i.next();
                            if(LOG.isDebugEnabled()) {
                                LOG.debug("[RuntimeConfiguration] Registering bean [" + bc.getName() + "]");
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

                            context.registerBeanDefinition(bc.getName(),
                                                                bc.getBeanDefinition()	);
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
                            context.registerBeanDefinition(key.toString(), bd);

                        }
                        context.refresh();
                        return context;
                    }

                    public BeanConfiguration addSingletonBean(String name) {
                        BeanConfiguration bc = new DefaultBeanConfiguration(name);
                        beanConfigs.put(name,bc);
                        beanNames.add(name);
                        return bc;
                    }

                    public BeanConfiguration createSingletonBean(Class clazz) {
                        return new DefaultBeanConfiguration(clazz);
                    }

                    public BeanConfiguration addSingletonBean(String name, Class clazz, Collection args) {
                        BeanConfiguration bc = new DefaultBeanConfiguration(name,clazz,args);
                        beanConfigs.put(name,bc);
                        beanNames.add(name);
                        return bc;
                    }

                    public BeanConfiguration addPrototypeBean(String name) {
                        BeanConfiguration bc = new DefaultBeanConfiguration(name,true);
                        beanConfigs.put(name,bc);
                        beanNames.add(name);
                        return bc;
                    }

                    public BeanConfiguration createSingletonBean(Class clazz, Collection constructorArguments) {
                        return new DefaultBeanConfiguration(clazz, constructorArguments);
                    }

                    public void setServletContext(ServletContext context) {
                        this.context.setServletContext(context);
                    }

                    public BeanConfiguration createPrototypeBean(String name) {
                        return new DefaultBeanConfiguration(name,true);
                    }

                    public BeanConfiguration createSingletonBean(String name) {
                        return new DefaultBeanConfiguration(name);
                    }

                    public void addBeanConfiguration(String beanName, BeanConfiguration beanConfiguration) {
                        beanConfiguration.setName(beanName);
                        beanConfigs.put(beanName,beanConfiguration);
                        beanNames.add(beanName);
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

                    public void registerPostProcessor(BeanFactoryPostProcessor processor) {
                        this.context.addBeanFactoryPostProcessor(processor);
                    }

                    public Map createBeanDefinitions() {
                        Map definitions = new HashMap();
                        for (Iterator i = beanConfigs.values().iterator(); i.hasNext();) {
                            BeanConfiguration bc = (BeanConfiguration) i.next();
                            if(LOG.isDebugEnabled()) {
                                LOG.debug("[RuntimeConfiguration] Registering bean [" + bc.getName() + "]");
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

                            definitions.put(bc.getName(),bc.getBeanDefinition());
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
                            definitions.put(key,bd);

                        }
                        return definitions;
                    }

                }
