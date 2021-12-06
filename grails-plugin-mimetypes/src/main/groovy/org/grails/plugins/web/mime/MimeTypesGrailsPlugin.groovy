package org.grails.plugins.web.mime
/**
 * Provides content negotiation capabilities to Grails via a new withFormat method on controllers
 * as well as a format property on the HttpServletRequest instance.
 *
 * @author Graeme Rocher
 * @since 1.0
 * @deprecated Use {@link MimeTypesConfiguration} instead
 */
@Deprecated
class MimeTypesGrailsPlugin extends AbstractMimeTypesGrailsPlugin {
    @Override
    Closure doWithSpring() {
        return super.doWithSpring();
    }
}
