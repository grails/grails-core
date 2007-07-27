/**
 * Class description here.
 
 * @author Graeme Rocher
 * @since 0.4
  *
 * Created: Jul 27, 2007
 * Time: 3:53:37 PM
 * 
 */

package org.codehaus.groovy.grails.compiler

import org.codehaus.groovy.ant.Groovyc
import org.codehaus.groovy.control.CompilerConfiguration
import org.springframework.core.io.Resource
import org.codehaus.groovy.grails.compiler.support.*
import org.codehaus.groovy.grails.compiler.injection.*
import org.apache.tools.ant.BuildException
import org.codehaus.groovy.tools.javac.JavaAwareCompilationUnit
import org.codehaus.groovy.tools.ErrorReporter
import org.apache.tools.ant.AntClassLoader
import org.codehaus.groovy.control.*

class GrailsCompiler extends Groovyc {

    GrailsCompiler() {
        jointCompilationOptions = "-j"
    }

	def resolver = new org.springframework.core.io.support.PathMatchingResourcePatternResolver()

	String resourcePattern

    void compile() {
        println "Compiling ${compileList.length} source file${compileList ? 's' : ''} to ${destdir}"
        CompilerConfiguration configuration = new CompilerConfiguration()
        if(classpath) configuration.classpath = classpath.toString()

        def compilerOptions = [stubDir: createTempDir()]
        configuration.targetDirectory = destdir
        configuration.jointCompilationOptions = compilerOptions

        if(encoding)configuration.sourceEncoding = encoding

		def resources = resourcePattern ? resolveResources(resourcePattern) : [] as Resource[]
		def resourceLoader = new GrailsResourceLoader(resources)
        GrailsResourceLoaderHolder.resourceLoader = resourceLoader

		def classInjectors = [new DefaultGrailsDomainClassInjector()] as ClassInjector[]

        def unit = new JavaAwareCompilationUnit(configuration, buildClassLoaderFor(configuration, resourceLoader, classInjectors));
		def injectionOperation = new GrailsAwareInjectionOperation(resourceLoader, classInjectors)
        unit.addPhaseOperation(injectionOperation, Phases.CONVERSION)
        unit.addSources(compileList)


        try {
            unit.compile()
        }
        catch(Exception e) {
            e.printStackTrace()
            def ErrorReporter reporter = new org.codehaus.groovy.tools.ErrorReporter(e, false)
            def sw = new StringWriter()
            def writer = new PrintWriter(sw)
            throw new BuildException(sw.toString(), e, getLocation())
        }
    }

    private File createTempDir()  {
        File tempFile;
        try {
            tempFile = File.createTempFile("generated-", "java-source");
            tempFile.delete();
            tempFile.mkdirs();
        } catch (IOException e) {
            throw new BuildException(e);
        }
        return tempFile;
    }


    private GroovyClassLoader buildClassLoaderFor(CompilerConfiguration configuration, def resourceLoader, ClassInjector[] classInjectors) {
        ClassLoader parent = this.getClass().getClassLoader()
        if (parent instanceof AntClassLoader) {
            AntClassLoader antLoader = parent;
            String[] pathElm = antLoader.getClasspath().split(File.pathSeparator);
            List classpath = configuration.getClasspath();
            /*
             * Iterate over the classpath provided to groovyc, and add any missing path
             * entries to the AntClassLoader.  This is a workaround, since for some reason
             * 'directory' classpath entries were not added to the AntClassLoader' classpath.
             */
            for (cpEntry in classpath) {
                boolean found = false;
                for (pathEntry in pathElm) {
                    if (cpEntry.equals(pathEntry)) {
                        found = true;
                        break;
                    }
                }
                if (!found)
                    antLoader.addPathElement(cpEntry);
            }
        }



        def classLoader = new GrailsAwareClassLoader(	parent,
												 		configuration	)

		classLoader.classInjectors  =  classInjectors
		classLoader.resourceLoader = resourceLoader

		return classLoader
    }
	def resolveResources(String pattern) {
		try {
			return resolver.getResources(pattern)
		}
		catch(Throwable e) {
	         return []
		}
	}

}

