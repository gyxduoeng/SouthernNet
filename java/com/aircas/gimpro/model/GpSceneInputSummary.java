package com.aircas.gimpro.model;

import java.nio.file.Path;

/**
 * GIM Pro 三维接入输入摘要。
 */
public class GpSceneInputSummary {

	private final String projectId;
	private final String projectName;
	private final String stationName;
	private final String voltageLevel;
	private final String versionId;
	private final String coordinateSystemCode;
	private final String projectMapName;
	private final String modelLibraryPath;
	private final Path projectFolder;
	private final Path workspaceFile;
	private final Path oneModelRuntimeDir;
	private final Path oneModelScenePlanFile;
	private final Path gimProRuntimeDir;
	private final Path sharedDatasourcePath;
	private final int sceneNodeCount;
	private final boolean scenePlanExists;
	private final boolean workspaceFileExists;
	private final boolean modelLibraryExists;
	private final boolean sharedDatasourceExists;
	private final long scenePlanSize;

	public GpSceneInputSummary(String projectId, String projectName, String stationName, String voltageLevel,
			String versionId, String coordinateSystemCode, String projectMapName, String modelLibraryPath,
			Path projectFolder, Path workspaceFile, Path oneModelRuntimeDir, Path oneModelScenePlanFile,
			Path gimProRuntimeDir, Path sharedDatasourcePath, int sceneNodeCount, boolean scenePlanExists,
			boolean workspaceFileExists, boolean modelLibraryExists, boolean sharedDatasourceExists, long scenePlanSize) {
		this.projectId = safe(projectId);
		this.projectName = safe(projectName);
		this.stationName = safe(stationName);
		this.voltageLevel = safe(voltageLevel);
		this.versionId = safe(versionId);
		this.coordinateSystemCode = safe(coordinateSystemCode);
		this.projectMapName = safe(projectMapName);
		this.modelLibraryPath = safe(modelLibraryPath);
		this.projectFolder = projectFolder;
		this.workspaceFile = workspaceFile;
		this.oneModelRuntimeDir = oneModelRuntimeDir;
		this.oneModelScenePlanFile = oneModelScenePlanFile;
		this.gimProRuntimeDir = gimProRuntimeDir;
		this.sharedDatasourcePath = sharedDatasourcePath;
		this.sceneNodeCount = Math.max(sceneNodeCount, 0);
		this.scenePlanExists = scenePlanExists;
		this.workspaceFileExists = workspaceFileExists;
		this.modelLibraryExists = modelLibraryExists;
		this.sharedDatasourceExists = sharedDatasourceExists;
		this.scenePlanSize = Math.max(scenePlanSize, -1L);
	}

	public String getProjectId() {
		return projectId;
	}

	public String getProjectName() {
		return projectName;
	}

	public String getStationName() {
		return stationName;
	}

	public String getVoltageLevel() {
		return voltageLevel;
	}

	public String getVersionId() {
		return versionId;
	}

	public String getCoordinateSystemCode() {
		return coordinateSystemCode;
	}

	public String getProjectMapName() {
		return projectMapName;
	}

	public String getModelLibraryPath() {
		return modelLibraryPath;
	}

	public Path getProjectFolder() {
		return projectFolder;
	}

	public Path getWorkspaceFile() {
		return workspaceFile;
	}

	public Path getOneModelRuntimeDir() {
		return oneModelRuntimeDir;
	}

	public Path getOneModelScenePlanFile() {
		return oneModelScenePlanFile;
	}

	public Path getGimProRuntimeDir() {
		return gimProRuntimeDir;
	}

	public Path getSharedDatasourcePath() {
		return sharedDatasourcePath;
	}

	public int getSceneNodeCount() {
		return sceneNodeCount;
	}

	public boolean isScenePlanExists() {
		return scenePlanExists;
	}

	public boolean isWorkspaceFileExists() {
		return workspaceFileExists;
	}

	public boolean isModelLibraryExists() {
		return modelLibraryExists;
	}

	public boolean isSharedDatasourceExists() {
		return sharedDatasourceExists;
	}

	public long getScenePlanSize() {
		return scenePlanSize;
	}

	private String safe(String value) {
		return value == null ? "" : value.trim();
	}
}

