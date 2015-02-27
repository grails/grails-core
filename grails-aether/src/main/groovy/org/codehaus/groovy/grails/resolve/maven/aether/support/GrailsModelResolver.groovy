/*
 * Copyright 2012 the original author or authors.
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

package org.codehaus.groovy.grails.resolve.maven.aether.support

import groovy.transform.CompileStatic
import org.apache.maven.model.Parent
import org.apache.maven.model.Repository
import org.apache.maven.model.building.FileModelSource
import org.apache.maven.model.building.ModelSource
import org.apache.maven.model.resolution.InvalidRepositoryException
import org.apache.maven.model.resolution.ModelResolver
import org.apache.maven.model.resolution.UnresolvableModelException
import org.apache.maven.repository.internal.MavenRepositorySystemUtils
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.RepositorySystemSession
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.impl.VersionRangeResolver
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.resolution.*

/**
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class GrailsModelResolver implements ModelResolver{
    private RepositorySystem  system;
    private RepositorySystemSession session;
    private List<RemoteRepository> repositories = []
    private VersionRangeResolver versionRangeResolver
    private String mavenRequestContext

    GrailsModelResolver(RepositorySystem  system, RepositorySystemSession session, List<RemoteRepository> repositories) {
        this(system, session, repositories, MavenRepositorySystemUtils.newServiceLocator().getService(VersionRangeResolver), '')
    }

    GrailsModelResolver(RepositorySystem  system, RepositorySystemSession session, List<RemoteRepository> repositories, VersionRangeResolver versionRangeResolver, String mavenRequestContext) {
        this.system = system
        this.session = session
        this.repositories = repositories
        this.versionRangeResolver = versionRangeResolver
        this.mavenRequestContext = mavenRequestContext
    }

    @Override
    ModelSource resolveModel(String groupId, String artifactId, String version) {
        def pomArtifact = new DefaultArtifact(groupId, artifactId, "", "pom", version)
        try {
            ArtifactRequest request = new ArtifactRequest(pomArtifact, repositories, null)
            pomArtifact = system.resolveArtifact(session, request).getArtifact()

        } catch (ArtifactResolutionException e) {
            throw new UnresolvableModelException("Failed to resolve POM for " + groupId + ":" + artifactId + ":" + version
                + " due to " + e.getMessage(), groupId, artifactId, version, e);
        }

        File pomFile = pomArtifact.file

        return new FileModelSource(pomFile);
    }

    @Override
    ModelSource resolveModel(Parent parent) throws UnresolvableModelException {
        Artifact artifact = new DefaultArtifact(parent.getGroupId(), parent.getArtifactId(), "", "pom",
            parent.getVersion());

        VersionRangeRequest versionRangeRequest = new VersionRangeRequest(artifact, repositories, mavenRequestContext);
        //versionRangeRequest.setTrace( trace );

        try {
            VersionRangeResult versionRangeResult =
                versionRangeResolver.resolveVersionRange(session, versionRangeRequest);

            if (versionRangeResult.getHighestVersion() == null) {
                throw new UnresolvableModelException("No versions matched the requested range '" + parent.getVersion()
                    + "'", parent.getGroupId(), parent.getArtifactId(),
                    parent.getVersion());
            }

            if (versionRangeResult.getVersionConstraint() != null
                && versionRangeResult.getVersionConstraint().getRange() != null
                && versionRangeResult.getVersionConstraint().getRange().getUpperBound() == null) {
                throw new UnresolvableModelException("The requested version range '" + parent.getVersion()
                    + "' does not specify an upper bound", parent.getGroupId(),
                    parent.getArtifactId(), parent.getVersion());

            }

            parent.setVersion(versionRangeResult.getHighestVersion().toString());
        } catch (VersionRangeResolutionException e) {
            throw new UnresolvableModelException(e.getMessage(), parent.getGroupId(), parent.getArtifactId(),
                parent.getVersion(), e);
        }

        return resolveModel(parent.groupId, parent.artifactId, parent.version)
    }

    @Override
    void addRepository(Repository repository) {
        addRepository(repository, false)
    }

    @Override
    void addRepository(Repository repository, boolean replace) throws InvalidRepositoryException {
        if(replace) {
            repositories.findAll() { it.getId() == repository.id }.each {
                repositories.remove it
            }
        }
        repositories << new RemoteRepository.Builder(repository.id, "default",repository.url).build()
    }

    @Override
    ModelResolver newCopy() {
        return new GrailsModelResolver(system, session, repositories, versionRangeResolver, mavenRequestContext)
    }
}
