package org.codehaus.groovy.grails.web.pages;

import java.util.Stack;

/**
 * 
 * Parses an expression in a GSP.
 * 
 * Used by GroovyPageScanner and GroovyPageParser to search for the end of an expression.
 * 
 * @author Lari Hotari
 *
 */
class GroovyPageExpressionParser {
    private static enum ParsingState {
        NORMAL, EXPRESSION, QUOTEDVALUE_SINGLE, QUOTEDVALUE_DOUBLE, TRIPLEQUOTED_SINGLE, TRIPLEQUOTED_DOUBLE;
    }
    
    /**
     * Finds the ending position of an expression.
     * 
     * 
     * @param scriptTokens the source script
     * @param startPos starting position
     * @param terminationChar the termination character
     * @param nextTerminationChar optional 2nd termination character (0 if none)
     * @param startInExpression starts in EXPRESSION state
     * @return
     */
    static int findExpressionEndPos(String scriptTokens, int startPos, char terminationChar, char nextTerminationChar, boolean startInExpression) {
        int currentPos = startPos;
        int terminationCharPos = -1;
        char previousChar = 0;
        char previousPreviousChar = 0;
        int relativeCharIndex=0;
        Stack<ParsingState> parsingStateStack = new Stack<ParsingState>();
        if(startInExpression) {
            parsingStateStack.push(ParsingState.EXPRESSION);
        } else {
            parsingStateStack.push(ParsingState.NORMAL);
        } 
        
        while(currentPos < scriptTokens.length() && terminationCharPos==-1) {
            ParsingState parsingState = parsingStateStack.peek();
            char ch = scriptTokens.charAt(currentPos++);
            char nextChar = (currentPos < scriptTokens.length()) ? scriptTokens.charAt(currentPos) : 0; 
            
            if (parsingStateStack.size()==1 && ch==terminationChar && (nextTerminationChar==0 || nextTerminationChar==nextChar)) {
                terminationCharPos = currentPos-1;
            } else if (parsingState==ParsingState.EXPRESSION || parsingState==ParsingState.NORMAL) {
                switch(ch) {
                    case '{':
                        if (previousChar=='$' && parsingState==ParsingState.EXPRESSION) {
                            // invalid expression, starting new ${} expression inside expression
                            return -1;
                        }
                        if (previousChar=='$' || parsingState==ParsingState.EXPRESSION) {
                            parsingStateStack.push(ParsingState.EXPRESSION);
                        }
                        break;
                    case '[':
                        if (relativeCharIndex==0 || parsingState==ParsingState.EXPRESSION) {
                            parsingStateStack.push(ParsingState.EXPRESSION);
                        }
                        break;
                    case '}':
                    case ']':
                        if (parsingState==ParsingState.EXPRESSION) {
                            parsingStateStack.pop();
                        }
                        break;
                    case '\'':
                    case '"':
                        if (parsingState==ParsingState.EXPRESSION) {
                            if(nextChar != ch && previousChar != ch) {
                                parsingStateStack.push(ch=='"' ? ParsingState.QUOTEDVALUE_DOUBLE : ParsingState.QUOTEDVALUE_SINGLE);
                            } else if (previousChar==ch && previousPreviousChar==ch) {
                                parsingStateStack.push(ch=='"' ? ParsingState.TRIPLEQUOTED_DOUBLE : ParsingState.TRIPLEQUOTED_SINGLE);
                            }
                        }
                        break;
                }
            } else if (ch=='"' || ch=='\'') {
                if (nextChar != ch && (previousChar != ch || previousPreviousChar=='\\') && (previousChar != '\\' || (previousChar=='\\' && previousPreviousChar=='\\'))
                        && ((parsingState == ParsingState.QUOTEDVALUE_DOUBLE && ch == '"') || (parsingState == ParsingState.QUOTEDVALUE_SINGLE && ch == '\''))) {
                    parsingStateStack.pop();
                }
                else if ((previousChar == ch && previousPreviousChar == ch)
                        && ((parsingState == ParsingState.TRIPLEQUOTED_DOUBLE && ch == '"') || (parsingState == ParsingState.TRIPLEQUOTED_SINGLE && ch == '\''))) {
                    parsingStateStack.pop();
                }
            }
            previousPreviousChar = previousChar;
            previousChar=ch;
            relativeCharIndex++;
        }
        return terminationCharPos;
    }
}
