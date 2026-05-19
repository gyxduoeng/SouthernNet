package com.aircas.onemodel.service;

import com.aircas.onemodel.model.OneModelAreaRecord;
import com.aircas.onemodel.model.OneModelConnectionRecord;
import com.aircas.onemodel.model.OneModelEquipmentRecord;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.List;

/**
 * 对象树与关系结构派生。
 */
public class OneModelStructureBuilder {

	private final OneModelMapRepository repository = new OneModelMapRepository();

	public DefaultMutableTreeNode buildTree() {
		DefaultMutableTreeNode root = new DefaultMutableTreeNode("OneModel 派生结构");
		List<OneModelAreaRecord> areas = repository.listAreas();
		List<OneModelEquipmentRecord> equipments = repository.listEquipments();
		List<OneModelConnectionRecord> connections = repository.listConnections();
		for (OneModelAreaRecord area : areas) {
			DefaultMutableTreeNode areaNode = new DefaultMutableTreeNode(area.getAreaName() + " [" + area.getAreaType() + "]");
			for (OneModelEquipmentRecord equipment : equipments) {
				if (area.getAreaId().equals(equipment.getAreaId())) {
					DefaultMutableTreeNode equipmentNode = new DefaultMutableTreeNode(equipment.getEquipmentName() + formatBindingSuffix(equipment));
					for (OneModelConnectionRecord connection : connections) {
						if (equipment.getEquipmentId().equals(connection.getFromEquipmentId()) || equipment.getEquipmentId().equals(connection.getToEquipmentId())) {
							equipmentNode.add(new DefaultMutableTreeNode(connection.getConnectionId() + " / " + connection.getConnectionType()));
						}
					}
					areaNode.add(equipmentNode);
				}
			}
			root.add(areaNode);
		}
		return root;
	}

	public String buildSummary() {
		List<OneModelAreaRecord> areas = repository.listAreas();
		List<OneModelEquipmentRecord> equipments = repository.listEquipments();
		List<OneModelConnectionRecord> connections = repository.listConnections();
		int boundModels = 0;
		for (OneModelEquipmentRecord equipment : equipments) {
			if (equipment.hasModelBinding()) {
				boundModels++;
			}
		}
		return "派生结构摘要\n\n"
				+ "区域数：" + areas.size() + "\n"
				+ "设备数：" + equipments.size() + "\n"
				+ "关联关系数：" + connections.size() + "\n"
				+ "已绑定模型设备数：" + boundModels + "\n";
	}

	private String formatBindingSuffix(OneModelEquipmentRecord equipment) {
		return equipment.hasModelBinding() ? " [已绑模]" : " [未绑模]";
	}
}

