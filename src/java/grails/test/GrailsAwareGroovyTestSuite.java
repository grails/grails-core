/* Copyright 2004-2005 Graeme Rocher
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
package grails.test;

import grails.util.BuildSettings;
import grails.util.BuildSettingsHolder;
import groovy.util.GroovyTestSuite;
import grails.util.PluginBuildSettings;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.Test;
import junit.textui.TestRunner;

import org.codehaus.groovy.grails.compiler.injection.ClassInjector;
import org.codehaus.groovy.grails.compiler.injection.DefaultGrailsDomainClassInjector;
import org.codehaus.groovy.grails.compiler.injection.GrailsAwareClassLoader;
import org.codehaus.groovy.grails.compiler.support.GrailsResourceLoader;
import org.springframework.core.io.Resource;

/**
 *
 * Adds support for running Grails JUnit Tests from Eclipse JUnit runner or even
 * from the command line.
 *
 * Set GRAILS_HOME environment variable before running the test. Change the
 * working directory to the Grails application's root directory.
 *
 * There are several extension points (protected template methods customize*) for customizing behaviour in a subclass.
 * You will have to copy&paste the suite() and main() methods to the subclass because they are static methods
 *
 * Contributed by Lari Hotari
 *
 * @author Graeme Rocher
 * @author Lari Hotari
 * @since 1.1
 *
 */
public class GrailsAwareGroovyTestSuite extends GroovyTestSuite {
	private GrailsAwareClassLoader gcl;
	private BuildSettings grailsSettings;
	private PluginBuildSettings pluginSettings;
	protected boolean registerContextClassLoader = true;

	public GrailsAwareGroovyTestSuite() {
		initGrails();
	}

	private void initGrails() {
		initBuildSettings();
		createClassLoader();
		if (registerContextClassLoader) {
			Thread.currentThread().setContextClassLoader(gcl);
		}
	}

	private void createClassLoader() {
		gcl = new GrailsAwareClassLoader(getClass().getClassLoader());
		gcl.setClassInjectors(resolveClassInjectors());
		gcl.setResourceLoader(new GrailsResourceLoader(resolveGrailsResources()));
		customizeClassLoader(gcl);
	}

	protected void customizeClassLoader(GrailsAwareClassLoader gcl) {

	}

	private ClassInjector[] resolveClassInjectors() {
		List<ClassInjector> classInjectors = new ArrayList<ClassInjector>();
		classInjectors.add(new DefaultGrailsDomainClassInjector());
		customizeClassInjectors(classInjectors);
		return classInjectors.toArray(new ClassInjector[classInjectors.size()]);
	}

	protected void customizeClassInjectors(List<ClassInjector> classInjectors) {

	}

	private Resource[] resolveGrailsResources() {
		Resource[] baseResources = pluginSettings.getArtefactResources();
		List<Resource> grailsResources = new ArrayList<Resource>(Arrays.asList(baseResources));
		customizeGrailsResources(grailsResources);
		return grailsResources.toArray(new Resource[grailsResources.size()]);
	}

	protected void customizeGrailsResources(List<Resource> grailsResources) {

	}

	private void initBuildSettings() {
		final File basedir = findBaseDir();
		final File grailsHome = findGrailsHome();
		System.setProperty(BuildSettings.APP_BASE_DIR, basedir.getPath());
		grailsSettings = new BuildSettings(grailsHome, basedir);
                pluginSettings = new PluginBuildSettings(grailsSettings);
		customizeBuildSettings(grailsSettings);
		BuildSettingsHolder.setSettings(grailsSettings);
	}

	protected File findBaseDir() {
		String basedir = System.getProperty(BuildSettings.APP_BASE_DIR);
		if (basedir == null) {
			basedir = ".";
		}
		return new File(basedir);
	}

	protected File findGrailsHome() {
		final String grailsHome = System.getenv("GRAILS_HOME");
		if (grailsHome == null) {
			throw new RuntimeException("You must set the GRAILS_HOME environment variable");
		}
		return new File(grailsHome);
	}

	protected void customizeBuildSettings(BuildSettings grailsSettings) {

	}

	public static Test suite() {
		GrailsAwareGroovyTestSuite suite = new GrailsAwareGroovyTestSuite();
		try {
			suite.loadTestSuite();
		} catch (Exception e) {
			throw new RuntimeException((new StringBuilder()).append("Could not create the test suite: ").append(e)
					.toString(), e);
		}
		return suite;
	}

	public static void main(String args[]) {
		if (args.length > 0)
			file = args[0];
		TestRunner.run(suite());
	}

	@Override
	public Class compile(String fileName) throws Exception {
		return gcl.parseClass(new File(fileName));
	}
}
