package org.codehaus.groovy.grails.test;

public class PotentialTest {
    private String classPattern;
    private String methodName;
    private String filePattern;

    public PotentialTest(String testPattern) {
        if (containsMethodName(testPattern)) {
            // Filter out the method name
            int pos = testPattern.lastIndexOf('.');
            this.methodName = testPattern.substring(pos + 1);
            this.classPattern = testPattern.substring(0, pos);
        }
        else {
            this.classPattern = testPattern;
        }

        this.filePattern = classPatternToFilePattern(this.classPattern);
    }

    public PotentialTest(String classPattern, String methodName) {
        this.classPattern = classPattern;
        this.methodName = methodName;
        this.filePattern = classPatternToFilePattern(classPattern);
    }

    public String getClassPattern() {
        return classPattern;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getFilePattern() {
        return filePattern;
    }

    boolean hasMethodName() {
        return this.methodName != null;
    }

    String classPatternToFilePattern(String classPattern) {
        if (classPattern.indexOf('.') != -1) {
            return classPattern.replace('.', '/');
        }
        else {
            // Allow the test class to be in any package.
            return "**/" + classPattern;
        }
    }

    boolean containsMethodName(String testPattern) {
        // Probably we should check for method names starting with "test"
        return Character.isLowerCase(testPattern.charAt(testPattern.lastIndexOf('.') + 1));
    }
}
