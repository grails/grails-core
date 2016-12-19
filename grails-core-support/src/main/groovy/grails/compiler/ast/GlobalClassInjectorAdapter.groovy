package grails.compiler.ast

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.classgen.GeneratorContext
import org.codehaus.groovy.control.SourceUnit

/**
 * Helper super class to ease the creation of {@link AllArtefactClassInjector} implementations
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
abstract class GlobalClassInjectorAdapter implements GlobalClassInjector {

    Set<String> processesClassNames = []

    @Override
    void performInjection(SourceUnit source, GeneratorContext context, ClassNode classNode) {
        performInjectionOnAnnotatedClass(source, classNode)
    }

    @Override
    void performInjection(SourceUnit source, ClassNode classNode) {
        performInjectionOnAnnotatedClass(source, classNode)
    }

    @Override
    void performInjectionOnAnnotatedClass(SourceUnit source, ClassNode classNode) {
        def className = classNode.name
        if(!processesClassNames.contains(className)) {
            performInjectionInternal source, classNode
            processesClassNames.add className
        }
    }

    abstract void performInjectionInternal(SourceUnit source, ClassNode classNode)

    @Override
    boolean shouldInject(URL url) {
        return true
    }
}
