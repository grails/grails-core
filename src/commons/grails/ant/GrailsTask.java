package grails.ant;

import grails.util.GrailsNameUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.codehaus.groovy.grails.cli.support.GrailsBuildHelper;
import org.codehaus.groovy.grails.cli.support.GrailsRootLoader;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>Ant task for executing Grails scripts. To use it, first create a
 * task definition for it:
 * <pre>
 *   &lt;path id="grails.classpath"&gt;
 *        &lt;fileset dir="${grails.home}/dist" includes="grails-bootstrap-1.1.jar"/&gt;
 *        &lt;fileset dir="${grails.home}/lib" includes="groovy-all-1.6.jar"/&gt;
 *    &lt;/path&gt;
 *
 *    &lt;taskdef name="grails"
 *             classname="grails.ant.GrailsTask"
 *             classpathref="grails.classpath"/&gt;
 * </pre>
 * You must have both the "grails-bootstrap" and "groovy-all" JARs on
 * the <code>taskdef<code>'s classpath, otherwise the task won't load.
 * </p>
 * <p>Once the task is defined, you can use it like this:
 * <pre>
 *   &lt;grails home="${grails.home}" script="Clean"/&gt;
 * </pre>
 * The <code>home</code> attribute contains the location of a local
 * Grails installation, while <code>script</code> is the name of the
 * Grails script to run. Note that it's the <em>script</em> name not
 * the equivalent command name.
 * </p>
 * <p>If you want to use the Ant task without a Grails installation,
 * then you can use the <code>classpathref</code> attribute or
 * <code>classpath</code> nested element instead of <code>home</code>.
 * This allows you to control precisely which JARs and versions are
 * used to execute the Grails scripts. Typically you would use this
 * option in conjunction with something like Ivy.
 * </p>
 */
public class GrailsTask extends Task {
    private File home;
    private String script;
    private String args;
    private String environment;
    private boolean includeRuntimeClasspath = true;
    private Path classpath;
    private Path compileClasspath;
    private Path testClasspath;
    private Path runtimeClasspath;

    @Override
    public void execute() throws BuildException {
        // The "script" must be specified.
        if (script == null) throw new BuildException("'script' must be provided.");

        // Check that one, and only one, of Grails home and classpath
        // are set.
        if (home == null && classpath == null) {
            throw new BuildException("One of 'home' or 'classpath' must be provided.");
        }
        else if (home != null && classpath != null) {
            throw new BuildException("You cannot use both 'home' and 'classpath' with the Grails task.");
        }

        runGrails(script, args);
    }

    protected void runGrails(String targetName, String args) {
        // First get the dependencies from the classpath.
        List<URL> urls = new ArrayList<URL>();
        if (classpath != null) {
            urls.addAll(pathsToUrls(classpath));
        }
        else {
            urls.addAll(getRequiredLibsFromHome());
        }

        // Now add the runtime dependencies if necessary.
        if (includeRuntimeClasspath && runtimeClasspath != null) {
            urls.addAll(pathsToUrls(runtimeClasspath));
        }

        try {
            URL[] loaderUrls = urls.toArray(new URL[urls.size()]);
            GrailsRootLoader rootLoader = new GrailsRootLoader(loaderUrls, getClass().getClassLoader());

            GrailsBuildHelper helper;

            if(getProject().getBaseDir() != null) {
                helper = new GrailsBuildHelper(rootLoader, home == null ? null : home.getCanonicalPath(), getProject().getBaseDir().getCanonicalPath());
            }
            else {
                helper = new GrailsBuildHelper(rootLoader, home == null ? null : home.getCanonicalPath());
            }
            
            configureBuildSettings(helper);

            int retval;
            if (environment == null) {
                retval = helper.execute(targetName, args);
            }
            else {
                retval = helper.execute(targetName, args, environment);
            }

            if (retval != 0) {
                throw new BuildException("Grails returned non-zero value: " + retval);
            }
        }
        catch (Exception ex) {
            throw new BuildException("Unable to start Grails: " + ex.getMessage(), ex);
        }
    }

    private List<URL> getRequiredLibsFromHome() {
        List<URL> urls = new ArrayList<URL>();

        try {
            // Make sure Groovy and Gant are on the classpath if we are
            // using "grailsHome".
            File[] files = new File(home, "lib").listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.startsWith("gant_") || name.startsWith("groovy-all");
                }
            });

            for (File file : files) {
                urls.add(file.toURI().toURL());
            }

            // Also make sure the bootstrap JAR is on the classpath.
            files = new File(home, "dist").listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.startsWith("grails-bootstrap");
                }
            });

            for (File file : files) {
                urls.add(file.toURI().toURL());
            }

            return urls;
        }
        catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private void configureBuildSettings(GrailsBuildHelper helper)
            throws ClassNotFoundException, IllegalAccessException,
            InstantiationException, NoSuchMethodException, InvocationTargetException {
        helper.setCompileDependencies(pathsToFiles(compileClasspath));
        helper.setTestDependencies(pathsToFiles(testClasspath));
        helper.setRuntimeDependencies(pathsToFiles(runtimeClasspath));
    }

    private List<URL> pathsToUrls(Path path) {
        if (path == null) return Collections.emptyList();

        List<URL> urls = new ArrayList<URL>(path.size());
        for (String filePath : path.list()) {
            try { urls.add(new File(filePath).toURI().toURL()); }
            catch (MalformedURLException ex) { throw new RuntimeException(ex); }
        }

        return urls;
    }

    private List<File> pathsToFiles(Path path) {
        if (path == null) return Collections.emptyList();

        List<File> files = new ArrayList<File>(path.size());
        for (String filePath : path.list()) {
            files.add(new File(filePath));
        }

        return files;
    }

    public String getCommand() {
        return GrailsNameUtils.getScriptName(this.script);
    }

    public void setCommand(String command) {
        this.script = GrailsNameUtils.getNameFromScript(command);
    }

    public File getHome() {
        return home;
    }

    public void setHome(File home) {
        this.home = home;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public String getArgs() {
        return args;
    }

    public void setArgs(String args) {
        this.args = args;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public boolean isIncludeRuntimeClasspath() {
        return includeRuntimeClasspath;
    }

    public void setIncludeRuntimeClasspath(boolean includeRuntimeClasspath) {
        this.includeRuntimeClasspath = includeRuntimeClasspath;
    }

    public Path getClasspath() {
        return classpath;
    }

    public void addClasspath(Path classpath) {
        this.classpath = classpath;
    }

    public void setClasspathRef(Reference ref) {
        addClasspath((Path) ref.getReferencedObject());
    }

    public Path getCompileClasspath() {
        return compileClasspath;
    }

    public void addCompileClasspath(Path compileClasspath) {
        this.compileClasspath = compileClasspath;
    }

    public Path getTestClasspath() {
        return testClasspath;
    }

    public void addTestClasspath(Path testClasspath) {
        this.testClasspath = testClasspath;
    }

    public Path getRuntimeClasspath() {
        return runtimeClasspath;
    }

    public void addRuntimeClasspath(Path runtimeClasspath) {
        this.runtimeClasspath = runtimeClasspath;
    }
}
