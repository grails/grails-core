package grails.ui.command

import grails.config.Settings
import grails.dev.commands.ApplicationCommand
import grails.dev.commands.ExecutionContext
import grails.ui.support.DevelopmentGrailsApplication
import groovy.transform.CompileStatic
import org.grails.build.parsing.CommandLine
import org.grails.build.parsing.CommandLineParser
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.springframework.context.ConfigurableApplicationContext

/**
 * Created by Jim on 8/2/2016.
 */
@CompileStatic
class GrailsRuntimeApplicationContextCommandRunner extends DevelopmentGrailsApplication {

    String commandName

    protected GrailsRuntimeApplicationContextCommandRunner(String commandName, Object... sources) {
        super(sources)
        this.commandName = commandName
    }

    @Override
    ConfigurableApplicationContext run(String... args) {
        try {
            def commandClass = classLoader.loadClass(commandName)
            if (ApplicationCommand.isAssignableFrom(commandClass)) {
                def command = (ApplicationCommand)commandClass.newInstance()
                Object skipBootstrap = command.hasProperty("skipBootstrap")?.getProperty(command)
                if (skipBootstrap instanceof Boolean && !System.getProperty(Settings.SETTING_SKIP_BOOTSTRAP)) {
                    System.setProperty(Settings.SETTING_SKIP_BOOTSTRAP, skipBootstrap.toString())
                }

                ConfigurableApplicationContext ctx = null
                try {
                    ctx = super.run(args)
                } catch (Throwable e) {
                    System.err.println("Context failed to load: $e.message")
                    System.exit(1)
                }

                try {
                    CommandLine commandLine = new CommandLineParser().parse(args)
                    ctx.autowireCapableBeanFactory.autowireBeanProperties(command, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false)
                    command.applicationContext = ctx
                    def result = command.handle(new ExecutionContext(commandLine))
                    result ? System.exit(0) : System.exit(1)
                } catch (Throwable e) {
                    System.err.println("Command execution error: $e.message")
                    System.exit(1)
                }

                finally {
                    try {
                        ctx?.close()
                    } catch (Throwable e) {
                        // ignore
                    }
                }
            } else {
                System.err.println("Command not found")
                System.exit(1)
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Command not found")
            System.exit(1)
        }
        return null
    }

    /**
     * Main method to run an existing Application class
     *
     * @param args The first argument is the Command name, the last argument is the Application class name
     */
    public static void main(String[] args) {
        if(args.size() > 1) {
            Class applicationClass
            try {
                applicationClass = Thread.currentThread().contextClassLoader.loadClass(args.last())
            } catch (Throwable e) {
                System.err.println("Application class not found")
                System.exit(1)
            }

            def runner = new GrailsRuntimeApplicationContextCommandRunner(args[0], applicationClass)
            runner.run(args.init() as String[])
        }
        else {
            System.err.println("Missing application class name and script name arguments")
            System.exit(1)
        }
    }
}
