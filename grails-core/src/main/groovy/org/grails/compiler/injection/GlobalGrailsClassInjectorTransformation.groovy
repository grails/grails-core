package org.grails.compiler.injection

import grails.artefact.Artefact
import grails.compiler.ast.ClassInjector
import grails.compiler.traits.TraitInjector
import grails.core.ArtefactHandler
import grails.io.IOUtils
import grails.util.GrailsNameUtils
import groovy.transform.CompilationUnitAware
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.xml.MarkupBuilder
import groovy.xml.StreamingMarkupBuilder
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

    static Set<String> pendingPluginClasses = []

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

        Set<String> transformedClasses = []
        String pluginClassName = null
        for (ClassNode classNode : classes) {
            if(classNode.name.endsWith("GrailsPlugin")) {
                pluginClassName = classNode.name
                continue
            }
            for(ArtefactHandler handler in artefactHandlers) {
                if(handler.isArtefact(classNode)) {
                    if(!classNode.getAnnotations(ARTEFACT_CLASS_NODE)) {
                        transformedClasses << classNode.name
                        def annotationNode = new AnnotationNode(new ClassNode(Artefact.class))
                        annotationNode.addMember("value", new ConstantExpression(handler.getType()))
                        classNode.addAnnotation(annotationNode)

                        List<ClassInjector> injectors = cache[handler.type]
                        ArtefactTypeAstTransformation.performInjection(source, classNode, injectors)

                        List<TraitInjector> traitInjectorsToUse = traitInjectorCache[handler.type]
                        if(traitInjectorsToUse != null && traitInjectorsToUse.size() > 0) {
                            grailsTraitInjector.performTraitInjection(source, classNode, traitInjectorsToUse)
                        }
                    }
                }
            }


        }

        // now create or update grails-plugin.xml

        // first check if plugin.xml exists
        def compilationTargetDirectory = source.configuration.targetDirectory
        def pluginXmlFile = new File(compilationTargetDirectory, "META-INF/grails-plugin.xml")
        pluginXmlFile.parentFile.mkdirs()

        generatePluginXml(pluginClassName, transformedClasses, pluginXmlFile)
    }

    protected static void generatePluginXml(String pluginClassName, Set<String> transformedClasses, File pluginXmlFile) {
        def pluginXmlExists = pluginXmlFile.exists()
        Set pluginClasses = []
        pluginClasses.addAll(transformedClasses)
        pluginClasses.addAll(pendingPluginClasses)

        // if the class being transformed is a *GrailsPlugin class then if it doesn't exist create it
        if (pluginClassName) {
            if (!pluginXmlExists) {
                writePluginXml(pluginClassName, pluginXmlFile, pluginClasses)
            } else {
                // otherwise if the file does exist, update it with the plugin name
                updatePluginXml(pluginClassName, pluginXmlFile, pluginClasses)
            }
        } else if (pluginXmlExists) {
            // if the class isn't the *GrailsPlugin class then only update the plugin.xml if it already exists
            updatePluginXml(null, pluginXmlFile, pluginClasses)
        } else {
            // otherwise add it to a list of pending classes to populated when the plugin.xml is created
            pendingPluginClasses.addAll(transformedClasses)
        }
    }

    @CompileDynamic
    static void writePluginXml(String pluginClassName, File pluginXml, Collection<String> artefactClasses) {
        if(pluginClassName) {
            pluginXml.withWriter( "UTF-8") { Writer writer ->
                def mkp = new MarkupBuilder(writer)
                def pluginName = GrailsNameUtils.getLogicalPropertyName(pluginClassName, "GrailsPlugin")
                mkp.plugin(name:pluginName) {
                    type(pluginClassName)

                    // if there are pending classes to add to the plugin.xml add those
                    if(artefactClasses) {
                        resources {
                            for(String cn in artefactClasses) {
                                resource cn
                            }
                        }
                    }
                }
            }

            pendingPluginClasses.clear()
        }
    }

    @CompileDynamic
    static void updatePluginXml(String pluginClassName, File pluginXmlFile, Collection<String> artefactClasses) {
        if(!pluginClassName && !artefactClasses) return

        try {
            XmlSlurper xmlSlurper = IOUtils.createXmlSlurper()

            def pluginXml = xmlSlurper.parse(pluginXmlFile)
            if(pluginClassName) {
                def pluginName = GrailsNameUtils.getLogicalPropertyName(pluginClassName, "GrailsPlugin")
                pluginXml.@name = pluginName
                pluginXml.type = pluginClassName
            }

            def resources = pluginXml.resources

            for(String cn in artefactClasses) {
                if ( !resources.resource.find { it.text() == cn } ) {
                    resources.appendNode {
                        resource(cn)
                    }
                }
            }

            Writable writable = new StreamingMarkupBuilder().bind {
                mkp.yield pluginXml
            }


            pluginXmlFile.withWriter("UTF-8") { Writer writer ->
                writable.writeTo(writer)
            }

            pendingPluginClasses.clear()

        } catch (e) {
            // corrupt, recreate
            writePluginXml(pluginClassName, pluginXmlFile, artefactClasses)
        }
    }

    public static final ClassNode ARTEFACT_CLASS_NODE = new ClassNode(Artefact.class)

    CompilationUnit compilationUnit
}
