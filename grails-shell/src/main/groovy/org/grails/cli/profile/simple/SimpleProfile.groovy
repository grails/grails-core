package org.grails.cli.profile.simple

import groovy.transform.CompileStatic
import jline.console.completer.Completer

import org.grails.cli.profile.CommandLineHandler
import org.grails.cli.profile.Profile
import org.grails.cli.profile.GitProfileRepository
import org.grails.cli.profile.ProjectContext
import org.yaml.snakeyaml.Yaml;

@CompileStatic
class SimpleProfile implements Profile {
    File profileDir
    String name
    private List<CommandLineHandler> commandLineHandlers = null
    List<Profile> parentProfiles
    Map<String, Object> profileConfig

    private SimpleProfile(String name, File profileDir) {
        this.name = name
        this.profileDir = profileDir
    }

    private void initialize(GitProfileRepository repository) {
        parentProfiles = []
        File profileYml = new File(profileDir, "profile.yml")
        if(profileYml.isFile()) {
            profileConfig = (Map<String, Object>)profileYml.withInputStream {
                new Yaml().loadAs(it, Map)
            }
            String[] extendsProfiles = profileConfig.get("extends")?.toString()?.split(/\s*,\s*/)
            if(extendsProfiles) {
                parentProfiles = extendsProfiles.collect { String profileName ->
                    Profile extendsProfile = repository.getProfile(profileName)
                    if(extendsProfile==null) {
                        throw new RuntimeException("Profile $profileName not found. ${this.name} extends it.")
                    }
                    extendsProfile
                }
            } else {
            }
        }
    }

    public static SimpleProfile create(GitProfileRepository repository, String name, File profileDir) {
        SimpleProfile profile = new SimpleProfile(name, profileDir)
        profile.initialize(repository)
        profile
    }

    @Override
    public Iterable<Completer> getCompleters(ProjectContext context) {
        [new CommandLineHandlersCompleter(context:context, commandLineHandlersClosure:{ -> this.getCommandLineHandlers(context) })]
    }

    @Override
    public Iterable<CommandLineHandler> getCommandLineHandlers(ProjectContext context) {
        if(commandLineHandlers == null) {
            commandLineHandlers = []
            Collection<File> commandFiles = findCommandFiles()
            SimpleCommandHandler commandHandler = createCommandHandler(commandFiles)
            commandHandler.initialize()
            commandLineHandlers << commandHandler
            addParentCommandLineHandlers(context, commandLineHandlers)
        }
        commandLineHandlers
    }

    protected void addParentCommandLineHandlers(ProjectContext context, List<CommandLineHandler> commandLineHandlers) {
        parentProfiles.each {
            it.getCommandLineHandlers(context)?.each { CommandLineHandler handler ->
                commandLineHandlers.add(handler)
            }
        }
    }

    protected Collection<File> findCommandFiles() {
        File commandsDir = new File(profileDir, "commands")
        Collection<File> commandFiles = commandsDir.listFiles().findAll { File file ->
            file.isFile() && file.name ==~ /^.*\.(yml|json)$/
        }.sort(false) { File file -> file.name }
        return commandFiles
    }

    protected SimpleCommandHandler createCommandHandler(Collection<File> commandFiles) {
        return new SimpleCommandHandler(commandFiles: commandFiles, profile: this)
    }

    @Override
    public Iterable<Profile> getExtends() {
        return parentProfiles;
    }
}
