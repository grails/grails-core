package grails.web.mapping

import groovy.transform.CompileStatic
import org.grails.web.mapping.DefaultUrlMappingEvaluator
import org.grails.web.mapping.DefaultUrlMappingsHolder
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

/**
 * Helper class for creating a {@link UrlMapping}. Useful for testing
 *
 * @author Graeme Rocher
     * @since 3.1
 */
@CompileStatic
class UrlMappingsFactory implements ApplicationContextAware {

    ApplicationContext applicationContext

    UrlMappings create(Closure mappingsClosure) {
        def evaluator = new DefaultUrlMappingEvaluator(applicationContext)
        return new DefaultUrlMappingsHolder(evaluator.evaluateMappings(mappingsClosure))
    }

    UrlMappings create(Class mappingsClass) {
        def evaluator = new DefaultUrlMappingEvaluator(applicationContext)
        return new DefaultUrlMappingsHolder(evaluator.evaluateMappings(mappingsClass))
    }
}
