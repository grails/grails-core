/*
 * Copyright 2014 the original author or authors.
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
package org.grails.compiler.injection.test;

import grails.test.runtime.FreshRuntime;

import java.util.List;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.grails.compiler.injection.GrailsASTUtils;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;

/**
 * 
 * @author Jeff Brown
 * @since 2.4.4
 *
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class DirtiesRuntimeTransformation implements ASTTransformation {

    @Override
    public void visit(final ASTNode[] astNodes, final SourceUnit source) {
        if (!(astNodes[0] instanceof AnnotationNode) || !(astNodes[1] instanceof MethodNode)) {
            throw new RuntimeException("Internal error: wrong types: $node.class / $parent.class");
        }
        
        final MethodNode methodNode = (MethodNode)astNodes[1];
        final ClassNode declaringClassNode = methodNode.getDeclaringClass();
        if(declaringClassNode != null) {
            final List<AnnotationNode> annotations = declaringClassNode.getAnnotations(ClassHelper.make(FreshRuntime.class));
            if(annotations != null && annotations.size() > 0) {
                final String message = "The [" + methodNode.getName() + "] method in [" +
                                 declaringClassNode.getName() + "] is marked with @DirtiesRuntime.  " +
                                 "There is no need to mark a test method with @DirtiesRuntime " +
                                 "inside of a test class which is marked with @FreshRuntime.";
                GrailsASTUtils.warning(source, methodNode, message);
            }
        }
    }
}
