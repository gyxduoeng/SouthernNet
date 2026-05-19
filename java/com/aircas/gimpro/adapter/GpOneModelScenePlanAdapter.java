package com.aircas.gimpro.adapter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OneModel scene-plan.json → GIM Pro 场景节点适配器。
 */
public class GpOneModelScenePlanAdapter {

	private static final Pattern OBJECT_PATTERN = Pattern.compile("\\{(.*?)}", Pattern.DOTALL);
	private static final Pattern STRING_FIELD_PATTERN = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"");
	private static final Pattern NUMBER_FIELD_PATTERN = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");

	public AdaptResult adapt(Path scenePlanFile) {
		List<GpAdaptedSceneNode> nodes = new ArrayList<>();
		List<String> errors = new ArrayList<>();
		List<String> warnings = new ArrayList<>();
		if (scenePlanFile == null) {
			errors.add("scene-plan.json 路径为空。") ;
			return new AdaptResult(nodes, errors, warnings);
		}
		if (!Files.exists(scenePlanFile)) {
			errors.add("未找到 scene-plan.json：" + scenePlanFile);
			return new AdaptResult(nodes, errors, warnings);
		}
		String content;
		try {
			content = new String(Files.readAllBytes(scenePlanFile), StandardCharsets.UTF_8);
		} catch (IOException e) {
			errors.add("读取 scene-plan.json 失败：" + scenePlanFile);
			return new AdaptResult(nodes, errors, warnings);
		}
		String arrayContent = extractSceneNodesArray(content);
		if (arrayContent == null) {
			errors.add("scene-plan.json 中未找到 sceneNodes 数组。") ;
			return new AdaptResult(nodes, errors, warnings);
		}
		Matcher objectMatcher = OBJECT_PATTERN.matcher(arrayContent);
		int index = 0;
		while (objectMatcher.find()) {
			String objectBody = objectMatcher.group(1);
			GpAdaptedSceneNode node = parseNode(index, objectBody, warnings);
			if (node != null) {
				nodes.add(node);
			}
			index++;
		}
		if (nodes.isEmpty()) {
			warnings.add("sceneNodes 数组存在，但未解析出有效节点。") ;
		}
		return new AdaptResult(nodes, errors, warnings);
	}

	private String extractSceneNodesArray(String content) {
		if (content == null) {
			return null;
		}
		int keyIndex = content.indexOf("\"sceneNodes\"");
		if (keyIndex < 0) {
			return null;
		}
		int arrayStart = content.indexOf('[', keyIndex);
		if (arrayStart < 0) {
			return null;
		}
		int depth = 0;
		for (int i = arrayStart; i < content.length(); i++) {
			char ch = content.charAt(i);
			if (ch == '[') {
				depth++;
			} else if (ch == ']') {
				depth--;
				if (depth == 0) {
					return content.substring(arrayStart + 1, i);
				}
			}
		}
		return null;
	}

	private GpAdaptedSceneNode parseNode(int index, String objectBody, List<String> warnings) {
		java.util.Map<String, String> stringValues = new java.util.LinkedHashMap<>();
		java.util.Map<String, Double> numericValues = new java.util.LinkedHashMap<>();
		Matcher stringMatcher = STRING_FIELD_PATTERN.matcher(objectBody);
		while (stringMatcher.find()) {
			stringValues.put(stringMatcher.group(1), unescape(stringMatcher.group(2)));
		}
		Matcher numberMatcher = NUMBER_FIELD_PATTERN.matcher(objectBody);
		while (numberMatcher.find()) {
			numericValues.put(numberMatcher.group(1), parseDouble(numberMatcher.group(2)));
		}
		String equipmentId = stringValue(stringValues, "equipmentId");
		String equipmentName = stringValue(stringValues, "equipmentName");
		String areaId = stringValue(stringValues, "areaId");
		String modelId = stringValue(stringValues, "modelId");
		String modelName = stringValue(stringValues, "modelName");
		String modelPath = stringValue(stringValues, "modelPath");
		String equipmentType = stringValue(stringValues, "equipmentType");
		String graphicType = stringValue(stringValues, "graphicType");
		String modelAttributes = stringValue(stringValues, "modelAttributes");
		double x = numberValue(numericValues, "x");
		double y = numberValue(numericValues, "y");
		double z = numberValue(numericValues, "z");
		if (equipmentId.isEmpty()) {
			warnings.add("sceneNodes[" + index + "] 缺少 equipmentId，已跳过。");
			return null;
		}
		if (modelPath.isEmpty()) {
			warnings.add("sceneNodes[" + index + "] 未配置 modelPath：" + equipmentId);
		}
		return new GpAdaptedSceneNode(index, equipmentId, equipmentName, areaId,
				modelId, modelName, modelPath, equipmentType, graphicType, modelAttributes, x, y, z);
	}

	private String stringValue(java.util.Map<String, String> values, String key) {
		String value = values.get(key);
		return value == null ? "" : value.trim();
	}

	private double numberValue(java.util.Map<String, Double> values, String key) {
		Double value = values.get(key);
		return value == null ? 0D : value;
	}

	private double parseDouble(String value) {
		try {
			return Double.parseDouble(value);
		} catch (Exception ignored) {
			return 0D;
		}
	}

	private String unescape(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("\\\"", "\"").replace("\\\\", "\\");
	}

	public static final class AdaptResult {
		private final List<GpAdaptedSceneNode> nodes;
		private final List<String> errors;
		private final List<String> warnings;

		private AdaptResult(List<GpAdaptedSceneNode> nodes, List<String> errors, List<String> warnings) {
			this.nodes = Collections.unmodifiableList(new ArrayList<>(nodes));
			this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
			this.warnings = Collections.unmodifiableList(new ArrayList<>(warnings));
		}

		public List<GpAdaptedSceneNode> getNodes() {
			return nodes;
		}

		public List<String> getErrors() {
			return errors;
		}

		public List<String> getWarnings() {
			return warnings;
		}
	}
}


