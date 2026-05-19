package com.aircas.onemodel.service;

import com.supermap.data.Dataset;
import com.supermap.data.DatasetType;
import com.supermap.data.DatasetVector;
import com.supermap.data.DatasetVectorInfo;
import com.supermap.data.Datasource;
import com.supermap.data.FieldInfo;
import com.supermap.data.FieldType;

/**
 * OneModel 数据集结构初始化。
 */
public class OneModelSchemaService {

	public static final String AREA_DATASET = "OM_AREA_A";
	public static final String EQUIPMENT_DATASET_PREFIX = "OM_EQUIP_";
	public static final String EQUIPMENT_DATASET_SUFFIX = "_P";
	public static final String CONNECTION_DATASET = "OM_CONN_L";
	public static final String MAP_NAME = "OneModel_Mainline_Map";

	private final OneModelSessionStore sessionStore = OneModelSessionStore.getInstance();
	private final OneModelCoordinateSystemSupport coordinateSystemSupport = new OneModelCoordinateSystemSupport();

	public DatasetVector ensureAreaDataset(Datasource datasource) {
		DatasetVector dataset = ensureDataset(datasource, AREA_DATASET, DatasetType.REGION);
		ensureTextField(dataset, "AREA_ID", 64);
		ensureTextField(dataset, "AREA_NAME", 128);
		ensureTextField(dataset, "AREA_TYPE", 64);
		ensureTextField(dataset, "VERSION_ID", 32);
		ensureDoubleField(dataset, "MIN_X");
		ensureDoubleField(dataset, "MIN_Y");
		ensureDoubleField(dataset, "MAX_X");
		ensureDoubleField(dataset, "MAX_Y");
		coordinateSystemSupport.applyToDatasetQuietly(dataset, resolveCoordinateSystemCode());
		return dataset;
	}

	public DatasetVector ensureEquipmentDataset(Datasource datasource, String modelCategory) {
		String normalizedKey = normalizeEquipmentCategoryKey(modelCategory);
		if (normalizedKey.isEmpty()) {
			throw new IllegalArgumentException("模型类别不能为空，无法建设备点数据集。");
		}
		String datasetName = EQUIPMENT_DATASET_PREFIX + normalizedKey + EQUIPMENT_DATASET_SUFFIX;
		DatasetVector dataset = ensureDataset(datasource, datasetName, DatasetType.POINT);
		ensureEquipmentFields(dataset);
		coordinateSystemSupport.applyToDatasetQuietly(dataset, resolveCoordinateSystemCode());
		return dataset;
	}

	public java.util.List<DatasetVector> listManagedEquipmentDatasets(Datasource datasource) {
		java.util.List<DatasetVector> result = new java.util.ArrayList<>();
		if (datasource == null) {
			return result;
		}
		for (int i = 0; i < datasource.getDatasets().getCount(); i++) {
			Dataset dataset = datasource.getDatasets().get(i);
			if (dataset instanceof DatasetVector && isManagedEquipmentDataset(dataset.getName())) {
				DatasetVector vector = (DatasetVector) dataset;
				ensureEquipmentFields(vector);
				result.add(vector);
			}
		}
		return result;
	}

	public boolean isManagedEquipmentDataset(String datasetName) {
		if (datasetName == null || datasetName.trim().isEmpty()) {
			return false;
		}
		String value = datasetName.trim();
		return value.startsWith(EQUIPMENT_DATASET_PREFIX) && value.endsWith(EQUIPMENT_DATASET_SUFFIX);
	}

	public String normalizeEquipmentCategoryKey(String modelCategory) {
		String value = modelCategory == null ? "" : modelCategory.trim();
		if (value.isEmpty()) {
			return "";
		}
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < value.length(); i++) {
			char ch = value.charAt(i);
			if (Character.isLetterOrDigit(ch)) {
				builder.append(Character.toUpperCase(ch));
			} else if (builder.length() > 0 && builder.charAt(builder.length() - 1) != '_') {
				builder.append('_');
			}
		}
		String sanitized = builder.toString().replaceAll("_+", "_").replaceAll("^_|_$", "");
		if (!sanitized.isEmpty()) {
			return sanitized;
		}
		return "CAT_" + Integer.toHexString(value.hashCode()).toUpperCase(java.util.Locale.ROOT);
	}

	private void ensureEquipmentFields(DatasetVector dataset) {
		ensureTextField(dataset, "EQUIP_ID", 64);
		ensureTextField(dataset, "EQUIP_CODE", 64);
		ensureTextField(dataset, "AREA_ID", 64);
		ensureTextField(dataset, "EQUIP_NAME", 128);
		ensureTextField(dataset, "EQUIP_TYPE", 64);
		ensureTextField(dataset, "MODEL_CAT_KEY", 64);
		ensureTextField(dataset, "STATUS", 32);
		ensureTextField(dataset, "VERSION_ID", 32);
		ensureTextField(dataset, "GRAPHIC_TP", 64);
		ensureTextField(dataset, "MODEL_ID", 64);
		ensureTextField(dataset, "MODEL_NAME", 128);
		ensureTextField(dataset, "MODEL_PATH", 512);
		ensureTextField(dataset, "MODEL_ATTRS", 4096);
		ensureDoubleField(dataset, "PX");
		ensureDoubleField(dataset, "PY");
	}

	public DatasetVector ensureConnectionDataset(Datasource datasource) {
		DatasetVector dataset = ensureDataset(datasource, CONNECTION_DATASET, DatasetType.LINE);
		ensureTextField(dataset, "CONN_ID", 64);
		ensureTextField(dataset, "FROM_ID", 64);
		ensureTextField(dataset, "TO_ID", 64);
		ensureTextField(dataset, "CONN_TP", 64);
		ensureTextField(dataset, "STATUS", 32);
		ensureTextField(dataset, "VERSION_ID", 32);
		ensureTextField(dataset, "GRAPHIC_TP", 64);
		coordinateSystemSupport.applyToDatasetQuietly(dataset, resolveCoordinateSystemCode());
		return dataset;
	}

	private DatasetVector ensureDataset(Datasource datasource, String name, DatasetType type) {
		DatasetVector existing = (DatasetVector) datasource.getDatasets().get(name);
		if (existing != null) {
			return existing;
		}
		DatasetVectorInfo info = new DatasetVectorInfo();
		info.setName(name);
		info.setType(type);
		coordinateSystemSupport.applyToDatasetInfoQuietly(info, resolveCoordinateSystemCode());
		Dataset dataset = datasource.getDatasets().create(info);
		if (!(dataset instanceof DatasetVector)) {
			throw new IllegalStateException("无法创建数据集：" + name);
		}
		return (DatasetVector) dataset;
	}

	private String resolveCoordinateSystemCode() {
		String value = sessionStore.getParameters().getCoordinateSystemCode();
		return value == null || value.trim().isEmpty() ? "4490" : value.trim();
	}


	private void ensureTextField(DatasetVector dataset, String fieldName, int length) {
		if (hasField(dataset, fieldName)) {
			return;
		}
		FieldInfo fieldInfo = new FieldInfo(fieldName, FieldType.TEXT);
		fieldInfo.setMaxLength(length);
		dataset.getFieldInfos().add(fieldInfo);
	}

	private void ensureDoubleField(DatasetVector dataset, String fieldName) {
		if (hasField(dataset, fieldName)) {
			return;
		}
		FieldInfo fieldInfo = new FieldInfo(fieldName, FieldType.DOUBLE);
		dataset.getFieldInfos().add(fieldInfo);
	}

	private boolean hasField(DatasetVector dataset, String fieldName) {
		for (int i = 0; i < dataset.getFieldInfos().getCount(); i++) {
			FieldInfo fieldInfo = dataset.getFieldInfos().get(i);
			if (fieldName.equalsIgnoreCase(fieldInfo.getName())) {
				return true;
			}
		}
		return false;
	}
}

