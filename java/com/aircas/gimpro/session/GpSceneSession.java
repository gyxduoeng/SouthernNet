package com.aircas.gimpro.session;

import com.aircas.gimpro.adapter.GpAdaptedSceneNode;
import com.aircas.gimpro.model.GpSceneInputSummary;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GIM Pro 三维工程会话快照。
 */
public class GpSceneSession {

	private final String sessionId;
	private final String generatedAt;
	private final String sourceMode;
	private final GpSceneInputSummary inputSummary;
	private final Path sessionFile;
	private final Path sourceScenePlanFile;
	private final List<GpAdaptedSceneNode> nodes;
	private final List<String> errors;
	private final List<String> warnings;
	private final int connectionCount;

	public GpSceneSession(String sessionId, String generatedAt, String sourceMode, GpSceneInputSummary inputSummary,
			Path sessionFile, Path sourceScenePlanFile, List<GpAdaptedSceneNode> nodes,
			List<String> errors, List<String> warnings, int connectionCount) {
		this.sessionId = safe(sessionId);
		this.generatedAt = safe(generatedAt);
		this.sourceMode = safe(sourceMode);
		this.inputSummary = inputSummary;
		this.sessionFile = sessionFile;
		this.sourceScenePlanFile = sourceScenePlanFile;
		this.nodes = Collections.unmodifiableList(new ArrayList<>(nodes == null ? Collections.emptyList() : nodes));
		this.errors = Collections.unmodifiableList(new ArrayList<>(errors == null ? Collections.emptyList() : errors));
		this.warnings = Collections.unmodifiableList(new ArrayList<>(warnings == null ? Collections.emptyList() : warnings));
		this.connectionCount = Math.max(connectionCount, 0);
	}

	public String getSessionId() {
		return sessionId;
	}

	public String getGeneratedAt() {
		return generatedAt;
	}

	public String getSourceMode() {
		return sourceMode;
	}

	public GpSceneInputSummary getInputSummary() {
		return inputSummary;
	}

	public Path getSessionFile() {
		return sessionFile;
	}

	public Path getSourceScenePlanFile() {
		return sourceScenePlanFile;
	}

	public List<GpAdaptedSceneNode> getNodes() {
		return nodes;
	}

	public List<String> getErrors() {
		return errors;
	}

	public List<String> getWarnings() {
		return warnings;
	}

	public int getNodeCount() {
		return nodes.size();
	}

	public boolean isReady() {
		return errors.isEmpty();
	}

	public int getConnectionCount() {
		return connectionCount;
	}

	public int getBoundNodeCount() {
		int count = 0;
		for (GpAdaptedSceneNode node : nodes) {
			if (node != null && node.hasModelPath()) {
				count++;
			}
		}
		return count;
	}

	private String safe(String value) {
		return value == null ? "" : value.trim();
	}
}


