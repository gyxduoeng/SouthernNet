package com.aircas.gimpro.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GIM Pro 多源数据整合报告。
 */
public class GpIntegrationReport {

	private final List<GpSupportedTypeEntry> supportedTypes;
	private final List<GpIntegratedSource> sources;
	private final List<String> warnings;
	private final List<String> errors;

	public GpIntegrationReport(List<GpSupportedTypeEntry> supportedTypes, List<GpIntegratedSource> sources,
			List<String> warnings, List<String> errors) {
		this.supportedTypes = Collections.unmodifiableList(new ArrayList<>(supportedTypes == null ? Collections.<GpSupportedTypeEntry>emptyList() : supportedTypes));
		this.sources = Collections.unmodifiableList(new ArrayList<>(sources == null ? Collections.<GpIntegratedSource>emptyList() : sources));
		this.warnings = Collections.unmodifiableList(new ArrayList<>(warnings == null ? Collections.<String>emptyList() : warnings));
		this.errors = Collections.unmodifiableList(new ArrayList<>(errors == null ? Collections.<String>emptyList() : errors));
	}

	public List<GpSupportedTypeEntry> getSupportedTypes() {
		return supportedTypes;
	}

	public List<GpIntegratedSource> getSources() {
		return sources;
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

