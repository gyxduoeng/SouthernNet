package com.aircas.gimpro.model;

/**
 * GIM Pro 支持的数据类型条目。
 */
public class GpSupportedTypeEntry {

	private final GpDataCategory category;
	private final String code;
	private final String displayName;
	private final String notes;

	public GpSupportedTypeEntry(GpDataCategory category, String code, String displayName, String notes) {
		this.category = category;
		this.code = safe(code);
		this.displayName = safe(displayName);
		this.notes = safe(notes);
	}

	public GpDataCategory getCategory() {
		return category;
	}

	public String getCode() {
		return code;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getNotes() {
		return notes;
	}

	private String safe(String value) {
		return value == null ? "" : value.trim();
	}
}

