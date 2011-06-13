/*
 * Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.cli;

import static org.apache.commons.cli.OptionBuilder.withArgName;
import gant.Gant;
import grails.build.logging.GrailsConsole;
import grails.util.BuildSettings;
import grails.util.BuildSettingsHolder;
import grails.util.CosineSimilarity;
import grails.util.Environment;
import grails.util.GrailsNameUtils;
import grails.util.PluginBuildSettings;
import groovy.lang.Closure;
import groovy.lang.ExpandoMetaClass;
import groovy.lang.GroovyObject;
import groovy.lang.GroovySystem;
import groovy.util.AntBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.tools.ant.Project;
import org.codehaus.gant.GantBinding;
import org.codehaus.gant.GantMetaClass;
import org.codehaus.groovy.grails.cli.interactive.InteractiveMode;
import org.codehaus.groovy.grails.cli.support.ClasspathConfigurer;
import org.codehaus.groovy.grails.cli.support.PluginPathDiscoverySupport;
import org.codehaus.groovy.grails.cli.support.ScriptBindingInitializer;
import org.codehaus.groovy.grails.plugins.GrailsPluginUtils;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.Log4jConfigurer;

/**
 * Handles Grails command line interface for running scripts.
 *
 * @author Graeme Rocher
 *
 * @since 0.4
 */
public class GrailsScriptRunner {

    private static Map<String, String> ENV_ARGS = new HashMap<String, String>();
    // this map contains default environments for several scripts in form 'script-name':'env-code'
    private static Map<String, String> DEFAULT_ENVS = new HashMap<String, String>();

    static {
        ENV_ARGS.put("dev", Environment.DEVELOPMENT.getName());
        ENV_ARGS.put("prod", Environment.PRODUCTION.getName());
        ENV_ARGS.put("test", Environment.TEST.getName());
        DEFAULT_ENVS.put("War", Environment.PRODUCTION.getName());
        DEFAULT_ENVS.put("TestApp", Environment.TEST.getName());
        DEFAULT_ENVS.put("RunWebtest", Environment.TEST.getName());
        ExpandoMetaClass.enableGlobally();
        // disable annoying ehcache up-to-date check
        System.setProperty("net.sf.ehcache.skipUpdateCheck", "true");
    }

    private static final Pattern scriptFilePattern = Pattern.compile("^[^_]\\w+\\.groovy$");

    public static final String VERBOSE_ARGUMENT = "verbose";
    public static final String AGENT_ARGUMENT = "reloading";
    public static final String VERSION_ARGUMENT = "version";
    public static final String HELP_ARGUMENT = "help";
    public static final String NON_INTERACTIVE_ARGUMENT = "nonInteractive";
    @SuppressWarnings("rawtypes")
    public static final Closure DO_NOTHING_CLOSURE = new Closure(GrailsScriptRunner.class) {
        private static final long serialVersionUID = 1L;
        @Override public Object call(Object arguments) { return null; }
        @Override public Object call() { return null; }
        @Override public Object call(Object... args) { return null; }
    };
    public static final String NOANSI_ARGUMENT = "plainOutput";
    private static InputStream originalIn;
    private static PrintStream originalOut;

    private PluginPathDiscoverySupport pluginPathSupport;
    private BuildSettings settings;
    private PrintStream out = System.out;

    private boolean isInteractive = true;
    private URLClassLoader classLoader;
    private GrailsConsole console = GrailsConsole.getInstance();
    private File scriptCacheDir;

    private final List<Resource> scriptsAllowedOutsideOfProject = new ArrayList<Resource>();

    public GrailsScriptRunner() {
        this(new BuildSettings());
    }

    public GrailsScriptRunner(String grailsHome) {
        this(new BuildSettings(new File(grailsHome)));
    }

    public GrailsScriptRunner(BuildSettings settings) {
        if (originalIn == null) {
            originalIn = System.in;
            originalOut = System.out;
        }
        this.settings = settings;
        this.pluginPathSupport = new PluginPathDiscoverySupport(settings);
    }

    public void setInteractive(boolean interactive) {
        isInteractive = interactive;
    }

    /**
     * Evaluate the arguments to get the name of the script to execute, which environment
     * to run it in, and the arguments to pass to the script. This also evaluates arguments
     * of the form "-Dprop=value" and creates system properties from each one.
     *
     * @param args Command line arguments
     */
    @SuppressWarnings("static-access")
    public static void main(String[] args) {
        originalIn = System.in;
        originalOut = System.out;

        CommandLineParser parser = new GnuParser();
        args = splitAndTrimArgs(args);


        Options options = new Options();
        options.addOption(new Option(VERBOSE_ARGUMENT, "Enable verbose output"));
        options.addOption(new Option(AGENT_ARGUMENT, "Enable the reloading agent"));
        options.addOption(new Option(NON_INTERACTIVE_ARGUMENT, "Whether to allow the command line to request input"));
        options.addOption(new Option(HELP_ARGUMENT, "Command line help"));
        options.addOption(new Option(VERSION_ARGUMENT, "Current Grails version"));
        options.addOption(new Option(NOANSI_ARGUMENT, "Disables ANSI output"));

        options.addOption(withArgName("property=value")
                                 .hasArgs(2)
                                 .withValueSeparator()
                                 .withDescription("Used to specify System properties")
                                 .create("D"));
        GrailsConsole console = GrailsConsole.getInstance();
        CommandLine commandLine;

        try {
            commandLine = parser.parse(options, args);
            if (commandLine.hasOption(NOANSI_ARGUMENT)) {
                console.setAnsiEnabled(false);
            }
        } catch (ParseException e) {
            console.error("Error processing command line arguments: " + e.getMessage());
            System.exit(1);
            return;
        }

        console.updateStatus("Initializing");

        ScriptAndArgs script = processArgumentsAndReturnScriptName(commandLine);

        // Get hold of the GRAILS_HOME environment variable if it is available.
        String grailsHome = System.getProperty("grails.home");

        // Now we can pick up the Grails version from the Ant project properties.
        BuildSettings build = null;
        try {
            build = new BuildSettings(new File(grailsHome));
            if (build.getRootLoader() == null) {
                build.setRootLoader((URLClassLoader) GrailsScriptRunner.class.getClassLoader());
            }

        }
        catch (Exception e) {
            exitWithError("An error occurred loading the grails-app/conf/BuildConfig.groovy file: " + e.getMessage());
        }

        // Check that Grails' home actually exists.
        final File grailsHomeInSettings = build.getGrailsHome();
        if (grailsHomeInSettings == null || !grailsHomeInSettings.exists()) {
            exitWithError("Grails' installation directory not found: " + build.getGrailsHome());
        }

        if (commandLine.hasOption(VERSION_ARGUMENT)) {
            console.log("Grails version: " + build.getGrailsVersion());
            System.exit(0);
        }

        if (commandLine.hasOption(HELP_ARGUMENT)) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("grails [options] [command]",options);
            System.exit(0);
        }

        // If there aren't any arguments, then we don't have a command
        // to execute. So we have to exit.
        GrailsScriptRunner scriptRunner = new GrailsScriptRunner(build);
        scriptRunner.setInteractive(!commandLine.hasOption(NON_INTERACTIVE_ARGUMENT));
        if (script.name == null) {
            console.updateStatus("Loading build configuration");

            build.loadConfig();
            scriptRunner.initializeState();
            try {
                new InteractiveMode(build, scriptRunner).run();
            } catch (Throwable e) {
                console.error("Interactive mode exited with error: " + e.getMessage(), e);
            }
        }
        else {
            console.getCategory().push(script.inputName);
            console.verbose("Base Directory: " + build.getBaseDir().getPath());

            try {
                int exitCode = scriptRunner.executeCommand(
                        script.name, script.args, script.env);
                System.exit(exitCode);
            }
            catch (ScriptNotFoundException ex) {
                console.error("Script not found: " + ex.getScriptName());
            }
            catch (Throwable t) {
                String msg = "Error executing script " + script.name + ": " + t.getMessage();
                sanitizeStacktrace(t);
                t.printStackTrace(System.out);
                exitWithError(msg);
            }
        }
    }

    private static String[] splitAndTrimArgs(String[] args) {
        StringBuilder allArgs = new StringBuilder("");
        for (String arg : args) {
            arg = arg.trim();
            if (arg.length()>0) {
                allArgs.append(" ").append(arg);
            }
        }

        String allArgsString = allArgs.toString().trim();
        if (allArgsString.length() == 0) {
            return new String[0];
        }
        return allArgsString.split(" ");
    }

    private static void exitWithError(String error) {
        GrailsConsole.getInstance().error(error);
        System.exit(1);
    }

    private static ScriptAndArgs processArgumentsAndReturnScriptName(CommandLine commandLine) {

        if (commandLine.hasOption(VERBOSE_ARGUMENT)) {
            GrailsConsole.getInstance().setVerbose(true);
        }

        processSystemArguments(commandLine);
        String[] arguments = commandLine.getArgs();

        if (arguments.length > 0) {
            return processAndReturnArguments(arguments);
        }
        return new ScriptAndArgs();
    }

    private static ScriptAndArgs processAndReturnArguments(String[] arguments) {
        ScriptAndArgs info = new ScriptAndArgs();
        int currentParamIndex = 0;
        if (Environment.isSystemSet()) {
            info.env = Environment.getCurrent().getName();
        }
        else if (isEnvironmentArgs(arguments[currentParamIndex])) {
            // use first argument as environment name and step further
            String env = arguments[currentParamIndex++];
            info.env = ENV_ARGS.get(env);
        }

        abortIfOutOfBounds(arguments, currentParamIndex);
        // use current argument as script name and step further
        String paramName = arguments[currentParamIndex++];


        if (paramName.charAt(0) == '-') {
            paramName = paramName.substring(1);
        }
        info.inputName = paramName;
        info.name = GrailsNameUtils.getNameFromScript(paramName);

        if (currentParamIndex < arguments.length) {
            // if we have additional params provided - store it in system property
            StringBuilder b = new StringBuilder(arguments[currentParamIndex]);
            for (int i = currentParamIndex + 1; i < arguments.length; i++) {
                b.append(' ').append(arguments[i]);
            }
            info.args = b.toString();
        }
        return info;
    }

    private static void abortIfOutOfBounds(String[] splitArgs, int currentParamIndex) {
        if (currentParamIndex >= splitArgs.length) {
            GrailsConsole.getInstance().error("You should specify a script to run. Run 'grails help' for a complete list of available scripts.");
            System.exit(0);
        }
    }

    private static void processSystemArguments(CommandLine allArgs) {
        Properties systemProps = allArgs.getOptionProperties("D");
        if (systemProps != null) {
            for (Map.Entry<Object, Object> entry : systemProps.entrySet()) {
                System.setProperty(entry.getKey().toString(), entry.getValue().toString());
            }
        }
    }

    private static boolean isEnvironmentArgs(String env) {
        return ENV_ARGS.containsKey(env);
    }

    public PrintStream getOut() {
        return out;
    }

    public void setOut(PrintStream outputStream) {
        this.out = outputStream;
    }

    public int executeCommand(String scriptName, String args) {
        return executeCommand(scriptName, args, null);
    }

    public int executeCommand(String scriptName, String args, String env) {
        // Populate the root loader with all libraries that this app
        // depends on. If a root loader doesn't exist yet, create it now.

        if (args != null) {
            System.setProperty("grails.cli.args", args.replace(' ', '\n'));
        }
        else {
            // If GrailsScriptRunner is executed more than once in a
            // single JVM, we have to make sure that the CLI args are reset.
            System.setProperty("grails.cli.args", "");
        }

        @SuppressWarnings("hiding") GrailsConsole console = GrailsConsole.getInstance();
        // Load the BuildSettings file for this project if it exists. Note
        // that this does not load any environment-specific settings.
        try {
            System.setProperty("disable.grails.plugin.transform", "true");

            console.updateStatus("Loading build config");
            settings.loadConfig();

            System.setProperty("springloaded.directoriesContainingReloadableCode", settings.getClassesDir().getAbsolutePath() + ',' + settings.getPluginClassesDir().getAbsolutePath());
        }
        catch (Exception e) {
            e.printStackTrace(System.err);
            System.err.println("WARNING: There was an error loading the BuildConfig: " + e.getMessage());
            System.exit(1);
        }
        finally {
            System.setProperty("disable.grails.plugin.transform", "false");
        }

        // Add some extra binding variables that are now available.
        // settings.setGrailsEnv(env);
        // settings.setDefaultEnv(useDefaultEnv);

        BuildSettingsHolder.setSettings(settings);

        return callPluginOrGrailsScript(scriptName, env);
    }

    private void setRunningEnvironment(String scriptName, String env) {
        // Get the default environment if one hasn't been set.
        boolean useDefaultEnv = env == null;
        if (useDefaultEnv) {
            env = DEFAULT_ENVS.get(scriptName);
            env = env != null ? env : Environment.DEVELOPMENT.getName();
        }

        System.setProperty("base.dir", settings.getBaseDir().getPath());
        System.setProperty(Environment.KEY, env);
        System.setProperty(Environment.DEFAULT, "true");

        // Add some extra binding variables that are now available.
        settings.setGrailsEnv(env);
        settings.setDefaultEnv(useDefaultEnv);
    }

    private int callPluginOrGrailsScript(String scriptName, String env) {
        initializeState(scriptName);
        return executeScriptWithCaching(scriptName, env);
    }

    public int executeScriptWithCaching(String scriptName, String env, String args) {
        if (args != null) {
            System.setProperty("grails.cli.args", args.replace(' ', '\n'));
        }
        return executeScriptWithCaching(scriptName, env);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public int executeScriptWithCaching(String scriptName, String env) {
        List<Resource> potentialScripts;
        List<Resource> allScripts = getAvailableScripts();
        GantBinding binding = new GantBinding();
        setDefaultInputStream(binding);

        // Now find what scripts match the one requested by the user.
        boolean exactMatchFound = false;
        potentialScripts = new ArrayList<Resource>();
        for (Resource scriptPath : allScripts) {
            String scriptFileName = scriptPath.getFilename().substring(0,scriptPath.getFilename().length()-7); // trim .groovy extension
            if (scriptFileName.endsWith("_")) {
                scriptsAllowedOutsideOfProject.add(scriptPath);
                scriptFileName = scriptFileName.substring(0, scriptFileName.length()-1);
            }

            if (scriptFileName.equals(scriptName)) {
                potentialScripts.add(scriptPath);
                exactMatchFound = true;
                continue;
            }

            if (!exactMatchFound && ScriptNameResolver.resolvesTo(scriptName, scriptFileName)) {
                potentialScripts.add(scriptPath);
            }
        }

        // First try to load the script from its file. If there is no
        // file, then attempt to load it as a pre-compiled script. If
        // that fails, then let the user know and then exit.
        if (potentialScripts.size() > 0) {
            potentialScripts = (List) DefaultGroovyMethods.unique(potentialScripts);
            final Resource scriptFile = potentialScripts.get(0);
            if (!isGrailsProject() && !isExternalScript(scriptFile)) {
                return handleScriptExecutedOutsideProjectError();
            }
            return executeScriptFile(scriptName, env, binding, scriptFile);
        }

        return attemptPrecompiledScriptExecute(scriptName, env, binding, allScripts);
    }

    private int attemptPrecompiledScriptExecute(String scriptName, String env, GantBinding binding, List<Resource> allScripts) {
        console.updateStatus("Running pre-compiled script");

        // Must be called before the binding is initialised.
        setRunningEnvironment(scriptName, env);

        // Get Gant to load the class by name using our class loader.
        ScriptBindingInitializer bindingInitializer = new ScriptBindingInitializer(settings, pluginPathSupport,isInteractive);
        Gant gant = new Gant(bindingInitializer.initBinding(binding, scriptName), classLoader);

        try {
            loadScriptClass(gant, scriptName);
        }
        catch (ScriptNotFoundException e) {
            if (isInteractive && !InteractiveMode.isActive()) {
                scriptName = fixScriptName(scriptName, allScripts);
                if (scriptName == null) {
                    throw e;
                }

                loadScriptClass(gant, scriptName);
            } else {
                throw e;
            }
        }

        return executeWithGantInstance(gant, DO_NOTHING_CLOSURE).exitCode;
    }

    private int executeScriptFile(String scriptName, String env, GantBinding binding, Resource scriptFile) {
        // We can now safely set the default environment
        String scriptFileName = getScriptNameFromFile(scriptFile);
        setRunningEnvironment(scriptFileName, env);
        binding.setVariable("scriptName", scriptFileName);

        // Setup the script to call.
        ScriptBindingInitializer bindingInitializer = new ScriptBindingInitializer(settings, pluginPathSupport,isInteractive);
        Gant gant = new Gant(bindingInitializer.initBinding(binding, scriptName), classLoader);
        gant.setUseCache(true);
        gant.setCacheDirectory(scriptCacheDir);
        GantResult result = null;
        try {
            gant.loadScript(scriptFile.getURL());
            result = executeWithGantInstance(gant, DO_NOTHING_CLOSURE);
            return result.exitCode;
        } catch (IOException e) {
            console.error("I/O exception loading script [" + e.getMessage() + "]: " + e.getMessage());
            return 1;
        }
        finally {
            cleanup(result, binding);
        }
    }

    @SuppressWarnings("rawtypes")
    private void cleanup(GantResult result, GantBinding binding) {
        if (result != null) {
            Class cls = GantMetaClass.class;
            try {
                Field methodsInvoked = cls.getDeclaredField("methodsInvoked");
                methodsInvoked.setAccessible(true);
                Set methodsInvokedSet = (Set) methodsInvoked.get(cls);
                if (methodsInvokedSet != null) {
                    methodsInvokedSet.clear();
                }
            } catch (NoSuchFieldException e) {
                // ignore
            } catch (IllegalAccessException e) {
                // ignore
            }
        }
        System.setIn(originalIn);
        System.setOut(originalOut);
        GrailsPluginUtils.clearCaches();
        Map variables = binding.getVariables();
        Object pluginsSettingsObject = variables.get("pluginsSettings");
        if (pluginsSettingsObject instanceof PluginBuildSettings) {
            ((PluginBuildSettings)pluginsSettingsObject).clearCache();
        }
        GroovySystem.getMetaClassRegistry().removeMetaClass(GantBinding.class);
        GroovySystem.getMetaClassRegistry().removeMetaClass(Gant.class);
    }

    public void initializeState() {
        initializeState(null);
    }

    private void initializeState(String scriptName) {
        // The directory where scripts are cached.
        this.scriptCacheDir = new File(settings.getProjectWorkDir(), "scriptCache");
        this.console = GrailsConsole.getInstance();
        // Add the remaining JARs (from 'grailsHome', the app, and
        // the plugins) to the root loader.

        boolean skipPlugins = scriptName != null && ("UninstallPlugin".equals(scriptName) || "InstallPlugin".equals(scriptName));

        console.updateStatus("Configuring classpath");
        ClasspathConfigurer configurer = new ClasspathConfigurer(pluginPathSupport, settings, skipPlugins);
        this.classLoader = configurer.configuredClassLoader();
        initializeLogging();
    }

    private int handleScriptExecutedOutsideProjectError() {
        console.error(settings.getBaseDir().getPath() + " does not appear to be part of a Grails application.");
        console.error("The following commands are supported outside of a project:");
        Collections.sort(scriptsAllowedOutsideOfProject, new Comparator<Resource>() {
            public int compare(Resource resource, Resource resource1) {
                return resource.getFilename().compareTo(resource1.getFilename());
            }
        });
        for (Resource file : scriptsAllowedOutsideOfProject) {
            console.log("\t" + GrailsNameUtils.getScriptName(file.getFilename()));
        }
        console.addStatus("Run 'grails help' for a complete list of available scripts.");
        return -1;
    }

    protected void initializeLogging() {
        if (settings.getGrailsHome() == null) {
            return;
        }

        try {
            Log4jConfigurer.initLogging("file:" + settings.getGrailsHome() + "/scripts/log4j.properties");
        } catch (FileNotFoundException e) {
            // ignore, Log4j will print an error in this case
        }
    }

    private void setDefaultInputStream(GantBinding binding) {

        // Gant does not initialise the default input stream for
        // the Ant project, so we manually do it here.
        AntBuilder antBuilder = (AntBuilder) binding.getVariable("ant");
        Project p = antBuilder.getAntProject();

        try {
            System.setIn(originalIn);
            p.setInputHandler(new CommandLineInputHandler());
            p.setDefaultInputStream(originalIn);
        }
        catch (NoSuchMethodError nsme) {
            // will only happen due to a bug in JRockit
            // note - the only approach that works is to loop through the public methods
            for (Method m : p.getClass().getMethods()) {
                if ("setDefaultInputStream".equals(m.getName()) && m.getParameterTypes().length == 1 &&
                        InputStream.class.equals(m.getParameterTypes()[0])) {
                    try {
                        m.invoke(p, originalIn);
                        break;
                    }
                    catch (Exception e) {
                        // shouldn't happen, but let it bubble up to the catch(Throwable)
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    private void loadScriptClass(Gant gant, String scriptName) {
        try {
            // try externalized script first
            gant.loadScriptClass(scriptName + "_");
        }
        catch (Exception e) {
            try {
                gant.loadScriptClass(scriptName);
            }
            catch (Exception ex) {
                if (ex instanceof ClassNotFoundException &&
                        ex.getMessage() != null &&
                        ex.getMessage().contains(scriptName)) {
                    throw new ScriptNotFoundException(scriptName);
                }
            }
        }
    }

    private String fixScriptName(String scriptName, List<Resource> allScripts) {
        try {
            Set<String> names = new HashSet<String>();
            for (Resource script : allScripts) {
                names.add(script.getFilename().substring(0, script.getFilename().length() - 7));
            }
            List<String> mostSimilar = CosineSimilarity.mostSimilar(scriptName, names);
            if (mostSimilar.isEmpty()) {
                return null;
            }
            List<String> topMatches = mostSimilar.subList(0, Math.min(5, mostSimilar.size()));
            return askUserForBestMatch(scriptName, topMatches);
        }
        catch (Exception e) {
            return null;
        }
    }

    private String askUserForBestMatch(String scriptName, List<String> topMatches) {
        @SuppressWarnings("hiding") GrailsConsole console = GrailsConsole.getInstance();
        console.addStatus("Script '" + scriptName + "' not found, did you mean:");
        int i = 0;
        for (String s : topMatches) {
            console.log("   " + ++i + ") " + s);
        }

        int attempts = 0;
        while (true) {
            String selection = console.userInput("Please make a selection or enter Q to quit: ");

            if ("Q".equalsIgnoreCase(selection)) {
                System.exit(0);
            }

            try {
                int number = Integer.parseInt(selection);
                if (number > 0 && number <= topMatches.size()) {
                    return topMatches.get(number - 1);
                }
            }
            catch (NumberFormatException ignored) {
                // ignored
            }

            attempts++;
            if (attempts > 4) {
                exitWithError("Selection not found.");
            }
        }
    }

    private GantResult executeWithGantInstance(Gant gant, final Closure<?> doNothingClosure) {
        GantResult result = new GantResult();
        result.script = gant.prepareTargets();
        gant.setAllPerTargetPostHooks(doNothingClosure);
        gant.setAllPerTargetPreHooks(doNothingClosure);
        // Invoke the default target.
        result.exitCode = gant.executeTargets();
        return result;
    }

    class GantResult {
        int exitCode;
        GroovyObject script;
    }

    private boolean isGrailsProject() {
        return new File(settings.getBaseDir(), "grails-app").exists();
    }

    private boolean isExternalScript(Resource scriptFile) {
        return scriptsAllowedOutsideOfProject.contains(scriptFile);
    }

    private String getScriptNameFromFile(Resource scriptPath) {
        String scriptFileName = scriptPath.getFilename().substring(0,scriptPath.getFilename().length()-7); // trim .groovy extension
        if (scriptFileName.endsWith("_")) {
            scriptFileName = scriptFileName.substring(0, scriptFileName.length()-1);
        }
        return scriptFileName;
    }

    /**
     * Returns a list of all the executable Gant scripts available to this application.
     */
    public List<Resource> getAvailableScripts() {
        List<Resource> scripts = new ArrayList<Resource>();
        if (settings.getGrailsHome() != null) {
            addCommandScripts(new File(settings.getGrailsHome(), "scripts"), scripts);
        }
        addCommandScripts(new File(settings.getBaseDir(), "scripts"), scripts);
        addCommandScripts(new File(settings.getUserHome(), ".grails/scripts"), scripts);

        for (File dir : pluginPathSupport.listKnownPluginDirs()) {
            addPluginScripts(dir, scripts);
        }

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(settings.getRootLoader());
        try {
            final Resource[] resources = resolver.getResources("classpath*:META-INF/scripts/*.groovy");
            scripts.addAll(Arrays.asList(resources));
        } catch (IOException e) {
            // ignore
        }
        return scripts;
    }

    /**
     * Collects all the command scripts provided by the plugin contained
     * in the given directory and adds them to the given list.
     */
    private static void addPluginScripts(File pluginDir, List<Resource> scripts) {
        if (!pluginDir.exists()) return;

        File scriptDir = new File(pluginDir, "scripts");
        if (scriptDir.exists()) addCommandScripts(scriptDir, scripts);
    }

    /**
     * Adds all the command scripts (i.e. those whose name does *not* start with an
     * underscore, '_') found in the given directory to the given list.
     */
    private static void addCommandScripts(File dir, List<Resource> scripts) {
        if (dir.exists()) {
            for (File file : dir.listFiles()) {
                if (scriptFilePattern.matcher(file.getName()).matches()) {
                    scripts.add(new FileSystemResource(file));
                }
            }
        }
    }


    /**
     * Sanitizes a stack trace using GrailsUtil.deepSanitize(). We use
     * this method so that the GrailsUtil class is loaded from the
     * context class loader. Basically, we don't want this class to
     * have a direct dependency on GrailsUtil otherwise the class loader
     * used to load this class (GrailsScriptRunner) would have to have
     * far more libraries on its classpath than we want.
     */
    private static void sanitizeStacktrace(Throwable t) {
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            Class<?> clazz = loader.loadClass("grails.util.GrailsUtil");
            Method method = clazz.getMethod("deepSanitize", Throwable.class);
            method.invoke(null, t);
        }
        catch (Throwable ex) {
            // cannot sanitize, ignore
        }
    }

    /**
     * Contains details about a Grails command invocation such as the
     * name of the corresponding script, the environment (if specified),
     * and the arguments to the command.
     */
    private static class ScriptAndArgs {
        public String inputName;
        public String name;
        public String env;
        public String args;
    }
}
