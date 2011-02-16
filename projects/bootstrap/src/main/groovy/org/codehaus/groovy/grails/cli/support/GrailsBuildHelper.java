/* Copyright 2004-2005 the original author or authors.
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
     * @param classLoader The class loader that will be used to load Grails.
     */
    public GrailsBuildHelper(ClassLoader classLoader) {
        this(classLoader, null);
    }

    /**
     * Creates a helper that loads the Grails build system with the given
     * class loader. Ideally, the class loader should be an instance of
     * {@link org.codehaus.groovy.grails.cli.support.GrailsRootLoader}.
     * You can try other class loaders, but you may run into problems.
     * @param classLoader The class loader that will be used to load Grails.
     * @param grailsHome Location of a local Grails installation.
     */
    public GrailsBuildHelper(ClassLoader classLoader, String grailsHome) {
        this(classLoader, grailsHome, null);
    }

    public GrailsBuildHelper(ClassLoader classLoader, String grailsHome, String baseDir) {
        try {
            this.classLoader = classLoader;
            Class<?> clazz = classLoader.loadClass("grails.util.BuildSettings");

            // Use the BuildSettings(File grailsHome, File baseDir) constructor.
            File grailsHomeFile = grailsHome == null ? null : new File(grailsHome);
            File baseDirFile = baseDir == null ? null : new File(baseDir);
            settings = clazz.getConstructor(File.class, File.class).newInstance(grailsHomeFile, baseDirFile);

            // Initialise the root loader for the BuildSettings.
            invokeMethod(settings, "setRootLoader",
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
        return (File) invokeMethod(settings, "getGrailsWorkDir", new Object[0]);
    }

    public void setGrailsWorkDir(File dir) {
        invokeMethod(settings, "setGrailsWorkDir", new Object[] { dir });
    }

    public File getProjectWorkDir() {
        return (File) invokeMethod(settings, "getProjectWorkDir", new Object[0]);
    }

    public void setProjectWorkDir(File dir) {
        invokeMethod(settings, "setProjectWorkDir", new Object[] { dir });
    }

    public File getClassesDir() {
        return (File) invokeMethod(settings, "getClassesDir", new Object[0]);
    }

    public void setClassesDir(File dir) {
        invokeMethod(settings, "setClassesDir", new Object[] { dir });
    }

    public File getTestClassesDir() {
        return (File) invokeMethod(settings, "getTestClassesDir", new Object[0]);
    }

    public void setTestClassesDir(File dir) {
        invokeMethod(settings, "setTestClassesDir", new Object[] { dir });
    }

    public File getResourcesDir() {
        return (File) invokeMethod(settings, "getResourcesDir", new Object[0]);
    }

    public void setResourcesDir(File dir) {
        invokeMethod(settings, "setResourcesDir", new Object[] { dir });
    }

    public File getProjectPluginsDir() {
        return (File) invokeMethod(settings, "getProjectPluginsDir", new Object[0]);
    }

    public void setProjectPluginsDir(File dir) {
        invokeMethod(settings, "setProjectPluginsDir", new Object[] { dir });
    }

    public File getGlobalPluginsDir() {
        return (File) invokeMethod(settings, "getGlobalPluginsDir", new Object[0]);
    }

    public void setGlobalPluginsDir(File dir) {
        invokeMethod(settings, "setGlobalPluginsDir", new Object[] { dir });
    }

    public File getTestReportsDir() {
        return (File) invokeMethod(settings, "getTestReportsDir", new Object[0]);
    }

    public void setTestReportsDir(File dir) {
        invokeMethod(settings, "setTestReportsDir", new Object[] { dir });
    }

    @SuppressWarnings("rawtypes")
    public List getCompileDependencies() {
        return (List) invokeMethod(settings, "getCompileDependencies", new Object[0]);
    }

    @SuppressWarnings("rawtypes")
    public void setCompileDependencies(List dependencies) {
        invokeMethod(settings, "setCompileDependencies", new Class[] { List.class }, new Object[] { dependencies });
    }

    public void setDependenciesExternallyConfigured(boolean b) {
        invokeMethod(settings, "setDependenciesExternallyConfigured", new Class[] { boolean.class }, new Object[] { b });
    }

    @SuppressWarnings("rawtypes")
    public List getTestDependencies() {
        return (List) invokeMethod(settings, "getTestDependencies", new Object[0]);
    }

    @SuppressWarnings("rawtypes")
    public void setTestDependencies(List dependencies) {
        invokeMethod(settings, "setTestDependencies", new Class[] { List.class }, new Object[] { dependencies });
    }

    @SuppressWarnings("rawtypes")
    public List getRuntimeDependencies() {
        return (List) invokeMethod(settings, "getRuntimeDependencies", new Object[0]);
    }

    @SuppressWarnings("rawtypes")
    public void setRuntimeDependencies(List dependencies) {
        invokeMethod(settings, "setRuntimeDependencies", new Class[] { List.class }, new Object[] { dependencies });
    }

    private Object createScriptRunner() throws Exception {
        return classLoader.loadClass("org.codehaus.groovy.grails.cli.GrailsScriptRunner").
            getDeclaredConstructor(new Class[] { settings.getClass() }).
            newInstance(new Object[] { settings });
    }

    /**
     * Invokes the named method on a target object using reflection.
     * The method signature is determined by the classes of each argument.
     * @param target The object to call the method on.
     * @param name The name of the method to call.
     * @param args The arguments to pass to the method (may be an empty array).
     * @return The value returned by the method.
     */
    private Object invokeMethod(Object target, String name, Object[] args) {
        Class<?>[] argTypes = new Class[args.length];
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
    private Object invokeMethod(Object target, String name, Class<?>[] argTypes, Object[] args) {
        try {
            return target.getClass().getMethod(name, argTypes).invoke(target, args);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
