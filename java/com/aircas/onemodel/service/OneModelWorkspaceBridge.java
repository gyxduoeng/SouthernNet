package com.aircas.onemodel.service;

import com.supermap.data.Datasource;
import com.supermap.data.DatasourceConnectionInfo;
import com.supermap.data.EngineType;
import com.supermap.data.Workspace;
import com.supermap.desktop.core.Application;

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
			getActiveWorkspace().save();
		} catch (Exception ignored) {
			// 不阻塞当前会话。
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

	private String nullToEmpty(String value) {
		return value == null ? "" : value;
	}
}

