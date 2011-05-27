/* Copyright (C) 2011 SpringSource
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
package org.codehaus.groovy.grails.validation;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.Map;

/**
 *
 * Constructs the default constraints evaluator instance
 *
 * @author Graeme Rocher
 * @since 1.4
 */
public class ConstraintsEvaluatorFactoryBean implements FactoryBean<ConstraintsEvaluator>, InitializingBean {
    private ConstraintsEvaluator constraintsEvaluator;
    private Class<?> constraintsEvaluatorClass = DefaultConstraintEvaluator.class;
    @SuppressWarnings("rawtypes") private Map defaultConstraints;

    public void setConstraintsEvaluatorClass(Class<?> constraintsEvaluatorClass) {
        this.constraintsEvaluatorClass = constraintsEvaluatorClass;
    }

    public void setDefaultConstraints(@SuppressWarnings("rawtypes") Map defaultConstraints) {
        this.defaultConstraints = defaultConstraints;
    }

    public ConstraintsEvaluator getObject() throws Exception {
        return constraintsEvaluator;
    }

    public Class<?> getObjectType() {
        return ConstraintsEvaluator.class;
    }

    public boolean isSingleton() {
        return Boolean.TRUE;
    }

    public void afterPropertiesSet() throws Exception {
        constraintsEvaluator = (ConstraintsEvaluator) BeanUtils.instantiateClass(constraintsEvaluatorClass.getConstructor(new Class[]{Map.class}), defaultConstraints);
    }
}
