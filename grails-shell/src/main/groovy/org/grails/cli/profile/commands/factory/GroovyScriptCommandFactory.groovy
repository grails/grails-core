package org.grails.cli.profile.commands.factory

import grails.util.GrailsNameUtils
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer
import org.grails.cli.profile.Command
import org.grails.cli.profile.Profile
import org.grails.cli.profile.commands.script.CommandScript
import org.grails.cli.profile.commands.script.CommandScriptTransform

import java.util.regex.Pattern

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
 * A {@link CommandFactory} that creates {@link Command} instances from Groovy scripts
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class GroovyScriptCommandFactory extends FileBasedCommandFactory<CommandScript> {

    final Pattern fileExtensionPattern = ~/\.(groovy)$/
    final Pattern fileNamePattern = ~/^.*\.(groovy)$/

    @Override
    @CompileDynamic
    protected CommandScript readCommandFile(File file) {
        def configuration = new CompilerConfiguration()
        // TODO: Report bug, this fails with @CompileStatic with a ClassCastException
        String baseClassName = CommandScript.class.getName()
        configuration.setScriptBaseClass(baseClassName)
        configuration.addCompilationCustomizers(new ASTTransformationCustomizer(new CommandScriptTransform()))
        def classLoader = new GroovyClassLoader(Thread.currentThread().contextClassLoader, configuration)
        return (CommandScript) classLoader.parseClass(file).newInstance()
    }

    @Override
    protected String evaluateFileName(File file) {
        def fileName = super.evaluateFileName(file)
        return fileName.contains('-') ? fileName.toLowerCase() : GrailsNameUtils.getScriptName(fileName)
    }

    @Override
    protected Command createCommand(Profile profile, String commandName, File file, CommandScript data) {
        data.setProfile(profile)
        return data
    }
}
