package com.aircas.onemodel.service;

import com.aircas.onemodel.model.OneModelModelResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * OneModel 图层显示目录。
 */
public class OneModelLayerCatalogService {

	private static final String FILE_NAME = "layer-catalog.properties";
	private static final String PREFIX = "layer.";
	private static final String MODEL_ID_SUFFIX = ".defaultModelId";
	private static final String MODEL_NAME_SUFFIX = ".defaultModelName";
	private static final String MODEL_TYPE_SUFFIX = ".defaultModelType";
	private static final String MODEL_PATH_SUFFIX = ".defaultModelPath";
	private static final String MODEL_ATTRS_SUFFIX = ".defaultModelAttrs";

	private final OneModelWorkspaceBridge workspaceBridge = new OneModelWorkspaceBridge();

	public void registerLayerCaption(String datasetName, String caption) {
		if (datasetName == null || datasetName.trim().isEmpty() || caption == null || caption.trim().isEmpty()) {
			return;
		}
		Properties properties = load();
		properties.setProperty(PREFIX + datasetName.trim(), caption.trim());
		save(properties);
	}

	public String findLayerCaption(String datasetName) {
		if (datasetName == null || datasetName.trim().isEmpty()) {
			return "";
		}
		Properties properties = load();
		return properties.getProperty(PREFIX + datasetName.trim(), "");
	}

	public void registerLayerDefaultModel(String datasetName, String modelId, String modelName, String modelPath) {
		registerLayerDefaultModel(datasetName, modelId, modelName, "", modelPath, "");
	}

	public void registerLayerDefaultModel(String datasetName, OneModelModelResource modelResource, String fallbackModelType) {
		registerLayerDefaultModel(datasetName,
				modelResource == null ? "" : modelResource.getModelId(),
				modelResource == null ? "" : modelResource.getModelName(),
				modelResource == null ? fallbackModelType : firstNonBlank(modelResource.getModelType(), fallbackModelType),
				modelResource == null ? "" : modelResource.getModelPath(),
				modelResource == null ? "" : modelResource.getModelAttributes());
	}

	public OneModelModelResource findLayerDefaultModel(String datasetName, String fallbackModelType) {
		if (datasetName == null || datasetName.trim().isEmpty()) {
			return null;
		}
		Properties properties = load();
		String key = PREFIX + datasetName.trim();
		String modelId = readNullable(properties, key + MODEL_ID_SUFFIX);
		String modelName = readNullable(properties, key + MODEL_NAME_SUFFIX);
		String modelType = firstNonBlank(readNullable(properties, key + MODEL_TYPE_SUFFIX), fallbackModelType);
		String modelPath = readNullable(properties, key + MODEL_PATH_SUFFIX);
		String modelAttrs = readNullable(properties, key + MODEL_ATTRS_SUFFIX);
		if (isBlank(modelId) && isBlank(modelName) && isBlank(modelPath) && isBlank(modelAttrs)) {
			return null;
		}
		String actualName = firstNonBlank(modelName, deriveNameFromPath(modelPath), "未命名模型");
		String actualType = firstNonBlank(modelType, "未分类");
		String actualPath = modelPath == null ? "" : modelPath;
		String actualId = firstNonBlank(modelId, "MR-" + Math.abs((actualPath + "|" + actualName).hashCode()));
		String actualAttrs = isBlank(modelAttrs) ? buildPropertyPayload(actualName, actualType, actualPath) : modelAttrs;
		return new OneModelModelResource(actualId, actualName, actualType, actualPath, actualAttrs);
	}

	private void registerLayerDefaultModel(String datasetName, String modelId, String modelName, String modelType, String modelPath, String modelAttrs) {
		if (datasetName == null || datasetName.trim().isEmpty()) {
			return;
		}
		Properties properties = load();
		String key = PREFIX + datasetName.trim();
		writeNullable(properties, key + MODEL_ID_SUFFIX, modelId);
		writeNullable(properties, key + MODEL_NAME_SUFFIX, modelName);
		writeNullable(properties, key + MODEL_TYPE_SUFFIX, modelType);
		writeNullable(properties, key + MODEL_PATH_SUFFIX, modelPath);
		writeNullable(properties, key + MODEL_ATTRS_SUFFIX, modelAttrs);
		save(properties);
	}

	private Properties load() {
		Path file = resolveFile();
		Properties properties = new Properties();
		if (!Files.exists(file)) {
			return properties;
		}
		try (InputStream inputStream = Files.newInputStream(file)) {
			properties.load(inputStream);
		} catch (IOException ignored) {
			// 返回空目录。
		}
		return properties;
	}

	private void save(Properties properties) {
		Path file = resolveFile();
		try {
			Files.createDirectories(file.getParent());
			try (OutputStream outputStream = Files.newOutputStream(file)) {
				properties.store(outputStream, "OneModel Layer Catalog");
			}
		} catch (IOException ignored) {
			// 不阻塞主流程。
		}
	}

	private void writeNullable(Properties properties, String key, String value) {
		if (value == null || value.trim().isEmpty()) {
			properties.remove(key);
		} else {
			properties.setProperty(key, value.trim());
		}
	}

	private String readNullable(Properties properties, String key) {
		String value = properties.getProperty(key);
		return value == null ? null : value.trim();
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	private String firstNonBlank(String... values) {
		for (String value : values) {
			if (!isBlank(value)) {
				return value.trim();
			}
		}
		return "";
	}

	private String deriveNameFromPath(String modelPath) {
		if (isBlank(modelPath)) {
			return "";
		}
		try {
			Path path = Paths.get(modelPath.trim());
			Path fileName = path.getFileName();
			return fileName == null ? modelPath.trim() : fileName.toString();
		} catch (Exception ignored) {
			return modelPath.trim();
		}
	}

	private String buildPropertyPayload(String modelName, String modelType, String modelPath) {
		return "{\"modelName\":\"" + escape(modelName)
				+ "\",\"modelType\":\"" + escape(modelType)
				+ "\",\"modelPath\":\"" + escape(modelPath)
				+ "\"}";
	}

	private String escape(String value) {
		return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	private Path resolveFile() {
		return workspaceBridge.resolveWorkspaceRuntimeDir().resolve(FILE_NAME);
	}
}

