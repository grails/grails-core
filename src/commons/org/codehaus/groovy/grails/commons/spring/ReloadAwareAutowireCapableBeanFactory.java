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
package org.codehaus.groovy.grails.commons.spring;

import grails.util.Environment;
import groovy.lang.GroovyObject;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.compiler.GrailsClassLoader;
import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.CglibSubclassingInstantiationStrategy;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;

import java.lang.reflect.Constructor;

/**
 * A BeanFactory that can deal with class cast exceptions that may occur due to class reload events and then
 * attempt to reload the bean being instantiated to avoid them.
 *
 * @author Graeme Rocher
 * @since 1.1.1
 *        <p/>
 *        Created: May 8, 2009
 */
public class ReloadAwareAutowireCapableBeanFactory extends DefaultListableBeanFactory{

    public ReloadAwareAutowireCapableBeanFactory() {
        if(Environment.getCurrent().isReloadEnabled()) {

            // Implementation note: The default Spring InstantiationStrategy caches constructors. This is no good at development time
            // because if the class reloads then Spring continues to use the old class. We deal with this here by disabling the caching
            // for development time only
            setInstantiationStrategy(new CglibSubclassingInstantiationStrategy() {
                @Override
                public Object instantiate(RootBeanDefinition beanDefinition, String beanName, BeanFactory owner) {
                    // Don't override the class with CGLIB if no overrides.
                    if (beanDefinition.getMethodOverrides().isEmpty()) {
                        Constructor constructorToUse;
                        Class clazz = beanDefinition.getBeanClass();
                        if (clazz.isInterface()) {
                            throw new BeanInstantiationException(clazz, "Specified class is an interface");
                        }
                        try {
                            constructorToUse = clazz.getDeclaredConstructor((Class[]) null);

                        }
                        catch (Exception ex) {
                            throw new BeanInstantiationException(clazz, "No default constructor found", ex);
                        }

                        return BeanUtils.instantiateClass(constructorToUse, null);
                    }
                    else {
                        // Must generate CGLIB subclass.
                        return instantiateWithMethodInjection(beanDefinition, beanName, owner);
                    }

                }

            });
        }
    }

    @Override
    protected Object doCreateBean(String beanName, RootBeanDefinition mbd, Object[] args) {
        if(Environment.getCurrent().isReloadEnabled()) {
            try {
                return super.doCreateBean(beanName, mbd, args);
            }
            catch (BeanCreationException t) {
                if(t.getCause() instanceof TypeMismatchException)  {
                    // type mismatch probably occured because another class was reloaded
                    final Class beanClass = mbd.getBeanClass();
                    if(GroovyObject.class.isAssignableFrom(beanClass)) {
                        GrailsApplication application = (GrailsApplication) getBean(GrailsApplication.APPLICATION_ID);
                        ClassLoader classLoader = application.getClassLoader();
                        if(classLoader instanceof GrailsClassLoader) {
                            GrailsClassLoader gcl = (GrailsClassLoader) classLoader;
                            gcl.reloadClass(beanClass.getName());
                            try {
                                Class  newBeanClass = gcl.loadClass(beanClass.getName());
                                mbd.setBeanClass(newBeanClass);
                                if(!newBeanClass.equals(beanClass)) {
                                    GrailsPluginManager pluginManager = (GrailsPluginManager) getBean(GrailsPluginManager.BEAN_NAME);
                                    pluginManager.informOfClassChange(newBeanClass);
                                    return super.doCreateBean(beanName, mbd, args);
                                }

                            }
                            catch (ClassNotFoundException e) {
                                throw t;
                            }
                        }
                    }
                }
                throw t;
            }
        }
        else {
            return super.doCreateBean(beanName, mbd, args);
        }

    }

}
