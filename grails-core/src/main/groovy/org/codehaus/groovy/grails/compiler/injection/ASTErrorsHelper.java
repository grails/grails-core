package org.codehaus.groovy.grails.compiler.injection;

import org.codehaus.groovy.ast.ClassNode;

/**
 * Enhances a class to contain an Errors property of type org.springframework.validation.Errors.  Methods added include:
 *
 *  <pre>
 *  public void setErrors(Errors errors)
 *  public Errors getErrors()
 *  public void clearErrors()
 *  public Boolean hasErrors()
 *  </pre>
 */
public interface ASTErrorsHelper {

    void injectErrorsCode(ClassNode classNode);
}
