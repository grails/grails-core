package org.codehaus.groovy.grails.compiler.injection

import org.codehaus.groovy.ast.ClassNode

/**
 * @author Burt Beckwith
 */
class GrailsASTUtilsTests extends GroovyTestCase {

    void testGetFurthestParent() {
        def fooNode = new ClassNode(Foo)
        def barNode = new ClassNode(Bar)
        def bazNode = new ClassNode(Baz)

        assertEquals Foo.name, GrailsASTUtils.getFurthestParent(fooNode).name
        assertEquals Foo.name, GrailsASTUtils.getFurthestParent(barNode).name
        assertEquals Foo.name, GrailsASTUtils.getFurthestParent(bazNode).name
    }
}

class Foo {}

class Bar extends Foo {}

class Baz extends Bar {}
