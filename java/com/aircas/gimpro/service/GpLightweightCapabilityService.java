package com.aircas.gimpro.service;

import com.aircas.gimpro.model.GpLightweightSessionConfig;
import com.aircas.gimpro.session.GpSceneSession;
import com.aircas.gimpro.session.GpSceneSessionService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * GIM Pro 轻量化能力接入服务。
 *
 * <p>当前版本不实现底层轻量化引擎，而是组织 GIM Pro 对 iDesktopX 平台能力的接入参数与说明。</p>
 */
public class GpLightweightCapabilityService {

	private GpSceneSessionService sessionService;
	private final GpSceneManifestService manifestService = new GpSceneManifestService();

	public GpLightweightSessionConfig buildConfig() {
		GpSceneSession session = sessionService().buildCurrentSession();
		return buildConfig(session);
	}

	public GpLightweightSessionConfig buildConfig(GpSceneSession session) {
		Path runtimeDir = session.getInputSummary().getGimProRuntimeDir();
		Path cacheDir = runtimeDir.resolve("cache");
		Path manifestFile = runtimeDir.resolve("scene-manifest.json");
		return new GpLightweightSessionConfig(
				session.getInputSummary().getProjectId(),
				session.getInputSummary().getProjectName(),
				runtimeDir,
				cacheDir,
				manifestFile,
				session.getNodeCount(),
				true);
	}

	public String prepareAndDescribe() {
		GpSceneSession session = sessionService().buildCurrentSession();
		return prepareAndDescribe(session);
	}

	public String prepareAndDescribe(GpSceneSession session) {
		sessionService().writeSession(session);
		Path manifestOutput = manifestService.writeManifest(session);
		GpLightweightSessionConfig config = buildConfig(session);
		Path output = writeConfig(config);
		return buildDescription(config, output, manifestOutput);
	}

	public Path writeConfig(GpLightweightSessionConfig config) {
		Path output = config.getRuntimeDir().resolve("lightweight-session.json");
		try {
			Files.createDirectories(config.getCacheDir());
			Files.write(output, buildJson(config).getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new IllegalStateException("写入轻量化接入配置失败：" + output, e);
		}
		return output;
	}

	public String buildDescription(GpLightweightSessionConfig config, Path output) {
		return buildDescription(config, output, config.getManifestFile());
	}

	public String buildDescription(GpLightweightSessionConfig config, Path output, Path manifestOutput) {
		StringBuilder builder = new StringBuilder();
		builder.append("GIM Pro 轻量化能力接入说明\n\n");
		builder.append("工程 ID：").append(config.getProjectId()).append("\n");
		builder.append("工程名称：").append(config.getProjectName()).append("\n");
		builder.append("运行目录：").append(config.getRuntimeDir()).append("\n");
		builder.append("缓存目录：").append(config.getCacheDir()).append("\n");
		builder.append("场景清单：").append(manifestOutput).append("\n");
		builder.append("场景节点数：").append(config.getSceneNodeCount()).append("\n");
		builder.append("平台轻量化能力：").append(config.isPlatformManaged() ? "由 iDesktopX 提供" : "未启用").append("\n");
		builder.append("接入配置文件：").append(output).append("\n\n");
		builder.append("说明：\n");
		builder.append("1. 当前 GIM Pro 不自研底层轻量化引擎。\n");
		builder.append("2. 当前 GIM Pro 负责输出会话清单、运行目录、缓存目录和业务级参数说明。\n");
		builder.append("3. 实际的轻量化加载、动态调度和底层渲染能力由 iDesktopX 平台提供。\n");
		return builder.toString();
	}

	private String buildJson(GpLightweightSessionConfig config) {
		StringBuilder builder = new StringBuilder();
		builder.append("{\n");
		builder.append("  \"projectId\": \"").append(escape(config.getProjectId())).append("\",\n");
		builder.append("  \"projectName\": \"").append(escape(config.getProjectName())).append("\",\n");
		builder.append("  \"runtimeDir\": \"").append(escape(String.valueOf(config.getRuntimeDir()))).append("\",\n");
		builder.append("  \"cacheDir\": \"").append(escape(String.valueOf(config.getCacheDir()))).append("\",\n");
		builder.append("  \"manifestFile\": \"").append(escape(String.valueOf(config.getManifestFile()))).append("\",\n");
		builder.append("  \"sceneNodeCount\": ").append(config.getSceneNodeCount()).append(",\n");
		builder.append("  \"platformManaged\": ").append(config.isPlatformManaged()).append("\n");
		builder.append("}\n");
		return builder.toString();
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

