package com.aircas.gimpro.service;

import com.aircas.gimpro.adapter.GpAdaptedSceneNode;
import com.aircas.onemodel.model.OneModelConnectionRecord;
import com.aircas.onemodel.model.OneModelEquipmentRecord;
import com.aircas.onemodel.service.OneModelMapRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 从共享数据源读取 GIM Pro 三维场景主链输入。
 */
public class GpSharedDatasourceSceneReader {

	private final OneModelMapRepository repository = new OneModelMapRepository();

	public ReadResult read() {
		List<GpAdaptedSceneNode> nodes = new ArrayList<>();
		List<String> warnings = new ArrayList<>();
		List<String> errors = new ArrayList<>();
		try {
			for (OneModelEquipmentRecord equipment : repository.listEquipments()) {
				nodes.add(new GpAdaptedSceneNode(
						nodes.size(),
						equipment.getEquipmentId(),
						equipment.getEquipmentName(),
						equipment.getAreaId(),
						equipment.getModelId(),
						equipment.getModelName(),
						equipment.getModelPath(),
						equipment.getEquipmentType(),
						equipment.getGraphicType(),
						equipment.getModelAttributes(),
						equipment.getX(),
						equipment.getY(),
						0D));
			}
		} catch (Exception ex) {
			errors.add("读取共享数据源中的设备成果失败：" + safe(ex.getMessage(), ex.getClass().getSimpleName()));
		}
		if (nodes.isEmpty() && errors.isEmpty()) {
			warnings.add("共享数据源中未读取到可用于三维场景生成的设备成果。" );
		}
		int connectionCount = 0;
		try {
			List<OneModelConnectionRecord> connections = repository.listConnections();
			connectionCount = connections == null ? 0 : connections.size();
		} catch (Exception ex) {
			warnings.add("读取共享关联关系成果失败：" + safe(ex.getMessage(), ex.getClass().getSimpleName()));
		}
		return new ReadResult(nodes, connectionCount, warnings, errors);
	}

	private String safe(String value, String fallback) {
		if (value != null && !value.trim().isEmpty()) {
			return value.trim();
		}
		return fallback == null ? "" : fallback.trim();
	}

	public static final class ReadResult {
		private final List<GpAdaptedSceneNode> nodes;
		private final int connectionCount;
		private final List<String> warnings;
		private final List<String> errors;

		private ReadResult(List<GpAdaptedSceneNode> nodes, int connectionCount,
				List<String> warnings, List<String> errors) {
			this.nodes = Collections.unmodifiableList(new ArrayList<>(nodes));
			this.connectionCount = Math.max(connectionCount, 0);
			this.warnings = Collections.unmodifiableList(new ArrayList<>(warnings));
			this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
		}

		public List<GpAdaptedSceneNode> getNodes() {
			return nodes;
		}

		public int getConnectionCount() {
			return connectionCount;
		}

		public List<String> getWarnings() {
			return warnings;
		}

		public List<String> getErrors() {
			return errors;
		}
	}
}

