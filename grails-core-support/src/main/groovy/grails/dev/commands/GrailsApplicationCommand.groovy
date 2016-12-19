package grails.dev.commands

import grails.codegen.model.ModelBuilder
import grails.dev.commands.io.FileSystemInteraction
import grails.dev.commands.io.FileSystemInteractionImpl
import grails.dev.commands.template.TemplateRenderer
import grails.dev.commands.template.TemplateRendererImpl

trait GrailsApplicationCommand implements ApplicationCommand, ModelBuilder {

    @Delegate TemplateRenderer templateRenderer
    @Delegate FileSystemInteraction fileSystemInteraction
    ExecutionContext executionContext

    boolean handle(ExecutionContext executionContext) {
        this.executionContext = executionContext
        this.templateRenderer = new TemplateRendererImpl(executionContext.baseDir)
        this.fileSystemInteraction = new FileSystemInteractionImpl(executionContext.baseDir)
        handle()
    }

    List<String> getArgs() {
        executionContext.commandLine.remainingArgs
    }

    abstract boolean handle()
}
