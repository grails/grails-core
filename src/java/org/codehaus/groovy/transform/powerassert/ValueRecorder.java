package org.codehaus.groovy.transform.powerassert;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ValueRecorder extends org.codehaus.groovy.runtime.powerassert.ValueRecorder {

	protected static Log log = LogFactory.getLog(ValueRecorder.class);

	public ValueRecorder() {
		log.warn("The org.codehaus.groovy.transform.powerassert.ValueRecorder class that is in the Grails code base is temporary and should NOT be used!");
	}
}
