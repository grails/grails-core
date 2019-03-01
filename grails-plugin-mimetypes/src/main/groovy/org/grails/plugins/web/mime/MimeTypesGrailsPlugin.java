package org.grails.plugins.web.mime;

import groovy.lang.Closure;

/**
 * Provides content negotiation capabilities to Grails via a new withFormat method on controllers
 * as well as a format property on the HttpServletRequest instance.
 *
 * @author Graeme Rocher
 * @since 1.0
 * @deprecated Use {@link MimeTypesConfiguration} instead
 */
@Deprecated
public class MimeTypesGrailsPlugin extends AbstractMimeTypesGrailsPlugin {
    @Override
    public Closure doWithSpring() {
        return super.doWithSpring();
    }
}
