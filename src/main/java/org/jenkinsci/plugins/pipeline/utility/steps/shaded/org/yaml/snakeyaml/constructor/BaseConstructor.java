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
package org.jenkinsci.plugins.pipeline.utility.steps.shaded.org.yaml.snakeyaml.constructor;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jenkinsci.plugins.pipeline.utility.steps.shaded.org.yaml.snakeyaml.nodes.MappingNode;
import org.jenkinsci.plugins.pipeline.utility.steps.shaded.org.yaml.snakeyaml.composer.Composer;
import org.jenkinsci.plugins.pipeline.utility.steps.shaded.org.yaml.snakeyaml.composer.ComposerException;
import org.jenkinsci.plugins.pipeline.utility.steps.shaded.org.yaml.snakeyaml.error.YAMLException;
import org.jenkinsci.plugins.pipeline.utility.steps.shaded.org.yaml.snakeyaml.introspector.PropertyUtils;
import org.jenkinsci.plugins.pipeline.utility.steps.shaded.org.yaml.snakeyaml.nodes.Node;
import org.jenkinsci.plugins.pipeline.utility.steps.shaded.org.yaml.snakeyaml.nodes.NodeId;
import org.jenkinsci.plugins.pipeline.utility.steps.shaded.org.yaml.snakeyaml.nodes.NodeTuple;
import org.jenkinsci.plugins.pipeline.utility.steps.shaded.org.yaml.snakeyaml.nodes.ScalarNode;
import org.jenkinsci.plugins.pipeline.utility.steps.shaded.org.yaml.snakeyaml.nodes.SequenceNode;
import org.jenkinsci.plugins.pipeline.utility.steps.shaded.org.yaml.snakeyaml.nodes.Tag;

public abstract class BaseConstructor {
    /**
     * It maps the node kind to the the Construct implementation. When the
     * runtime class is known then the implicit tag is ignored.
     */
    protected final Map<NodeId, Construct> yamlClassConstructors = new EnumMap<>(
            NodeId.class);
    /**
     * It maps the (explicit or implicit) tag to the Construct implementation.
     * It is used:
     * 1) explicit tag - if present.
     * 2) implicit tag - when the runtime class of the instance is unknown (the
     * node has the Object.class)
     */
    protected final Map<Tag, Construct> yamlConstructors = new HashMap<>();
    /**
     * It maps the (explicit or implicit) tag to the Construct implementation.
     * It is used when no exact match found.
     */
    protected final Map<String, Construct> yamlMultiConstructors = new HashMap<>();

    protected Composer composer;
    private final Map<Node, Object> constructedObjects;
    private final Set<Node> recursiveObjects;
    private final ArrayList<RecursiveTuple<Map<Object, Object>, RecursiveTuple<Object, Object>>> maps2fill;
    private final ArrayList<RecursiveTuple<Set<Object>, Object>> sets2fill;

    protected Tag rootTag;
    private PropertyUtils propertyUtils;
    private boolean explicitPropertyUtils;

    public BaseConstructor() {
        constructedObjects = new HashMap<>();
        recursiveObjects = new HashSet<>();
        maps2fill = new ArrayList<>();
        sets2fill = new ArrayList<>();
        rootTag = null;
        explicitPropertyUtils = false;
    }

    public void setComposer(Composer composer) {
        this.composer = composer;
    }

    /**
     * Check if more documents available
     * 
     * @return true when there are more YAML documents in the stream
     */
    public boolean checkData() {
        // If there are more documents available?
        return composer.checkNode();
    }

    /**
     * Construct and return the next document
     * 
     * @return constructed instance
     */
    public Object getData() {
        // Construct and return the next document.
        composer.checkNode();
        Node node = composer.getNode();
        if (rootTag != null) {
            node.setTag(rootTag);
        }
        return constructDocument(node);
    }

    /**
     * Ensure that the stream contains a single document and construct it
     * 
     * @return constructed instance
     * @throws ComposerException
     *             in case there are more documents in the stream
     */
    public Object getSingleData(Class<?> type) {
        // Ensure that the stream contains a single document and construct it
        Node node = composer.getSingleNode();
        if (node != null) {
            if (Object.class != type) {
                node.setTag(new Tag(type));
            } else if (rootTag != null) {
                node.setTag(rootTag);
            }
            return constructDocument(node);
        }
        return null;
    }

    /**
     * Construct complete YAML document. Call the second step in case of
     * recursive structures. At the end cleans all the state.
     * 
     * @param node
     *            root Node
     * @return Java instance
     */
    protected final Object constructDocument(Node node) {
        Object data = constructObject(node);
        fillRecursive();
        constructedObjects.clear();
        recursiveObjects.clear();
        return data;
    }

    private void fillRecursive() {
        if (!maps2fill.isEmpty()) {
            for (RecursiveTuple<Map<Object, Object>, RecursiveTuple<Object, Object>> entry : maps2fill) {
                RecursiveTuple<Object, Object> key_value = entry._2();
                entry._1().put(key_value._1(), key_value._2());
            }
            maps2fill.clear();
        }
        if (!sets2fill.isEmpty()) {
            for (RecursiveTuple<Set<Object>, Object> value : sets2fill) {
                value._1().add(value._2());
            }
            sets2fill.clear();
        }
    }

    /**
     * Construct object from the specified Node. Return existing instance if the
     * node is already constructed.
     * 
     * @param node
     *            Node to be constructed
     * @return Java instance
     */
    protected Object constructObject(Node node) {
        if (constructedObjects.containsKey(node)) {
            return constructedObjects.get(node);
        }
        if (recursiveObjects.contains(node)) {
            throw new ConstructorException(null, null, "found unconstructable recursive node",
                    node.getStartMark());
        }
        recursiveObjects.add(node);
        Construct constructor = getConstructor(node);
        Object data = constructor.construct(node);
        constructedObjects.put(node, data);
        recursiveObjects.remove(node);
        if (node.isTwoStepsConstruction()) {
            constructor.construct2ndStep(node, data);
        }
        return data;
    }

    /**
     * Get the constructor to construct the Node. For implicit tags if the
     * runtime class is known a dedicated Construct implementation is used.
     * Otherwise the constructor is chosen by the tag.
     * 
     * @param node
     *            Node to be constructed
     * @return Construct implementation for the specified node
     */
    protected Construct getConstructor(Node node) {
        if (node.useClassConstructor()) {
            return yamlClassConstructors.get(node.getNodeId());
        } else {
            Construct constructor = yamlConstructors.get(node.getTag());
            if (constructor == null) {
                for (String prefix : yamlMultiConstructors.keySet()) {
                    if (node.getTag().startsWith(prefix)) {
                        return yamlMultiConstructors.get(prefix);
                    }
                }
                return yamlConstructors.get(null);
            }
            return constructor;
        }
    }

    protected Object constructScalar(ScalarNode node) {
        return node.getValue();
    }

    protected List<Object> createDefaultList(int initSize) {
        return new ArrayList<>(initSize);
    }

    protected Set<Object> createDefaultSet(int initSize) {
        return new LinkedHashSet<>(initSize);
    }

    protected Object createArray(Class<?> type, int size) {
        return Array.newInstance(type.getComponentType(), size);
    }

    @SuppressWarnings("unchecked")
    protected List<? extends Object> constructSequence(SequenceNode node) {
        List<Object> result;
        if (List.class.isAssignableFrom(node.getType()) && !node.getType().isInterface()) {
            // the root class may be defined (Vector for instance)
            try {
                result = (List<Object>) node.getType().newInstance();
            } catch (Exception e) {
                throw new YAMLException(e);
            }
        } else {
            result = createDefaultList(node.getValue().size());
        }
        constructSequenceStep2(node, result);
        return result;

    }

    @SuppressWarnings("unchecked")
    protected Set<? extends Object> constructSet(SequenceNode node) {
        Set<Object> result;
        if (!node.getType().isInterface()) {
            // the root class may be defined
            try {
                result = (Set<Object>) node.getType().newInstance();
            } catch (Exception e) {
                throw new YAMLException(e);
            }
        } else {
            result = createDefaultSet(node.getValue().size());
        }
        constructSequenceStep2(node, result);
        return result;

    }

    protected Object constructArray(SequenceNode node) {
        return constructArrayStep2(node, createArray(node.getType(), node.getValue().size()));
    }

    protected void constructSequenceStep2(SequenceNode node, Collection<Object> collection) {
        for (Node child : node.getValue()) {
            collection.add(constructObject(child));
        }
    }

    protected Object constructArrayStep2(SequenceNode node, Object array) {
        final Class<?> componentType = node.getType().getComponentType();

        int index = 0;
        for (Node child : node.getValue()) {
            // Handle multi-dimensional arrays...
            if (child.getType() == Object.class) {
                child.setType(componentType);
            }

            final Object value = constructObject(child);

            if (componentType.isPrimitive()) {
                // Null values are disallowed for primitives
                if (value == null) {
                    throw new NullPointerException("Unable to construct element value for " + child);
                }

                // Primitive arrays require quite a lot of work.
                if (byte.class.equals(componentType)) {
                    Array.setByte(array, index, ((Number) value).byteValue());

                } else if (short.class.equals(componentType)) {
                    Array.setShort(array, index, ((Number) value).shortValue());

                } else if (int.class.equals(componentType)) {
                    Array.setInt(array, index, ((Number) value).intValue());

                } else if (long.class.equals(componentType)) {
                    Array.setLong(array, index, ((Number) value).longValue());

                } else if (float.class.equals(componentType)) {
                    Array.setFloat(array, index, ((Number) value).floatValue());

                } else if (double.class.equals(componentType)) {
                    Array.setDouble(array, index, ((Number) value).doubleValue());

                } else if (char.class.equals(componentType)) {
                    Array.setChar(array, index, ((Character) value).charValue());

                } else if (boolean.class.equals(componentType)) {
                    Array.setBoolean(array, index, ((Boolean) value).booleanValue());

                } else {
                    throw new YAMLException("unexpected primitive type");
                }

            } else {
                // Non-primitive arrays can simply be assigned:
                Array.set(array, index, value);
            }

            ++index;
        }
        return array;
    }

    protected Map<Object, Object> createDefaultMap() {
        // respect order from YAML document
        return new LinkedHashMap<>();
    }

    protected Set<Object> createDefaultSet() {
        // respect order from YAML document
        return new LinkedHashSet<>();
    }

    protected Set<Object> constructSet(MappingNode node) {
        Set<Object> set = createDefaultSet();
        constructSet2ndStep(node, set);
        return set;
    }

    protected Map<Object, Object> constructMapping(MappingNode node) {
        Map<Object, Object> mapping = createDefaultMap();
        constructMapping2ndStep(node, mapping);
        return mapping;
    }

    protected void constructMapping2ndStep(MappingNode node, Map<Object, Object> mapping) {
        List<NodeTuple> nodeValue = (List<NodeTuple>) node.getValue();
        for (NodeTuple tuple : nodeValue) {
            Node keyNode = tuple.getKeyNode();
            Node valueNode = tuple.getValueNode();
            Object key = constructObject(keyNode);
            if (key != null) {
                try {
                    key.hashCode();// check circular dependencies
                } catch (Exception e) {
                    throw new ConstructorException("while constructing a mapping",
                            node.getStartMark(), "found unacceptable key " + key, tuple
                                    .getKeyNode().getStartMark(), e);
                }
            }
            Object value = constructObject(valueNode);
            if (keyNode.isTwoStepsConstruction()) {
                /*
                 * if keyObject is created it 2 steps we should postpone putting
                 * it in map because it may have different hash after
                 * initialization compared to clean just created one. And map of
                 * course does not observe key hashCode changes.
                 */
                maps2fill.add(0,
                        new RecursiveTuple<>(
                                mapping, new RecursiveTuple<>(key, value)));
            } else {
                mapping.put(key, value);
            }
        }
    }

    protected void constructSet2ndStep(MappingNode node, Set<Object> set) {
        List<NodeTuple> nodeValue = (List<NodeTuple>) node.getValue();
        for (NodeTuple tuple : nodeValue) {
            Node keyNode = tuple.getKeyNode();
            Object key = constructObject(keyNode);
            if (key != null) {
                try {
                    key.hashCode();// check circular dependencies
                } catch (Exception e) {
                    throw new ConstructorException("while constructing a Set", node.getStartMark(),
                            "found unacceptable key " + key, tuple.getKeyNode().getStartMark(), e);
                }
            }
            if (keyNode.isTwoStepsConstruction()) {
                /*
                 * if keyObject is created it 2 steps we should postpone putting
                 * it into the set because it may have different hash after
                 * initialization compared to clean just created one. And set of
                 * course does not observe value hashCode changes.
                 */
                sets2fill.add(0, new RecursiveTuple<>(set, key));
            } else {
                set.add(key);
            }
        }
    }

    public void setPropertyUtils(PropertyUtils propertyUtils) {
        this.propertyUtils = propertyUtils;
        explicitPropertyUtils = true;
    }

    public final PropertyUtils getPropertyUtils() {
        if (propertyUtils == null) {
            propertyUtils = new PropertyUtils();
        }
        return propertyUtils;
    }

    private static class RecursiveTuple<T, K> {
        private final T _1;
        private final K _2;

        public RecursiveTuple(T _1, K _2) {
            this._1 = _1;
            this._2 = _2;
        }

        public K _2() {
            return _2;
        }

        public T _1() {
            return _1;
        }
    }

    public final boolean isExplicitPropertyUtils() {
        return explicitPropertyUtils;
    }
}
