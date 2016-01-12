/*
 * Copyright 2013 the original author or authors.
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

package grails.test.mixin

import org.apache.tools.ant.BuildException
import org.apache.tools.ant.Project
import org.apache.tools.ant.types.Path
import org.codehaus.groovy.grails.test.compiler.GrailsIntegrationTestCompiler
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll

import java.lang.reflect.Field

/**
 * Tests for {@link org.codehaus.groovy.grails.compiler.injection.test.IntegrationTestMixinTransformation}
 * to verify that it only transforms the right classes and ignores the rest.
 *
 * @author patrick.jungermann
 * @since 12/01/16
 * @see <a href="https://github.com/grails/grails-core/issues/9552">https://github.com/grails/grails-core/issues/9552</a>
 * @see <a href="https://github.com/grails/grails-core/issues/9553">https://github.com/grails/grails-core/issues/9553</a>
 */
@Unroll
class IntegrationTestMixinTransformationSpec extends Specification {

    @SuppressWarnings("GroovyPointlessBoolean")
    void "IntegrationTestMixin should get applied to #type? #toBeApplied (class: #clazz)"(String type, Class clazz, boolean toBeApplied) {
        expect:
        clazz != null // class loading failed otherwise
        Field field = getDeclaredField(clazz, '$integrationTestMixin')
        toBeApplied == (field != null)

        where:
        type                 | clazz                                  || toBeApplied
        "class"              | getJunit4TestClass()                   || true
        "interface"          | getInterfaceClass()                    || false
        "trait"              | getTraitClass()                        || false
        "annotation"         | getAnnotationClass()                   || false
        "inner class"        | getInnerClassOfJunit4TestClass()       || false
        "static inner class" | getStaticInnerClassOfJunit4TestClass() || false
        "anonymous class"    | getAnonymousClassOfJunit4TestClass()   || false
    }

    Field getDeclaredField(Class clazz, String name) {
        try {
            return clazz.getDeclaredField(name)

        } catch (NoSuchFieldException ignore) {
            return null
        }
    }

    Class getJunit4TestClass() {
        return compileClass('MyJunit4Test', '''
class MyJunit4Test {

    @org.junit.Test
    void testSomething() {
        grailsApplication != null
        applicationContext != null
        callMe()
    }
}
''')
    }

    Class getInterfaceClass() {
        return compileClass('MyInterface', '''
interface MyInterface {

    void something()
}
''')
    }

    Class getTraitClass() {
        return compileClass('MyTrait', '''
trait MyTrait {

    void something() {
        println "something"
    }
}
''')
    }

    Class getAnnotationClass() {
        return compileClass('MyAnnotation', '''
public @interface MyAnnotation {
}
''')
    }

    Class getInnerClassOfJunit4TestClass() {
        return compileClass('MyJunit4Test', '''
class MyJunit4Test {

    @org.junit.Test
    void testSomething() {
        new InnerClass()
    }

    class InnerClass {}
}
''', 'InnerClass')
    }

    Class getStaticInnerClassOfJunit4TestClass() {
        return compileClass('MyJunit4Test', '''
class MyJunit4Test {

    @org.junit.Test
    void testSomething() {
        new StaticInnerClass()
    }

    static class StaticInnerClass {}
}
''', 'StaticInnerClass')
    }

    Class getAnonymousClassOfJunit4TestClass() {
        return compileClass('MyJunit4Test', '''
class MyJunit4Test {

    Runnable anonymousClassInstance = new Runnable() {

        @Override
        void run() {
        }
    }

    @org.junit.Test
    void testSomething() {
        grailsApplication != null
        applicationContext != null
        callMe()
    }
}
''', '1')
    }

    Class compileClass(String name, String content, String innerClassToLoad = null) {
        // @Rule creation happens after the data table creation
        TemporaryFolder temp = new TemporaryFolder()
        temp.create()
        try {
            File srcDir = temp.newFolder()
            File destDir = temp.newFolder()
            File source = new File(srcDir, name + ".groovy")
            source.createNewFile()
            source.write(content, "UTF-8")

            GrailsIntegrationTestCompiler compiler = new GrailsIntegrationTestCompiler()
            compiler.setProject(new Project())
            Path srcDirPath = new Path(compiler.getProject())
            srcDirPath.setLocation(srcDir)
            compiler.setSrcdir(srcDirPath)
            compiler.setDestdir(destDir)

            compiler.execute()

            GroovyClassLoader cl = new GroovyClassLoader()
            cl.addClasspath(destDir.canonicalPath)

            Class clazz = cl.loadClass(name)
            if (innerClassToLoad) {
                clazz = cl.loadClass(name + '$' + innerClassToLoad)
            }
            return clazz

        } catch (BuildException ignore) {
            // usually caused by org.codehaus.groovy.control.MultipleCompilationErrorsException
            return null

        } finally {
            temp.delete()
        }
    }
}
