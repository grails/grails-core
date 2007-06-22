// log4j configuration
log4j {
    appender.stdout = "org.apache.log4j.ConsoleAppender"
	appender.'stdout.layout'="org.apache.log4j.PatternLayout"        
    rootLogger="error,stdout"
    logger {
		grails.spring="info,stdout"
		org {            
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
        org.codehaus.groovy.grails=false
        org.springframework=false
		org.hibernate=false
    }
}