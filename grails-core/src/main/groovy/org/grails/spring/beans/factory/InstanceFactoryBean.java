package org.grails.spring.beans.factory;

import org.springframework.beans.factory.FactoryBean;

/**
 * Simple singleton instance implementation of Spring's FactoryBean interface
 * 
 * mainly useful in unit tests
 * 
 */
public class InstanceFactoryBean<T> implements FactoryBean<T> {
    T object;
    Class<?> objectType;
    
    public InstanceFactoryBean() {
        
    }
    
    public InstanceFactoryBean(T object, Class<?> objectType) {
        this.object = object;
        this.objectType = objectType;
    }

    public InstanceFactoryBean(T object) {
        this.object = object;
        this.objectType = object.getClass();
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public T getObject() {
        return object;
    }


    public void setObject(T object) {
        this.object = object;
    }

    @Override
    public Class<?> getObjectType() {
        return objectType != null ? objectType : object.getClass();
    }

    public void setObjectType(Class<?> objectType) {
        this.objectType = objectType;
    }
}
