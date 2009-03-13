package grails.ant

import org.apache.tools.ant.BuildException
import org.apache.tools.ant.types.Path

/**
 * Test case for {@link GrailsTask}.
 */
public class GrailsTaskTests extends GroovyTestCase {
    void testWithClasspath() {
//        def task = new GrailsTask()
//        task.script = "Compile"
//        task.addClasspath(new Path(task.project))
//
//        task.execute()
    }

    void testNoScript() {
        def task = new GrailsTask()
        task.home = new File(".")

        shouldFail(BuildException) {
            task.execute()
        }
    }

    void testNoHomeAndNoClasspath() {
        def task = new GrailsTask()
        task.script = "Compile"

        shouldFail(BuildException) {
            task.execute()
        }
    }

    void testHomeAndClasspath() {
        def task = new GrailsTask()
        task.script = "Compile"
        task.home = new File(".")
        task.addClasspath(new Path(task.project))

        shouldFail(BuildException) {
            task.execute()
        }
    }

    void testGetCommand() {
        def task = new GrailsTask()
        task.script = "TestApp"
        assertEquals "test-app", task.command

        task.script = "Compile"
        assertEquals "compile", task.command

        task.script = ""
        assertEquals "", task.command

        task.script = null
        assertNull task.command
    }

    void testSetCommand() {
        def task = new GrailsTask()
        task.command = "test-app"
        assertEquals "TestApp", task.script

        task.command = "compile"
        assertEquals "Compile", task.script

        task.command = ""
        assertEquals "", task.script

        task.command = null
        assertNull task.script
    }
}
