package org.codehaus.groovy.grails.web.filters

import org.codehaus.groovy.grails.plugins.web.filters.FilterConfig


/**
 * @author Graeme Rocher
 * @deprecated Use {@link org.grails.plugins.web.filters.GrailsFiltersClass} instead
 */
@Deprecated
public interface GrailsFiltersClass extends org.grails.plugins.web.filters.GrailsFiltersClass{

    @Override
    List<? extends FilterConfig> getConfigs(Object filterInstance)
}