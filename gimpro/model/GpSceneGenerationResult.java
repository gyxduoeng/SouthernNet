package com.aircas.gimpro.model;

import com.aircas.gimpro.adapter.GpAdaptedSceneNode;
import com.aircas.gimpro.session.GpSceneSession;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GIM Pro 三维场景生成结果。
 */
public class GpSceneGenerationResult {

	private final GpSceneSession session;
	private final List<GpAdaptedSceneNode> nodes;
	private final List<GpModelLibraryEntry> modelEntries;
	private final List<String> warnings;
	private final List<String> errors;

	public GpSceneGenerationResult(GpSceneSession session, List<GpAdaptedSceneNode> nodes,
			List<GpModelLibraryEntry> modelEntries, List<String> warnings, List<String> errors) {
		this.session = session;
		this.nodes = Collections.unmodifiableList(new ArrayList<>(nodes == null ? Collections.<GpAdaptedSceneNode>emptyList() : nodes));
		this.modelEntries = Collections.unmodifiableList(new ArrayList<>(modelEntries == null ? Collections.<GpModelLibraryEntry>emptyList() : modelEntries));
		this.warnings = Collections.unmodifiableList(new ArrayList<>(warnings == null ? Collections.<String>emptyList() : warnings));
		this.errors = Collections.unmodifiableList(new ArrayList<>(errors == null ? Collections.<String>emptyList() : errors));
	}

	public GpSceneSession getSession() {
		return session;
	}

	public List<GpAdaptedSceneNode> getNodes() {
		return nodes;
	}

	public List<GpModelLibraryEntry> getModelEntries() {
		return modelEntries;
	}

	public List<String> getWarnings() {
		return warnings;
	}

	public List<String> getErrors() {
		return errors;
	}

	public boolean isSuccess() {
		return errors.isEmpty();
	}
}

