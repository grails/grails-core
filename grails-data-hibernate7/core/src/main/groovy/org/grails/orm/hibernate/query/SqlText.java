/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.grails.orm.hibernate.query;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lexical reading of SQL or HQL text, so that every scanner of this package skips the same things the same way:
 * string literals and quoted identifiers, where a doubled quote stands for the quote itself; in native SQL also
 * {@code [bracket identifiers]}, where {@code ]]} stands for a bracket, and {@code $tag$...$tag$} strings; line and
 * block comments; and parentheses, when a top-level position is asked for. The text is read once, when the instance
 * is created: each character is classified as plain text, quoted text or comment, and each plain character carries
 * its parenthesis depth, so every question asked afterwards walks those two arrays and never rescans the text. The
 * text is scanned, never parsed: this answers where something is, not what it means.
 *
 * @since 8.0.1
 */
final class SqlText {

    /** What a text may hold besides string literals, quoted identifiers and comments. */
    enum Dialect {
        /** HQL, which has neither bracket identifiers nor dollar-quoted strings. */
        HQL,
        /** Native SQL, with {@code [bracket identifiers]} and {@code $tag$...$tag$} strings. */
        SQL
    }

    private static final byte PLAIN = 0;
    private static final byte QUOTED = 1;
    private static final byte BLOCK_COMMENT = 2;
    private static final byte LINE_COMMENT = 3;

    private static final Pattern DOLLAR_QUOTE_TAG = Pattern.compile("\\$(?:[A-Za-z_][A-Za-z0-9_]*)?\\$");

    private final String text;
    private final Dialect dialect;
    /** The kind of each character: plain text, inside a literal or quoted identifier, or inside a comment. */
    private final byte[] kind;
    /**
     * The parenthesis depth of each plain character: the depth a {@code (} opens and a {@code )} closes, so the two
     * parentheses of a pair carry the same depth, and the depth enclosing any other character.
     */
    private final int[] depth;

    private SqlText(String text, Dialect dialect) {
        this.text = text;
        this.dialect = dialect;
        int length = text.length();
        this.kind = new byte[length];
        this.depth = new int[length];
        int d = 0;
        int i = 0;
        while (i < length) {
            int after = unitEnd(i);
            if (after > i) {
                byte unitKind = unitKind(i);
                for (int j = i; j < after; j++) {
                    kind[j] = unitKind;
                }
                i = after;
                continue;
            }
            char c = text.charAt(i);
            if (c == '(') {
                d++;
                depth[i] = d;
            } else if (c == ')') {
                depth[i] = d;
                if (d > 0) {
                    d--;
                }
            } else {
                depth[i] = d;
            }
            i++;
        }
    }

    static SqlText hql(String text) {
        return new SqlText(text, Dialect.HQL);
    }

    static SqlText sql(String text) {
        return new SqlText(text, Dialect.SQL);
    }

    String text() {
        return text;
    }

    int length() {
        return text.length();
    }

    /** Returns whether the character at {@code index} is plain text: outside every literal, identifier and comment. */
    boolean isPlain(int index) {
        return kind[index] == PLAIN;
    }

    /**
     * Returns the index of {@code keyword} at or after {@code fromIndex}, outside string literals, quoted identifiers,
     * comments and parentheses, where it starts a word: {@code select} in {@code preselect} or in {@code b.select} is
     * part of a name. A keyword that starts with a space starts a word. The keyword is matched as written, so a
     * case-insensitive search passes the text in lower case.
     *
     * @return the index, or -1 when the keyword is not at the top level of the text
     */
    int indexOfTopLevel(String keyword, int fromIndex) {
        for (int i = Math.max(fromIndex, 0); i < text.length(); i++) {
            if (kind[i] == PLAIN && depth[i] == 0 && text.startsWith(keyword, i) && startsWord(i)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Splits the text at the separators outside string literals, quoted identifiers, comments and parentheses, and
     * trims each part. A text without the separator is one part.
     */
    List<String> splitTopLevel(char separator) {
        List<String> parts = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            if (isTopLevel(i, separator)) {
                parts.add(text.substring(start, i).trim());
                start = i + 1;
            }
        }
        parts.add(text.substring(start).trim());
        return parts;
    }

    /** Returns how many times {@code separator} occurs at the top level of the whole text. */
    int countTopLevel(char separator) {
        return countTopLevel(separator, 0, text.length());
    }

    /** Returns how many times {@code separator} occurs at the top level between {@code from} and {@code to}. */
    int countTopLevel(char separator, int from, int to) {
        int count = 0;
        for (int i = Math.max(from, 0); i < Math.min(to, text.length()); i++) {
            if (isTopLevel(i, separator)) {
                count++;
            }
        }
        return count;
    }

    /** Returns the indexes of {@code c} outside string literals, quoted identifiers and comments, at any depth. */
    List<Integer> plainIndexes(char c) {
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < text.length(); i++) {
            if (kind[i] == PLAIN && text.charAt(i) == c) {
                indexes.add(i);
            }
        }
        return indexes;
    }

    /**
     * Returns the text without parentheses that enclose all of it, however many times they are nested, and without
     * the whitespace around it. A text whose first parenthesis closes before its end, such as the parenthesized
     * branches of a union, is returned as it is, trimmed.
     */
    String unwrapOuterParentheses() {
        int start = 0;
        int end = text.length() - 1;
        while (true) {
            while (start <= end && Character.isWhitespace(text.charAt(start))) {
                start++;
            }
            while (end >= start && Character.isWhitespace(text.charAt(end))) {
                end--;
            }
            if (start > end || text.charAt(start) != '(' || kind[start] != PLAIN || closingParenthesisOf(start) != end) {
                break;
            }
            start++;
            end--;
        }
        return start > end ? "" : text.substring(start, end + 1);
    }

    /**
     * Returns the index of the parenthesis closing the one at {@code open}, outside string literals, quoted
     * identifiers and comments, or -1 when it is not closed.
     */
    int closingParenthesisOf(int open) {
        int target = depth[open];
        for (int i = open + 1; i < text.length(); i++) {
            if (kind[i] == PLAIN && text.charAt(i) == ')' && depth[i] == target) {
                return i;
            }
        }
        return -1;
    }

    /** Returns the text without the comments and whitespace it ends with. */
    String withoutTrailingComments() {
        int end = text.length();
        while (end > 0 && (kind[end - 1] >= BLOCK_COMMENT || (kind[end - 1] == PLAIN && Character.isWhitespace(text.charAt(end - 1))))) {
            end--;
        }
        return text.substring(0, end);
    }

    /** Returns whether the text ends in a line comment, which would comment out anything appended on the same line. */
    boolean endsInLineComment() {
        return text.length() > 0 && kind[text.length() - 1] == LINE_COMMENT;
    }

    private boolean isTopLevel(int index, char c) {
        return kind[index] == PLAIN && depth[index] == 0 && text.charAt(index) == c;
    }

    private boolean startsWord(int index) {
        if (index == 0 || text.charAt(index) == ' ') {
            return true;
        }
        char previous = text.charAt(index - 1);
        return !Character.isJavaIdentifierPart(previous) && previous != '.';
    }

    /**
     * Returns the index after the string literal, quoted identifier or comment that starts at {@code index}, or
     * {@code index} itself when none starts there and the character is plain text.
     */
    private int unitEnd(int index) {
        char c = text.charAt(index);
        if (c == '\'' || c == '"' || c == '`') {
            return skipQuoted(index, c);
        }
        if (c == '-' && text.startsWith("--", index)) {
            return endOfLine(index);
        }
        if (c == '/' && text.startsWith("/*", index)) {
            int end = text.indexOf("*/", index + 2);
            return end == -1 ? text.length() : end + 2;
        }
        if (dialect == Dialect.SQL) {
            if (c == '[') {
                return skipBracketIdentifier(index);
            }
            if (c == '$') {
                return skipDollarQuoted(index);
            }
        }
        return index;
    }

    private byte unitKind(int index) {
        if (text.startsWith("--", index)) {
            return LINE_COMMENT;
        }
        if (text.startsWith("/*", index)) {
            return BLOCK_COMMENT;
        }
        return QUOTED;
    }

    /** Returns the index after the quoted text starting at {@code start}, where a doubled quote stands for the quote. */
    private int skipQuoted(int start, char quote) {
        int i = start + 1;
        while (i < text.length()) {
            if (text.charAt(i) == quote) {
                if (i + 1 < text.length() && text.charAt(i + 1) == quote) {
                    i += 2;
                    continue;
                }
                return i + 1;
            }
            i++;
        }
        return i;
    }

    /**
     * Returns the index after the bracket identifier starting at {@code start}. Inside it only brackets count: a
     * nested {@code [} opens one more level, as in an array subscript, and {@code ]]} at the outermost level stands
     * for a bracket, so {@code [box's width]}, {@code [total (cm)]} and {@code [a]]b]} are each one identifier.
     */
    private int skipBracketIdentifier(int start) {
        int brackets = 1;
        int i = start + 1;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '[') {
                brackets++;
            } else if (c == ']') {
                if (brackets == 1 && text.startsWith("]]", i)) {
                    i += 2;
                    continue;
                }
                brackets--;
                if (brackets == 0) {
                    return i + 1;
                }
            }
            i++;
        }
        return i;
    }

    /**
     * Returns the index after the dollar-quoted string starting at {@code start}, such as {@code $$a,b$$} or
     * {@code $tag$a,b$tag$}, or {@code start} itself when no string starts there, as in the identifier {@code a$b}
     * or the parameter {@code $1}.
     */
    private int skipDollarQuoted(int start) {
        char previous = start > 0 ? text.charAt(start - 1) : ' ';
        Matcher tag = DOLLAR_QUOTE_TAG.matcher(text).region(start, text.length());
        if (Character.isLetterOrDigit(previous) || previous == '_' || previous == '$' || !tag.lookingAt()) {
            return start;
        }
        int end = text.indexOf(tag.group(), tag.end());
        return end == -1 ? text.length() : end + tag.group().length();
    }

    private int endOfLine(int start) {
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n' || c == '\r') {
                return i;
            }
        }
        return text.length();
    }
}
