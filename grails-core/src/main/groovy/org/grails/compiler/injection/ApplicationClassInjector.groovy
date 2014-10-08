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
package org.grails.compiler.injection

import grails.compiler.ast.AstTransformer
import grails.compiler.ast.GrailsArtefactClassInjector
import grails.dev.Support
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.classgen.GeneratorContext
import org.codehaus.groovy.control.SourceUnit
import org.grails.core.artefact.ApplicationArtefactHandler
import org.grails.io.support.GrailsResourceUtils
import org.grails.io.support.UrlResource
import org.springframework.util.ClassUtils

/**
 * Injector for the 'Application' class
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
@AstTransformer
class ApplicationClassInjector implements GrailsArtefactClassInjector {

    ApplicationArtefactHandler applicationArtefactHandler = new ApplicationArtefactHandler()

    private static final List<Integer> transformedInstances = []

    @Override
    String[] getArtefactTypes() {
        return [ApplicationArtefactHandler.TYPE] as String[]
    }

    @Override
    void performInjection(SourceUnit source, GeneratorContext context, ClassNode classNode) {
        performInjection(source, classNode)
    }

    @Override
    void performInjection(SourceUnit source, ClassNode classNode) {
        performInjectionOnAnnotatedClass(source, classNode)
    }

    @Override
    @CompileDynamic
    void performInjectionOnAnnotatedClass(SourceUnit source, ClassNode classNode) {
        if(applicationArtefactHandler.isArtefact(classNode)) {
            def objectId = Integer.valueOf( System.identityHashCode(classNode) )
            if(!transformedInstances.contains(objectId)) {
                transformedInstances << objectId

                def enableAgentMethodCall = new MethodCallExpression(new ClassExpression(ClassHelper.make(Support)), "enableAgentIfNotPresent", GrailsASTUtils.ZERO_ARGUMENTS)
                def methodCallStatement = new ExpressionStatement(enableAgentMethodCall)
                List<Statement> statements = [ methodCallStatement ]
                classNode.addStaticInitializerStatements(statements, false)

                def classLoader = getClass().classLoader
                if(ClassUtils.isPresent('org.springframework.boot.autoconfigure.EnableAutoConfiguration', classLoader) ) {
                    GrailsASTUtils.addAnnotationIfNecessary(classNode, classLoader.loadClass('org.springframework.boot.autoconfigure.EnableAutoConfiguration'))
                }
            }
        }
    }

    @Override
    boolean shouldInject(URL url) {
        if(url == null) return false
        def res = new UrlResource(url)
        return GrailsResourceUtils.isGrailsResource(res) && res.filename == "Application.groovy"
    }
}
