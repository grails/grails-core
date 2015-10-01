/*
 * Copyright 2015 original authors
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

package org.grails.gradle.plugin.profiles.tasks

import groovy.transform.CompileStatic
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer
import org.codehaus.groovy.control.customizers.ImportCustomizer
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.grails.cli.profile.commands.script.GroovyScriptCommand
import org.grails.cli.profile.commands.script.GroovyScriptCommandTransform


/**
 * Compiles the classes for a profile
 *
 * @author Graeme Rocher
 * @since 3.1
 */
@CompileStatic
class ProfileCompilerTask extends AbstractCompile {

    public static final String DEFAULT_COMPATIBILITY = "1.7"

    ProfileCompilerTask() {
        setSourceCompatibility(DEFAULT_COMPATIBILITY)
        setTargetCompatibility(DEFAULT_COMPATIBILITY)
    }

    @TaskAction
    void execute(IncrementalTaskInputs inputs) {
        compile()
    }

    @Override
    protected void compile() {

        CompilerConfiguration configuration = new CompilerConfiguration()
        configuration.setScriptBaseClass(GroovyScriptCommand.name)
        destinationDir.mkdirs()
        configuration.setTargetDirectory(destinationDir)

        def importCustomizer = new ImportCustomizer()
        importCustomizer.addStarImports("org.grails.cli.interactive.completers")
        importCustomizer.addStarImports("grails.util")
        importCustomizer.addStarImports("grails.codegen.model")
        configuration.addCompilationCustomizers(importCustomizer,new ASTTransformationCustomizer(new GroovyScriptCommandTransform()))

        CompilationUnit compilationUnit = new CompilationUnit(configuration)

        def sourceFiles = getSource().files.findAll() { File f ->
            f.name.endsWith('.groovy')
        } as File[]
        compilationUnit.addSources(sourceFiles)
        compilationUnit.compile()
    }
}
