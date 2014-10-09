/*
 * Copyright 2008 the original author or authors.
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
package grails.util

import java.util.regex.Pattern

/**
 * Build time settings and configuration
 *
 * @author Graeme Rocher
 */
class BuildSettings {

    static final String BUILD_SCOPE = "build"
    static final String COMPILE_SCOPE = "compile"
    static final String RUNTIME_SCOPE = "runtime"
    static final String TEST_SCOPE = "test"
    static final String PROVIDED_SCOPE = "provided"

    static final String BUILD_SCOPE_DESC = "Dependencies for the build system only"
    static final String COMPILE_SCOPE_DESC = "Dependencies placed on the classpath for compilation"
    static final String RUNTIME_SCOPE_DESC = "Dependencies needed at runtime but not for compilation"
    static final String TEST_SCOPE_DESC = "Dependencies needed for test compilation and execution but not at runtime"
    static final String PROVIDED_SCOPE_DESC = "Dependencies needed at development time, but not during deployment"

    static final Map<String, String> SCOPE_TO_DESC = [
            (BUILD_SCOPE): BUILD_SCOPE_DESC,
            (PROVIDED_SCOPE): PROVIDED_SCOPE_DESC,
            (COMPILE_SCOPE): COMPILE_SCOPE_DESC,
            (RUNTIME_SCOPE): RUNTIME_SCOPE_DESC,
            (TEST_SCOPE): TEST_SCOPE_DESC
    ]

    static final Pattern JAR_PATTERN = ~/^\S+\.jar$/

    /**
     * The compiler source level to use
     */
    public static final String COMPILER_SOURCE_LEVEL = "grails.project.source.level"

    /**
     * The dependency resolver to use
     */
    public static final String DEPENDENCY_RESOLVER = "grails.project.dependency.resolver"

    /**
     * The compiler source level to use
     */
    public static final String COMPILER_TARGET_LEVEL = "grails.project.target.level"
    /**
     * The version of the servlet API
     */
    public static final String SERVLET_VERSION = "grails.servlet.version"
    /**
     * The base directory of the application
     */
    public static final String APP_BASE_DIR = "base.dir"
    /**
     * The name of the system property for {@link #grailsWorkDir}.
     */
    public static final String WORK_DIR = "grails.work.dir"

    /**
     * The name of the system property for {@link #projectWorkDir}.
     */
    public static final String PROJECT_WORK_DIR = "grails.project.work.dir"

    public static final String OFFLINE_MODE= "grails.offline.mode"

    /**
     * The name of the system property for {@link #projectWarExplodedDir}.
     */
    public static final String PROJECT_WAR_EXPLODED_DIR = "grails.project.war.exploded.dir"

    /**
     * The name of the system property for {@link #pluginStagingDir}.
     */
    public static final String PLUGIN_STAGING_DIR = "grails.project.plugin.staging.dir"

    /**
     * The name of the system property for {@link #pluginIncludeSource}.
     */
    public static final String PLUGIN_INCLUDE_SOURCE = "grails.project.plugin.includeSource"

    /**
     * The name of the system property for {@link #projectPluginsDir}.
     */
    public static final String PLUGINS_DIR = "grails.project.plugins.dir"

    /**
     * The name of the system property for {@link #globalPluginsDir}.
     */
    public static final String GLOBAL_PLUGINS_DIR = "grails.global.plugins.dir"

    /**
     * The name of the system property for {@link #}.
     */
    public static final String PROJECT_RESOURCES_DIR = "grails.project.resource.dir"

    /**
     * The name of the system property for {@link #sourceDir}.
     */
    public static final String PROJECT_SOURCE_DIR = "grails.project.source.dir"

    /**
     * The name of the system property for {@link BuildSettings#webXmlLocation}.
     */
    public static final String PROJECT_WEB_XML_FILE = "grails.project.web.xml"
    /**
     * The name of the system property for {@link #classesDir}.
     */
    public static final String PROJECT_CLASSES_DIR = "grails.project.class.dir"
    /**
     * The name of the system property for {@link #pluginClassesDir}.
     */
    public static final String PROJECT_PLUGIN_CLASSES_DIR = "grails.project.plugin.class.dir"

    /**
     * The name of the system property for {@link #pluginBuildClassesDir}.
     */
    public static final String PROJECT_PLUGIN_BUILD_CLASSES_DIR = "grails.project.plugin.build.class.dir"

    /**
     * The name of the system property for {@link #pluginProvidedClassesDir}.
     */
    public static final String PROJECT_PLUGIN_PROVIDED_CLASSES_DIR = "grails.project.plugin.provided.class.dir"

    /**
     * The name of the system property for {@link #testClassesDir}.
     */
    public static final String PROJECT_TEST_CLASSES_DIR = "grails.project.test.class.dir"

    /**
     * The name of the system property for {@link #testReportsDir}.
     */
    public static final String PROJECT_TEST_REPORTS_DIR = "grails.project.test.reports.dir"

    /**
     * The name of the system property for {@link #docsOutputDir}.
     */
    public static final String PROJECT_DOCS_OUTPUT_DIR = "grails.project.docs.output.dir"

    /**
     * The name of the system property for {@link #testSourceDir}.
     */
    public static final String PROJECT_TEST_SOURCE_DIR = "grails.project.test.source.dir"

    /**
     * The name of the system property for {@link #projectTargetDir}.
     */
    public static final String PROJECT_TARGET_DIR = "grails.project.target.dir"

    /**
     * The name of the WAR file of the project
     */
    public static final String PROJECT_WAR_FILE = "grails.project.war.file"

    /**
     * The name of the WAR file of the project
     */
    public static final String PROJECT_AUTODEPLOY_DIR = "grails.project.autodeploy.dir"

    /**
     * The name of the system property for enabling osgi headers in the WAR Manifest
     */
    public static final String PROJECT_WAR_OSGI_HEADERS = "grails.project.war.osgi.headers"

    /**
     * The name of the system property for multiple {@link #buildListeners}.
     */
    public static final String BUILD_LISTENERS = "grails.build.listeners"

    /**
     * The name of the system property for enabling verbose compilation {@link #verboseCompile}.
     */
    public static final String VERBOSE_COMPILE = "grails.project.compile.verbose"

    /**
     * A system property with this name is populated in the preparation phase of functional testing
     * with the base URL that tests should be run against.
     */
    public static final String FUNCTIONAL_BASE_URL_PROPERTY = 'grails.testing.functional.baseUrl'

    /**
     * The name of the working directory for commands that don't belong to a project (like create-app)
     */
    public static final String CORE_WORKING_DIR_NAME = '.core'

    /**
     *  A property name to enable/disable AST conversion of closures actions&tags to methods
     */
    public static final String CONVERT_CLOSURES_KEY = "grails.compile.artefacts.closures.convert"

    /**
     *  Property name to enable/disable script event time logging.
     */
    public static final String LOG_SCRIPT_TIMING_KEY = 'grails.script.logTiming'

}
