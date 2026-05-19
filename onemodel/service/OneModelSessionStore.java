package com.aircas.onemodel.service;

import com.aircas.onemodel.model.OneModelParameters;

/**
 * OneModel 当前工程参数访问入口。
 */
public class OneModelSessionStore {

	private static final OneModelSessionStore INSTANCE = new OneModelSessionStore();

	private final OneModelProjectService projectService = new OneModelProjectService();
	private final OneModelParameters defaults = new OneModelParameters();

	private OneModelSessionStore() {
	}

	public static OneModelSessionStore getInstance() {
		return INSTANCE;
	}

	public synchronized OneModelParameters getParameters() {
		OneModelParameters current = projectService.getCurrentProject();
		return current == null ? defaults.copy() : current;
	}

	public synchronized void update(String projectName, String stationName, String voltageLevel,
									String versionId, String modelLibraryPath) {
		OneModelParameters current = projectService.getCurrentProject();
		if (current == null) {
			throw new IllegalStateException("请先新建或选择工程，再更新工程参数。");
		}
		OneModelProjectService.ProjectDraft draft = new OneModelProjectService.ProjectDraft()
				.setProjectId(current.getProjectId())
				.setProjectName(defaultIfBlank(projectName, current.getProjectName()))
				.setStationName(defaultIfBlank(stationName, current.getStationName()))
				.setVoltageLevel(defaultIfBlank(voltageLevel, current.getVoltageLevel()))
				.setVersionId(defaultIfBlank(versionId, current.getVersionId()))
				.setModelLibraryPath(modelLibraryPath == null ? "" : modelLibraryPath.trim())
				.setProjectFolder(current.getProjectFolder())
				.setProjectMapName(current.getProjectMapName())
				.setCoordinateSystemCode(current.getCoordinateSystemCode());
		projectService.updateProject(draft);
	}

	public synchronized String buildSummary() {
		return projectService.buildSummary(projectService.getCurrentProject());
	}

	private String defaultIfBlank(String value, String fallback) {
		return value == null || value.trim().isEmpty() ? fallback : value.trim();
	}
}

