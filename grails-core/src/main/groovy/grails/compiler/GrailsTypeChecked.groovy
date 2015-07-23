package grails.compiler

import groovy.transform.AnnotationCollector
import groovy.transform.TypeChecked

/**
 *
 * @since 2.4
 *
 */
@AnnotationCollector
@TypeChecked(extensions=['org.grails.compiler.ValidateableTypeCheckingExtension',
                         'org.grails.compiler.HttpServletRequestTypeCheckingExtension',
                         'org.grails.compiler.DynamicFinderTypeCheckingExtension',
                         'org.grails.compiler.DomainMappingTypeCheckingExtension',
                         'org.grails.compiler.RelationshipManagementMethodTypeCheckingExtension'])
@interface GrailsTypeChecked {

}
