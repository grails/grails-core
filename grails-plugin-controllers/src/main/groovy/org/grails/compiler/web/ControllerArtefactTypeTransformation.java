/*
 * Copyright 2024 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

