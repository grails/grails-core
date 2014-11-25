/*
 * Copyright 2008 the original author or authors.
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
package grails.test

import grails.core.GrailsDomainClass
import grails.util.GrailsClassUtils
import grails.util.GrailsMetaClassUtils
import grails.validation.ConstrainedProperty
import grails.web.databinding.DataBindingUtils

import org.grails.core.DefaultGrailsDomainClass
import org.grails.core.artefact.DomainClassArtefactHandler
import org.grails.plugins.testing.GrailsMockErrors
import org.grails.validation.ConstrainedPropertyBuilder
import org.grails.validation.DefaultConstraintEvaluator
import org.grails.validation.GrailsDomainClassValidator
import org.grails.web.databinding.DataBindingLazyMetaPropertyMap
import org.springframework.beans.BeanUtils
import org.springframework.validation.Errors
import org.springframework.validation.FieldError

/**
 * A utility/helper class for mocking various types of Grails artifacts
 * and is one of the foundations of the Grails unit testing framework.
 *
 * @author Peter Ledbrook
 */
class MockUtils {

    static final errorsObjects = new ThreadLocalMap()

    /**
     * <p>Adds a <code>validate()</code> method to the given domain class
     * that performs validation against the constraints and returns a
     * map of errors. Each key in the map is the name of a field with
     * at least one error, while the value is the name of the constraint
     * that triggered the error, e.g. "nullable", or "min".</p>
     * <p>The main use for this version of the method is to test unique
     * constraints. The domain instance being validated will be checked
     * against the given list of objects and if a uniqueness constraint
     * is violated, it will appear in the returned list of errors.</p>
     * @param clazz The domain class to mock.
     * @param errorsMap A map that Errors instances will be stored in.
     * Each Errors object will be stored against the domain instance it
     * is attached to, so this should ideally be an "identity" map of
     * some sort.
     * @param testInstances A list of instances of type <code>clazz</code>
     * or of anything that can act as that type, such as a map with keys
     * that match the domain class's fields. In fact, the instances only
     * need properties that match the fields taking part in the unique
     * constraints.
     */
    static void prepareForConstraintsTests(Class clazz, Map errorsMap, List testInstances = [], Map defaultConstraints = [:]) {
        def dc = null
        if (DomainClassArtefactHandler.isDomainClass(clazz))
           dc = new DefaultGrailsDomainClass(clazz, defaultConstraints)

        addValidateMethod(clazz, dc, errorsMap, testInstances, defaultConstraints)

        // Note that if the test instances are of type "clazz", they
        // will not have the extra dynamic methods because they were
        // created before the methods were added to the class.
        //
        // So, for each test object that is an instance of "clazz", we
        // manually change its metaclass to "clazz"'s so that it gets
        // the extra methods.
        updateMetaClassForClass(testInstances, clazz)
    }

    /**
     * Adds a <code>validate()</code> method to the given domain class
     * or command object. It also adds the errors-related methods and
     * data-binding support (via the constructor and the <code>properties
     * </code> property).
     * @param clazz The domain or command class to add the methods to.
     * @param testInstances An optional list of existing instances of
     * the associated class, which is used to test the "unique" constraint.
     */
    private static void addValidateMethod(
            Class clazz,
            final GrailsDomainClass dc,
            final Map errorsMap,
            final List testInstances = [],
            final Map defaultConstraints = [:]) {
        def constraintsEvaluator = new DefaultConstraintEvaluator(defaultConstraints)

        // If we have a GrailsDomainClass, i.e. we are adding the method
        // to a domain class, then create a validator for it. This gives
        // us support for cascading validation, as well as being the
        // "real deal" (the validator used by running applications).
        def constrainedProperties
        if (dc) {
            def v = new GrailsDomainClassValidator()
            v.domainClass = dc
            constrainedProperties = constraintsEvaluator.evaluate(clazz, dc.properties)
        }
        else {
            def c = GrailsClassUtils.getStaticPropertyValue(clazz, "constraints")
            if (c instanceof Map) {
                constrainedProperties = c
            } else {
                def constraintsBuilder = new ConstrainedPropertyBuilder(clazz)
                // Get clazz's class hierarchy up to, but not including, Object
                // as a linked list. The list starts with the ultimate base class
                // and ends with "clazz".
                LinkedList classChain = new LinkedList()
                while (clazz != Object) {
                    classChain.addFirst(clazz)
                    clazz = clazz.getSuperclass()
                }

                // Now get build up our constraints from all "constraints"
                // properties in all the classes in the hierarchy.
                for (Iterator it = classChain.iterator(); it.hasNext();) {
                    clazz = (Class) it.next()
                    // Read the constraints.
                    c = GrailsClassUtils.getStaticPropertyValue(clazz, "constraints")

                    if (c) {
                        c = c.clone()
                        c.delegate = constraintsBuilder
                        try {
                            c.call()
                        } finally {
                            c.delegate = null
                        }
                    }
                }

                constrainedProperties = constraintsBuilder.constrainedProperties
            }
        }

        def emc = GrailsMetaClassUtils.getExpandoMetaClass(clazz)

        // Attach the instantiated constraints to the domain/command
        // object.
        emc.getConstraints = {->
            constrainedProperties
        }

        // Add data binding capabilities

        emc.constructor = { Map params ->
            def obj = BeanUtils.instantiateClass(delegate)
            DataBindingUtils.bindObjectToInstance(obj,params)
            return obj
        }

        emc.setProperties = { Object o ->
            DataBindingUtils.bindObjectToInstance(delegate,o)
        }

        emc.getProperties = {->
            new DataBindingLazyMetaPropertyMap(delegate)
        }

        // Add all the errors-related methods.
        emc.getErrors = {-> getErrorsFor(errorsMap, delegate) }
        emc.hasErrors = {-> getErrorsFor(errorsMap, delegate).hasErrors() }
        emc.setErrors = { Errors errors ->
            if (!(errors instanceof GrailsMockErrors)) {
                def mockErrors = new GrailsMockErrors(delegate)
                mockErrors.addAllErrors errors
                errors = mockErrors
            }
            setErrorsFor(errorsMap, delegate, errors)
        }
        emc.clearErrors = {-> clearErrorsFor(errorsMap, delegate) }

        // Finally add the "validate()" method, which can simply be
        // used to test the constraints or used from code under test.
        emc.validate = { Map args ->
            if (args == null) args = [:]
            def obj = delegate

            // Make sure any existing errors are cleared.
            clearErrors()

            // If we have a GrailsDomainClass, then we can use its
            // validator to perform the validation.
            if (dc) {
                dc.validator.validate(obj, errors, args["deepValidate"] == null ? true : args["deepValidate"])
            }

            // The GDC validator does not handle the "unique" constraint
            // for mocked domain classes, so we have to run through all
            // the constraints and check any "unique" ones. While doing
            // that, we also do the normal validation for the case where
            // we don't have a GDC, i.e. if we're validating a command
            // object.
            constrainedProperties.each { property, ConstrainedProperty constraint ->
                // Only perform the validation if we don't have a GDC
                // (since if there is one the validation has already
                // been done).
                if (!dc) constraint.validate(obj, obj."${property}", errors)

                // Handle the unique constraint if this field has one.
                def uniqueValue = constraint.getMetaConstraintValue("unique") ?: constraint.getAppliedConstraint("unique")?.parameter
                if (uniqueValue) {
                    def props
                    if (uniqueValue instanceof Boolean && uniqueValue) {
                        props = []
                    }
                    else if (uniqueValue instanceof String) {
                        props = [ uniqueValue ]
                    }
                    else if (uniqueValue instanceof Collection) {
                        props = uniqueValue
                    }

                    props = [ property ] + props
                    def existing = testInstances.find { inst -> !inst.is(obj) && props.every { inst."$it" != null && inst."$it" == obj."$it" } }
                    if (existing != null) {
                        errors.rejectValue(property, "unique")
                    }
                }
            }
            return !errors.hasErrors()
        }

        def beforeValidateHelper
        try {
            def helperClass = Thread.currentThread().contextClassLoader.loadClass('org.grails.datastore.gorm.support.BeforeValidateHelper')
            beforeValidateHelper = helperClass.newInstance()
        }
        catch (ignored) {
            // Hibernate isn't installed
        }

        // add no-arg attributes validator, just to be inline with what HibernatePluginSupport.addValidationMethods does.
        // It works lke validate(Map) with empty map
        emc.validate = { ->
            beforeValidateHelper?.invokeBeforeValidate delegate, null
            validate([:])
        }

        // add validator that is able to discard changes to the domain objects, that possible validator can do.
        // Validator is required for compatibility with HibernatePluginSupport.addValidationMethods.
        // Internally it call validator(Map) with evict parameter. Because current mock implementation of validator does not do any database interaction
        // so this validator works exactly the same way as validate()
        emc.validate = { Boolean b -> validate([evict: b]) }

        // add validator that validates only fields that names are passed in input list of fieldsToValdate.
        // All errors for the other fields are removed.
        emc.validate = { List fieldsToValidate ->
            beforeValidateHelper?.invokeBeforeValidate delegate, fieldsToValidate
            if (!validate([:]) && fieldsToValidate != null && !fieldsToValidate.isEmpty()) {
                def result = new GrailsMockErrors(delegate)
                for (e in errors.allErrors) {
                    if (e instanceof FieldError && !fieldsToValidate.contains(e.field)) {
                        continue
                    }
                    result.addError(e)
                }
                setErrors result
            }

            return !errors.hasErrors()
        }
    }

    /**
     * Gets the errors instance for an object if one exists, or creates a
     * new one otherwise. The errors instances are stored in a thread-local
     * map.
     * @param object The object whose errors instance should be retrieved.
     * @param replaceExisting <code>true</code> if any existing errors
     * instance should be replaced by a fresh one.
     * @return the Errors instance attached to the object.
     */
    private static Errors getErrorsFor(Map errorsMap, object, boolean replaceExisting = false) {
        // Check whether there is already an errors object for this
        // command object. If so, use that...
        def errors = errorsMap[object]

        // ...otherwise create a new one and store it in the thread
        // local map.
        if (!errors || replaceExisting) {
            // This takes advantage of the fact that "clearErrorsFor()"
            // creates a new, empty Errors instance.
            errors = clearErrorsFor(errorsMap, object)
        }

        return errors
    }

    /**
     * Sets the errors instance for an object. The errors instances are
     * stored in a thread-local map.
     * @param object The object whose errors instance should be changed.
     * @param errors The new Errors instance for the object.
     */
    private static void setErrorsFor(Map errorsMap, object, Errors errors) {
        errorsMap[object] = errors
    }

    /**
     * Clears any errors that might be attached to the given object.
     * What it really does is replace the errors instance in the
     * thread-local map with a completely new one.
     * @param object The object to clear the errors from.
     * @return the new, empty Errors instance attached to the object.
     */
    private static Errors clearErrorsFor(Map errorsMap, object) {
        Errors errors = new GrailsMockErrors(object)
        errorsMap[object] = errors
        return errors
    }

    /**
     * Updates all object in the given collection of type "clazz" so
     * that their meta-class is whatever clazz's is. This also injects
     * an "id" property into all the objects.
     * @param instances The objects whose meta-classes need updating.
     * @param clazz The type of objects we want to update. The "new"
     * meta-class is retrieved from this class instance.
     */
    private static void updateMetaClassForClass(Collection instances, Class clazz) {
        // For each object in the collection that is an instance of
        // "clazz", we manually change its metaclass to "clazz"'s.
        instances.each { obj ->

            if (clazz.isAssignableFrom(obj.getClass())) {
                obj.metaClass = clazz.metaClass
            }
        }
    }
}

/**
 * Custom ThreadLocal that stores a weak hash map. This is the map that
 * we use to associate command objects/domain instances with their
 * corresponding errors objects.
 */
class ThreadLocalMap extends ThreadLocal {
    protected initialValue() {
        return new WeakHashMap()
    }
}
