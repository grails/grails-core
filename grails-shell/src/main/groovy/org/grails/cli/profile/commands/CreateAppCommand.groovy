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

package org.grails.cli.profile.commands

import grails.build.logging.GrailsConsole
import grails.util.Environment
import grails.util.GrailsNameUtils
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.grails.build.logging.GrailsConsoleAntBuilder
import org.grails.build.parsing.CommandLine
import org.grails.cli.profile.*
/**
 * Command for creating Grails applications
 *
 * @author Graeme Rocher
 * @author Lari Hotari
 * @since 3.0
 */
@CompileStatic
class CreateAppCommand implements Command, ProfileRepositoryAware {
    private static final String GRAILS_VERSION_FALLBACK_IN_IDE_ENVIRONMENTS_FOR_RUNNING_TESTS ='3.0.0.BUILD-SNAPSHOT'
    public static final String NAME = "create-app"
    ProfileRepository profileRepository
    Map<String, String> variables = [:]
    String appname
    String groupname
    File targetDirectory
    List<String> binaryFileExtensions = ['png','gif','jpg','jpeg','ico','icns','pdf','zip','jar','class']

    CommandDescription description = new CommandDescription(name, "Creates an application", "create-app [NAME] --profile=web")

    CreateAppCommand() {
        populateDescription()
    }

    protected void populateDescription() {
        description.argument(name: "Application Name", description: "The name of the application to create.", required: false)
        description.flag(name: "inplace", description: "Used to create an application using the current directory")
    }

    @Override
    String getName() {
        return NAME
    }


    @Override
    boolean handle(ExecutionContext executionContext) {
        if(profileRepository == null) throw new IllegalStateException("Property 'profileRepository' must be set")


        def mainCommandLine = executionContext.commandLine
        def profileName = evaluateProfileName(mainCommandLine)

        Profile profileInstance = profileRepository.getProfile(profileName)
        if(profileInstance) {

            if( !initializeVariables(profileInstance, mainCommandLine) ) {
                return false
            }
            targetDirectory = mainCommandLine.hasOption('inplace') ? new File(".").canonicalFile : new File(appname)
            File applicationYmlFile = new File(targetDirectory, "grails-app/conf/application.yml")

            def profiles = profileRepository.getProfileAndDependencies(profileInstance)
            for(Profile p : profiles) {
                String previousApplicationYml = (applicationYmlFile.isFile()) ? applicationYmlFile.text : null
                copySkeleton(profileInstance, profileRepository.getProfileDirectory(p.getName()))

                if(!applicationYmlFile.exists()) {
                    applicationYmlFile = new File('application.yml')
                }
                if(applicationYmlFile.exists()) {
                    appendToYmlSubDocument(applicationYmlFile, previousApplicationYml)
                }
            }
            executionContext.console.addStatus(
                "${name == 'create-plugin' ? 'Plugin' : 'Application'} created at $targetDirectory.absolutePath"
            )
            return true
        }
        else {
            System.err.println "Cannot find profile $profileName"
            return false
        }
    }

    protected String evaluateProfileName(CommandLine mainCommandLine) {
        mainCommandLine.optionValue('profile')?.toString() ?: getDefaultProfile()
    }

    protected String getDefaultProfile() {
        ProfileRepository.DEFAULT_PROFILE_NAME
    }


    private void appendToYmlSubDocument(File applicationYmlFile, String previousApplicationYml) {
        String newApplicationYml = applicationYmlFile.text
        if(previousApplicationYml && newApplicationYml != previousApplicationYml) {
            StringBuilder appended = new StringBuilder(previousApplicationYml.length() + newApplicationYml.length() + 30)
            if(!previousApplicationYml.startsWith("---")) {
                appended.append('---\n')
            }
            appended.append(previousApplicationYml).append("\n---\n")
            appended.append(newApplicationYml)
            applicationYmlFile.text = appended.toString()
        }
    }
    
    protected boolean initializeVariables(Profile profile, CommandLine commandLine) {
        String defaultPackage

        def args = commandLine.getRemainingArgs()
        boolean inPlace = commandLine.hasOption('inplace')

        if(!args && !inPlace) {
            GrailsConsole.getInstance().error("Specify an application name or use --inplace to create an application in the current directory")
            return false
        }
        String groupAndAppName = args ? args[0] : null



        if(inPlace) {
            appname = new File(".").canonicalFile.name
            if(groupAndAppName) {
                groupname = groupAndAppName
                defaultPackage = groupname
            }
            else {
                try {
                    defaultPackage = createValidPackageName()
                } catch (IllegalArgumentException e ) {
                    GrailsConsole.instance.error(e.message)
                    return false
                }
                groupname = defaultPackage
            }
        }
        else {
            if(!groupAndAppName) {
                GrailsConsole.getInstance().error("Specify an application name or use --inplace to create an application in the current directory")
                return false
            }
            List<String> parts = groupAndAppName.split(/\./) as List
            if(parts.size() == 1) {
                appname = parts[0]
                try {
                    defaultPackage = createValidPackageName()
                } catch (IllegalArgumentException e ) {
                    GrailsConsole.instance.error(e.message)
                    return false
                }
                groupname = defaultPackage
            } else {
                appname = parts[-1]
                groupname = parts[0..-2].join('.')
                defaultPackage = groupname
            }
        }


        variables.APPNAME = appname

        variables['grails.codegen.defaultPackage'] = defaultPackage
        variables['grails.codegen.defaultPackage.path']  = defaultPackage.replace('.', '/')

        def projectClassName = GrailsNameUtils.getNameFromScript(appname)
        variables['grails.codegen.projectClassName'] = projectClassName
        variables['grails.codegen.projectNaturalName'] = GrailsNameUtils.getNaturalName(projectClassName)
        variables['grails.codegen.projectName'] = GrailsNameUtils.getScriptName(projectClassName)
        variables['grails.profile'] = profile.name
        variables['grails.version'] = Environment.getPackage().getImplementationVersion() ?: GRAILS_VERSION_FALLBACK_IN_IDE_ENVIRONMENTS_FOR_RUNNING_TESTS
        variables['grails.app.name'] = appname
        variables['grails.app.group'] = groupname
    }

    private String createValidPackageName() {
        String defaultPackage = appname.split(/[-]+/).collect { String token -> (token.toLowerCase().toCharArray().findAll  { char ch -> Character.isJavaIdentifierPart(ch) } as char[]) as String }.join('.')
        if(!GrailsNameUtils.isValidJavaPackage(defaultPackage)) {
            throw new IllegalArgumentException("Cannot create a valid package name for [$appname]. Please specify a name that is also a valid Java package.")
        }
        return defaultPackage
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    private void copySkeleton(Profile profile, File profileDirectory) {

        def excludes = profile.configuration.navigate("skeleton", "excludes") ?: []

        AntBuilder ant = new GrailsConsoleAntBuilder()
        File srcDir = new File(profileDirectory, "skeleton")
        ant.copy(todir: targetDirectory, overwrite: true, encoding: 'UTF-8') {
            fileSet(dir: srcDir, casesensitive: false) {
                exclude(name: '**/.gitkeep')
                for(exc in excludes) {
                    exclude name: exc
                }
                binaryFileExtensions.each { ext ->
                    exclude(name: "**/*.${ext}")
                }
            }
            filterset { 
                variables.each { k, v ->
                    filter(token:k, value:v)
                } 
            }
            mapper {
                filtermapper {
                    variables.each { k, v ->
                        replacestring(from: "@${k}@".toString(), to:v)
                    }
                }
            }
        }
        ant.copy(todir: targetDirectory, overwrite: true) {
            fileSet(dir: srcDir, casesensitive: false) {
                binaryFileExtensions.each { ext ->
                    include(name: "**/*.${ext}")
                }
                for(exc in excludes) {
                    exclude name: exc
                }
            }
            mapper {
                filtermapper {
                    variables.each { k, v ->
                        replacestring(from: "@${k}@".toString(), to:v)
                    }
                }
            }
        }
        ant.chmod(file: "${targetDirectory}/gradlew", perm: 'u+x')
    }
}
