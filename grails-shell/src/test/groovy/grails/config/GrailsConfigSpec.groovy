package grails.config;

import spock.lang.Specification

class GrailsConfigSpec extends Specification{
    
    def "should merge sub-documents in yaml file to single config"() {
        given:
        File file = new File("src/test/resources/grails/config/application.yml")
        GrailsConfig config = new GrailsConfig()
        when:
        config.loadYml(file)
        then:
        config.config.grails.profile == 'web'
        println config.config
    }

}
