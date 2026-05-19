package com.aircas.gimpro.model;

/**
 * GIM Pro 数据类型分类。
 */
public enum GpDataCategory {
	TWO_D("二维"),
	THREE_D("三维");

	private final String label;

	GpDataCategory(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}

