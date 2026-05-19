package com.aircas.gimpro.model;

/**
 * 三维碰撞问题。
 */
public class GpCollisionProblem {

	private final String problemId;
	private final String problemType;
	private final String severity;
	private final String objectA;
	private final String objectB;
	private final String location;
	private final String note;

	public GpCollisionProblem(String problemId, String problemType, String severity,
			String objectA, String objectB, String location, String note) {
		this.problemId = safe(problemId);
		this.problemType = safe(problemType);
		this.severity = safe(severity);
		this.objectA = safe(objectA);
		this.objectB = safe(objectB);
		this.location = safe(location);
		this.note = safe(note);
	}

	public String getProblemId() {
		return problemId;
	}

	public String getProblemType() {
		return problemType;
	}

	public String getSeverity() {
		return severity;
	}

	public String getObjectA() {
		return objectA;
	}

	public String getObjectB() {
		return objectB;
	}

	public String getLocation() {
		return location;
	}

	public String getNote() {
		return note;
	}

	private String safe(String value) {
		return value == null ? "" : value.trim();
	}
}

