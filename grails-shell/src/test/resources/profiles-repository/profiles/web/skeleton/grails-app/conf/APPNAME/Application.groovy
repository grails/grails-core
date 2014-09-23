package @APPNAME@

import grails.boot.config.GrailsConfiguration
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.Import

@EnableAutoConfiguration
class Application extends GrailsConfiguration {
    static void main(String[] args) {
        SpringApplication.run(Application)
    }
}
