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
import groovy.lang.Closure;
import groovy.util.AntBuilder;
import org.apache.tools.ant.BuildLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.LogLevel;
import org.codehaus.gant.GantBinding;
import org.codehaus.groovy.grails.cli.api.BaseSettingsApi;
import org.codehaus.groovy.grails.cli.logging.GrailsConsoleAntBuilder;
import org.codehaus.groovy.grails.cli.logging.GrailsConsoleBuildListener;
import org.codehaus.groovy.runtime.MethodClosure;
import org.springframework.util.ReflectionUtils;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Configures the binding used when running Grails scripts
 *
 * @author Graeme Rocher
 * @since 1.4
 *
 */
public class ScriptBindingInitializer {

    private static final Pattern pluginDescriptorPattern = Pattern.compile("^(\\S+)GrailsPlugin.groovy$");

    private BuildSettings settings;
    private PluginPathDiscoverySupport pluginPathSupport;
    private boolean isInteractive;

    public ScriptBindingInitializer(BuildSettings settings, PluginPathDiscoverySupport pluginPathSupport, boolean interactive) {
        this.settings = settings;
        this.pluginPathSupport = pluginPathSupport;
        isInteractive = interactive;
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
     public GantBinding initBinding(final GantBinding binding, String scriptName) {
         Closure<?> c = settings.getGrailsScriptClosure();
         c.setDelegate(binding);
         binding.setVariable("grailsScript", c);
         binding.setVariable("console", GrailsConsole.getInstance());
         binding.setVariable("grailsSettings", settings);

         // Add other binding variables, such as Grails version and environment.
         final File basedir = settings.getBaseDir();
         final String baseDirPath = basedir.getPath();
         binding.setVariable("basedir", baseDirPath);
         binding.setVariable("scaffoldDir", baseDirPath + "/web-app/WEB-INF/templates/scaffolding");
         binding.setVariable("baseFile", basedir);
         binding.setVariable("baseName", basedir.getName());
         binding.setVariable("grailsHome", (settings.getGrailsHome() != null ? settings.getGrailsHome().getPath() : null));
         binding.setVariable("grailsVersion", settings.getGrailsVersion());
         binding.setVariable("userHome", settings.getUserHome());
         binding.setVariable("grailsEnv", settings.getGrailsEnv());
         binding.setVariable("defaultEnv", Boolean.valueOf(settings.getDefaultEnv()));
         binding.setVariable("buildConfig", settings.getConfig());
         binding.setVariable("rootLoader", settings.getRootLoader());
         binding.setVariable("configFile", new File(baseDirPath + "/grails-app/conf/Config.groovy"));

         // Add the project paths too!
         String grailsWork = settings.getGrailsWorkDir().getPath();
         binding.setVariable("grailsWorkDir", grailsWork);
         binding.setVariable("projectWorkDir", settings.getProjectWorkDir().getPath());
         binding.setVariable("projectTargetDir", settings.getProjectTargetDir());
         binding.setVariable("classesDir", settings.getClassesDir());
         binding.setVariable("pluginClassesDir", settings.getPluginClassesDir());
         binding.setVariable("grailsTmp", grailsWork +"/tmp");
         binding.setVariable("classesDirPath", settings.getClassesDir().getPath());
         binding.setVariable("pluginClassesDirPath", settings.getPluginClassesDir().getPath());
         binding.setVariable("testDirPath", settings.getTestClassesDir().getPath());
         final String resourcesDir = settings.getResourcesDir().getPath();
         binding.setVariable("resourcesDirPath", resourcesDir);
         binding.setVariable("webXmlFile", settings.getWebXmlLocation());
         binding.setVariable("pluginsDirPath", settings.getProjectPluginsDir().getPath());
         binding.setVariable("globalPluginsDirPath", settings.getGlobalPluginsDir().getPath());

         final BaseSettingsApi cla = new BaseSettingsApi(settings, isInteractive);

         // Enable UAA for run-app because it is likely that the container will be running long enough to report useful info
         if (scriptName.equals("RunApp")) {
             cla.enableUaa();
         }

         makeApiAvailableToScripts(binding, cla);
         makeApiAvailableToScripts(binding, cla.getPluginSettings());

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
                     GrailsConsole.getInstance().log("Cannot find plugin descriptor for path '" + dir.getPath() + "'.");
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

     private void setUIListener(GantBinding binding) {
         AntBuilder ant = (AntBuilder) binding.getVariable("ant");

         Project project = ant.getProject();
         project.getBuildListeners().clear();
         GrailsConsoleAntBuilder.addGrailsConsoleBuildListener(project);

         GrailsConsole instance = GrailsConsole.getInstance();
         project.addBuildListener(new GrailsConsoleBuildListener(instance));

         if(!instance.isVerbose()) {
             Vector buildListeners = project.getBuildListeners();
             for (Object buildListener : buildListeners) {
                 if(buildListener instanceof BuildLogger) {
                     ((BuildLogger)buildListener).setMessageOutputLevel(LogLevel.ERR.getLevel());
                 }
             }
         }

     }

     protected void makeApiAvailableToScripts(final GantBinding binding, final Object cla) {
         final Method[] declaredMethods = cla.getClass().getDeclaredMethods();
         for (Method method : declaredMethods) {
             final String name = method.getName();

             final int modifiers = method.getModifiers();
             if (Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers)) {
                 binding.setVariable(name, new MethodClosure(cla, name));
             }
         }

         PropertyDescriptor[] propertyDescriptors;
         try {
             propertyDescriptors = Introspector.getBeanInfo(cla.getClass()).getPropertyDescriptors();
             for (PropertyDescriptor pd : propertyDescriptors) {
                 final Method readMethod = pd.getReadMethod();
                 if (readMethod != null) {
                     if (isDeclared(cla, readMethod)) {
                         binding.setVariable(pd.getName(), ReflectionUtils.invokeMethod(readMethod, cla));
                     }
                 }
             }
         }
         catch (IntrospectionException e1) {
             // ignore
         }
     }

     protected boolean isDeclared(final Object cla, final Method readMethod) {
         try {
             return cla.getClass().getDeclaredMethod(readMethod.getName(),
                 readMethod.getParameterTypes()) != null;
         }
         catch (Exception e) {
             return false;
         }
     }

}
