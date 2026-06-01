package com.aircas.onemodel.service;

import com.aircas.onemodel.model.OneModelAreaRecord;
import com.aircas.onemodel.model.OneModelConnectionRecord;
import com.aircas.onemodel.model.OneModelEquipmentRecord;
import com.aircas.onemodel.model.OneModelLayerOption;
import com.aircas.onemodel.model.OneModelModelResource;
import com.supermap.data.CursorType;
import com.supermap.data.DatasetVector;
import com.supermap.data.Datasource;
import com.supermap.data.GeoLine;
import com.supermap.data.GeoPoint;
import com.supermap.data.GeoRegion;
import com.supermap.data.Geometry;
import com.supermap.data.Point2D;
import com.supermap.data.Point2Ds;
import com.supermap.data.Recordset;
import com.supermap.data.Rectangle2D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OneModel 主线地图对象仓储。
 */
public class OneModelMapRepository {

	private final OneModelWorkspaceBridge workspaceBridge = new OneModelWorkspaceBridge();
	private final OneModelSchemaService schemaService = new OneModelSchemaService();
	private final OneModelMapBridge mapBridge = new OneModelMapBridge();
	private final OneModelLayerCatalogService layerCatalogService = new OneModelLayerCatalogService();
	private final OneModelSessionStore sessionStore = OneModelSessionStore.getInstance();

	public void initializeRuntimeSchema() {
		Datasource datasource = workspaceBridge.getOrCreateSharedDatasource();
		schemaService.ensureAreaDataset(datasource);
		schemaService.ensureConnectionDataset(datasource);
		mapBridge.ensureManagedLayersPresent(datasource);
		workspaceBridge.saveWorkspaceQuietly();
	}

	public String createEquipmentLayer(String modelCategory) {
		return createEquipmentLayer(modelCategory, null);
	}

	public String createEquipmentLayer(String modelCategory, OneModelModelResource defaultModel) {
		initializeRuntimeSchema();
		String category = modelCategory == null ? "" : modelCategory.trim();
		if (category.isEmpty()) {
			throw new IllegalArgumentException("模型类别不能为空。");
		}
		Datasource datasource = workspaceBridge.getOrCreateSharedDatasource();
		DatasetVector dataset = schemaService.ensureEquipmentDataset(datasource, category);
		layerCatalogService.registerLayerCaption(dataset.getName(), category);
		layerCatalogService.registerLayerDefaultModel(dataset.getName(), defaultModel, category);
		mapBridge.ensureManagedLayersPresent(datasource);
		mapBridge.activateEditableLayer(dataset.getName());
		workspaceBridge.saveWorkspaceQuietly();
		return dataset.getName();
	}

	public List<OneModelLayerOption> listEquipmentLayers() {
		initializeRuntimeSchema();
		List<OneModelLayerOption> result = new ArrayList<>();
		for (DatasetVector dataset : schemaService.listManagedEquipmentDatasets(workspaceBridge.getOrCreateSharedDatasource())) {
			String caption = layerCatalogService.findLayerCaption(dataset.getName());
			result.add(new OneModelLayerOption(dataset.getName(), caption == null || caption.trim().isEmpty() ? dataset.getName() : caption));
		}
		return result;
	}

	public OneModelAreaRecord addArea(String areaName, String areaType, double minX, double minY, double maxX, double maxY) {
		initializeRuntimeSchema();
		String areaId = nextId("AR");
		String versionId = sessionStore.getParameters().getVersionId();
		OneModelAreaRecord areaRecord = new OneModelAreaRecord(areaId, areaName, areaType, versionId, minX, minY, maxX, maxY);
		DatasetVector dataset = schemaService.ensureAreaDataset(workspaceBridge.getOrCreateSharedDatasource());
		Recordset recordset = dataset.getRecordset(false, CursorType.DYNAMIC);
		try {
			recordset.addNew(buildAreaGeometry(areaRecord), buildAreaAttributes(areaRecord));
			recordset.update();
		} finally {
			release(recordset);
		}
		workspaceBridge.saveWorkspaceQuietly();
		return areaRecord;
	}

	public OneModelEquipmentRecord addEquipment(String areaId, String equipmentName, String equipmentType, String status, double x, double y) {
		return addEquipment(areaId, equipmentName, equipmentType, null, status, x, y);
	}

	public OneModelEquipmentRecord addEquipment(String areaId, String equipmentName, String equipmentType,
			OneModelModelResource resource, String status, double x, double y) {
		initializeRuntimeSchema();
		String modelCategory = equipmentType == null ? "" : equipmentType.trim();
		if (modelCategory.isEmpty()) {
			throw new IllegalArgumentException("模型类别不能为空。");
		}
		String equipmentId = nextId("EQ");
		String equipmentCode = equipmentName == null || equipmentName.trim().isEmpty() ? equipmentId : equipmentName.trim();
		String versionId = sessionStore.getParameters().getVersionId();
		String categoryKey = schemaService.normalizeEquipmentCategoryKey(modelCategory);
		OneModelEquipmentRecord record = new OneModelEquipmentRecord(equipmentId, equipmentCode, areaId, equipmentName, equipmentType,
				categoryKey, status, versionId, "设备点",
				resource == null ? "" : resource.getModelId(),
				resource == null ? "" : resource.getModelName(),
				resource == null ? "" : resource.getModelPath(),
				resource == null ? "" : resource.getModelAttributes(),
				x, y);
		Datasource datasource = workspaceBridge.getOrCreateSharedDatasource();
		DatasetVector dataset = schemaService.ensureEquipmentDataset(datasource, modelCategory);
		Recordset recordset = dataset.getRecordset(false, CursorType.DYNAMIC);
		try {
			recordset.addNew(new GeoPoint(x, y), buildEquipmentAttributes(record));
			recordset.update();
		} finally {
			release(recordset);
		}
		mapBridge.ensureManagedLayersPresent(datasource);
		mapBridge.activateEditableLayer(dataset.getName());
		workspaceBridge.saveWorkspaceQuietly();
		return record;
	}

	public OneModelEquipmentRecord addEquipmentToLayer(String datasetName, String areaId, String equipmentName, String equipmentType,
			OneModelModelResource resource, String status, String graphicType, double x, double y) {
		initializeRuntimeSchema();
		String targetDatasetName = datasetName == null ? "" : datasetName.trim();
		if (!schemaService.isManagedEquipmentDataset(targetDatasetName)) {
			throw new IllegalArgumentException("目标设备图层不是 OneModel 管理图层：" + targetDatasetName);
		}
		String category = equipmentType == null ? "" : equipmentType.trim();
		if (category.isEmpty()) {
			throw new IllegalArgumentException("设备类型不能为空。");
		}
		Datasource datasource = workspaceBridge.getOrCreateSharedDatasource();
		schemaService.listManagedEquipmentDatasets(datasource);
		DatasetVector dataset = (DatasetVector) datasource.getDatasets().get(targetDatasetName);
		if (dataset == null) {
			throw new IllegalArgumentException("目标设备图层不存在：" + targetDatasetName);
		}
		String equipmentId = nextId("EQ");
		String equipmentCode = equipmentName == null || equipmentName.trim().isEmpty() ? equipmentId : equipmentName.trim();
		String versionId = sessionStore.getParameters().getVersionId();
		String categoryKey = schemaService.normalizeEquipmentCategoryKey(category);
		String state = isBlank(status) ? "现状" : status.trim();
		String graphic = isBlank(graphicType) ? "设备点" : graphicType.trim();
		OneModelEquipmentRecord record = new OneModelEquipmentRecord(equipmentId, equipmentCode, areaId, equipmentName, category,
				categoryKey, state, versionId, graphic,
				resource == null ? "" : resource.getModelId(),
				resource == null ? "" : resource.getModelName(),
				resource == null ? "" : resource.getModelPath(),
				resource == null ? "" : resource.getModelAttributes(),
				x, y);
		Recordset recordset = dataset.getRecordset(false, CursorType.DYNAMIC);
		try {
			recordset.addNew(new GeoPoint(x, y), buildEquipmentAttributes(record));
			recordset.update();
		} finally {
			release(recordset);
		}
		mapBridge.ensureManagedLayersPresent(datasource);
		mapBridge.activateEditableLayer(dataset.getName());
		workspaceBridge.saveWorkspaceQuietly();
		return record;
	}
	public OneModelConnectionRecord addConnection(String fromEquipmentId, String toEquipmentId, String connectionType, String status) {
		initializeRuntimeSchema();
		String type = connectionTypeOrDefault(connectionType);
		String state = isBlank(status) ? "现状" : status.trim();
		if (isBlank(fromEquipmentId) || isBlank(toEquipmentId)) {
			throw new IllegalArgumentException("请选择关联关系两端设备。");
		}
		if (fromEquipmentId.trim().equals(toEquipmentId.trim())) {
			throw new IllegalArgumentException("关联关系的两端设备不能相同。");
		}
		boolean directed = isDirectedConnectionType(type);
		String leftEquipmentId = fromEquipmentId.trim();
		String rightEquipmentId = toEquipmentId.trim();
		if (!directed && leftEquipmentId.compareTo(rightEquipmentId) > 0) {
			leftEquipmentId = toEquipmentId.trim();
			rightEquipmentId = fromEquipmentId.trim();
		}
		OneModelEquipmentRecord from = getEquipmentById(leftEquipmentId);
		OneModelEquipmentRecord to = getEquipmentById(rightEquipmentId);
		if (from == null || to == null) {
			throw new IllegalArgumentException("关联关系两端设备不存在。");
		}
		String targetPair = normalizePair(from.getEquipmentId(), to.getEquipmentId(), type, directed);
		for (OneModelConnectionRecord existing : listConnections()) {
			String existingPair = normalizePair(existing.getFromEquipmentId(), existing.getToEquipmentId(), existing.getConnectionType(),
					isDirectedConnectionType(existing.getConnectionType()));
			if (existingPair.equals(targetPair)) {
				throw new IllegalArgumentException("相同类型的关联关系已存在，无需重复创建。");
			}
		}
		String connectionId = nextId("CN");
		String versionId = sessionStore.getParameters().getVersionId();
		OneModelConnectionRecord record = new OneModelConnectionRecord(connectionId, from.getEquipmentId(), to.getEquipmentId(),
				type, state, versionId, "关联关系线");
		Datasource datasource = workspaceBridge.getOrCreateSharedDatasource();
		DatasetVector dataset = schemaService.ensureConnectionDataset(datasource);
		Recordset recordset = dataset.getRecordset(false, CursorType.DYNAMIC);
		try {
			recordset.addNew(buildLineGeometry(from, to), buildConnectionAttributes(record));
			recordset.update();
		} finally {
			release(recordset);
		}
		mapBridge.ensureManagedLayersPresent(datasource);
		mapBridge.refreshActiveMap();
		workspaceBridge.saveWorkspaceQuietly();
		return record;
	}
	public void updateGraphicBinding(String equipmentId, String graphicType) {
		for (DatasetVector dataset : schemaService.listManagedEquipmentDatasets(workspaceBridge.getOrCreateSharedDatasource())) {
			Recordset recordset = dataset.getRecordset(false, CursorType.DYNAMIC);
			try {
				if (updateTextField(recordset, equipmentId, "GRAPHIC_TP", graphicType)) {
					break;
				}
			} finally {
				release(recordset);
			}
		}
		workspaceBridge.saveWorkspaceQuietly();
	}

	public void updateModelBinding(String equipmentId, OneModelModelResource resource) {
		for (DatasetVector dataset : schemaService.listManagedEquipmentDatasets(workspaceBridge.getOrCreateSharedDatasource())) {
			Recordset recordset = dataset.getRecordset(false, CursorType.DYNAMIC);
			try {
				recordset.moveFirst();
				while (!recordset.isEOF()) {
					if (equipmentId.equals(recordset.getString("EQUIP_ID"))) {
						recordset.edit();
						recordset.setObject("MODEL_ID", resource.getModelId());
						recordset.setObject("MODEL_NAME", resource.getModelName());
						recordset.setObject("MODEL_PATH", resource.getModelPath());
						recordset.setObject("MODEL_ATTRS", resource.getModelAttributes());
						recordset.update();
						break;
					}
					recordset.moveNext();
				}
			} finally {
				release(recordset);
			}
		}
		workspaceBridge.saveWorkspaceQuietly();
	}

	public List<OneModelAreaRecord> listAreas() {
		initializeRuntimeSchema();
		List<OneModelAreaRecord> result = new ArrayList<>();
		Recordset recordset = schemaService.ensureAreaDataset(workspaceBridge.getOrCreateSharedDatasource()).getRecordset(false, CursorType.STATIC);
		try {
			recordset.moveFirst();
			while (!recordset.isEOF()) {
				result.add(new OneModelAreaRecord(
						recordset.getString("AREA_ID"),
						recordset.getString("AREA_NAME"),
						recordset.getString("AREA_TYPE"),
						recordset.getString("VERSION_ID"),
						recordset.getDouble("MIN_X"),
						recordset.getDouble("MIN_Y"),
						recordset.getDouble("MAX_X"),
						recordset.getDouble("MAX_Y")));
				recordset.moveNext();
			}
		} finally {
			release(recordset);
		}
		return result;
	}

	public List<OneModelEquipmentRecord> listEquipments() {
		initializeRuntimeSchema();
		List<OneModelEquipmentRecord> result = new ArrayList<>();
		for (DatasetVector dataset : schemaService.listManagedEquipmentDatasets(workspaceBridge.getOrCreateSharedDatasource())) {
			Recordset recordset = dataset.getRecordset(false, CursorType.STATIC);
			try {
				recordset.moveFirst();
				while (!recordset.isEOF()) {
					Point point = readPoint(recordset.getGeometry());
					double x = coordinateValue(recordset, "PX", point == null ? null : point.x);
					double y = coordinateValue(recordset, "PY", point == null ? null : point.y);
					if (point != null && x == 0D && y == 0D && (point.x != 0D || point.y != 0D)) {
						x = point.x;
						y = point.y;
					}
					result.add(new OneModelEquipmentRecord(
							recordset.getString("EQUIP_ID"),
							recordset.getString("EQUIP_CODE"),
							recordset.getString("AREA_ID"),
							recordset.getString("EQUIP_NAME"),
							recordset.getString("EQUIP_TYPE"),
							recordset.getString("MODEL_CAT_KEY"),
							recordset.getString("STATUS"),
							recordset.getString("VERSION_ID"),
							recordset.getString("GRAPHIC_TP"),
							recordset.getString("MODEL_ID"),
							recordset.getString("MODEL_NAME"),
							recordset.getString("MODEL_PATH"),
							recordset.getString("MODEL_ATTRS"),
							x,
							y));
					recordset.moveNext();
				}
			} finally {
				release(recordset);
			}
		}
		return result;
	}
	public List<OneModelConnectionRecord> listConnections() {
		initializeRuntimeSchema();
		List<OneModelConnectionRecord> result = new ArrayList<>();
		Recordset recordset = schemaService.ensureConnectionDataset(workspaceBridge.getOrCreateSharedDatasource()).getRecordset(false, CursorType.STATIC);
		try {
			recordset.moveFirst();
			while (!recordset.isEOF()) {
				result.add(new OneModelConnectionRecord(
						recordset.getString("CONN_ID"),
						recordset.getString("FROM_ID"),
						recordset.getString("TO_ID"),
						recordset.getString("CONN_TP"),
						recordset.getString("STATUS"),
						recordset.getString("VERSION_ID"),
						recordset.getString("GRAPHIC_TP")));
				recordset.moveNext();
			}
		} finally {
			release(recordset);
		}
		return result;
	}

	public OneModelEquipmentRecord getEquipmentById(String equipmentId) {
		for (OneModelEquipmentRecord record : listEquipments()) {
			if (record.getEquipmentId().equals(equipmentId)) {
				return record;
			}
		}
		return null;
	}

	private double coordinateValue(Recordset recordset, String fieldName, Double fallback) {
		try {
			Object value = recordset.getObject(fieldName);
			if (value instanceof Number) {
				return ((Number) value).doubleValue();
			}
			if (value != null && !String.valueOf(value).trim().isEmpty()) {
				return Double.parseDouble(String.valueOf(value).trim());
			}
		} catch (Exception ignored) {
		}
		return fallback == null ? 0D : fallback.doubleValue();
	}

	private Point readPoint(Geometry geometry) {
		if (geometry == null) {
			return null;
		}
		try {
			Rectangle2D bounds = geometry.getBounds();
			if (bounds == null) {
				return null;
			}
			return new Point((bounds.getLeft() + bounds.getRight()) / 2D, (bounds.getBottom() + bounds.getTop()) / 2D);
		} catch (Exception ignored) {
			return null;
		}
	}
	private Map<String, Object> buildAreaAttributes(OneModelAreaRecord record) {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("AREA_ID", record.getAreaId());
		attributes.put("AREA_NAME", record.getAreaName());
		attributes.put("AREA_TYPE", record.getAreaType());
		attributes.put("VERSION_ID", record.getVersionId());
		attributes.put("MIN_X", record.getMinX());
		attributes.put("MIN_Y", record.getMinY());
		attributes.put("MAX_X", record.getMaxX());
		attributes.put("MAX_Y", record.getMaxY());
		return attributes;
	}

	private Map<String, Object> buildEquipmentAttributes(OneModelEquipmentRecord record) {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("EQUIP_ID", record.getEquipmentId());
		attributes.put("EQUIP_CODE", record.getEquipmentCode());
		attributes.put("AREA_ID", record.getAreaId());
		attributes.put("EQUIP_NAME", record.getEquipmentName());
		attributes.put("EQUIP_TYPE", record.getEquipmentType());
		attributes.put("MODEL_CAT_KEY", record.getModelCategoryKey());
		attributes.put("STATUS", record.getStatus());
		attributes.put("VERSION_ID", record.getVersionId());
		attributes.put("GRAPHIC_TP", record.getGraphicType());
		attributes.put("MODEL_ID", record.getModelId());
		attributes.put("MODEL_NAME", record.getModelName());
		attributes.put("MODEL_PATH", record.getModelPath());
		attributes.put("MODEL_ATTRS", record.getModelAttributes());
		attributes.put("PX", record.getX());
		attributes.put("PY", record.getY());
		return attributes;
	}

	private Map<String, Object> buildConnectionAttributes(OneModelConnectionRecord record) {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("CONN_ID", record.getConnectionId());
		attributes.put("FROM_ID", record.getFromEquipmentId());
		attributes.put("TO_ID", record.getToEquipmentId());
		attributes.put("CONN_TP", record.getConnectionType());
		attributes.put("STATUS", record.getStatus());
		attributes.put("VERSION_ID", record.getVersionId());
		attributes.put("GRAPHIC_TP", record.getGraphicType());
		return attributes;
	}

	private Geometry buildAreaGeometry(OneModelAreaRecord record) {
		Point2Ds points = new Point2Ds();
		points.add(new Point2D(record.getMinX(), record.getMinY()));
		points.add(new Point2D(record.getMaxX(), record.getMinY()));
		points.add(new Point2D(record.getMaxX(), record.getMaxY()));
		points.add(new Point2D(record.getMinX(), record.getMaxY()));
		points.add(new Point2D(record.getMinX(), record.getMinY()));
		return new GeoRegion(points);
	}

	private Geometry buildLineGeometry(OneModelEquipmentRecord from, OneModelEquipmentRecord to) {
		Point2Ds points = new Point2Ds();
		points.add(new Point2D(from.getX(), from.getY()));
		points.add(new Point2D(to.getX(), to.getY()));
		// V1：关联关系的线几何统一自动生成直线，后续再评估手工折线编辑。
		return new GeoLine(points);
	}

	private String normalizePair(String fromId, String toId, String type, boolean directed) {
		String left = fromId == null ? "" : fromId;
		String right = toId == null ? "" : toId;
		String relationType = connectionTypeOrDefault(type);
		if (directed) {
			return left + "|" + right + "|" + relationType;
		}
		return left.compareTo(right) <= 0 ? left + "|" + right + "|" + relationType : right + "|" + left + "|" + relationType;
	}

	private String connectionTypeOrDefault(String type) {
		String value = type == null ? "" : type.trim();
		return value.isEmpty() ? "电气连接" : value;
	}

	private boolean isDirectedConnectionType(String type) {
		return "从属关系".equals(connectionTypeOrDefault(type));
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}
	private boolean updateTextField(Recordset recordset, String id, String targetField, String value) {
		recordset.moveFirst();
		while (!recordset.isEOF()) {
			if (id.equals(recordset.getString("EQUIP_ID"))) {
				recordset.edit();
				recordset.setObject(targetField, value);
				recordset.update();
				return true;
			}
			recordset.moveNext();
		}
		return false;
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

	private String nextId(String prefix) {
		return prefix + "-" + System.currentTimeMillis() + "-" + Long.toHexString(System.nanoTime());
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




