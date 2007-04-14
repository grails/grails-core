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
package org.codehaus.groovy.grails.validation;

import org.springframework.validation.Errors;
import org.springframework.beans.BeanWrapper;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.hibernate.collection.PersistentCollection;

/**
 * A validator that first checks if the Hibernate PersistentCollection instance has been initialised before bothering
 * to cascade
 *
 * @author Graeme Rocher
 * @since 0.5
 * 
 *        <p/>
 *        Created: Apr 13, 2007
 *        Time: 6:32:08 PM
 */
public class HibernateDomainClassValidator extends GrailsDomainClassValidator {

    /**
     * Overrides the default behaviour and first checks if a PersistentCollection instance has been initialised using the
     * wasInitialised() method before cascading
     *
     * @param errors The Spring Errors instance
     * @param bean The BeanWrapper for the bean
     * @param persistentProperty The GrailsDomainClassProperty instance
     * @param propertyName The name of the property
     *
     * @see org.hibernate.collection.PersistentCollection#wasInitialized()
     */
    protected void cascadeValidationToMany(Errors errors, BeanWrapper bean, GrailsDomainClassProperty persistentProperty, String propertyName) {
        Object collection = bean.getPropertyValue(propertyName);
        if(collection != null) {
            if(collection instanceof PersistentCollection) {
                PersistentCollection persistentCollection = (PersistentCollection)collection;
                if(persistentCollection.wasInitialized()) {
                    super.cascadeValidationToMany(errors, bean, persistentProperty, propertyName);
                }
            }
            else {
                super.cascadeValidationToMany(errors, bean, persistentProperty, propertyName);
            }

        }

    }
}
