package org.grails.compiler.injection

import grails.artefact.Artefact
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.PropertyExpression
import org.grails.core.artefact.ControllerArtefactHandler
import spock.lang.Specification

/**
 * @author James Kleeh
 */
class ArtefactTypeAstTransformationSpec extends Specification {


    void "test resolveArtefactType with string literal"() {
        given:
        ArtefactTypeAstTransformation ast = new ArtefactTypeAstTransformation()
        ClassNode classNode = ClassHelper.make(Object)
        AnnotationNode annotationNode = new AnnotationNode(ClassHelper.make(Artefact))
        annotationNode.addMember("value", new ConstantExpression("ABC"))

        when:
        String returnValue = ast.resolveArtefactType(null, annotationNode, classNode)

        then:
        returnValue == "ABC"
    }

    void "test resolveArtefactType with property expression"() {
        given:
        ArtefactTypeAstTransformation ast = new ArtefactTypeAstTransformation()
        ClassNode classNode = ClassHelper.make(Object)
        AnnotationNode annotationNode = new AnnotationNode(ClassHelper.make(Artefact))
        annotationNode.addMember("value",
                new PropertyExpression(
                        new ClassExpression(ClassHelper.make(ControllerArtefactHandler)), "TYPE"))

        when:
        String returnValue = ast.resolveArtefactType(null, annotationNode, classNode)

        then:
        returnValue == "Controller"
    }

    void "test resolveArtefactType with null"() {
        given:
        ArtefactTypeAstTransformation ast = new ArtefactTypeAstTransformation()
        ClassNode classNode = ClassHelper.make(Object)
        AnnotationNode annotationNode = new AnnotationNode(ClassHelper.make(Artefact))
        annotationNode.addMember("value", null)

        when:
        ast.resolveArtefactType(null, annotationNode, classNode)

        then:
        thrown(RuntimeException)
    }


    //Inclusion to verify compilation
    @Artefact("Controller")
    class Test {
    }

    @Artefact(ControllerArtefactHandler.TYPE)
    class Test2 {

    }

}
