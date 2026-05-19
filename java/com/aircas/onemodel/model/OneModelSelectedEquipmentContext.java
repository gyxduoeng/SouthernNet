package com.aircas.onemodel.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 当前选中设备的上下文信息。
 */
public class OneModelSelectedEquipmentContext {

	private final String datasetName;
	private final String layerCaption;
	private final long recordId;
	private final OneModelEquipmentRecord equipmentRecord;
	private final OneModelModelResource modelResource;
	private final String modelSource;
	private final Map<String, String> equipmentProperties;
	private final Map<String, String> modelSummaryProperties;
	private final Map<String, String> modelAttributeProperties;

	public OneModelSelectedEquipmentContext(String datasetName, String layerCaption, long recordId,
			OneModelEquipmentRecord equipmentRecord, OneModelModelResource modelResource, String modelSource,
			Map<String, String> equipmentProperties, Map<String, String> modelSummaryProperties,
			Map<String, String> modelAttributeProperties) {
		this.datasetName = datasetName;
		this.layerCaption = layerCaption;
		this.recordId = recordId;
		this.equipmentRecord = equipmentRecord;
		this.modelResource = modelResource;
		this.modelSource = modelSource;
		this.equipmentProperties = wrap(equipmentProperties);
		this.modelSummaryProperties = wrap(modelSummaryProperties);
		this.modelAttributeProperties = wrap(modelAttributeProperties);
	}

	public String getDatasetName() {
		return datasetName;
	}

	public String getLayerCaption() {
		return layerCaption;
	}

	public long getRecordId() {
		return recordId;
	}

	public OneModelEquipmentRecord getEquipmentRecord() {
		return equipmentRecord;
	}

	public OneModelModelResource getModelResource() {
		return modelResource;
	}

	public String getModelSource() {
		return modelSource;
	}

	public Map<String, String> getEquipmentProperties() {
		return equipmentProperties;
	}

	public Map<String, String> getModelSummaryProperties() {
		return modelSummaryProperties;
	}

	public Map<String, String> getModelAttributeProperties() {
		return modelAttributeProperties;
	}

	public String getDisplayName() {
		if (equipmentRecord == null) {
			return "未选中设备";
		}
		String name = trimToEmpty(equipmentRecord.getEquipmentName());
		String code = trimToEmpty(equipmentRecord.getEquipmentCode());
		String id = trimToEmpty(equipmentRecord.getEquipmentId());
		if (!name.isEmpty()) {
			return code.isEmpty() ? name : name + " [" + code + "]";
		}
		if (!code.isEmpty()) {
			return code;
		}
		return id.isEmpty() ? "未命名设备" : id;
	}

	private Map<String, String> wrap(Map<String, String> source) {
		if (source == null || source.isEmpty()) {
			return Collections.emptyMap();
		}
		return Collections.unmodifiableMap(new LinkedHashMap<>(source));
	}

	private String trimToEmpty(String value) {
		return value == null ? "" : value.trim();
	}
}

