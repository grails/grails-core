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

import org.codehaus.groovy.grails.compiler.support.GrailsResourceLoader
import org.codehaus.groovy.grails.compiler.support.GrailsResourceLoaderHolder
import org.codehaus.groovy.grails.plugins.GrailsPluginUtils

import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver

/**
 * An extended version of the Groovy compiler that sets up the Grails ResourceLoader upon compilation.
 *
 * @author Graeme Rocher
 * @deprecated Use {@link Grailsc} instead
 * 
 * @since 0.6
 */
class GrailsCompiler extends Groovyc {

    String projectName
    def resolver = new PathMatchingResourcePatternResolver()

    private destList = []

    @Override
    void scanDir(File srcDir, File destDir, String[] files) {
        def srcList = []
        def srcPath = srcDir.absolutePath
        def destPath = destDir.absolutePath
        for (f in files) {
            def sf = new File("${srcPath}/$f")
            def df = null
            if (f.endsWith(".groovy") ) {
                df = new File("${destPath}/${f[0..-7] + 'class'}")
                def i = f.lastIndexOf('/')
                if (!df.exists() && i > -1) {
                    // check root package
                    def tmp = new File("${destPath}/${f[i..-7] + 'class'}")
                    if (tmp.exists()) {
                        df = tmp
                    }
                }
            }
            else if (f.endsWith(".java")) {
                df = new File("${destPath}/${f[0..-5] + 'class'}")
            }
            else {
                continue
            }

            if (sf.lastModified() > df.lastModified()) {
                srcList << sf
                destList << df
            }
        }
        addToCompileList(srcList as File[])
    }

    void compile() {

        configureResourceLoader()

        if (compileList) {
            long now = System.currentTimeMillis()
            try {
                super.compile()
                getDestdir().setLastModified(now)
            }
            finally {
                // set the destination files as modified so recompile doesn't happen continuously
                for(f in destList) {
                    f.setLastModified(now)
                }
            }
        }
    }

    private configureResourceLoader() {
        def basedir = System.getProperty("base.dir") ?: "."
        Resource[] resources = GrailsPluginUtils.getArtefactResources(basedir)
        def resourceLoader = new GrailsResourceLoader(resources)
        GrailsResourceLoaderHolder.resourceLoader = resourceLoader
    }
}
