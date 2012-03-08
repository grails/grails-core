package org.codehaus.groovy.grails.web.binding;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.SourceUnit;

public interface ASTDatabindingHelper {
    void injectDatabindingCode(SourceUnit source, GeneratorContext context, ClassNode classNode);
}
