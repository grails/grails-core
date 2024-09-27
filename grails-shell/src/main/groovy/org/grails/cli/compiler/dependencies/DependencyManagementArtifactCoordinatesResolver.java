/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.grails.cli.compiler.dependencies;

import org.springframework.util.StringUtils;

/**
 * {@link ArtifactCoordinatesResolver} backed by
 * {@link SpringBootDependenciesDependencyManagement}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.0.0
 */
public class DependencyManagementArtifactCoordinatesResolver implements ArtifactCoordinatesResolver {

	private final DependencyManagement dependencyManagement;

	public DependencyManagementArtifactCoordinatesResolver() {
		this(new SpringBootDependenciesDependencyManagement());
	}

	public DependencyManagementArtifactCoordinatesResolver(DependencyManagement dependencyManagement) {
		this.dependencyManagement = dependencyManagement;
	}

	@Override
	public String getGroupId(String artifactId) {
		Dependency dependency = find(artifactId);
		return (dependency != null) ? dependency.getGroupId() : null;
	}

	@Override
	public String getArtifactId(String id) {
		Dependency dependency = find(id);
		return (dependency != null) ? dependency.getArtifactId() : null;
	}

	private Dependency find(String id) {
		if (StringUtils.countOccurrencesOf(id, ":") == 2) {
			String[] tokens = id.split(":");
			return new Dependency(tokens[0], tokens[1], tokens[2]);
		}
		if (id != null) {
			if (id.startsWith("spring-boot")) {
				return new Dependency("org.springframework.boot", id, this.dependencyManagement.getSpringBootVersion());
			}
			return this.dependencyManagement.find(id);
		}
		return null;
	}

	@Override
	public String getVersion(String module) {
		Dependency dependency = find(module);
		return (dependency != null) ? dependency.getVersion() : null;
	}

}
