package @grails.codegen.defaultPackage@

import grails.boot.config.GrailsConfiguration
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.EnableAutoConfiguration

@EnableAutoConfiguration
class Application extends GrailsConfiguration {
    static void main(String[] args) {
        SpringApplication.run(Application)
    }
}