package org.grails.cli

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
    
    void run() {
        Profile profileInstance = profileRepository.getProfile(profile)
        initializeVariables()
        for(Profile p : profileRepository.getProfileAndDependencies(profileInstance)) {
            copySkeleton(profileRepository.getProfileDirectory(p.getName()))
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
        ant.copy(todir: new File(appname), overwrite: true) {
            fileSet(dir: new File(profileDirectory, "skeleton")) {
                exclude(name: '**/.gitkeep')
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
    }
}
