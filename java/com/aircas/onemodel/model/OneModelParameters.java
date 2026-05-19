package com.aircas.onemodel.model;

/**
 * OneModel 主线运行参数。
 */
public class OneModelParameters {

	private String projectId = "";
	private String projectName = "OneModel 主线工程";
	private String stationName = "220kV 样板电站";
	private String voltageLevel = "220kV";
	private String versionId = "V1";
	private String modelLibraryPath = "";
	private String projectFolder = "";
	private String workspaceFilePath = "";
	private String projectMapName = "工程地图";
	private String coordinateSystemCode = "4490";

	public String getProjectId() {
		return projectId;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public String getStationName() {
		return stationName;
	}

	public void setStationName(String stationName) {
		this.stationName = stationName;
	}

	public String getVoltageLevel() {
		return voltageLevel;
	}

	public void setVoltageLevel(String voltageLevel) {
		this.voltageLevel = voltageLevel;
	}

	public String getVersionId() {
		return versionId;
	}

	public void setVersionId(String versionId) {
		this.versionId = versionId;
	}

	public String getModelLibraryPath() {
		return modelLibraryPath;
	}

	public void setModelLibraryPath(String modelLibraryPath) {
		this.modelLibraryPath = modelLibraryPath;
	}

	public String getProjectFolder() {
		return projectFolder;
	}

	public void setProjectFolder(String projectFolder) {
		this.projectFolder = projectFolder;
	}

	public String getWorkspaceFilePath() {
		return workspaceFilePath;
	}

	public void setWorkspaceFilePath(String workspaceFilePath) {
		this.workspaceFilePath = workspaceFilePath;
	}

	public String getProjectMapName() {
		return projectMapName;
	}

	public void setProjectMapName(String projectMapName) {
		this.projectMapName = projectMapName;
	}

	public String getCoordinateSystemCode() {
		return coordinateSystemCode;
	}

	public void setCoordinateSystemCode(String coordinateSystemCode) {
		this.coordinateSystemCode = coordinateSystemCode;
	}

	public OneModelParameters copy() {
		OneModelParameters copy = new OneModelParameters();
		copy.setProjectId(projectId);
		copy.setProjectName(projectName);
		copy.setStationName(stationName);
		copy.setVoltageLevel(voltageLevel);
		copy.setVersionId(versionId);
		copy.setModelLibraryPath(modelLibraryPath);
		copy.setProjectFolder(projectFolder);
		copy.setWorkspaceFilePath(workspaceFilePath);
		copy.setProjectMapName(projectMapName);
		copy.setCoordinateSystemCode(coordinateSystemCode);
		return copy;
	}
}

