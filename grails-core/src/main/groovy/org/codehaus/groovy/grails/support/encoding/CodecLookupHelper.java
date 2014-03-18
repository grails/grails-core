package org.codehaus.groovy.grails.support.encoding;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;

public class CodecLookupHelper {
    private static final Logger log = LoggerFactory.getLogger(CodecLookupHelper.class);
    
    /**
     * Lookup encoder.
     *
     * @param grailsApplication the grailsApplication instance
     * @param codecName the codec name
     * @return the encoder instance
     */
    public static Encoder lookupEncoder(GrailsApplication grailsApplication, String codecName) {
        ApplicationContext ctx = grailsApplication != null ? grailsApplication.getMainContext() : null;
        if(ctx != null) {
            try {
                CodecLookup codecLookup = ctx.getBean("codecLookup", CodecLookup.class);
                return codecLookup.lookupEncoder(codecName);
            } catch (NoSuchBeanDefinitionException e) {
                // ignore missing codecLookup bean in tests
                log.debug("codecLookup bean is missing from test context.", e);
            }
        }
        return null;
    }
}
