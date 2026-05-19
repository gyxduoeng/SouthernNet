package com.aircas.onemodel.model;

/**
 * 设备点记录。
 */
public class OneModelEquipmentRecord {

	private final String equipmentId;
	private final String equipmentCode;
	private final String areaId;
	private final String equipmentName;
	private final String equipmentType;
	private final String modelCategoryKey;
	private final String status;
	private final String versionId;
	private final String graphicType;
	private final String modelId;
	private final String modelName;
	private final String modelPath;
	private final String modelAttributes;
	private final double x;
	private final double y;

	public OneModelEquipmentRecord(String equipmentId, String equipmentCode, String areaId, String equipmentName, String equipmentType,
									 String modelCategoryKey, String status, String versionId, String graphicType,
									 String modelId, String modelName, String modelPath, String modelAttributes,
									 double x, double y) {
		this.equipmentId = equipmentId;
		this.equipmentCode = equipmentCode;
		this.areaId = areaId;
		this.equipmentName = equipmentName;
		this.equipmentType = equipmentType;
		this.modelCategoryKey = modelCategoryKey;
		this.status = status;
		this.versionId = versionId;
		this.graphicType = graphicType;
		this.modelId = modelId;
		this.modelName = modelName;
		this.modelPath = modelPath;
		this.modelAttributes = modelAttributes;
		this.x = x;
		this.y = y;
	}

	public String getEquipmentId() {
		return equipmentId;
	}

	public String getEquipmentCode() {
		return equipmentCode;
	}

	public String getAreaId() {
		return areaId;
	}

	public String getEquipmentName() {
		return equipmentName;
	}

	public String getEquipmentType() {
		return equipmentType;
	}

	public String getModelCategory() {
		return equipmentType;
	}

	public String getModelCategoryKey() {
		return modelCategoryKey;
	}

	public String getStatus() {
		return status;
	}

	public String getVersionId() {
		return versionId;
	}

	public String getGraphicType() {
		return graphicType;
	}

	public String getModelId() {
		return modelId;
	}

	public String getModelName() {
		return modelName;
	}

	public String getModelPath() {
		return modelPath;
	}

	public String getModelAttributes() {
		return modelAttributes;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public boolean hasModelBinding() {
		return modelId != null && !modelId.trim().isEmpty();
	}

	@Override
	public String toString() {
		String code = equipmentCode == null || equipmentCode.trim().isEmpty() ? equipmentId : equipmentCode;
		return equipmentName + " [" + equipmentType + "]" + " <" + code + ">";
	}
}

