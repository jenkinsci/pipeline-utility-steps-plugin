/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 CloudBees Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.jenkinsci.plugins.pipeline.utility.steps.conf.mf;

import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Simplified and {@link Serializable}, read only version of {@link java.util.jar.Manifest}.
 *
 * @author Robert Sandell &lt;rsandell@cloudbees.com&gt;.
 */
public class SimpleManifest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Map<String, String> main;
    private Map<String, Map<String, String>> entries;

    protected SimpleManifest(Map<String, String> main, Map<String, Map<String, String>> entries) {
        this.main = Collections.unmodifiableMap(main);
        this.entries = Collections.unmodifiableMap(entries);
    }

    protected SimpleManifest(Manifest mf) {
        this(extractMainAttributes(mf), extractEntries(mf));
    }

    protected static Map<String, Map<String, String>> extractEntries(Manifest mf) {
        Map<String, Map<String, String>> mapMap = new HashMap<>();
        Map<String, Attributes> entries = mf.getEntries();
        for (Map.Entry<String, Attributes> entrySection : entries.entrySet()) {
            Map<String, String> map = new HashMap<>();
            for (Map.Entry<Object, Object> entry : entrySection.getValue().entrySet()) {
                map.put(entry.getKey().toString(), entry.getValue().toString());
            }
            mapMap.put(entrySection.getKey(), map);
        }
        return mapMap;
    }

    protected static Map<String, String> extractMainAttributes(Manifest mf) {
        Map<String, String> map = new HashMap<>();
        Attributes attributes = mf.getMainAttributes();
        for (Map.Entry<Object, Object> entry : attributes.entrySet()) {
            map.put(entry.getKey().toString(), entry.getValue().toString());
        }
        return map;
    }

    /**
     * Returns the main Attributes for the Manifest.
     *
     * @return the main attributes
     * @see Manifest#getMainAttributes()
     */
    @Whitelisted
    public Map<String, String> getMain() {
        return main;
    }

    /**
     * Returns a Map of the entries contained in this Manifest. Each entry
     * is represented by a String name (key) and associated attributes (value) as a Map of Strings.
     *
     * @return the other manifest entries
     */
    @Whitelisted
    public Map<String, Map<String, String>> getEntries() {
        return entries;
    }

}
