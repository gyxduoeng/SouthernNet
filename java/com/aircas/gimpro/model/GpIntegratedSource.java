package com.aircas.gimpro.model;

/**
 * GIM Pro 已整合数据源条目。
 */
public class GpIntegratedSource {

	private final GpDataCategory category;
	private final String name;
	private final String path;
	private final boolean available;
	private final String notes;

	public GpIntegratedSource(GpDataCategory category, String name, String path, boolean available, String notes) {
		this.category = category;
		this.name = safe(name);
		this.path = safe(path);
		this.available = available;
		this.notes = safe(notes);
	}

	public GpDataCategory getCategory() {
		return category;
	}

	public String getName() {
		return name;
	}

	public String getPath() {
		return path;
	}

	public boolean isAvailable() {
		return available;
	}

	public String getNotes() {
		return notes;
	}

	private String safe(String value) {
		return value == null ? "" : value.trim();
	}
}

