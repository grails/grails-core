package org.grails.cli

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

import org.grails.cli.profile.ProfileRepository

@CompileStatic
class CreateAppCommand {
    ProfileRepository profileRepository
    String appname
    String profile
    
    void run() {
        copySkeleton(profileRepository.getProfileDirectory(profile))
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    private void copySkeleton(File profileDirectory) {
        AntBuilder ant = new AntBuilder()
        ant.copy(todir: new File(appname)) {
            fileSet(dir: new File(profileDirectory, "skeleton")) {
                exclude(name: '**/.gitkeep')
            }
            filterset { filter(token:'APPNAME', value:appname) }
            mapper {
                filtermapper {
                    replacestring(from:'APPNAME', to:appname)
                }
            }
        }
    }
}
