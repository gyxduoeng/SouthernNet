package com.aircas.onemodel.service;

import com.aircas.onemodel.model.OneModelAreaRecord;
import com.aircas.onemodel.model.OneModelEquipmentRecord;
import com.supermap.data.CursorType;
import com.supermap.data.DatasetVector;
import com.supermap.data.Datasource;
import com.supermap.data.Recordset;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 第二版“读取当前地图”同步服务。
 */
public class OneModelCurrentMapImportService {

	private static final double DEFAULT_MARGIN = 20.0D;

	private final OneModelActiveMapService activeMapService = new OneModelActiveMapService();
	private final OneModelMapRepository repository = new OneModelMapRepository();
	private final OneModelSchemaService schemaService = new OneModelSchemaService();
	private final OneModelWorkspaceBridge workspaceBridge = new OneModelWorkspaceBridge();
	private final OneModelProjectService projectService = new OneModelProjectService();

	public String importCurrentMap(ImportRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("导入请求不能为空。");
		}
		DatasetVector areaDataset = activeMapService.resolveDataset(request.getAreaDataset());
		DatasetVector equipmentDataset = activeMapService.resolveDataset(request.getEquipmentDataset());
		DatasetVector connectionDataset = activeMapService.resolveDataset(request.getConnectionDataset());
		if (areaDataset == null && equipmentDataset == null && connectionDataset == null) {
			throw new IllegalArgumentException("请至少选择一个要同步的数据集。");
		}

		repository.initializeRuntimeSchema();
		Datasource runtimeDatasource = workspaceBridge.getOrCreateSharedDatasource();
		if (request.isClearBeforeImport()) {
			clearRuntimeData(runtimeDatasource);
		}

		ImportContext context = new ImportContext();
		context.fallbackBounds = mergeBounds(scanBounds(equipmentDataset), scanBounds(connectionDataset));

		if (areaDataset != null) {
			importAreas(areaDataset, context);
		}
		if (equipmentDataset != null) {
			importEquipments(equipmentDataset, context);
		}
		if (connectionDataset != null) {
			importConnections(connectionDataset, context);
		}

		workspaceBridge.saveWorkspaceQuietly();
		return buildReport(request, context, areaDataset, equipmentDataset, connectionDataset);
	}

	private void clearRuntimeData(Datasource datasource) {
		clearDataset(schemaService.ensureConnectionDataset(datasource));
		for (DatasetVector dataset : schemaService.listManagedEquipmentDatasets(datasource)) {
			clearDataset(dataset);
		}
		clearDataset(schemaService.ensureAreaDataset(datasource));
	}

	private void clearDataset(DatasetVector dataset) {
		Recordset recordset = dataset.getRecordset(false, CursorType.DYNAMIC);
		try {
			Object deleted = invokeQuietly(recordset, "deleteAll");
			if (deleted instanceof Boolean && ((Boolean) deleted)) {
				invokeQuietly(recordset, "update");
				return;
			}
			recordset.moveFirst();
			int guard = 0;
			while (!recordset.isEOF() && guard < 100000) {
				invokeQuietly(recordset, "delete");
				guard++;
			}
			invokeQuietly(recordset, "update");
		} finally {
			release(recordset);
		}
	}

	private void importAreas(DatasetVector dataset, ImportContext context) {
		Recordset recordset = dataset.getRecordset(false, CursorType.STATIC);
		try {
			recordset.moveFirst();
			int index = 1;
			while (!recordset.isEOF()) {
				Bounds bounds = readBounds(recordset);
				if (bounds == null || bounds.isInvalid()) {
					context.notes.add("区域记录缺少可用范围，已跳过：" + dataset.getName() + "#" + index);
					context.skippedAreas++;
					recordset.moveNext();
					index++;
					continue;
				}
				String sourceKey = resolveSourceKey(recordset, index, "AREA_ID", "ID", "SMID");
				String areaName = firstNonBlank(stringValue(recordset, "AREA_NAME"), stringValue(recordset, "NAME"),
						stringValue(recordset, "REGION_NAME"), stringValue(recordset, "CAPTION"), "区域-" + index);
				String areaType = firstNonBlank(stringValue(recordset, "AREA_TYPE"), stringValue(recordset, "TYPE"),
						stringValue(recordset, "CATEGORY"), "导入区域");
				OneModelAreaRecord area = repository.addArea(areaName, areaType,
						bounds.minX, bounds.minY, bounds.maxX, bounds.maxY);
				context.importedAreas++;
				context.areaBySourceKey.put(normalizeKey(sourceKey), area.getAreaId());
				context.areaByName.put(normalizeKey(areaName), area.getAreaId());
				context.areaRecords.add(area);
				recordset.moveNext();
				index++;
			}
		} finally {
			release(recordset);
		}
	}

	public String importCurrentMapWithMappings(MappedImportRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("导入请求不能为空。");
		}
		DatasetVector areaDataset = activeMapService.resolveDataset(request.getAreaDataset());
		DatasetVector connectionDataset = activeMapService.resolveDataset(request.getConnectionDataset());
		List<ResolvedEquipmentMapping> equipmentMappings = resolveEquipmentMappings(request.getEquipmentMappings());
		if (areaDataset == null && connectionDataset == null && equipmentMappings.isEmpty()) {
			throw new IllegalArgumentException("请至少选择一个区域、设备或连接线数据集。");
		}

		repository.initializeRuntimeSchema();
		Datasource runtimeDatasource = workspaceBridge.getOrCreateSharedDatasource();
		if (request.isClearBeforeImport()) {
			clearRuntimeData(runtimeDatasource);
		}

		ImportContext context = new ImportContext();
		Bounds equipmentBounds = null;
		for (ResolvedEquipmentMapping mapping : equipmentMappings) {
			equipmentBounds = mergeBounds(equipmentBounds, scanBounds(mapping.sourceDataset));
		}
		context.fallbackBounds = mergeBounds(equipmentBounds, scanBounds(connectionDataset));

		if (areaDataset != null) {
			importAreas(areaDataset, context);
		}
		for (ResolvedEquipmentMapping mapping : equipmentMappings) {
			importEquipments(mapping.sourceDataset, mapping, context);
		}
		if (connectionDataset != null) {
			importConnections(connectionDataset, context);
		}

		workspaceBridge.saveWorkspaceQuietly();
		return buildMappedReport(request, context, areaDataset, equipmentMappings, connectionDataset);
	}

	private List<ResolvedEquipmentMapping> resolveEquipmentMappings(List<EquipmentLayerMapping> mappings) {
		List<ResolvedEquipmentMapping> result = new ArrayList<>();
		if (mappings == null) {
			return result;
		}
		for (EquipmentLayerMapping mapping : mappings) {
			if (mapping == null || mapping.isSkip()) {
				continue;
			}
			DatasetVector sourceDataset = activeMapService.resolveDataset(mapping.getSourceDataset());
			if (sourceDataset == null) {
				continue;
			}
			String targetCategory = firstNonBlank(mapping.getTargetCategory(), deriveCategoryName(sourceDataset.getName()), "导入设备");
			String targetDatasetName;
			if (EquipmentLayerMapping.TargetMode.NEW_LAYER.equals(mapping.getTargetMode())) {
				targetDatasetName = repository.createEquipmentLayer(targetCategory);
			} else {
				targetDatasetName = mapping.getTargetDatasetName();
				if (isBlank(targetDatasetName)) {
					throw new IllegalArgumentException("请选择目标设备图层：" + sourceDataset.getName());
				}
			}
			result.add(new ResolvedEquipmentMapping(sourceDataset, targetDatasetName, targetCategory));
		}
		return result;
	}

	private void importEquipments(DatasetVector dataset, ResolvedEquipmentMapping mapping, ImportContext context) {
		Recordset recordset = dataset.getRecordset(false, CursorType.STATIC);
		try {
			recordset.moveFirst();
			int index = 1;
			while (!recordset.isEOF()) {
				Point point = readPoint(recordset);
				if (point == null) {
					context.notes.add("设备记录缺少坐标，已跳过：" + dataset.getName() + "#" + index);
					context.skippedEquipments++;
					recordset.moveNext();
					index++;
					continue;
				}
				String sourceKey = resolveSourceKey(recordset, index, "EQUIP_ID", "ID", "SMID", "osm_id");
				String areaId = resolveAreaId(recordset, point, context);
				String name = firstNonBlank(stringValue(recordset, "EQUIP_NAME"), stringValue(recordset, "NAME"),
						stringValue(recordset, "name"), stringValue(recordset, "DEVICE_NAME"), stringValue(recordset, "CAPTION"),
						dataset.getName() + "-" + index);
				String status = firstNonBlank(stringValue(recordset, "STATUS"), stringValue(recordset, "STATE"),
						stringValue(recordset, "PLAN_STATUS"), "现状");
				OneModelEquipmentRecord equipment = repository.addEquipmentToLayer(mapping.targetDatasetName, areaId, name,
						mapping.targetCategory, null, status, "当前地图映射导入/" + dataset.getName(), point.x, point.y);
				context.importedEquipments++;
				context.equipmentBySourceKey.put(normalizeKey(sourceKey), equipment.getEquipmentId());
				context.equipmentBySourceKey.put(normalizeKey(dataset.getName() + ":" + sourceKey), equipment.getEquipmentId());
				context.equipmentBySourceKey.put(normalizeKey(name), equipment.getEquipmentId());
				context.equipmentRecords.put(equipment.getEquipmentId(), equipment);
				recordset.moveNext();
				index++;
			}
		} finally {
			release(recordset);
		}
	}
	private void importEquipments(DatasetVector dataset, ImportContext context) {
		Recordset recordset = dataset.getRecordset(false, CursorType.STATIC);
		try {
			recordset.moveFirst();
			int index = 1;
			while (!recordset.isEOF()) {
				Point point = readPoint(recordset);
				if (point == null) {
					context.notes.add("设备记录缺少坐标，已跳过：" + dataset.getName() + "#" + index);
					context.skippedEquipments++;
					recordset.moveNext();
					index++;
					continue;
				}
				String sourceKey = resolveSourceKey(recordset, index, "EQUIP_ID", "ID", "SMID");
				String areaId = resolveAreaId(recordset, point, context);
				String name = firstNonBlank(stringValue(recordset, "EQUIP_NAME"), stringValue(recordset, "NAME"),
						stringValue(recordset, "DEVICE_NAME"), stringValue(recordset, "CAPTION"), "设备-" + index);
				String type = firstNonBlank(stringValue(recordset, "EQUIP_TYPE"), stringValue(recordset, "TYPE"),
						stringValue(recordset, "CATEGORY"), "导入设备");
				String status = firstNonBlank(stringValue(recordset, "STATUS"), stringValue(recordset, "STATE"),
						stringValue(recordset, "PLAN_STATUS"), "现状");
				OneModelEquipmentRecord equipment = repository.addEquipment(areaId, name, type, status, point.x, point.y);
				repository.updateGraphicBinding(equipment.getEquipmentId(), "当前地图导入/" + dataset.getName());
				context.importedEquipments++;
				context.equipmentBySourceKey.put(normalizeKey(sourceKey), equipment.getEquipmentId());
				context.equipmentBySourceKey.put(normalizeKey(name), equipment.getEquipmentId());
				context.equipmentRecords.put(equipment.getEquipmentId(), equipment);
				recordset.moveNext();
				index++;
			}
		} finally {
			release(recordset);
		}
	}

	private void importConnections(DatasetVector dataset, ImportContext context) {
		Recordset recordset = dataset.getRecordset(false, CursorType.STATIC);
		Set<String> seenPairs = new HashSet<>();
		try {
			recordset.moveFirst();
			int index = 1;
			while (!recordset.isEOF()) {
				String type = firstNonBlank(stringValue(recordset, "CONN_TP"), stringValue(recordset, "TYPE"),
						stringValue(recordset, "CATEGORY"), "电气连接");
				String fromEquipmentId = resolveConnectionEndpoint(recordset, context,
						new String[]{"FROM_ID", "FROM_EQ", "FROM_EQUIP", "START_ID", "BEGIN_ID"}, true);
				String toEquipmentId = resolveConnectionEndpoint(recordset, context,
						new String[]{"TO_ID", "TO_EQ", "TO_EQUIP", "END_ID", "TARGET_ID"}, false);
				if (isBlank(fromEquipmentId) || isBlank(toEquipmentId) || Objects.equals(fromEquipmentId, toEquipmentId)) {
					context.notes.add("连接记录无法解析起止设备，已跳过：" + dataset.getName() + "#" + index);
					context.skippedConnections++;
					recordset.moveNext();
					index++;
					continue;
				}
				String pair = normalizePair(fromEquipmentId, toEquipmentId, type);
				if (!seenPairs.add(pair)) {
					context.notes.add("连接记录与已导入记录重复，已跳过：" + dataset.getName() + "#" + index);
					context.skippedConnections++;
					recordset.moveNext();
					index++;
					continue;
				}
				String status = firstNonBlank(stringValue(recordset, "STATUS"), stringValue(recordset, "STATE"), "现状");
				repository.addConnection(fromEquipmentId, toEquipmentId, type, status);
				context.importedConnections++;
				recordset.moveNext();
				index++;
			}
		} finally {
			release(recordset);
		}
	}

	private String resolveAreaId(Recordset recordset, Point point, ImportContext context) {
		String sourceAreaId = firstNonBlank(stringValue(recordset, "AREA_ID"), stringValue(recordset, "REGION_ID"), stringValue(recordset, "ZONE_ID"));
		String mappedById = context.areaBySourceKey.get(normalizeKey(sourceAreaId));
		if (!isBlank(mappedById)) {
			return mappedById;
		}
		String sourceAreaName = firstNonBlank(stringValue(recordset, "AREA_NAME"), stringValue(recordset, "REGION_NAME"), stringValue(recordset, "ZONE_NAME"));
		String mappedByName = context.areaByName.get(normalizeKey(sourceAreaName));
		if (!isBlank(mappedByName)) {
			return mappedByName;
		}
		for (OneModelAreaRecord area : context.areaRecords) {
			if (contains(area, point)) {
				return area.getAreaId();
			}
		}
		if (!context.areaRecords.isEmpty()) {
			return context.areaRecords.get(0).getAreaId();
		}
		return ensureFallbackArea(context, Bounds.point(point.x, point.y));
	}

	private String resolveConnectionEndpoint(Recordset recordset, ImportContext context, String[] fieldNames, boolean startPoint) {
		for (String fieldName : fieldNames) {
			String value = normalizeKey(stringValue(recordset, fieldName));
			if (!isBlank(value)) {
				String equipmentId = context.equipmentBySourceKey.get(value);
				if (!isBlank(equipmentId)) {
					return equipmentId;
				}
				if (context.equipmentRecords.containsKey(value)) {
					return value;
				}
			}
		}
		LineEndpoints endpoints = readLineEndpoints(recordset);
		Point point = endpoints == null ? null : (startPoint ? endpoints.start : endpoints.end);
		return point == null ? null : findNearestEquipment(point, context.equipmentRecords);
	}

	private String ensureFallbackArea(ImportContext context, Bounds anchor) {
		if (!isBlank(context.fallbackAreaId)) {
			return context.fallbackAreaId;
		}
		Bounds base = mergeBounds(context.fallbackBounds, anchor);
		if (base == null || base.isInvalid()) {
			base = new Bounds(0, 0, 100, 100);
		}
		Bounds expanded = base.expand();
		OneModelAreaRecord area = repository.addArea("当前地图导入区域", "自动生成",
				expanded.minX, expanded.minY, expanded.maxX, expanded.maxY);
		context.fallbackAreaId = area.getAreaId();
		context.areaRecords.add(area);
		context.importedAreas++;
		context.notes.add("未找到可匹配的区域面，已自动创建兜底区域：" + area.getAreaName());
		return context.fallbackAreaId;
	}

	private Bounds scanBounds(DatasetVector dataset) {
		if (dataset == null) {
			return null;
		}
		Recordset recordset = dataset.getRecordset(false, CursorType.STATIC);
		try {
			Bounds merged = null;
			recordset.moveFirst();
			while (!recordset.isEOF()) {
				merged = mergeBounds(merged, readBounds(recordset));
				recordset.moveNext();
			}
			return merged;
		} finally {
			release(recordset);
		}
	}

	private Bounds readBounds(Recordset recordset) {
		Bounds fieldBounds = boundsFromFields(recordset);
		if (fieldBounds != null) {
			return fieldBounds;
		}
		Object geometry = invokeQuietly(recordset, "getGeometry");
		Bounds geometryBounds = readBoundsFromGeometry(geometry);
		if (geometryBounds != null) {
			return geometryBounds;
		}
		Point point = pointFromFields(recordset);
		return point == null ? null : Bounds.point(point.x, point.y);
	}

	private Bounds boundsFromFields(Recordset recordset) {
		Double minX = firstNumber(recordset, "MIN_X", "XMIN", "LEFT");
		Double minY = firstNumber(recordset, "MIN_Y", "YMIN", "BOTTOM");
		Double maxX = firstNumber(recordset, "MAX_X", "XMAX", "RIGHT");
		Double maxY = firstNumber(recordset, "MAX_Y", "YMAX", "TOP");
		if (minX == null || minY == null || maxX == null || maxY == null) {
			return null;
		}
		return new Bounds(minX, minY, maxX, maxY);
	}

	private Bounds readBoundsFromGeometry(Object geometry) {
		if (geometry == null) {
			return null;
		}
		Object bounds = invokeQuietly(geometry, "getBounds");
		if (bounds != null) {
			Double minX = firstNumber(bounds, new String[]{"getLeft", "getMinX", "getXMin"});
			Double minY = firstNumber(bounds, new String[]{"getBottom", "getMinY", "getYMin"});
			Double maxX = firstNumber(bounds, new String[]{"getRight", "getMaxX", "getXMax"});
			Double maxY = firstNumber(bounds, new String[]{"getTop", "getMaxY", "getYMax"});
			if (minX != null && minY != null && maxX != null && maxY != null) {
				return new Bounds(minX, minY, maxX, maxY);
			}
		}
		Point point = readPointFromGeometry(geometry);
		return point == null ? null : Bounds.point(point.x, point.y);
	}

	private Point readPoint(Recordset recordset) {
		Object geometry = invokeQuietly(recordset, "getGeometry");
		Point geometryPoint = readPointFromGeometry(geometry);
		if (geometryPoint != null) {
			return geometryPoint;
		}
		Point fieldPoint = pointFromFields(recordset);
		if (fieldPoint != null) {
			return fieldPoint;
		}
		Bounds bounds = readBounds(recordset);
		return bounds == null ? null : bounds.center();
	}

	private Point readPointFromGeometry(Object geometry) {
		if (geometry == null) {
			return null;
		}
		Double x = firstNumber(geometry, new String[]{"getX", "getCenterX"});
		Double y = firstNumber(geometry, new String[]{"getY", "getCenterY"});
		if (x != null && y != null) {
			return new Point(x, y);
		}
		Object innerPoint = invokeQuietly(geometry, "getInnerPoint");
		if (innerPoint != null) {
			x = firstNumber(innerPoint, new String[]{"getX"});
			y = firstNumber(innerPoint, new String[]{"getY"});
			if (x != null && y != null) {
				return new Point(x, y);
			}
		}
		Bounds bounds = readBoundsFromGeometryOnly(geometry);
		return bounds == null ? null : bounds.center();
	}

	private Bounds readBoundsFromGeometryOnly(Object geometry) {
		if (geometry == null) {
			return null;
		}
		Object bounds = invokeQuietly(geometry, "getBounds");
		if (bounds == null) {
			return null;
		}
		Double minX = firstNumber(bounds, new String[]{"getLeft", "getMinX", "getXMin"});
		Double minY = firstNumber(bounds, new String[]{"getBottom", "getMinY", "getYMin"});
		Double maxX = firstNumber(bounds, new String[]{"getRight", "getMaxX", "getXMax"});
		Double maxY = firstNumber(bounds, new String[]{"getTop", "getMaxY", "getYMax"});
		return minX != null && minY != null && maxX != null && maxY != null ? new Bounds(minX, minY, maxX, maxY) : null;
	}

	private Point pointFromFields(Recordset recordset) {
		Double x = firstNumber(recordset, "PX", "X", "LON", "LONGITUDE");
		Double y = firstNumber(recordset, "PY", "Y", "LAT", "LATITUDE");
		return x == null || y == null ? null : new Point(x, y);
	}

	private LineEndpoints readLineEndpoints(Recordset recordset) {
		Bounds bounds = readBounds(recordset);
		if (bounds == null) {
			return null;
		}
		Point start = new Point(bounds.minX, bounds.minY);
		Point end = new Point(bounds.maxX, bounds.maxY);
		return new LineEndpoints(start, end);
	}

	private String findNearestEquipment(Point point, Map<String, OneModelEquipmentRecord> equipments) {
		double bestDistance = Double.MAX_VALUE;
		String bestId = null;
		for (OneModelEquipmentRecord equipment : equipments.values()) {
			double distance = Math.hypot(equipment.getX() - point.x, equipment.getY() - point.y);
			if (distance < bestDistance) {
				bestDistance = distance;
				bestId = equipment.getEquipmentId();
			}
		}
		return bestDistance <= 100.0D ? bestId : null;
	}

	private boolean contains(OneModelAreaRecord area, Point point) {
		return point.x >= area.getMinX() && point.x <= area.getMaxX()
				&& point.y >= area.getMinY() && point.y <= area.getMaxY();
	}

	private String resolveSourceKey(Recordset recordset, int index, String... preferredFields) {
		for (String field : preferredFields) {
			String value = stringValue(recordset, field);
			if (!isBlank(value)) {
				return value;
			}
		}
		Object id = invokeQuietly(recordset, "getID");
		if (id != null) {
			return String.valueOf(id);
		}
		return "REC-" + index;
	}

	private String stringValue(Recordset recordset, String fieldName) {
		if (isBlank(fieldName)) {
			return null;
		}
		try {
			String value = recordset.getString(fieldName);
			if (!isBlank(value)) {
				return value.trim();
			}
		} catch (Exception ignored) {
		}
		try {
			Object value = recordset.getObject(fieldName);
			if (value != null) {
				String text = String.valueOf(value).trim();
				return text.isEmpty() ? null : text;
			}
		} catch (Exception ignored) {
		}
		return null;
	}

	private Double firstNumber(Recordset recordset, String... fieldNames) {
		for (String fieldName : fieldNames) {
			try {
				return recordset.getDouble(fieldName);
			} catch (Exception ignored) {
			}
			String text = stringValue(recordset, fieldName);
			if (!isBlank(text)) {
				try {
					return Double.parseDouble(text);
				} catch (NumberFormatException ignored) {
				}
			}
		}
		return null;
	}

	private Double firstNumber(Object target, String[] methodNames) {
		for (String methodName : methodNames) {
			Object value = invokeQuietly(target, methodName);
			if (value instanceof Number) {
				return ((Number) value).doubleValue();
			}
		}
		return null;
	}

	private String firstNonBlank(String... values) {
		for (String value : values) {
			if (!isBlank(value)) {
				return value.trim();
			}
		}
		return null;
	}

	private String normalizePair(String left, String right, String type) {
		return left.compareTo(right) <= 0 ? left + "|" + right + "|" + type : right + "|" + left + "|" + type;
	}

	private Bounds mergeBounds(Bounds left, Bounds right) {
		if (left == null) {
			return right;
		}
		if (right == null) {
			return left;
		}
		return new Bounds(
				Math.min(left.minX, right.minX),
				Math.min(left.minY, right.minY),
				Math.max(left.maxX, right.maxX),
				Math.max(left.maxY, right.maxY));
	}

	private String normalizeKey(String value) {
		return isBlank(value) ? null : value.trim().toLowerCase(Locale.ROOT);
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

	private String buildReport(ImportRequest request, ImportContext context,
							   DatasetVector areaDataset, DatasetVector equipmentDataset, DatasetVector connectionDataset) {
		String projectMapName = projectService.getCurrentProject() == null ? "未选择" : projectService.getCurrentProject().getProjectMapName();
		StringBuilder builder = new StringBuilder();
		builder.append("读取当前地图完成\n\n");
		builder.append("工程地图：").append(projectMapName).append("\n");
		builder.append("导入策略：").append(request.isClearBeforeImport() ? "先清空 OneModel 运行数据后全量同步" : "在现有 OneModel 运行数据上追加同步").append("\n");
		builder.append("区域数据集：").append(datasetLabel(areaDataset)).append("\n");
		builder.append("设备数据集：").append(datasetLabel(equipmentDataset)).append("\n");
		builder.append("连接数据集：").append(datasetLabel(connectionDataset)).append("\n\n");
		builder.append("区域导入：").append(context.importedAreas).append("，跳过：").append(context.skippedAreas).append("\n");
		builder.append("设备导入：").append(context.importedEquipments).append("，跳过：").append(context.skippedEquipments).append("\n");
		builder.append("连接导入：").append(context.importedConnections).append("，跳过：").append(context.skippedConnections).append("\n");
		builder.append("\n导入目标：当前工程自己的 OneModel 数据源。\n");
		builder.append("下一步：在工程地图中检查结果，然后执行“拓扑校正”。\n");
		if (!context.notes.isEmpty()) {
			builder.append("\n同步备注：\n");
			for (String note : context.notes) {
				builder.append("- ").append(note).append("\n");
			}
		}
		return builder.toString();
	}

	private String datasetLabel(DatasetVector dataset) {
		return dataset == null ? "未选择" : dataset.getDatasource().getAlias() + " / " + dataset.getName();
	}

	private String buildMappedReport(MappedImportRequest request, ImportContext context,
			DatasetVector areaDataset, List<ResolvedEquipmentMapping> equipmentMappings, DatasetVector connectionDataset) {
		String projectMapName = projectService.getCurrentProject() == null ? "未选择" : projectService.getCurrentProject().getProjectMapName();
		StringBuilder builder = new StringBuilder();
		builder.append("映射导入完成\n\n");
		builder.append("工程地图：").append(projectMapName).append("\n");
		builder.append("导入策略：").append(request.isClearBeforeImport() ? "先清空 OneModel 运行数据后全量同步" : "在现有 OneModel 运行数据上追加同步").append("\n");
		builder.append("区域数据集：").append(datasetLabel(areaDataset)).append("\n");
		builder.append("连接数据集：").append(datasetLabel(connectionDataset)).append("\n\n");
		builder.append("设备图层映射：\n");
		if (equipmentMappings.isEmpty()) {
			builder.append("- 未导入设备点\n");
		} else {
			for (ResolvedEquipmentMapping mapping : equipmentMappings) {
				builder.append("- ").append(datasetLabel(mapping.sourceDataset))
						.append(" -> ").append(mapping.targetCategory)
						.append(" [").append(mapping.targetDatasetName).append("]\n");
			}
		}
		builder.append("\n区域导入：").append(context.importedAreas).append("，跳过：").append(context.skippedAreas).append("\n");
		builder.append("设备导入：").append(context.importedEquipments).append("，跳过：").append(context.skippedEquipments).append("\n");
		builder.append("连接导入：").append(context.importedConnections).append("，跳过：").append(context.skippedConnections).append("\n");
		builder.append("\n导入目标：当前工程自己的 OneModel 数据源。\n");
		builder.append("下一步：可继续在目标设备图层上绘制设备点，或执行拓扑校正/设备-模型绑定。\n");
		if (!context.notes.isEmpty()) {
			builder.append("\n同步备注：\n");
			for (String note : context.notes) {
				builder.append("- ").append(note).append("\n");
			}
		}
		return builder.toString();
	}

	private String deriveCategoryName(String datasetName) {
		String value = datasetName == null ? "" : datasetName.trim();
		if (value.isEmpty()) {
			return "导入设备";
		}
		if (value.startsWith("OM_EQUIP_") && value.endsWith("_P") && value.length() > "OM_EQUIP__P".length()) {
			value = value.substring("OM_EQUIP_".length(), value.length() - 2);
		}
		return value.replace('_', ' ').trim();
	}
	public static final class ImportRequest {

		private final OneModelActiveMapService.DatasetRef areaDataset;
		private final OneModelActiveMapService.DatasetRef equipmentDataset;
		private final OneModelActiveMapService.DatasetRef connectionDataset;
		private final boolean clearBeforeImport;

		public ImportRequest(OneModelActiveMapService.DatasetRef areaDataset,
							 OneModelActiveMapService.DatasetRef equipmentDataset,
							 OneModelActiveMapService.DatasetRef connectionDataset,
							 boolean clearBeforeImport) {
			this.areaDataset = areaDataset;
			this.equipmentDataset = equipmentDataset;
			this.connectionDataset = connectionDataset;
			this.clearBeforeImport = clearBeforeImport;
		}

		public OneModelActiveMapService.DatasetRef getAreaDataset() {
			return areaDataset;
		}

		public OneModelActiveMapService.DatasetRef getEquipmentDataset() {
			return equipmentDataset;
		}

		public OneModelActiveMapService.DatasetRef getConnectionDataset() {
			return connectionDataset;
		}

		public boolean isClearBeforeImport() {
			return clearBeforeImport;
		}
	}

	public static final class MappedImportRequest {

		private final OneModelActiveMapService.DatasetRef areaDataset;
		private final List<EquipmentLayerMapping> equipmentMappings;
		private final OneModelActiveMapService.DatasetRef connectionDataset;
		private final boolean clearBeforeImport;

		public MappedImportRequest(OneModelActiveMapService.DatasetRef areaDataset,
				List<EquipmentLayerMapping> equipmentMappings,
				OneModelActiveMapService.DatasetRef connectionDataset,
				boolean clearBeforeImport) {
			this.areaDataset = areaDataset;
			this.equipmentMappings = equipmentMappings == null ? new ArrayList<>() : new ArrayList<>(equipmentMappings);
			this.connectionDataset = connectionDataset;
			this.clearBeforeImport = clearBeforeImport;
		}

		public OneModelActiveMapService.DatasetRef getAreaDataset() {
			return areaDataset;
		}

		public List<EquipmentLayerMapping> getEquipmentMappings() {
			return equipmentMappings;
		}

		public OneModelActiveMapService.DatasetRef getConnectionDataset() {
			return connectionDataset;
		}

		public boolean isClearBeforeImport() {
			return clearBeforeImport;
		}
	}

	public static final class EquipmentLayerMapping {

		public enum TargetMode {
			SKIP,
			EXISTING_LAYER,
			NEW_LAYER
		}

		private final OneModelActiveMapService.DatasetRef sourceDataset;
		private final TargetMode targetMode;
		private final String targetDatasetName;
		private final String targetCategory;

		public EquipmentLayerMapping(OneModelActiveMapService.DatasetRef sourceDataset, TargetMode targetMode,
				String targetDatasetName, String targetCategory) {
			this.sourceDataset = sourceDataset;
			this.targetMode = targetMode == null ? TargetMode.SKIP : targetMode;
			this.targetDatasetName = targetDatasetName;
			this.targetCategory = targetCategory;
		}

		public boolean isSkip() {
			return TargetMode.SKIP.equals(targetMode) || sourceDataset == null || sourceDataset.isNone();
		}

		public OneModelActiveMapService.DatasetRef getSourceDataset() {
			return sourceDataset;
		}

		public TargetMode getTargetMode() {
			return targetMode;
		}

		public String getTargetDatasetName() {
			return targetDatasetName;
		}

		public String getTargetCategory() {
			return targetCategory;
		}
	}

	private static final class ResolvedEquipmentMapping {

		private final DatasetVector sourceDataset;
		private final String targetDatasetName;
		private final String targetCategory;

		private ResolvedEquipmentMapping(DatasetVector sourceDataset, String targetDatasetName, String targetCategory) {
			this.sourceDataset = sourceDataset;
			this.targetDatasetName = targetDatasetName;
			this.targetCategory = targetCategory;
		}
	}
	private static final class ImportContext {

		private final Map<String, String> areaBySourceKey = new HashMap<>();
		private final Map<String, String> areaByName = new HashMap<>();
		private final List<OneModelAreaRecord> areaRecords = new ArrayList<>();
		private final Map<String, String> equipmentBySourceKey = new HashMap<>();
		private final Map<String, OneModelEquipmentRecord> equipmentRecords = new HashMap<>();
		private final List<String> notes = new ArrayList<>();
		private Bounds fallbackBounds;
		private String fallbackAreaId;
		private int importedAreas;
		private int skippedAreas;
		private int importedEquipments;
		private int skippedEquipments;
		private int importedConnections;
		private int skippedConnections;
	}

	private static final class Bounds {

		private final double minX;
		private final double minY;
		private final double maxX;
		private final double maxY;

		private Bounds(double minX, double minY, double maxX, double maxY) {
			this.minX = Math.min(minX, maxX);
			this.minY = Math.min(minY, maxY);
			this.maxX = Math.max(minX, maxX);
			this.maxY = Math.max(minY, maxY);
		}

		private static Bounds point(double x, double y) {
			return new Bounds(x, y, x, y);
		}

		private boolean isInvalid() {
			return Double.isNaN(minX) || Double.isNaN(minY) || Double.isNaN(maxX) || Double.isNaN(maxY);
		}

		private Point center() {
			return new Point((minX + maxX) / 2.0D, (minY + maxY) / 2.0D);
		}

		private Bounds expand() {
			return new Bounds(minX - DEFAULT_MARGIN, minY - DEFAULT_MARGIN, maxX + DEFAULT_MARGIN, maxY + DEFAULT_MARGIN);
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

	private static final class LineEndpoints {

		private final Point start;
		private final Point end;

		private LineEndpoints(Point start, Point end) {
			this.start = start;
			this.end = end;
		}
	}
}



