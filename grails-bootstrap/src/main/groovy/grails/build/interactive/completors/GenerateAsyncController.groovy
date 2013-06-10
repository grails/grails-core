package grails.build.interactive.completors

import org.codehaus.groovy.grails.cli.interactive.completors.ClassNameCompletor
import org.codehaus.groovy.grails.io.support.GrailsResourceUtils
import org.codehaus.groovy.grails.io.support.Resource

/**
 * Completor for the generate-controller command.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
class GenerateAsyncController extends ClassNameCompletor {

    @Override
    String getCommandName() { "generate-async-controller" }

    @Override
    boolean shouldInclude(Resource res) {
        GrailsResourceUtils.isDomainClass(res.getURL())
    }
}
