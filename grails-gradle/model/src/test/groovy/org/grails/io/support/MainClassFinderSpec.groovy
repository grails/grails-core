/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.grails.io.support

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes

import spock.lang.Specification
import spock.lang.TempDir

class MainClassFinderSpec extends Specification {

    private static final int PUBLIC_STATIC = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC
    private static final int PRIVATE_STATIC = Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC

    @TempDir
    File tempDir

    private final Set<String> cacheKeys = new HashSet<>()

    void cleanup() {
        cacheKeys.each { MainClassFinder.mainClasses.remove(it) }
    }

    void 'findMainClass finds a nested Grails application and its class file'() {
        given:
        File application = writeClass(tempDir, 'example/nested/Application')
        new File(tempDir, 'notes.txt').text = 'not bytecode'
        writeClass(tempDir, 'example/PlainMain', 'java/lang/Object')

        when:
        def holder = MainClassFinder.findMainClass(tempDir, false)

        then:
        holder.className == 'example.nested.Application'
        holder.classFile == application
    }

    void 'findMainClass rejects a candidate with superclass #superName access #access and signature #descriptor'() {
        given:
        writeClass(tempDir, 'example/Application', superName, access, descriptor)

        expect:
        MainClassFinder.findMainClass(tempDir, false) == null

        where:
        superName                                  | access             | descriptor
        'java/lang/Object'                          | PUBLIC_STATIC      | '([Ljava/lang/String;)V'
        'grails/boot/config/GrailsAutoConfiguration' | PRIVATE_STATIC     | '([Ljava/lang/String;)V'
        'grails/boot/config/GrailsAutoConfiguration' | Opcodes.ACC_PUBLIC | '([Ljava/lang/String;)V'
        'grails/boot/config/GrailsAutoConfiguration' | PUBLIC_STATIC      | '()V'
    }

    void 'findMainClass ignores hidden directories and classes without a main method'() {
        given:
        writeClass(tempDir, '.hidden/Application')
        writeClass(tempDir, 'example/NoMain', 'grails/boot/config/GrailsAutoConfiguration', 0, null)

        expect:
        MainClassFinder.findMainClass(tempDir, false) == null
    }

    void 'findMainClass returns null for missing and empty directories'() {
        expect:
        MainClassFinder.findMainClass(new File(tempDir, 'missing'), false) == null
        MainClassFinder.findMainClass(tempDir, false) == null
    }

    void 'findMainClass rejects a file as the search root'() {
        given:
        File file = new File(tempDir, 'file.txt')
        file.text = 'content'

        when:
        MainClassFinder.findMainClass(file, false)

        then:
        def error = thrown(IllegalArgumentException)
        error.message.contains('Invalid root folder')
    }

    void 'findMainClass can bypass its cache and rescan after a cached class is deleted'() {
        given:
        File application = writeClass(tempDir, 'example/Application')
        cacheKeys.add(tempDir.canonicalPath)
        def original = MainClassFinder.findMainClass(tempDir)

        when:
        writeClass(tempDir, 'example/Application', 'java/lang/Object')

        then:
        MainClassFinder.findMainClass(tempDir).is(original)
        MainClassFinder.findMainClass(tempDir, false) == null

        when:
        assert application.delete()
        File replacement = writeClass(tempDir, 'example/Replacement')
        def result = MainClassFinder.findMainClass(tempDir)

        then:
        result.className == 'example.Replacement'
        result.classFile == replacement
    }

    void 'searchMainClass discovers #classesPath using the #marker project marker'() {
        given:
        if (marker == 'grails-app') {
            assert new File(tempDir, marker).mkdirs()
        } else {
            new File(tempDir, marker).text = ''
        }
        File source = new File(tempDir, 'src/main/groovy/example/Application.groovy')
        assert source.parentFile.mkdirs()
        source.text = 'package example'
        File classes = new File(tempDir, classesPath)
        writeClass(classes, 'example/Application')

        expect:
        MainClassFinder.searchMainClass(source.toURI(), false) == 'example.Application'

        where:
        classesPath                 | marker
        'build/classes/main'        | 'build.gradle'
        'build/classes/groovy/main' | 'grails-app'
    }

    void 'searchMainClass caches by URI and invalidates deleted class files'() {
        given:
        new File(tempDir, 'build.gradle').text = ''
        File source = new File(tempDir, 'Application.groovy')
        source.text = 'package example'
        File classes = new File(tempDir, 'build/classes/main')
        File application = writeClass(classes, 'example/Application')
        cacheKeys.add(source.toURI().toString())
        cacheKeys.add(classes.canonicalPath)

        expect:
        MainClassFinder.searchMainClass(source.toURI()) == 'example.Application'
        MainClassFinder.searchMainClass(source.toURI()) == 'example.Application'

        when:
        writeClass(classes, 'example/Application', 'java/lang/Object')

        then:
        MainClassFinder.searchMainClass(source.toURI(), false) == null
        MainClassFinder.searchMainClass(source.toURI()) == 'example.Application'

        when:
        assert application.delete()
        writeClass(classes, 'example/Replacement')

        then:
        MainClassFinder.searchMainClass(source.toURI()) == 'example.Replacement'
    }

    void 'searchMainClass returns null for absent projects and unsupported URIs'() {
        expect:
        MainClassFinder.searchMainClass(null, false) == null
        MainClassFinder.searchMainClass(new URI('https://example.invalid/Application.groovy'), false) == null
        MainClassFinder.searchMainClass(new File(tempDir, 'missing.groovy').toURI(), false) == null
    }

    private static File writeClass(File root, String name,
                                   String superName = 'grails/boot/config/GrailsAutoConfiguration',
                                   int access = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                                   String descriptor = '([Ljava/lang/String;)V') {
        ClassWriter writer = new ClassWriter(0)
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, name, null, superName, null)
        if (descriptor != null) {
            def method = writer.visitMethod(access, 'main', descriptor, null, null)
            method.visitCode()
            method.visitInsn(Opcodes.RETURN)
            method.visitMaxs(0, 2)
            method.visitEnd()
        }
        writer.visitEnd()
        File file = new File(root, name + '.class')
        assert file.parentFile.mkdirs() || file.parentFile.isDirectory()
        file.bytes = writer.toByteArray()
        file
    }
}
