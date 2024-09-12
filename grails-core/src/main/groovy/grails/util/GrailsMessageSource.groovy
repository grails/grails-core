package grails.util

import org.springframework.context.MessageSource

class GrailsMessageSource {
    static MessageSource getMessageSource(List<MessageSource> messageSources){
        if(!messageSources) {
            return null
        }

        if(messageSources.size() == 1) {
            return messageSources.get(0)
        }

        messageSources.each { messageSource ->
            String className = messageSource.class.name
            // use the grails or spring message source
            if(className.startsWith("org.grails") || className.startsWith("grails")
                    || className.startsWith("org.springframework")) {
                return messageSource
            }
        }

        //return the first message source
        return messageSources.get(0)
    }
}
