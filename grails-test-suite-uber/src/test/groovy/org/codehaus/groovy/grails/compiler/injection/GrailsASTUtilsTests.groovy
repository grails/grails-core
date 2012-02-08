package org.codehaus.groovy.grails.compiler.injection


import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

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
    
    void testHasAnyAnnotations() {
        def widgetNode = new ClassNode(Widget)
        assert GrailsASTUtils.hasAnyAnnotations(widgetNode, FirstAnnotation, SecondAnnotation)
        assert GrailsASTUtils.hasAnyAnnotations(widgetNode, FirstAnnotation, SecondAnnotation, ThirdAnnotation)
        assert GrailsASTUtils.hasAnyAnnotations(widgetNode, FirstAnnotation, SecondAnnotation, ThirdAnnotation, FourthAnnotation)
        assert GrailsASTUtils.hasAnyAnnotations(widgetNode, SecondAnnotation, ThirdAnnotation, FourthAnnotation)
        assert !GrailsASTUtils.hasAnyAnnotations(widgetNode, ThirdAnnotation, FourthAnnotation)
        assert !GrailsASTUtils.hasAnyAnnotations(widgetNode, FourthAnnotation)
        assert !GrailsASTUtils.hasAnyAnnotations(widgetNode)
    }
    
    void testHasAnnotation() {
        def widgetNode = new ClassNode(Widget)
        assert GrailsASTUtils.hasAnnotation(widgetNode, FirstAnnotation)
        assert GrailsASTUtils.hasAnnotation(widgetNode, SecondAnnotation)
        assert !GrailsASTUtils.hasAnnotation(widgetNode, ThirdAnnotation)
        assert !GrailsASTUtils.hasAnnotation(widgetNode, FourthAnnotation)
    }
}

class Foo {}

class Bar extends Foo {}

class Baz extends Bar {}

@Retention(RetentionPolicy.RUNTIME)
@interface FirstAnnotation{}
@Retention(RetentionPolicy.RUNTIME)
@interface SecondAnnotation{}
@Retention(RetentionPolicy.RUNTIME)
@interface ThirdAnnotation{}
@Retention(RetentionPolicy.RUNTIME)
@interface FourthAnnotation{}

@FirstAnnotation
@SecondAnnotation
class Widget {}