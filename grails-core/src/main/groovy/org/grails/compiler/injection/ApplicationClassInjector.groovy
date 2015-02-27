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
import grails.io.ResourceUtils
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.ListExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.ReturnStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.classgen.GeneratorContext
import org.codehaus.groovy.control.SourceUnit
import org.grails.core.artefact.ApplicationArtefactHandler
import org.grails.io.support.GrailsResourceUtils
import org.grails.io.support.UrlResource
import org.springframework.util.ClassUtils

import java.lang.reflect.Modifier
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

                def packageNamesMethod = classNode.getMethod('packageNames', GrailsASTUtils.ZERO_PARAMETERS)

                if(packageNamesMethod == null || packageNamesMethod.declaringClass != classNode) {
                    def collectionClassNode = GrailsASTUtils.replaceGenericsPlaceholders(ClassHelper.make(Collection), [E: ClassHelper.make(String)])

                    def packageNamesBody = new BlockStatement()
                    def grailsAppDir = GrailsResourceUtils.getAppDir(new UrlResource(GrailsASTUtils.getSourceUrl(source)))
                    if(grailsAppDir.exists()) {

                        def packageNames = ResourceUtils.getProjectPackageNames(grailsAppDir.file.parentFile).collect() { String str -> new ConstantExpression(str) }
                        packageNamesBody.addStatement(new ReturnStatement(new ExpressionStatement(new ListExpression(packageNames.toList()))))
                        classNode.addMethod("packageNames", Modifier.PUBLIC, collectionClassNode, ZERO_PARAMETERS, null, packageNamesBody)
                    }
                }

                def classLoader = getClass().classLoader
                if(ClassUtils.isPresent('javax.servlet.ServletContext', classLoader)) {
                    GrailsASTUtils.addAnnotationOrGetExisting(classNode, ClassHelper.make(classLoader.loadClass('org.springframework.web.servlet.config.annotation.EnableWebMvc')))
                }
                if(ClassUtils.isPresent('org.springframework.boot.autoconfigure.EnableAutoConfiguration', classLoader) ) {
                    def enableAutoConfigurationAnnotation = GrailsASTUtils.addAnnotationOrGetExisting(classNode, ClassHelper.make(classLoader.loadClass('org.springframework.boot.autoconfigure.EnableAutoConfiguration')))
                    if(ClassUtils.isPresent('org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration', classLoader)) {
                        def dataSourceAutoConfig = new ClassExpression(ClassHelper.make(classLoader.loadClass('org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration')))
                        GrailsASTUtils.addExpressionToAnnotationMember(enableAutoConfigurationAnnotation, "exclude", dataSourceAutoConfig)
                    }
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
