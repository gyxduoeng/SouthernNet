package com.aircas.onemodel.service;

import com.supermap.data.Datasource;
import com.supermap.data.DatasourceConnectionInfo;
import com.supermap.data.EngineType;
import com.supermap.data.Workspace;
import com.supermap.desktop.core.Application;
import com.supermap.desktop.core.Interface.IFormMap;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * OneModel 与宿主工作空间桥接。
 */
public class OneModelWorkspaceBridge {

	public static final String SHARED_DATASOURCE_ALIAS = "GIM_SHARED_MAIN";

	private final OmPathSupport pathSupport = new OmPathSupport();
	private final OneModelCoordinateSystemSupport coordinateSystemSupport = new OneModelCoordinateSystemSupport();

	public Workspace getActiveWorkspace() {
		Workspace workspace = Application.getActiveApplication().getWorkspace();
		if (workspace == null) {
			throw new IllegalStateException("当前未获取到 iDesktopX 工作空间。");
		}
		return workspace;
	}

	public Datasource getOrCreateSharedDatasource() {
		Workspace workspace = getActiveWorkspace();
		Datasource datasource = workspace.getDatasources().get(SHARED_DATASOURCE_ALIAS);
		if (datasource != null) {
			coordinateSystemSupport.applyToDatasourceQuietly(datasource, coordinateSystemSupport.resolveCurrentEpsgCode());
			return datasource;
		}
		Path datasourcePath = resolveWorkspaceRuntimeDir().resolve("GIM_SHARED_MAIN.udbx");
		try {
			Files.createDirectories(datasourcePath.getParent());
		} catch (IOException e) {
			throw new IllegalStateException("创建 OneModel 运行目录失败：" + datasourcePath.getParent(), e);
		}
		DatasourceConnectionInfo info = new DatasourceConnectionInfo();
		info.setAlias(SHARED_DATASOURCE_ALIAS);
		info.setEngineType(EngineType.UDBX);
		info.setServer(datasourcePath.toString());
		info.setReadOnly(false);
		info.setAutoConnect(true);
		coordinateSystemSupport.applyToDatasourceConnectionInfoQuietly(info, coordinateSystemSupport.resolveCurrentEpsgCode());
		datasource = Files.exists(datasourcePath)
				? workspace.getDatasources().open(info)
				: workspace.getDatasources().create(info);
		if (datasource == null) {
			throw new IllegalStateException("无法打开或创建共享数据源：" + datasourcePath);
		}
		coordinateSystemSupport.applyToDatasourceQuietly(datasource, coordinateSystemSupport.resolveCurrentEpsgCode());
		return datasource;
	}

	public Path resolveWorkspaceRuntimeDir() {
		try {
			Path workspaceHome = resolveWorkspaceHome(getActiveWorkspace());
			if (workspaceHome != null) {
				return workspaceHome.resolve("_onemodel");
			}
		} catch (Exception ignored) {
			// 回退到历史运行目录。
		}
		return pathSupport.resolveRuntimeDir();
	}

	public boolean isManagedRuntimeDataset(String datasourceAlias, String datasetName) {
		OneModelSchemaService schemaService = new OneModelSchemaService();
		return SHARED_DATASOURCE_ALIAS.equalsIgnoreCase(nullToEmpty(datasourceAlias))
				&& (OneModelSchemaService.AREA_DATASET.equalsIgnoreCase(nullToEmpty(datasetName))
				|| schemaService.isManagedEquipmentDataset(nullToEmpty(datasetName))
				|| OneModelSchemaService.CONNECTION_DATASET.equalsIgnoreCase(nullToEmpty(datasetName)));
	}

	public void saveWorkspaceQuietly() {
		try {
			Workspace workspace = getActiveWorkspace();
			saveOpenedMapsQuietly(workspace);
			workspace.save();
			markOpenedMapsSavedQuietly();
		} catch (Exception ignored) {
			// 不阻塞当前会话。
		}
	}

	public boolean isCurrentWorkspaceFile(String workspaceFilePath) {
		try {
			String currentPath = readWorkspacePath(getActiveWorkspace());
			if (currentPath == null || currentPath.trim().isEmpty() || workspaceFilePath == null || workspaceFilePath.trim().isEmpty()) {
				return false;
			}
			return Paths.get(currentPath).toAbsolutePath().normalize().equals(Paths.get(workspaceFilePath).toAbsolutePath().normalize());
		} catch (Exception ignored) {
			return false;
		}
	}

	public void setWorkspaceCaptionQuietly(String caption) {
		try {
			setWorkspaceCaptionQuietly(getActiveWorkspace(), caption);
		} catch (Exception ignored) {
			// 不阻塞当前会话。
		}
	}

	public void setWorkspaceCaptionQuietly(Workspace workspace, String caption) {
		if (workspace == null || caption == null || caption.trim().isEmpty()) {
			return;
		}
		String value = caption.trim();
		invokeQuietly(workspace, "setCaption", new Class<?>[]{String.class}, value);
		invokeQuietly(workspace, "setName", new Class<?>[]{String.class}, value);
		Object connectionInfo = invokeQuietly(workspace, "getConnectionInfo");
		invokeQuietly(connectionInfo, "setName", new Class<?>[]{String.class}, value);
	}

	private void saveOpenedMapsQuietly(Workspace workspace) {
		for (Object form : listOpenedForms()) {
			if (!(form instanceof IFormMap)) {
				continue;
			}
			Object map = invokeQuietly(invokeQuietly(form, "getMapControl"), "getMap");
			String mapName = readMapName(form, map);
			String xml = stringValue(invokeQuietly(map, "toXML"));
			if (mapName.isEmpty() || xml.isEmpty()) {
				continue;
			}
			try {
				if (workspace.getMaps().indexOf(mapName) >= 0) {
					workspace.getMaps().setMapXML(mapName, xml);
				} else {
					workspace.getMaps().add(mapName, xml);
				}
			} catch (Exception ignored) {
			}
		}
	}

	private void markOpenedMapsSavedQuietly() {
		for (Object form : listOpenedForms()) {
			if (!(form instanceof IFormMap)) {
				continue;
			}
			Object map = invokeQuietly(invokeQuietly(form, "getMapControl"), "getMap");
			invokeQuietly(map, "setModified", new Class<?>[]{boolean.class}, false);
		}
	}

	private java.util.List<Object> listOpenedForms() {
		java.util.List<Object> result = new java.util.ArrayList<>();
		try {
			Object activeForm = Application.getActiveApplication().getActiveForm();
			if (activeForm != null) {
				result.add(activeForm);
			}
			Object mainFrame = Application.getActiveApplication().getMainFrame();
			Object formManager = invokeQuietly(mainFrame, "getFormManager");
			Object forms = invokeQuietly(formManager, "getForms");
			int count = readCount(forms);
			for (int i = 0; i < count; i++) {
				Object form = invokeQuietly(forms, "get", new Class<?>[]{int.class}, i);
				if (form != null && !result.contains(form)) {
					result.add(form);
				}
			}
		} catch (Exception ignored) {
			// 不阻塞保存。
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

	private String readMapName(Object form, Object map) {
		String name = stringValue(invokeQuietly(map, "getName"));
		if (!name.isEmpty()) {
			return name;
		}
		name = stringValue(invokeQuietly(form, "getTitle"));
		if (!name.isEmpty()) {
			return name;
		}
		return stringValue(invokeQuietly(form, "getText"));
	}

	private Path resolveWorkspaceHome(Workspace workspace) {
		String workspacePath = readWorkspacePath(workspace);
		if (workspacePath == null || workspacePath.trim().isEmpty()) {
			return null;
		}
		Path path = Paths.get(workspacePath).toAbsolutePath().normalize();
		if (Files.isDirectory(path)) {
			return path;
		}
		if (path.getParent() != null) {
			return path.getParent();
		}
		return null;
	}

	private String readWorkspacePath(Workspace workspace) {
		Object direct = invokeQuietly(workspace, "getFilePath");
		if (direct != null && !String.valueOf(direct).trim().isEmpty()) {
			return String.valueOf(direct).trim();
		}
		Object connectionInfo = invokeQuietly(workspace, "getConnectionInfo");
		Object server = invokeQuietly(connectionInfo, "getServer");
		if (server != null && !String.valueOf(server).trim().isEmpty()) {
			return String.valueOf(server).trim();
		}
		Object fileName = invokeQuietly(workspace, "getFileName");
		return fileName == null ? null : String.valueOf(fileName).trim();
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

	private String stringValue(Object value) {
		return value == null ? "" : String.valueOf(value).trim();
	}

	private String nullToEmpty(String value) {
		return value == null ? "" : value;
	}
}
