package org.codehaus.groovy.grails.web.binding
 
import org.codehaus.groovy.grails.validation.ConstrainedProperty
import org.codehaus.groovy.runtime.MetaClassHelper
import org.codehaus.groovy.runtime.metaclass.ThreadManagedMetaBeanProperty
import org.grails.databinding.errors.BindingError
import org.grails.databinding.events.DataBindingListenerAdapter
import org.springframework.context.support.DefaultMessageSourceResolvable
import org.springframework.validation.BindingResult
import org.springframework.validation.Errors
import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError
 
class GormAwareDataBindindingListener extends
            DataBindingListenerAdapter {
        private final BindingResult tmpBindingResult;
        private final Object object;
 
        GormAwareDataBindindingListener(BindingResult tmpBindingResult,
                Object object) {
            this.tmpBindingResult = tmpBindingResult;
            this.object = object;
        }
 
        @Override
        public Boolean beforeBinding(Object obj, String propertyName, Object value) {
            if("".equals(value)) {
                def cps = resolveConstrainedProperties(obj)
                
                ConstrainedProperty cp = cps[propertyName]
                if(cp && cp.isNullable()) {
                    obj[propertyName] = null
                    return false;
                }
 
            }
            return true;
        }
 
        @Override
        public void bindingError(BindingError error) {
            
            // TODO
            // This is obviously a temporary placeholder...
            
            Object[] o = getArgumentsForBindError(object.getClass().getName(), error.getPropertyName());
            def codes = ['typeMismatch']
            String defaultMessage = "Some Default Message";
            if(error.getCause() instanceof MalformedURLException) {
                defaultMessage = "Failed to convert property value of type '" + 
                error.getRejectedValue().getClass().getName() + 
                "' to required type 'java.net.URL' for property '" + 
                error.getPropertyName() + 
                "'; nested exception is java.lang.IllegalArgumentException: Could not retrieve URL for class path resource [" +
                error.getRejectedValue() +
                "]: class path resource [" +
                error.getRejectedValue() +
                "] cannot be resolved to URL because it does not exist";
            }
            ObjectError fieldError = new FieldError("", error.getPropertyName(), error.getRejectedValue(), true, codes.toArray(new String[0]), o, defaultMessage);
            tmpBindingResult.addError(fieldError);
        }
        
        private Map resolveConstrainedProperties(Object object) {
            Map constrainedProperties = null;
                // is this dead code? , didn't remove in case it's used somewhere
                MetaClass mc = GroovySystem.getMetaClassRegistry().getMetaClass(object.getClass());
                MetaProperty metaProp = mc.getMetaProperty('constraints');
                if (metaProp != null) {
                    Object constrainedPropsObj = getMetaPropertyValue(metaProp, object);
                    if (constrainedPropsObj instanceof Map) {
                        constrainedProperties = (Map)constrainedPropsObj;
                }
            }
            return constrainedProperties;
        }
    
        private Object getMetaPropertyValue(MetaProperty metaProperty, Object delegate) {
        if (metaProperty instanceof ThreadManagedMetaBeanProperty) {
            return ((ThreadManagedMetaBeanProperty)metaProperty).getGetter().invoke(delegate, MetaClassHelper.EMPTY_ARRAY);
        }
 
        return metaProperty.getProperty(delegate);
    }
        
        protected Object[] getArgumentsForBindError(String objectName, String field) {
            def codes = [objectName + Errors.NESTED_PATH_SEPARATOR + field, field] as String[]
            [new DefaultMessageSourceResolvable(codes, field)] as Object[];
        }
}