// enabled native2ascii conversion of i18n properties files
grails.enable.native2ascii = true
// log4j configuration
log4j {
    appender.stdout = "org.apache.log4j.ConsoleAppender"
	appender.'stdout.layout'="org.apache.log4j.PatternLayout"
 	appender.'stdout.layout.ConversionPattern'='[%r] %c{2} %m%n'
    rootLogger="error,stdout"
    logger {
	        org {
	            springframework="off,stdout"
	            hibernate="off,stdout"
	        }
     }
 	additivity.'default' = false

}
environments {
	development {
		log4j {
    	    logger {
		        grails="info,stdout"
		        org {
		            codehaus.groovy.grails {
						web.servlet="info,stdout"  //  controllers
						web.pages="info,stdout" //  GSP
						web.sitemesh="info,stdout" //  layouts
						this."web.mapping.filter"="info,stdout" // URL mapping
						this."web.mapping"="info,stdout" // URL mapping
						commons="info,stdout" // core / classloading
						plugins="info,stdout" // plugins
						orm.hibernate="info,stdout" // hibernate integration
					}
		        }
		    }
		    additivity {
				grails=false
				org {
		           codehaus.groovy.grails=false
		           springframework=false
				   hibernate=false
				}
		    }
		}
	}
}
