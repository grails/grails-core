package org.codehaus.groovy.grails.test

import junit.framework.TestSuite
import junit.framework.Test
import org.springframework.core.io.Resource
import org.codehaus.groovy.grails.test.GrailsTestHelper
import org.codehaus.groovy.grails.test.PotentialTest
import grails.util.BuildSettings
import java.lang.reflect.Modifier
import junit.framework.TestCase

/**
 * Standard implementation of the Grails test helper, which uses standard
 * JUnit classes to load the tests
 */
class DefaultGrailsTestHelper implements GrailsTestHelper {

    String testSuffix = "Tests"

    protected final File baseDir
    protected final File testClassesDir
    protected final ClassLoader parentLoader
    protected final Closure resourceResolver

    private ClassLoader currentClassLoader

    DefaultGrailsTestHelper(
            BuildSettings settings,
            ClassLoader classLoader,
            Closure resourceResolver) {
        this.baseDir = settings.baseDir
        this.testClassesDir = settings.testClassesDir
        this.parentLoader = classLoader
        this.resourceResolver = resourceResolver
    }

    TestSuite createTests(List<String> testNames, String type) {
        def testSrcDir = "${baseDir.absolutePath}/test/$type"
        def testSuite = new TestSuite("Grails Test Suite")

        currentClassLoader = createClassLoader(type)

        def potentialTests = testNames.collect { String name -> new PotentialTest(name) }
        potentialTests.findAll { it.hasMethodName() }.each { PotentialTest test ->
            def resources = resourceResolver("file:${testSrcDir}/${test.filePattern}${testSuffix}.groovy") as List
            resources += resourceResolver("file:${testSrcDir}/${test.filePattern}${testSuffix}.java") as List

            if (resources) {
                def className = fileToClassName(resources[0].file, new File(testSrcDir))
                def clazz = currentClassLoader.loadClass(className)

                if (TestCase.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.modifiers)) {
                    def suite = createTestSuite()
                    suite.name = className
                    suite.addTest(TestSuite.createTest(clazz, test.methodName))
                    testSuite.addTest(suite)
                }
            }
        }

        def nonMethodTests = potentialTests.findAll { !it.hasMethodName() }
        def nmTestResources = nonMethodTests*.filePattern.inject([]) { resources, String filePattern ->
            resources += resourceResolver("file:${testSrcDir}/${filePattern}${testSuffix}.groovy") as List
            resources += resourceResolver("file:${testSrcDir}/${filePattern}${testSuffix}.java") as List
        }
        nmTestResources.findAll { it.exists() }.each { Resource resource ->
            def className = fileToClassName(resource.file, new File(testSrcDir))
            def clazz = currentClassLoader.loadClass(className)

            if (TestCase.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.modifiers)) {
                testSuite.addTest(createTestSuite(clazz))
            }
        }

        return testSuite
    }

    /**
     * Creates a new test suite from the given test. The given class
     * typically extends <code>junit.framework.TestCase</code>. This
     * standard implementation returns a new <code>junit.framework.TestSuite</code>.
     */
    TestSuite createTestSuite(Class clazz) {
        new TestSuite(clazz)
    }

    /**
     * Creates a new test suite. This standard implementation just
     * returns a new <code>junit.framework.TestSuite</code>.
     */
    TestSuite createTestSuite() {
        new TestSuite()
    }

    /**
     * Given the location of a test file, and the directory that it is
     * relative to, this method returns the fully qualifed class name
     * of that tests. So for example, if you have "test/unit/org/example/MyTest.groovy"
     * with a base directory of "test/unit", the method returns the
     * string "org.example.MyTest".
     */
    String fileToClassName(File file, File baseDir) {
        def filePath = file.canonicalFile.absolutePath
        def basePath = baseDir.canonicalFile.absolutePath
        if (!filePath.startsWith(basePath)) {
            throw new IllegalArgumentException("File path (${filePath}) is not descendent of base path (${basePath}).")
        }

        filePath = filePath.substring(basePath.size() + 1)
        def suffixPos = filePath.lastIndexOf(".")
        return filePath[0..(suffixPos - 1)].replace(File.separatorChar, '.' as char)
    }

    ClassLoader getCurrentClassLoader() {
        return currentClassLoader
    }

    /**
     * Creates a class loader that the tests of the given type can be
     * loaded from.
     */
    protected ClassLoader createClassLoader(String type) {
        return new URLClassLoader([
                new File("test/$type").toURI().toURL(),
                new File(testClassesDir, type).toURI().toURL()
        ] as URL[], parentLoader)
    }
}
