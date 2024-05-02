/*
 * Copyright 2024 original authors
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
package grails.boot

import grails.boot.config.GrailsAutoConfiguration
import org.springframework.boot.SpringApplication
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import spock.lang.Specification

/**
 * Created by graemerocher on 28/05/14.
 */
class GrailsSpringApplicationSpec extends Specification{

    AnnotationConfigServletWebServerApplicationContext context

    void cleanup() {
        context.close()
    }

    void "Test run Grails via SpringApplication"() {
        when:"SpringApplication is used to run a Grails app"
        SpringApplication springApplication  = new SpringApplication(Application)
        springApplication.allowBeanDefinitionOverriding = true
        context = (AnnotationConfigServletWebServerApplicationContext) springApplication.run()

        then:"The application runs"
            context != null
            new URL("http://localhost:${context.webServer.port}/foo/bar").text == 'hello world'
    }


    @Configuration
    @EnableWebMvc
    static class Application extends GrailsAutoConfiguration {
        @Bean
        ConfigurableServletWebServerFactory webServerFactory() {
            TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory(0)
        }
    }
}
