/*
 * Copyright 2012-2022 the original author or authors.
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

package org.grails.cli.compiler.grape;

import java.util.List;

import groovy.grape.GrapeEngine;
import groovy.lang.GroovyClassLoader;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * A {@link GrapeEngine} implementation that uses
 * <a href="https://eclipse.org/aether">Aether</a>, the dependency resolution system used
 * by Maven.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 1.0.0
 * @deprecated since 2.5.9 for removal in 3.0.0 in favor of
 * {@link MavenResolverGrapeEngine}
 */
@Deprecated
public class AetherGrapeEngine extends MavenResolverGrapeEngine {

	public AetherGrapeEngine(GroovyClassLoader classLoader, RepositorySystem repositorySystem,
			DefaultRepositorySystemSession repositorySystemSession, List<RemoteRepository> remoteRepositories,
			DependencyResolutionContext resolutionContext, boolean quiet) {
		super(classLoader, repositorySystem, repositorySystemSession, remoteRepositories, resolutionContext, quiet);
	}

}
