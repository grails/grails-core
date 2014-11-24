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
import org.grails.cli.profile.AbstractStep
import org.grails.cli.profile.ExecutionContext
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
        String nameAsArgument = context.getCommandLine().getRemainingArgs()[0]
        String artifactName
        String artifactPackage 
        (artifactName, artifactPackage) = resolveNameAndPackage(context, nameAsArgument)

        def variableResolver = new ArtefactVariableResolver(artifactName, artifactPackage)

        File destination = variableResolver.resolveFile(parameters.destination, context)
        if(destination.exists()) {
            context.console.error("${destination.canonicalPath} already exists.")
            return false
        }

        String relPath = relativePath(context.baseDir, destination)
        context.console.info("Creating $relPath")
        
        renderToDestination(destination, variableResolver.variables)
        
        return true
    }

    protected renderToDestination(File destination, Map variables) {
        File profileDir = command.profile.profileDir
        File templateFile = new File(profileDir, parameters.template)
        destination.text = new SimpleTemplate(templateFile.text).render(variables)
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
