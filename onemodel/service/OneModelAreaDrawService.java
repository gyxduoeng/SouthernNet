package com.aircas.onemodel.service;

import com.supermap.data.CursorType;
import com.supermap.data.Dataset;
import com.supermap.data.DatasetVector;
import com.supermap.data.Geometry;
import com.supermap.data.Rectangle2D;
import com.supermap.data.Recordset;
import com.supermap.desktop.core.Interface.IFormMap;
import com.supermap.mapping.Layer;
import com.supermap.ui.Action;
import com.supermap.ui.GeometryAddedListener;
import com.supermap.ui.GeometryEvent;
import com.supermap.ui.MapControl;

import javax.swing.SwingUtilities;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 绘制区域面后自动写入区域属性和经纬度范围。
 */
public class OneModelAreaDrawService {

	private static final Map<MapControl, GeometryAddedListener> LISTENERS = Collections.synchronizedMap(new WeakHashMap<>());
	private static final Map<MapControl, AreaDraft> DRAFTS = Collections.synchronizedMap(new WeakHashMap<>());

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

	public void beginDraw(IFormMap formMap, String areaName, String areaType) {
		if (formMap == null || formMap.getMapControl() == null) {
			return;
		}
		install(formMap);
		synchronized (DRAFTS) {
			DRAFTS.put(formMap.getMapControl(), new AreaDraft(areaName, areaType));
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
		if (!OneModelSchemaService.AREA_DATASET.equalsIgnoreCase(vector.getName())) {
			return;
		}
		MapControl mapControl = event.getSource() instanceof MapControl ? (MapControl) event.getSource() : null;
		AreaDraft draft = resolveDraft(mapControl);
		int recordId = event.getID();
		SwingUtilities.invokeLater(() -> fillAreaAttributes(vector, recordId, draft, mapControl, layer));
	}

	private AreaDraft resolveDraft(MapControl mapControl) {
		if (mapControl == null) {
			return AreaDraft.defaultDraft();
		}
		synchronized (DRAFTS) {
			AreaDraft draft = DRAFTS.remove(mapControl);
			return draft == null ? AreaDraft.defaultDraft() : draft;
		}
	}

	private void fillAreaAttributes(DatasetVector dataset, int recordId, AreaDraft draft, MapControl mapControl, Layer layer) {
		schemaService.ensureAreaDataset(dataset.getDatasource());
		Recordset recordset = dataset.getRecordset(false, CursorType.DYNAMIC);
		try {
			if (!seekTargetRecord(recordset, recordId)) {
				return;
			}
			Geometry geometry = recordset.getGeometry();
			Rectangle2D bounds = geometry == null ? null : geometry.getBounds();
			if (bounds == null) {
				return;
			}
			recordset.edit();
			recordset.setObject("AREA_ID", "AR-" + System.currentTimeMillis() + "-" + Math.max(recordset.getID(), 0));
			recordset.setObject("AREA_NAME", firstNonBlank(draft.areaName, "区域"));
			recordset.setObject("AREA_TYPE", firstNonBlank(draft.areaType, "功能分区"));
			recordset.setObject("VERSION_ID", sessionStore.getParameters().getVersionId());
			recordset.setObject("MIN_X", bounds.getLeft());
			recordset.setObject("MIN_Y", bounds.getBottom());
			recordset.setObject("MAX_X", bounds.getRight());
			recordset.setObject("MAX_Y", bounds.getTop());
			recordset.update();
			workspaceBridge.saveWorkspaceQuietly();
		} finally {
			release(recordset);
		}
		if (mapControl != null) {
			mapControl.setAction(Action.SELECT);
		}
		if (layer != null) {
			layer.setEditable(false);
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

	private String firstNonBlank(String value, String fallback) {
		return value == null || value.trim().isEmpty() ? fallback : value.trim();
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

	private static final class AreaDraft {
		private final String areaName;
		private final String areaType;

		private AreaDraft(String areaName, String areaType) {
			this.areaName = areaName;
			this.areaType = areaType;
		}

		private static AreaDraft defaultDraft() {
			return new AreaDraft("区域", "功能分区");
		}
	}
}
