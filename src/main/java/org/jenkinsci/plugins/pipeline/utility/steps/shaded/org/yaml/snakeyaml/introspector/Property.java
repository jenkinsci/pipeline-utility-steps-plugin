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
package org.jenkinsci.plugins.pipeline.utility.steps.shaded.org.yaml.snakeyaml.introspector;

/**
 * <p>
 * A <code>Property</code> represents a single member variable of a class,
 * possibly including its accessor methods (getX, setX). The name stored in this
 * class is the actual name of the property as given for the class, not an
 * alias.
 * </p>
 * 
 * <p>
 * Objects of this class have a total ordering which defaults to ordering based
 * on the name of the property.
 * </p>
 */
public abstract class Property implements Comparable<Property> {

    private final String name;
    private final Class<?> type;

    public Property(String name, Class<?> type) {
        this.name = name;
        this.type = type;
    }

    public Class<?> getType() {
        return type;
    }

    abstract public Class<?>[] getActualTypeArguments();

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getName() + " of " + getType();
    }

    public int compareTo(Property o) {
        return name.compareTo(o.name);
    }

    public boolean isWritable() {
        return true;
    }

    public boolean isReadable() {
        return true;
    }

    abstract public void set(Object object, Object value) throws Exception;

    abstract public Object get(Object object);

    @Override
    public int hashCode() {
        return name.hashCode() + type.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Property) {
            Property p = (Property) other;
            return name.equals(p.getName()) && type.equals(p.getType());
        }
        return false;
    }
}