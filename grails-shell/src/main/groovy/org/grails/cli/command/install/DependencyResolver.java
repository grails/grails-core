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

package org.grails.cli.command.install;

import java.io.File;
import java.util.List;

/**
 * Resolve artifact identifiers (typically in the form {@literal group:artifact:version})
 * to {@link File}s.
 *
 * @author Andy Wilkinson
 */
@FunctionalInterface
interface DependencyResolver {

	/**
	 * Resolves the given {@code artifactIdentifiers}, typically in the form
	 * "group:artifact:version", and their dependencies.
	 * @param artifactIdentifiers the artifacts to resolve
	 * @return the {@code File}s for the resolved artifacts
	 * @throws Exception if dependency resolution fails
	 */
	List<File> resolve(List<String> artifactIdentifiers) throws Exception;

}
