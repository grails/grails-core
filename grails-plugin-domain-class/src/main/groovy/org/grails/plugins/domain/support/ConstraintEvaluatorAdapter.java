package org.grails.plugins.domain.support;

import grails.gorm.validation.ConstrainedProperty;
import grails.validation.Constrained;
import grails.validation.ConstrainedDelegate;
import grails.validation.ConstraintsEvaluator;
import groovy.lang.Closure;

import java.util.LinkedHashMap;
import java.util.Map;

public class ConstraintEvaluatorAdapter implements ConstraintsEvaluator {

    private final org.grails.datastore.gorm.validation.constraints.eval.ConstraintsEvaluator constraintsEvaluator;

    public ConstraintEvaluatorAdapter(org.grails.datastore.gorm.validation.constraints.eval.ConstraintsEvaluator constraintsEvaluator) {
        this.constraintsEvaluator = constraintsEvaluator;
    }

    @Override
    public Map<String, Object> getDefaultConstraints() {
        return constraintsEvaluator.getDefaultConstraints();
    }

    @Override
    public Map<String, Constrained> evaluate(Class cls) {
        final Map<String, ConstrainedProperty> result = constraintsEvaluator.evaluate(cls);
        return adapt(result);
    }

    @Override
    public Map<String, Constrained> evaluate(Class cls, boolean defaultNullable) {
        final Map<String, ConstrainedProperty> result = constraintsEvaluator.evaluate(cls, defaultNullable);
        return adapt(result);
    }

    @Override
    public Map<String, Constrained> evaluate(Class<?> cls, boolean defaultNullable, boolean useOnlyAdHocConstraints, Closure... adHocConstraintsClosures) {
        final Map<String, ConstrainedProperty> result = constraintsEvaluator.evaluate(cls, defaultNullable, useOnlyAdHocConstraints);
        return adapt(result);
    }

    private Map<String, Constrained> adapt(Map<String, ConstrainedProperty> result) {
        Map<String, Constrained> adapted = new LinkedHashMap<>(result.size());
        for (Map.Entry<String, ConstrainedProperty> entry : result.entrySet()) {
            adapted.put(entry.getKey(), new ConstrainedDelegate(entry.getValue()));
        }
        return adapted;
    }
}
