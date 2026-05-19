package com.aircas.gimpro.service;

import com.aircas.gimpro.adapter.GpAdaptedSceneNode;
import com.aircas.gimpro.model.GpCollisionProblem;
import com.aircas.gimpro.model.GpSceneGenerationResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GIM Pro 三维碰撞检测服务。
 */
public class GpCollisionDetectionService {

	private static final Pattern NUMBER_PATTERN = Pattern.compile("(-?\\d+(?:\\.\\d+)?)");

	private GpSceneGenerationService sceneGenerationService;

	public List<GpCollisionProblem> detectCurrentSceneProblems() {
		return detectCurrentSceneProblems(sceneGenerationService().generateCurrentScene());
	}

	public List<GpCollisionProblem> detectCurrentSceneProblems(GpSceneGenerationResult result) {
		List<GpCollisionProblem> problems = new ArrayList<>();
		List<GpAdaptedSceneNode> nodes = result.getNodes();
		for (int i = 0; i < nodes.size(); i++) {
			GpAdaptedSceneNode a = nodes.get(i);
			double aSize = resolveCollisionSize(a);
			for (int j = i + 1; j < nodes.size(); j++) {
				GpAdaptedSceneNode b = nodes.get(j);
				double bSize = resolveCollisionSize(b);
				if (overlaps(a.getX(), a.getY(), aSize, b.getX(), b.getY(), bSize)) {
					String problemId = "COL-" + (problems.size() + 1);
					String severity = Math.abs(aSize - bSize) < 1.5D ? "高" : "中";
					String location = "(" + average(a.getX(), b.getX()) + ", " + average(a.getY(), b.getY()) + ")";
					String note = "依据设备位置与基础空间占用估算发生碰撞，请复核布局。";
					problems.add(new GpCollisionProblem(problemId, "设备碰撞", severity,
							a.getDisplayLabel(), b.getDisplayLabel(), location, note));
				}
			}
		}
		return problems;
	}

	public String buildProblemListReport() {
		GpSceneGenerationResult result = sceneGenerationService().generateCurrentScene();
		return buildProblemListReport(result);
	}

	public String buildProblemListReport(GpSceneGenerationResult result) {
		List<GpCollisionProblem> problems = detectCurrentSceneProblems(result);
		Path output = writeCollisionReport(result, problems);
		StringBuilder builder = new StringBuilder();
		builder.append("GIM Pro 三维碰撞检测结果\n\n");
		builder.append("场景会话：").append(result.getSession().getSessionId()).append("\n");
		builder.append("场景来源：").append(result.getSession().getSourceMode()).append("\n");
		builder.append("检测节点数：").append(result.getNodes().size()).append("\n");
		builder.append("输出文件：").append(output).append("\n");
		if (!result.getErrors().isEmpty()) {
			builder.append("上游阻塞错误数：").append(result.getErrors().size()).append("\n");
		}
		if (!result.getWarnings().isEmpty()) {
			builder.append("上游警告数：").append(result.getWarnings().size()).append("\n");
		}
		builder.append("\n");
		builder.append("问题总数：").append(problems.size()).append("\n\n");
		if (problems.isEmpty()) {
			builder.append("当前未检测到明显的设备碰撞问题。\n");
			builder.append("说明：第一版仅输出问题列表，不自动优化布局。\n");
			return builder.toString();
		}
		builder.append("问题列表：\n");
		for (GpCollisionProblem problem : problems) {
			builder.append("- ").append(problem.getProblemId())
					.append(" | 类型=").append(problem.getProblemType())
					.append(" | 严重等级=").append(problem.getSeverity())
					.append(" | 对象A=").append(problem.getObjectA())
					.append(" | 对象B=").append(problem.getObjectB())
					.append(" | 位置=").append(problem.getLocation())
					.append(" | 备注=").append(problem.getNote())
					.append("\n");
		}
		builder.append("\n说明：第一版仅输出问题列表，不做自动布局优化与自动修正。\n");
		return builder.toString();
	}

	private Path writeCollisionReport(GpSceneGenerationResult result, List<GpCollisionProblem> problems) {
		Path output = result.getSession().getInputSummary().getGimProRuntimeDir().resolve("collision-report.json");
		try {
			Files.createDirectories(output.getParent());
			Files.write(output, buildCollisionJson(result, problems).getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new IllegalStateException("写入 GIM Pro 三维碰撞检测结果失败：" + output, e);
		}
		return output;
	}

	private String buildCollisionJson(GpSceneGenerationResult result, List<GpCollisionProblem> problems) {
		StringBuilder builder = new StringBuilder();
		builder.append("{\n");
		builder.append("  \"plugin\": \"GIM Pro\",\n");
		builder.append("  \"sessionId\": \"").append(escape(result.getSession().getSessionId())).append("\",\n");
		builder.append("  \"sourceMode\": \"").append(escape(result.getSession().getSourceMode())).append("\",\n");
		builder.append("  \"nodeCount\": ").append(result.getNodes().size()).append(",\n");
		builder.append("  \"problemCount\": ").append(problems.size()).append(",\n");
		builder.append("  \"upstreamErrors\": [");
		appendStringArray(builder, result.getErrors());
		builder.append("],\n");
		builder.append("  \"upstreamWarnings\": [");
		appendStringArray(builder, result.getWarnings());
		builder.append("],\n");
		builder.append("  \"problems\": [\n");
		for (int i = 0; i < problems.size(); i++) {
			GpCollisionProblem problem = problems.get(i);
			builder.append("    {\n")
					.append("      \"problemId\": \"").append(escape(problem.getProblemId())).append("\",\n")
					.append("      \"problemType\": \"").append(escape(problem.getProblemType())).append("\",\n")
					.append("      \"severity\": \"").append(escape(problem.getSeverity())).append("\",\n")
					.append("      \"objectA\": \"").append(escape(problem.getObjectA())).append("\",\n")
					.append("      \"objectB\": \"").append(escape(problem.getObjectB())).append("\",\n")
					.append("      \"location\": \"").append(escape(problem.getLocation())).append("\",\n")
					.append("      \"note\": \"").append(escape(problem.getNote())).append("\"\n")
					.append("    }");
			if (i < problems.size() - 1) {
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

	private boolean overlaps(double ax, double ay, double aSize, double bx, double by, double bSize) {
		double aHalf = aSize / 2D;
		double bHalf = bSize / 2D;
		return Math.abs(ax - bx) <= (aHalf + bHalf) && Math.abs(ay - by) <= (aHalf + bHalf);
	}

	private double average(double a, double b) {
		return Math.round(((a + b) / 2D) * 100D) / 100D;
	}

	private double resolveCollisionSize(GpAdaptedSceneNode node) {
		String attrs = node.getModelAttributes();
		if (attrs != null && !attrs.trim().isEmpty()) {
			String lower = attrs.toLowerCase(Locale.ROOT);
			if (containsDimensionKey(lower)) {
				Matcher matcher = NUMBER_PATTERN.matcher(lower);
				double max = 0.0D;
				while (matcher.find()) {
					try {
						max = Math.max(max, Double.parseDouble(matcher.group(1)));
					} catch (Exception ignored) {
						// keep scanning
					}
				}
				if (max > 0.0D) {
					return Math.max(1.0D, Math.min(max, 20.0D));
				}
			}
		}
		return 1.5D;
	}

	private boolean containsDimensionKey(String attrs) {
		return attrs.contains("width") || attrs.contains("length") || attrs.contains("size")
				|| attrs.contains("宽") || attrs.contains("长") || attrs.contains("尺寸");
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

	private GpSceneGenerationService sceneGenerationService() {
		if (sceneGenerationService == null) {
			sceneGenerationService = new GpSceneGenerationService();
		}
		return sceneGenerationService;
	}
}

