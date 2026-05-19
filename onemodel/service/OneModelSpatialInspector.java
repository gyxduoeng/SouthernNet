package com.aircas.onemodel.service;

import com.aircas.onemodel.model.OneModelAreaRecord;
import com.aircas.onemodel.model.OneModelEquipmentRecord;

import java.util.List;

/**
 * 三维问题检查，当前只做 A 级提示。
 */
public class OneModelSpatialInspector {

	private final OneModelMapRepository repository = new OneModelMapRepository();

	public String inspect() {
		List<OneModelAreaRecord> areas = repository.listAreas();
		List<OneModelEquipmentRecord> equipments = repository.listEquipments();
		StringBuilder builder = new StringBuilder();
		builder.append("空间范围 / 碰撞检查\n\n");
		int issueCount = 0;
		for (OneModelEquipmentRecord equipment : equipments) {
			if (!equipment.hasModelBinding()) {
				builder.append("[提示] 设备未完成模型绑定，暂无法进行完整空间检查：")
						.append(equipment.getEquipmentName()).append("\n");
				issueCount++;
			}
			OneModelAreaRecord area = findArea(areas, equipment.getAreaId());
			if (area != null && !contains(area, equipment)) {
				builder.append("[提示] 设备点落在所属区域之外：")
						.append(equipment.getEquipmentName()).append(" / ").append(area.getAreaName()).append("\n");
				issueCount++;
			}
		}
		for (int i = 0; i < equipments.size(); i++) {
			for (int j = i + 1; j < equipments.size(); j++) {
				OneModelEquipmentRecord left = equipments.get(i);
				OneModelEquipmentRecord right = equipments.get(j);
				double distance = Math.hypot(left.getX() - right.getX(), left.getY() - right.getY());
				if (distance < 6.0D) {
					builder.append("[提示] 设备疑似过近或碰撞：")
							.append(left.getEquipmentName()).append(" <-> ").append(right.getEquipmentName())
							.append("，距离=").append(String.format("%.2f", distance)).append("\n");
					issueCount++;
				}
			}
		}
		if (issueCount == 0) {
			builder.append("未发现明显的空间范围或碰撞问题。\n");
		}
		builder.append("\n当前策略：仅输出问题提示，由人工回到二维地图修图，不自动回写。\n");
		return builder.toString();
	}

	private OneModelAreaRecord findArea(List<OneModelAreaRecord> areas, String areaId) {
		for (OneModelAreaRecord area : areas) {
			if (area.getAreaId().equals(areaId)) {
				return area;
			}
		}
		return null;
	}

	private boolean contains(OneModelAreaRecord area, OneModelEquipmentRecord equipment) {
		return equipment.getX() >= area.getMinX() && equipment.getX() <= area.getMaxX()
				&& equipment.getY() >= area.getMinY() && equipment.getY() <= area.getMaxY();
	}
}

