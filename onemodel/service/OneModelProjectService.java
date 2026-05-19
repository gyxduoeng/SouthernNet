package com.aircas.onemodel.service;

import com.aircas.onemodel.model.OneModelParameters;
import com.supermap.data.Datasource;
import com.supermap.data.Workspace;
import com.supermap.data.WorkspaceConnectionInfo;
import com.supermap.desktop.controls.utilities.MapViewUIUtilities;
import com.supermap.desktop.core.Application;
import com.supermap.desktop.core.Interface.IFormMap;
import com.supermap.desktop.core.utilties.WorkspaceUtilities;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * OneModel 工程管理服务。
 */
public class OneModelProjectService {

	private static final String REGISTRY_FILE_NAME = "project-registry.properties";
	private static final String PROJECT_CONFIG_NAME = "project.properties";
	private static final String CURRENT_PROJECT_KEY = "currentProjectId";
	private static final String REGISTRY_PREFIX = "project.";

	private final OmPathSupport pathSupport = new OmPathSupport();
	private final OneModelWorkspaceBridge workspaceBridge = new OneModelWorkspaceBridge();
	private final OneModelMapBridge mapBridge = new OneModelMapBridge();

	public List<OneModelParameters> listProjects() {
		RegistryState registry = loadRegistry();
		List<OneModelParameters> result = new ArrayList<>();
		for (String projectId : registry.projectPaths.keySet()) {
			OneModelParameters parameters = loadProjectQuietly(projectId, registry.projectPaths.get(projectId));
			if (parameters != null) {
				result.add(parameters);
			}
		}
		result.sort(Comparator.comparing(OneModelParameters::getProjectName, String.CASE_INSENSITIVE_ORDER));
		return result;
	}

	public OneModelParameters getCurrentProject() {
		RegistryState registry = loadRegistry();
		if (isBlank(registry.currentProjectId)) {
			return null;
		}
		return loadProjectQuietly(registry.currentProjectId, registry.projectPaths.get(registry.currentProjectId));
	}

	public OneModelParameters createProject(ProjectDraft draft) {
		validateDraft(draft, true);
		Path projectFolder = normalizeDirectory(draft.getProjectFolder());
		Path configFile = resolveProjectConfigFile(projectFolder);
		if (Files.exists(configFile)) {
			throw new IllegalArgumentException("目标目录下已存在 OneModel 工程配置，请改用“选择工程”或更换目录。\n" + projectFolder);
		}
		try {
			Files.createDirectories(projectFolder);
			Files.createDirectories(resolveProjectRuntimeDir(projectFolder));
		} catch (IOException e) {
			throw new IllegalStateException("创建工程目录失败：" + projectFolder, e);
		}
		OneModelParameters parameters = buildParameters(newProjectId(draft.getProjectName()), projectFolder, draft);
		writeProjectConfig(parameters);
		RegistryState registry = loadRegistry();
		registry.currentProjectId = parameters.getProjectId();
		registry.projectPaths.put(parameters.getProjectId(), projectFolder.toString());
		saveRegistry(registry);
		try {
			openProjectWorkspace(parameters, true);
		} catch (Exception ex) {
			registry.currentProjectId = "";
			saveRegistry(registry);
			throw ex;
		}
		return parameters.copy();
	}

	public OneModelParameters selectProject(String projectId) {
		OneModelParameters parameters = requireProject(projectId);
		RegistryState registry = loadRegistry();
		registry.currentProjectId = parameters.getProjectId();
		registry.projectPaths.put(parameters.getProjectId(), parameters.getProjectFolder());
		saveRegistry(registry);
		try {
			openProjectWorkspace(parameters, false);
		} catch (Exception ex) {
			registry.currentProjectId = "";
			saveRegistry(registry);
			throw ex;
		}
		return parameters.copy();
	}

	public OneModelParameters updateProject(ProjectDraft draft) {
		validateDraft(draft, false);
		OneModelParameters existing = requireProject(draft.getProjectId());
		OneModelParameters updated = existing.copy();
		updated.setProjectName(draft.getProjectName().trim());
		updated.setStationName(draft.getStationName().trim());
		updated.setVoltageLevel(draft.getVoltageLevel().trim());
		updated.setVersionId(defaultIfBlank(draft.getVersionId(), updated.getVersionId()));
		updated.setModelLibraryPath(trimToEmpty(draft.getModelLibraryPath()));
		updated.setProjectFolder(existing.getProjectFolder());
		updated.setWorkspaceFilePath(existing.getWorkspaceFilePath());
		updated.setProjectMapName(defaultIfBlank(draft.getProjectMapName(), updated.getProjectMapName()));
		updated.setCoordinateSystemCode(defaultIfBlank(draft.getCoordinateSystemCode(), updated.getCoordinateSystemCode()));
		writeProjectConfig(updated);
		OneModelParameters current = getCurrentProject();
		if (current != null && Objects.equals(current.getProjectId(), updated.getProjectId())) {
			openProjectWorkspace(updated, false);
		}
		return updated.copy();
	}

	public void deleteProject(String projectId, boolean deleteFiles) {
		OneModelParameters parameters = requireProject(projectId);
		if (deleteFiles && isCurrentProject(projectId)) {
			throw new IllegalStateException("当前已打开工程禁止删除工程目录，请先关闭工程后再删除目录。");
		}
		RegistryState registry = loadRegistry();
		registry.projectPaths.remove(projectId);
		if (Objects.equals(registry.currentProjectId, projectId)) {
			registry.currentProjectId = "";
		}
		saveRegistry(registry);
		if (deleteFiles) {
			deleteDirectory(Paths.get(parameters.getProjectFolder()));
		}
	}

	public boolean isCurrentProject(String projectId) {
		OneModelParameters current = getCurrentProject();
		return current != null && Objects.equals(current.getProjectId(), projectId);
	}

	public void clearCurrentProjectSelection() {
		RegistryState registry = loadRegistry();
		if (isBlank(registry.currentProjectId)) {
			return;
		}
		registry.currentProjectId = "";
		saveRegistry(registry);
	}

	public OneModelParameters ensureCurrentProjectReady() {
		OneModelParameters current = getCurrentProject();
		if (current == null) {
			throw new IllegalStateException("请先通过“工程管理”新建或选择工程。当前没有活动工程。");
		}
		openProjectWorkspace(current, false);
		return current;
	}

	public void closeCurrentProject() {
		RegistryState registry = loadRegistry();
		registry.currentProjectId = "";
		saveRegistry(registry);
		closeAllFormsQuietly();
		Workspace workspace = Application.getActiveApplication().getWorkspace();
		invokeQuietly(workspace, "close");
	}

	public String buildSummary(OneModelParameters parameters) {
		if (parameters == null) {
			return "当前未选择 OneModel 工程。";
		}
		return "工程：" + parameters.getProjectName() + "\n"
				+ "电站：" + parameters.getStationName() + "\n"
				+ "电压等级：" + parameters.getVoltageLevel() + "\n"
				+ "版本：" + parameters.getVersionId() + "\n"
				+ "坐标系 EPSG：" + parameters.getCoordinateSystemCode() + "\n"
				+ "工程地图：" + parameters.getProjectMapName() + "\n"
				+ "工作空间：" + parameters.getWorkspaceFilePath() + "\n"
				+ "模型库：" + (isBlank(parameters.getModelLibraryPath()) ? "使用默认扫描路径" : parameters.getModelLibraryPath());
	}

	private void openProjectWorkspace(OneModelParameters parameters, boolean createIfMissing) {
		Path workspaceFile = Paths.get(parameters.getWorkspaceFilePath()).toAbsolutePath().normalize();
		if (!Files.exists(workspaceFile)) {
			if (!createIfMissing || !tryCreateWorkspaceFile(workspaceFile, parameters.getProjectName())) {
				throw new IllegalStateException("无法创建工程工作空间：" + workspaceFile);
			}
		}
		WorkspaceConnectionInfo info = new WorkspaceConnectionInfo(workspaceFile.toString());
		Object result = WorkspaceUtilities.openWorkspace(info, true);
		if (result == null || !String.valueOf(result).toUpperCase(Locale.ROOT).contains("SUCCESSED")) {
			throw new IllegalStateException("打开工程工作空间失败：" + workspaceFile + "，结果：" + result);
		}
		workspaceBridge.setWorkspaceCaptionQuietly(parameters.getProjectName());
		closeAllFormsQuietly();
		Datasource datasource = workspaceBridge.getOrCreateSharedDatasource();
		new OneModelMapRepository().initializeRuntimeSchema();
		IFormMap formMap = mapBridge.openOrCreateWorkingMap(datasource, parameters.getProjectMapName());
		if (formMap == null) {
			MapViewUIUtilities.openMap(parameters.getProjectMapName());
		}
		workspaceBridge.saveWorkspaceQuietly();
	}

	private void closeAllFormsQuietly() {
		try {
			Object mainFrame = Application.getActiveApplication().getMainFrame();
			Object formManager = invokeQuietly(mainFrame, "getFormManager");
			invokeQuietly(formManager, "closeAll");
		} catch (Exception ignored) {
			// 不阻塞工程切换。
		}
	}

	private boolean tryCreateWorkspaceFile(Path workspaceFile, String workspaceCaption) {
		try {
			Files.createDirectories(workspaceFile.getParent());
		} catch (IOException e) {
			return false;
		}
		Workspace blankWorkspace = new Workspace();
		try {
			if (trySaveWorkspace(blankWorkspace, workspaceFile, workspaceCaption)) {
				return true;
			}
		} finally {
			invokeQuietly(blankWorkspace, "dispose");
		}
		Workspace workspace = Application.getActiveApplication().getWorkspace();
		if (workspace == null) {
			return false;
		}
		return trySaveWorkspace(workspace, workspaceFile, workspaceCaption);
	}

	private boolean trySaveWorkspace(Workspace workspace, Path workspaceFile, String workspaceCaption) {
		workspaceBridge.setWorkspaceCaptionQuietly(workspace,
				isBlank(workspaceCaption)
						? (workspaceFile.getFileName() == null ? "未命名工程" : stripExtension(workspaceFile.getFileName().toString()))
						: workspaceCaption);
		WorkspaceConnectionInfo info = new WorkspaceConnectionInfo(workspaceFile.toString());
		if (invokeBoolean(workspace, "saveAs", new Class<?>[]{String.class}, workspaceFile.toString())) {
			return true;
		}
		if (invokeBoolean(workspace, "saveAs", new Class<?>[]{String.class, boolean.class}, workspaceFile.toString(), true)) {
			return true;
		}
		if (invokeBoolean(workspace, "saveAs", new Class<?>[]{WorkspaceConnectionInfo.class}, info)) {
			return true;
		}
		if (invokeBoolean(workspace, "saveAs", new Class<?>[]{WorkspaceConnectionInfo.class, boolean.class}, info, true)) {
			return true;
		}
		invokeQuietly(workspace, "save");
		return Files.exists(workspaceFile);
	}

	private OneModelParameters requireProject(String projectId) {
		RegistryState registry = loadRegistry();
		OneModelParameters parameters = loadProjectQuietly(projectId, registry.projectPaths.get(projectId));
		if (parameters == null) {
			throw new IllegalArgumentException("未找到指定工程：" + projectId);
		}
		return parameters;
	}

	private void validateDraft(ProjectDraft draft, boolean requireFolder) {
		if (draft == null) {
			throw new IllegalArgumentException("工程信息不能为空。");
		}
		if (isBlank(draft.getProjectName())) {
			throw new IllegalArgumentException("工程名称不能为空。");
		}
		if (isBlank(draft.getStationName())) {
			throw new IllegalArgumentException("电站名称不能为空。");
		}
		if (isBlank(draft.getVoltageLevel())) {
			throw new IllegalArgumentException("电压等级不能为空。");
		}
		if (isBlank(draft.getProjectMapName())) {
			throw new IllegalArgumentException("工程地图名称不能为空。");
		}
		if (requireFolder && isBlank(draft.getProjectFolder())) {
			throw new IllegalArgumentException("工程目录不能为空。");
		}
		if (!requireFolder && isBlank(draft.getProjectId())) {
			throw new IllegalArgumentException("缺少要更新的工程标识。");
		}
	}

	private OneModelParameters buildParameters(String projectId, Path projectFolder, ProjectDraft draft) {
		OneModelParameters parameters = new OneModelParameters();
		parameters.setProjectId(projectId);
		parameters.setProjectName(draft.getProjectName().trim());
		parameters.setStationName(draft.getStationName().trim());
		parameters.setVoltageLevel(draft.getVoltageLevel().trim());
		parameters.setVersionId(defaultIfBlank(draft.getVersionId(), parameters.getVersionId()));
		parameters.setModelLibraryPath(trimToEmpty(draft.getModelLibraryPath()));
		parameters.setProjectFolder(projectFolder.toString());
		parameters.setWorkspaceFilePath(projectFolder.resolve(buildWorkspaceFileName(draft.getProjectName())).toString());
		parameters.setProjectMapName(defaultIfBlank(draft.getProjectMapName(), parameters.getProjectMapName()));
		parameters.setCoordinateSystemCode(defaultIfBlank(draft.getCoordinateSystemCode(), parameters.getCoordinateSystemCode()));
		return parameters;
	}

	private void writeProjectConfig(OneModelParameters parameters) {
		Path file = resolveProjectConfigFile(Paths.get(parameters.getProjectFolder()));
		Properties properties = new Properties();
		properties.setProperty("projectId", safe(parameters.getProjectId()));
		properties.setProperty("projectName", safe(parameters.getProjectName()));
		properties.setProperty("stationName", safe(parameters.getStationName()));
		properties.setProperty("voltageLevel", safe(parameters.getVoltageLevel()));
		properties.setProperty("versionId", safe(parameters.getVersionId()));
		properties.setProperty("modelLibraryPath", safe(parameters.getModelLibraryPath()));
		properties.setProperty("projectFolder", safe(parameters.getProjectFolder()));
		properties.setProperty("workspaceFilePath", safe(parameters.getWorkspaceFilePath()));
		properties.setProperty("projectMapName", safe(parameters.getProjectMapName()));
		properties.setProperty("coordinateSystemCode", safe(parameters.getCoordinateSystemCode()));
		try {
			Files.createDirectories(file.getParent());
			try (OutputStream outputStream = Files.newOutputStream(file)) {
				properties.store(outputStream, "OneModel Project Settings");
			}
		} catch (IOException e) {
			throw new IllegalStateException("写入工程配置失败：" + file, e);
		}
	}

	private OneModelParameters loadProjectQuietly(String projectId, String projectFolder) {
		if (isBlank(projectId) || isBlank(projectFolder)) {
			return null;
		}
		Path file = resolveProjectConfigFile(Paths.get(projectFolder));
		if (!Files.exists(file)) {
			return null;
		}
		Properties properties = new Properties();
		try (InputStream inputStream = Files.newInputStream(file)) {
			properties.load(inputStream);
			OneModelParameters parameters = new OneModelParameters();
			parameters.setProjectId(properties.getProperty("projectId", projectId));
			parameters.setProjectName(properties.getProperty("projectName", parameters.getProjectName()));
			parameters.setStationName(properties.getProperty("stationName", parameters.getStationName()));
			parameters.setVoltageLevel(properties.getProperty("voltageLevel", parameters.getVoltageLevel()));
			parameters.setVersionId(properties.getProperty("versionId", parameters.getVersionId()));
			parameters.setModelLibraryPath(properties.getProperty("modelLibraryPath", parameters.getModelLibraryPath()));
			String resolvedProjectFolder = Paths.get(properties.getProperty("projectFolder", projectFolder)).toAbsolutePath().normalize().toString();
			parameters.setProjectFolder(resolvedProjectFolder);
			parameters.setWorkspaceFilePath(normalizeWorkspaceFilePath(resolvedProjectFolder,
					properties.getProperty("workspaceFilePath",
							Paths.get(resolvedProjectFolder).resolve(buildWorkspaceFileName(parameters.getProjectName())).toString())));
			parameters.setProjectMapName(properties.getProperty("projectMapName", parameters.getProjectMapName()));
			parameters.setCoordinateSystemCode(properties.getProperty("coordinateSystemCode", parameters.getCoordinateSystemCode()));
			return parameters;
		} catch (IOException ignored) {
			return null;
		}
	}

	private RegistryState loadRegistry() {
		Path file = pathSupport.resolveGlobalConfigDir().resolve(REGISTRY_FILE_NAME);
		if (!Files.exists(file)) {
			return new RegistryState();
		}
		Properties properties = new Properties();
		try (InputStream inputStream = Files.newInputStream(file)) {
			properties.load(inputStream);
			RegistryState state = new RegistryState();
			state.currentProjectId = properties.getProperty(CURRENT_PROJECT_KEY, "");
			for (String key : properties.stringPropertyNames()) {
				if (key.startsWith(REGISTRY_PREFIX) && key.endsWith(".path")) {
					String projectId = key.substring(REGISTRY_PREFIX.length(), key.length() - 5);
					state.projectPaths.put(projectId, properties.getProperty(key, ""));
				}
			}
			return state;
		} catch (IOException ignored) {
			return new RegistryState();
		}
	}

	private void saveRegistry(RegistryState state) {
		Path file = pathSupport.resolveGlobalConfigDir().resolve(REGISTRY_FILE_NAME);
		Properties properties = new Properties();
		properties.setProperty(CURRENT_PROJECT_KEY, safe(state.currentProjectId));
		for (Map.Entry<String, String> entry : state.projectPaths.entrySet()) {
			properties.setProperty(REGISTRY_PREFIX + entry.getKey() + ".path", safe(entry.getValue()));
		}
		try {
			Files.createDirectories(file.getParent());
			try (OutputStream outputStream = Files.newOutputStream(file)) {
				properties.store(outputStream, "OneModel Project Registry");
			}
		} catch (IOException e) {
			throw new IllegalStateException("写入工程注册表失败：" + file, e);
		}
	}

	private Path resolveProjectConfigFile(Path projectFolder) {
		return resolveProjectRuntimeDir(projectFolder).resolve(PROJECT_CONFIG_NAME);
	}

	private Path resolveProjectRuntimeDir(Path projectFolder) {
		return projectFolder.toAbsolutePath().normalize().resolve("_onemodel");
	}

	private String normalizeWorkspaceFilePath(String projectFolder, String workspaceFilePath) {
		Path folder = Paths.get(projectFolder).toAbsolutePath().normalize();
		if (workspaceFilePath == null || workspaceFilePath.trim().isEmpty()) {
			return folder.resolve("project.smwu").toString();
		}
		Path configured = Paths.get(workspaceFilePath).toAbsolutePath().normalize();
		if (configured.startsWith(folder)) {
			return configured.toString();
		}
		return folder.resolve(configured.getFileName() == null ? "project.smwu" : configured.getFileName().toString()).toString();
	}

	private Path normalizeDirectory(String directory) {
		return Paths.get(directory).toAbsolutePath().normalize();
	}

	private String buildWorkspaceFileName(String projectName) {
		return sanitizeName(projectName) + ".smwu";
	}

	private String newProjectId(String projectName) {
		return sanitizeName(projectName).toLowerCase(Locale.ROOT) + "-" + System.currentTimeMillis();
	}

	private String sanitizeName(String text) {
		String value = text == null ? "project" : text.trim();
		value = value.replaceAll("[\\\\/:*?\"<>|]+", "-");
		value = value.replaceAll("\\s+", "-");
		value = value.replaceAll("-+", "-");
		value = value.replaceAll("^-|-$", "");
		return value.isEmpty() ? "project" : value;
	}

	private String stripExtension(String fileName) {
		if (fileName == null) {
			return "";
		}
		int index = fileName.lastIndexOf('.');
		return index <= 0 ? fileName : fileName.substring(0, index);
	}

	private void deleteDirectory(Path directory) {
		if (directory == null || !Files.exists(directory)) {
			return;
		}
		try (java.util.stream.Stream<Path> stream = Files.walk(directory)) {
			List<Path> paths = new ArrayList<>();
			stream.forEach(paths::add);
			paths.sort(Collections.reverseOrder());
			for (Path path : paths) {
				Files.deleteIfExists(path);
			}
		} catch (IOException e) {
			throw new IllegalStateException("删除工程目录失败：" + directory, e);
		}
	}

	private boolean invokeBoolean(Object target, String methodName, Class<?>[] parameterTypes, Object... args) {
		Object value = invokeQuietly(target, methodName, parameterTypes, args);
		return value instanceof Boolean ? (Boolean) value : value != null;
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

	private Object invokeQuietly(Object target, String methodName) {
		return invokeQuietly(target, methodName, new Class<?>[0]);
	}

	private String defaultIfBlank(String value, String fallback) {
		return isBlank(value) ? fallback : value.trim();
	}

	private String trimToEmpty(String value) {
		return value == null ? "" : value.trim();
	}

	private String safe(String value) {
		return value == null ? "" : value;
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	private static final class RegistryState {
		private String currentProjectId = "";
		private final Map<String, String> projectPaths = new LinkedHashMap<>();
	}

	public static final class ProjectDraft {
		private String projectId;
		private String projectName;
		private String stationName;
		private String voltageLevel;
		private String versionId;
		private String modelLibraryPath;
		private String projectFolder;
		private String projectMapName;
		private String coordinateSystemCode;

		public String getProjectId() {
			return projectId;
		}

		public ProjectDraft setProjectId(String projectId) {
			this.projectId = projectId;
			return this;
		}

		public String getProjectName() {
			return projectName;
		}

		public ProjectDraft setProjectName(String projectName) {
			this.projectName = projectName;
			return this;
		}

		public String getStationName() {
			return stationName;
		}

		public ProjectDraft setStationName(String stationName) {
			this.stationName = stationName;
			return this;
		}

		public String getVoltageLevel() {
			return voltageLevel;
		}

		public ProjectDraft setVoltageLevel(String voltageLevel) {
			this.voltageLevel = voltageLevel;
			return this;
		}

		public String getVersionId() {
			return versionId;
		}

		public ProjectDraft setVersionId(String versionId) {
			this.versionId = versionId;
			return this;
		}

		public String getModelLibraryPath() {
			return modelLibraryPath;
		}

		public ProjectDraft setModelLibraryPath(String modelLibraryPath) {
			this.modelLibraryPath = modelLibraryPath;
			return this;
		}

		public String getProjectFolder() {
			return projectFolder;
		}

		public ProjectDraft setProjectFolder(String projectFolder) {
			this.projectFolder = projectFolder;
			return this;
		}

		public String getProjectMapName() {
			return projectMapName;
		}

		public ProjectDraft setProjectMapName(String projectMapName) {
			this.projectMapName = projectMapName;
			return this;
		}

		public String getCoordinateSystemCode() {
			return coordinateSystemCode;
		}

		public ProjectDraft setCoordinateSystemCode(String coordinateSystemCode) {
			this.coordinateSystemCode = coordinateSystemCode;
			return this;
		}
	}
}




