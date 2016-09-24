/**
 * Copyright (c) 2008, http://www.snakeyaml.org
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
package org.jenkinsci.plugins.pipeline.utility.steps.shaded.org.yaml.snakeyaml.reader;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jenkinsci.plugins.pipeline.utility.steps.shaded.org.yaml.snakeyaml.error.Mark;
import org.jenkinsci.plugins.pipeline.utility.steps.shaded.org.yaml.snakeyaml.error.YAMLException;
import org.jenkinsci.plugins.pipeline.utility.steps.shaded.org.yaml.snakeyaml.reader.ReaderException;
import org.jenkinsci.plugins.pipeline.utility.steps.shaded.org.yaml.snakeyaml.reader.UnicodeReader;
import org.jenkinsci.plugins.pipeline.utility.steps.shaded.org.yaml.snakeyaml.scanner.Constant;

/**
 * Reader: checks if characters are in allowed range, adds '\0' to the end.
 */
public class StreamReader {
    public final static Pattern NON_PRINTABLE = Pattern
            .compile("[^\t\n\r\u0020-\u007E\u0085\u00A0-\uD7FF\uE000-\uFFFD]");
    private String name;
    private final Reader stream;
    private int pointer = 0;
    private boolean eof = true;
    private String buffer;
    private int index = 0;
    private int line = 0;
    private int column = 0;
    private char[] data;

    public StreamReader(String stream) {
        this.name = "'string'";
        this.buffer = ""; // to set length to 0
        checkPrintable(stream);
        this.buffer = stream + "\0";
        this.stream = null;
        this.eof = true;
        this.data = null;
    }

    public StreamReader(Reader reader) {
        this.name = "'reader'";
        this.buffer = "";
        this.stream = reader;
        this.eof = false;
        this.data = new char[1024];
        this.update();
    }

    void checkPrintable(CharSequence data) {
        Matcher em = NON_PRINTABLE.matcher(data);
        if (em.find()) {
            int position = this.index + this.buffer.length() - this.pointer + em.start();
            throw new ReaderException(name, position, em.group().charAt(0),
                    "special characters are not allowed");
        }
    }

    /**
     * Checks <code>chars</chars> for the non-printable characters.
     * 
     * @param chars
     *            the array where to search.
     * @param begin
     *            the beginning index, inclusive.
     * @param end
     *            the ending index, exclusive.
     * @throws ReaderException
     *             if <code>chars</code> contains non-printable character(s).
     */
    void checkPrintable(final char[] chars, final int begin, final int end) {
        for (int i = begin; i < end; i++) {
            final char c = chars[i];

            if (isPrintable(c)) {
                continue;
            }

            int position = this.index + this.buffer.length() - this.pointer + i;
            throw new ReaderException(name, position, c, "special characters are not allowed");
        }
    }

    public static boolean isPrintable(final char c) {
        return (c >= '\u0020' && c <= '\u007E') || c == '\n' || c == '\r' || c == '\t'
                || c == '\u0085' || (c >= '\u00A0' && c <= '\uD7FF')
                || (c >= '\uE000' && c <= '\uFFFD');
    }

    public Mark getMark() {
        return new Mark(name, this.index, this.line, this.column, this.buffer, this.pointer);
    }

    public void forward() {
        forward(1);
    }

    /**
     * read the next length characters and move the pointer.
     * 
     * @param length
     */
    public void forward(int length) {
        if (this.pointer + length + 1 >= this.buffer.length()) {
            update();
        }
        char ch = 0;
        for (int i = 0; i < length; i++) {
            ch = this.buffer.charAt(this.pointer);
            this.pointer++;
            this.index++;
            if (Constant.LINEBR.has(ch) || (ch == '\r' && buffer.charAt(pointer) != '\n')) {
                this.line++;
                this.column = 0;
            } else if (ch != '\uFEFF') {
                this.column++;
            }
        }
    }

    public char peek() {
        return this.buffer.charAt(this.pointer);
    }

    /**
     * Peek the next index-th character
     * 
     * @param index
     * @return the next index-th character
     */
    public char peek(int index) {
        if (this.pointer + index + 1 > this.buffer.length()) {
            update();
        }
        return this.buffer.charAt(this.pointer + index);
    }

    /**
     * peek the next length characters
     * 
     * @param length
     * @return the next length characters
     */
    public String prefix(int length) {
        if (this.pointer + length >= this.buffer.length()) {
            update();
        }
        if (this.pointer + length > this.buffer.length()) {
            return this.buffer.substring(this.pointer);
        }
        return this.buffer.substring(this.pointer, this.pointer + length);
    }

    /**
     * prefix(length) immediately followed by forward(length)
     */
    public String prefixForward(int length) {
        final String prefix = prefix(length);
        this.pointer += length;
        this.index += length;
        // prefix never contains new line characters
        this.column += length;
        return prefix;
    }

    private void update() {
        if (!this.eof) {
            this.buffer = buffer.substring(this.pointer);
            this.pointer = 0;
            try {
                int converted = this.stream.read(data);
                if (converted > 0) {
                    /*
                     * Let's create StringBuilder manually. Anyway str1 + str2
                     * generates new StringBuilder(str1).append(str2).toSting()
                     * Giving correct capacity to the constructor prevents
                     * unnecessary operations in appends.
                     */
                    checkPrintable(data, 0, converted);
                    this.buffer = new StringBuilder(buffer.length() + converted).append(buffer)
                            .append(data, 0, converted).toString();
                } else {
                    this.eof = true;
                    this.buffer += "\0";
                }
            } catch (IOException ioe) {
                throw new YAMLException(ioe);
            }
        }
    }

    public int getColumn() {
        return column;
    }

    public Charset getEncoding() {
        return Charset.forName(((UnicodeReader) this.stream).getEncoding());
    }

    public int getIndex() {
        return index;
    }

    public int getLine() {
        return line;
    }
}
