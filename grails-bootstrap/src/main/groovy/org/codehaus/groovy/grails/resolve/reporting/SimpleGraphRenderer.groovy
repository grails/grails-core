/*
 * Copyright 2013 the original author or authors.
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
package org.codehaus.groovy.grails.resolve.reporting

import static org.fusesource.jansi.Ansi.Color.*
import grails.build.logging.GrailsConsole
import groovy.transform.CompileStatic

import org.fusesource.jansi.Ansi

/**
 * A graph renderer that outputs a dependency graph to system out or the given writer.
 *
 * @since 2.3
 * @author Graeme Rocher
 */
@CompileStatic
class SimpleGraphRenderer implements DependencyGraphRenderer {
    private static final GrailsConsole CONSOLE = GrailsConsole.getInstance()
    private static final String TOP_LEVEL_PREFIX = "+--- "
    private static final String UNRESOLVED_PREFIX = ">>>> "
    private static final String INITIAL_TRANSITIVE_PREFIX = '|    '
    private static final String PADDING = "     "
    private static final String TRANSITIVE_PREFIX = '\\--- '

    boolean ansiEnabled = true
    String scope
    String description

    SimpleGraphRenderer(String scope, String description) {
        this.scope = scope
        this.description = description
    }

    void render(GraphNode root, Writer writer) {
        def pw = new PrintWriter(writer)

        pw.println()
        if (ansiEnabled && CONSOLE.isAnsiEnabled()) {
            pw.println(new Ansi().a(Ansi.Attribute.INTENSITY_BOLD).fg(GREEN).a(scope).fg(YELLOW).a(" - $description".toString()).fg(DEFAULT).a(Ansi.Attribute.INTENSITY_BOLD_OFF))
        }
        else {
            pw.println("$scope - $description")
        }
        for (child in root.children) {
            renderGraph(child,pw, 0)
        }
        if (ansiEnabled) {
            pw.print(new Ansi().a(Ansi.Attribute.INTENSITY_BOLD_OFF).fg(DEFAULT))
        }
        pw.println()
        pw.flush()
    }

    private renderGraph(GraphNode current, PrintWriter writer, int depth) {
        if (depth == 0) {
            def prefix = TOP_LEVEL_PREFIX
            writeDependency(writer, prefix, current)
        }
        else {
            if (ansiEnabled && CONSOLE.isAnsiEnabled()) {
                writer.print(new Ansi().a(Ansi.Attribute.INTENSITY_BOLD).fg(YELLOW).a(INITIAL_TRANSITIVE_PREFIX).fg(DEFAULT).a(Ansi.Attribute.INTENSITY_BOLD_OFF))
            }
            else {
                writer.print(INITIAL_TRANSITIVE_PREFIX)
            }

            if (depth>1) {
                for (num in 1..(depth-1)) {
                    writer.print(PADDING)
                }
            }

            writeDependency(writer, TRANSITIVE_PREFIX, current)
        }
        for (child in current.children) {
            renderGraph(child, writer,depth + 1)
        }
    }

    private void writeDependency(PrintWriter writer, String prefix, GraphNode node) {
        if (ansiEnabled && CONSOLE.isAnsiEnabled()) {
            final resolved = node.resolved
            writer.println(new Ansi().a(Ansi.Attribute.INTENSITY_BOLD).fg(resolved ? YELLOW : RED).a(resolved ? prefix : UNRESOLVED_PREFIX).fg(resolved ? DEFAULT : RED).a(node.dependency.toString()).fg(DEFAULT).a(Ansi.Attribute.INTENSITY_BOLD_OFF))
        } else {
            writer.println("$prefix$node.dependency")
        }
    }

    void render(GraphNode root) {
        render(root, new OutputStreamWriter(System.out))
    }
}
