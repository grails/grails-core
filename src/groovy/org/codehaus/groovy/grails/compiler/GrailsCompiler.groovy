/*
 * Copyright 2003-2007 Graeme Rocher.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

/**
 * <p>An extended version of the Groovy compiler that performs AST injection into Grails domain classes

 * @author Graeme Rocher
 * @since 0.6
  *
 * Created: Jul 27, 2007
 * Time: 3:53:37 PM
 *
 */
class GrailsCompiler extends Groovyc {

    GrailsCompiler() {
        jointCompilationOptions = "-j"
    }


	def resolver = new org.springframework.core.io.support.PathMatchingResourcePatternResolver()
	def compileList = []

	String resourcePattern
	String projectName

	void resetFileLists() { compileList.clear() }

	File[] getFileList() { compileList as File[] }

	void scanDir(File srcDir, File destDir, String[] files) {
        def srcPath = srcDir.absolutePath
        def destPath = destDir.absolutePath
        for(f in files) {
            def sf = new File("${srcPath}/$f")
            def df = null
            if(f.endsWith(".groovy") ) {
                df = new File("${destPath}/${f-'.groovy' + '.class'}")
                def i = f.lastIndexOf('/')
                if(!df.exists() && i > -1) {
                    // check root package
                    def tmp = new File("${destPath}/${f[i..-1] - '.groovy' + '.class'}")
                    if(tmp.exists()) df = tmp
                }
            }
            else if(f.endsWith(".java")) {
                df = new File("${destPath}/${f-'.java' + '.class'}")
            }
            else {
                continue
            }

            if(sf.lastModified() > df.lastModified()) {
                compileList << sf
            }            
        }
    }

    File createTempDir()  {
        File tempFile;
        try {
            def userHome = System.getProperty("user.home")
            tempFile = new File("${userHome}/.grails/${grails.util.GrailsUtil.getGrailsVersion()}/projects/${projectName}/generated-java-source")
            if(tempFile.exists()) {                
                tempFile.delete();
            }
            tempFile.mkdirs();
        } catch (IOException e) {
            throw new BuildException(e);
        }
        return tempFile;
    }

    void compile() {
        setJointCompilationOptions("-j -Jclasspath=\"${getClasspath()}\"")

        def resources = resourcePattern ? resolveResources(resourcePattern) : [] as Resource[]
		def resourceLoader = new GrailsResourceLoader(resources)
        GrailsResourceLoaderHolder.resourceLoader = resourceLoader
	
		if(compileList) {
	        println "Compiling ${compileList.size()} source file${compileList ? 's' : ''} to ${destdir}"
	        
	        if(classpath) configuration.classpath = classpath.toString()
            configuration.targetDirectory = destdir
	        if(encoding)configuration.sourceEncoding = encoding


			def classInjectors = [new DefaultGrailsDomainClassInjector()] as ClassInjector[]

	        def unit = new JavaAwareCompilationUnit(configuration, buildClassLoaderFor(configuration, resourceLoader, classInjectors));
			def injectionOperation = new GrailsAwareInjectionOperation(resourceLoader, classInjectors)
	        unit.addPhaseOperation(injectionOperation, Phases.CONVERSION)
	        unit.addSources(compileList as File[])


	        try {
	            unit.compile()
	            getDestdir().setLastModified(System.currentTimeMillis())
	        }
	        catch(Exception e) {
	            def ErrorReporter reporter = new org.codehaus.groovy.tools.ErrorReporter(e, false)
	            def sw = new StringWriter()
				reporter.write( new PrintWriter(sw) );
	            throw new BuildException(sw.toString(), e, getLocation())
	        }
			
		}
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

