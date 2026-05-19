package com.aircas.onemodel.service;

import com.aircas.onemodel.model.OneModelConnectionRecord;
import com.aircas.onemodel.model.OneModelEquipmentRecord;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 拓扑校正检查。
 */
public class OneModelTopologyChecker {

	private final OneModelMapRepository repository = new OneModelMapRepository();

	public String buildReport() {
		List<OneModelEquipmentRecord> equipments = repository.listEquipments();
		List<OneModelConnectionRecord> connections = repository.listConnections();
		StringBuilder builder = new StringBuilder();
		builder.append("拓扑校正结果\n\n");
		builder.append("设备数：").append(equipments.size()).append("\n");
		builder.append("关联关系数：").append(connections.size()).append("\n\n");
		if (equipments.isEmpty()) {
			builder.append("当前没有设备点，无法进行拓扑校正。\n");
			return builder.toString();
		}
		Map<String, OneModelEquipmentRecord> equipmentMap = new HashMap<>();
		for (OneModelEquipmentRecord equipment : equipments) {
			equipmentMap.put(equipment.getEquipmentId(), equipment);
		}
		Set<String> seenPairs = new HashSet<>();
		Set<String> connected = new HashSet<>();
		int issueCount = 0;
		for (OneModelConnectionRecord connection : connections) {
			String fromId = connection.getFromEquipmentId();
			String toId = connection.getToEquipmentId();
			if (fromId.equals(toId)) {
				builder.append("[问题] 存在自连接：").append(connection.getConnectionId()).append("\n");
				issueCount++;
			}
			if (!equipmentMap.containsKey(fromId) || !equipmentMap.containsKey(toId)) {
				builder.append("[问题] 连接引用了不存在的设备：").append(connection.getConnectionId()).append("\n");
				issueCount++;
			}
			String pair = normalizePair(fromId, toId, connection.getConnectionType());
			if (!seenPairs.add(pair)) {
				builder.append("[问题] 存在重复连接：").append(connection.getConnectionId()).append("\n");
				issueCount++;
			}
			connected.add(fromId);
			connected.add(toId);
		}
		for (OneModelEquipmentRecord equipment : equipments) {
			if (!connected.contains(equipment.getEquipmentId())) {
				builder.append("[提示] 设备未接入关系：").append(equipment.getEquipmentName()).append("\n");
			}
		}
		if (issueCount == 0) {
			builder.append("未发现硬性拓扑错误。\n");
		}
		builder.append("\n下一步建议：完成图元绑定与设备-模型绑定，再生成三维站体。\n");
		return builder.toString();
	}

	private String normalizePair(String fromId, String toId, String type) {
		return fromId.compareTo(toId) <= 0 ? fromId + "|" + toId + "|" + type : toId + "|" + fromId + "|" + type;
	}
}

