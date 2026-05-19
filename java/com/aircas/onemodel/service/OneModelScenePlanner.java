package com.aircas.onemodel.service;

import com.aircas.onemodel.model.OneModelEquipmentRecord;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 三维生成输入规划。
 */
public class OneModelScenePlanner {

	private final OneModelMapRepository repository = new OneModelMapRepository();
	private final OneModelWorkspaceBridge workspaceBridge = new OneModelWorkspaceBridge();

	public String generatePlan() {
		List<OneModelEquipmentRecord> equipments = repository.listEquipments();
		List<OneModelEquipmentRecord> boundEquipments = new ArrayList<>();
		List<String> missing = new ArrayList<>();
		for (OneModelEquipmentRecord equipment : equipments) {
			if (equipment.hasModelBinding()) {
				boundEquipments.add(equipment);
			} else {
				missing.add(equipment.getEquipmentName());
			}
		}
		Path output = workspaceBridge.resolveWorkspaceRuntimeDir().resolve("scene-plan.json");
		try {
			Files.createDirectories(output.getParent());
			Files.write(output, buildJson(boundEquipments).getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new IllegalStateException("写入三维生成计划失败：" + output, e);
		}
		StringBuilder builder = new StringBuilder();
		builder.append("三维生成计划已输出\n\n");
		builder.append("计划文件：").append(output).append("\n");
		builder.append("可参与生成的设备数：").append(boundEquipments.size()).append("\n");
		builder.append("未完成模型绑定的设备数：").append(missing.size()).append("\n");
		if (!missing.isEmpty()) {
			builder.append("\n以下设备尚未绑模，将被跳过：\n");
			for (String name : missing) {
				builder.append("- ").append(name).append("\n");
			}
		}
		return builder.toString();
	}

	private String buildJson(List<OneModelEquipmentRecord> equipments) {
		StringBuilder builder = new StringBuilder();
		builder.append("{\n  \"sceneNodes\": [\n");
		for (int i = 0; i < equipments.size(); i++) {
			OneModelEquipmentRecord equipment = equipments.get(i);
			builder.append("    {\n")
					.append("      \"equipmentId\": \"").append(escape(equipment.getEquipmentId())).append("\",\n")
					.append("      \"equipmentName\": \"").append(escape(equipment.getEquipmentName())).append("\",\n")
					.append("      \"areaId\": \"").append(escape(equipment.getAreaId())).append("\",\n")
					.append("      \"modelId\": \"").append(escape(equipment.getModelId())).append("\",\n")
					.append("      \"modelName\": \"").append(escape(equipment.getModelName())).append("\",\n")
					.append("      \"modelPath\": \"").append(escape(equipment.getModelPath())).append("\",\n")
					.append("      \"x\": ").append(equipment.getX()).append(",\n")
					.append("      \"y\": ").append(equipment.getY()).append(",\n")
					.append("      \"z\": 0.0\n")
					.append("    }");
			if (i < equipments.size() - 1) {
				builder.append(",");
			}
			builder.append("\n");
		}
		builder.append("  ]\n}\n");
		return builder.toString();
	}

	private String escape(String value) {
		return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
	}
}

