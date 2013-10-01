import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.grails.compiler.injection.GrailsASTUtils

methodNotFound { receiver, name, argumentList, argTypes, call ->
    def result
    if(receiver.typeClass == Class &&
    receiver.isUsingGenerics() &&
    GrailsASTUtils.isDomainClass(receiver.genericsTypes[0].type, null)) {
        if(call.methodAsString.startsWith('findAllBy')) {
            def listNode = new ClassNode(List)
            result = makeDynamic(call, listNode)
        } else if(call.methodAsString.startsWith('countBy')) {
            result = makeDynamic(call, int_TYPE)
        }
    }
    return result
}
