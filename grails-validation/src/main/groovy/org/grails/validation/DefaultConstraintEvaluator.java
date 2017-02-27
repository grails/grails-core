/*
 * Copyright (C) 2011 SpringSource
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
package org.grails.validation;

import grails.core.GrailsDomainClass;
import grails.core.GrailsDomainClassProperty;
import grails.gorm.validation.ConstrainedProperty;
import grails.util.Holders;
import grails.validation.Constrained;
import grails.validation.ConstraintsEvaluator;
import groovy.lang.Closure;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Default implementation of the {@link grails.validation.ConstraintsEvaluator} interface.
 *
 * @author Graeme Rocher
 * @since 2.0
 * @deprecated Use {@link org.grails.datastore.gorm.validation.constraints.eval.DefaultConstraintEvaluator} instead
 */
@Deprecated
public class DefaultConstraintEvaluator implements ConstraintsEvaluator {

    protected org.grails.datastore.gorm.validation.constraints.eval.ConstraintsEvaluator delegate;

    public DefaultConstraintEvaluator(org.grails.datastore.gorm.validation.constraints.eval.ConstraintsEvaluator delegate) {
        this.delegate = delegate;
    }

    public DefaultConstraintEvaluator() {
    }

    @Override
    public Map<String, Object> getDefaultConstraints() {
        return resolveDelegate().getDefaultConstraints();
    }

    protected org.grails.datastore.gorm.validation.constraints.eval.ConstraintsEvaluator resolveDelegate() {
        if(delegate == null) {
            ApplicationContext applicationContext = Holders.findApplicationContext();
            if(applicationContext != null) {
                try {
                    delegate = applicationContext.getBean(org.grails.datastore.gorm.validation.constraints.eval.ConstraintsEvaluator.class);
                } catch (BeansException e) {
                    // doesn't exist, use default
                }
            }

            if(delegate == null) {
                delegate = new org.grails.datastore.gorm.validation.constraints.eval.DefaultConstraintEvaluator();
            }
        }
        return delegate;
    }

    @Override
    public Map<String, Constrained> evaluate(@SuppressWarnings("rawtypes") Class cls) {
        Map<String, ConstrainedProperty> evaluated = resolveDelegate().evaluate(cls);
        return adaptConstraints(evaluated);
    }

    @Override
    public Map<String, Constrained> evaluate(@SuppressWarnings("rawtypes") Class cls, boolean defaultNullable) {
        Map<String, ConstrainedProperty> evaluated = resolveDelegate().evaluate(cls, defaultNullable);
        return adaptConstraints(evaluated);
    }

    @Override
    public Map<String, Constrained> evaluate(Class<?> cls, boolean defaultNullable, boolean useOnlyAdHocConstraints, Closure[] adHocConstraintsClosures) {
        Map<String, ConstrainedProperty> evaluated = resolveDelegate().evaluate(cls, defaultNullable, useOnlyAdHocConstraints, adHocConstraintsClosures);
        return adaptConstraints(evaluated);
    }

    @Override
    public Map<String, Constrained> evaluate(GrailsDomainClass cls) {
        return evaluate(cls.getClazz());
    }

    @Override
    public Map<String, Constrained> evaluate(Object object, GrailsDomainClassProperty[] properties) {
        return evaluate(object.getClass(), properties);
    }

    @Override
    public Map<String, Constrained> evaluate(Class<?> cls, GrailsDomainClassProperty[] properties) {
        Map<String, ConstrainedProperty> evaluated = resolveDelegate().evaluate(cls);
        Map<String, Constrained> adapted = adaptConstraints(evaluated);
        for (GrailsDomainClassProperty property : properties) {
            String name = property.getName();
            if(!adapted.containsKey(name)) {
                adapted.remove(name);
            }
        }
        return adapted;

    }

    private Map<String, Constrained> adaptConstraints(Map<String, ConstrainedProperty> evaluated) {
        Map<String, Constrained> finalConstraints = new LinkedHashMap<>(evaluated.size());
        for (Map.Entry<String, ConstrainedProperty> entry : evaluated.entrySet()) {
            finalConstraints.put(entry.getKey(), new ConstrainedDelegate(entry.getValue()));
        }
        return finalConstraints;
    }
}
