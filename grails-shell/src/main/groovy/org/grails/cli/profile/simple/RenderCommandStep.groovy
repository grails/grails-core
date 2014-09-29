package org.grails.cli.profile.simple

import java.io.File;

import org.grails.cli.profile.ExecutionContext

class RenderCommandStep extends SimpleCommandStep {

    @Override
    public boolean handleStep(ExecutionContext context) {
        File profileDir = command.profile.profileDir
        File templateFile = new File(profileDir, commandParameters.template)
        
        String nameAsArgument = context.getCommandLine().getRemainingArgs()[0]
        List<String> parts = nameAsArgument.split(/\./) as List
        
        String artifactName
        String artifactPackage
        
        if(parts.size() == 1) {
            artifactName = parts[0]
            artifactPackage = context.navigateConfig('grails', 'codegen', 'defaultPackage')
        } else {
            artifactName = parts[-1]
            artifactPackage = parts[0..-2].join('.')
        }
        
        Map<String, String> variables = [:]
        variables['artifact.package.name'] = artifactPackage
        variables['artifact.package.path'] = artifactPackage.replace('.','/')
        variables['artifact.package'] = "package $artifactPackage\n"
        variables['artifact.name'] = artifactName
        
        String destinationName = doReplacements(commandParameters.destination, variables)
        File destination = new File(context.baseDir, destinationName).absoluteFile
        
        if(destination.exists()) {
            throw new RuntimeException("$destination already exists.") 
        }
        if(!destination.getParentFile().exists()) {
            destination.getParentFile().mkdirs()
        }
        
        String relPath = relativePath(context.baseDir, destination)
        context.console.info("Creating $relPath")
        
        destination.text = doReplacements(templateFile.text, variables)
        
        return true
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
    
    private String doReplacements(String source, Map<String, String> variables) {
        String result = source
        variables.each { k, v ->
            result = result.replace("@${k}@".toString(), v)
        }
        result
    }
}
