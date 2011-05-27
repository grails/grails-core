package org.codehaus.groovy.grails.compiler.web.taglib;

import org.codehaus.groovy.grails.commons.ControllerArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsResourceUtils;
import org.codehaus.groovy.grails.compiler.injection.AbstractGrailsArtefactTransformer;
import org.codehaus.groovy.grails.compiler.injection.AstTransformer;
import org.codehaus.groovy.grails.plugins.web.api.ControllerTagLibraryApi;

import java.net.URL;
import java.util.regex.Pattern;

/**
 * Enhances controller classes with a method missing implementation for tags.
 *
 * @author Graeme Rocher
 * @since 1.4
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
    public String getArtefactType() {
        return ControllerArtefactHandler.TYPE;
    }
}
