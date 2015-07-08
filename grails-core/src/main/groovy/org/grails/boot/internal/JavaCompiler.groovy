/*
 * Copyright 2015 original authors
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
package org.grails.boot.internal

import groovy.transform.CompileStatic
import org.codehaus.groovy.control.CompilerConfiguration
import org.springframework.util.ClassUtils

import javax.tools.ToolProvider
import java.nio.charset.Charset


/**
 * Helper for recompiling Java code at runtime
 *
 * @author Graeme Rocher
 * @since 3.0.3
 */
@CompileStatic
class JavaCompiler {

    static boolean isAvailable() {
        ClassUtils.isPresent("javax.tools.JavaCompiler", JavaCompiler.classLoader)
    }

    static boolean recompile(CompilerConfiguration config, File... files) {
        // compile java source
        javax.tools.JavaCompiler compiler = ToolProvider.getSystemJavaCompiler()
        def sfm = compiler.getStandardFileManager(null, null, Charset.forName('UTF-8'))
        def compileTask = compiler.getTask(null, null, null, ['-d', config.targetDirectory.absolutePath], null, sfm.getJavaFileObjects(files))
        compileTask.call()
    }
}
