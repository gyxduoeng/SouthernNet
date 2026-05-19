package com.aircas.onemodel.service;

import com.aircas.onemodel.model.OneModelModelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 模型资源扫描。
 */
public class OneModelModelScanner {

	private final OmPathSupport pathSupport = new OmPathSupport();
	private final OneModelSessionStore sessionStore = OneModelSessionStore.getInstance();

	public List<OneModelModelResource> scanModels() {
		Path root = resolveModelRoot();
		List<OneModelModelResource> resources = new ArrayList<>();
		if (root == null || !Files.exists(root)) {
			return resources;
		}
		try (Stream<Path> stream = Files.walk(root)) {
			List<Path> modelFiles = stream.filter(Files::isRegularFile)
					.filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".gim"))
					.sorted(Comparator.comparing(path -> path.toAbsolutePath().normalize().toString(), String.CASE_INSENSITIVE_ORDER))
					.collect(Collectors.toList());
			for (Path path : modelFiles) {
				resources.add(new OneModelModelResource(
							"MR-" + Math.abs(path.toString().hashCode()),
							path.getFileName().toString(),
							resolveCategory(root, path),
							path.toString(),
							buildPropertyPayload(path)));
			}
		} catch (IOException ignored) {
			// 交由上层展示空结果。
		}
		return resources;
	}

	public List<String> scanModelCategories() {
		Set<String> categories = new LinkedHashSet<>();
		for (OneModelModelResource resource : scanModels()) {
			String category = resource.getModelType();
			if (category != null && !category.trim().isEmpty()) {
				categories.add(category.trim());
			}
		}
		return new ArrayList<>(categories);
	}

	private String buildPropertyPayload(Path path) {
		String fileName = path.getFileName() == null ? path.toString() : path.getFileName().toString();
		String modelType = resolveCategory(resolveModelRoot(), path);
		long fileSize = -1L;
		FileTime modifiedTime = null;
		try {
			fileSize = Files.size(path);
			modifiedTime = Files.getLastModifiedTime(path);
		} catch (IOException ignored) {
			// 使用兜底值。
		}
		return "{\"modelName\":\"" + escape(fileName)
				+ "\",\"modelType\":\"" + escape(modelType)
				+ "\",\"modelPath\":\"" + escape(path.toString())
				+ "\",\"fileSize\":" + fileSize
				+ ",\"lastModified\":\"" + escape(modifiedTime == null ? "" : modifiedTime.toString())
				+ "\"}";
	}

	private Path resolveModelRoot() {
		String configured = sessionStore.getParameters().getModelLibraryPath();
		if (configured != null && !configured.trim().isEmpty()) {
			return Paths.get(configured.trim());
		}
		return pathSupport.resolveProjectRoot().resolve("data");
	}

	private String resolveCategory(Path root, Path modelFile) {
		if (root == null || modelFile == null) {
			return "未分类";
		}
		try {
			Path relative = root.toAbsolutePath().normalize().relativize(modelFile.toAbsolutePath().normalize());
			if (relative.getNameCount() > 1) {
				String folderName = relative.getName(relative.getNameCount() - 2).toString().trim();
				if (!folderName.isEmpty()) {
					return folderName;
				}
			}
		} catch (Exception ignored) {
			// 回退到文件名。
		}
		String fileName = modelFile.getFileName() == null ? modelFile.toString() : modelFile.getFileName().toString();
		int dotIndex = fileName.lastIndexOf('.');
		String trimmed = dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
		return trimmed.trim().isEmpty() ? "未分类" : trimmed.trim();
	}

	private String escape(String value) {
		return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
	}
}


