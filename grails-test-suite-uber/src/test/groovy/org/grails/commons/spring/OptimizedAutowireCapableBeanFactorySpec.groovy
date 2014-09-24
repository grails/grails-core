package org.grails.commons.spring

import org.grails.spring.beans.factory.OptimizedAutowireCapableBeanFactory
import spock.lang.Specification
import javassist.util.proxy.*

import java.beans.PropertyDescriptor
import org.springframework.beans.factory.support.GenericBeanDefinition

class OptimizedAutowireCapableBeanFactorySpec extends Specification {
    static int AUTOWIRE_BY_NAME = 1

    void "Test factory bean cache fallback"() {
        setup: "Setup the factory bean"
            def factoryBean = new OptimizedAutowireCapableBeanFactory()

        and: "Bean to be inspected for autowire"
            def existingBean = new TestObject()

        and: "Creates a Javassist proxy"
            def proxyFactory = new ProxyFactory()
            proxyFactory.setSuperclass(TestObject.class)
            def clazz = proxyFactory.createClass()
            def proxy = clazz.newInstance([:])

        and: "Saves the property 'setAutowireProperty' and 'getAutowireProperty' inside the properties cache"
            def writeMethod = clazz.getMethod("setAutowireProperty", Object.class)
            def readMethod = clazz.getMethod("getAutowireProperty")
            def autowireableBeanProps = ['autowireProperty': new PropertyDescriptor('autowireProperty', readMethod, writeMethod)]

        and: "Put the proxy inside the properties cache"
            def beanDefinition = new GenericBeanDefinition()
            beanDefinition.beanClass = String.class
            beanDefinition.autowireCandidate = true
            factoryBean.registerBeanDefinition("autowireProperty", beanDefinition)
            factoryBean.autowireableBeanPropsCacheForClass.put(TestObject.class, autowireableBeanProps)

        when: "We try to autowire a normal bean but the cache is populated with the proxy"
            factoryBean.autowireBeanProperties(existingBean, 1, false)

        then:
            notThrown(Exception)
    }
}

class TestObject{
    def autowireProperty
}
