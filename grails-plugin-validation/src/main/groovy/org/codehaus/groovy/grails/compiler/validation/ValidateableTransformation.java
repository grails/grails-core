package org.codehaus.groovy.grails.compiler.validation;

import grails.validation.ASTValidateableHelper;
import grails.validation.DefaultASTValidateableHelper;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;

@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class ValidateableTransformation implements ASTTransformation{

    private static final ClassNode ORIGINAL_VALIDATEABLE_CLASS_NODE = ClassHelper.make(org.codehaus.groovy.grails.validation.Validateable.class).getPlainNodeReference();
    private static final ClassNode NEW_VALIDATEABLE_CLASS_NODE = ClassHelper.make(grails.validation.Validateable.class).getPlainNodeReference();


    public void visit(ASTNode[] astNodes, SourceUnit source) {
        if (!(astNodes[0] instanceof AnnotationNode) || !(astNodes[1] instanceof AnnotatedNode)) {
            throw new RuntimeException("Internal error: wrong types: $node.class / $parent.class");
        }
        if (!(astNodes[0] instanceof AnnotationNode) || !(astNodes[1] instanceof AnnotatedNode)) {
            throw new RuntimeException("Internal error: wrong types: $node.class / $parent.class");
        }

        AnnotatedNode parent = (AnnotatedNode) astNodes[1];
        AnnotationNode node = (AnnotationNode) astNodes[0];
        if ( (!NEW_VALIDATEABLE_CLASS_NODE.equals(node.getClassNode()) && !ORIGINAL_VALIDATEABLE_CLASS_NODE.equals(node.getClassNode())) || !(parent instanceof ClassNode)) {
            return;
        }

        ClassNode cNode = (ClassNode) parent;
        String cName = cNode.getName();
        if (cNode.isInterface()) {
            throw new RuntimeException("Error processing interface '" + cName + "'.  @Validateable not allowed for interfaces.");
        }

        ASTValidateableHelper helper = new DefaultASTValidateableHelper();
        helper.injectValidateableCode(cNode);
    }

}
