package com.aircas.gimpro.session;

import com.aircas.gimpro.adapter.GpAdaptedSceneNode;
import com.aircas.gimpro.adapter.GpOneModelScenePlanAdapter;
import com.aircas.gimpro.model.GpSceneInputSummary;
import com.aircas.gimpro.service.GpSceneInputService;
import com.aircas.gimpro.service.GpSharedDatasourceSceneReader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * GIM Pro 三维工程会话服务。
 */
public class GpSceneSessionService {

	private static final DateTimeFormatter SESSION_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final String SOURCE_SHARED_DATASOURCE = "shared-datasource";
	private static final String SOURCE_SCENE_PLAN = "scene-plan";

	private GpSceneInputService inputService;
	private GpOneModelScenePlanAdapter adapter;
	private GpSharedDatasourceSceneReader sharedDatasourceSceneReader;

	public GpSceneSession buildCurrentSession() {
		GpSceneInputSummary summary = inputService().loadCurrentSummary();
		List<String> errors = new ArrayList<>();
		List<String> warnings = new ArrayList<>();
		List<GpAdaptedSceneNode> nodes = new ArrayList<>();
		int connectionCount = 0;
		String sourceMode = SOURCE_SHARED_DATASOURCE;
		if (isBlank(summary.getProjectId())) {
			errors.add("当前未识别到有效的 OneModel 工程，请先在 OneModel 中打开工程。");
		}
		if (summary.getProjectFolder() == null || !Files.exists(summary.getProjectFolder())) {
			errors.add("工程目录不存在，无法建立 GIM Pro 三维工程会话。");
		}
		if (!summary.isWorkspaceFileExists()) {
			errors.add("工程工作空间文件不存在，建议先在 OneModel 中打开并保存工程地图。");
		}
		if (!summary.isSharedDatasourceExists()) {
			warnings.add("未找到共享工程数据源，将尝试回退到 scene-plan.json 兼容输入。" );
		}
		if (!summary.isModelLibraryExists()) {
			warnings.add("模型库目录不存在或未配置，GIM Pro 仍可建立会话，但后续三维模型加载可能失败。");
		}
		if (isBlank(summary.getCoordinateSystemCode())) {
			warnings.add("当前工程坐标系为空，建议在 OneModel 工程参数中显式配置 EPSG 代码。");
		}
		if (isBlank(summary.getProjectMapName())) {
			warnings.add("当前工程地图名为空，建议在 OneModel 工程参数中补齐工程地图名称。");
		}
		if (summary.isSharedDatasourceExists()) {
			GpSharedDatasourceSceneReader.ReadResult readResult = sharedDatasourceSceneReader().read();
			nodes.addAll(readResult.getNodes());
			connectionCount = readResult.getConnectionCount();
			errors.addAll(readResult.getErrors());
			warnings.addAll(readResult.getWarnings());
		}
		if (nodes.isEmpty()) {
			sourceMode = SOURCE_SCENE_PLAN;
			GpOneModelScenePlanAdapter.AdaptResult adaptResult = adapter().adapt(summary.getOneModelScenePlanFile());
			nodes.addAll(adaptResult.getNodes());
			errors.addAll(adaptResult.getErrors());
			warnings.addAll(adaptResult.getWarnings());
			if (summary.isScenePlanExists() && adaptResult.getNodes().isEmpty()) {
				warnings.add("scene-plan.json 已存在，但未解析到可用的场景节点。");
			}
			if (!summary.isScenePlanExists()) {
				errors.add("未找到 scene-plan.json 兼容输入，且共享工程数据源未能生成有效场景节点。" );
			}
		}
		Path sessionFile = summary.getGimProRuntimeDir().resolve("scene-session.json");
		String sessionId = buildSessionId(summary);
		String generatedAt = LocalDateTime.now().format(SESSION_TIME_FORMAT);
		return new GpSceneSession(sessionId, generatedAt, sourceMode, summary, sessionFile, summary.getOneModelScenePlanFile(),
				nodes, errors, warnings, connectionCount);
	}

	public String buildAndWriteSession() {
		GpSceneSession session = buildCurrentSession();
		writeSession(session);
		return buildSessionReport(session);
	}

	public Path writeSession(GpSceneSession session) {
		Path output = session.getSessionFile();
		try {
			Files.createDirectories(output.getParent());
			Files.write(output, buildSessionJson(session).getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new IllegalStateException("写入 GIM Pro 场景会话失败：" + output, e);
		}
		return output;
	}

	public String buildSessionReport(GpSceneSession session) {
		StringBuilder builder = new StringBuilder();
		builder.append("GIM Pro 三维工程会话\n\n");
		builder.append(buildHumanReadableSummary(session.getInputSummary())).append("\n");
		builder.append("会话 ID：").append(session.getSessionId()).append("\n");
		builder.append("生成时间：").append(session.getGeneratedAt()).append("\n");
		builder.append("场景来源：").append(session.getSourceMode()).append("\n");
		builder.append("会话文件：").append(session.getSessionFile()).append("\n");
		builder.append("来源规划文件：").append(session.getSourceScenePlanFile()).append("\n");
		builder.append("解析节点数：").append(session.getNodeCount()).append("\n");
		builder.append("关联关系数：").append(session.getConnectionCount()).append("\n");
		builder.append("已绑模节点数：").append(session.getBoundNodeCount()).append("\n");
		builder.append("会话状态：").append(session.isReady() ? "可继续进入后续三维流程" : "存在阻塞问题").append("\n");
		if (!session.getErrors().isEmpty()) {
			builder.append("\n错误项：\n");
			for (String error : session.getErrors()) {
				builder.append("- ").append(error).append("\n");
			}
		}
		if (!session.getWarnings().isEmpty()) {
			builder.append("\n警告项：\n");
			for (String warning : session.getWarnings()) {
				builder.append("- ").append(warning).append("\n");
			}
		}
		if (!session.getNodes().isEmpty()) {
			builder.append("\n节点预览：\n");
			int previewLimit = Math.min(session.getNodes().size(), 8);
			for (int i = 0; i < previewLimit; i++) {
				GpAdaptedSceneNode node = session.getNodes().get(i);
				builder.append(i + 1).append(". ").append(node.getDisplayLabel())
						.append(" -> ").append(node.getModelName())
						.append(" @ (").append(node.getX()).append(", ").append(node.getY()).append(", ").append(node.getZ()).append(")\n");
			}
			if (session.getNodes().size() > previewLimit) {
				builder.append("... 共 ").append(session.getNodes().size()).append(" 个节点\n");
			}
		}
		builder.append("\n说明：该会话是 GIM Pro 自研的三维接入快照，用于固化当前 OneModel 输入状态、适配后的场景节点和诊断结果。\n");
		return builder.toString();
	}

	private String buildSessionJson(GpSceneSession session) {
		StringBuilder builder = new StringBuilder();
		builder.append("{\n");
		builder.append("  \"plugin\": \"GIM Pro\",\n");
		builder.append("  \"sessionId\": \"").append(escape(session.getSessionId())).append("\",\n");
		builder.append("  \"generatedAt\": \"").append(escape(session.getGeneratedAt())).append("\",\n");
		builder.append("  \"projectId\": \"").append(escape(session.getInputSummary().getProjectId())).append("\",\n");
		builder.append("  \"sourceMode\": \"").append(escape(session.getSourceMode())).append("\",\n");
		builder.append("  \"projectName\": \"").append(escape(session.getInputSummary().getProjectName())).append("\",\n");
		builder.append("  \"sourceScenePlanFile\": \"").append(escape(path(session.getSourceScenePlanFile()))).append("\",\n");
		builder.append("  \"nodeCount\": ").append(session.getNodeCount()).append(",\n");
		builder.append("  \"connectionCount\": ").append(session.getConnectionCount()).append(",\n");
		builder.append("  \"boundNodeCount\": ").append(session.getBoundNodeCount()).append(",\n");
		builder.append("  \"ready\": ").append(session.isReady()).append(",\n");
		builder.append("  \"errors\": [");
		appendStringArray(builder, session.getErrors());
		builder.append("],\n");
		builder.append("  \"warnings\": [");
		appendStringArray(builder, session.getWarnings());
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
		builder.append("  ]\n");
		builder.append("}\n");
		return builder.toString();
	}

	private void appendStringArray(StringBuilder builder, List<String> values) {
		for (int i = 0; i < values.size(); i++) {
			if (i > 0) {
				builder.append(", ");
			}
			builder.append("\"").append(escape(values.get(i))).append("\"");
		}
	}

	private String buildSessionId(GpSceneInputSummary summary) {
		String projectId = summary == null ? "" : summary.getProjectId();
		String seed = isBlank(projectId) ? "NO_PROJECT" : projectId;
		return "GP-SESSION-" + seed + "-" + System.currentTimeMillis();
	}

	private String buildHumanReadableSummary(GpSceneInputSummary summary) {
		StringBuilder builder = new StringBuilder();
		builder.append("GIM Pro 三维接入输入摘要\n\n");
		builder.append("工程 ID：").append(summary.getProjectId()).append("\n");
		builder.append("工程名称：").append(summary.getProjectName()).append("\n");
		builder.append("电站名称：").append(summary.getStationName()).append("\n");
		builder.append("电压等级：").append(summary.getVoltageLevel()).append("\n");
		builder.append("版本号：").append(summary.getVersionId()).append("\n");
		builder.append("坐标系：EPSG:").append(summary.getCoordinateSystemCode()).append("\n");
		builder.append("工程地图：").append(summary.getProjectMapName()).append("\n");
		builder.append("工程目录：").append(path(summary.getProjectFolder())).append("\n");
		builder.append("工作空间：").append(path(summary.getWorkspaceFile())).append("\n");
		builder.append("OneModel 运行目录：").append(path(summary.getOneModelRuntimeDir())).append("\n");
		builder.append("共享数据源：").append(path(summary.getSharedDatasourcePath())).append("\n");
		builder.append("OneModel 场景规划：").append(path(summary.getOneModelScenePlanFile())).append("\n");
		builder.append("GIM Pro 运行目录：").append(path(summary.getGimProRuntimeDir())).append("\n");
		builder.append("模型库目录：").append(summary.getModelLibraryPath()).append("\n");
		builder.append("工作空间文件存在：").append(summary.isWorkspaceFileExists() ? "是" : "否").append("\n");
		builder.append("共享数据源存在：").append(summary.isSharedDatasourceExists() ? "是" : "否").append("\n");
		builder.append("场景规划文件存在：").append(summary.isScenePlanExists() ? "是" : "否").append("\n");
		builder.append("模型库目录存在：").append(summary.isModelLibraryExists() ? "是" : "否").append("\n");
		builder.append("场景节点数：").append(summary.getSceneNodeCount()).append("\n");
		builder.append("场景规划文件大小：").append(summary.getScenePlanSize()).append(" 字节\n");
		builder.append("\n说明：GIM Pro 当前基础实现优先以共享数据源为三维场景主链输入；`scene-plan.json` 仅保留为兼容输入。\n");
		return builder.toString();
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

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	private GpSceneInputService inputService() {
		if (inputService == null) {
			inputService = new GpSceneInputService();
		}
		return inputService;
	}

	private GpOneModelScenePlanAdapter adapter() {
		if (adapter == null) {
			adapter = new GpOneModelScenePlanAdapter();
		}
		return adapter;
	}

	private GpSharedDatasourceSceneReader sharedDatasourceSceneReader() {
		if (sharedDatasourceSceneReader == null) {
			sharedDatasourceSceneReader = new GpSharedDatasourceSceneReader();
		}
		return sharedDatasourceSceneReader;
	}
}

