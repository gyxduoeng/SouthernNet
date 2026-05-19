package com.aircas.onemodel.model;

/**
 * 区域面记录。
 */
public class OneModelAreaRecord {

	private final String areaId;
	private final String areaName;
	private final String areaType;
	private final String versionId;
	private final double minX;
	private final double minY;
	private final double maxX;
	private final double maxY;

	public OneModelAreaRecord(String areaId, String areaName, String areaType, String versionId,
							 double minX, double minY, double maxX, double maxY) {
		this.areaId = areaId;
		this.areaName = areaName;
		this.areaType = areaType;
		this.versionId = versionId;
		this.minX = minX;
		this.minY = minY;
		this.maxX = maxX;
		this.maxY = maxY;
	}

	public String getAreaId() {
		return areaId;
	}

	public String getAreaName() {
		return areaName;
	}

	public String getAreaType() {
		return areaType;
	}

	public String getVersionId() {
		return versionId;
	}

	public double getMinX() {
		return minX;
	}

	public double getMinY() {
		return minY;
	}

	public double getMaxX() {
		return maxX;
	}

	public double getMaxY() {
		return maxY;
	}

	@Override
	public String toString() {
		return areaName + " (" + areaType + ")";
	}
}

