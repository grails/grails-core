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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.codehaus.groovy.control.CompilationFailedException;

import org.grails.cli.compiler.GroovyCompiler;
import org.grails.cli.compiler.GroovyCompilerConfiguration;

/**
 * A {@code DependencyResolver} implemented using Groovy's {@code @Grab}.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 */
class GroovyGrabDependencyResolver implements DependencyResolver {

	private final GroovyCompilerConfiguration configuration;

	GroovyGrabDependencyResolver(GroovyCompilerConfiguration configuration) {
		this.configuration = configuration;
	}

	@Override
	public List<File> resolve(List<String> artifactIdentifiers) throws CompilationFailedException, IOException {
		GroovyCompiler groovyCompiler = new GroovyCompiler(this.configuration);
		List<File> artifactFiles = new ArrayList<>();
		if (!artifactIdentifiers.isEmpty()) {
			List<URL> initialUrls = getClassPathUrls(groovyCompiler);
			groovyCompiler.compile(createSources(artifactIdentifiers));
			List<URL> artifactUrls = getClassPathUrls(groovyCompiler);
			artifactUrls.removeAll(initialUrls);
			for (URL artifactUrl : artifactUrls) {
				artifactFiles.add(toFile(artifactUrl));
			}
		}
		return artifactFiles;
	}

	private List<URL> getClassPathUrls(GroovyCompiler compiler) {
		return new ArrayList<>(Arrays.asList(compiler.getLoader().getURLs()));
	}

	private String createSources(List<String> artifactIdentifiers) throws IOException {
		File file = File.createTempFile("SpringCLIDependency", ".groovy");
		file.deleteOnExit();
		try (OutputStreamWriter stream = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
			for (String artifactIdentifier : artifactIdentifiers) {
				stream.write("@Grab('" + artifactIdentifier + "')");
			}
			// Dummy class to force compiler to do grab
			stream.write("class Installer {}");
		}
		// Windows paths get tricky unless you work with URI
		return file.getAbsoluteFile().toURI().toString();
	}

	private File toFile(URL url) {
		try {
			return new File(url.toURI());
		}
		catch (URISyntaxException ex) {
			return new File(url.getPath());
		}
	}

}
