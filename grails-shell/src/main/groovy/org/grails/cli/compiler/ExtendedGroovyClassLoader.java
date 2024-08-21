/*
 * Copyright 2012-2020 the original author or authors.
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

package org.grails.cli.compiler;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import groovy.lang.GroovyClassLoader;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.SourceUnit;

import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

/**
 * Extension of the {@link GroovyClassLoader} with support for obtaining '.class' files as
 * resources.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @since 1.0.0
 */
public class ExtendedGroovyClassLoader extends GroovyClassLoader {

	private static final String SHARED_PACKAGE = "org.springframework.boot.groovy";

	private static final URL[] NO_URLS = new URL[] {};

	private final Map<String, byte[]> classResources = new HashMap<>();

	private final GroovyCompilerScope scope;

	private final CompilerConfiguration configuration;

	public ExtendedGroovyClassLoader(GroovyCompilerScope scope) {
		this(scope, createParentClassLoader(scope), new CompilerConfiguration());
	}

	private static ClassLoader createParentClassLoader(GroovyCompilerScope scope) {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		if (scope == GroovyCompilerScope.DEFAULT) {
			classLoader = new DefaultScopeParentClassLoader(classLoader);
		}
		return classLoader;
	}

	private ExtendedGroovyClassLoader(GroovyCompilerScope scope, ClassLoader parent,
			CompilerConfiguration configuration) {
		super(parent, configuration);
		this.configuration = configuration;
		this.scope = scope;
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		try {
			return super.findClass(name);
		}
		catch (ClassNotFoundException ex) {
			if (this.scope == GroovyCompilerScope.DEFAULT && name.startsWith(SHARED_PACKAGE)) {
				Class<?> sharedClass = findSharedClass(name);
				if (sharedClass != null) {
					return sharedClass;
				}
			}
			throw ex;
		}
	}

	private Class<?> findSharedClass(String name) {
		try {
			String path = name.replace('.', '/').concat(".class");
			try (InputStream inputStream = getParent().getResourceAsStream(path)) {
				if (inputStream != null) {
					return defineClass(name, FileCopyUtils.copyToByteArray(inputStream));
				}
			}
			return null;
		}
		catch (Exception ex) {
			return null;
		}
	}

	@Override
	public InputStream getResourceAsStream(String name) {
		InputStream resourceStream = super.getResourceAsStream(name);
		if (resourceStream == null) {
			byte[] bytes = this.classResources.get(name);
			resourceStream = (bytes != null) ? new ByteArrayInputStream(bytes) : null;
		}
		return resourceStream;
	}

	@Override
	public ClassCollector createCollector(CompilationUnit unit, SourceUnit su) {
		InnerLoader loader = getInnerLoader().run();
		return new ExtendedClassCollector(loader, unit, su);
	}

	private PrivilegedAction<InnerLoader> getInnerLoader() {
		return () -> new InnerLoader(ExtendedGroovyClassLoader.this) {

			// Don't return URLs from the inner loader so that Tomcat only
			// searches the parent. Fixes 'TLD skipped' issues
			@Override
			public URL[] getURLs() {
				return NO_URLS;
			}

		};
	}

	public CompilerConfiguration getConfiguration() {
		return this.configuration;
	}

	/**
	 * Inner collector class used to track as classes are added.
	 */
	protected class ExtendedClassCollector extends ClassCollector {

		protected ExtendedClassCollector(InnerLoader loader, CompilationUnit unit, SourceUnit su) {
			super(loader, unit, su);
		}

		@Override
		protected Class<?> createClass(byte[] code, ClassNode classNode) {
			Class<?> createdClass = super.createClass(code, classNode);
			ExtendedGroovyClassLoader.this.classResources.put(classNode.getName().replace('.', '/') + ".class", code);
			return createdClass;
		}

	}

	/**
	 * ClassLoader used for a parent that filters so that only classes from groovy-all.jar
	 * are exposed.
	 */
	private static class DefaultScopeParentClassLoader extends ClassLoader {

		private static final String[] GROOVY_JARS_PREFIXES = { "groovy", "antlr", "asm" };

		private final URLClassLoader groovyOnlyClassLoader;

		DefaultScopeParentClassLoader(ClassLoader parent) {
			super(parent);
			this.groovyOnlyClassLoader = new URLClassLoader(getGroovyJars(parent),
					getClass().getClassLoader().getParent());
		}

		private URL[] getGroovyJars(ClassLoader parent) {
			Set<URL> urls = new HashSet<>();
			findGroovyJarsDirectly(parent, urls);
			if (urls.isEmpty()) {
				findGroovyJarsFromClassPath(urls);
			}
			Assert.state(!urls.isEmpty(), "Unable to find groovy JAR");
			return new ArrayList<>(urls).toArray(new URL[0]);
		}

		private void findGroovyJarsDirectly(ClassLoader classLoader, Set<URL> urls) {
			while (classLoader != null) {
				if (classLoader instanceof URLClassLoader) {
					for (URL url : ((URLClassLoader) classLoader).getURLs()) {
						if (isGroovyJar(url.toString())) {
							urls.add(url);
						}
					}
				}
				classLoader = classLoader.getParent();
			}
		}

		private void findGroovyJarsFromClassPath(Set<URL> urls) {
			String classpath = System.getProperty("java.class.path");
			String[] entries = classpath.split(System.getProperty("path.separator"));
			for (String entry : entries) {
				if (isGroovyJar(entry)) {
					File file = new File(entry);
					if (file.canRead()) {
						try {
							urls.add(file.toURI().toURL());
						}
						catch (MalformedURLException ex) {
							// Swallow and continue
						}
					}
				}
			}
		}

		private boolean isGroovyJar(String entry) {
			entry = StringUtils.cleanPath(entry);
			for (String jarPrefix : GROOVY_JARS_PREFIXES) {
				if (entry.contains("/" + jarPrefix + "-")) {
					return true;
				}
			}
			return false;
		}

		@Override
		public Enumeration<URL> getResources(String name) throws IOException {
			return this.groovyOnlyClassLoader.getResources(name);
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			if (!name.startsWith("java.")) {
				Class.forName(name, false, this.groovyOnlyClassLoader);
			}
			return super.loadClass(name, resolve);
		}

	}

}
