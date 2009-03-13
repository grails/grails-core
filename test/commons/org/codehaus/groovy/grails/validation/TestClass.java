package org.codehaus.groovy.grails.validation;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;

/**
 * Simple test class for testing constraints.
 *
 * @author Sergey Nebolsin (<a href="mailto:nebolsin@gmail.com"/>)
 */
public class TestClass {
    private Object[] testArray;
    private BigDecimal testBigDecimal;
    private Collection testCollection;
    private String testString;
    private Date testDate;
    private Double testDouble;
    private String testEmail;
    private Float testFloat;
    private Integer testInteger;
    private Long testLong;
    private String testURL;

    public Object[] getTestArray() {
        return testArray;
    }

    public void setTestArray(Object[] testArray) {
        this.testArray = testArray;
    }

    public BigDecimal getTestBigDecimal() {
        return testBigDecimal;
    }

    public void setTestBigDecimal(BigDecimal testBigDecimal) {
        this.testBigDecimal = testBigDecimal;
    }

    public Collection getTestCollection() {
        return testCollection;
    }

    public void setTestCollection(Collection testCollection) {
        this.testCollection = testCollection;
    }

    public Date getTestDate() {
        return testDate;
    }

    public void setTestDate(Date testDate) {
        this.testDate = testDate;
    }

    public Double getTestDouble() {
        return testDouble;
    }

    public void setTestDouble(Double testDouble) {
        this.testDouble = testDouble;
    }

    public String getTestEmail() {
        return testEmail;
    }

    public void setTestEmail(String testEmail) {
        this.testEmail = testEmail;
    }

    public Float getTestFloat() {
        return testFloat;
    }

    public void setTestFloat(Float testFloat) {
        this.testFloat = testFloat;
    }

    public Integer getTestInteger() {
        return testInteger;
    }

    public void setTestInteger(Integer testInteger) {
        this.testInteger = testInteger;
    }

    public String getTestURL() {
        return testURL;
    }

    public void setTestURL(String testURL) {
        this.testURL = testURL;
    }

    public String getTestString() {
        return testString;
    }

    public void setTestString( String testString ) {
        this.testString = testString;
    }

    public Long getTestLong() {
        return testLong;
    }

    public void setTestLong( Long testLong ) {
        this.testLong = testLong;
    }
}
