/*
 * Copyright 2012 the original author or authors.
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

package org.codehaus.groovy.grails.cli.support

import org.apache.tools.ant.BuildLogger
import org.apache.tools.ant.Project
import spock.lang.Specification

/**
 * @author jbeutel@hawaii.edu
 */
class GrailsRootLoaderSpec extends Specification {

    def "NoClassDefFoundError is not obscured by ClassNotFoundException"() {
        // i.e., GrailsRootLoader does not throw ClassNotFoundException for a class that it actually did find.

        setup: "tmp dir for the isolated classpath"
        File tmpDir = createTempDir("GrailsRootLoaderSpec", ".tmp")

        and: "cut off class file A from its dependency B by copying only A to the tmp dir"
        copyClassFileToClasspath(A, tmpDir)

        and: "parent ClassLoader has neither A nor B"
        def parent = createClassLoaderExcluding([A.name, B.name])

        and: "GrailsRootLoader with just the tmp dir containing A"
        def grl = new GrailsRootLoader([tmpDir.toURI().toURL()] as URL[], parent)

        when: "loading A"
        grl.loadClass(A.name)

        then: "throws NoClassDefFoundError for B (not the parent's ClassNotFoundException for A)"
        NoClassDefFoundError e = thrown()
        e.message == B.name.replace('.', '/')

        cleanup:
        tmpDir.deleteDir()
    }

    static class A implements B {}

    static interface B {}

    private static File createTempDir(String prefix, String suffix) {
        File tmpDir = File.createTempFile(prefix, suffix)
        assert tmpDir.delete()
        assert tmpDir.mkdir()
        tmpDir
    }

    private static void copyClassFileToClasspath(Class c, File baseDir) {
        String classFilePath = c.name.replace('.', '/').concat('.class')
        def source = new File(c.classLoader.getResource(classFilePath).path)
        def dest = new File(baseDir, classFilePath)

        def quiet = new AntBuilder()
        quiet.antProject.buildListeners.find {it instanceof BuildLogger}?.messageOutputLevel = Project.MSG_WARN
        quiet.copy(file: source, tofile: dest)  // also creates intervening package dirs
    }


    private static ClassLoader createClassLoaderExcluding(classNames) {
        new ClassLoader() {
            @Override
            protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name in classNames) {
                    throw new ClassNotFoundException(name)
                } else {
                    super.loadClass(name, resolve)
                }
            }
        }
    }
}
