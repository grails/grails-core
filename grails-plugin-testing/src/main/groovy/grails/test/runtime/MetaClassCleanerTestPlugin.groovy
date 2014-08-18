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
    String[] requiredFeatures = []
    String[] providedFeatures = ['metaClassCleaner']
    int ordinal = 0

    protected void registerMetaClassRegistryWatcher(TestRuntime runtime) {
        MetaClassRegistryCleaner metaClassRegistryListener = MetaClassRegistryCleaner.createAndRegister()
        runtime.putValue(META_CLASS_REGISTRY_LISTENER, metaClassRegistryListener)
    }

    protected void deregisterMetaClassCleaner(TestRuntime runtime) {
        if (runtime.containsValueFor(META_CLASS_REGISTRY_LISTENER)) {
            MetaClassRegistryCleaner metaClassRegistryListener =  (MetaClassRegistryCleaner)runtime.getValue(META_CLASS_REGISTRY_LISTENER)
            MetaClassRegistryCleaner.cleanAndRemove(metaClassRegistryListener)
            runtime.removeValue(META_CLASS_REGISTRY_LISTENER)
        }
    }

    public void onTestEvent(TestEvent event) {
        TestRuntime runtime=event.runtime
        switch(event.name) {
            case 'startRuntime':
                registerMetaClassRegistryWatcher(runtime)
                break
            case 'closeRuntime':
                deregisterMetaClassCleaner(runtime)
                break
            case 'startFreshRuntime':
                deregisterMetaClassCleaner(runtime)
                registerMetaClassRegistryWatcher(runtime)
                break
        }
    }
    
    public void close(TestRuntime runtime) {
        deregisterMetaClassCleaner(runtime)
    }
}
