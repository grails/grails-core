package org.grails.cli

import grails.plugins.GrailsVersionUtils;
import grails.util.Metadata;
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

import org.grails.cli.profile.Profile
import org.grails.cli.profile.ProfileRepository


@CompileStatic
class CreateAppCommand {
    ProfileRepository profileRepository
    String profile
    String groupAndAppName
    Map<String, String> variables = [:]
    String appname
    String groupname
    File targetDirectory
    List<String> binaryFileExtensions = ['png','gif','jpg','jpeg','ico','icns','pdf','zip','jar','class']
    
    void run() {
        Profile profileInstance = profileRepository.getProfile(profile)
        initializeVariables()
        targetDirectory = new File(appname)
        File applicationYmlFile = new File(targetDirectory, "grails-app/conf/application.yml")
        for(Profile p : profileRepository.getProfileAndDependencies(profileInstance)) {
            String previousApplicationYml = (applicationYmlFile.isFile()) ? applicationYmlFile.text : null
            copySkeleton(profileRepository.getProfileDirectory(p.getName()))
            appendToYmlSubDocument(applicationYmlFile, previousApplicationYml)
        }
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
    
    protected initializeVariables() {
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
        variables['grails.profile'] = profile
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
