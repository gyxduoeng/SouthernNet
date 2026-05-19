package com.aircas.onemodel.service;

import com.aircas.onemodel.model.OneModelModelResource;
import com.supermap.data.CursorType;
import com.supermap.data.Dataset;
import com.supermap.data.DatasetVector;
import com.supermap.data.Geometry;
import com.supermap.data.Recordset;
import com.supermap.desktop.core.Interface.IFormMap;
import com.supermap.mapping.Layer;
import com.supermap.ui.Action;
import com.supermap.ui.GeometryAddedListener;
import com.supermap.ui.GeometryEvent;
import com.supermap.ui.MapControl;

import javax.swing.SwingUtilities;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 监听 SuperMap 原生绘点操作，自动为 OneModel 设备点写入默认业务属性。
 */
public class OneModelEquipmentAutoAttributeService {

	private static final Map<MapControl, GeometryAddedListener> LISTENERS = Collections.synchronizedMap(new WeakHashMap<>());
	private static final Map<MapControl, DrawSession> DRAW_SESSIONS = Collections.synchronizedMap(new WeakHashMap<>());
	private static final Pattern NUMBER_SUFFIX = Pattern.compile(".*-(\\d+)$");

	private final OneModelSchemaService schemaService = new OneModelSchemaService();
	private final OneModelLayerCatalogService layerCatalogService = new OneModelLayerCatalogService();
	private final OneModelSessionStore sessionStore = OneModelSessionStore.getInstance();
	private final OneModelWorkspaceBridge workspaceBridge = new OneModelWorkspaceBridge();

	public void install(IFormMap formMap) {
		if (formMap == null || formMap.getMapControl() == null) {
			return;
		}
		MapControl mapControl = formMap.getMapControl();
		synchronized (LISTENERS) {
			if (LISTENERS.containsKey(mapControl)) {
				return;
			}
			GeometryAddedListener listener = this::geometryAdded;
			mapControl.addGeometryAddedListener(listener);
			LISTENERS.put(mapControl, listener);
		}
	}

	public void beginDraw(IFormMap formMap, boolean continuous) {
		if (formMap == null || formMap.getMapControl() == null) {
			return;
		}
		install(formMap);
		synchronized (DRAW_SESSIONS) {
			DRAW_SESSIONS.put(formMap.getMapControl(), new DrawSession(continuous));
		}
	}
	private void geometryAdded(GeometryEvent event) {
		if (event == null || event.getLayer() == null) {
			return;
		}
		Layer layer = event.getLayer();
		Dataset dataset = layer.getDataset();
		if (!(dataset instanceof DatasetVector)) {
			return;
		}
		DatasetVector vector = (DatasetVector) dataset;
		if (!schemaService.isManagedEquipmentDataset(vector.getName())) {
			return;
		}
		int recordId = event.getID();
		MapControl mapControl = event.getSource() instanceof MapControl ? (MapControl) event.getSource() : null;
		SwingUtilities.invokeLater(() -> autoFillEquipmentAttributes(vector, recordId, mapControl, layer));
	}

	private void autoFillEquipmentAttributes(DatasetVector dataset, int recordId, MapControl mapControl, Layer layer) {
		if (dataset == null || !schemaService.isManagedEquipmentDataset(dataset.getName())) {
			return;
		}
		schemaService.listManagedEquipmentDatasets(dataset.getDatasource());
		String category = resolveCategory(dataset);
		String categoryKey = schemaService.normalizeEquipmentCategoryKey(category);
		String namePrefix = resolveNamePrefix(category);
		OneModelModelResource defaultModel = layerCatalogService.findLayerDefaultModel(dataset.getName(), category);
		int sequence = nextSequence(dataset, namePrefix, recordId);
		String sequenceText = String.format(Locale.ROOT, "%03d", sequence);

		Recordset recordset = dataset.getRecordset(false, CursorType.DYNAMIC);
		boolean changed = false;
		try {
			if (!seekTargetRecord(recordset, recordId)) {
				return;
			}
			Geometry geometry = recordset.getGeometry();
			Point point = readPoint(geometry);
			recordset.edit();
			changed |= setIfBlank(recordset, "EQUIP_ID", buildEquipmentId(categoryKey, recordset.getID()));
			changed |= setIfBlank(recordset, "EQUIP_CODE", "EQ-" + sequenceText);
			changed |= setIfBlank(recordset, "EQUIP_NAME", namePrefix + "-" + sequenceText);
			changed |= setIfBlank(recordset, "EQUIP_TYPE", category);
			changed |= setIfBlank(recordset, "MODEL_CAT_KEY", categoryKey);
			changed |= setIfBlank(recordset, "STATUS", "现状");
			changed |= setIfBlank(recordset, "VERSION_ID", sessionStore.getParameters().getVersionId());
			changed |= setIfBlank(recordset, "GRAPHIC_TP", "设备点");
			if (point != null) {
				changed |= setCoordinate(recordset, "PX", point.x);
				changed |= setCoordinate(recordset, "PY", point.y);
			}
			if (defaultModel != null) {
				changed |= setIfBlank(recordset, "MODEL_ID", defaultModel.getModelId());
				changed |= setIfBlank(recordset, "MODEL_NAME", defaultModel.getModelName());
				changed |= setIfBlank(recordset, "MODEL_PATH", defaultModel.getModelPath());
				changed |= setIfBlank(recordset, "MODEL_ATTRS", defaultModel.getModelAttributes());
			}
			if (changed) {
				recordset.update();
				workspaceBridge.saveWorkspaceQuietly();
			} else {
				recordset.cancelUpdate();
			}
		} catch (Exception ignored) {
			try {
				recordset.cancelUpdate();
			} catch (Exception ignoredToo) {
			}
		} finally {
			release(recordset);
			finishOneShotDraw(mapControl, layer);
		}
	}

	private void finishOneShotDraw(MapControl mapControl, Layer layer) {
		DrawSession session = resolveDrawSession(mapControl);
		if (session == null || session.continuous) {
			return;
		}
		endDraw(mapControl, layer);
	}

	public void endDraw(MapControl mapControl, Layer layer) {
		if (mapControl == null) {
			return;
		}
		synchronized (DRAW_SESSIONS) {
			DRAW_SESSIONS.remove(mapControl);
		}
		try {
			mapControl.setAction(Action.SELECT);
		} catch (Exception ignored) {
		}
		if (layer != null) {
			try {
				layer.setEditable(false);
			} catch (Exception ignored) {
			}
		}
		invokeQuietly(mapControl, "refresh");
		invokeQuietly(mapControl, "repaint");
	}

	private DrawSession resolveDrawSession(MapControl mapControl) {
		if (mapControl == null) {
			return null;
		}
		synchronized (DRAW_SESSIONS) {
			return DRAW_SESSIONS.get(mapControl);
		}
	}
	private boolean seekTargetRecord(Recordset recordset, int recordId) {
		if (recordset == null || recordset.isEmpty()) {
			return false;
		}
		if (recordId > 0 && recordset.seekID(recordId)) {
			return true;
		}
		recordset.moveLast();
		return !recordset.isEOF();
	}

	private int nextSequence(DatasetVector dataset, String namePrefix, int currentRecordId) {
		int max = 0;
		Recordset recordset = dataset.getRecordset(false, CursorType.STATIC);
		try {
			recordset.moveFirst();
			while (!recordset.isEOF()) {
				if (currentRecordId <= 0 || recordset.getID() != currentRecordId) {
					max = Math.max(max, suffixNumber(stringValue(recordset, "EQUIP_NAME"), namePrefix));
					max = Math.max(max, suffixNumber(stringValue(recordset, "EQUIP_CODE"), "EQ"));
				}
				recordset.moveNext();
			}
		} finally {
			release(recordset);
		}
		return max + 1;
	}

	private int suffixNumber(String value, String prefix) {
		if (isBlank(value)) {
			return 0;
		}
		String text = value.trim();
		String actualPrefix = prefix == null ? "" : prefix.trim();
		if (!actualPrefix.isEmpty() && !text.startsWith(actualPrefix + "-")) {
			return 0;
		}
		Matcher matcher = NUMBER_SUFFIX.matcher(text);
		if (!matcher.matches()) {
			return 0;
		}
		try {
			return Integer.parseInt(matcher.group(1));
		} catch (Exception ignored) {
			return 0;
		}
	}

	private String resolveCategory(DatasetVector dataset) {
		String caption = layerCatalogService.findLayerCaption(dataset.getName());
		if (!isBlank(caption)) {
			return caption.trim();
		}
		return dataset.getName();
	}

	private String resolveNamePrefix(String category) {
		String value = category == null ? "" : category.trim();
		value = value.replaceFirst("^[0-9A-Za-z]+[\\s_\\-]+", "").trim();
		return value.isEmpty() ? "设备" : value;
	}

	private String buildEquipmentId(String categoryKey, int recordId) {
		String key = isBlank(categoryKey) ? "CAT" : categoryKey;
		return "EQ-" + key + "-" + System.currentTimeMillis() + "-" + Math.max(recordId, 0);
	}

	private Point readPoint(Geometry geometry) {
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

	private boolean setCoordinate(Recordset recordset, String fieldName, double value) {
		try {
			Object current = recordset.getObject(fieldName);
			if (current instanceof Number && Math.abs(((Number) current).doubleValue() - value) < 0.000000001D) {
				return false;
			}
		} catch (Exception ignored) {
		}
		recordset.setObject(fieldName, value);
		return true;
	}
	private boolean setIfBlank(Recordset recordset, String fieldName, Object value) {
		if (value == null || isBlank(String.valueOf(value))) {
			return false;
		}
		if (!isBlank(stringValue(recordset, fieldName))) {
			return false;
		}
		recordset.setObject(fieldName, value);
		return true;
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

	private static final class DrawSession {
		private final boolean continuous;

		private DrawSession(boolean continuous) {
			this.continuous = continuous;
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
