package org.codehaus.groovy.grails.compiler.web;


import java.net.URL;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;
import org.codehaus.groovy.grails.compiler.injection.AbstractGrailsArtefactTransformer;
import org.codehaus.groovy.grails.compiler.injection.AstTransformer;
import org.codehaus.groovy.grails.io.support.GrailsResourceUtils;
import org.codehaus.groovy.grails.plugins.web.api.ControllersDomainBindingApi;
import org.codehaus.groovy.grails.web.binding.DefaultASTDatabindingHelper;

/**
 * Adds binding methods to domain classes.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
@AstTransformer
public class ControllerDomainTransformer extends AbstractGrailsArtefactTransformer {

    @Override
    public String getArtefactType() {
        return DomainClassArtefactHandler.TYPE;
    }

    @Override
    public Class<?> getInstanceImplementation() {
        return ControllersDomainBindingApi.class;
    }

    @Override
    protected boolean isCandidateInstanceMethod(ClassNode classNode, MethodNode declaredMethod) {
        return false; // don't include instance methods
    }

    @Override
    public Class<?> getStaticImplementation() {
        return null;  // no static methods
    }

    @Override
    protected boolean requiresAutowiring() {
        return false;
    }

    public boolean shouldInject(URL url) {
        return GrailsResourceUtils.isDomainClass(url);
    }
    
    @Override
    public void performInjection(final SourceUnit source, final GeneratorContext context, final ClassNode classNode) {
        super.performInjection(source,  context, classNode);
        new DefaultASTDatabindingHelper().injectDatabindingCode(source, context, classNode);
    }
}
