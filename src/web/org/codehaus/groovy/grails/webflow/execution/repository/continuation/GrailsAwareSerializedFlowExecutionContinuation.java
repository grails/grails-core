/* Copyright 2006-2007 Graeme Rocher
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
package org.codehaus.groovy.grails.webflow.execution.repository.continuation;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware;
import org.springframework.util.Assert;
import org.springframework.webflow.execution.FlowExecution;
import org.springframework.webflow.execution.repository.continuation.SerializedFlowExecutionContinuation;
import org.springframework.webflow.execution.repository.continuation.ContinuationCreationException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

/**
 * <p>A SerializedFlowExecutionContinuation that is capable of deserializing flows that have Grails classes
 * stored in the flow context.
 *
 * <p>This class overrides the deserialize(byte[]) method and makes the ObjectInputStream capable of loading classes
 * from the Grails class loader
 *
 * @author Graeme Rocher
 * @since 0.6
 *        <p/>
 *        Created: Jul 6, 2007
 *        Time: 11:02:00 PM
 */
public class GrailsAwareSerializedFlowExecutionContinuation extends SerializedFlowExecutionContinuation implements GrailsApplicationAware {
    private GrailsApplication grailsApplication;

    public GrailsAwareSerializedFlowExecutionContinuation() {
        super();
    }

    public GrailsAwareSerializedFlowExecutionContinuation(FlowExecution flowExecution, boolean compress) throws ContinuationCreationException {
        super(flowExecution, compress);
    }

    protected FlowExecution deserialize(byte[] data) throws IOException, ClassNotFoundException {
        Assert.notNull(grailsApplication, "Property [grailsApplication] must be set!");
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data)) {
            protected Class resolveClass(ObjectStreamClass objectStreamClass) throws IOException, ClassNotFoundException {
                return Class.forName(objectStreamClass.getName(), true, grailsApplication.getClassLoader());
            }
        };
        try {
            return (FlowExecution)ois.readObject();
        }
        finally {
            ois.close();
        }

    }

    public void setGrailsApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication;
    }
}
