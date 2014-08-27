package org.grails.compiler.injection

import grails.artefact.Artefact
import grails.compiler.ast.ClassInjector
import grails.compiler.traits.TraitInjector
import grails.core.ArtefactHandler
import groovy.transform.CompilationUnitAware
import groovy.transform.CompileStatic

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.ModuleNode
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.control.CompilationUnit
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
class GlobalGrailsClassInjectorTransformation implements ASTTransformation, CompilationUnitAware {

    public static final ClassNode ARTEFACT_CLASS_NODE = new ClassNode(Artefact.class)
    CompilationUnit compilationUnit

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
        ClassInjector[] classInjectors = GrailsAwareInjectionOperation.getClassInjectors()

        GrailsAwareTraitInjectionOperation grailsTraitInjector = new GrailsAwareTraitInjectionOperation(compilationUnit)
        List<TraitInjector> allTraitInjectors = grailsTraitInjector.getTraitInjectors()

        Map<String, List<ClassInjector>> cache = new HashMap<String, List<ClassInjector>>().withDefault { String key ->
            ArtefactTypeAstTransformation.findInjectors(key, classInjectors)
        }

        Map<String, List<TraitInjector>> traitInjectorCache = new HashMap<String, List<TraitInjector>>().withDefault { String key ->
            List<TraitInjector> injectorsToUse = new ArrayList<TraitInjector>();
            for(TraitInjector injector : allTraitInjectors) {
                List<String> artefactTypes = Arrays.asList(injector.getArtefactTypes())
                if(artefactTypes.contains(key)) {
                    injectorsToUse.add(injector)
                }
            }
            injectorsToUse
        }

        for (ClassNode classNode : classes) {

            for(ArtefactHandler handler in artefactHandlers) {
                if(handler.isArtefact(classNode)) {
                    if(!classNode.getAnnotations(ARTEFACT_CLASS_NODE)) {
                        def annotationNode = new AnnotationNode(new ClassNode(Artefact.class))
                        annotationNode.addMember("value", new ConstantExpression(handler.getType()))
                        classNode.addAnnotation(annotationNode)

                        List<ClassInjector> injectors = cache[handler.type]
                        ArtefactTypeAstTransformation.performInjection(source, classNode, injectors)
                        
                        List<TraitInjector> traitInjectorsToUse = traitInjectorCache[handler.type]
                        grailsTraitInjector.performTraitInjection(source, classNode, traitInjectorsToUse)
                    }
                }
            }
        }
    }
}
