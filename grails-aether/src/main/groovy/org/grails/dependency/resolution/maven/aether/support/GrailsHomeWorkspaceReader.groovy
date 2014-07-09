package org.grails.dependency.resolution.maven.aether.support

import groovy.transform.CompileStatic
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.repository.WorkspaceReader
import org.eclipse.aether.repository.WorkspaceRepository

/**
 * A {@link WorkspaceReader} that resolves dependencies from GRAILS_HOME
 *
 * @author Graeme Rocher
 * @since 2.3.10
 */
@CompileStatic
class GrailsHomeWorkspaceReader implements WorkspaceReader{

    String grailsHome

    GrailsHomeWorkspaceReader() {
        this(System.getProperty('grails.home') ?: System.getenv('GRAILS_HOME'))
    }

    GrailsHomeWorkspaceReader(String grailsHome ) {
        this.grailsHome =  grailsHome
    }

    @Override
    WorkspaceRepository getRepository() {
        return new WorkspaceRepository("grailsHome")
    }

    @Override
    File findArtifact(Artifact artifact) {
        File artifactFile = null
        if(grailsHome) {
            def pomsParent = new File(grailsHome, "lib/${artifact.groupId}/${artifact.artifactId}")
            def jarsParent = new File(grailsHome, "lib/${artifact.groupId}/${artifact.artifactId}/jars")

            if(artifact.extension == "pom") {
                artifactFile = resolveFile(artifact, pomsParent, "pom")
            }
            else if(artifact.extension == 'jar') {
                artifactFile = resolveFile(artifact, jarsParent, "jar")
            }
        }
        return artifactFile
    }

    protected File resolveFile(Artifact artifact, File parentDir, String fileType) {
        File artifactFile = null
        def pomFileName = "${artifact.artifactId}-${artifact.version}.$fileType"
        def grailsHomePom = new File(parentDir, pomFileName)
        if (grailsHomePom.exists()) {
            artifactFile = grailsHomePom
        } else if (artifact.groupId == 'org.grails') {
            grailsHomePom = new File(grailsHome, "dist/$pomFileName")
            if (grailsHomePom.exists()) {
                artifactFile = grailsHomePom
            }
        }
        artifactFile
    }

    @Override
    List<String> findVersions(Artifact artifact) {
        // only support explicit version from GRAILS_HOME
        return Collections.emptyList()
    }
}
