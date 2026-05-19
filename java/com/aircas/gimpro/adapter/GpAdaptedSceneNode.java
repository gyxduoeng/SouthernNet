package com.aircas.gimpro.adapter;

/**
 * GIM Pro 适配后的场景节点。
 *
 * <p>该对象用于承接 OneModel 输出的 scene-plan 节点，并冻结为 GIM Pro 自己的三维接入对象。</p>
 */
public class GpAdaptedSceneNode {

	private final int sourceIndex;
	private final String equipmentId;
	private final String equipmentName;
	private final String areaId;
	private final String modelId;
	private final String modelName;
	private final String modelPath;
	private final String equipmentType;
	private final String graphicType;
	private final String modelAttributes;
	private final double x;
	private final double y;
	private final double z;

	public GpAdaptedSceneNode(int sourceIndex, String equipmentId, String equipmentName, String areaId,
			String modelId, String modelName, String modelPath, String equipmentType,
			String graphicType, String modelAttributes, double x, double y, double z) {
		this.sourceIndex = Math.max(sourceIndex, 0);
		this.equipmentId = safe(equipmentId);
		this.equipmentName = safe(equipmentName);
		this.areaId = safe(areaId);
		this.modelId = safe(modelId);
		this.modelName = safe(modelName);
		this.modelPath = safe(modelPath);
		this.equipmentType = safe(equipmentType);
		this.graphicType = safe(graphicType);
		this.modelAttributes = safe(modelAttributes);
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public int getSourceIndex() {
		return sourceIndex;
	}

	public String getEquipmentId() {
		return equipmentId;
	}

	public String getEquipmentName() {
		return equipmentName;
	}

	public String getAreaId() {
		return areaId;
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

	public String getEquipmentType() {
		return equipmentType;
	}

	public String getGraphicType() {
		return graphicType;
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

	public double getZ() {
		return z;
	}

	public boolean hasModelPath() {
		return !modelPath.isEmpty();
	}

	public String getDisplayLabel() {
		if (!equipmentName.isEmpty() && !equipmentId.isEmpty()) {
			return equipmentName + " [" + equipmentId + "]";
		}
		if (!equipmentName.isEmpty()) {
			return equipmentName;
		}
		return equipmentId.isEmpty() ? "未命名节点" : equipmentId;
	}

	private String safe(String value) {
		return value == null ? "" : value.trim();
	}
}

