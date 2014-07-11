package org.grails.compiler.injection

import grails.artefact.Artefact
import grails.compiler.ast.ClassInjector
import grails.core.ArtefactHandler
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.ModuleNode
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.grails.core.io.support.GrailsFactoriesLoader
import org.grails.io.support.FileSystemResource
import org.grails.io.support.GrailsResourceUtils
import org.grails.io.support.Resource
import org.grails.io.support.UrlResource

/**
 * A global transformation that applies Grails' transformations to classes within a Grails project
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@GroovyASTTransformation(phase= CompilePhase.CANONICALIZATION)
@CompileStatic
class GlobalGrailsClassInjectorTransformation implements ASTTransformation {

    public static final ClassNode ARTEFACT_CLASS_NODE = new ClassNode(Artefact.class)

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {

        ModuleNode ast = source.getAST();
        List<ClassNode> classes = ast.getClasses();

        URL url = null
        final String filename = source.name
        Resource resource = new FileSystemResource(filename)
        if (resource.exists()) {
            try {
                url = resource.URL
            } catch (IOException e) {
                // ignore
            }
        }

        if(url == null || !GrailsResourceUtils.isGrailsResource(new UrlResource(url))) return


        List<ArtefactHandler> artefactHandlers = GrailsFactoriesLoader.loadFactories(ArtefactHandler)
        def classInjectors = GrailsAwareInjectionOperation.getClassInjectors()
        for (ClassNode classNode : classes) {

            for(ArtefactHandler handler in artefactHandlers) {
                if(handler.isArtefact(classNode)) {
                    if(!classNode.getAnnotations(ARTEFACT_CLASS_NODE)) {
                        def annotationNode = new AnnotationNode(new ClassNode(Artefact.class))
                        annotationNode.addMember("value", new ConstantExpression(handler.getType()))
                        classNode.addAnnotation(annotationNode)
                    }
                }
            }

            for(ClassInjector injector in classInjectors) {
                if(injector.shouldInject(url)) {
                    injector.performInjection(source, classNode)
                }
            }
        }
    }
}
