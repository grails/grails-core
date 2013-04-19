package org.codehaus.groovy.grails.compiler.web.taglib;

import java.lang.Override;
import java.net.URL;
import java.util.regex.Pattern;

import grails.web.controllers.ControllerMethod;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.grails.commons.ControllerArtefactHandler;
import org.codehaus.groovy.grails.compiler.injection.AbstractGrailsArtefactTransformer;
import org.codehaus.groovy.grails.compiler.injection.AstTransformer;
import org.codehaus.groovy.grails.io.support.GrailsResourceUtils;
import org.codehaus.groovy.grails.plugins.web.api.ControllerTagLibraryApi;

/**
 * Enhances controller classes with a method missing implementation for tags.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
@AstTransformer
public class ControllerTagLibraryTransformer extends AbstractGrailsArtefactTransformer {

    public static Pattern CONTROLLER_PATTERN = Pattern.compile(".+/" +
            GrailsResourceUtils.GRAILS_APP_DIR + "/controllers/(.+)Controller\\.groovy");

    @Override
    public Class<?> getInstanceImplementation() {
        return ControllerTagLibraryApi.class;
    }

    @Override
    public Class<?> getStaticImplementation() {
        return null;  // No static api
    }

    public boolean shouldInject(URL url) {
        return url != null && CONTROLLER_PATTERN.matcher(url.getFile()).find();
    }


    @Override
    protected AnnotationNode getMarkerAnnotation() {
        return new AnnotationNode(new ClassNode(ControllerMethod.class).getPlainNodeReference());
    }

    @Override
    public String getArtefactType() {
        return ControllerArtefactHandler.TYPE;
    }
}
