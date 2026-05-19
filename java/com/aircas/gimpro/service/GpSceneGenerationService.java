package com.aircas.gimpro.service;

import com.aircas.gimpro.adapter.GpAdaptedSceneNode;
import com.aircas.gimpro.model.GpLightweightSessionConfig;
import com.aircas.gimpro.model.GpModelLibraryEntry;
import com.aircas.gimpro.model.GpSceneGenerationResult;
import com.aircas.gimpro.session.GpSceneSession;
import com.aircas.gimpro.session.GpSceneSessionService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * GIM Pro 三维场景生成服务。
 */
public class GpSceneGenerationService {

	private GpSceneSessionService sessionService;
	private final GpModelLibraryResolver modelLibraryResolver = new GpModelLibraryResolver();
	private final GpSceneManifestService manifestService = new GpSceneManifestService();
	private final GpLightweightCapabilityService lightweightCapabilityService = new GpLightweightCapabilityService();

	public GpSceneGenerationResult generateCurrentScene() {
		return generateCurrentScene(sessionService().buildCurrentSession());
	}

	public GpSceneGenerationResult generateCurrentScene(GpSceneSession session) {
		List<String> warnings = new ArrayList<>(session.getWarnings());
		List<String> errors = new ArrayList<>(session.getErrors());
		List<GpAdaptedSceneNode> nodes = new ArrayList<>(session.getNodes());
		List<GpModelLibraryEntry> modelEntries = modelLibraryResolver.resolve(nodes,
				session.getInputSummary().getModelLibraryPath());
		int unresolved = 0;
		for (GpModelLibraryEntry entry : modelEntries) {
			if ("未定位到模型文件".equals(entry.getStatus())) {
				unresolved++;
			}
		}
		if (unresolved > 0) {
			warnings.add("存在 " + unresolved + " 个场景节点未能在模型库中定位到有效模型文件。" );
		}
		if (nodes.isEmpty() && errors.isEmpty()) {
			errors.add("未生成任何三维场景节点，无法继续进行三维场景生成。" );
		}
		return new GpSceneGenerationResult(session, nodes, modelEntries, warnings, errors);
	}

	public String generateAndWriteSceneReport() {
		GpSceneSession session = sessionService().buildCurrentSession();
		return generateAndWriteSceneReport(session);
	}

	public String generateAndWriteSceneReport(GpSceneSession session) {
		Path sessionOutput = sessionService().writeSession(session);
		GpSceneGenerationResult result = generateCurrentScene(session);
		Path output = result.getSession().getInputSummary().getGimProRuntimeDir().resolve("scene-generation.json");
		Path manifestOutput = manifestService.writeManifest(result.getSession());
		GpLightweightSessionConfig lightweightConfig = lightweightCapabilityService.buildConfig(result.getSession());
		Path lightweightOutput = lightweightCapabilityService.writeConfig(lightweightConfig);
		try {
			Files.createDirectories(output.getParent());
			Files.write(output, buildSceneJson(result, sessionOutput, manifestOutput, lightweightOutput).getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new IllegalStateException("写入 GIM Pro 三维场景结果失败：" + output, e);
		}
		return buildHumanReadableReport(result, output, sessionOutput, manifestOutput, lightweightOutput);
	}

	public String buildHumanReadableReport(GpSceneGenerationResult result, Path output) {
		return buildHumanReadableReport(result, output, null, null, null);
	}

	public String buildHumanReadableReport(GpSceneGenerationResult result, Path output,
			Path manifestOutput, Path lightweightOutput) {
		return buildHumanReadableReport(result, output, null, manifestOutput, lightweightOutput);
	}

	public String buildHumanReadableReport(GpSceneGenerationResult result, Path output,
			Path sessionOutput, Path manifestOutput, Path lightweightOutput) {
		StringBuilder builder = new StringBuilder();
		builder.append("GIM Pro 三维场景生成结果\n\n");
		builder.append(sessionService().buildSessionReport(result.getSession())).append("\n");
		builder.append("输出文件：").append(output).append("\n");
		if (sessionOutput != null) {
			builder.append("场景会话：").append(sessionOutput).append("\n");
		}
		if (manifestOutput != null) {
			builder.append("场景清单：").append(manifestOutput).append("\n");
		}
		if (lightweightOutput != null) {
			builder.append("轻量化配置：").append(lightweightOutput).append("\n");
		}
		builder.append("生成节点数：").append(result.getNodes().size()).append("\n");
		builder.append("模型条目数：").append(result.getModelEntries().size()).append("\n");
		builder.append("可继续预览：").append(result.isSuccess() ? "是" : "否").append("\n");
		if (!result.getModelEntries().isEmpty()) {
			builder.append("\n模型库调整 / 属性查看预览：\n");
			int preview = Math.min(result.getModelEntries().size(), 8);
			for (int i = 0; i < preview; i++) {
				GpModelLibraryEntry entry = result.getModelEntries().get(i);
				builder.append(i + 1).append(". ").append(entry.getDisplayName())
						.append(" | ").append(entry.getStatus())
						.append(entry.getResolvedModelPath().isEmpty() ? "" : " | " + entry.getResolvedModelPath())
						.append(entry.getPropertySummary().isEmpty() ? "" : " | " + entry.getPropertySummary())
						.append("\n");
			}
			if (result.getModelEntries().size() > preview) {
				builder.append("... 共 ").append(result.getModelEntries().size()).append(" 条模型条目\n");
			}
		}
		if (!result.getErrors().isEmpty()) {
			builder.append("\n错误项：\n");
			for (String error : result.getErrors()) {
				builder.append("- ").append(error).append("\n");
			}
		}
		if (!result.getWarnings().isEmpty()) {
			builder.append("\n警告项：\n");
			for (String warning : result.getWarnings()) {
				builder.append("- ").append(warning).append("\n");
			}
		}
		builder.append("\n后续建议：\n");
		builder.append("1. 若存在缺模条目，优先补齐模型库或模型路径配置。\n");
		builder.append("2. 使用场景清单与轻量化配置作为后续三维预览 / 平台加载输入。\n");
		builder.append("3. 生成完成后可继续执行碰撞检测，输出问题列表用于布局复核。\n");
		builder.append("\n说明：当前三维场景生成主链优先直读共享数据源；模型库调整与属性查看已并入本流程。\n");
		return builder.toString();
	}

	private String buildSceneJson(GpSceneGenerationResult result, Path sessionOutput,
			Path manifestOutput, Path lightweightOutput) {
		StringBuilder builder = new StringBuilder();
		builder.append("{\n");
		builder.append("  \"plugin\": \"GIM Pro\",\n");
		builder.append("  \"sessionId\": \"").append(escape(result.getSession().getSessionId())).append("\",\n");
		builder.append("  \"sourceMode\": \"").append(escape(result.getSession().getSourceMode())).append("\",\n");
		builder.append("  \"sessionFile\": \"").append(escape(path(sessionOutput))).append("\",\n");
		builder.append("  \"manifestFile\": \"").append(escape(path(manifestOutput))).append("\",\n");
		builder.append("  \"lightweightConfigFile\": \"").append(escape(path(lightweightOutput))).append("\",\n");
		builder.append("  \"nodeCount\": ").append(result.getNodes().size()).append(",\n");
		builder.append("  \"connectionCount\": ").append(result.getSession().getConnectionCount()).append(",\n");
		builder.append("  \"boundNodeCount\": ").append(result.getSession().getBoundNodeCount()).append(",\n");
		builder.append("  \"errors\": [");
		appendStringArray(builder, result.getErrors());
		builder.append("],\n");
		builder.append("  \"warnings\": [");
		appendStringArray(builder, result.getWarnings());
		builder.append("],\n");
		builder.append("  \"modelEntries\": [\n");
		for (int i = 0; i < result.getModelEntries().size(); i++) {
			GpModelLibraryEntry entry = result.getModelEntries().get(i);
			builder.append("    {\n")
					.append("      \"equipmentId\": \"").append(escape(entry.getEquipmentId())).append("\",\n")
					.append("      \"displayName\": \"").append(escape(entry.getDisplayName())).append("\",\n")
					.append("      \"configuredModelPath\": \"").append(escape(entry.getConfiguredModelPath())).append("\",\n")
					.append("      \"resolvedModelPath\": \"").append(escape(entry.getResolvedModelPath())).append("\",\n")
					.append("      \"status\": \"").append(escape(entry.getStatus())).append("\",\n")
					.append("      \"propertySummary\": \"").append(escape(entry.getPropertySummary())).append("\"\n")
					.append("    }");
			if (i < result.getModelEntries().size() - 1) {
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

	private String path(Path path) {
		return path == null ? "" : path.toString();
	}

	private GpSceneSessionService sessionService() {
		if (sessionService == null) {
			sessionService = new GpSceneSessionService();
		}
		return sessionService;
	}
}

