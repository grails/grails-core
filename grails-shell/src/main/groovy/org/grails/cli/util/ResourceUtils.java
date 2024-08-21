/*
 * Copyright 2012-2023 the original author or authors.
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

package org.grails.cli.util;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Utilities for manipulating resource paths and URLs.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @since 1.0.0
 */
public abstract class ResourceUtils {

	/**
	 * Pseudo URL prefix for loading from the class path: "classpath:".
	 */
	public static final String CLASSPATH_URL_PREFIX = "classpath:";

	/**
	 * Pseudo URL prefix for loading all resources from the class path: "classpath*:".
	 */
	public static final String ALL_CLASSPATH_URL_PREFIX = "classpath*:";

	/**
	 * URL prefix for loading from the file system: "file:".
	 */
	public static final String FILE_URL_PREFIX = "file:";

	/**
	 * Return URLs from a given source path. Source paths can be simple file locations
	 * (/some/file.java) or wildcard patterns (/some/**). Additionally the prefixes
	 * "file:", "classpath:" and "classpath*:" can be used for specific path types.
	 * @param path the source path
	 * @param classLoader the class loader or {@code null} to use the default
	 * @return a list of URLs
	 */
	public static List<String> getUrls(String path, ClassLoader classLoader) {
		if (classLoader == null) {
			classLoader = ClassUtils.getDefaultClassLoader();
		}
		path = StringUtils.cleanPath(path);
		try {
			return getUrlsFromWildcardPath(path, classLoader);
		}
		catch (Exception ex) {
			throw new IllegalArgumentException("Cannot create URL from path [" + path + "]", ex);
		}
	}

	private static List<String> getUrlsFromWildcardPath(String path, ClassLoader classLoader) throws IOException {
		if (path.contains(":")) {
			return getUrlsFromPrefixedWildcardPath(path, classLoader);
		}
		Set<String> result = new LinkedHashSet<>();
		try {
			result.addAll(getUrls(FILE_URL_PREFIX + path, classLoader));
		}
		catch (IllegalArgumentException ex) {
			// ignore
		}
		path = stripLeadingSlashes(path);
		result.addAll(getUrls(ALL_CLASSPATH_URL_PREFIX + path, classLoader));
		return new ArrayList<>(result);
	}

	private static List<String> getUrlsFromPrefixedWildcardPath(String path, ClassLoader classLoader)
			throws IOException {
		Resource[] resources = new PathMatchingResourcePatternResolver(new FileSearchResourceLoader(classLoader))
			.getResources(path);
		List<String> result = new ArrayList<>();
		for (Resource resource : resources) {
			if (resource.exists()) {
				if ("file".equals(resource.getURI().getScheme()) && resource.getFile().isDirectory()) {
					result.addAll(getChildFiles(resource));
					continue;
				}
				result.add(absolutePath(resource));
			}
		}
		return result;
	}

	private static List<String> getChildFiles(Resource resource) throws IOException {
		Resource[] children = new PathMatchingResourcePatternResolver().getResources(resource.getURL() + "/**");
		List<String> childFiles = new ArrayList<>();
		for (Resource child : children) {
			if (!child.getFile().isDirectory()) {
				childFiles.add(absolutePath(child));
			}
		}
		return childFiles;
	}

	private static String absolutePath(Resource resource) throws IOException {
		if (!"file".equals(resource.getURI().getScheme())) {
			return resource.getURL().toExternalForm();
		}
		return resource.getFile().getAbsoluteFile().toURI().toString();
	}

	private static String stripLeadingSlashes(String path) {
		while (path.startsWith("/")) {
			path = path.substring(1);
		}
		return path;
	}

	private static class FileSearchResourceLoader extends DefaultResourceLoader {

		private final FileSystemResourceLoader files;

		FileSearchResourceLoader(ClassLoader classLoader) {
			super(classLoader);
			this.files = new FileSystemResourceLoader();
		}

		@Override
		public Resource getResource(String location) {
			Assert.notNull(location, "Location must not be null");
			if (location.startsWith(CLASSPATH_URL_PREFIX)) {
				return new ClassPathResource(location.substring(CLASSPATH_URL_PREFIX.length()), getClassLoader());
			}
			if (location.startsWith(FILE_URL_PREFIX)) {
				return this.files.getResource(location);
			}
			try {
				// Try to parse the location as a URL...
				URL url = new URL(location);
				return new UrlResource(url);
			}
			catch (MalformedURLException ex) {
				// No URL -> resolve as resource path.
				return getResourceByPath(location);
			}
		}

	}

}
