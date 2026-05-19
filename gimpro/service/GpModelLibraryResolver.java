package com.aircas.gimpro.service;

import com.aircas.gimpro.adapter.GpAdaptedSceneNode;
import com.aircas.gimpro.model.GpModelLibraryEntry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * GIM Pro 模型库解析服务。
 */
public class GpModelLibraryResolver {

	public List<GpModelLibraryEntry> resolve(List<GpAdaptedSceneNode> nodes, String modelLibraryPath) {
		List<GpModelLibraryEntry> result = new ArrayList<>();
		Path libraryRoot = toPath(modelLibraryPath);
		Map<String, Path> index = libraryRoot == null ? java.util.Collections.emptyMap() : buildIndex(libraryRoot);
		for (GpAdaptedSceneNode node : nodes) {
			String configured = node.getModelPath();
			Path configuredPath = toPath(configured);
			Path resolved = resolveEffectivePath(configuredPath, node, index);
			String status;
			if (resolved != null && Files.exists(resolved)) {
				status = configuredPath != null && configuredPath.equals(resolved) ? "已按配置定位" : "已按模型库补全";
			} else {
				status = "未定位到模型文件";
			}
			result.add(new GpModelLibraryEntry(
					node.getEquipmentId(),
					node.getDisplayLabel(),
					configured,
					resolved == null ? "" : resolved.toString(),
					status,
					buildPropertySummary(node)));
		}
		return result;
	}

	private Path resolveEffectivePath(Path configuredPath, GpAdaptedSceneNode node, Map<String, Path> index) {
		if (configuredPath != null && Files.exists(configuredPath)) {
			return configuredPath;
		}
		String modelName = normalizeKey(node.getModelName());
		if (!modelName.isEmpty() && index.containsKey(modelName)) {
			return index.get(modelName);
		}
		String equipmentId = normalizeKey(node.getEquipmentId());
		if (!equipmentId.isEmpty() && index.containsKey(equipmentId)) {
			return index.get(equipmentId);
		}
		return null;
	}

	private Map<String, Path> buildIndex(Path root) {
		Map<String, Path> result = new HashMap<>();
		try (Stream<Path> stream = Files.walk(root)) {
			stream
					.filter(Files::isRegularFile)
					.filter(path -> path.getFileName() != null)
					.filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".gim"))
					.limit(5000)
					.forEach(path -> {
						String fileName = path.getFileName().toString();
						String base = fileName.substring(0, fileName.length() - 4);
						result.putIfAbsent(normalizeKey(base), path);
						result.putIfAbsent(normalizeKey(fileName), path);
					});
		} catch (IOException ignored) {
			// 保持宽松。
		}
		return result;
	}

	private String buildPropertySummary(GpAdaptedSceneNode node) {
		StringBuilder builder = new StringBuilder();
		builder.append("设备类型=").append(node.getEquipmentType());
		if (!node.getGraphicType().isEmpty()) {
			builder.append("; 图形类型=").append(node.getGraphicType());
		}
		if (!node.getModelAttributes().isEmpty()) {
			String attrs = node.getModelAttributes();
			builder.append("; 模型属性=").append(attrs.length() > 120 ? attrs.substring(0, 120) + "..." : attrs);
		}
		return builder.toString();
	}

	private Path toPath(String value) {
		if (value == null || value.trim().isEmpty()) {
			return null;
		}
		try {
			return Paths.get(value.trim()).toAbsolutePath().normalize();
		} catch (Exception ignored) {
			return null;
		}
	}

	private String normalizeKey(String value) {
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
	}
}

