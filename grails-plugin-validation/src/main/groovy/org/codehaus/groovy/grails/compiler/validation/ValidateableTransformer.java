package org.codehaus.groovy.grails.compiler.validation;

import grails.validation.ASTValidateableHelper;
import grails.validation.DefaultASTValidateableHelper;

import java.net.URL;
import java.util.List;

import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.grails.compiler.injection.AstTransformer;
import org.codehaus.groovy.grails.compiler.injection.ClassInjector;

@AstTransformer
public class ValidateableTransformer implements ClassInjector{

    private static final ClassNode ORIGINAL_VALIDATEABLE_CLASS_NODE = new ClassNode(org.codehaus.groovy.grails.validation.Validateable.class);
    private static final ClassNode NEW_VALIDATEABLE_CLASS_NODE = new ClassNode(grails.validation.Validateable.class);

    public void performInjection(SourceUnit source, GeneratorContext context,
            ClassNode classNode) {
        List<AnnotationNode> annotations = classNode.getAnnotations(NEW_VALIDATEABLE_CLASS_NODE);
        if(annotations.size() == 0) {
            annotations = classNode.getAnnotations(ORIGINAL_VALIDATEABLE_CLASS_NODE);
            if(annotations.size() == 0) {
                return;
            }
        }
        ASTValidateableHelper helper = new DefaultASTValidateableHelper();
        helper.injectValidateableCode(classNode);
    }

    public void performInjection(SourceUnit source, ClassNode classNode) {
        performInjection(source, null, classNode);
    }

    public boolean shouldInject(URL url) {
        return true;
    }

}
