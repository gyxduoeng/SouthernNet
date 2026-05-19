package com.aircas.gimpro.service;

import com.aircas.gimpro.model.GpSceneInputSummary;
import com.aircas.onemodel.model.OneModelParameters;
import com.aircas.onemodel.service.OneModelSessionStore;
import com.aircas.onemodel.service.OneModelWorkspaceBridge;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GIM Pro 三维接入输入服务。
 *
 * <p>负责从 OneModel 当前工程和场景规划中间文件中提取 GIM Pro 所需的输入摘要。</p>
 */
public class GpSceneInputService {

	private static final Pattern SCENE_NODE_PATTERN = Pattern.compile("\"equipmentId\"\\s*:");

	private final OneModelSessionStore sessionStore = OneModelSessionStore.getInstance();
	private final OneModelWorkspaceBridge workspaceBridge = new OneModelWorkspaceBridge();

	public GpSceneInputSummary loadCurrentSummary() {
		OneModelParameters parameters = sessionStore.getParameters();
		Path projectFolder = toPath(parameters.getProjectFolder());
		Path workspaceFile = toPath(parameters.getWorkspaceFilePath());
		Path oneModelRuntimeDir = resolveOneModelRuntimeDir();
		Path scenePlanFile = oneModelRuntimeDir.resolve("scene-plan.json");
		Path gimProRuntimeDir = resolveGimProRuntimeDir(projectFolder, oneModelRuntimeDir);
		Path sharedDatasourcePath = oneModelRuntimeDir.resolve("GIM_SHARED_MAIN.udbx");
		boolean scenePlanExists = Files.exists(scenePlanFile);
		boolean workspaceExists = workspaceFile != null && Files.exists(workspaceFile);
		Path modelLibraryPath = toPath(parameters.getModelLibraryPath());
		boolean modelLibraryExists = modelLibraryPath != null && Files.exists(modelLibraryPath);
		boolean sharedDatasourceExists = Files.exists(sharedDatasourcePath);
		long scenePlanSize = sizeOf(scenePlanFile);
		int sceneNodeCount = scenePlanExists ? countSceneNodes(scenePlanFile) : 0;
		return new GpSceneInputSummary(
				parameters.getProjectId(),
				parameters.getProjectName(),
				parameters.getStationName(),
				parameters.getVoltageLevel(),
				parameters.getVersionId(),
				parameters.getCoordinateSystemCode(),
				parameters.getProjectMapName(),
				parameters.getModelLibraryPath(),
				projectFolder,
				workspaceFile,
				oneModelRuntimeDir,
				scenePlanFile,
				gimProRuntimeDir,
				sharedDatasourcePath,
				sceneNodeCount,
				scenePlanExists,
				workspaceExists,
				modelLibraryExists,
				sharedDatasourceExists,
				scenePlanSize);
	}

	public String buildHumanReadableSummary(GpSceneInputSummary summary) {
		StringBuilder builder = new StringBuilder();
		builder.append("GIM Pro 三维接入输入摘要\n\n");
		builder.append("工程 ID：").append(summary.getProjectId()).append("\n");
		builder.append("工程名称：").append(summary.getProjectName()).append("\n");
		builder.append("电站名称：").append(summary.getStationName()).append("\n");
		builder.append("电压等级：").append(summary.getVoltageLevel()).append("\n");
		builder.append("版本号：").append(summary.getVersionId()).append("\n");
		builder.append("坐标系：EPSG:").append(summary.getCoordinateSystemCode()).append("\n");
		builder.append("工程地图：").append(summary.getProjectMapName()).append("\n");
		builder.append("工程目录：").append(path(summary.getProjectFolder())).append("\n");
		builder.append("工作空间：").append(path(summary.getWorkspaceFile())).append("\n");
		builder.append("OneModel 运行目录：").append(path(summary.getOneModelRuntimeDir())).append("\n");
		builder.append("共享数据源：").append(path(summary.getSharedDatasourcePath())).append("\n");
		builder.append("OneModel 场景规划：").append(path(summary.getOneModelScenePlanFile())).append("\n");
		builder.append("GIM Pro 运行目录：").append(path(summary.getGimProRuntimeDir())).append("\n");
		builder.append("模型库目录：").append(summary.getModelLibraryPath()).append("\n");
		builder.append("工作空间文件存在：").append(summary.isWorkspaceFileExists() ? "是" : "否").append("\n");
		builder.append("共享数据源存在：").append(summary.isSharedDatasourceExists() ? "是" : "否").append("\n");
		builder.append("场景规划文件存在：").append(summary.isScenePlanExists() ? "是" : "否").append("\n");
		builder.append("模型库目录存在：").append(summary.isModelLibraryExists() ? "是" : "否").append("\n");
		builder.append("场景节点数：").append(summary.getSceneNodeCount()).append("\n");
		builder.append("场景规划文件大小：").append(summary.getScenePlanSize()).append(" 字节\n");
		builder.append("\n说明：GIM Pro 当前基础实现优先以共享数据源为三维场景主链输入；`scene-plan.json` 仅保留为兼容输入。\n");
		return builder.toString();
	}

	private Path resolveOneModelRuntimeDir() {
		try {
			return workspaceBridge.resolveWorkspaceRuntimeDir();
		} catch (Exception ex) {
			OneModelParameters parameters = sessionStore.getParameters();
			Path projectFolder = toPath(parameters.getProjectFolder());
			if (projectFolder != null) {
				return projectFolder.resolve("_onemodel");
			}
			return Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().normalize()
					.resolve("data").resolve("_onemodel");
		}
	}

	private Path resolveGimProRuntimeDir(Path projectFolder, Path oneModelRuntimeDir) {
		if (projectFolder != null) {
			return projectFolder.resolve("_gimpro");
		}
		if (oneModelRuntimeDir != null && oneModelRuntimeDir.getParent() != null) {
			return oneModelRuntimeDir.getParent().resolve("_gimpro");
		}
		return Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().normalize()
				.resolve("data").resolve("_gimpro");
	}

	private Path toPath(String value) {
		if (value == null || value.trim().isEmpty()) {
			return null;
		}
		try {
			return Paths.get(value.trim()).toAbsolutePath().normalize();
		} catch (Exception ignored) {
			return null;
		}
	}

	private long sizeOf(Path path) {
		try {
			return path != null && Files.exists(path) ? Files.size(path) : -1L;
		} catch (IOException ignored) {
			return -1L;
		}
	}

	private int countSceneNodes(Path scenePlanFile) {
		try {
			String content = new String(Files.readAllBytes(scenePlanFile), StandardCharsets.UTF_8);
			Matcher matcher = SCENE_NODE_PATTERN.matcher(content);
			int count = 0;
			while (matcher.find()) {
				count++;
			}
			return count;
		} catch (IOException ignored) {
			return 0;
		}
	}

	private String path(Path path) {
		return path == null ? "" : path.toString();
	}
}


