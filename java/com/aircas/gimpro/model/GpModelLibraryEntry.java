package com.aircas.gimpro.model;

/**
 * 模型库解析条目。
 */
public class GpModelLibraryEntry {

	private final String equipmentId;
	private final String displayName;
	private final String configuredModelPath;
	private final String resolvedModelPath;
	private final String status;
	private final String propertySummary;

	public GpModelLibraryEntry(String equipmentId, String displayName, String configuredModelPath,
			String resolvedModelPath, String status, String propertySummary) {
		this.equipmentId = safe(equipmentId);
		this.displayName = safe(displayName);
		this.configuredModelPath = safe(configuredModelPath);
		this.resolvedModelPath = safe(resolvedModelPath);
		this.status = safe(status);
		this.propertySummary = safe(propertySummary);
	}

	public String getEquipmentId() {
		return equipmentId;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getConfiguredModelPath() {
		return configuredModelPath;
	}

	public String getResolvedModelPath() {
		return resolvedModelPath;
	}

	public String getStatus() {
		return status;
	}

	public String getPropertySummary() {
		return propertySummary;
	}

	private String safe(String value) {
		return value == null ? "" : value.trim();
	}
}

