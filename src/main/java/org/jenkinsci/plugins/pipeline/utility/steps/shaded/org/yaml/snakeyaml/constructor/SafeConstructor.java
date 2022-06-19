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

import java.math.BigInteger;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jenkinsci.plugins.pipeline.utility.steps.shaded.org.yaml.snakeyaml.nodes.MappingNode;
import org.jenkinsci.plugins.pipeline.utility.steps.shaded.org.yaml.snakeyaml.error.YAMLException;
import org.jenkinsci.plugins.pipeline.utility.steps.shaded.org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;
import org.jenkinsci.plugins.pipeline.utility.steps.shaded.org.yaml.snakeyaml.nodes.Node;
import org.jenkinsci.plugins.pipeline.utility.steps.shaded.org.yaml.snakeyaml.nodes.NodeId;
import org.jenkinsci.plugins.pipeline.utility.steps.shaded.org.yaml.snakeyaml.nodes.NodeTuple;
import org.jenkinsci.plugins.pipeline.utility.steps.shaded.org.yaml.snakeyaml.nodes.ScalarNode;
import org.jenkinsci.plugins.pipeline.utility.steps.shaded.org.yaml.snakeyaml.nodes.SequenceNode;
import org.jenkinsci.plugins.pipeline.utility.steps.shaded.org.yaml.snakeyaml.nodes.Tag;

/**
 * Construct standard Java classes
 */
public class SafeConstructor extends BaseConstructor {

    public static final ConstructUndefined undefinedConstructor = new ConstructUndefined();

    public SafeConstructor() {
        this.yamlConstructors.put(Tag.NULL, new ConstructYamlNull());
        this.yamlConstructors.put(Tag.BOOL, new ConstructYamlBool());
        this.yamlConstructors.put(Tag.INT, new ConstructYamlInt());
        this.yamlConstructors.put(Tag.FLOAT, new ConstructYamlFloat());
        this.yamlConstructors.put(Tag.BINARY, new ConstructYamlBinary());
        this.yamlConstructors.put(Tag.TIMESTAMP, new ConstructYamlTimestamp());
        this.yamlConstructors.put(Tag.OMAP, new ConstructYamlOmap());
        this.yamlConstructors.put(Tag.PAIRS, new ConstructYamlPairs());
        this.yamlConstructors.put(Tag.SET, new ConstructYamlSet());
        this.yamlConstructors.put(Tag.STR, new ConstructYamlStr());
        this.yamlConstructors.put(Tag.SEQ, new ConstructYamlSeq());
        this.yamlConstructors.put(Tag.MAP, new ConstructYamlMap());
        this.yamlConstructors.put(null, undefinedConstructor);
        this.yamlClassConstructors.put(NodeId.scalar, undefinedConstructor);
        this.yamlClassConstructors.put(NodeId.sequence, undefinedConstructor);
        this.yamlClassConstructors.put(NodeId.mapping, undefinedConstructor);
    }

    protected void flattenMapping(MappingNode node) {
        // perform merging only on nodes containing merge node(s)
        if (node.isMerged()) {
            node.setValue(mergeNode(node, true, new HashMap<>(),
                    new ArrayList<>()));
        }
    }

    /**
     * Does merge for supplied mapping node.
     * 
     * @param node
     *            where to merge
     * @param isPreffered
     *            true if keys of node should take precedence over others...
     * @param key2index
     *            maps already merged keys to index from values
     * @param values
     *            collects merged NodeTuple
     * @return list of the merged NodeTuple (to be set as value for the
     *         MappingNode)
     */
    private List<NodeTuple> mergeNode(MappingNode node, boolean isPreffered,
            Map<Object, Integer> key2index, List<NodeTuple> values) {
        List<NodeTuple> nodeValue = node.getValue();
        // reversed for http://code.google.com/p/snakeyaml/issues/detail?id=139
        Collections.reverse(nodeValue);
        for (Iterator<NodeTuple> iter = nodeValue.iterator(); iter.hasNext();) {
            final NodeTuple nodeTuple = iter.next();
            final Node keyNode = nodeTuple.getKeyNode();
            final Node valueNode = nodeTuple.getValueNode();
            if (keyNode.getTag().equals(Tag.MERGE)) {
                iter.remove();
                switch (valueNode.getNodeId()) {
                case mapping:
                    MappingNode mn = (MappingNode) valueNode;
                    mergeNode(mn, false, key2index, values);
                    break;
                case sequence:
                    SequenceNode sn = (SequenceNode) valueNode;
                    List<Node> vals = sn.getValue();
                    for (Node subnode : vals) {
                        if (!(subnode instanceof MappingNode)) {
                            throw new ConstructorException("while constructing a mapping",
                                    node.getStartMark(),
                                    "expected a mapping for merging, but found "
                                            + subnode.getNodeId(), subnode.getStartMark());
                        }
                        MappingNode mnode = (MappingNode) subnode;
                        mergeNode(mnode, false, key2index, values);
                    }
                    break;
                default:
                    throw new ConstructorException("while constructing a mapping",
                            node.getStartMark(),
                            "expected a mapping or list of mappings for merging, but found "
                                    + valueNode.getNodeId(), valueNode.getStartMark());
                }
            } else {
                // we need to construct keys to avoid duplications
                Object key = constructObject(keyNode);
                if (!key2index.containsKey(key)) { // 1st time merging key
                    values.add(nodeTuple);
                    // keep track where tuple for the key is
                    key2index.put(key, values.size() - 1);
                } else if (isPreffered) { // there is value for the key, but we
                                          // need to override it
                    // change value for the key using saved position
                    values.set(key2index.get(key), nodeTuple);
                }
            }
        }
        return values;
    }

    protected void constructMapping2ndStep(MappingNode node, Map<Object, Object> mapping) {
        flattenMapping(node);
        super.constructMapping2ndStep(node, mapping);
    }

    @Override
    protected void constructSet2ndStep(MappingNode node, Set<Object> set) {
        flattenMapping(node);
        super.constructSet2ndStep(node, set);
    }

    public class ConstructYamlNull extends AbstractConstruct {
        public Object construct(Node node) {
            constructScalar((ScalarNode) node);
            return null;
        }
    }

    private final static Map<String, Boolean> BOOL_VALUES = new HashMap<>();
    static {
        BOOL_VALUES.put("yes", Boolean.TRUE);
        BOOL_VALUES.put("no", Boolean.FALSE);
        BOOL_VALUES.put("true", Boolean.TRUE);
        BOOL_VALUES.put("false", Boolean.FALSE);
        BOOL_VALUES.put("on", Boolean.TRUE);
        BOOL_VALUES.put("off", Boolean.FALSE);
    }

    public class ConstructYamlBool extends AbstractConstruct {
        public Object construct(Node node) {
            String val = (String) constructScalar((ScalarNode) node);
            return BOOL_VALUES.get(val.toLowerCase());
        }
    }

    public class ConstructYamlInt extends AbstractConstruct {
        public Object construct(Node node) {
            String value = constructScalar((ScalarNode) node).toString().replaceAll("_", "");
            int sign = +1;
            char first = value.charAt(0);
            if (first == '-') {
                sign = -1;
                value = value.substring(1);
            } else if (first == '+') {
                value = value.substring(1);
            }
            int base = 10;
            if ("0".equals(value)) {
                return Integer.valueOf(0);
            } else if (value.startsWith("0b")) {
                value = value.substring(2);
                base = 2;
            } else if (value.startsWith("0x")) {
                value = value.substring(2);
                base = 16;
            } else if (value.startsWith("0")) {
                value = value.substring(1);
                base = 8;
            } else if (value.indexOf(':') != -1) {
                String[] digits = value.split(":");
                int bes = 1;
                int val = 0;
                for (int i = 0, j = digits.length; i < j; i++) {
                    val += Long.parseLong(digits[j - i - 1]) * bes;
                    bes *= 60;
                }
                return createNumber(sign, String.valueOf(val), 10);
            } else {
                return createNumber(sign, value, 10);
            }
            return createNumber(sign, value, base);
        }
    }

    private Number createNumber(int sign, String number, int radix) {
        Number result;
        if (sign < 0) {
            number = "-" + number;
        }
        try {
            result = Integer.valueOf(number, radix);
        } catch (NumberFormatException e) {
            try {
                result = Long.valueOf(number, radix);
            } catch (NumberFormatException e1) {
                result = new BigInteger(number, radix);
            }
        }
        return result;
    }

    public class ConstructYamlFloat extends AbstractConstruct {
        public Object construct(Node node) {
            String value = constructScalar((ScalarNode) node).toString().replaceAll("_", "");
            int sign = +1;
            char first = value.charAt(0);
            if (first == '-') {
                sign = -1;
                value = value.substring(1);
            } else if (first == '+') {
                value = value.substring(1);
            }
            String valLower = value.toLowerCase();
            if (".inf".equals(valLower)) {
                return new Double(sign == -1 ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY);
            } else if (".nan".equals(valLower)) {
                return new Double(Double.NaN);
            } else if (value.indexOf(':') != -1) {
                String[] digits = value.split(":");
                int bes = 1;
                double val = 0.0;
                for (int i = 0, j = digits.length; i < j; i++) {
                    val += Double.parseDouble(digits[j - i - 1]) * bes;
                    bes *= 60;
                }
                return new Double(sign * val);
            } else {
                Double d = Double.valueOf(value);
                return new Double(d.doubleValue() * sign);
            }
        }
    }

    public class ConstructYamlBinary extends AbstractConstruct {
        public Object construct(Node node) {
            byte[] decoded = Base64Coder.decode(constructScalar((ScalarNode) node).toString()
                    .toCharArray());
            return decoded;
        }
    }

    public class ConstructYamlNumber extends AbstractConstruct {

        private final NumberFormat nf = NumberFormat.getInstance();

        public Object construct(Node node) {
            ScalarNode scalar = (ScalarNode) node;
            try {
                return nf.parse(scalar.getValue());
            } catch (ParseException e) {
                String lowerCaseValue = scalar.getValue().toLowerCase();
                if (lowerCaseValue.contains("inf") || lowerCaseValue.contains("nan")) {
                    /*
                     * Non-finites such as (+/-)infinity and NaN are not
                     * parseable by NumberFormat when these `Double` values are
                     * dumped by snakeyaml. Delegate to the `Tag.FLOAT`
                     * constructor when for this expected failure cause.
                     */
                    return (Number) yamlConstructors.get(Tag.FLOAT).construct(node);
                } else {
                    throw new IllegalArgumentException("Unable to parse as Number: "
                            + scalar.getValue());
                }
            }
        }
    }

    private final static Pattern TIMESTAMP_REGEXP = Pattern
            .compile("^([0-9][0-9][0-9][0-9])-([0-9][0-9]?)-([0-9][0-9]?)(?:(?:[Tt]|[ \t]+)([0-9][0-9]?):([0-9][0-9]):([0-9][0-9])(?:\\.([0-9]*))?(?:[ \t]*(?:Z|([-+][0-9][0-9]?)(?::([0-9][0-9])?)?))?)?$");
    private final static Pattern YMD_REGEXP = Pattern
            .compile("^([0-9][0-9][0-9][0-9])-([0-9][0-9]?)-([0-9][0-9]?)$");

    public static class ConstructYamlTimestamp extends AbstractConstruct {
        private Calendar calendar;

        public Calendar getCalendar() {
            return calendar;
        }

        public Object construct(Node node) {
            ScalarNode scalar = (ScalarNode) node;
            String nodeValue = scalar.getValue();
            Matcher match = YMD_REGEXP.matcher(nodeValue);
            if (match.matches()) {
                String year_s = match.group(1);
                String month_s = match.group(2);
                String day_s = match.group(3);
                calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                calendar.clear();
                calendar.set(Calendar.YEAR, Integer.parseInt(year_s));
                // Java's months are zero-based...
                calendar.set(Calendar.MONTH, Integer.parseInt(month_s) - 1); // x
                calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(day_s));
                return calendar.getTime();
            } else {
                match = TIMESTAMP_REGEXP.matcher(nodeValue);
                if (!match.matches()) {
                    throw new YAMLException("Unexpected timestamp: " + nodeValue);
                }
                String year_s = match.group(1);
                String month_s = match.group(2);
                String day_s = match.group(3);
                String hour_s = match.group(4);
                String min_s = match.group(5);
                // seconds and milliseconds
                String seconds = match.group(6);
                String millis = match.group(7);
                if (millis != null) {
                    seconds = seconds + "." + millis;
                }
                double fractions = Double.parseDouble(seconds);
                int sec_s = (int) Math.round(Math.floor(fractions));
                int usec = (int) Math.round((fractions - sec_s) * 1000);
                // timezone
                String timezoneh_s = match.group(8);
                String timezonem_s = match.group(9);
                TimeZone timeZone;
                if (timezoneh_s != null) {
                    String time = timezonem_s != null ? ":" + timezonem_s : "00";
                    timeZone = TimeZone.getTimeZone("GMT" + timezoneh_s + time);
                } else {
                    // no time zone provided
                    timeZone = TimeZone.getTimeZone("UTC");
                }
                calendar = Calendar.getInstance(timeZone);
                calendar.set(Calendar.YEAR, Integer.parseInt(year_s));
                // Java's months are zero-based...
                calendar.set(Calendar.MONTH, Integer.parseInt(month_s) - 1);
                calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(day_s));
                calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hour_s));
                calendar.set(Calendar.MINUTE, Integer.parseInt(min_s));
                calendar.set(Calendar.SECOND, sec_s);
                calendar.set(Calendar.MILLISECOND, usec);
                return calendar.getTime();
            }
        }
    }

    public class ConstructYamlOmap extends AbstractConstruct {
        public Object construct(Node node) {
            // Note: we do not check for duplicate keys, because it's too
            // CPU-expensive.
            Map<Object, Object> omap = new LinkedHashMap<>();
            if (!(node instanceof SequenceNode)) {
                throw new ConstructorException("while constructing an ordered map",
                        node.getStartMark(), "expected a sequence, but found " + node.getNodeId(),
                        node.getStartMark());
            }
            SequenceNode snode = (SequenceNode) node;
            for (Node subnode : snode.getValue()) {
                if (!(subnode instanceof MappingNode)) {
                    throw new ConstructorException("while constructing an ordered map",
                            node.getStartMark(), "expected a mapping of length 1, but found "
                                    + subnode.getNodeId(), subnode.getStartMark());
                }
                MappingNode mnode = (MappingNode) subnode;
                if (mnode.getValue().size() != 1) {
                    throw new ConstructorException("while constructing an ordered map",
                            node.getStartMark(), "expected a single mapping item, but found "
                                    + mnode.getValue().size() + " items", mnode.getStartMark());
                }
                Node keyNode = mnode.getValue().get(0).getKeyNode();
                Node valueNode = mnode.getValue().get(0).getValueNode();
                Object key = constructObject(keyNode);
                Object value = constructObject(valueNode);
                omap.put(key, value);
            }
            return omap;
        }
    }

    // Note: the same code as `construct_yaml_omap`.
    public class ConstructYamlPairs extends AbstractConstruct {
        public Object construct(Node node) {
            // Note: we do not check for duplicate keys, because it's too
            // CPU-expensive.
            if (!(node instanceof SequenceNode)) {
                throw new ConstructorException("while constructing pairs", node.getStartMark(),
                        "expected a sequence, but found " + node.getNodeId(), node.getStartMark());
            }
            SequenceNode snode = (SequenceNode) node;
            List<Object[]> pairs = new ArrayList<>(snode.getValue().size());
            for (Node subnode : snode.getValue()) {
                if (!(subnode instanceof MappingNode)) {
                    throw new ConstructorException("while constructingpairs", node.getStartMark(),
                            "expected a mapping of length 1, but found " + subnode.getNodeId(),
                            subnode.getStartMark());
                }
                MappingNode mnode = (MappingNode) subnode;
                if (mnode.getValue().size() != 1) {
                    throw new ConstructorException("while constructing pairs", node.getStartMark(),
                            "expected a single mapping item, but found " + mnode.getValue().size()
                                    + " items", mnode.getStartMark());
                }
                Node keyNode = mnode.getValue().get(0).getKeyNode();
                Node valueNode = mnode.getValue().get(0).getValueNode();
                Object key = constructObject(keyNode);
                Object value = constructObject(valueNode);
                pairs.add(new Object[] { key, value });
            }
            return pairs;
        }
    }

    public class ConstructYamlSet implements Construct {
        public Object construct(Node node) {
            if (node.isTwoStepsConstruction()) {
                return createDefaultSet();
            } else {
                return constructSet((MappingNode) node);
            }
        }

        @SuppressWarnings("unchecked")
        public void construct2ndStep(Node node, Object object) {
            if (node.isTwoStepsConstruction()) {
                constructSet2ndStep((MappingNode) node, (Set<Object>) object);
            } else {
                throw new YAMLException("Unexpected recursive set structure. Node: " + node);
            }
        }
    }

    public class ConstructYamlStr extends AbstractConstruct {
        public Object construct(Node node) {
            return constructScalar((ScalarNode) node);
        }
    }

    public class ConstructYamlSeq implements Construct {
        public Object construct(Node node) {
            SequenceNode seqNode = (SequenceNode) node;
            if (node.isTwoStepsConstruction()) {
                return createDefaultList(seqNode.getValue().size());
            } else {
                return constructSequence(seqNode);
            }
        }

        @SuppressWarnings("unchecked")
        public void construct2ndStep(Node node, Object data) {
            if (node.isTwoStepsConstruction()) {
                constructSequenceStep2((SequenceNode) node, (List<Object>) data);
            } else {
                throw new YAMLException("Unexpected recursive sequence structure. Node: " + node);
            }
        }
    }

    public class ConstructYamlMap implements Construct {
        public Object construct(Node node) {
            if (node.isTwoStepsConstruction()) {
                return createDefaultMap();
            } else {
                return constructMapping((MappingNode) node);
            }
        }

        @SuppressWarnings("unchecked")
        public void construct2ndStep(Node node, Object object) {
            if (node.isTwoStepsConstruction()) {
                constructMapping2ndStep((MappingNode) node, (Map<Object, Object>) object);
            } else {
                throw new YAMLException("Unexpected recursive mapping structure. Node: " + node);
            }
        }
    }

    public static final class ConstructUndefined extends AbstractConstruct {
        public Object construct(Node node) {
            throw new ConstructorException(null, null,
                    "could not determine a constructor for the tag " + node.getTag(),
                    node.getStartMark());
        }
    }
}
