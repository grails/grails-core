/* Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.validation;

import groovy.lang.MissingPropertyException;
import groovy.lang.Range;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.validation.exceptions.ConstraintException;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.context.MessageSource;
import org.springframework.util.Assert;
import org.springframework.validation.Errors;

/**
 * Provides the ability to set contraints against a properties of a class. Constraints can either be
 * set via the property setters or via the <pre>applyConstraint(String constraintName, Object constrainingValue)</pre>
 * in combination with a constraint constant. Example:
 *
 * <code>
 *      ...
 *
 *         ConstrainedProperty cp = new ConstrainedProperty(owningClass, propertyName, propertyType);
 *      if (cp.supportsConstraint(ConstrainedProperty.EMAIL_CONSTRAINT)) {
 *          cp.applyConstraint(ConstrainedProperty.EMAIL_CONSTRAINT, new Boolean(true));
 *      }
 * </code>
 *
 * Alternatively constraints can be applied directly using the java bean getters/setters if a static (as oposed to dynamic)
 * approach to constraint creation is possible:
 *
 * <code>
 *       cp.setEmail(true)
 * </code>
 * @author Graeme Rocher
 * @since 07-Nov-2005
 */
public class ConstrainedProperty   {

    static final String DEFAULT_NULL_MESSAGE_CODE = "default.null.message";
    static final String DEFAULT_INVALID_MIN_SIZE_MESSAGE_CODE = "default.invalid.min.size.message";
    static final String DEFAULT_INVALID_MAX_SIZE_MESSAGE_CODE = "default.invalid.max.size.message";
    static final String DEFAULT_NOT_EQUAL_MESSAGE_CODE = "default.not.equal.message";
    static final String DEFAULT_INVALID_MIN_MESSAGE_CODE = "default.invalid.min.message";
    static final String DEFAULT_INVALID_MAX_MESSAGE_CODE = "default.invalid.max.message";
    static final String DEFAULT_INVALID_SIZE_MESSAGE_CODE = "default.invalid.size.message";
    static final String DEFAULT_NOT_INLIST_MESSAGE_CODE = "default.not.inlist.message";
    static final String DEFAULT_INVALID_RANGE_MESSAGE_CODE = "default.invalid.range.message";
    static final String DEFAULT_INVALID_EMAIL_MESSAGE_CODE = "default.invalid.email.message";
    static final String DEFAULT_INVALID_CREDIT_CARD_MESSAGE_CODE = "default.invalid.creditCard.message";
    static final String DEFAULT_INVALID_URL_MESSAGE_CODE = "default.invalid.url.message";
    static final String DEFAULT_INVALID_VALIDATOR_MESSAGE_CODE = "default.invalid.validator.message";
    static final String DEFAULT_DOESNT_MATCH_MESSAGE_CODE = "default.doesnt.match.message";
    static final String DEFAULT_BLANK_MESSAGE_CODE = "default.blank.message";

    protected static final ResourceBundle bundle = ResourceBundle.getBundle("org.codehaus.groovy.grails.validation.DefaultErrorMessages");

    private static final String DEFAULT_BLANK_MESSAGE = bundle.getString(DEFAULT_BLANK_MESSAGE_CODE);
    private static final String DEFAULT_DOESNT_MATCH_MESSAGE = bundle.getString(DEFAULT_DOESNT_MATCH_MESSAGE_CODE);
    private static final String DEFAULT_INVALID_URL_MESSAGE = bundle.getString(DEFAULT_INVALID_URL_MESSAGE_CODE);
    private static final String DEFAULT_INVALID_CREDIT_CARD_MESSAGE = bundle.getString(DEFAULT_INVALID_CREDIT_CARD_MESSAGE_CODE);
    private static final String DEFAULT_INVALID_EMAIL_MESSAGE = bundle.getString(DEFAULT_INVALID_EMAIL_MESSAGE_CODE);
    private static final String DEFAULT_INVALID_RANGE_MESSAGE = bundle.getString(DEFAULT_INVALID_RANGE_MESSAGE_CODE);
    private static final String DEFAULT_NOT_IN_LIST_MESSAGE = bundle.getString(DEFAULT_NOT_INLIST_MESSAGE_CODE);
    private static final String DEFAULT_INVALID_SIZE_MESSAGE = bundle.getString(DEFAULT_INVALID_SIZE_MESSAGE_CODE);
    private static final String DEFAULT_INVALID_MAX_MESSAGE = bundle.getString(DEFAULT_INVALID_MAX_MESSAGE_CODE);
    private static final String DEFAULT_INVALID_MIN_MESSAGE = bundle.getString(DEFAULT_INVALID_MIN_MESSAGE_CODE);
    private static final String DEFAULT_NOT_EQUAL_MESSAGE = bundle.getString(DEFAULT_NOT_EQUAL_MESSAGE_CODE);
    private static final String DEFAULT_INVALID_MAX_SIZE_MESSAGE = bundle.getString(DEFAULT_INVALID_MAX_SIZE_MESSAGE_CODE);
    private static final String DEFAULT_INVALID_MIN_SIZE_MESSAGE = bundle.getString(DEFAULT_INVALID_MIN_SIZE_MESSAGE_CODE);
    private static final String DEFAULT_NULL_MESSAGE = bundle.getString(DEFAULT_NULL_MESSAGE_CODE);
    private static final String DEFAULT_INVALID_VALIDATOR_MESSAGE = bundle.getString(DEFAULT_INVALID_VALIDATOR_MESSAGE_CODE);

    public static final String CREDIT_CARD_CONSTRAINT = "creditCard";
    public static final String EMAIL_CONSTRAINT = "email";
    public static final String BLANK_CONSTRAINT = "blank";
    public static final String RANGE_CONSTRAINT = "range";
    public static final String IN_LIST_CONSTRAINT = "inList";
    public static final String URL_CONSTRAINT = "url";
    public static final String MATCHES_CONSTRAINT = "matches";
    public static final String SIZE_CONSTRAINT = "size";
    public static final String MIN_CONSTRAINT = "min";
    public static final String MAX_CONSTRAINT = "max";
    public static final String MAX_SIZE_CONSTRAINT = "maxSize";
    public static final String MIN_SIZE_CONSTRAINT = "minSize";
    public static final String SCALE_CONSTRAINT = "scale";
    public static final String NOT_EQUAL_CONSTRAINT = "notEqual";
    public static final String NULLABLE_CONSTRAINT = "nullable";
    public static final String VALIDATOR_CONSTRAINT = "validator";

    protected static final String INVALID_SUFFIX = ".invalid";
    protected static final String EXCEEDED_SUFFIX = ".exceeded";
    protected static final String NOTMET_SUFFIX = ".notmet";
    protected static final String NOT_PREFIX = "not.";
    protected static final String TOOBIG_SUFFIX = ".toobig";
    protected static final String TOOLONG_SUFFIX = ".toolong";
    protected static final String TOOSMALL_SUFFIX = ".toosmall";
    protected static final String TOOSHORT_SUFFIX = ".tooshort";

    protected static Map<String, Object> constraints = new HashMap<String, Object>();
    protected static final Map<String, String> DEFAULT_MESSAGES = new HashMap<String, String>();

    static {
        DEFAULT_MESSAGES.put(DEFAULT_BLANK_MESSAGE_CODE,DEFAULT_BLANK_MESSAGE);
        DEFAULT_MESSAGES.put(DEFAULT_DOESNT_MATCH_MESSAGE_CODE,DEFAULT_DOESNT_MATCH_MESSAGE);
        DEFAULT_MESSAGES.put(DEFAULT_INVALID_CREDIT_CARD_MESSAGE_CODE,DEFAULT_INVALID_CREDIT_CARD_MESSAGE);
        DEFAULT_MESSAGES.put(DEFAULT_INVALID_EMAIL_MESSAGE_CODE,DEFAULT_INVALID_EMAIL_MESSAGE);
        DEFAULT_MESSAGES.put(DEFAULT_INVALID_MAX_MESSAGE_CODE,DEFAULT_INVALID_MAX_MESSAGE);
        DEFAULT_MESSAGES.put(DEFAULT_INVALID_MAX_SIZE_MESSAGE_CODE,DEFAULT_INVALID_MAX_SIZE_MESSAGE);
        DEFAULT_MESSAGES.put(DEFAULT_INVALID_MIN_MESSAGE_CODE,DEFAULT_INVALID_MIN_MESSAGE);
        DEFAULT_MESSAGES.put(DEFAULT_INVALID_MIN_SIZE_MESSAGE_CODE,DEFAULT_INVALID_MIN_SIZE_MESSAGE);
        DEFAULT_MESSAGES.put(DEFAULT_INVALID_RANGE_MESSAGE_CODE,DEFAULT_INVALID_RANGE_MESSAGE);
        DEFAULT_MESSAGES.put(DEFAULT_INVALID_SIZE_MESSAGE_CODE,DEFAULT_INVALID_SIZE_MESSAGE);
        DEFAULT_MESSAGES.put(DEFAULT_INVALID_URL_MESSAGE_CODE,DEFAULT_INVALID_URL_MESSAGE);
        DEFAULT_MESSAGES.put(DEFAULT_NOT_EQUAL_MESSAGE_CODE,DEFAULT_NOT_EQUAL_MESSAGE);
        DEFAULT_MESSAGES.put(DEFAULT_NOT_INLIST_MESSAGE_CODE,DEFAULT_NOT_IN_LIST_MESSAGE);
        DEFAULT_MESSAGES.put(DEFAULT_NULL_MESSAGE_CODE,DEFAULT_NULL_MESSAGE);
        DEFAULT_MESSAGES.put(DEFAULT_INVALID_VALIDATOR_MESSAGE_CODE, DEFAULT_INVALID_VALIDATOR_MESSAGE);

        constraints.put(CREDIT_CARD_CONSTRAINT, CreditCardConstraint.class);
        constraints.put(EMAIL_CONSTRAINT, EmailConstraint.class);
        constraints.put(BLANK_CONSTRAINT, BlankConstraint.class);
        constraints.put(RANGE_CONSTRAINT, RangeConstraint.class);
        constraints.put(IN_LIST_CONSTRAINT, InListConstraint.class);
        constraints.put(URL_CONSTRAINT, UrlConstraint.class);
        constraints.put(SIZE_CONSTRAINT, SizeConstraint.class);
        constraints.put(MATCHES_CONSTRAINT, MatchesConstraint.class);
        constraints.put(MIN_CONSTRAINT, MinConstraint.class);
        constraints.put(MAX_CONSTRAINT, MaxConstraint.class);
        constraints.put(MAX_SIZE_CONSTRAINT, MaxSizeConstraint.class);
        constraints.put(MIN_SIZE_CONSTRAINT, MinSizeConstraint.class);
        constraints.put(SCALE_CONSTRAINT, ScaleConstraint.class);
        constraints.put(NULLABLE_CONSTRAINT, NullableConstraint.class);
        constraints.put(NOT_EQUAL_CONSTRAINT, NotEqualConstraint.class);
        constraints.put(VALIDATOR_CONSTRAINT, ValidatorConstraint.class);
    }

    protected static final Log LOG = LogFactory.getLog(ConstrainedProperty.class);

    // move these to subclass

    protected String propertyName;
    protected Class<?> propertyType;

    protected Map<String, Constraint> appliedConstraints = new LinkedHashMap<String, Constraint>();
    protected Class<?> owningClass;
    private BeanWrapper bean;

    // simple constraints
    private boolean display = true; // whether the property should be displayed
    private boolean editable = true; // whether the property is editable
    //private boolean file; // whether the property is a file
    private int order; // what order to property appears in
    private String format; // the format of the property (for example a date pattern)
    private String widget; // the widget to use to render the property
    private boolean password; // whether the property is a password
    @SuppressWarnings("rawtypes")
    private Map attributes = Collections.EMPTY_MAP; // a map of attributes of property
    protected MessageSource messageSource;
    private Map<String, Object> metaConstraints = new HashMap<String, Object>();

    /**
     * Constructs a new ConstrainedProperty for the given arguments.
     *
     * @param clazz The owning class
     * @param propertyName The name of the property
     * @param propertyType The property type
     */
    public ConstrainedProperty(Class<?> clazz,String propertyName, Class<?> propertyType) {
        owningClass = clazz;
        this.propertyName = propertyName;
        this.propertyType = propertyType;
        bean = new BeanWrapperImpl(this);
    }

    public static void registerNewConstraint(String name, Class<?> constraintClass) {
        Assert.hasLength(name, "Argument [name] cannot be null");
        if (constraintClass == null || !Constraint.class.isAssignableFrom(constraintClass)) {
            throw new IllegalArgumentException("Argument [constraintClass] with value [" + constraintClass +
                    "] is not a valid constraint");
        }

        constraints.put(name, constraintClass);
    }

    public static void registerNewConstraint(String name, ConstraintFactory factory) {
        Assert.hasLength(name, "Argument [name] cannot be null or blank");
        Assert.notNull(factory, "Argument [factory] cannot be null");
        constraints.put(name, factory);
    }

    public static boolean hasRegisteredConstraint(String constraintName) {
        return constraints.containsKey(constraintName);
    }

    /**
     * @return Returns the appliedConstraints.
     */
    public Collection<Constraint> getAppliedConstraints() {
        return appliedConstraints.values();
    }

    /**
     * Obtains an applied constraint by name.
     * @param name The name of the constraint
     * @return The applied constraint
     */
    public Constraint getAppliedConstraint(String name) {
        return appliedConstraints.get(name);
    }

    /**
     * @param constraintName The name of the constraint to check
     * @return Returns true if the specified constraint name is being applied to this property
     */
    public boolean hasAppliedConstraint(String constraintName) {
        return appliedConstraints.containsKey(constraintName);
    }

    /**
     * @return Returns the propertyType.
     */
    public Class<?> getPropertyType() {
        return propertyType;
    }

    /**
     * @return Returns the max.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Comparable getMax() {
        Comparable maxValue = null;

        MaxConstraint maxConstraint = (MaxConstraint)appliedConstraints.get(MAX_CONSTRAINT);
        RangeConstraint rangeConstraint = (RangeConstraint)appliedConstraints.get(RANGE_CONSTRAINT);

        if ((maxConstraint != null) || (rangeConstraint != null)) {
            Comparable maxConstraintValue = maxConstraint != null ? maxConstraint.getMaxValue() : null;
            Comparable rangeConstraintHighValue = rangeConstraint != null ? rangeConstraint.getRange().getTo() : null;

            if ((maxConstraintValue != null) && (rangeConstraintHighValue != null)) {
                maxValue = (maxConstraintValue.compareTo(rangeConstraintHighValue) < 0) ? maxConstraintValue : rangeConstraintHighValue;
            }
            else if ((maxConstraintValue == null) && (rangeConstraintHighValue != null)) {
                maxValue = rangeConstraintHighValue;
            }
            else if ((maxConstraintValue != null) && (rangeConstraintHighValue == null)) {
                maxValue = maxConstraintValue;
            }
        }

        return maxValue;
    }

    /**
     * @param max The max to set.
     */
    @SuppressWarnings("rawtypes")
    public void setMax(Comparable max) {
        if (max == null) {
            appliedConstraints.remove(MAX_CONSTRAINT);
            return;
        }

        if (!propertyType.equals(max.getClass())) {
            throw new MissingPropertyException(MAX_CONSTRAINT,propertyType);
        }

        Range r = getRange();
        if (r != null) {
            LOG.warn("Range constraint already set ignoring constraint [" + MAX_CONSTRAINT + "] for value [" + max + "]");
            return;
        }

        Constraint c = appliedConstraints.get(MAX_CONSTRAINT);
        if (c != null) {
            c.setParameter(max);
        }
        else {
            c = new MaxConstraint();
            c.setOwningClass(owningClass);
            c.setPropertyName(propertyName);
            c.setParameter(max);
            appliedConstraints.put(MAX_CONSTRAINT, c);
        }
    }

    /**
     * @return Returns the min.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Comparable getMin() {
        Comparable minValue = null;

        MinConstraint minConstraint = (MinConstraint)appliedConstraints.get(MIN_CONSTRAINT);
        RangeConstraint rangeConstraint = (RangeConstraint)appliedConstraints.get(RANGE_CONSTRAINT);

        if ((minConstraint != null) || (rangeConstraint != null)) {
            Comparable minConstraintValue = minConstraint != null ? minConstraint.getMinValue() : null;
            Comparable rangeConstraintLowValue = rangeConstraint != null ? rangeConstraint.getRange().getFrom() : null;

            if ((minConstraintValue != null) && (rangeConstraintLowValue != null)) {
                minValue = (minConstraintValue.compareTo(rangeConstraintLowValue) > 0) ? minConstraintValue : rangeConstraintLowValue;
            }
            else if ((minConstraintValue == null) && (rangeConstraintLowValue != null)) {
                minValue = rangeConstraintLowValue;
            }
            else if ((minConstraintValue != null) && (rangeConstraintLowValue == null)) {
                minValue = minConstraintValue;
            }
        }

        return minValue;
    }

    /**
     * @param min The min to set.
     */
    @SuppressWarnings("rawtypes")
    public void setMin(Comparable min) {
        if (min == null) {
            appliedConstraints.remove(MIN_CONSTRAINT);
            return;
        }

        if (!propertyType.equals(min.getClass())) {
            throw new MissingPropertyException(MIN_CONSTRAINT,propertyType);
        }

        Range r = getRange();
        if (r != null) {
            LOG.warn("Range constraint already set ignoring constraint ["+MIN_CONSTRAINT+"] for value ["+min+"]");
            return;
        }

        Constraint c = appliedConstraints.get(MIN_CONSTRAINT);
        if (c != null) {
            c.setParameter(min);
        }
        else {
            c = new MinConstraint();
            c.setOwningClass(owningClass);
            c.setPropertyName(propertyName);
            c.setParameter(min);
            appliedConstraints.put(MIN_CONSTRAINT, c);
        }
    }

    /**
     * @return Returns the inList.
     */
    @SuppressWarnings("rawtypes")
    public List getInList() {
        InListConstraint c = (InListConstraint)appliedConstraints.get(IN_LIST_CONSTRAINT);
        return c == null ? null : c.getList();
    }

    /**
     * @param inList The inList to set.
     */
    @SuppressWarnings("rawtypes")
    public void setInList(List inList) {
        Constraint c = appliedConstraints.get(IN_LIST_CONSTRAINT);
        if (inList == null) {
            appliedConstraints.remove(IN_LIST_CONSTRAINT);
        }
        else {
            if (c != null) {
                c.setParameter(inList);
            }
            else {
                c = new InListConstraint();
                c.setOwningClass(owningClass);
                c.setPropertyName(propertyName);
                c.setParameter(inList);
                appliedConstraints.put(IN_LIST_CONSTRAINT, c);
            }
        }
    }

    /**
     * @return Returns the range.
     */
    @SuppressWarnings("rawtypes")
    public Range getRange() {
        RangeConstraint c = (RangeConstraint)appliedConstraints.get(RANGE_CONSTRAINT);
        return c == null ? null : c.getRange();
    }

    /**
     * @param range The range to set.
     */
    @SuppressWarnings("rawtypes")
    public void setRange(Range range) {
        if (appliedConstraints.containsKey(MAX_CONSTRAINT)) {
            LOG.warn("Setting range constraint on property ["+propertyName+"] of class ["+owningClass+"] forced removal of max constraint");
            appliedConstraints.remove(MAX_CONSTRAINT);
        }
        if (appliedConstraints.containsKey(MIN_CONSTRAINT)) {
            LOG.warn("Setting range constraint on property ["+propertyName+"] of class ["+owningClass+"] forced removal of min constraint");
            appliedConstraints.remove(MIN_CONSTRAINT);
        }
        if (range == null) {
            appliedConstraints.remove(RANGE_CONSTRAINT);
        }
        else {
            Constraint c = appliedConstraints.get(RANGE_CONSTRAINT);
            if (c != null) {
                c.setParameter(range);
            }
            else {
                c = new RangeConstraint();
                c.setOwningClass(owningClass);
                c.setPropertyName(propertyName);
                c.setParameter(range);

                appliedConstraints.put(RANGE_CONSTRAINT,c);
            }
        }
    }

    /**
     * @return The scale, if defined for this property; null, otherwise
     */
    public Integer getScale() {
        ScaleConstraint scaleConstraint = (ScaleConstraint)appliedConstraints.get(SCALE_CONSTRAINT);
        if (scaleConstraint != null) {
            return scaleConstraint.getScale();
        }

        return null;
    }

    /**
     * @return Returns the size.
     */
    @SuppressWarnings("rawtypes")
    public Range getSize() {
        SizeConstraint c = (SizeConstraint)appliedConstraints.get(SIZE_CONSTRAINT);
        return c == null ? null : c.getRange();
    }

    /**
     * @param size The size to set.
     */
    @SuppressWarnings("rawtypes")
    public void setSize(Range size) {
        Constraint c = appliedConstraints.get(SIZE_CONSTRAINT);
        if (size == null) {
            appliedConstraints.remove(SIZE_CONSTRAINT);
        }
        else {
            if (c != null) {
                c.setParameter(size);
            }
            else {
                c = new SizeConstraint();
                c.setOwningClass(owningClass);
                c.setPropertyName(propertyName);
                c.setParameter(size);
                appliedConstraints.put(SIZE_CONSTRAINT, c);
            }
        }
    }

    /**
     * @return Returns the blank.
     */
    public boolean isBlank() {
        Object cons = appliedConstraints.get(BLANK_CONSTRAINT);
        return cons == null || ((Boolean) ((BlankConstraint) cons).getParameter()).booleanValue();
    }

    /**
     * @param blank The blank to set.
     */
    public void setBlank(boolean blank) {
        if (isNotValidStringType()) {
            throw new MissingPropertyException("Blank constraint can only be applied to a String property",
                    BLANK_CONSTRAINT, owningClass);
        }

        if (!blank) {
            appliedConstraints.remove(BLANK_CONSTRAINT);
        }
        else {
            Constraint c = appliedConstraints.get(BLANK_CONSTRAINT);
            if (c != null) {
                c.setParameter(Boolean.valueOf(blank));
            }
            else {
                c = new BlankConstraint();
                c.setOwningClass(owningClass);
                c.setPropertyName(propertyName);
                c.setParameter(Boolean.valueOf(blank));
                appliedConstraints.put(BLANK_CONSTRAINT,c);
            }
        }
    }

    /**
     * @return Returns the email.
     */
    public boolean isEmail() {
        if (isNotValidStringType()) {
            throw new MissingPropertyException("Email constraint only applies to a String property",
                    EMAIL_CONSTRAINT, owningClass);
        }

        return appliedConstraints.containsKey(EMAIL_CONSTRAINT);
    }

    /**
     * @param email The email to set.
     */
    public void setEmail(boolean email) {
        if (isNotValidStringType()) {
            throw new MissingPropertyException("Email constraint can only be applied to a String property",
                    EMAIL_CONSTRAINT, owningClass);
        }

        Constraint c = appliedConstraints.get(EMAIL_CONSTRAINT);
        if (email) {
            if (c != null) {
                c.setParameter(Boolean.valueOf(email));
            }
            else {
                c = new EmailConstraint();
                c.setOwningClass(owningClass);
                c.setPropertyName(propertyName);
                c.setParameter(Boolean.valueOf(email));
                appliedConstraints.put(EMAIL_CONSTRAINT,c);
            }
        }
        else {
            if (c != null) {
                appliedConstraints.remove(EMAIL_CONSTRAINT);
            }
        }
    }

    private boolean isNotValidStringType() {
        return !CharSequence.class.isAssignableFrom(propertyType);
    }

    /**
     * @return Returns the creditCard.
     */
    public boolean isCreditCard() {
        if (isNotValidStringType()) {
            throw new MissingPropertyException("CreditCard constraint only applies to a String property",
                    CREDIT_CARD_CONSTRAINT,owningClass);
        }

        return appliedConstraints.containsKey(CREDIT_CARD_CONSTRAINT);
    }

    /**
     * @param creditCard The creditCard to set.
     */
    public void setCreditCard(boolean creditCard) {
        if (isNotValidStringType()) {
            throw new MissingPropertyException("CreditCard constraint only applies to a String property",
                    CREDIT_CARD_CONSTRAINT,owningClass);
        }

        Constraint c = appliedConstraints.get(CREDIT_CARD_CONSTRAINT);
        if (creditCard) {
            if (c != null) {
                c.setParameter(Boolean.valueOf(creditCard));
            }
            else {
                c = new CreditCardConstraint();
                c.setOwningClass(owningClass);
                c.setPropertyName(propertyName);
                c.setParameter(Boolean.valueOf(creditCard));
                appliedConstraints.put(CREDIT_CARD_CONSTRAINT,c);
            }
        }
        else {
            if (c != null) {
                appliedConstraints.remove(CREDIT_CARD_CONSTRAINT);
            }
        }
    }

    /**
     * @return Returns the matches.
     */
    public String getMatches() {
        if (isNotValidStringType()) {
            throw new MissingPropertyException("Matches constraint only applies to a String property",
                    MATCHES_CONSTRAINT, owningClass);
        }
        MatchesConstraint c = (MatchesConstraint)appliedConstraints.get(MATCHES_CONSTRAINT);
        return c == null ? null : c.getRegex();
    }

    /**
     * @param regex The matches to set.
     */
    public void setMatches(String regex) {
        if (isNotValidStringType()) {
            throw new MissingPropertyException("Matches constraint can only be applied to a String property",
                    MATCHES_CONSTRAINT, owningClass);
        }

        Constraint c = appliedConstraints.get(MATCHES_CONSTRAINT);
        if (regex == null) {
            appliedConstraints.remove(MATCHES_CONSTRAINT);
        }
        else {
            if (c != null) {
                c.setParameter(regex);
            }
            else {
                c = new MatchesConstraint();
                c.setOwningClass(owningClass);
                c.setPropertyName(propertyName);
                c.setParameter(regex);
                appliedConstraints.put(MATCHES_CONSTRAINT,c);
            }
        }
    }

    /**
     * @return Returns the notEqual.
     */
    public Object getNotEqual() {
        NotEqualConstraint c = (NotEqualConstraint)appliedConstraints.get(NOT_EQUAL_CONSTRAINT);
        return c == null ? null : c.getNotEqualTo();
    }

    /**
     * @return Returns the maxSize.
     */
    public Integer getMaxSize() {
        Integer maxSize = null;

        MaxSizeConstraint maxSizeConstraint = (MaxSizeConstraint)appliedConstraints.get(MAX_SIZE_CONSTRAINT);
        SizeConstraint sizeConstraint = (SizeConstraint)appliedConstraints.get(SIZE_CONSTRAINT);

        if ((maxSizeConstraint != null) || (sizeConstraint != null)) {
            int maxSizeConstraintValue = maxSizeConstraint != null ? maxSizeConstraint.getMaxSize() : Integer.MAX_VALUE;
            int sizeConstraintHighValue = sizeConstraint != null ? sizeConstraint.getRange().getToInt() : Integer.MAX_VALUE;
            maxSize = new Integer(Math.min(maxSizeConstraintValue, sizeConstraintHighValue));
        }

        return maxSize;
    }

    /**
     * @param maxSize The maxSize to set.
     */
    public void setMaxSize(Integer maxSize) {
        Constraint c = appliedConstraints.get(MAX_SIZE_CONSTRAINT);
        if (c != null) {
            c.setParameter(maxSize);
        }
        else {
            c = new MaxSizeConstraint();
            c.setOwningClass(owningClass);
            c.setPropertyName(propertyName);
            c.setParameter(maxSize);
            appliedConstraints.put(MAX_SIZE_CONSTRAINT,c);
        }
    }

    /**
     * @return Returns the minSize.
     */
    public Integer getMinSize() {
        Integer minSize = null;

        MinSizeConstraint minSizeConstraint = (MinSizeConstraint)appliedConstraints.get(MIN_SIZE_CONSTRAINT);
        SizeConstraint sizeConstraint = (SizeConstraint)appliedConstraints.get(SIZE_CONSTRAINT);

        if ((minSizeConstraint != null) || (sizeConstraint != null)) {
            int minSizeConstraintValue = minSizeConstraint != null ? minSizeConstraint.getMinSize() : Integer.MIN_VALUE;
            int sizeConstraintLowValue = sizeConstraint != null ? sizeConstraint.getRange().getFromInt() : Integer.MIN_VALUE;

            minSize = new Integer(Math.max(minSizeConstraintValue, sizeConstraintLowValue));
        }

        return minSize;
    }

    /**
     * @param minSize The minLength to set.
     */
    public void setMinSize(Integer minSize) {
        Constraint c = appliedConstraints.get(MIN_SIZE_CONSTRAINT);
        if (c != null) {
            c.setParameter(minSize);
        }
        else {
            c = new MinSizeConstraint();
            c.setOwningClass(owningClass);
            c.setPropertyName(propertyName);
            c.setParameter(minSize);
            appliedConstraints.put(MIN_SIZE_CONSTRAINT,c);
        }
    }

    /**
     * @param notEqual The notEqual to set.
     */
    public void setNotEqual(Object notEqual) {
        if (notEqual == null) {
            appliedConstraints.remove(NOT_EQUAL_CONSTRAINT);
        }
        else {
            Constraint c = new NotEqualConstraint();
            c.setOwningClass(owningClass);
            c.setPropertyName(propertyName);
            c.setParameter(notEqual);
            appliedConstraints.put(NOT_EQUAL_CONSTRAINT, c);
        }
    }

    /**
     * @return Returns the nullable.
     */
    public boolean isNullable() {
        if (appliedConstraints.containsKey(NULLABLE_CONSTRAINT)) {
            NullableConstraint nc = (NullableConstraint)appliedConstraints.get(NULLABLE_CONSTRAINT);
            return nc.isNullable();
        }

        return false;
    }

    /**
     * @param nullable The nullable to set.
     */
    public void setNullable(boolean nullable) {
        NullableConstraint nc = (NullableConstraint)appliedConstraints.get(NULLABLE_CONSTRAINT);
        if (nc == null) {
            nc = new NullableConstraint();
            nc.setOwningClass(owningClass);
            nc.setPropertyName(propertyName);
            appliedConstraints.put(NULLABLE_CONSTRAINT, nc);
        }

        nc.setParameter(Boolean.valueOf(nullable));
    }

    /**
     * @return Returns the propertyName.
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * @param propertyName The propertyName to set.
     */
    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    /**
     * @return Returns the url.
     */
    public boolean isUrl() {
        if (isNotValidStringType()) {
            throw new MissingPropertyException("URL constraint can only be applied to a String property",
                    URL_CONSTRAINT, owningClass);
        }
        return appliedConstraints.containsKey(URL_CONSTRAINT);
    }

    /**
     * @param url The url to set.
     */
    public void setUrl(boolean url) {
        if (isNotValidStringType()) {
            throw new MissingPropertyException("URL constraint can only be applied to a String property",URL_CONSTRAINT,owningClass);
        }

        Constraint c = appliedConstraints.get(URL_CONSTRAINT);
        if (url) {
            if (c != null) {
                c.setParameter(Boolean.valueOf(url));
            }
            else {
                c = new UrlConstraint();
                c.setOwningClass(owningClass);
                c.setPropertyName(propertyName);
                c.setParameter(Boolean.valueOf(url));
                appliedConstraints.put(URL_CONSTRAINT, c);
            }
        }
        else {
            if (c != null) {
                appliedConstraints.remove(URL_CONSTRAINT);
            }
        }
    }

    /**
     * @return Returns the display.
     */
    public boolean isDisplay() {
        return display;
    }

    /**
     * @param display The display to set.
     */
    public void setDisplay(boolean display) {
        this.display = display;
    }

    /**
     * @return Returns the editable.
     */
    public boolean isEditable() {
        return editable;
    }

    /**
     * @param editable The editable to set.
     */
    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    /**
     * @return Returns the order.
     */
    public int getOrder() {
        return order;
    }

    /**
     * @param order The order to set.
     */
    public void setOrder(int order) {
        this.order = order;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public boolean isPassword() {
        return password;
    }

    public void setPassword(boolean password) {
        this.password = password;
    }

    @SuppressWarnings("rawtypes")
    public Map getAttributes() {
        return attributes;
    }
    @SuppressWarnings("rawtypes")
    public void setAttributes(Map attributes) {
        this.attributes = attributes;
    }

    public String getWidget() {
        return widget;
    }

    public void setWidget(String widget) {
        this.widget = widget;
    }

    /**
     * The message source used to evaluate error messages
     * @param source The MessageSource instance to use to resolve messages
     */
    public void setMessageSource(MessageSource source) {
        messageSource = source;
    }

    /**
     * Validate this constrainted property against specified property value
     *
     * @param target The target object to validate
     * @param propertyValue The value of the property to validate
     * @param errors The Errors instances to report errors to
     */
    public void validate(Object target, Object propertyValue, Errors errors) {
        List<Constraint> delayedConstraints = new ArrayList<Constraint>();

        // validate only vetoing constraints first, putting non-vetoing into delayedConstraints
        for (Constraint c : appliedConstraints.values()) {
            if (c instanceof VetoingConstraint) {
                c.setMessageSource(messageSource);
                // stop validation process when constraint vetoes
                if (((VetoingConstraint)c).validateWithVetoing(target, propertyValue, errors)) {
                    return;
                }
            }
            else {
                delayedConstraints.add(c);
            }
        }

        // process non-vetoing constraints
        for (Constraint c : delayedConstraints) {
            c.setMessageSource(messageSource);
            c.validate(target, propertyValue, errors);
        }
    }

    /**
     * Checks with this ConstraintedProperty instance supports applying the specified constraint.
     *
     * @param constraintName The name of the constraint
     * @return True if the constraint is supported
     */
    public boolean supportsContraint(String constraintName) {

        if (!constraints.containsKey(constraintName)) {
            return bean.isWritableProperty(constraintName);
        }

        try {
            Constraint c = instantiateConstraint(constraintName);
            return c.supports(propertyType);
        }
        catch (Exception e) {
            LOG.error("Exception thrown instantiating constraint [" + constraintName +
                    "] to class [" + owningClass + "]", e);
            throw new ConstraintException("Exception thrown instantiating  constraint [" + constraintName +
                    "] to class [" + owningClass + "]");
        }
    }

    /**
     * Applies a constraint for the specified name and consraint value.
     *
     * @param constraintName The name of the constraint
     * @param constrainingValue The constraining value
     *
     * @throws ConstraintException Thrown when the specified constraint is not supported by this ConstrainedProperty. Use <code>supportsContraint(String constraintName)</code> to check before calling
     */
    public void applyConstraint(String constraintName, Object constrainingValue) {

        if (constraints.containsKey(constraintName)) {
            if (constrainingValue == null) {
                appliedConstraints.remove(constraintName);
            }
            else {
                try {
                    Constraint c = instantiateConstraint(constraintName);

                    c.setOwningClass(owningClass);
                    c.setPropertyName(propertyName);
                    c.setParameter(constrainingValue);
                    appliedConstraints.put(constraintName, c);
                }
                catch (Exception e) {
                    LOG.error("Exception thrown applying constraint [" + constraintName +
                            "] to class [" + owningClass + "] for value [" + constrainingValue + "]: " + e.getMessage(), e);
                    throw new ConstraintException("Exception thrown applying constraint [" + constraintName +
                            "] to class [" + owningClass + "] for value [" + constrainingValue + "]: " + e.getMessage(), e);
                }
            }
        }
        else if (bean.isWritableProperty(constraintName)) {
            bean.setPropertyValue(constraintName, constrainingValue);
        }
        else {
            throw new ConstraintException("Constraint [" + constraintName + "] is not supported for property [" +
                    propertyName + "] of class [" + owningClass + "] with type [" + propertyType + "]");
        }
    }

    private Constraint instantiateConstraint(String constraintName) throws InstantiationException, IllegalAccessException {
        Object constraintFactory = constraints.get(constraintName);
        if (constraintFactory instanceof ConstraintFactory) {
            return ((ConstraintFactory)constraintFactory).newInstance();
        }

        return (Constraint)((Class<?>)constraintFactory).newInstance();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return new ToStringBuilder(this)
                   .append(owningClass)
                   .append(propertyName)
                   .append(propertyType)
                   .append(appliedConstraints)
                   .toString();
    }

    /**
     * Adds a meta constraints which is a non-validating informational constraint.
     *
     * @param name The name of the constraint
     * @param value The value
     */
    public void addMetaConstraint(String name, Object value) {
        metaConstraints.put(name, value);
    }

    /**
     * Obtains the value of the named meta constraint.
     * @param name The name of the constraint
     * @return The value
     */
    public Object getMetaConstraintValue(String name) {
        return metaConstraints.get(name);
    }
}
