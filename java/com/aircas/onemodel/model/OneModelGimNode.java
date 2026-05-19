package com.aircas.onemodel.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * .gim 模型树节点。
 */
public class OneModelGimNode {

	private final String label;
	private final String nodeType;
	private final Map<String, String> properties;
	private final List<OneModelGimNode> children;

	public OneModelGimNode(String label, String nodeType, Map<String, String> properties, List<OneModelGimNode> children) {
		this.label = label == null ? "" : label.trim();
		this.nodeType = nodeType == null ? "" : nodeType.trim();
		this.properties = wrapProperties(properties);
		this.children = wrapChildren(children);
	}

	public String getLabel() {
		return label;
	}

	public String getNodeType() {
		return nodeType;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public List<OneModelGimNode> getChildren() {
		return children;
	}

	private Map<String, String> wrapProperties(Map<String, String> source) {
		if (source == null || source.isEmpty()) {
			return Collections.emptyMap();
		}
		return Collections.unmodifiableMap(new LinkedHashMap<>(source));
	}

	private List<OneModelGimNode> wrapChildren(List<OneModelGimNode> source) {
		if (source == null || source.isEmpty()) {
			return Collections.emptyList();
		}
		return Collections.unmodifiableList(new ArrayList<>(source));
	}

	@Override
	public String toString() {
		return label;
	}
}

