package com.aircas.onemodel.service;

import com.aircas.onemodel.model.OneModelParameters;
import com.supermap.data.Dataset;
import com.supermap.data.DatasetType;
import com.supermap.data.DatasetVector;
import com.supermap.data.Datasource;
import com.supermap.data.Workspace;
import com.supermap.desktop.controls.utilities.MapViewUIUtilities;
import com.supermap.desktop.core.Application;
import com.supermap.desktop.core.Interface.IFormMap;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 第二版“读取当前地图”候选数据集发现服务。
 */
public class OneModelActiveMapService {

	private static final DatasetRef NONE_REGION = DatasetRef.none("不导入区域面", DatasetType.REGION);
	private static final DatasetRef NONE_POINT = DatasetRef.none("不导入设备点", DatasetType.POINT);
	private static final DatasetRef NONE_LINE = DatasetRef.none("不导入连接线", DatasetType.LINE);

	private final OneModelWorkspaceBridge workspaceBridge = new OneModelWorkspaceBridge();
	private final OneModelProjectService projectService = new OneModelProjectService();

	public SelectionSnapshot loadSelectionSnapshot() {
		List<DatasetRef> projectMapDatasets = listCandidateVectorDatasets();

		List<DatasetRef> areaOptions = buildOptions(DatasetType.REGION, projectMapDatasets, NONE_REGION);
		List<DatasetRef> equipmentOptions = buildOptions(DatasetType.POINT, projectMapDatasets, NONE_POINT);
		List<DatasetRef> connectionOptions = buildOptions(DatasetType.LINE, projectMapDatasets, NONE_LINE);

		return new SelectionSnapshot(
				areaOptions,
				equipmentOptions,
				connectionOptions,
				pickDefault(DatasetType.REGION, areaOptions, new String[]{"区域", "分区", "站区", "area", "region", "zone"}),
				pickDefault(DatasetType.POINT, equipmentOptions, new String[]{"设备", "点", "device", "equip", "point"}),
				pickDefault(DatasetType.LINE, connectionOptions, new String[]{"连接", "线", "line", "conn", "link", "cable", "bus"}));
	}

	public DatasetVector resolveDataset(DatasetRef ref) {
		if (ref == null || ref.isNone()) {
			return null;
		}
		Workspace workspace = workspaceBridge.getActiveWorkspace();
		Datasource datasource = workspace.getDatasources().get(ref.getDatasourceAlias());
		if (datasource == null) {
			return null;
		}
		Dataset dataset = datasource.getDatasets().get(ref.getDatasetName());
		return dataset instanceof DatasetVector ? (DatasetVector) dataset : null;
	}

	private List<DatasetRef> buildOptions(DatasetType type, List<DatasetRef> projectMapDatasets, DatasetRef noneOption) {
		List<DatasetRef> merged = new ArrayList<>();
		merged.add(noneOption);
		Set<String> seen = new LinkedHashSet<>();
		for (DatasetRef ref : projectMapDatasets) {
			if (type.equals(ref.getDatasetType()) && seen.add(ref.key())) {
				merged.add(ref);
			}
		}
		return merged;
	}

	private DatasetRef pickDefault(DatasetType type, List<DatasetRef> options, String[] preferredKeywords) {
		for (DatasetRef option : options) {
			if (!type.equals(option.getDatasetType()) || option.isNone()) {
				continue;
			}
			if ("当前地图".equals(option.getSource()) && !isRuntimeDataset(option)) {
				return option;
			}
		}
		for (String keyword : preferredKeywords) {
			for (DatasetRef option : options) {
				if (!type.equals(option.getDatasetType()) || option.isNone() || isRuntimeDataset(option)) {
					continue;
				}
				if (containsKeyword(option, keyword)) {
					return option;
				}
			}
		}
		for (DatasetRef option : options) {
			if (!type.equals(option.getDatasetType()) || option.isNone() || isRuntimeDataset(option)) {
				continue;
			}
			return option;
		}
		return options.isEmpty() ? null : options.get(0);
	}

	private boolean containsKeyword(DatasetRef option, String keyword) {
		String left = (option.getDatasetName() + " " + option.getDatasourceAlias()).toLowerCase(Locale.ROOT);
		return left.contains(keyword.toLowerCase(Locale.ROOT));
	}

	private boolean isRuntimeDataset(DatasetRef option) {
		return workspaceBridge.isManagedRuntimeDataset(option.getDatasourceAlias(), option.getDatasetName());
	}

	private List<DatasetRef> listCandidateVectorDatasets() {
		List<DatasetRef> result = new ArrayList<>();
		try {
			result.addAll(listProjectMapVectorDatasets());
		} catch (Exception ignored) {
			// 工程地图未打开时仍允许从已加载的数据源中选择外部电网图层。
		}
		result.addAll(listWorkspaceVectorDatasets());
		return deduplicate(result);
	}

	private List<DatasetRef> listWorkspaceVectorDatasets() {
		Workspace workspace = workspaceBridge.getActiveWorkspace();
		List<DatasetRef> result = new ArrayList<>();
		for (int i = 0; i < workspace.getDatasources().getCount(); i++) {
			Datasource datasource = workspace.getDatasources().get(i);
			if (datasource == null) {
				continue;
			}
			String alias = datasource.getAlias();
			for (int j = 0; j < datasource.getDatasets().getCount(); j++) {
				Dataset dataset = datasource.getDatasets().get(j);
				DatasetRef ref = dataset instanceof DatasetVector ? toDatasetRef(dataset, alias, "工作空间") : null;
				if (ref != null && !isRuntimeDataset(ref)) {
					result.add(ref);
				}
			}
		}
		return result;
	}
	private List<DatasetRef> listProjectMapVectorDatasets() {
		OneModelParameters project = projectService.getCurrentProject();
		if (project == null) {
			throw new IllegalStateException("请先通过“工程管理”新建或选择工程。");
		}
		Workspace workspace = workspaceBridge.getActiveWorkspace();
		if (workspace.getMaps().indexOf(project.getProjectMapName()) < 0) {
			throw new IllegalStateException("当前工程指定地图不存在：" + project.getProjectMapName());
		}
		MapViewUIUtilities.openMap(project.getProjectMapName());
		IFormMap formMap = resolveProjectMapForm(project.getProjectMapName());
		if (formMap == null) {
			throw new IllegalStateException("无法打开工程指定地图：" + project.getProjectMapName());
		}
		Object layers = invokeQuietly(invokeQuietly(formMap, "getMapControl"), "getMap");
		layers = invokeQuietly(layers, "getLayers");
		List<DatasetRef> result = new ArrayList<>();
		for (Object layer : iterate(layers)) {
			Object dataset = invokeQuietly(layer, "getDataset");
			DatasetRef ref = dataset instanceof DatasetVector ? toDatasetRef(dataset,
					readDatasourceAlias((DatasetVector) dataset)) : null;
			if (ref != null && !isRuntimeDataset(ref)) {
				result.add(ref);
			}
		}
		return deduplicate(result);
	}

	private IFormMap resolveProjectMapForm(String projectMapName) {
		Object activeForm = Application.getActiveApplication().getActiveForm();
		if (activeForm instanceof IFormMap && projectMapName.equals(readFormTitle(activeForm))) {
			return (IFormMap) activeForm;
		}
		Object mainFrame = Application.getActiveApplication().getMainFrame();
		Object formManager = invokeQuietly(mainFrame, "getFormManager");
		for (Object form : iterate(invokeQuietly(formManager, "getForms"))) {
			if (form instanceof IFormMap && projectMapName.equals(readFormTitle(form))) {
				return (IFormMap) form;
			}
		}
		return activeForm instanceof IFormMap ? (IFormMap) activeForm : null;
	}

	private String readFormTitle(Object form) {
		Object title = invokeQuietly(form, "getTitle");
		if (title != null && !String.valueOf(title).trim().isEmpty()) {
			return String.valueOf(title).trim();
		}
		title = invokeQuietly(form, "getText");
		return title == null ? "" : String.valueOf(title).trim();
	}

	private String readDatasourceAlias(DatasetVector dataset) {
		Object datasource = invokeQuietly(dataset, "getDatasource");
		Object alias = invokeQuietly(datasource, "getAlias");
		return alias == null ? "" : String.valueOf(alias);
	}

	private DatasetRef toDatasetRef(Object datasetObject, String datasourceAlias) {
		return toDatasetRef(datasetObject, datasourceAlias, "工程地图");
	}

	private DatasetRef toDatasetRef(Object datasetObject, String datasourceAlias, String source) {
		if (!(datasetObject instanceof DatasetVector)) {
			return null;
		}
		DatasetVector dataset = (DatasetVector) datasetObject;
		Object typeObject = invokeQuietly(dataset, "getType");
		if (!(typeObject instanceof DatasetType)) {
			return null;
		}
		DatasetType type = (DatasetType) typeObject;
		if (!(DatasetType.REGION.equals(type) || DatasetType.POINT.equals(type) || DatasetType.LINE.equals(type))) {
			return null;
		}
		String actualSource = source == null || source.trim().isEmpty() ? "工作空间" : source.trim();
		return new DatasetRef(actualSource, datasourceAlias, dataset.getName(), type, null);
	}

	private List<DatasetRef> deduplicate(List<DatasetRef> refs) {
		Map<String, DatasetRef> ordered = new LinkedHashMap<>();
		for (DatasetRef ref : refs) {
			ordered.putIfAbsent(ref.key(), ref);
		}
		return new ArrayList<>(ordered.values());
	}

	private List<Object> iterate(Object collection) {
		if (collection == null) {
			return Collections.emptyList();
		}
		int count = readCount(collection);
		if (count <= 0) {
			return Collections.emptyList();
		}
		List<Object> result = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			Object item = invokeQuietly(collection, "get", new Class<?>[]{int.class}, i);
			if (item != null) {
				result.add(item);
			}
		}
		return result;
	}

	private int readCount(Object collection) {
		Object value = invokeQuietly(collection, "getCount");
		if (value instanceof Number) {
			return ((Number) value).intValue();
		}
		value = invokeQuietly(collection, "size");
		return value instanceof Number ? ((Number) value).intValue() : 0;
	}

	private Object invokeQuietly(Object target, String methodName) {
		return invokeQuietly(target, methodName, new Class<?>[0]);
	}

	private Object invokeQuietly(Object target, String methodName, Class<?>[] parameterTypes, Object... args) {
		if (target == null) {
			return null;
		}
		try {
			Method method = target.getClass().getMethod(methodName, parameterTypes);
			method.setAccessible(true);
			return method.invoke(target, args);
		} catch (Exception ignored) {
			return null;
		}
	}

	public static final class SelectionSnapshot {

		private final List<DatasetRef> areaOptions;
		private final List<DatasetRef> equipmentOptions;
		private final List<DatasetRef> connectionOptions;
		private final DatasetRef defaultArea;
		private final DatasetRef defaultEquipment;
		private final DatasetRef defaultConnection;

		public SelectionSnapshot(List<DatasetRef> areaOptions, List<DatasetRef> equipmentOptions,
									 List<DatasetRef> connectionOptions, DatasetRef defaultArea,
									 DatasetRef defaultEquipment, DatasetRef defaultConnection) {
			this.areaOptions = areaOptions;
			this.equipmentOptions = equipmentOptions;
			this.connectionOptions = connectionOptions;
			this.defaultArea = defaultArea;
			this.defaultEquipment = defaultEquipment;
			this.defaultConnection = defaultConnection;
		}

		public List<DatasetRef> getAreaOptions() {
			return areaOptions;
		}

		public List<DatasetRef> getEquipmentOptions() {
			return equipmentOptions;
		}

		public List<DatasetRef> getConnectionOptions() {
			return connectionOptions;
		}

		public DatasetRef getDefaultArea() {
			return defaultArea;
		}

		public DatasetRef getDefaultEquipment() {
			return defaultEquipment;
		}

		public DatasetRef getDefaultConnection() {
			return defaultConnection;
		}

	}

	public static final class DatasetRef {

		private final String source;
		private final String datasourceAlias;
		private final String datasetName;
		private final DatasetType datasetType;
		private final String placeholderLabel;

		private DatasetRef(String source, String datasourceAlias, String datasetName, DatasetType datasetType, String placeholderLabel) {
			this.source = source;
			this.datasourceAlias = datasourceAlias;
			this.datasetName = datasetName;
			this.datasetType = datasetType;
			this.placeholderLabel = placeholderLabel;
		}

		public static DatasetRef none(String label, DatasetType datasetType) {
			return new DatasetRef("占位", "", "", datasetType, label);
		}

		public boolean isNone() {
			return datasetName == null || datasetName.trim().isEmpty();
		}

		public String getSource() {
			return source;
		}

		public String getDatasourceAlias() {
			return datasourceAlias;
		}

		public String getDatasetName() {
			return datasetName;
		}

		public DatasetType getDatasetType() {
			return datasetType;
		}

		public String key() {
			return datasourceAlias + "::" + datasetName;
		}

		@Override
		public String toString() {
			if (isNone()) {
				return placeholderLabel;
			}
			return String.format("[%s] %s / %s", source, datasourceAlias, datasetName);
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof DatasetRef)) {
				return false;
			}
			DatasetRef ref = (DatasetRef) other;
			return Objects.equals(source, ref.source)
					&& Objects.equals(datasourceAlias, ref.datasourceAlias)
					&& Objects.equals(datasetName, ref.datasetName)
					&& Objects.equals(datasetType, ref.datasetType)
					&& Objects.equals(placeholderLabel, ref.placeholderLabel);
		}

		@Override
		public int hashCode() {
			return Objects.hash(source, datasourceAlias, datasetName, datasetType, placeholderLabel);
		}
	}
}


