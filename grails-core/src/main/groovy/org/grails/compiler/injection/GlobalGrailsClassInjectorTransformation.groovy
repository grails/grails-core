package org.grails.compiler.injection

import grails.artefact.Artefact
import grails.compiler.ast.ClassInjector
import grails.core.ArtefactHandler
import grails.io.IOUtils
import grails.plugins.metadata.GrailsPlugin
import grails.util.GrailsNameUtils
import groovy.transform.CompilationUnitAware
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.slurpersupport.GPathResult
import groovy.xml.MarkupBuilder
import groovy.xml.StreamingMarkupBuilder
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.grails.core.io.support.GrailsFactoriesLoader
import org.grails.io.support.AntPathMatcher
import org.grails.io.support.GrailsResourceUtils
import org.grails.io.support.UrlResource

import java.lang.reflect.Modifier

/**
 * A global transformation that applies Grails' transformations to classes within a Grails project
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@GroovyASTTransformation(phase= CompilePhase.CANONICALIZATION)
@CompileStatic
class GlobalGrailsClassInjectorTransformation implements ASTTransformation, CompilationUnitAware {


    public static final ClassNode ARTEFACT_HANDLER_CLASS = ClassHelper.make("grails.core.ArtefactHandler")
    public static final ClassNode APPLICATION_CONTEXT_COMMAND_CLASS = ClassHelper.make("grails.dev.commands.ApplicationCommand")
    public static final ClassNode TRAIT_INJECTOR_CLASS = ClassHelper.make("grails.compiler.traits.TraitInjector")

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {

        ModuleNode ast = source.getAST();
        List<ClassNode> classes = new ArrayList<>(ast.getClasses());

        URL url = GrailsASTUtils.getSourceUrl(source);

        if(url == null ) return
        if(!GrailsResourceUtils.isProjectSource(new UrlResource(url))) return;



        List<ArtefactHandler> artefactHandlers = GrailsFactoriesLoader.loadFactories(ArtefactHandler)
        ClassInjector[] classInjectors = GrailsAwareInjectionOperation.getClassInjectors()

        Map<String, List<ClassInjector>> cache = new HashMap<String, List<ClassInjector>>().withDefault { String key ->
            ArtefactTypeAstTransformation.findInjectors(key, classInjectors)
        }

        Set<String> transformedClasses = []
        String pluginVersion = null
        ClassNode pluginClassNode = null
        def compilationTargetDirectory = resolveCompilationTargetDirectory(source)
        def pluginXmlFile = new File(compilationTargetDirectory, "META-INF/grails-plugin.xml")

        for (ClassNode classNode : classes) {
            def projectName = classNode.getNodeMetaData("projectName")
            def projectVersion = classNode.getNodeMetaData("projectVersion")
            pluginVersion = projectVersion



            def classNodeName = classNode.name

            if(classNodeName.endsWith("GrailsPlugin")) {
                pluginClassNode = classNode
                if(projectVersion && !classNode.getProperty('version')) {
                    classNode.addProperty(new PropertyNode('version', Modifier.PUBLIC, ClassHelper.make(Object), classNode, new ConstantExpression(projectVersion.toString()) , null, null))
                }

                continue
            }


            if(updateGrailsFactoriesWithType(classNode, ARTEFACT_HANDLER_CLASS, compilationTargetDirectory)) {
                continue
            }
            if(updateGrailsFactoriesWithType(classNode, APPLICATION_CONTEXT_COMMAND_CLASS, compilationTargetDirectory)) {
                continue
            }
            if(updateGrailsFactoriesWithType(classNode, TRAIT_INJECTOR_CLASS, compilationTargetDirectory)) {
                continue
            }

            if(!GrailsResourceUtils.isGrailsResource(new UrlResource(url))) continue;


            if(projectName && projectVersion) {
                GrailsASTUtils.addAnnotationOrGetExisting(classNode, GrailsPlugin, [name: GrailsNameUtils.getPropertyNameForLowerCaseHyphenSeparatedName(projectName.toString()), version:projectVersion])
            }

            classNode.getModule().addImport("Autowired", ClassHelper.make("org.springframework.beans.factory.annotation.Autowired"))

            for(ArtefactHandler handler in artefactHandlers) {
                if(handler.isArtefact(classNode)) {
                    if(!classNode.getAnnotations(ARTEFACT_CLASS_NODE)) {
                        transformedClasses << classNodeName
                        def annotationNode = new AnnotationNode(new ClassNode(Artefact.class))
                        annotationNode.addMember("value", new ConstantExpression(handler.getType()))
                        classNode.addAnnotation(annotationNode)

                        List<ClassInjector> injectors = cache[handler.type]
                        ArtefactTypeAstTransformation.performInjection(source, classNode, injectors)
                        TraitInjectionUtils.processTraitsForNode(source, classNode, handler.getType(), compilationUnit)
                    }
                }
            }


            if(!transformedClasses.contains(classNodeName)) {
                def globalClassInjectors = GrailsAwareInjectionOperation.globalClassInjectors

                for(ClassInjector injector in globalClassInjectors) {
                    injector.performInjection(source, classNode)
                }
            }
        }


        // now create or update grails-plugin.xml
        // first check if plugin.xml exists
        pluginXmlFile.parentFile.mkdirs()

        generatePluginXml(pluginClassNode, pluginVersion, transformedClasses, pluginXmlFile)
    }

    static File resolveCompilationTargetDirectory(SourceUnit source) {
        File targetDirectory = source.configuration.targetDirectory
        if(targetDirectory==null && source.getClass().name == 'org.codehaus.jdt.groovy.control.EclipseSourceUnit') {
            targetDirectory = GroovyEclipseCompilationHelper.resolveEclipseCompilationTargetDirectory(source)
        }
        return targetDirectory
    }

    static boolean updateGrailsFactoriesWithType(ClassNode classNode, ClassNode superType, File compilationTargetDirectory) {
        if (GrailsASTUtils.isSubclassOfOrImplementsInterface(classNode, superType)) {
            def classNodeName = classNode.name
            // generate META-INF/grails.factories
            def factoriesFile = new File(compilationTargetDirectory, "META-INF/grails.factories")
            factoriesFile.parentFile.mkdirs()
            def props = new Properties()
            def superTypeName = superType.getName()
            if (factoriesFile.exists()) {
                // update
                factoriesFile.withInputStream { InputStream input ->
                    props.load(input)
                }

                def existing = props.getProperty(superTypeName)
                if (!existing.contains(classNodeName)) {
                    props.put(superTypeName, [existing, classNodeName].join(','))
                }
            } else {
                props.put(superTypeName, classNodeName)
            }
            factoriesFile.withWriter {  Writer writer ->
                props.store(writer, "Grails Factories File")
            }
            return true
        }
        return false
    }

    static Set<String> pendingPluginClasses = []
    static Collection<String> pluginExcludes = []

    protected static void generatePluginXml(ClassNode pluginClassNode, String pluginVersion, Set<String> transformedClasses, File pluginXmlFile) {
        def pluginXmlExists = pluginXmlFile.exists()
        Set pluginClasses = []
        pluginClasses.addAll(transformedClasses)
        pluginClasses.addAll(pendingPluginClasses)

        // if the class being transformed is a *GrailsPlugin class then if it doesn't exist create it
        if (pluginClassNode) {
            if (!pluginXmlExists) {
                writePluginXml(pluginClassNode, pluginVersion, pluginXmlFile, pluginClasses)
            } else {
                // otherwise if the file does exist, update it with the plugin name
                updatePluginXml(pluginClassNode, pluginVersion, pluginXmlFile, pluginClasses)
            }
        } else if (pluginXmlExists) {
            // if the class isn't the *GrailsPlugin class then only update the plugin.xml if it already exists
            updatePluginXml(null, pluginVersion, pluginXmlFile,  pluginClasses)
        } else {
            // otherwise add it to a list of pending classes to populated when the plugin.xml is created
            pendingPluginClasses.addAll(transformedClasses)
        }
    }

    @CompileDynamic
    static void writePluginXml(ClassNode pluginClassNode, String pluginVersion, File pluginXml, Collection<String> artefactClasses) {
        if(pluginClassNode) {
            PluginAstReader pluginAstReader = new PluginAstReader()
            def info = pluginAstReader.readPluginInfo(pluginClassNode)

            pluginXml.withWriter( "UTF-8") { Writer writer ->
                def mkp = new MarkupBuilder(writer)
                def pluginName = GrailsNameUtils.getLogicalPropertyName(pluginClassNode.name, "GrailsPlugin")

                def pluginProperties = info.getProperties()
                def excludes = pluginProperties.get('pluginExcludes')
                if(excludes instanceof List) {
                    pluginExcludes.clear()
                    pluginExcludes.addAll(excludes)
                }

                def grailsVersion = pluginProperties['grailsVersion'] ?: getClass().getPackage().getImplementationVersion() + " > *"
                mkp.plugin(name:pluginName, version: pluginVersion, grailsVersion: grailsVersion) {
                    type(pluginClassNode.name)

                    for(entry in pluginProperties) {
                        delegate."$entry.key"(entry.value)
                    }
                    // if there are pending classes to add to the plugin.xml add those
                    if(artefactClasses) {
                        def antPathMatcher = new AntPathMatcher()
                        resources {
                            for(String cn in artefactClasses) {
                                if (!pluginExcludes.any() { String exc -> antPathMatcher.match(exc, cn.replace('.','/')) }) {
                                    resource cn
                                }
                            }
                        }
                    }

                }
            }

            pendingPluginClasses.clear()
        }
    }

    @CompileDynamic
    static void updatePluginXml(ClassNode pluginClassNode, String pluginVersion, File pluginXmlFile, Collection<String> artefactClasses) {
        if(!artefactClasses) return

        try {
            XmlSlurper xmlSlurper = IOUtils.createXmlSlurper()

            def pluginXml = xmlSlurper.parse(pluginXmlFile)
            if(pluginClassNode) {
                def pluginName = GrailsNameUtils.getLogicalPropertyName(pluginClassNode.name, "GrailsPlugin")
                pluginXml.@name = pluginName
                pluginXml.@version = pluginVersion
                pluginXml.type = pluginClassNode.name

                PluginAstReader pluginAstReader = new PluginAstReader()
                def info = pluginAstReader.readPluginInfo(pluginClassNode)

                def pluginProperties = info.getProperties()
                def grailsVersion = pluginProperties['grailsVersion'] ?: getClass().getPackage().getImplementationVersion() + " > *"
                pluginXml.@grailsVersion = grailsVersion
                for(entry in pluginProperties) {
                    pluginXml."$entry.key" = entry.value
                }

                def excludes = pluginProperties.get('pluginExcludes')
                if(excludes instanceof List) {
                    pluginExcludes.clear()
                    pluginExcludes.addAll(excludes)
                }
            }

            def resources = pluginXml.resources

            for(String cn in artefactClasses) {
                if ( !resources.resource.find { it.text() == cn } ) {
                    resources.appendNode {
                        resource(cn)
                    }
                }
            }

            handleExcludes(pluginXml)

            Writable writable = new StreamingMarkupBuilder().bind {
                mkp.yield pluginXml
            }


            pluginXmlFile.withWriter("UTF-8") { Writer writer ->
                writable.writeTo(writer)
            }

            pendingPluginClasses.clear()

        } catch (e) {
            // corrupt, recreate
            writePluginXml(pluginClassNode, pluginXmlFile, artefactClasses)
        }
    }

    @CompileDynamic
    protected static void handleExcludes(GPathResult pluginXml) {
        if (pluginExcludes) {

            def antPathMatcher = new AntPathMatcher()
            pluginXml.resources.resource.each { res ->
                if (pluginExcludes.any() { String exc -> antPathMatcher.match(exc, res.text().replace('.','/')) }) {
                    res.replaceNode {}
                }
            }
        }
    }

    public static final ClassNode ARTEFACT_CLASS_NODE = new ClassNode(Artefact.class)

    CompilationUnit compilationUnit
}
