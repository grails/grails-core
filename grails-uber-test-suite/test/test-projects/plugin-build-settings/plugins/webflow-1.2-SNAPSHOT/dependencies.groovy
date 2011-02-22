grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir	= "target/test-reports"
grails.project.dependency.resolution = {
    inherits "global" // inherit Grails' default dependencies
    log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    dependencies {
        // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes eg.
        compile 'org.grails:grails-webflow:1.2-SNAPSHOT',
 				'org.springframework.webflow:org.springframework.webflow:2.0.8.RELEASE',
 				'org.springframework.webflow:org.springframework.binding:2.0.8.RELEASE',
 				'org.springframework.webflow:org.springframework.js:2.0.8.RELEASE'
		runtime 'ognl:ognl:2.7.3'

    }

}
