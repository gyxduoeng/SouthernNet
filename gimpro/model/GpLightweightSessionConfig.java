package com.aircas.gimpro.model;

import java.nio.file.Path;

/**
 * GIM Pro 轻量化接入配置。
 */
public class GpLightweightSessionConfig {

	private final String projectId;
	private final String projectName;
	private final Path runtimeDir;
	private final Path cacheDir;
	private final Path manifestFile;
	private final int sceneNodeCount;
	private final boolean platformManaged;

	public GpLightweightSessionConfig(String projectId, String projectName, Path runtimeDir,
			Path cacheDir, Path manifestFile, int sceneNodeCount, boolean platformManaged) {
		this.projectId = safe(projectId);
		this.projectName = safe(projectName);
		this.runtimeDir = runtimeDir;
		this.cacheDir = cacheDir;
		this.manifestFile = manifestFile;
		this.sceneNodeCount = Math.max(sceneNodeCount, 0);
		this.platformManaged = platformManaged;
	}

	public String getProjectId() {
		return projectId;
	}

	public String getProjectName() {
		return projectName;
	}

	public Path getRuntimeDir() {
		return runtimeDir;
	}

	public Path getCacheDir() {
		return cacheDir;
	}

	public Path getManifestFile() {
		return manifestFile;
	}

	public int getSceneNodeCount() {
		return sceneNodeCount;
	}

	public boolean isPlatformManaged() {
		return platformManaged;
	}

	private String safe(String value) {
		return value == null ? "" : value.trim();
	}
}

