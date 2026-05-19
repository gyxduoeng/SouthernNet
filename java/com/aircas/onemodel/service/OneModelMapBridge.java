package com.aircas.onemodel.service;

import com.supermap.data.DatasetVector;
import com.supermap.data.Datasource;
import com.supermap.data.Point2D;
import com.supermap.data.Rectangle2D;
import com.supermap.data.Workspace;
import com.supermap.desktop.controls.utilities.MapViewUIUtilities;
import com.supermap.desktop.core.Application;
import com.supermap.desktop.core.Interface.IFormMap;
import com.supermap.desktop.core.enums.WindowType;
import com.supermap.desktop.core.utilties.*;
import com.supermap.mapping.Layer;
import com.supermap.ui.Action;

import java.awt.Color;
import java.lang.reflect.Method;

/**
 * OneModel 与宿主地图窗口桥接。
 */
public class OneModelMapBridge {

	private final OneModelWorkspaceBridge workspaceBridge = new OneModelWorkspaceBridge();
	private final OneModelSchemaService schemaService = new OneModelSchemaService();
	private final OneModelLayerCatalogService layerCatalogService = new OneModelLayerCatalogService();
	private final OneModelCoordinateSystemSupport coordinateSystemSupport = new OneModelCoordinateSystemSupport();
	private final OneModelEquipmentAutoAttributeService autoAttributeService = new OneModelEquipmentAutoAttributeService();
	private final OneModelAreaDrawService areaDrawService = new OneModelAreaDrawService();

	public IFormMap openOrCreateWorkingMap(Datasource datasource) {
		return openOrCreateWorkingMap(datasource, "工程地图");
	}

	public IFormMap openOrCreateWorkingMap(Datasource datasource, String mapName) {
		String targetMapName = mapName == null || mapName.trim().isEmpty() ? "工程地图" : mapName.trim();
		Workspace workspace = workspaceBridge.getActiveWorkspace();
		if (workspace.getMaps().indexOf(targetMapName) >= 0) {
			MapViewUIUtilities.openMap(targetMapName);
			IFormMap formMap = resolveActiveFormMap();
			applyMapCoordinateSystem(formMap);
			ensureManagedLayersPresent(formMap, datasource);
			return formMap;
		}
		IFormMap workingMap = (IFormMap) FormUtilities.fireNewWindowEvent(WindowType.MAP);
		if (workingMap == null) {
			return null;
		}
		applyMapCoordinateSystem(workingMap);
		ensureManagedLayersPresent(workingMap, datasource);
		initializeMapViewQuietly(workingMap, datasource);
		setMapNameQuietly(workingMap, targetMapName);
		saveWorkspaceMapQuietly(workspace, workingMap, targetMapName);
		return workingMap;
	}

	public void ensureManagedLayersPresent(Datasource datasource) {
		ensureManagedLayersPresent(resolveActiveFormMap(), datasource);
	}

	public void ensureManagedLayersPresent(IFormMap formMap, Datasource datasource) {
		if (formMap == null || datasource == null) {
			return;
		}
		autoAttributeService.install(formMap);
		areaDrawService.install(formMap);
		java.util.List<DatasetVector> datasets = new java.util.ArrayList<>();
		DatasetVector areaDataset = (DatasetVector) datasource.getDatasets().get(OneModelSchemaService.AREA_DATASET);
		if (areaDataset != null) {
			datasets.add(areaDataset);
		}
		datasets.addAll(schemaService.listManagedEquipmentDatasets(datasource));
		DatasetVector connectionDataset = (DatasetVector) datasource.getDatasets().get(OneModelSchemaService.CONNECTION_DATASET);
		if (connectionDataset != null) {
			datasets.add(connectionDataset);
		}
		java.util.List<DatasetVector> missing = new java.util.ArrayList<>();
		for (DatasetVector dataset : datasets) {
			if (dataset != null && !containsLayer(formMap, dataset)) {
				missing.add(dataset);
			}
		}
		if (!missing.isEmpty()) {
			MapViewUIUtilities.addDatasetsToMap(formMap.getMapControl().getMap(), 0, missing.toArray(new DatasetVector[0]));
		}
		for (DatasetVector dataset : datasets) {
			applyDefaultManagedLayerStyle(formMap, dataset);
		}
		applyManagedLayerCaptions(formMap);
		refreshMapQuietly(formMap);
		workspaceBridge.saveWorkspaceQuietly();
	}

	public void refreshActiveMap() {
		refreshMapQuietly(resolveActiveFormMap());
	}

	public void activateEditableLayer(String datasetName) {
		IFormMap formMap = resolveActiveFormMap();
		if (formMap == null || formMap.getMapControl() == null || datasetName == null || datasetName.trim().isEmpty()) {
			return;
		}
		Object layer = findLayer(formMap, datasetName.trim());
		if (layer instanceof Layer) {
			formMap.getMapControl().setActiveEditableLayer((Layer) layer);
		}
	}

	public boolean beginCreatePoint(String datasetName) {
		return beginCreatePoint(datasetName, false);
	}

	public boolean beginCreatePoint(String datasetName, boolean continuous) {
		IFormMap formMap = resolveActiveFormMap();
		if (formMap == null || formMap.getMapControl() == null || datasetName == null || datasetName.trim().isEmpty()) {
			return false;
		}
		Object layer = findLayer(formMap, datasetName.trim());
		if (!(layer instanceof Layer)) {
			return false;
		}
		Layer editableLayer = (Layer) layer;
		autoAttributeService.beginDraw(formMap, continuous);
		editableLayer.setVisible(true);
		editableLayer.setSelectable(true);
		editableLayer.setEditable(true);
		formMap.getMapControl().setActiveEditableLayer(editableLayer);
		formMap.getMapControl().setAction(Action.CREATEPOINT);
		return true;
	}

	public boolean endDrawEquipmentPoint() {
		IFormMap formMap = resolveActiveFormMap();
		if (formMap == null || formMap.getMapControl() == null) {
			return false;
		}
		Layer layer = null;
		Object activeLayer = invokeQuietly(formMap.getMapControl(), "getActiveEditableLayer");
		if (activeLayer instanceof Layer) {
			layer = (Layer) activeLayer;
		}
		autoAttributeService.endDraw(formMap.getMapControl(), layer);
		return true;
	}
	public boolean beginCreateArea(String areaName, String areaType) {
		IFormMap formMap = resolveActiveFormMap();
		if (formMap == null || formMap.getMapControl() == null) {
			return false;
		}
		Object layer = findLayer(formMap, OneModelSchemaService.AREA_DATASET);
		if (!(layer instanceof Layer)) {
			return false;
		}
		Layer editableLayer = (Layer) layer;
		editableLayer.setVisible(true);
		editableLayer.setSelectable(true);
		editableLayer.setEditable(true);
		formMap.getMapControl().setActiveEditableLayer(editableLayer);
		areaDrawService.beginDraw(formMap, areaName, areaType);
		formMap.getMapControl().setAction(Action.CREATERECTANGLE);
		return true;
	}

	private void saveWorkspaceMapQuietly(Workspace workspace, IFormMap workingMap, String mapName) {
		try {
			String xml = workingMap.getMapControl().getMap().toXML();
			if (workspace.getMaps().indexOf(mapName) >= 0) {
				workspace.getMaps().setMapXML(mapName, xml);
			} else {
				workspace.getMaps().add(mapName, xml);
			}
			workingMap.getMapControl().getMap().setModified(false);
			workspace.save();
		} catch (Exception ignored) {
			// 不阻塞工程加载。
		}
	}

	private void setMapNameQuietly(IFormMap formMap, String mapName) {
		try {
			formMap.getMapControl().getMap().setName(mapName);
		} catch (Exception ignored) {
		}
		invokeQuietly(formMap, "setTitle", mapName);
		invokeQuietly(formMap, "setText", mapName);
	}

	private void applyMapCoordinateSystem(IFormMap formMap) {
		if (formMap == null) {
			return;
		}
		Object mapObject = formMap.getMapControl() == null ? null : formMap.getMapControl().getMap();
		coordinateSystemSupport.applyToMapQuietly(mapObject, coordinateSystemSupport.resolveCurrentEpsgCode());
	}

	private void initializeMapViewQuietly(IFormMap formMap, Datasource datasource) {
		Object mapObject = formMap == null || formMap.getMapControl() == null ? null : formMap.getMapControl().getMap();
		if (mapObject == null) {
			return;
		}
		Rectangle2D dataBounds = resolveManagedDatasetBounds(datasource);
		if (dataBounds != null) {
			trySetViewBounds(mapObject, expandBounds(dataBounds, 0.1D));
			return;
		}
		Rectangle2D defaultBounds = resolveDefaultBounds(datasource);
		trySetViewBounds(mapObject, defaultBounds);
	}

	private Rectangle2D resolveDefaultBounds(Datasource datasource) {
		String epsgCode = coordinateSystemSupport.resolveCurrentEpsgCode();
		if ("4490".equals(epsgCode) || "4326".equals(epsgCode)) {
			return new Rectangle2D(70.0D, 0.0D, 140.0D, 55.0D);
		}
		return new Rectangle2D(0.0D, 0.0D, 10000.0D, 10000.0D);
	}

	private Rectangle2D resolveManagedDatasetBounds(Datasource datasource) {
		if (datasource == null) {
			return null;
		}
		Rectangle2D merged = null;
		for (DatasetVector dataset : schemaService.listManagedEquipmentDatasets(datasource)) {
			merged = mergeBounds(merged, readDatasetBounds(dataset));
		}
		merged = mergeBounds(merged, readDatasetBounds((DatasetVector) datasource.getDatasets().get(OneModelSchemaService.AREA_DATASET)));
		merged = mergeBounds(merged, readDatasetBounds((DatasetVector) datasource.getDatasets().get(OneModelSchemaService.CONNECTION_DATASET)));
		return merged;
	}

	private Rectangle2D readDatasetBounds(DatasetVector dataset) {
		if (dataset == null) {
			return null;
		}
		try {
			Rectangle2D bounds = dataset.getBounds();
			if (bounds == null) {
				return null;
			}
			double width = bounds.getRight() - bounds.getLeft();
			double height = bounds.getTop() - bounds.getBottom();
			return width == 0D && height == 0D ? null : bounds;
		} catch (Exception ignored) {
			return null;
		}
	}

	private Rectangle2D mergeBounds(Rectangle2D current, Rectangle2D candidate) {
		if (candidate == null) {
			return current;
		}
		if (current == null) {
			return new Rectangle2D(candidate.getLeft(), candidate.getBottom(), candidate.getRight(), candidate.getTop());
		}
		return new Rectangle2D(
				Math.min(current.getLeft(), candidate.getLeft()),
				Math.min(current.getBottom(), candidate.getBottom()),
				Math.max(current.getRight(), candidate.getRight()),
				Math.max(current.getTop(), candidate.getTop()));
	}

	private Rectangle2D expandBounds(Rectangle2D bounds, double ratio) {
		if (bounds == null) {
			return null;
		}
		double width = bounds.getRight() - bounds.getLeft();
		double height = bounds.getTop() - bounds.getBottom();
		double actualWidth = width <= 0D ? 0.01D : width;
		double actualHeight = height <= 0D ? 0.01D : height;
		double paddingX = actualWidth * Math.max(0.05D, ratio);
		double paddingY = actualHeight * Math.max(0.05D, ratio);
		return new Rectangle2D(
				bounds.getLeft() - paddingX,
				bounds.getBottom() - paddingY,
				bounds.getRight() + paddingX,
				bounds.getTop() + paddingY);
	}

	private void trySetViewBounds(Object mapObject, Rectangle2D bounds) {
		if (mapObject == null || bounds == null) {
			return;
		}
		if (hasMethod(mapObject, "setViewBounds", Rectangle2D.class)) {
			invokeQuietly(mapObject, "setViewBounds", bounds);
			return;
		}
		Point2D center = new Point2D((bounds.getLeft() + bounds.getRight()) / 2D, (bounds.getBottom() + bounds.getTop()) / 2D);
		if (hasMethod(mapObject, "setCenter", Point2D.class)) {
			invokeQuietly(mapObject, "setCenter", center);
		}
	}

	private void refreshMapQuietly(IFormMap formMap) {
		if (formMap == null || formMap.getMapControl() == null) {
			return;
		}
		Object mapControl = formMap.getMapControl();
		Object map = invokeQuietly(mapControl, "getMap");
		invokeQuietly(map, "refresh");
		invokeQuietly(mapControl, "refresh");
		invokeQuietly(mapControl, "repaint");
	}
	private IFormMap resolveActiveFormMap() {
		Object activeForm = Application.getActiveApplication().getActiveForm();
		return activeForm instanceof IFormMap ? (IFormMap) activeForm : null;
	}

	private boolean containsLayer(IFormMap formMap, DatasetVector dataset) {
		Object layers = invokeQuietly(formMap.getMapControl().getMap(), "getLayers");
		Object countValue = invokeQuietly(layers, "getCount");
		int count = countValue instanceof Number ? ((Number) countValue).intValue() : 0;
		for (int i = 0; i < count; i++) {
			Object layer = invokeQuietly(layers, "get", Integer.valueOf(i));
			Object layerDataset = invokeQuietly(layer, "getDataset");
			if (layerDataset instanceof DatasetVector) {
				DatasetVector vector = (DatasetVector) layerDataset;
				if (dataset.getName().equalsIgnoreCase(vector.getName())) {
					return true;
				}
			}
		}
		return false;
	}

	private void applyManagedLayerCaptions(IFormMap formMap) {
		Object layers = invokeQuietly(formMap.getMapControl().getMap(), "getLayers");
		Object countValue = invokeQuietly(layers, "getCount");
		int count = countValue instanceof Number ? ((Number) countValue).intValue() : 0;
		for (int i = 0; i < count; i++) {
			Object layer = invokeQuietly(layers, "get", Integer.valueOf(i));
			Object layerDataset = invokeQuietly(layer, "getDataset");
			if (layerDataset instanceof DatasetVector) {
				DatasetVector vector = (DatasetVector) layerDataset;
				String caption = layerCatalogService.findLayerCaption(vector.getName());
				if (caption != null && !caption.trim().isEmpty()) {
					invokeQuietly(layer, "setCaption", caption.trim());
					invokeQuietly(layer, "setName", caption.trim());
				}
			}
		}
	}

	private void applyDefaultManagedLayerStyle(IFormMap formMap, DatasetVector dataset) {
		if (formMap == null || dataset == null) {
			return;
		}
		Object layer = findLayer(formMap, dataset.getName());
		if (layer == null) {
			return;
		}
		invokeQuietly(layer, "setVisible", Boolean.TRUE);
		invokeQuietly(layer, "setSelectable", Boolean.TRUE);
		invokeQuietly(layer, "setEditable", Boolean.FALSE);
		Object style = invokeQuietly(layer, "getStyle");
		if (style == null) {
			return;
		}
		String datasetName = dataset.getName();
		if (schemaService.isManagedEquipmentDataset(datasetName)) {
			invokeQuietly(style, "setMarkerSize", Integer.valueOf(4));
			invokeQuietly(style, "setLineWidth", Double.valueOf(0.3D));
		} else if (OneModelSchemaService.CONNECTION_DATASET.equalsIgnoreCase(datasetName)) {
			Color connectionColor = new Color(220, 65, 45);
			invokeQuietly(style, "setLineWidth", Double.valueOf(1.2D));
			invokeQuietly(style, "setLineColor", connectionColor);
			invokeQuietly(style, "setForeColor", connectionColor);
		} else if (OneModelSchemaService.AREA_DATASET.equalsIgnoreCase(datasetName)) {
			invokeQuietly(style, "setLineWidth", Double.valueOf(0.5D));
		}
	}
	private Object findLayer(IFormMap formMap, String datasetName) {
		Object layers = invokeQuietly(formMap.getMapControl().getMap(), "getLayers");
		Object countValue = invokeQuietly(layers, "getCount");
		int count = countValue instanceof Number ? ((Number) countValue).intValue() : 0;
		for (int i = 0; i < count; i++) {
			Object layer = invokeQuietly(layers, "get", Integer.valueOf(i));
			Object layerDataset = invokeQuietly(layer, "getDataset");
			if (layerDataset instanceof DatasetVector) {
				DatasetVector vector = (DatasetVector) layerDataset;
				if (datasetName.equalsIgnoreCase(vector.getName())) {
					return layer;
				}
			}
		}
		return null;
	}

	private Object invokeQuietly(Object target, String methodName, Object... args) {
		if (target == null) {
			return null;
		}
		Class<?>[] parameterTypes = new Class<?>[args.length];
		for (int i = 0; i < args.length; i++) {
			if (args[i] instanceof Integer) {
				parameterTypes[i] = int.class;
			} else if (args[i] instanceof Double) {
				parameterTypes[i] = double.class;
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

	private boolean hasMethod(Object target, String methodName, Class<?>... parameterTypes) {
		if (target == null) {
			return false;
		}
		try {
			target.getClass().getMethod(methodName, parameterTypes);
			return true;
		} catch (Exception ignored) {
			return false;
		}
	}
}














