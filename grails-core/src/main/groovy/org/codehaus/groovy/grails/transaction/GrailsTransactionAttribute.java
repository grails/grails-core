package org.codehaus.groovy.grails.transaction;

import java.util.List;

import org.springframework.transaction.interceptor.RollbackRuleAttribute;
import org.springframework.transaction.interceptor.RuleBasedTransactionAttribute;

public class GrailsTransactionAttribute extends RuleBasedTransactionAttribute {
    private static final long serialVersionUID = 1L;
    private boolean inheritRollbackOnly = true;
    private boolean unbindResources = false;

    public GrailsTransactionAttribute() {
        super();
    }

    public GrailsTransactionAttribute(int propagationBehavior, List<RollbackRuleAttribute> rollbackRules) {
        super(propagationBehavior, rollbackRules);
    }

    public GrailsTransactionAttribute(RuleBasedTransactionAttribute other) {
        super(other);
        if(other instanceof GrailsTransactionAttribute) {
            this.inheritRollbackOnly = ((GrailsTransactionAttribute)other).inheritRollbackOnly;
            this.unbindResources = ((GrailsTransactionAttribute)other).unbindResources;
        }
    }

    public boolean isInheritRollbackOnly() {
        return inheritRollbackOnly;
    }

    public void setInheritRollbackOnly(boolean inheritRollbackOnly) {
        this.inheritRollbackOnly = inheritRollbackOnly;
    }

    public boolean isUnbindResources() {
        return unbindResources;
    }

    public void setUnbindResources(boolean unbindResources) {
        this.unbindResources = unbindResources;
    }
}
