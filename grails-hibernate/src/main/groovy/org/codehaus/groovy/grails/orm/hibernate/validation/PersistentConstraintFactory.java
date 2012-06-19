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
package org.codehaus.groovy.grails.orm.hibernate.validation;

import org.codehaus.groovy.grails.exceptions.GrailsDomainException;
import org.codehaus.groovy.grails.validation.Constraint;
import org.codehaus.groovy.grails.validation.ConstraintFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;

/**
 * Creates PersistentConstraint instances ensuring that dependencies are provided.
 *
 * @author Graeme Rocher
 * @since 0.4
 */
public class PersistentConstraintFactory implements ConstraintFactory {

    private Class<?> constraintClass;
    private ApplicationContext applicationContext;

    public PersistentConstraintFactory(ApplicationContext applicationContext, Class<?> persistentConstraint) {

        Assert.notNull(applicationContext, "Argument [applicationContext] cannot be null");

        if (persistentConstraint == null || !PersistentConstraint.class.isAssignableFrom(persistentConstraint)) {
            throw new IllegalArgumentException(
                    "Argument [persistentConstraint] must be an instance of " + PersistentConstraint.class.getName());
        }

        this.applicationContext = applicationContext;
        this.constraintClass = persistentConstraint;
    }

    public Constraint newInstance() {
        try {
            PersistentConstraint instance = (PersistentConstraint)constraintClass.newInstance();
            instance.setApplicationContext(applicationContext);
            return instance;
        }
        catch (InstantiationException e) {
            throw new GrailsDomainException("Error instantiating constraint [" + constraintClass +
                    "] during validation: " + e.getMessage(), e);
        }
        catch (IllegalAccessException e) {
            throw new GrailsDomainException("Error instantiating constraint [" + constraintClass +
                    "] during validation: " + e.getMessage(), e);
        }
    }
}
