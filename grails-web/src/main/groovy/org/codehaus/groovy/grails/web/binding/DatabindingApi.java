package org.codehaus.groovy.grails.web.binding; 

import java.util.Map;

import org.springframework.validation.BindingResult;

public class DatabindingApi {
    /**
     * Binds the source object to the properties of the target instance converting any types as necessary
     *
     * @param instance The instance
     * @param bindingSource The binding source
     * @return The BindingResult
     */
    public BindingResult setProperties(final Object instance, final Object bindingSource) {
        return DataBindingUtils.bindObjectToInstance(instance, bindingSource);
    }
    
    /**
     * Returns a map of the objects properties that can be used to during binding to bind a subset of properties
     *
     * @param instance The instance
     * @return An instance of {@link DataBindingLazyMetaPropertyMap}
     */
    public Map getProperties(final Object instance) {
        return new DataBindingLazyMetaPropertyMap(instance);
    }
}