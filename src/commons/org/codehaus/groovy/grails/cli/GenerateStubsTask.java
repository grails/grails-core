/* Copyright 2004-2005 Graeme Rocher
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

import org.codehaus.groovy.ant.CompileTaskSupport;
import org.codehaus.groovy.tools.javac.JavaStubCompilationUnit;
import org.codehaus.groovy.control.Phases;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import groovy.lang.GroovyClassLoader;

import java.io.File;

/**
 *
 * Need to spin our own GenerateStubsTask because Groovy's one stupidly tries to compile properties files and anything
 * that doesn't end with Java
 *
 * @author Graeme Rocher
 * @since 1.1
 *        <p/>
 *        Created: Nov 26, 2008
 */
public class GenerateStubsTask extends CompileTaskSupport {
    protected void compile() throws Exception {
       GroovyClassLoader gcl = createClassLoader();
        JavaStubCompilationUnit compilation = new JavaStubCompilationUnit(config, gcl, destdir);

        int count = 0;

        String[] list = src.list();
        for (int i = 0; i < list.length; i++) {
            File basedir = getProject().resolveFile(list[i]);
            if (!basedir.exists()) {
                throw new BuildException("Source directory does not exist: " + basedir, getLocation());
            }

            DirectoryScanner scanner = getDirectoryScanner(basedir);
            String[] includes = scanner.getIncludedFiles();

            log.debug("Including files from: " + basedir);

            for (int j=0; j < includes.length; j++) {
                final String currentInclude = includes[j];
                log.debug("    "  + currentInclude);

                File file = new File(basedir, currentInclude);
                if(isSource(currentInclude))
                    compilation.addSourceFile(file);

                // Increment the count for each non/java src we found
                if (currentInclude.endsWith(".groovy")) {
                    count++;
                }
            }
        }

        if (count > 0) {
            log.info("Generating " + count + " Java stub" + (count > 1 ? "s" : "") + " to " + destdir);

            // Generate the stubs
            compilation.compile(Phases.CONVERSION);
        }
        else {
            log.info("No sources found for stub generation");
        }
    }

    private boolean isSource(String currentInclude) {
        return currentInclude.endsWith(".groovy") || currentInclude.endsWith(".java");
    }
}
