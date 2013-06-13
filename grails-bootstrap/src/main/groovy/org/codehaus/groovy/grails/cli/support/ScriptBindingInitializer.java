/*
 * Copyright 2011 SpringSource
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
package org.codehaus.groovy.grails.cli.support;

import grails.build.logging.GrailsConsole;
import grails.util.BuildSettings;
import grails.util.GrailsNameUtils;
import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GroovyClassLoader;
import groovy.util.AntBuilder;

import java.io.File;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tools.ant.Project;
import org.codehaus.gant.GantBinding;
import org.codehaus.groovy.grails.cli.api.BaseSettingsApi;
import org.codehaus.groovy.grails.cli.logging.GrailsConsoleAntBuilder;
import org.codehaus.groovy.grails.cli.parsing.CommandLine;

/**
 * Configures the binding used when running Grails scripts.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
public class ScriptBindingInitializer {

    private static final Pattern pluginDescriptorPattern = Pattern.compile("^(\\S+)GrailsPlugin.groovy$");
    public static final String GRAILS_SCRIPT = "grailsScript";
    public static final String GRAILS_CONSOLE = "grailsConsole";
    public static final String GRAILS_SETTINGS = "grailsSettings";
    public static final String BASEDIR = "basedir";
    public static final String SCAFFOLD_DIR = "scaffoldDir";
    public static final String BASE_FILE = "baseFile";
    public static final String BASE_NAME = "baseName";
    public static final String GRAILS_HOME = "grailsHome";
    public static final String GRAILS_VERSION = "grailsVersion";
    public static final String USER_HOME = "userHome";
    public static final String GRAILS_ENV = "grailsEnv";
    public static final String ARGS_MAP = "argsMap";

    private BuildSettings settings;
    private PluginPathDiscoverySupport pluginPathSupport;
    private CommandLine commandLine;
    private URLClassLoader classLoader;
    private boolean interactive;

    public ScriptBindingInitializer(CommandLine commandLine, URLClassLoader classLoader, BuildSettings settings,
            PluginPathDiscoverySupport pluginPathSupport, boolean interactive) {
        this.commandLine = commandLine;
        this.settings = settings;
        this.pluginPathSupport = pluginPathSupport;
        this.classLoader = classLoader;
        this.interactive = interactive;
    }

    /**
     * Prep the binding. We add the location of GRAILS_HOME under
     * the variable name "grailsHome". We also add a closure that
     * should be used with "includeTargets <<" - it takes a string
     * and returns either a file containing the named Grails script
     * or the script class.
     *
     * So, this:
     *
     *   includeTargets << grailsScript("Init")
     *
     * will load the "Init" script from $GRAILS_HOME/scripts if it
     * exists there; otherwise it will load the Init class.
     */
    @SuppressWarnings("unchecked")
    public GantBinding initBinding(final GantBinding binding, String scriptName) {
         BuildSettings buildSettings = settings;
         Closure<?> c = buildSettings.getGrailsScriptClosure();
         c.setDelegate(binding);
         @SuppressWarnings("rawtypes")
         Map argsMap = new LinkedHashMap(commandLine.getUndeclaredOptions());
         argsMap.put("params", commandLine.getRemainingArgs());
         binding.setVariable(ARGS_MAP, argsMap);
         binding.setVariable("args", commandLine.getRemainingArgsLineSeparated());
         binding.setVariable(GRAILS_SCRIPT, c);
         URLClassLoader classLoader = this.classLoader;
         final GrailsConsole grailsConsole = GrailsConsole.getInstance();
         final File basedir = buildSettings.getBaseDir();

         BaseSettingsApi cla = initBinding(binding, buildSettings, classLoader, grailsConsole, interactive);

         // Enable UAA for run-app because it is likely that the container will be running long enough to report useful info
         if (scriptName.equals("RunApp")) {
             cla.enableUaa();
         }

         // setup Ant alias for older scripts
         binding.setVariable("Ant", binding.getVariable("ant"));

         // Hide the deprecation warnings that occur with plugins that
         // use "Ant" instead of "ant".
         // TODO Remove this after 1.1 is released. Plugins should be
         // able to safely switch to "ant" by then (few people should
         // still be on 1.0.3 or earlier).
         setUIListener(binding);

         // Create binding variables that contain the locations of each of the
         // plugins loaded by the application. The name of each variable is of
         // the form <pluginName>PluginDir.
         try {
             // First, if this is a plugin project, we need to add its descriptor.
             List<File> descriptors = new ArrayList<File>();
             File desc = pluginPathSupport.getPluginDescriptor(basedir);
             if (desc != null) descriptors.add(desc);

             // Next add all those of installed plugins.
             for (File dir : pluginPathSupport.listKnownPluginDirs()) {
                 File pluginDescriptor = pluginPathSupport.getPluginDescriptor(dir);
                 if (pluginDescriptor != null) {
                     descriptors.add(pluginDescriptor);
                 }
                 else {
                     grailsConsole.log("Cannot find plugin descriptor for path '" + dir.getPath() + "'.");
                 }
             }

             // Go through all the descriptors and add the appropriate binding
             // variable for each one that contains the location of its plugin directory.
             for (File file : descriptors) {
                 Matcher matcher = pluginDescriptorPattern.matcher(file.getName());
                 matcher.find();
                 String pluginName = GrailsNameUtils.getPropertyName(matcher.group(1));

                 // Add the plugin path to the binding.
                 binding.setVariable(pluginName + "PluginDir", file.getParentFile());
             }
         }
         catch (Exception e) {
             // No plugins found.
         }

         return binding;
     }

    public static BaseSettingsApi initBinding(Binding binding, BuildSettings buildSettings, URLClassLoader classLoader,
            GrailsConsole grailsConsole, boolean interactive) {
        binding.setVariable(GRAILS_CONSOLE, grailsConsole);
        binding.setVariable(GRAILS_SETTINGS, buildSettings);

        // Add other binding variables, such as Grails version and environment.
        final File basedir = buildSettings.getBaseDir();
        final String baseDirPath = basedir.getPath();
        binding.setVariable(BASEDIR, baseDirPath);
        binding.setVariable(SCAFFOLD_DIR, baseDirPath + "/web-app/WEB-INF/templates/scaffolding");
        binding.setVariable(BASE_FILE, basedir);
        binding.setVariable(BASE_NAME, basedir.getName());
        binding.setVariable(GRAILS_HOME, (buildSettings.getGrailsHome() != null ? buildSettings.getGrailsHome().getPath() : null));
        binding.setVariable(GRAILS_VERSION, buildSettings.getGrailsVersion());
        binding.setVariable(USER_HOME, buildSettings.getUserHome());
        binding.setVariable(GRAILS_ENV, buildSettings.getGrailsEnv());
        binding.setVariable("defaultEnv", buildSettings.getDefaultEnv());
        binding.setVariable("buildConfig", buildSettings.getConfig());
        binding.setVariable("rootLoader", buildSettings.getRootLoader());
        binding.setVariable("configFile", buildSettings.getConfigFile());

        // Add the project paths too!
        String grailsWork = buildSettings.getGrailsWorkDir().getPath();
        binding.setVariable("grailsWorkDir", grailsWork);
        binding.setVariable("projectWorkDir", buildSettings.getProjectWorkDir().getPath());
        binding.setVariable("projectTargetDir", buildSettings.getProjectTargetDir());
        binding.setVariable("classesDir", buildSettings.getClassesDir());
        binding.setVariable("pluginClassesDir", buildSettings.getPluginClassesDir());
        binding.setVariable("grailsTmp", grailsWork +"/tmp");
        binding.setVariable("classesDirPath", buildSettings.getClassesDir().getPath());
        binding.setVariable("pluginClassesDirPath", buildSettings.getPluginClassesDir().getPath());
        binding.setVariable("testDirPath", buildSettings.getTestClassesDir().getPath());
        final String resourcesDir = buildSettings.getResourcesDir().getPath();
        binding.setVariable("resourcesDirPath", resourcesDir);
        binding.setVariable("webXmlFile", buildSettings.getWebXmlLocation());
        binding.setVariable("pluginsDirPath", buildSettings.getProjectPluginsDir().getPath());
        binding.setVariable("globalPluginsDirPath", buildSettings.getGlobalPluginsDir().getPath());

        GroovyClassLoader eventsClassLoader = new GroovyClassLoader(classLoader);
        GrailsBuildEventListener buildEventListener = new GrailsBuildEventListener(eventsClassLoader, binding, buildSettings);
        binding.setVariable("eventsClassLoader", eventsClassLoader);
        binding.setVariable("eventListener", buildEventListener);
        if (binding instanceof GantBinding) {
            ((GantBinding)binding).addBuildListener(buildEventListener);
        }

        final BaseSettingsApi cla = new BaseSettingsApi(buildSettings, buildEventListener, interactive);

        cla.makeApiAvailableToScripts(binding, cla);
        cla.makeApiAvailableToScripts(binding, cla.getPluginSettings());

        return cla;
    }

    private void setUIListener(GantBinding binding) {
         AntBuilder ant = (AntBuilder) binding.getVariable("ant");

         Project project = ant.getProject();
         project.getBuildListeners().clear();
         GrailsConsoleAntBuilder.addGrailsConsoleBuildListener(project);
     }
}
