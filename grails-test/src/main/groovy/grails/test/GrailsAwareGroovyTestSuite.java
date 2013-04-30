/*
 * Copyright 2004-2005 Graeme Rocher
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

import java.io.File;
import java.util.List;

import junit.framework.Test;
import junit.textui.TestRunner;

import org.codehaus.groovy.grails.compiler.injection.ClassInjector;
import org.codehaus.groovy.grails.compiler.injection.GrailsAwareClassLoader;

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
 *
 * @deprecated
 * @since 1.1
 */
@Deprecated
public class GrailsAwareGroovyTestSuite extends GroovyTestSuite {

    private GrailsAwareClassLoader gcl;
    private BuildSettings grailsSettings;
    protected boolean registerContextClassLoader = true;

    /**
     * Constructor.
     */
    public GrailsAwareGroovyTestSuite() {
        initBuildSettings();
        createClassLoader();
        if (registerContextClassLoader) {
            Thread.currentThread().setContextClassLoader(gcl);
        }
    }

    private void createClassLoader() {
        gcl = new GrailsAwareClassLoader(getClass().getClassLoader());
        customizeClassLoader(gcl);
    }

    protected void customizeClassLoader(GrailsAwareClassLoader classLoader) {
        // do nothing by default
    }

    protected void customizeClassInjectors(List<ClassInjector> classInjectors) {
        // do nothing by default
    }

    protected void customizeGrailsResources(List<org.codehaus.groovy.grails.io.support.Resource> grailsResources) {
        // do nothing by default
    }

    private void initBuildSettings() {
        final File basedir = findBaseDir();
        final File grailsHome = findGrailsHome();
        System.setProperty(BuildSettings.APP_BASE_DIR, basedir.getPath());
        grailsSettings = new BuildSettings(grailsHome, basedir);
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

    protected void customizeBuildSettings(BuildSettings settings) {
        // do nothing by default
    }

    public static Test suite() {
        GrailsAwareGroovyTestSuite suite = new GrailsAwareGroovyTestSuite();
        try {
            suite.loadTestSuite();
        }
        catch (Exception e) {
            throw new RuntimeException("Could not create the test suite: " + e.getMessage(), e);
        }
        return suite;
    }

    public static void main(String args[]) {
        if (args.length > 0) {
            file = args[0];
        }
        TestRunner.run(suite());
    }

    @Override
    public Class<?> compile(String fileName) throws Exception {
        return gcl.parseClass(new File(fileName));
    }
}
