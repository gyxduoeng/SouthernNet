package com.aircas.onemodel.service;

import com.aircas.onemodel.model.OneModelEquipmentRecord;
import com.aircas.onemodel.model.OneModelModelResource;
import com.aircas.onemodel.model.OneModelSelectedEquipmentContext;
import com.supermap.data.DatasetVector;
import com.supermap.data.Recordset;
import com.supermap.desktop.core.Application;
import com.supermap.desktop.core.Interface.IFormMap;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 当前选中设备读取服务。
 */
public class OneModelSelectedEquipmentService {

    private static final Pattern JSON_ENTRY_PATTERN = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(\"((?:\\\\.|[^\"])*)\"|[^,}]+)");

	private final OneModelSchemaService schemaService = new OneModelSchemaService();
	private final OneModelLayerCatalogService layerCatalogService = new OneModelLayerCatalogService();

	public LookupResult resolveCurrentSelection() {
		IFormMap formMap = resolveActiveFormMap();
		if (formMap == null || formMap.getMapControl() == null || formMap.getMapControl().getMap() == null) {
			return LookupResult.failure("请先打开工程地图。");
		}
		Object map = formMap.getMapControl().getMap();
		Object layers = invokeQuietly(map, "getLayers");
		int layerCount = intValue(invokeQuietly(layers, "getCount"));
		SelectionCandidate candidate = null;
		int selectedCount = 0;
		for (int i = 0; i < layerCount; i++) {
			Object layer = invokeQuietly(layers, "get", i);
			Object datasetObject = invokeQuietly(layer, "getDataset");
			if (!(datasetObject instanceof DatasetVector)) {
				continue;
			}
			DatasetVector dataset = (DatasetVector) datasetObject;
			if (!schemaService.isManagedEquipmentDataset(dataset.getName())) {
				continue;
			}
			Object selection = invokeQuietly(layer, "getSelection");
			int currentSelectionCount = intValue(invokeQuietly(selection, "getCount"));
			if (currentSelectionCount <= 0) {
				continue;
			}
			selectedCount += currentSelectionCount;
			if (candidate == null) {
				candidate = new SelectionCandidate(dataset, selection);
			}
		}
		if (selectedCount <= 0) {
			return LookupResult.failure("请先选中一个设备点。");
		}
		if (candidate == null) {
			return LookupResult.failure("请先选中一个设备点。");
		}
		if (selectedCount > 1) {
			return LookupResult.failure("请先只选中一个设备点。");
		}
		Recordset recordset = null;
		try {
			recordset = toSelectedRecordset(candidate.selection);
			if (recordset == null) {
				return LookupResult.failure("请先选中一个设备点。");
			}
			recordset.moveFirst();
			if (recordset.isEOF()) {
				return LookupResult.failure("请先选中一个设备点。");
			}
			OneModelSelectedEquipmentContext context = buildContext(candidate.dataset, recordset);
			recordset.moveNext();
			if (!recordset.isEOF()) {
				return LookupResult.failure("请先只选中一个设备点。");
			}
			return LookupResult.success(context);
		} catch (Exception ex) {
			return LookupResult.failure("读取当前选中设备失败：" + trimToEmpty(ex.getMessage(), "未知原因"));
		} finally {
			release(recordset);
		}
	}

	private OneModelSelectedEquipmentContext buildContext(DatasetVector dataset, Recordset recordset) {
		String datasetName = dataset == null ? "" : trimToEmpty(dataset.getName());
		String layerCaption = trimToEmpty(layerCatalogService.findLayerCaption(datasetName), datasetName);
		String equipmentType = trimToEmpty(stringValue(recordset, "EQUIP_TYPE"), layerCaption);
		Double pointX = firstNonNull(doubleValue(recordset, "PX"), firstNumber(invokeQuietly(recordset, "getGeometry"), "getX", "getCenterX"));
		Double pointY = firstNonNull(doubleValue(recordset, "PY"), firstNumber(invokeQuietly(recordset, "getGeometry"), "getY", "getCenterY"));
		OneModelEquipmentRecord equipmentRecord = new OneModelEquipmentRecord(
				trimToEmpty(stringValue(recordset, "EQUIP_ID")),
				trimToEmpty(stringValue(recordset, "EQUIP_CODE")),
				trimToEmpty(stringValue(recordset, "AREA_ID")),
				trimToEmpty(stringValue(recordset, "EQUIP_NAME")),
				equipmentType,
				trimToEmpty(stringValue(recordset, "MODEL_CAT_KEY")),
				trimToEmpty(stringValue(recordset, "STATUS")),
				trimToEmpty(stringValue(recordset, "VERSION_ID")),
				trimToEmpty(stringValue(recordset, "GRAPHIC_TP"), "设备点"),
				trimToEmpty(stringValue(recordset, "MODEL_ID")),
				trimToEmpty(stringValue(recordset, "MODEL_NAME")),
				trimToEmpty(stringValue(recordset, "MODEL_PATH")),
				trimToEmpty(stringValue(recordset, "MODEL_ATTRS")),
				pointX == null ? 0D : pointX,
				pointY == null ? 0D : pointY);

		OneModelModelResource layerDefaultModel = layerCatalogService.findLayerDefaultModel(datasetName, equipmentType);
		OneModelModelResource effectiveModel = resolveEffectiveModel(equipmentRecord, layerDefaultModel, equipmentType);
		String modelSource = equipmentRecord.hasModelBinding() || !isBlank(equipmentRecord.getModelPath())
				? "设备绑定"
				: layerDefaultModel != null ? "图层默认模型" : "未指定";
		Map<String, String> equipmentProperties = buildEquipmentProperties(datasetName, layerCaption, recordset, equipmentRecord, pointX, pointY);
		Map<String, String> modelSummaryProperties = buildModelSummaryProperties(modelSource, effectiveModel);
		Map<String, String> modelAttributeProperties = buildModelAttributeProperties(effectiveModel);
		long recordId = longValue(invokeQuietly(recordset, "getID"));
		return new OneModelSelectedEquipmentContext(datasetName, layerCaption, recordId, equipmentRecord, effectiveModel,
				modelSource, equipmentProperties, modelSummaryProperties, modelAttributeProperties);
	}

	private Map<String, String> buildEquipmentProperties(String datasetName, String layerCaption, Recordset recordset,
			OneModelEquipmentRecord equipmentRecord, Double pointX, Double pointY) {
		Map<String, String> properties = new LinkedHashMap<>();
		properties.put("图层", trimToEmpty(layerCaption, datasetName));
		properties.put("数据集", datasetName);
		properties.put("记录 ID", String.valueOf(longValue(invokeQuietly(recordset, "getID"))));
		properties.put("设备 ID", equipmentRecord.getEquipmentId());
		properties.put("设备编码", equipmentRecord.getEquipmentCode());
		properties.put("设备名称", equipmentRecord.getEquipmentName());
		properties.put("模型类别", equipmentRecord.getEquipmentType());
		properties.put("分类键", equipmentRecord.getModelCategoryKey());
		properties.put("区域 ID", equipmentRecord.getAreaId());
		properties.put("状态", equipmentRecord.getStatus());
		properties.put("版本号", equipmentRecord.getVersionId());
		properties.put("图元类型", equipmentRecord.getGraphicType());
		properties.put("X", pointX == null ? "" : String.valueOf(pointX));
		properties.put("Y", pointY == null ? "" : String.valueOf(pointY));
		return properties;
	}

	private Map<String, String> buildModelSummaryProperties(String modelSource, OneModelModelResource modelResource) {
		Map<String, String> properties = new LinkedHashMap<>();
		properties.put("模型来源", trimToEmpty(modelSource, "未指定"));
		if (modelResource == null) {
			properties.put("当前模型", "未指定模型");
			return properties;
		}
		properties.put("模型 ID", trimToEmpty(modelResource.getModelId()));
		properties.put("模型名称", trimToEmpty(modelResource.getModelName()));
		properties.put("模型类别", trimToEmpty(modelResource.getModelType()));
		properties.put("模型路径", trimToEmpty(modelResource.getModelPath()));
		return properties;
	}

	private Map<String, String> buildModelAttributeProperties(OneModelModelResource modelResource) {
		Map<String, String> properties = new LinkedHashMap<>();
		if (modelResource == null) {
			properties.put("提示", "当前设备还没有指定模型。");
			return properties;
		}
		properties.putAll(parseAttributePayload(modelResource.getModelAttributes()));
		if (properties.isEmpty()) {
			properties.put("提示", "当前模型没有可解析的属性摘要。");
		}
		properties.put("原始 MODEL_ATTRS", trimToEmpty(modelResource.getModelAttributes()));
		return properties;
	}

	private Map<String, String> parseAttributePayload(String payload) {
		Map<String, String> properties = new LinkedHashMap<>();
		if (isBlank(payload)) {
			return properties;
		}
		Matcher matcher = JSON_ENTRY_PATTERN.matcher(payload);
		while (matcher.find()) {
			String key = trimToEmpty(matcher.group(1));
			String quoted = matcher.group(3);
			String raw = matcher.group(2);
			String value = quoted != null ? unescapeJson(quoted) : trimToEmpty(raw);
			if (!key.isEmpty()) {
				properties.put(key, value);
			}
		}
		return properties;
	}

	private String unescapeJson(String value) {
		return value == null ? "" : value.replace("\\\"", "\"").replace("\\\\", "\\");
	}

	private OneModelModelResource resolveEffectiveModel(OneModelEquipmentRecord equipmentRecord,
			OneModelModelResource layerDefaultModel, String fallbackModelType) {
		boolean hasDirectBinding = equipmentRecord != null
				&& (!isBlank(equipmentRecord.getModelId()) || !isBlank(equipmentRecord.getModelName()) || !isBlank(equipmentRecord.getModelPath()));
		if (hasDirectBinding) {
			String modelPath = trimToEmpty(equipmentRecord.getModelPath());
			String modelName = trimToEmpty(equipmentRecord.getModelName(), deriveModelName(modelPath), "未命名模型");
			String modelType = trimToEmpty(fallbackModelType, layerDefaultModel == null ? "" : layerDefaultModel.getModelType(), "未分类");
			String modelId = trimToEmpty(equipmentRecord.getModelId(), "MR-" + Math.abs((modelName + "|" + modelPath).hashCode()));
			String modelAttrs = trimToEmpty(equipmentRecord.getModelAttributes(), layerDefaultModel == null ? "" : layerDefaultModel.getModelAttributes());
			return new OneModelModelResource(modelId, modelName, modelType, modelPath, modelAttrs);
		}
		return layerDefaultModel;
	}

	private String deriveModelName(String modelPath) {
		if (isBlank(modelPath)) {
			return "";
		}
		int slashIndex = Math.max(modelPath.lastIndexOf('/'), modelPath.lastIndexOf('\\'));
		return slashIndex >= 0 ? modelPath.substring(slashIndex + 1) : modelPath;
	}

	private Recordset toSelectedRecordset(Object selection) {
		Object recordset = invokeQuietly(selection, "toRecordset");
		return recordset instanceof Recordset ? (Recordset) recordset : null;
	}

	private IFormMap resolveActiveFormMap() {
		Object activeForm = Application.getActiveApplication().getActiveForm();
		return activeForm instanceof IFormMap ? (IFormMap) activeForm : null;
	}

	private String stringValue(Recordset recordset, String fieldName) {
		try {
			Object value = recordset.getObject(fieldName);
			return value == null ? null : String.valueOf(value).trim();
		} catch (Exception ignored) {
			return null;
		}
	}

	private Double doubleValue(Recordset recordset, String fieldName) {
		try {
			Object value = recordset.getObject(fieldName);
			return value instanceof Number ? ((Number) value).doubleValue() : null;
		} catch (Exception ignored) {
			return null;
		}
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

	private <T> T firstNonNull(T left, T right) {
		return left != null ? left : right;
	}

	private int intValue(Object value) {
		return value instanceof Number ? ((Number) value).intValue() : 0;
	}

	private long longValue(Object value) {
		return value instanceof Number ? ((Number) value).longValue() : -1L;
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	private String trimToEmpty(String value, String... fallbacks) {
		if (!isBlank(value)) {
			return value.trim();
		}
		if (fallbacks != null) {
			for (String fallback : fallbacks) {
				if (!isBlank(fallback)) {
					return fallback.trim();
				}
			}
		}
		return "";
	}

	private Object invokeQuietly(Object target, String methodName, Object... args) {
		if (target == null) {
			return null;
		}
		Class<?>[] parameterTypes = new Class<?>[args.length];
		for (int i = 0; i < args.length; i++) {
			if (args[i] instanceof Integer) {
				parameterTypes[i] = int.class;
			} else if (args[i] instanceof Boolean) {
				parameterTypes[i] = boolean.class;
			} else {
				parameterTypes[i] = args[i] == null ? Object.class : args[i].getClass();
			}
		}
		try {
			Method method = target.getClass().getMethod(methodName, parameterTypes);
			method.setAccessible(true);
			return method.invoke(target, args);
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

	private static final class SelectionCandidate {
		private final DatasetVector dataset;
		private final Object selection;

		private SelectionCandidate(DatasetVector dataset, Object selection) {
			this.dataset = dataset;
			this.selection = selection;
		}
	}

	public static final class LookupResult {
		private final OneModelSelectedEquipmentContext context;
		private final String message;

		private LookupResult(OneModelSelectedEquipmentContext context, String message) {
			this.context = context;
			this.message = message;
		}

		public static LookupResult success(OneModelSelectedEquipmentContext context) {
			return new LookupResult(context, "");
		}

		public static LookupResult failure(String message) {
			return new LookupResult(null, message == null ? "" : message.trim());
		}

		public boolean isSuccess() {
			return context != null;
		}

		public OneModelSelectedEquipmentContext getContext() {
			return context;
		}

		public String getMessage() {
			return message;
		}
	}
}



