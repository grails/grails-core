package org.codehaus.groovy.grails.compiler.injection

import grails.compiler.ast.AstTransformer

/**
 * Default implementation of domain class injector interface that adds the 'id'
 * and 'version' properties and other previously boilerplate code.
 *
 * @author Graeme Rocher
 * @since 0.2
 */
@AstTransformer
@Deprecated
class DefaultGrailsDomainClassInjector extends org.grails.compiler.injection.DefaultGrailsDomainClassInjector implements GrailsDomainClassInjector{
}
