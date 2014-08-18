/*
 * Copyright 2014 the original author or authors.
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

package grails.test.runtime;

import groovy.transform.CompileStatic

import org.codehaus.groovy.grails.cli.support.MetaClassRegistryCleaner

/**
 * a TestPlugin for TestRuntime that cleans MetaClass changes in afterClass test event
 * 
 * @author Lari Hotari
 * @since 2.4.0
 *
 */
@CompileStatic
public class MetaClassCleanerTestPlugin implements TestPlugin {
    private static final String META_CLASS_REGISTRY_LISTENER = "metaClassRegistryListener"
    private static final String PER_TEST_METHOD_META_CLASS_REGISTRY_LISTENER = "perTestMethodMetaClassRegistryListener"
    String[] requiredFeatures = []
    String[] providedFeatures = ['metaClassCleaner']
    int ordinal = 0

    protected void registerMetaClassRegistryWatcher(TestRuntime runtime, String id) {
        MetaClassRegistryCleaner metaClassRegistryListener = MetaClassRegistryCleaner.createAndRegister()
        runtime.putValue(id, metaClassRegistryListener)
    }

    protected void deregisterMetaClassCleaner(TestRuntime runtime, String id) {
        if (runtime.containsValueFor(id)) {
            MetaClassRegistryCleaner metaClassRegistryListener =  (MetaClassRegistryCleaner)runtime.getValue(id)
            MetaClassRegistryCleaner.cleanAndRemove(metaClassRegistryListener)
            runtime.removeValue(id)
        }
    }

    public void onTestEvent(TestEvent event) {
        TestRuntime runtime=event.runtime
        def someName = event.name
        switch(event.name) {
            case 'startRuntime':
                registerMetaClassRegistryWatcher(runtime, META_CLASS_REGISTRY_LISTENER)
                break
            case 'closeRuntime':
                deregisterMetaClassCleaner(runtime, META_CLASS_REGISTRY_LISTENER)
                break
            case 'startFreshRuntime':
                deregisterMetaClassCleaner(runtime, META_CLASS_REGISTRY_LISTENER)
                registerMetaClassRegistryWatcher(runtime, META_CLASS_REGISTRY_LISTENER)
                break
            case 'before':
                registerMetaClassRegistryWatcher(runtime, PER_TEST_METHOD_META_CLASS_REGISTRY_LISTENER)
                break
            case 'after':
                deregisterMetaClassCleaner(runtime, PER_TEST_METHOD_META_CLASS_REGISTRY_LISTENER)
                break
        }
    }
    
    public void close(TestRuntime runtime) {
        deregisterMetaClassCleaner(runtime, PER_TEST_METHOD_META_CLASS_REGISTRY_LISTENER)
        deregisterMetaClassCleaner(runtime, META_CLASS_REGISTRY_LISTENER)
    }
}
