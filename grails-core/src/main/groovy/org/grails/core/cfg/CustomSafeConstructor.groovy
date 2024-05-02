/*
 * Copyright 2024 original authors
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
package org.grails.core.cfg

import groovy.transform.Internal
import io.micronaut.context.env.yaml.ConstructIsoTimestampString
import org.yaml.snakeyaml.constructor.SafeConstructor
import org.yaml.snakeyaml.nodes.MappingNode
import org.yaml.snakeyaml.nodes.SequenceNode
import org.yaml.snakeyaml.nodes.Tag

/**
 * Yaml constructor to create containers with sensible
 * default array bounds.
 *
 * @author James Kleeh
 * @since 6.0.0
 */
@Internal
class CustomSafeConstructor extends SafeConstructor {
    CustomSafeConstructor() {
        yamlConstructors.put(Tag.TIMESTAMP, new ConstructIsoTimestampString())
    }

    @Override
    protected Map<Object, Object> newMap(MappingNode node) {
        return createDefaultMap(node.getValue().size())
    }

    @Override
    protected List<Object> newList(SequenceNode node) {
        return createDefaultList(node.getValue().size())
    }
}

