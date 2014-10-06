package org.grails.cli.profile.simple

import org.grails.cli.profile.ExecutionContext

class RenderCommandStep extends SimpleCommandStep {
    @Override
    public boolean handleStep(ExecutionContext context) {
        String nameAsArgument = context.getCommandLine().getRemainingArgs()[0]
        String artifactName
        String artifactPackage 
        (artifactName, artifactPackage) = resolveNameAndPackage(context, nameAsArgument)
        Map<String, String> variables = createVariables(artifactPackage, artifactName)
        
        File destination = resolveDestination(context, variables)
        
        String relPath = relativePath(context.baseDir, destination)
        context.console.info("Creating $relPath")
        
        renderToDestination(destination, variables)
        
        return true
    }

    protected renderToDestination(File destination, Map variables) {
        File profileDir = command.profile.profileDir
        File templateFile = new File(profileDir, commandParameters.template)
        destination.text = new SimpleTemplate(templateFile.text).render(variables)
    }

    private File resolveDestination(ExecutionContext context, Map variables) {
        String destinationName = new SimpleTemplate(commandParameters.destination).render(variables)
        File destination = new File(context.baseDir, destinationName).absoluteFile

        if(destination.exists()) {
            throw new RuntimeException("$destination already exists.")
        }
        if(!destination.getParentFile().exists()) {
            destination.getParentFile().mkdirs()
        }
        return destination
    }

    private Map createVariables(String artifactPackage, String artifactName) {
        Map<String, String> variables = [:]
        variables['artifact.package.name'] = artifactPackage
        variables['artifact.package.path'] = artifactPackage?.replace('.','/')
        variables['artifact.package'] = "package $artifactPackage\n"
        variables['artifact.name'] = artifactName
        return variables
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
        
        [artifactName, artifactPackage]
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
