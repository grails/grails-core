package org.codehaus.groovy.grails.cli

class CreateScriptTests extends AbstractCliTests {
    def appDir

    void testCreateScript() {
        appDir = createTestApp()

        tryScript('CreateSomething')
        tryScript('generate-something-else', 'GenerateSomethingElse')
    }

    void tryScript(String className) {
        tryScript(className, className)
    }

    void tryScript(String scriptArg, String className) {
        // Run the 'create script' script with a single argument.
        System.setProperty("grails.cli.args", scriptArg)
        gantRun( ["-f", "scripts/CreateScript.groovy"] as String[])

        // Extract any package from the class name.
        def pkg = null
        def pos = className.lastIndexOf('.')
        if (pos != -1) {
            pkg = className[0..<pos]
            className = className[(pos + 1)..-1]
        }

        def pkgPath = ''
        if (pkg) {
            pkgPath = pkg.replace('.' as char, '/' as char) + '/'
        }

        // Check that the script has been created.
        def dcFile = new File("${appDir}/scripts/${pkgPath}${className}.groovy")
        assert dcFile.exists()
    }
}
