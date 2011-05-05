package org.codehaus.groovy.grails.compiler.web;

import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsResourceUtils;
import org.codehaus.groovy.grails.compiler.injection.AbstractGrailsArtefactTransformer;
import org.codehaus.groovy.grails.compiler.injection.AstTransformer;
import org.codehaus.groovy.grails.plugins.web.api.ControllersDomainBindingApi;

import java.net.URL;

/**
 * A transformer that adds binding methods to domain classes
 *
 * @author Graeme Rocher
 * @since 1.4
 */
@AstTransformer
public class ControllerDomainTransformer extends AbstractGrailsArtefactTransformer{

    @Override
    public String getArtefactType() {
        return DomainClassArtefactHandler.TYPE;
    }

    @Override
    public Class getInstanceImplementation() {
        return ControllersDomainBindingApi.class;
    }

    @Override
    public Class getStaticImplementation() {
        return null;  // no static methods
    }

    @Override
    protected boolean requiresAutowiring() {
        return false;
    }

    @Override
    public boolean shouldInject(URL url) {
        return GrailsResourceUtils.isDomainClass(url);
    }
}
