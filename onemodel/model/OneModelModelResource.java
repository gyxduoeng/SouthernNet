package com.aircas.onemodel.model;

/**
 * 三维模型资源。
 */
public class OneModelModelResource {

	private final String modelId;
	private final String modelName;
	private final String modelType;
	private final String modelPath;
	private final String modelAttributes;

	public OneModelModelResource(String modelId, String modelName, String modelType, String modelPath, String modelAttributes) {
		this.modelId = modelId;
		this.modelName = modelName;
		this.modelType = modelType;
		this.modelPath = modelPath;
		this.modelAttributes = modelAttributes;
	}

	public String getModelId() {
		return modelId;
	}

	public String getModelName() {
		return modelName;
	}

	public String getModelType() {
		return modelType;
	}

	public String getModelPath() {
		return modelPath;
	}

	public String getModelAttributes() {
		return modelAttributes;
	}

	@Override
	public String toString() {
		return modelName + " [" + modelType + "]";
	}
}

