package com.aircas.onemodel.model;

/**
 * 连接线记录。
 */
public class OneModelConnectionRecord {

	private final String connectionId;
	private final String fromEquipmentId;
	private final String toEquipmentId;
	private final String connectionType;
	private final String status;
	private final String versionId;
	private final String graphicType;

	public OneModelConnectionRecord(String connectionId, String fromEquipmentId, String toEquipmentId,
									  String connectionType, String status, String versionId, String graphicType) {
		this.connectionId = connectionId;
		this.fromEquipmentId = fromEquipmentId;
		this.toEquipmentId = toEquipmentId;
		this.connectionType = connectionType;
		this.status = status;
		this.versionId = versionId;
		this.graphicType = graphicType;
	}

	public String getConnectionId() {
		return connectionId;
	}

	public String getFromEquipmentId() {
		return fromEquipmentId;
	}

	public String getToEquipmentId() {
		return toEquipmentId;
	}

	public String getConnectionType() {
		return connectionType;
	}

	public String getStatus() {
		return status;
	}

	public String getVersionId() {
		return versionId;
	}

	public String getGraphicType() {
		return graphicType;
	}

	@Override
	public String toString() {
		return connectionId + " : " + fromEquipmentId + " -> " + toEquipmentId;
	}
}

