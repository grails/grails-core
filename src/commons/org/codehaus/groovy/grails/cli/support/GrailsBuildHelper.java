package org.codehaus.groovy.grails.cli.support;

import java.io.File;
import java.net.URLClassLoader;
import java.util.List;

/**
 * Helper class that allows a client to bootstrap the Grails build system
 * in its own class loader. It basically uses reflection to handle the
 * entry points to the build system: {@link grails.util.BuildSettings}
 * and {@link org.codehaus.groovy.grails.cli.GrailsScriptRunner}. This
 * ensures class loader isolation for Grails.
 *
 * @author Peter Ledbrook
 */
public class GrailsBuildHelper {
    private ClassLoader classLoader;
    private Object settings;

    /**
     * Creates a helper that loads the Grails build system with the given
     * class loader. Ideally, the class loader should be an instance of
     * {@link org.codehaus.groovy.grails.cli.support.GrailsRootLoader}.
     * You can try other class loaders, but you may run into problems.
     * @param classLoader The class loader that will be used to load
     * Grails.
     */
    public GrailsBuildHelper(ClassLoader classLoader) {
        this(classLoader, null);
    }

    /**
     * Creates a helper that loads the Grails build system with the given
     * class loader. Ideally, the class loader should be an instance of
     * {@link org.codehaus.groovy.grails.cli.support.GrailsRootLoader}.
     * You can try other class loaders, but you may run into problems.
     * @param classLoader The class loader that will be used to load
     * Grails.
     * @param grailsHome Location of a local Grails installation.
     */
    public GrailsBuildHelper(ClassLoader classLoader, String grailsHome) {
        this(classLoader, grailsHome, null);
    }

    public GrailsBuildHelper(ClassLoader classLoader, String grailsHome, String baseDir) {
        try {
            this.classLoader = classLoader;
            Class clazz = classLoader.loadClass("grails.util.BuildSettings");

            // Use the BuildSettings(File grailsHome, File baseDir) constructor.
            File grailsHomeFile = grailsHome == null ? null : new File(grailsHome);
            File baseDirFile = baseDir == null ? null : new File(baseDir);
            this.settings = clazz.getConstructor(File.class, File.class).newInstance(grailsHomeFile, baseDirFile);

            // Initialise the root loader for the BuildSettings.
            invokeMethod(
                    this.settings,
                    "setRootLoader",
                    new Class[] { URLClassLoader.class },
                    new Object[] { classLoader });
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Executes the named Grails script with no arguments.
     * @param script The name of the script to execute, such as "Compile".
     * @return The value returned by the build system (notionally the
     * exit code).
     */
    public int execute(String script) {
        return execute(script, null);
    }

    /**
     * Executes the named Grails script with the given arguments.
     * @param script The name of the script to execute, such as "Compile".
     * @param args A single string containing the arguments for the
     * script, each argument separated by whitespace.
     * @return The value returned by the build system (notionally the
     * exit code).
     */
    public int execute(String script, String args) {
        try {
            Object scriptRunner = createScriptRunner();
            Object retval = scriptRunner.getClass().
                    getMethod("executeCommand", new Class[] { String.class, String.class }).
                    invoke(scriptRunner, new Object[] { script, args });
            return ((Integer) retval).intValue();
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Executes the named Grails script with the given arguments in the
     * specified environment. Normally the script is run in the default
     * environment for that script.
     * @param script The name of the script to execute, such as "Compile".
     * @param args A single string containing the arguments for the
     * script, each argument separated by whitespace.
     * @param env The name of the environment to run in, e.g. "development"
     * or "production".
     * @return The value returned by the build system (notionally the
     * exit code).
     */
    public int execute(String script, String args, String env) {
        try {
            Object scriptRunner = createScriptRunner();
            Object retval = scriptRunner.getClass().
                    getMethod("executeCommand", new Class[] { String.class, String.class, String.class }).
                    invoke(scriptRunner, new Object[] { script, args, env });
            return ((Integer) retval).intValue();
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public File getGrailsWorkDir() {
        return (File) invokeMethod(this.settings, "getGrailsWorkDir", new Object[0]);
    }

    public void setGrailsWorkDir(File dir) {
        invokeMethod(this.settings, "setGrailsWorkDir", new Object[] { dir });
    }

    public File getProjectWorkDir() {
        return (File) invokeMethod(this.settings, "getProjectWorkDir", new Object[0]);
    }

    public void setProjectWorkDir(File dir) {
        invokeMethod(this.settings, "setProjectWorkDir", new Object[] { dir });
    }

    public File getClassesDir() {
        return (File) invokeMethod(this.settings, "getClassesDir", new Object[0]);
    }

    public void setClassesDir(File dir) {
        invokeMethod(this.settings, "setClassesDir", new Object[] { dir });
    }

    public File getTestClassesDir() {
        return (File) invokeMethod(this.settings, "getTestClassesDir", new Object[0]);
    }

    public void setTestClassesDir(File dir) {
        invokeMethod(this.settings, "setTestClassesDir", new Object[] { dir });
    }

    public File getResourcesDir() {
        return (File) invokeMethod(this.settings, "getResourcesDir", new Object[0]);
    }

    public void setResourcesDir(File dir) {
        invokeMethod(this.settings, "setResourcesDir", new Object[] { dir });
    }

    public File getProjectPluginsDir() {
        return (File) invokeMethod(this.settings, "getProjectPluginsDir", new Object[0]);
    }

    public void setProjectPluginsDir(File dir) {
        invokeMethod(this.settings, "setProjectPluginsDir", new Object[] { dir });
    }

    public File getGlobalPluginsDir() {
        return (File) invokeMethod(this.settings, "getGlobalPluginsDir", new Object[0]);
    }

    public void setGlobalPluginsDir(File dir) {
        invokeMethod(this.settings, "setGlobalPluginsDir", new Object[] { dir });
    }

    public File getTestReportsDir() {
        return (File) invokeMethod(this.settings, "getTestReportsDir", new Object[0]);
    }

    public void setTestReportsDir(File dir) {
        invokeMethod(this.settings, "setTestReportsDir", new Object[] { dir });
    }

    public List getCompileDependencies() {
        return (List) invokeMethod(this.settings, "getCompileDependencies", new Object[0]);
    }

    public void setCompileDependencies(List dependencies) {
        invokeMethod(this.settings, "setCompileDependencies", new Class[] { List.class }, new Object[] { dependencies });
    }

    public List getTestDependencies() {
        return (List) invokeMethod(this.settings, "getTestDependencies", new Object[0]);
    }

    public void setTestDependencies(List dependencies) {
        invokeMethod(this.settings, "setTestDependencies", new Class[] { List.class }, new Object[] { dependencies });
    }

    public List getRuntimeDependencies() {
        return (List) invokeMethod(this.settings, "getRuntimeDependencies", new Object[0]);
    }

    public void setRuntimeDependencies(List dependencies) {
        invokeMethod(this.settings, "setRuntimeDependencies", new Class[] { List.class }, new Object[] { dependencies });
    }

    private Object createScriptRunner() throws Exception {
        return this.classLoader.loadClass("org.codehaus.groovy.grails.cli.GrailsScriptRunner").
                getDeclaredConstructor(new Class[] { this.settings.getClass() }).
                newInstance(new Object[] { this.settings });
    }

    /**
     * Invokes the named method on a target object using reflection.
     * The method signature is determined by the classes of each argument.
     * @param target The object to call the method on.
     * @param name The name of the method to call.
     * @param args The arguments to pass to the method (may be an empty
     * array).
     * @return The value returned by the method.
     */
    private Object invokeMethod(Object target, String name, Object[] args) {
        Class[] argTypes = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            argTypes[i] = args[i].getClass();
        }

        return invokeMethod(target, name, argTypes, args);
    }

    /**
     * Invokes the named method on a target object using reflection.
     * The method signature is determined by given array of classes.
     * @param target The object to call the method on.
     * @param name The name of the method to call.
     * @param argTypes The argument types declared by the method we
     * want to invoke (may be an empty array for a method that takes
     * no arguments).
     * @param args The arguments to pass to the method (may be an empty
     * array).
     * @return The value returned by the method.
     */
    private Object invokeMethod(Object target, String name, Class[] argTypes, Object[] args) {
        try {
            return target.getClass().getMethod(name, argTypes).invoke(target, args);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
