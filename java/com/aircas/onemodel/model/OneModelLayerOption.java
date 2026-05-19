package com.aircas.onemodel.model;

/**
 * OneModel 图层选项。
 */
public class OneModelLayerOption {

	private final String datasetName;
	private final String caption;

	public OneModelLayerOption(String datasetName, String caption) {
		this.datasetName = datasetName;
		this.caption = caption;
	}

	public String getDatasetName() {
		return datasetName;
	}

	public String getCaption() {
		return caption;
	}

	@Override
	public String toString() {
		return caption == null || caption.trim().isEmpty() ? datasetName : caption + "  [" + datasetName + "]";
	}
}

