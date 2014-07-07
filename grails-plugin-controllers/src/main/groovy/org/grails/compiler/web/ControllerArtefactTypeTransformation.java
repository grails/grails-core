package org.grails.compiler.web;

import grails.web.Controller;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.grails.core.artefact.ControllerArtefactHandler;
import org.grails.compiler.injection.ArtefactTypeAstTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;

/**
 * A transformation that makes an Artefact a controller
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class ControllerArtefactTypeTransformation extends ArtefactTypeAstTransformation {
    @Override
    protected String resolveArtefactType(SourceUnit sourceUnit, AnnotationNode annotationNode, ClassNode classNode) {
        return ControllerArtefactHandler.TYPE;
    }

    @Override
    protected Class getAnnotationTypeClass() {
        return Controller.class;
    }
}

