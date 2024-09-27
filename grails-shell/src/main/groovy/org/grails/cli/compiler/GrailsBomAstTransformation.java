package org.grails.cli.compiler;

import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.transform.GroovyASTTransformation;
@GroovyASTTransformation(phase = CompilePhase.CONVERSION)
public class GrailsBomAstTransformation extends GenericBomAstTransformation {

    @Override
    protected String getBomModule() {
        return "grails-bom";
    }

    @Override
    public int getOrder() {
        return 0;
    }

}