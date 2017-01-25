package grails.core

import org.grails.datastore.mapping.model.MappingContext

/**
 * Created by jameskleeh on 1/20/17.
 */
class LazyMappingContext implements MappingContext {

    @Delegate MappingContext context

    void setMappingContext(MappingContext context) {
        this.context = context
    }

    boolean isInitialized() {
        this.context != null
    }
}
