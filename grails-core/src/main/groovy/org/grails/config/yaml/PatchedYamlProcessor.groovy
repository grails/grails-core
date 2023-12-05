package org.grails.config.yaml

import org.springframework.beans.factory.config.YamlProcessor
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor
import org.yaml.snakeyaml.nodes.MappingNode
import org.yaml.snakeyaml.parser.ParserException

class PatchedYamlProcessor extends YamlProcessor {

    @Override
    protected Yaml createYaml() {
        return new Yaml(new PatchedStrictMapAppenderConstructor())
    }

    protected static class PatchedStrictMapAppenderConstructor extends Constructor {

        PatchedStrictMapAppenderConstructor() {
            super(new LoaderOptions())
        }

        @Override
        protected Map<Object, Object> constructMapping(MappingNode node) {
            try {
                return super.constructMapping(node)
            } catch (IllegalStateException ex) {
                throw new ParserException("while parsing MappingNode", node.startMark, ex.message, node.endMark)
            }
        }

        @Override
        protected Map<Object, Object> createDefaultMap(int initSize) {
            final Map<Object, Object> delegate = super.createDefaultMap(initSize)
            return new AbstractMap<Object, Object>() {
                @Override
                Object put(Object key, Object value) {
                    if (delegate.containsKey(key)) {
                        throw new IllegalStateException("Duplicate key: " + key)
                    }
                    return delegate.put(key, value)
                }

                @Override
                Set<Map.Entry<Object, Object>> entrySet() {
                    return delegate.entrySet()
                }
            }
        }
    }
}
