// log4j configuration
log4j {
    appender.stdout = "org.apache.log4j.ConsoleAppender"
	appender.'stdout.layout'="org.apache.log4j.PatternLayout"
 	appender.'stdout.layout.ConversionPattern'='[%r] %c{2} %m%n'
    rootLogger="error,stdout"
    logger {
        grails="info,stdout"
        org {
            codehaus.groovy.grails.web="info,stdout"
            codehaus.groovy.grails.commons="info,stdout"
            codehaus.groovy.grails.plugins="info,stdout"
            codehaus.groovy.grails.orm.hibernate="info,stdout"
            springframework="off,stdout"
            hibernate="off,stdout"
        }
    }
	additivity.'default' = false
    additivity {
		grails=false
		org {
           codehaus.groovy.grails=false
           springframework=false
		   hibernate=false
		}
    }
}
