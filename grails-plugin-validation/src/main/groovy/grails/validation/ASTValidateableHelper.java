package grails.validation;

import org.codehaus.groovy.ast.ClassNode;

public interface ASTValidateableHelper {
    void injectValidateableCode(ClassNode classNode);
}
