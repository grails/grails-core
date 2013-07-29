package org.codehaus.groovy.grails.resolve.reporting

import org.codehaus.groovy.grails.resolve.Dependency
import spock.lang.Specification

/**
 */
class SimpleGraphRendererSpec extends Specification {

    void "Test render graph of dependencies to writer"() {
        given:"A graph renderer, a writer and a dependency graph"
            def writer = new StringWriter()
            def renderer = new SimpleGraphRenderer("compile", "Classpath for compiling sources")
            renderer.ansiEnabled = false
            def graph = buildGraph()
        when:"The graph is rendered"
            renderer.render(graph, writer)

        then:"The output is correct"
            writer.toString() ==
        '''
compile - Classpath for compiling sources
+--- org.grails:grails-core:2.3.0
|    \\--- org.grails:grails-bootstrap:2.3.0
|         \\--- org.apache.ant:ant:1.8.2

'''.denormalize()

    }

    private GraphNode buildGraph() {
        def root = new GraphNode()
        def grailsCore = new GraphNode(new Dependency("org.grails", "grails-core", "2.3.0"))
        root.children << grailsCore
        def child = new GraphNode(new Dependency("org.grails", "grails-bootstrap", "2.3.0"))
        child.children << new GraphNode(new Dependency("org.apache.ant", "ant", "1.8.2"))
        grailsCore.children << child

        return root
    }
}
