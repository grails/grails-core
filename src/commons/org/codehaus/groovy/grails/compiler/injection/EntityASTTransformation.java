/* Copyright 2004-2005 Graeme Rocher
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

import grails.persistence.Entity;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;

/**
 * An AST transformation that injects the necessary fields and behaviors into a domain
 * class in order to make it a property GORM entity
 * 
 * @author Graeme Rocher
 * @since 1.1
 *
 *        <p/>
 *        Created: Dec 17, 2008
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class EntityASTTransformation implements ASTTransformation{

    private static final Class MY_CLASS = Entity.class;
    private static final ClassNode MY_TYPE = new ClassNode(MY_CLASS);
    private static final String MY_TYPE_NAME = "@" + MY_TYPE.getNameWithoutPackage();

    public void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {
        if (!(astNodes[0] instanceof AnnotationNode) || !(astNodes[1] instanceof AnnotatedNode)) {
            throw new RuntimeException("Internal error: wrong types: $node.class / $parent.class");
        }

        AnnotatedNode parent = (AnnotatedNode) astNodes[1];
        AnnotationNode node = (AnnotationNode) astNodes[0];
        if (!MY_TYPE.equals(node.getClassNode())) return;

        if(parent instanceof ClassNode) {
            ClassNode cNode = (ClassNode) parent;
            String cName = cNode.getName();
            if (cNode.isInterface()) {
                throw new RuntimeException("Error processing interface '" + cName + "'. " + MY_TYPE_NAME + " not allowed for interfaces.");
            }

            GrailsDomainClassInjector domainInjector = new DefaultGrailsDomainClassInjector();
            domainInjector.performInjectionOnAnnotatedEntity(cNode);
        }

    }
}
