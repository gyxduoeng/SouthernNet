package com.aircas.onemodel.service;

import com.supermap.data.CursorType;
import com.supermap.data.Dataset;
import com.supermap.data.DatasetVector;
import com.supermap.data.GeoLine;
import com.supermap.data.Geometry;
import com.supermap.data.Point2D;
import com.supermap.data.Point2Ds;
import com.supermap.data.Rectangle2D;
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
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 手动绘制连接线后自动写入 OneModel 连接属性。
 */
public class OneModelConnectionDrawService {

	private static final Map<MapControl, GeometryAddedListener> LISTENERS = Collections.synchronizedMap(new WeakHashMap<>());
	private static final Map<MapControl, ConnectionDraft> DRAFTS = Collections.synchronizedMap(new WeakHashMap<>());

	private final OneModelSchemaService schemaService = new OneModelSchemaService();
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

	public void beginDraw(IFormMap formMap, String connectionType, String status) {
		if (formMap == null || formMap.getMapControl() == null) {
			return;
		}
		install(formMap);
		synchronized (DRAFTS) {
			DRAFTS.put(formMap.getMapControl(), new ConnectionDraft(connectionType, status));
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
		if (!OneModelSchemaService.CONNECTION_DATASET.equalsIgnoreCase(vector.getName())) {
			return;
		}
		MapControl mapControl = event.getSource() instanceof MapControl ? (MapControl) event.getSource() : null;
		ConnectionDraft draft = resolveDraft(mapControl);
		int recordId = event.getID();
		SwingUtilities.invokeLater(() -> fillConnectionAttributes(vector, recordId, draft, mapControl, layer));
	}

	private ConnectionDraft resolveDraft(MapControl mapControl) {
		if (mapControl == null) {
			return ConnectionDraft.defaultDraft();
		}
		synchronized (DRAFTS) {
			ConnectionDraft draft = DRAFTS.remove(mapControl);
			return draft == null ? ConnectionDraft.defaultDraft() : draft;
		}
	}

	private void fillConnectionAttributes(DatasetVector dataset, int recordId, ConnectionDraft draft, MapControl mapControl, Layer layer) {
		schemaService.ensureConnectionDataset(dataset.getDatasource());
		Recordset recordset = dataset.getRecordset(false, CursorType.DYNAMIC);
		try {
			if (!seekTargetRecord(recordset, recordId)) {
				return;
			}
			Geometry geometry = recordset.getGeometry();
			Endpoints endpoints = readEndpoints(geometry);
			String fromId = matchEquipmentId(dataset, endpoints.start, null);
			String toId = matchEquipmentId(dataset, endpoints.end, fromId);
			if (!isBlank(fromId) && fromId.equals(toId)) {
				toId = "";
			}
			recordset.edit();
			recordset.setObject("CONN_ID", "CN-" + System.currentTimeMillis() + "-" + Math.max(recordset.getID(), 0));
			recordset.setObject("FROM_ID", firstNonBlank(fromId, ""));
			recordset.setObject("TO_ID", firstNonBlank(toId, ""));
			recordset.setObject("CONN_TP", firstNonBlank(draft.connectionType, "电气连接"));
			recordset.setObject("STATUS", firstNonBlank(draft.status, "现状"));
			recordset.setObject("VERSION_ID", sessionStore.getParameters().getVersionId());
			recordset.setObject("GRAPHIC_TP", "关联关系线");
			recordset.update();
			workspaceBridge.saveWorkspaceQuietly();
		} finally {
			release(recordset);
		}
		endDraw(mapControl, layer);
	}

	private void endDraw(MapControl mapControl, Layer layer) {
		if (mapControl != null) {
			try {
				mapControl.setAction(Action.SELECT);
			} catch (Exception ignored) {
			}
			invokeQuietly(mapControl, "refresh");
			invokeQuietly(mapControl, "repaint");
		}
		if (layer != null) {
			try {
				layer.setEditable(false);
			} catch (Exception ignored) {
			}
		}
	}

	private Endpoints readEndpoints(Geometry geometry) {
		if (geometry instanceof GeoLine) {
			GeoLine line = (GeoLine) geometry;
			Point2D start = null;
			Point2D end = null;
			for (int i = 0; i < line.getPartCount(); i++) {
				Point2Ds part = line.getPart(i);
				if (part != null && part.getCount() > 0) {
					if (start == null) {
						start = part.getItem(0);
					}
					end = part.getItem(part.getCount() - 1);
				}
			}
			return new Endpoints(start, end);
		}
		Rectangle2D bounds = geometry == null ? null : geometry.getBounds();
		if (bounds == null) {
			return Endpoints.empty();
		}
		Point2D center = new Point2D((bounds.getLeft() + bounds.getRight()) / 2D, (bounds.getBottom() + bounds.getTop()) / 2D);
		return new Endpoints(center, center);
	}

	private String matchEquipmentId(DatasetVector connectionDataset, Point2D point, String excludedId) {
		if (connectionDataset == null || point == null) {
			return "";
		}
		double tolerance = resolveEndpointTolerance();
		double bestDistance = Double.MAX_VALUE;
		String bestId = "";
		for (DatasetVector equipmentDataset : schemaService.listManagedEquipmentDatasets(connectionDataset.getDatasource())) {
			Recordset recordset = equipmentDataset.getRecordset(false, CursorType.STATIC);
			try {
				recordset.moveFirst();
				while (!recordset.isEOF()) {
					String equipmentId = firstNonBlank(stringValue(recordset, "EQUIP_ID"), "");
					Point2D equipmentPoint = readEquipmentPoint(recordset);
					if (!isBlank(equipmentId) && !equipmentId.equals(excludedId) && equipmentPoint != null) {
						double distance = distance(point, equipmentPoint);
						if (distance < bestDistance) {
							bestDistance = distance;
							bestId = equipmentId;
						}
					}
					recordset.moveNext();
				}
			} finally {
				release(recordset);
			}
		}
		return bestDistance <= tolerance ? bestId : "";
	}

	private Point2D readEquipmentPoint(Recordset recordset) {
		double x = coordinateValue(recordset, "PX", null);
		double y = coordinateValue(recordset, "PY", null);
		if (x != 0D || y != 0D) {
			return new Point2D(x, y);
		}
		try {
			Geometry geometry = recordset.getGeometry();
			Rectangle2D bounds = geometry == null ? null : geometry.getBounds();
			if (bounds != null) {
				return new Point2D((bounds.getLeft() + bounds.getRight()) / 2D, (bounds.getBottom() + bounds.getTop()) / 2D);
			}
		} catch (Exception ignored) {
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

	private double resolveEndpointTolerance() {
		String code = firstNonBlank(sessionStore.getParameters().getCoordinateSystemCode(), "4490").replace("EPSG:", "").trim();
		return "4326".equals(code) || "4490".equals(code) ? 0.0005D : 5D;
	}

	private double distance(Point2D left, Point2D right) {
		double dx = left.getX() - right.getX();
		double dy = left.getY() - right.getY();
		return Math.sqrt(dx * dx + dy * dy);
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

	private String firstNonBlank(String value, String fallback) {
		return isBlank(value) ? fallback : value.trim();
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

	private static final class ConnectionDraft {
		private final String connectionType;
		private final String status;

		private ConnectionDraft(String connectionType, String status) {
			this.connectionType = connectionType;
			this.status = status;
		}

		private static ConnectionDraft defaultDraft() {
			return new ConnectionDraft("电气连接", "现状");
		}
	}

	private static final class Endpoints {
		private final Point2D start;
		private final Point2D end;

		private Endpoints(Point2D start, Point2D end) {
			this.start = start;
			this.end = end;
		}

		private static Endpoints empty() {
			return new Endpoints(null, null);
		}
	}
}