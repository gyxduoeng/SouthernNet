package com.aircas.gimpro.service;

import com.aircas.gimpro.model.GpDataCategory;
import com.aircas.gimpro.model.GpIntegratedSource;
import com.aircas.gimpro.model.GpIntegrationReport;
import com.aircas.gimpro.model.GpSceneInputSummary;
import com.aircas.gimpro.model.GpSupportedTypeEntry;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * GIM Pro 多源数据整合服务。
 */
public class GpDataIntegrationService {

	private final GpSceneInputService inputService = new GpSceneInputService();
	private final GpDesktopSupportedTypeCatalogService catalogService = new GpDesktopSupportedTypeCatalogService();

	public GpIntegrationReport buildReport() {
		GpSceneInputSummary summary = inputService.loadCurrentSummary();
		List<GpSupportedTypeEntry> supportedTypes = new ArrayList<>(catalogService.listSupportedTypes());
		List<GpIntegratedSource> sources = new ArrayList<>();
		List<String> warnings = new ArrayList<>();
		List<String> errors = new ArrayList<>();
		sources.add(new GpIntegratedSource(GpDataCategory.THREE_D,
				"共享工程数据源",
				summary.getSharedDatasourcePath() == null ? "" : summary.getSharedDatasourcePath().toString(),
				summary.isSharedDatasourceExists(),
				"GIM Pro 三维场景主链优先直读共享数据源"));
		sources.add(new GpIntegratedSource(GpDataCategory.THREE_D,
				"模型库目录",
				summary.getModelLibraryPath(),
				summary.isModelLibraryExists(),
				"供三维场景生成、模型调整和属性查看使用"));
		sources.add(new GpIntegratedSource(GpDataCategory.THREE_D,
				"scene-plan 兼容输入",
				summary.getOneModelScenePlanFile() == null ? "" : summary.getOneModelScenePlanFile().toString(),
				summary.isScenePlanExists(),
				"仅作为兼容输入保留，不作为首版主链前提"));
		if (!summary.isSharedDatasourceExists()) {
			errors.add("未找到共享工程数据源，GIM Pro 当前无法按主链生成三维场景。");
		}
		if (!summary.isWorkspaceFileExists()) {
			warnings.add("当前工程工作空间文件不存在，建议先保存工程工作空间。" );
		}
		if (!summary.isModelLibraryExists()) {
			warnings.add("模型库目录不存在或未配置，后续模型库调整与属性查看能力会受影响。");
		}
		if (summary.getProjectFolder() == null || !Files.exists(summary.getProjectFolder())) {
			warnings.add("工程目录不存在或不可访问，多源数据目录组织能力会受影响。");
		}
		return new GpIntegrationReport(supportedTypes, sources, warnings, errors);
	}

	public String buildHumanReadableReport() {
		GpIntegrationReport report = buildReport();
		StringBuilder builder = new StringBuilder();
		builder.append("GIM Pro 多源数据整合诊断\n\n");
		builder.append("平台允许接入类型（按当前首版目录组织）：\n");
		for (GpSupportedTypeEntry entry : report.getSupportedTypes()) {
			builder.append("- [").append(entry.getCategory().getLabel()).append("] ")
					.append(entry.getDisplayName()).append(" (").append(entry.getCode()).append(")")
					.append(entry.getNotes().isEmpty() ? "" : "：" + entry.getNotes())
					.append("\n");
		}
		builder.append("\n当前已识别数据源：\n");
		for (GpIntegratedSource source : report.getSources()) {
			builder.append("- [").append(source.getCategory().getLabel()).append("] ")
					.append(source.getName()).append(" -> ")
					.append(source.isAvailable() ? "可用" : "不可用")
					.append(source.getPath().isEmpty() ? "" : " | " + source.getPath())
					.append(source.getNotes().isEmpty() ? "" : " | " + source.getNotes())
					.append("\n");
		}
		if (!report.getErrors().isEmpty()) {
			builder.append("\n错误项：\n");
			for (String error : report.getErrors()) {
				builder.append("- ").append(error).append("\n");
			}
		}
		if (!report.getWarnings().isEmpty()) {
			builder.append("\n警告项：\n");
			for (String warning : report.getWarnings()) {
				builder.append("- ").append(warning).append("\n");
			}
		}
		builder.append("\n说明：当前首版多源数据整合按二维/三维分类统一组织，具体加载与显示优先复用 iDesktopX 平台能力。\n");
		return builder.toString();
	}
}

