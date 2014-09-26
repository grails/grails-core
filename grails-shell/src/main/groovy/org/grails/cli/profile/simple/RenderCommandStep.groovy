package org.grails.cli.profile.simple

import java.io.File;

import org.grails.cli.profile.ExecutionContext

class RenderCommandStep extends SimpleCommandStep {

    @Override
    public boolean handleStep(ExecutionContext context) {
        File profileDir = command.profile.profileDir
        File templateFile = new File(profileDir, commandParameters.template)
        
        String artifactName = context.getCommandLine().getRemainingArgs()[0]
        
        Map<String, String> variables = [('artifact.package.path'): '', ('artifact.name'): artifactName]
        
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
