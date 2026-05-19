package com.aircas.gimpro.service;

import com.aircas.gimpro.adapter.GpAdaptedSceneNode;
import com.aircas.gimpro.model.GpSceneInputSummary;
import com.aircas.gimpro.session.GpSceneSession;
import com.aircas.gimpro.session.GpSceneSessionService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * GIM Pro 三维场景清单服务。
 *
 * <p>当前版本负责把 OneModel 输出的工程与场景规划摘要重新整理为 GIM Pro 自己的会话清单。</p>
 */
public class GpSceneManifestService {

	private GpSceneSessionService sessionService;

	public String buildAndWriteManifest() {
		GpSceneSession session = sessionService().buildCurrentSession();
		Path output = writeManifest(session);
		return buildManifestReport(session, output);
	}

	public Path writeManifest(GpSceneSession session) {
		GpSceneInputSummary summary = session.getInputSummary();
		Path output = summary.getGimProRuntimeDir().resolve("scene-manifest.json");
		try {
			Files.createDirectories(output.getParent());
			Files.write(output, buildManifestJson(session).getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new IllegalStateException("写入 GIM Pro 场景清单失败：" + output, e);
		}
		return output;
	}

	public String buildManifestReport(GpSceneSession session, Path output) {
		GpSceneInputSummary summary = session.getInputSummary();
		StringBuilder builder = new StringBuilder();
		builder.append("GIM Pro 场景清单已生成\n\n");
		builder.append("输出文件：").append(output).append("\n");
		builder.append("会话 ID：").append(session.getSessionId()).append("\n");
		builder.append("工程名称：").append(summary.getProjectName()).append("\n");
		builder.append("场景节点数：").append(session.getNodeCount()).append("\n");
		builder.append("校验错误数：").append(session.getErrors().size()).append("\n");
		builder.append("校验警告数：").append(session.getWarnings().size()).append("\n");
		if (!session.getErrors().isEmpty()) {
			builder.append("\n存在错误，当前清单更适合作为排查材料，不建议直接进入完整三维流程。\n");
		}
		if (!session.getWarnings().isEmpty()) {
			builder.append("\n存在警告，建议先回到 OneModel 补齐工程或模型绑定信息。\n");
		}
		builder.append("\n说明：该清单是 GIM Pro 自研的三维接入编排文件，用于把 OneModel 的二维工程结果整理成 GIM Pro 可消费的统一输入。\n");
		return builder.toString();
	}

	private String buildManifestJson(GpSceneSession session) {
		GpSceneInputSummary summary = session.getInputSummary();
		StringBuilder builder = new StringBuilder();
		builder.append("{\n");
		builder.append("  \"plugin\": \"GIM Pro\",\n");
		builder.append("  \"sourcePlugin\": \"OneModel\",\n");
		builder.append("  \"sessionId\": \"").append(escape(session.getSessionId())).append("\",\n");
		builder.append("  \"generatedAt\": \"").append(escape(session.getGeneratedAt())).append("\",\n");
		builder.append("  \"projectId\": \"").append(escape(summary.getProjectId())).append("\",\n");
		builder.append("  \"projectName\": \"").append(escape(summary.getProjectName())).append("\",\n");
		builder.append("  \"stationName\": \"").append(escape(summary.getStationName())).append("\",\n");
		builder.append("  \"voltageLevel\": \"").append(escape(summary.getVoltageLevel())).append("\",\n");
		builder.append("  \"versionId\": \"").append(escape(summary.getVersionId())).append("\",\n");
		builder.append("  \"coordinateSystemCode\": \"").append(escape(summary.getCoordinateSystemCode())).append("\",\n");
		builder.append("  \"projectMapName\": \"").append(escape(summary.getProjectMapName())).append("\",\n");
		builder.append("  \"projectFolder\": \"").append(escape(path(summary.getProjectFolder()))).append("\",\n");
		builder.append("  \"workspaceFile\": \"").append(escape(path(summary.getWorkspaceFile()))).append("\",\n");
		builder.append("  \"oneModelRuntimeDir\": \"").append(escape(path(summary.getOneModelRuntimeDir()))).append("\",\n");
		builder.append("  \"oneModelScenePlanFile\": \"").append(escape(path(summary.getOneModelScenePlanFile()))).append("\",\n");
		builder.append("  \"modelLibraryPath\": \"").append(escape(summary.getModelLibraryPath())).append("\",\n");
		builder.append("  \"sceneNodeCount\": ").append(session.getNodeCount()).append(",\n");
		builder.append("  \"scenePlanExists\": ").append(summary.isScenePlanExists()).append(",\n");
		builder.append("  \"workspaceFileExists\": ").append(summary.isWorkspaceFileExists()).append(",\n");
		builder.append("  \"modelLibraryExists\": ").append(summary.isModelLibraryExists()).append(",\n");
		builder.append("  \"ready\": ").append(session.isReady()).append(",\n");
		builder.append("  \"errors\": [");
		appendArray(builder, session.getErrors());
		builder.append("],\n");
		builder.append("  \"warnings\": [");
		appendArray(builder, session.getWarnings());
		builder.append("],\n");
		builder.append("  \"nodes\": [\n");
		for (int i = 0; i < session.getNodes().size(); i++) {
			GpAdaptedSceneNode node = session.getNodes().get(i);
			builder.append("    {\n")
					.append("      \"sourceIndex\": ").append(node.getSourceIndex()).append(",\n")
					.append("      \"equipmentId\": \"").append(escape(node.getEquipmentId())).append("\",\n")
					.append("      \"equipmentName\": \"").append(escape(node.getEquipmentName())).append("\",\n")
					.append("      \"areaId\": \"").append(escape(node.getAreaId())).append("\",\n")
					.append("      \"modelId\": \"").append(escape(node.getModelId())).append("\",\n")
					.append("      \"modelName\": \"").append(escape(node.getModelName())).append("\",\n")
					.append("      \"modelPath\": \"").append(escape(node.getModelPath())).append("\",\n")
					.append("      \"equipmentType\": \"").append(escape(node.getEquipmentType())).append("\",\n")
					.append("      \"graphicType\": \"").append(escape(node.getGraphicType())).append("\",\n")
					.append("      \"modelAttributes\": \"").append(escape(node.getModelAttributes())).append("\",\n")
					.append("      \"x\": ").append(node.getX()).append(",\n")
					.append("      \"y\": ").append(node.getY()).append(",\n")
					.append("      \"z\": ").append(node.getZ()).append("\n")
					.append("    }");
			if (i < session.getNodes().size() - 1) {
				builder.append(",");
			}
			builder.append("\n");
		}
		builder.append("  ],\n");
		builder.append("  \"notes\": [");
		appendArray(builder, java.util.Arrays.asList(
				"GIM Pro 当前基础实现负责三维接入编排、输入校验和会话清单生成。",
				"GIM Pro 当前场景清单以 scene-session.json 会话快照为上游基础。",
				"三维浏览与交互、轻量化加载与动态调度等能力优先复用 iDesktopX 现有能力。",
				"当 OneModel 工程或模型绑定发生变化时，可重新生成本清单。"
		));
		builder.append("]\n");
		builder.append("}\n");
		return builder.toString();
	}

	private void appendArray(StringBuilder builder, java.util.List<String> values) {
		for (int i = 0; i < values.size(); i++) {
			if (i > 0) {
				builder.append(", ");
			}
			builder.append("\"").append(escape(values.get(i))).append("\"");
		}
	}

	private String path(Path path) {
		return path == null ? "" : path.toString();
	}

	private String escape(String value) {
		if (value == null) {
			return "";
		}
		return value
				.replace("\\", "\\\\")
				.replace("\"", "\\\"")
				.replace("\r", "\\r")
				.replace("\n", "\\n")
				.replace("\t", "\\t");
	}

	private GpSceneSessionService sessionService() {
		if (sessionService == null) {
			sessionService = new GpSceneSessionService();
		}
		return sessionService;
	}
}


