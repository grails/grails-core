package org.codehaus.groovy.grails.cli.parsing

import spock.lang.Specification

/**
 * Tests for {@link CommandLineParser}
 */
class CommandLineParserSpec extends Specification{

    void "Test parse basic command"() {
        when:
            def parser = new CommandLineParser()
            def cl = parser.parse("run-app")

        then:
            cl.commandName == 'run-app'
            cl.environment == 'development'
            cl.systemProperties.size() == 0
            cl.remainingArgs.size() == 0
    }

    void "Test parse command with environment"() {
       when:
            def parser = new CommandLineParser()
            def cl = parser.parse("prod", "run-app")

        then:
            cl.commandName == 'run-app'
            cl.environment == 'production'
            cl.systemProperties.size() == 0
            cl.remainingArgs.size() == 0
    }

    void "Test parse command with environment and sys props"() {
       when:
            def parser = new CommandLineParser()
            def cl = parser.parse("prod", "run-app", "-DmyProp=value")

        then:
            cl.commandName == 'run-app'
            cl.environment == 'production'
            cl.systemProperties.size() == 1
            cl.systemProperties['myProp'] == 'value'
            cl.remainingArgs.size() == 0
    }

    void "Test parse command with environment and sys props and arguments"() {
       when:
            def parser = new CommandLineParser()
            def cl = parser.parse("prod", "run-app", "-DmyProp=value", "foo", "bar")

        then:
            cl.commandName == 'run-app'
            cl.environment == 'production'
            cl.systemProperties.size() == 1
            cl.systemProperties['myProp'] == 'value'
            cl.remainingArgs.size() == 2
            cl.remainingArgs == ['foo', 'bar']
            cl.remainingArgsString == "foo bar"
    }

    void "Test that options with spaces throw an exception"() {
        when:
               def parser = new CommandLineParser()
               def cl = parser.parse("prod", "run-app", "-DmyProp=value", "foo", "bar", "--dev mode")

        then:
            thrown ParseException

    }

    void "Test parse command with environment, sys props, arguments and undeclared options"() {
       when:
            def parser = new CommandLineParser()
            def cl = parser.parse("prod", "run-app", "-DmyProp=value", "foo", "bar", "--dev-mode")

        then:
            cl.commandName == 'run-app'
            cl.environment == 'production'
            cl.systemProperties.size() == 1
            cl.systemProperties['myProp'] == 'value'
            cl.remainingArgs.size() == 2
            cl.remainingArgs == ['foo', 'bar']
            cl.hasOption('dev-mode')
            cl.optionValue('dev-mode') == true

    }

    void "Test parse command with environment, sys props, arguments and undeclared options with values"() {
       when:
            def parser = new CommandLineParser()
            def cl = parser.parse("prod", "run-app", "-DmyProp=value", "foo", "bar", "--host=localhost")

        then:
            cl.commandName == 'run-app'
            cl.environment == 'production'
            cl.systemProperties.size() == 1
            cl.systemProperties['myProp'] == 'value'
            cl.remainingArgs.size() == 2
            cl.remainingArgs == ['foo', 'bar']
            cl.hasOption('host')
            cl.optionValue('host') == "localhost"

    }

    void "Test help message with declared options"() {
        when:
            def parser = new CommandLineParser()
            parser.addOption("interactive-mode", "Enabled interactive mode")
            parser.addOption("version", "Shows the vesrion")

        then:
            String ls = System.getProperty("line.separator");
            parser.helpMessage == "usage: grails [options] [command]${ls} -interactive-mode        Enabled interactive mode${ls} -version                 Shows the vesrion${ls}"
       }



    // STRING tests


    void "Test parse string basic command"() {
        when:
            def parser = new CommandLineParser()
            def cl = parser.parseString("run-app")

        then:
            cl.commandName == 'run-app'
            cl.environment == 'development'
            cl.systemProperties.size() == 0
            cl.remainingArgs.size() == 0
    }

    void "Test parse string command with environment"() {
       when:
            def parser = new CommandLineParser()
            def cl = parser.parseString("prod run-app")

        then:
            cl.commandName == 'run-app'
            cl.environment == 'production'
            cl.systemProperties.size() == 0
            cl.remainingArgs.size() == 0
    }

    void "Test parse string command with environment and sys props"() {
       when:
            def parser = new CommandLineParser()
            def cl = parser.parseString("prod run-app -DmyProp=value")

        then:
            cl.commandName == 'run-app'
            cl.environment == 'production'
            cl.systemProperties.size() == 1
            cl.systemProperties['myProp'] == 'value'
            cl.remainingArgs.size() == 0
    }

    void "Test parse string command with environment and sys props and arguments"() {
       when:
            def parser = new CommandLineParser()
            def cl = parser.parseString("prod run-app -DmyProp=value foo bar")

        then:
            cl.commandName == 'run-app'
            cl.environment == 'production'
            cl.systemProperties.size() == 1
            cl.systemProperties['myProp'] == 'value'
            cl.remainingArgs.size() == 2
            cl.remainingArgs == ['foo', 'bar']
            cl.remainingArgsString == "foo bar"
    }


    void "Test parse string command with environment, sys props, arguments and undeclared options"() {
       when:
            def parser = new CommandLineParser()
            def cl = parser.parseString("prod run-app -DmyProp=value foo bar --dev-mode")

        then:
            cl.commandName == 'run-app'
            cl.environment == 'production'
            cl.systemProperties.size() == 1
            cl.systemProperties['myProp'] == 'value'
            cl.remainingArgs.size() == 2
            cl.remainingArgs == ['foo', 'bar']
            cl.hasOption('dev-mode')
            cl.optionValue('dev-mode') == true

    }

    void "Test parse string command with environment, sys props, arguments and undeclared options with values"() {
       when:
            def parser = new CommandLineParser()
            def cl = parser.parseString("prod run-app -DmyProp=value foo bar --host=localhost")

        then:
            cl.commandName == 'run-app'
            cl.environment == 'production'
            cl.systemProperties.size() == 1
            cl.systemProperties['myProp'] == 'value'
            cl.remainingArgs.size() == 2
            cl.remainingArgs == ['foo', 'bar']
            cl.hasOption('host')
            cl.optionValue('host') == "localhost"

    }
	
	void "Test that parseString handles quoted arguments with double quotes"() {
       when:
            def parser = new CommandLineParser()
            def cl = parser.parseString('refresh-dependencies --include-sources "file with spaces.xml"')

        then:
            cl.commandName == 'refresh-dependencies'
            cl.systemProperties.size() == 0
            cl.remainingArgs.size() == 1
            cl.remainingArgs == ['file with spaces.xml']
	}

	void "Test that parseString handles quoted arguments with single quotes"() {
		when:
			 def parser = new CommandLineParser()
			 def cl = parser.parseString("refresh-dependencies --include-sources 'file with spaces.xml'")
 
		 then:
			 cl.commandName == 'refresh-dependencies'
			 cl.systemProperties.size() == 0
			 cl.remainingArgs.size() == 1
			 cl.remainingArgs == ['file with spaces.xml']
             cl.hasOption('include-sources')
             cl.optionValue('include-sources') == true
	 }
	
	void "Test that parseString with unbalanced double quotes throws ParseException"() {
		when:
			 def parser = new CommandLineParser()
			 def cl = parser.parseString("refresh-dependencies --include-sources 'file with spaces.xml")
 
		 then:
            thrown ParseException
	}
 
	void "Test that parseString with unbalanced single quotes throws ParseException"() {
		when:
			 def parser = new CommandLineParser()
			 def cl = parser.parseString('refresh-dependencies --include-sources "file with spaces.xml')
 
		 then:
            thrown ParseException
	}
}
