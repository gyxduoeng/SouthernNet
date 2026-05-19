package com.aircas.gimpro.service;

import com.aircas.gimpro.session.GpSceneSession;
import com.aircas.gimpro.session.GpSceneSessionService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GIM Pro 三维接入输入校验服务。
 */
public class GpSceneValidationService {

	private final GpSceneSessionService sessionService = new GpSceneSessionService();

	public ValidationResult validateCurrentProject() {
		GpSceneSession session = sessionService.buildCurrentSession();
		return new ValidationResult(session,
				new ArrayList<>(session.getErrors()),
				new ArrayList<>(session.getWarnings()));
	}

	public String buildValidationReport() {
		ValidationResult result = validateCurrentProject();
		StringBuilder builder = new StringBuilder();
		builder.append("GIM Pro 三维接入校验结果\n\n");
		builder.append(sessionService.buildSessionReport(result.getSession())).append("\n");
		builder.append("校验状态：").append(result.isSuccess() ? "通过" : "失败").append("\n");
		builder.append("错误数：").append(result.getErrors().size()).append("\n");
		builder.append("警告数：").append(result.getWarnings().size()).append("\n");
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
		builder.append("\n建议：GIM Pro 当前基础实现优先负责接入校验、会话清单和菜单编排；三维浏览与交互、轻量化加载与动态调度优先复用 iDesktopX 原生能力，不在本轮重复开发。\n");
		return builder.toString();
	}

	public static final class ValidationResult {
		private final GpSceneSession session;
		private final List<String> errors;
		private final List<String> warnings;

		private ValidationResult(GpSceneSession session, List<String> errors, List<String> warnings) {
			this.session = session;
			this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
			this.warnings = Collections.unmodifiableList(new ArrayList<>(warnings));
		}

		public GpSceneSession getSession() {
			return session;
		}

		public List<String> getErrors() {
			return errors;
		}

		public List<String> getWarnings() {
			return warnings;
		}

		public boolean isSuccess() {
			return errors.isEmpty();
		}
	}
}

