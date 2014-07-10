/*
 * Copyright 2013 SpringSource
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
package org.grails.test.runner.phase

/**
 * The default strategy for discovering test types and phase configurers
 *
 * @author Graeme Rocher
 * @since 2.3
 */
class TestFeatureDiscoverySupport {

    // The four test phases that we can run.
    Set<String> unitTests = [ "unit" ] as Set
    Set<String> integrationTests = [ "integration" ] as Set
    Set<String> functionalTests = [] as Set
    Set<String> otherTests = [ "cli" ] as Set

    Map<String, TestPhaseConfigurer> configurers = [unit:new DefaultTestPhaseConfigurer(), other:new DefaultTestPhaseConfigurer()]
    Binding testExecutionContext


    Set<String> findTestType(String phase) {
        final phaseKey = "${phase}Tests"
        if (hasProperty(phaseKey)) {
            return this."$phaseKey"
        }
        else if (testExecutionContext?.hasVariable(phaseKey)) {
            return testExecutionContext.getVariable(phaseKey) as Set
        }
        else {
            return [] as Set
        }
    }

    TestPhaseConfigurer findPhaseConfigurer(String phase) {
        if (configurers.containsKey(phase)) {
            return configurers[phase]
        }
        else {
            if (testExecutionContext?.hasVariable("${phase}TestPhasePreparation") && testExecutionContext?.hasVariable("${phase}TestPhaseCleanUp")) {
                return new ClosureInvokingTestPhaseConfigurer((Closure)testExecutionContext.getVariable("${phase}TestPhasePreparation"),(Closure)testExecutionContext.getVariable("${phase}TestPhaseCleanUp"))
            }
        }
    }
}
