grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir	= "target/test-reports"
grails.project.dependency.resolution = {
    inherits "global" // inherit Grails' default dependencies
    log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    repositories {
       mavenCentral()
    }
    dependencies {
        // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes eg.
        compile( 'org.hibernate:hibernate-core:3.3.1.GA') {
			excludes 'ehcache', 'xml-apis', 'commons-logging'
		}
		compile('org.hibernate:hibernate-commons-annotations:3.3.0.ga') {
			excludes 'hibernate'
		}
        compile 'org.hibernate:hibernate-annotations:3.4.0.GA'
				
				
		runtime 'javassist:javassist:3.4.GA'
		
    }

}
