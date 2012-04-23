package org.codehaus.groovy.grails.plugins.web.filters;

import org.codehaus.groovy.grails.plugins.web.filters.DelegateMetaMethod.DelegateMetaMethodTargetStrategy;

class FilterConfigDelegateMetaMethodTargetStrategy implements DelegateMetaMethodTargetStrategy{
    public static final FilterConfigDelegateMetaMethodTargetStrategy instance=new FilterConfigDelegateMetaMethodTargetStrategy();
    
    public Object getTargetInstance(Object instance) {
        return ((FilterConfig)instance).getFiltersDefinition();
    }
}
