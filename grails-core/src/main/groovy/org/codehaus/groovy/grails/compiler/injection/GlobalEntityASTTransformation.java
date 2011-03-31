/*
 * Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.compiler.injection;

import java.util.List;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;

/**
 * A global AST transformation that injects methods and properties into classes in the grails-app/domain directory.
 *
 * @author Graeme Rocher
 * @since 1.2
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class GlobalEntityASTTransformation implements ASTTransformation {

    private GrailsDomainClassInjector domainClassInjector = new DefaultGrailsDomainClassInjector();

    public void visit(ASTNode[] nodes, SourceUnit source) {

        ASTNode astNode = nodes[0];

        if (!(astNode instanceof ModuleNode)) {
            return;
        }

        ModuleNode moduleNode = (ModuleNode) astNode;

        List<ClassNode> classes = moduleNode.getClasses();
        if (classes.size() > 0) {
            domainClassInjector.performInjection(source, classes.get(0));
        }
    }
}
