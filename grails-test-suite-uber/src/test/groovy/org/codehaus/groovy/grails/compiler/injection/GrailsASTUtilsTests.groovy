package org.codehaus.groovy.grails.compiler.injection

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.builder.AstBuilder

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

    void testConstraintMetadata() {
        def result = new AstBuilder().buildFromString('''
            return {
                firstName bindable: true, size: 5..15
                def sb = new StringBuffer()

                // this should NOT be considered a constraint configuration method call because
                // it isn't a method invoked on "this"
                sb.append nullable: true, size: 115
                lastName bindable: false
            }
        ''')
        def closureExpression = result[0].statements[0].expression

        def constraintMetadata = GrailsASTUtils.getConstraintMetadata(closureExpression)
        assert 2 == constraintMetadata.size()

        def firstNameMetadata = constraintMetadata['firstName']
        assert 2 == firstNameMetadata.size()
        def firstNameBindableExpression = firstNameMetadata['bindable']
        assert true == firstNameBindableExpression.value
        def firstNameSizeExpression = firstNameMetadata['size']
        assert 5 == firstNameSizeExpression.from.value
        assert 15 == firstNameSizeExpression.to.value

        def lastNameMetaData = constraintMetadata['lastName']
        assert 1 == lastNameMetaData.size()
        def lastNameBindableExpression = lastNameMetaData['bindable']
        assert false == lastNameBindableExpression.value
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
