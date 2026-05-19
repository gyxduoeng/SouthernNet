package com.aircas.onemodel.service;

import com.aircas.onemodel.model.OneModelLayerOption;
import com.aircas.onemodel.model.OneModelModelResource;
import com.supermap.data.CursorType;
import com.supermap.data.DatasetVector;
import com.supermap.data.Recordset;

import java.lang.reflect.Method;

/**
 * 设备属性初始化/补录服务。
 */
public class OneModelEquipmentAttributeService {

	private final OneModelWorkspaceBridge workspaceBridge = new OneModelWorkspaceBridge();
	private final OneModelSchemaService schemaService = new OneModelSchemaService();
	private final OneModelLayerCatalogService layerCatalogService = new OneModelLayerCatalogService();
	private final OneModelSessionStore sessionStore = OneModelSessionStore.getInstance();

	public String initializeAttributes(OneModelLayerOption layerOption, String modelCategory, OneModelModelResource resource,
			String namePrefix, String codePrefix, String status, boolean overwriteExisting) {
		if (layerOption == null || layerOption.getDatasetName() == null || layerOption.getDatasetName().trim().isEmpty()) {
			throw new IllegalArgumentException("请先选择要初始化的设备图层。");
		}
		String category = modelCategory == null || modelCategory.trim().isEmpty() ? layerOption.getCaption() : modelCategory.trim();
		if (category == null || category.trim().isEmpty()) {
			throw new IllegalArgumentException("模型类别不能为空。");
		}
		DatasetVector dataset = (DatasetVector) workspaceBridge.getOrCreateSharedDatasource().getDatasets().get(layerOption.getDatasetName());
		if (dataset == null) {
			throw new IllegalStateException("未找到设备数据集：" + layerOption.getDatasetName());
		}
		Recordset recordset = dataset.getRecordset(false, CursorType.DYNAMIC);
		int updated = 0;
		int skipped = 0;
		String versionId = sessionStore.getParameters().getVersionId();
		String categoryKey = schemaService.normalizeEquipmentCategoryKey(category);
		String actualStatus = status == null || status.trim().isEmpty() ? "现状" : status.trim();
		String actualNamePrefix = namePrefix == null || namePrefix.trim().isEmpty() ? category : namePrefix.trim();
		String actualCodePrefix = codePrefix == null || codePrefix.trim().isEmpty() ? categoryKey : codePrefix.trim();
		OneModelModelResource effectiveResource = resolveEffectiveResource(layerOption, category, resource);
		try {
			recordset.moveFirst();
			int index = 1;
			while (!recordset.isEOF()) {
				boolean changed = false;
				String equipId = stringValue(recordset, "EQUIP_ID");
				String equipCode = stringValue(recordset, "EQUIP_CODE");
				String equipName = stringValue(recordset, "EQUIP_NAME");
				recordset.edit();
				if (overwriteExisting || isBlank(equipId)) {
					recordset.setObject("EQUIP_ID", "EQ-" + System.currentTimeMillis() + "-" + index);
					changed = true;
				}
				if (overwriteExisting || isBlank(equipCode)) {
					recordset.setObject("EQUIP_CODE", actualCodePrefix + "-" + index);
					changed = true;
				}
				if (overwriteExisting || isBlank(equipName)) {
					recordset.setObject("EQUIP_NAME", actualNamePrefix + "-" + index);
					changed = true;
				}
				if (overwriteExisting || isBlank(stringValue(recordset, "EQUIP_TYPE"))) {
					recordset.setObject("EQUIP_TYPE", category);
					changed = true;
				}
				if (overwriteExisting || isBlank(stringValue(recordset, "MODEL_CAT_KEY"))) {
					recordset.setObject("MODEL_CAT_KEY", categoryKey);
					changed = true;
				}
				if (overwriteExisting || isBlank(stringValue(recordset, "STATUS"))) {
					recordset.setObject("STATUS", actualStatus);
					changed = true;
				}
				if (overwriteExisting || isBlank(stringValue(recordset, "VERSION_ID"))) {
					recordset.setObject("VERSION_ID", versionId);
					changed = true;
				}
				if (overwriteExisting || isBlank(stringValue(recordset, "GRAPHIC_TP"))) {
					recordset.setObject("GRAPHIC_TP", "设备点");
					changed = true;
				}
				Point point = readPoint(recordset);
				if (point != null) {
					recordset.setObject("PX", point.x);
					recordset.setObject("PY", point.y);
					changed = true;
				}
				if (effectiveResource != null) {
					if (overwriteExisting
							|| isBlank(stringValue(recordset, "MODEL_ID"))
							|| isBlank(stringValue(recordset, "MODEL_NAME"))
							|| isBlank(stringValue(recordset, "MODEL_PATH"))
							|| isBlank(stringValue(recordset, "MODEL_ATTRS"))) {
						recordset.setObject("MODEL_ID", effectiveResource.getModelId());
						recordset.setObject("MODEL_NAME", effectiveResource.getModelName());
						recordset.setObject("MODEL_PATH", effectiveResource.getModelPath());
						recordset.setObject("MODEL_ATTRS", effectiveResource.getModelAttributes());
						changed = true;
					}
				}
				if (changed) {
					recordset.update();
					updated++;
				} else {
					invokeQuietly(recordset, "cancelEdit");
					skipped++;
				}
				recordset.moveNext();
				index++;
			}
		} finally {
			release(recordset);
		}
		workspaceBridge.saveWorkspaceQuietly();
		return "设备属性初始化完成\n\n"
				+ "图层：" + layerOption.getCaption() + "\n"
				+ "数据集：" + layerOption.getDatasetName() + "\n"
				+ "模型类别：" + category + "\n"
				+ "更新记录数：" + updated + "\n"
				+ "未改动记录数：" + skipped + "\n"
				+ (effectiveResource == null ? "未指定具体模型。\n" : "已写入模型：" + effectiveResource.getModelName() + "\n");
	}

	public OneModelModelResource findLayerDefaultModel(OneModelLayerOption layerOption, String modelCategory) {
		if (layerOption == null || isBlank(layerOption.getDatasetName())) {
			return null;
		}
		return layerCatalogService.findLayerDefaultModel(layerOption.getDatasetName(), modelCategory);
	}

	private OneModelModelResource resolveEffectiveResource(OneModelLayerOption layerOption, String modelCategory, OneModelModelResource resource) {
		if (resource != null) {
			return resource;
		}
		return findLayerDefaultModel(layerOption, modelCategory);
	}

	private Point readPoint(Recordset recordset) {
		Object geometry = invokeQuietly(recordset, "getGeometry");
		if (geometry == null) {
			return null;
		}
		Double x = firstNumber(geometry, "getX", "getCenterX");
		Double y = firstNumber(geometry, "getY", "getCenterY");
		return x == null || y == null ? null : new Point(x, y);
	}

	private Double firstNumber(Object target, String... methodNames) {
		for (String methodName : methodNames) {
			Object value = invokeQuietly(target, methodName);
			if (value instanceof Number) {
				return ((Number) value).doubleValue();
			}
		}
		return null;
	}

	private String stringValue(Recordset recordset, String fieldName) {
		try {
			Object value = recordset.getObject(fieldName);
			if (value == null) {
				return null;
			}
			String text = String.valueOf(value).trim();
			return text.isEmpty() ? null : text;
		} catch (Exception ignored) {
			return null;
		}
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	private Object invokeQuietly(Object target, String methodName) {
		if (target == null) {
			return null;
		}
		try {
			Method method = target.getClass().getMethod(methodName);
			method.setAccessible(true);
			return method.invoke(target);
		} catch (Exception ignored) {
			return null;
		}
	}

	private void release(Recordset recordset) {
		if (recordset == null) {
			return;
		}
		try {
			recordset.close();
		} catch (Exception ignored) {
		}
		try {
			recordset.dispose();
		} catch (Exception ignored) {
		}
	}

	private static final class Point {
		private final double x;
		private final double y;

		private Point(double x, double y) {
			this.x = x;
			this.y = y;
		}
	}
}

