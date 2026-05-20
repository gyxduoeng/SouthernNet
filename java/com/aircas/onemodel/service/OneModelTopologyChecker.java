package com.aircas.onemodel.service;

import com.aircas.onemodel.model.OneModelAreaRecord;
import com.aircas.onemodel.model.OneModelConnectionRecord;
import com.aircas.onemodel.model.OneModelEquipmentRecord;

import java.util.ArrayList;
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
		List<OneModelAreaRecord> areas = repository.listAreas();
		List<OneModelEquipmentRecord> equipments = repository.listEquipments();
		List<OneModelConnectionRecord> connections = repository.listConnections();
		Map<String, OneModelEquipmentRecord> equipmentMap = buildEquipmentMap(equipments);
		Set<String> seenPairs = new HashSet<>();
		Set<String> connected = new HashSet<>();
		List<String> errors = new ArrayList<>();
		List<String> warnings = new ArrayList<>();

		for (OneModelConnectionRecord connection : connections) {
			String fromId = nullToEmpty(connection.getFromEquipmentId());
			String toId = nullToEmpty(connection.getToEquipmentId());
			if (fromId.isEmpty() || toId.isEmpty()) {
				warnings.add("连接尚未绑定两端设备：" + connectionLabel(connection));
				continue;
			}
			if (fromId.equals(toId)) {
				errors.add("存在自连接：" + connectionLabel(connection));
			}
			if (!equipmentMap.containsKey(fromId) || !equipmentMap.containsKey(toId)) {
				errors.add("连接引用了不存在的设备：" + connectionLabel(connection));
			} else {
				connected.add(fromId);
				connected.add(toId);
			}
			String pair = normalizePair(fromId, toId, connection.getConnectionType());
			if (!seenPairs.add(pair)) {
				errors.add("存在重复连接：" + connectionLabel(connection));
			}
		}
		for (OneModelEquipmentRecord equipment : equipments) {
			if (!connected.contains(equipment.getEquipmentId())) {
				warnings.add("设备未接入连接：" + equipmentLabel(equipment));
			}
		}

		StringBuilder builder = new StringBuilder();
		builder.append("二维电站地图拓扑检查\n\n");
		builder.append("一、数据概况\n");
		builder.append("区域数量：").append(areas.size()).append("\n");
		builder.append("设备点数量：").append(equipments.size()).append("\n");
		builder.append("连接线数量：").append(connections.size()).append("\n");
		builder.append("已接入设备数：").append(connected.size()).append("\n");
		builder.append("未接入设备数：").append(Math.max(0, equipments.size() - connected.size())).append("\n\n");

		builder.append("二、硬性问题\n");
		appendItems(builder, errors, "未发现硬性拓扑问题。", "[问题] ");
		builder.append("\n三、提示项\n");
		appendItems(builder, warnings, "暂无提示项。", "[提示] ");
		builder.append("\n四、建议操作\n");
		appendSuggestions(builder, areas, equipments, connections, errors, warnings);
		return builder.toString();
	}

	private Map<String, OneModelEquipmentRecord> buildEquipmentMap(List<OneModelEquipmentRecord> equipments) {
		Map<String, OneModelEquipmentRecord> result = new HashMap<>();
		for (OneModelEquipmentRecord equipment : equipments) {
			String equipmentId = nullToEmpty(equipment.getEquipmentId());
			if (!equipmentId.isEmpty()) {
				result.put(equipmentId, equipment);
			}
		}
		return result;
	}

	private void appendItems(StringBuilder builder, List<String> items, String emptyText, String prefix) {
		if (items.isEmpty()) {
			builder.append(emptyText).append("\n");
			return;
		}
		for (String item : items) {
			builder.append(prefix).append(item).append("\n");
		}
	}

	private void appendSuggestions(StringBuilder builder, List<OneModelAreaRecord> areas, List<OneModelEquipmentRecord> equipments,
			List<OneModelConnectionRecord> connections, List<String> errors, List<String> warnings) {
		if (areas.isEmpty()) {
			builder.append("- 先绘制或录入区域范围，明确二维电站地图的工作范围。\n");
		}
		if (equipments.isEmpty()) {
			builder.append("- 创建设备图层后，使用设备点的输入坐标或手动绘制补充站内设备。\n");
		}
		if (!equipments.isEmpty() && connections.isEmpty()) {
			builder.append("- 使用连接线的选择设备连线或手动绘制，补充设备之间的连接关系。\n");
		}
		if (!errors.isEmpty()) {
			builder.append("- 优先处理硬性问题，避免后续派生结构和三维站体生成引用错误。\n");
		}
		if (!warnings.isEmpty()) {
			builder.append("- 检查提示项，确认未接入设备和未绑定端点的连接线是否符合设计意图。\n");
		}
		if (!areas.isEmpty() && !equipments.isEmpty() && !connections.isEmpty() && errors.isEmpty()) {
			builder.append("- 当前二维底座已具备演示条件，可继续进行图元绑定、设备-模型绑定和派生结构生成。\n");
		}
	}

	private String connectionLabel(OneModelConnectionRecord connection) {
		String type = nullToEmpty(connection.getConnectionType());
		String fromId = nullToEmpty(connection.getFromEquipmentId());
		String toId = nullToEmpty(connection.getToEquipmentId());
		return nullToEmpty(connection.getConnectionId()) + " [" + firstNonBlank(type, "未分类") + "] "
				+ firstNonBlank(fromId, "未绑定") + " -> " + firstNonBlank(toId, "未绑定");
	}

	private String equipmentLabel(OneModelEquipmentRecord equipment) {
		String name = firstNonBlank(equipment.getEquipmentName(), equipment.getEquipmentId());
		String type = firstNonBlank(equipment.getEquipmentType(), "未分类");
		return name + " [" + type + "]";
	}

	private String normalizePair(String fromId, String toId, String type) {
		String relationType = firstNonBlank(type, "电气连接");
		return fromId.compareTo(toId) <= 0 ? fromId + "|" + toId + "|" + relationType : toId + "|" + fromId + "|" + relationType;
	}

	private String firstNonBlank(String value, String fallback) {
		String text = nullToEmpty(value);
		return text.isEmpty() ? fallback : text;
	}

	private String nullToEmpty(String value) {
		return value == null ? "" : value.trim();
	}
}
