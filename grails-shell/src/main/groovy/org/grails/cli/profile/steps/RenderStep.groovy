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
package org.grails.cli.profile.steps

import grails.build.logging.GrailsConsole
import grails.util.GrailsNameUtils
import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import org.grails.build.parsing.CommandLine
import org.grails.cli.interactive.completers.ClassNameCompleter
import org.grails.cli.profile.AbstractStep
import org.grails.cli.profile.ExecutionContext
import org.grails.cli.profile.Profile
import org.grails.cli.profile.commands.templates.SimpleTemplate
import org.grails.cli.profile.support.ArtefactVariableResolver

/**
 * A {@link org.grails.cli.profile.Step} that renders a template
 *
 * @author Lari Hotari
 * @author Graeme Rocher
 *
 * @since 3.0
 */
@InheritConstructors
class RenderStep extends AbstractStep {

    public static final String NAME = "render"

    @Override
    @CompileStatic
    String getName() { NAME }

    @Override
    public boolean handle(ExecutionContext context) {
        def commandLine = context.getCommandLine()
        String nameAsArgument = commandLine.getRemainingArgs()[0]
        String artifactName
        String artifactPackage
        (artifactName, artifactPackage) = resolveNameAndPackage(context, nameAsArgument)
        def variableResolver = new ArtefactVariableResolver(artifactName, (String) parameters.convention,artifactPackage)
        File destination = variableResolver.resolveFile(parameters.destination, context)

        try {

            String relPath = relativePath(context.baseDir, destination)
            if(destination.exists() && !flag(commandLine, 'force')) {
                context.console.error("${relPath} already exists.")
                return false
            }

            renderToDestination(destination, variableResolver.variables)
            context.console.addStatus("Created $relPath")

            return true
        } catch (Throwable e) {
            GrailsConsole.instance.error("Failed to render template to destination: ${e.message}", e)
            return false
        }
    }


    protected void renderToDestination(File destination, Map variables) {
        Profile profile = command.profile
        File profileDir = profile.profileDir
        File templateFile = new File(profileDir, parameters.template)
        if(!templateFile.exists()) {
            for(parent in profile.extends) {
                templateFile = new File(parent.profileDir, parameters.template)
                if(templateFile.exists()) break
            }
        }

        destination.text = new SimpleTemplate(templateFile.text).render(variables)
        ClassNameCompleter.refreshAll()
    }

    protected List<String> resolveNameAndPackage(ExecutionContext context, String nameAsArgument) {
        List<String> parts = nameAsArgument.split(/\./) as List

        String artifactName
        String artifactPackage

        if(parts.size() == 1) {
            artifactName = parts[0]
            artifactPackage = context.navigateConfig('grails', 'codegen', 'defaultPackage')?:''
        } else {
            artifactName = parts[-1]
            artifactPackage = parts[0..-2].join('.')
        }

        [GrailsNameUtils.getClassName(artifactName), artifactPackage]
    }
    
    protected String relativePath(File relbase, File file) {
        def pathParts = []
        def currentFile = file
        while (currentFile != null && currentFile != relbase) {
            pathParts += currentFile.name
            currentFile = currentFile.parentFile
        }
        pathParts.reverse().join('/')
    }
    

}
