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
package org.codehaus.groovy.grails.validation

import org.springframework.util.Assert


/**
 * Provides the ability to set contraints against a properties of a class. Constraints can either be
 * set via the property setters or via the <pre>applyConstraint(String constraintName, Object constrainingValue)</pre>
 * in combination with a constraint constant. Example:
 *
 * <code>
 *      ...
 *
 *         ConstrainedProperty cp = new ConstrainedProperty(owningClass, propertyName, propertyType);
 *      if (cp.supportsConstraint(ConstrainedProperty.EMAIL_CONSTRAINT)) {
 *          cp.applyConstraint(ConstrainedProperty.EMAIL_CONSTRAINT, new Boolean(true));
 *      }
 * </code>
 *
 * Alternatively constraints can be applied directly using the java bean getters/setters if a static (as oposed to dynamic)
 * approach to constraint creation is possible:
 *
 * <code>
 *       cp.setEmail(true)
 * </code>
 * @author Graeme Rocher
 * @since 07-Nov-2005
 * @deprecated Use {@link grails.validation.ConstrainedProperty}
 */
@Deprecated
public class ConstrainedProperty extends grails.validation.ConstrainedProperty {

   
    /**
     * Constructs a new ConstrainedProperty for the given arguments.
     *
     * @param clazz The owning class
     * @param propertyName The name of the property
     * @param propertyType The property type
     */
    public ConstrainedProperty(Class<?> clazz,String propertyName, Class<?> propertyType) {
        super(clazz, propertyName, propertyType)
    }


    private static List<Object> getOrInitializeConstraint(String name) {
        List<Object> objects = constraints.get(name);
        if (objects == null) {
            objects = new ArrayList<Object>();
            constraints.put(name, objects);
        }
        return objects;
    }

    public static void registerNewConstraint(String name, ConstraintFactory factory) {
        Assert.hasLength(name, "Argument [name] cannot be null or blank");
        Assert.notNull(factory, "Argument [factory] cannot be null");
        List<Object> objects = getOrInitializeConstraint(name);
        objects.add(factory);
    }
}
