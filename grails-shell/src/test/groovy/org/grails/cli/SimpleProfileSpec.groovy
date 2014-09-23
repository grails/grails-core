package org.grails.cli

import spock.lang.Specification

class SimpleProfileSpec extends Specification {
    SimpleProfile profile
    
    def setup() {
        profile = new SimpleProfile('web', new File('src/test/resources/profiles-repository/profiles/web'))
    }
    
    def "should contain 4 commands"() {
        when:
        def commands = profile.getCommandLineHandlers()[0].listCommands()
        then:
        commands.size() == 4
        commands*.name as Set == ['create-controller', 'create-domain', 'create-service', 'create-taglib'] as Set
    }
}
