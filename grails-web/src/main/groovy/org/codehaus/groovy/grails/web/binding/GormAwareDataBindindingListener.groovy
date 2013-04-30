package org.codehaus.groovy.grails.web.binding

import groovy.transform.CompileStatic

import org.codehaus.groovy.grails.validation.ConstrainedProperty
import org.codehaus.groovy.runtime.MetaClassHelper
import org.codehaus.groovy.runtime.metaclass.ThreadManagedMetaBeanProperty
import org.grails.databinding.errors.BindingError
import org.grails.databinding.events.DataBindingListenerAdapter
import org.springframework.context.support.DefaultMessageSourceResolvable
import org.springframework.validation.BindingResult
import org.springframework.validation.Errors
import org.springframework.validation.FieldError

@CompileStatic
class GormAwareDataBindindingListener extends DataBindingListenerAdapter {
    private final BindingResult tmpBindingResult

    GormAwareDataBindindingListener(BindingResult tmpBindingResult) {
        this.tmpBindingResult = tmpBindingResult
    }

    @Override
    Boolean beforeBinding(obj, String propertyName, value) {
        if ("".equals(value)) {
            def cps = resolveConstrainedProperties(obj)
            if (cps != null) {
                ConstrainedProperty cp = (ConstrainedProperty)cps[propertyName]
                if (cp && cp.isNullable()) {
                    obj[propertyName] = null
                    return false
                }
            }
        }
        return true
    }

    @Override
    void bindingError(BindingError error) {
        Object[] o = getArgumentsForBindError(error.object?.getClass()?.getName(), error.getPropertyName())
        def codes = ['typeMismatch']
        def cause = error.cause
        def defaultMessage = cause ? cause.message : 'Data Binding Failed'
        def fieldError = new FieldError(error.object?.getClass()?.getName(), error.getPropertyName(), error.getRejectedValue(), true, codes as String[], o, defaultMessage)
        tmpBindingResult.addError(fieldError)
    }

    private Map resolveConstrainedProperties(object) {
        Map constrainedProperties = null
        MetaClass mc = GroovySystem.getMetaClassRegistry().getMetaClass(object.getClass())
        MetaProperty metaProp = mc.getMetaProperty('constraints')
        if (metaProp != null) {
            Object constrainedPropsObj = getMetaPropertyValue(metaProp, object)
            if (constrainedPropsObj instanceof Map) {
                constrainedProperties = (Map)constrainedPropsObj
            }
        }
        constrainedProperties
    }

    private getMetaPropertyValue(MetaProperty metaProperty, delegate) {
        if (metaProperty instanceof ThreadManagedMetaBeanProperty) {
            return ((ThreadManagedMetaBeanProperty)metaProperty).getGetter().invoke(delegate, MetaClassHelper.EMPTY_ARRAY)
        }

        return metaProperty.getProperty(delegate)
    }

    protected Object[] getArgumentsForBindError(String objectName, String field) {
        def codes = [
            objectName + Errors.NESTED_PATH_SEPARATOR + field,
            field] as String[]
        [new DefaultMessageSourceResolvable(codes, field)] as Object[]
    }
}
