package org.grails.compiler.injection.test

import grails.boot.config.GrailsApplicationContextLoader
import grails.test.mixin.TestMixin
import grails.test.mixin.integration.Integration
import grails.util.BuildSettings
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotatedNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.grails.compiler.injection.GrailsASTUtils
import org.grails.io.support.MainClassFinder
import org.grails.test.context.junit4.GrailsJunit4ClassRunner
import org.grails.test.context.junit4.GrailsTestConfiguration
import org.junit.runner.RunWith
import org.springframework.boot.test.IntegrationTest
import org.springframework.boot.test.SpringApplicationConfiguration
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.util.ClassUtils

/*
 * Copyright 2014 original authors
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

/**
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class IntegrationTestMixinTransformation implements ASTTransformation {

    static final ClassNode MY_TYPE = new ClassNode(Integration.class);
    public static final ClassNode CONTEXT_CONFIG_ANNOTATION = ClassHelper.make(ContextConfiguration)
    public static final ClassNode GRAILS_APPLICATION_CONTEXT_LOADER = ClassHelper.make(GrailsApplicationContextLoader)
    public static final ClassNode WEB_APP_CONFIGURATION = ClassHelper.make(WebAppConfiguration)
    public static final ClassNode INTEGRATION_TEST_CLASS_NODE = ClassHelper.make(IntegrationTest)
    public static
    final ClassNode SPRING_APPLICATION_CONFIGURATION_CLASS_NODE = ClassHelper.make(GrailsTestConfiguration)
    public static final ClassNode RUN_WITH_ANNOTATION_NODE = ClassHelper.make(RunWith)
    public static final ClassNode SPRING_JUNIT4_CLASS_RUNNER = ClassHelper.make(GrailsJunit4ClassRunner)

    @Override
    void visit(ASTNode[] astNodes, SourceUnit source) {
        if (!(astNodes[0] instanceof AnnotationNode) || !(astNodes[1] instanceof AnnotatedNode)) {
            throw new RuntimeException("Internal error: wrong types: ${astNodes[0].getClass()} / ${astNodes[1].getClass()}")
        }

        AnnotatedNode parent = (AnnotatedNode) astNodes[1]
        AnnotationNode annotationNode = (AnnotationNode) astNodes[0]
        if (!MY_TYPE.equals(annotationNode.classNode) || !(parent instanceof ClassNode)) {
            return
        }

        ClassNode classNode = (ClassNode) parent
        def classesDir = BuildSettings.CLASSES_DIR
        Collection<File> searchDirs
        if(classesDir == null) {
            def tokens = System.getProperty('java.class.path').split(System.getProperty('path.separator'))
            def dirs = tokens.findAll() { String str -> !str.endsWith('.jar')}.collect() { String str -> new File(str)}
            searchDirs = dirs.findAll() { File f -> f.isDirectory() }

        }
        else {
            searchDirs = [classesDir]
        }

        String mainClass = null

        for(File dir in searchDirs) {
            mainClass = MainClassFinder.findMainClass(dir)
            if(mainClass) break
        }

        if(mainClass) {
            def applicationClassNode = ClassHelper.make(Thread.currentThread().contextClassLoader.loadClass(mainClass))

            if(TestMixinTransformation.isSpockTest(classNode)) {
                // first add context configuration
                // Example: @ContextConfiguration(loader = GrailsApplicationContextLoader, classes = Application)
                def contextConfigAnn = new AnnotationNode(CONTEXT_CONFIG_ANNOTATION)
                contextConfigAnn.addMember("loader", new ClassExpression(GRAILS_APPLICATION_CONTEXT_LOADER))
                contextConfigAnn.addMember("classes", new ClassExpression(applicationClassNode))
                classNode.addAnnotation(contextConfigAnn)


            }
            else {
                // Must be a JUnit 4 test so add JUnit spring annotations
                // @RunWith(SpringJUnit4ClassRunner)
                def runWithAnnotation = new AnnotationNode(RUN_WITH_ANNOTATION_NODE)
                runWithAnnotation.addMember("value", new ClassExpression(SPRING_JUNIT4_CLASS_RUNNER))
                classNode.addAnnotation(runWithAnnotation)

                // @SpringApplicationConfiguration(classes = Application)
                def contextConfigAnn = new AnnotationNode(SPRING_APPLICATION_CONFIGURATION_CLASS_NODE)
                contextConfigAnn.addMember("classes", new ClassExpression(applicationClassNode))
                classNode.addAnnotation(contextConfigAnn)
            }

            // now add integration test annotations
            // @WebAppConfiguration
            // @IntegrationTest

            if(GrailsASTUtils.isSubclassOf(applicationClassNode, "grails.boot.config.GrailsWebConfiguration")) {
                classNode.addAnnotation(new AnnotationNode(WEB_APP_CONFIGURATION))
                classNode.addAnnotation(new AnnotationNode(INTEGRATION_TEST_CLASS_NODE))
            }
            else {
                throw new RuntimeException("doesn't implement ${applicationClassNode.superClass}")
            }

        }

    }

}
