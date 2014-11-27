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

import grails.util.GrailsNameUtils
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.grails.build.parsing.CommandLine
import org.grails.cli.profile.Command
import org.grails.cli.profile.CommandDescription
import org.grails.cli.profile.ExecutionContext
import org.grails.cli.profile.Profile
import org.grails.cli.profile.ProfileRepository
import org.grails.cli.profile.ProfileRepositoryAware
import org.grails.cli.profile.git.GitProfileRepository


/**
 * Command for creating Grails applications
 *
 * @author Graeme Rocher
 * @author Lari Hotari
 * @since 3.0
 */
@CompileStatic
class CreateAppCommand implements Command, ProfileRepositoryAware {
    public static final String NAME = "create-app"
    ProfileRepository profileRepository
    Map<String, String> variables = [:]
    String appname
    String groupname
    File targetDirectory
    List<String> binaryFileExtensions = ['png','gif','jpg','jpeg','ico','icns','pdf','zip','jar','class']

    final CommandDescription description = new CommandDescription(name, "Creates an application", "create-app [NAME] --profile=web")

    @Override
    String getName() {
        return NAME
    }


    @Override
    boolean handle(ExecutionContext executionContext) {
        if(profileRepository == null) throw new IllegalStateException("Property 'profileRepository' must be set")


        def mainCommandLine = executionContext.commandLine
        def profileName = evaluateProfileName(mainCommandLine)
        String groupAndAppName = mainCommandLine.getRemainingArgs()[0]

        Profile profileInstance = profileRepository.getProfile(profileName)
        if(profileInstance) {

            initializeVariables(profileInstance, groupAndAppName)
            targetDirectory = new File(appname)
            File applicationYmlFile = new File(targetDirectory, "grails-app/conf/application.yml")
            for(Profile p : profileRepository.getProfileAndDependencies(profileInstance)) {
                String previousApplicationYml = (applicationYmlFile.isFile()) ? applicationYmlFile.text : null
                copySkeleton(profileRepository.getProfileDirectory(p.getName()))
                appendToYmlSubDocument(applicationYmlFile, previousApplicationYml)
            }
            return true
        }
        else {
            System.err.println "Cannot find profile $profileName"
            return false
        }
    }

    protected String evaluateProfileName(CommandLine mainCommandLine) {
        mainCommandLine.optionValue('profile')?.toString() ?: ProfileRepository.DEFAULT_PROFILE_NAME
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
    
    protected initializeVariables(Profile profile, String groupAndAppName) {
        String defaultPackage
        List<String> parts = groupAndAppName.split(/\./) as List
        if(parts.size() == 1) {
            appname = parts[0]
            defaultPackage = createValidPackageName()
            groupname = defaultPackage
        } else {
            appname = parts[-1]
            groupname = parts[0..-2].join('.')
            defaultPackage = groupname
        }

        variables.APPNAME = appname

        variables['grails.codegen.defaultPackage'] = defaultPackage
        variables['grails.codegen.defaultPackage.path']  = defaultPackage.replace('.', '/')

        def projectClassName = GrailsNameUtils.getNameFromScript(appname)
        variables['grails.codegen.projectClassName'] = projectClassName
        variables['grails.codegen.projectNaturalName'] = GrailsNameUtils.getNaturalName(projectClassName)
        variables['grails.codegen.projectName'] = GrailsNameUtils.getScriptName(projectClassName)
        variables['grails.profile'] = profile.name
        variables['grails.version'] = this.getClass().getPackage().getImplementationVersion()
        variables['grails.app.name'] = appname
        variables['grails.app.group'] = groupname
    }

    private String createValidPackageName() {
        String defaultPackage = (appname.toLowerCase().toCharArray().findAll  { char ch -> Character.isJavaIdentifierPart(ch) } as char[]) as String
        return defaultPackage
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    private void copySkeleton(File profileDirectory) {
        AntBuilder ant = new AntBuilder()
        File srcDir = new File(profileDirectory, "skeleton")
        ant.copy(todir: targetDirectory, overwrite: true, encoding: 'UTF-8') {
            fileSet(dir: srcDir, casesensitive: false) {
                exclude(name: '**/.gitkeep')
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
            }
            mapper {
                filtermapper {
                    variables.each { k, v ->
                        replacestring(from: "@${k}@".toString(), to:v)
                    }
                }
            }
        }
    }
}
