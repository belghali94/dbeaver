/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jkiss.dbeaver.model.sql.format.tokenized;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPKeywordType;
import org.jkiss.dbeaver.model.sql.format.SQLFormatterConfiguration;
import org.jkiss.dbeaver.model.text.parser.rules.NumberRule;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * SQLTokensParser
 * TODO: check comment characters from syntax manager, not constants
 */
class SQLTokensParser {

    private static final String[] twoCharacterSymbol = { "<>", "<=", ">=", "||", "()", "!=", ":=", ".*" };

    private final SQLFormatterConfiguration configuration;
    private final String[][] quoteStrings;
    private final char escapeChar;
    private String fBefore;
    private int fPos;
    private char structSeparator;
    private String catalogSeparator;
    private Set<String> commands = new HashSet<>();
    private String[] singleLineComments;
    private char[] singleLineCommentStart;

    public SQLTokensParser(SQLFormatterConfiguration configuration) {
        this.configuration = configuration;
        this.escapeChar = configuration.getSyntaxManager().getEscapeChar();
        this.structSeparator = configuration.getSyntaxManager().getStructSeparator();
        this.catalogSeparator = configuration.getSyntaxManager().getCatalogSeparator();
        this.quoteStrings = configuration.getSyntaxManager().getIdentifierQuoteStrings();
        this.singleLineComments = configuration.getSyntaxManager().getDialect().getSingleLineComments();
        this.singleLineCommentStart = new char[this.singleLineComments.length];
        for (int i = 0; i < singleLineComments.length; i++) {
            if (singleLineComments[i].isEmpty()) singleLineCommentStart[i] = 0;
            else singleLineCommentStart[i] = singleLineComments[i].charAt(0);
        }

        String delimiterRedefiner = configuration.getSyntaxManager().getDialect().getScriptDelimiterRedefiner();
        if(ArrayUtils.contains(configuration.getSyntaxManager().getDialect().getScriptDelimiters(), delimiterRedefiner)) {
            delimiterRedefiner = null;
        }
        if (!CommonUtils.isEmpty(delimiterRedefiner)) {
            commands.add(delimiterRedefiner.toUpperCase(Locale.ENGLISH));
        }
    }

    public static boolean isSpace(final char argChar) {
        return Character.isWhitespace(argChar);
    }

    public static boolean isLetter(final char argChar) {
        return !isSpace(argChar) && !isDigit(argChar) && !isSymbol(argChar);
    }

    public static boolean isDigit(final char argChar) {
        return Character.isDigit(argChar);
    }

    public static boolean isSymbol(final char argChar) {
        switch (argChar) {
        case '"': // double quote
        case '?': // question mark
        case '%': // percent
        case '&': // ampersand
        case '\'': // quote
        case '(': // left paren
        case ')': // right paren
        case '|': // vertical bar
        case '*': // asterisk
        case '+': // plus sign
        case ',': // comma
        case '-': // minus sign
        case '.': // period
        case '/': // solidus
        case ':': // colon
        case ';': // semicolon
        case '<': // less than operator
        case '=': // equals operator
        case '>': // greater than operator
        case '!': // greater than operator
        case '~': // greater than operator
        case '`': // apos
        case '[': // bracket open
        case ']': // bracket close
        case '#': //
            return true;
        default:
            return false;
        }
    }

    FormatterToken nextToken() {
        int start_pos = fPos;
        if (fPos >= fBefore.length()) {
            fPos++;
            return new FormatterToken(TokenType.END, "", start_pos);
        }

        char fChar = fBefore.charAt(fPos);

        if (isSpace(fChar)) {
            StringBuilder workString = new StringBuilder();
            for (; fPos < fBefore.length(); fPos++) {
                fChar = fBefore.charAt(fPos);
                if (!isSpace(fChar)) {
                    break;
                }
                workString.append(fChar);
            }
            return new FormatterToken(TokenType.SPACE, workString.toString(), start_pos);
        } else if (fChar == ';') {
            fPos++;
            return new FormatterToken(TokenType.SYMBOL, ";", start_pos);
        } else if (isDigit(fChar)) {
            int startPosition = fPos;
            StringBuilder s = new StringBuilder();
            int radix = NumberRule.RADIX_DECIMAL;
            while (CommonUtils.isDigit(fChar, radix) || (radix == NumberRule.RADIX_DECIMAL && (fChar == '.' || fChar == 'e' || fChar == 'E'))) {
                s.append(fChar);
                fPos++;

                if (fPos >= fBefore.length()) {
                    break;
                }

                if (fChar == '0' && fPos + 1 < fBefore.length()) {
                    fChar = fBefore.charAt(fPos);
                    if (fChar == 'x' || fChar == 'X') {
                        radix = NumberRule.RADIX_HEXADECIMAL;
                        s.append(fChar);
                        fPos++;
                    }
                }

                fChar = fBefore.charAt(fPos);
            }
            if (isLetter(fChar) 
                && configuration.getSyntaxManager().getDialect().validIdentifierStart(fBefore.charAt(startPosition))
            ) {
                return parseNameStartWithDigit(startPosition);
            }
            return new FormatterToken(TokenType.VALUE, s.toString(), start_pos);
        }
        // single line comment
        else if (ArrayUtils.contains(singleLineCommentStart, fChar)) {
            fPos++;
            String commentString = null;
            for (String slc : singleLineComments) {
                if (fBefore.length() >= start_pos + slc.length() && slc.equals(fBefore.substring(start_pos, start_pos + slc.length()))) {
                    commentString = slc;
                    break;
                }
            }
            if (commentString == null) {
                return new FormatterToken(TokenType.SYMBOL, String.valueOf(fChar), start_pos);
            }
            fPos += commentString.length() - 1;
            while (fPos < fBefore.length()) {
                fPos++;
                if (fBefore.substring(fPos).startsWith(System.lineSeparator())) {
                    break;
                }
            }
            commentString = fBefore.substring(start_pos, fPos);
            return new FormatterToken(TokenType.COMMENT, commentString, start_pos);
        }
        else if (isLetter(fChar)) {
            StringBuilder s = new StringBuilder();
            fPos = readWord(s, fPos);
            String word = s.toString();
            if (commands.contains(word.toUpperCase(Locale.ENGLISH))) {
                s.setLength(0);
                for (; fPos < fBefore.length(); fPos++) {
                    fChar = fBefore.charAt(fPos);
                    if (fChar == '\n' || fChar == '\r') {
                        break;
                    } else {
                        s.append(fChar);
                    }
                }
                return new FormatterToken(TokenType.COMMAND, word + s.toString(), start_pos);
            }
            if (configuration.getSyntaxManager().getDialect().getKeywordType(word) == DBPKeywordType.KEYWORD) {
                return new FormatterToken(TokenType.KEYWORD, word, start_pos);
            }
            return new FormatterToken(TokenType.NAME, word, start_pos);
        }
        else if (fChar == '/') {
            fPos++;
            char ch2 = fBefore.charAt(fPos);
            if (ch2 != '*') {
                return new FormatterToken(TokenType.SYMBOL, "/", start_pos);
            }

            StringBuilder s = new StringBuilder("/*");
            fPos++;
            for (;;) {
                int ch0 = fChar;
                fChar = fBefore.charAt(fPos);
                s.append(fChar);
                fPos++;
                if (ch0 == '*' && fChar == '/') {
                    return new FormatterToken(TokenType.COMMENT, s.toString(), start_pos);
                }
            }
        } else {
            if (fChar == '\'' || isQuoteChar(fChar)) {
                fPos++;
                char endQuoteChar = fChar;
                // Close quote char may differ
                if (quoteStrings != null) {
                    for (String[] quoteString : quoteStrings) {
                        if (quoteString[0].charAt(0) == endQuoteChar) {
                            endQuoteChar = quoteString[1].charAt(0);
                            break;
                        }
                    }
                }

                StringBuilder s = new StringBuilder();
                s.append(fChar);
                int posMark = fPos;
                while (fPos < fBefore.length()) {
                    fChar = fBefore.charAt(fPos);
                    s.append(fChar);
                    fPos++;
                    char fNextChar = fPos >= fBefore.length() - 1 ? 0 : fBefore.charAt(fPos);
                    boolean isDoubledQuote = fChar == endQuoteChar && fNextChar == endQuoteChar;
                    boolean isEscapedQuote = fChar == escapeChar && fNextChar == endQuoteChar;
                    if (isDoubledQuote || isEscapedQuote) {
                        // Escaped quote
                        s.append(fNextChar);
                        fPos++;
                        continue;
                    }
                    if (fChar == endQuoteChar) {
                        return new FormatterToken(TokenType.VALUE, s.toString(), start_pos);
                    }
                }
                // Bad quoting (no close quote)
                fPos = posMark;
                return new FormatterToken(TokenType.SYMBOL, String.valueOf(s.charAt(0)), start_pos);
            }

            else if (isSymbol(fChar)) {
                String s = String.valueOf(fChar);
                fPos++;
                if (fPos >= fBefore.length()) {
                    return new FormatterToken(TokenType.SYMBOL, s, start_pos);
                }
                char ch2 = fBefore.charAt(fPos);
                for (int i = 0; i < twoCharacterSymbol.length; i++) {
                    if (twoCharacterSymbol[i].charAt(0) == fChar && twoCharacterSymbol[i].charAt(1) == ch2) {
                        fPos++;
                        s += ch2;
                        break;
                    }
                }
                return new FormatterToken(TokenType.SYMBOL, s, start_pos);
            } else {
                fPos++;
                return new FormatterToken(TokenType.UNKNOWN, String.valueOf(fChar), start_pos);
            }
        }
    }
    
    @NotNull
    private FormatterToken parseNameStartWithDigit(int startPosition) {
        StringBuilder s = new StringBuilder();
        fPos = readWord(s, startPosition);
        String word = s.toString();
        return new FormatterToken(TokenType.NAME, word, startPosition);
    }

    private int readWord(@NotNull StringBuilder s, int startPosition) {
        char firstChar = fBefore.charAt(startPosition);
        int position = startPosition;
        while (isLetter(firstChar) || isDigit(firstChar)
            || (firstChar == '*' && position > 0 && fBefore.charAt(position - 1) == structSeparator)
            || structSeparator == firstChar || catalogSeparator.indexOf(firstChar) != -1
        ) {
            s.append(firstChar);
            position++;
            if (position >= fBefore.length()) {
                break;
            }
            firstChar = fBefore.charAt(position);
        }
        return position;
    }

    private boolean isQuoteChar(char fChar) {
        if (quoteStrings != null) {
            for (int i = 0; i < quoteStrings.length; i++) {
                if (quoteStrings[i][0].charAt(0) == fChar) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<FormatterToken> parse(final String argSql) {
        fPos = 0;
        fBefore = argSql;

        final List<FormatterToken> list = new ArrayList<>();
        for (;;) {
            final FormatterToken token = nextToken();
            if (token.getType() == TokenType.END) {
                break;
            }

            list.add(token);
        }
        return list;
    }
}
