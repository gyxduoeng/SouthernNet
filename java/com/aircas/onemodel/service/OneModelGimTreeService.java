package com.aircas.onemodel.service;

import com.aircas.onemodel.model.OneModelGimNode;
import com.aircas.onemodel.model.OneModelModelResource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * .gim 模型树解析服务。
 *
 * <p>当前版本采用 7-Zip 读取 7z 归档中的 DEV/FAM 文本定义，优先输出模型 / 子对象树与摘要属性。</p>
 */
public class OneModelGimTreeService {

	private static final int PROCESS_TIMEOUT_SECONDS = 20;
	private static final int LABEL_PREVIEW_LIMIT = 8;

	public ParseResult parse(OneModelModelResource modelResource) {
		if (modelResource == null) {
			return ParseResult.failure("当前设备还没有指定模型。", buildSummary(null, null, null, 0, 0, 0, "失败", "当前设备还没有指定模型。"));
		}
		return parse(modelResource.getModelPath(), modelResource.getModelName(), modelResource.getModelType());
	}

	public ParseResult parse(String modelPath, String modelName, String modelType) {
		Path archive = resolveArchive(modelPath);
		if (archive == null) {
			return ParseResult.failure("当前模型路径为空。", buildSummary(modelPath, modelName, modelType, 0, 0, 0, "失败", "当前模型路径为空。"));
		}
		if (!Files.exists(archive) || !Files.isRegularFile(archive)) {
			return ParseResult.failure("模型文件不存在：" + archive, buildSummary(archive.toString(), modelName, modelType, 0, 0, 0, "失败", "模型文件不存在。"));
		}
		String executable = resolve7ZipExecutable();
		if (isBlank(executable)) {
			return ParseResult.failure("未找到 7-Zip 可执行程序，当前版本无法深度解析 .gim 归档。", buildSummary(archive.toString(), modelName, modelType, 0, 0, 0, "失败", "未找到 7-Zip 可执行程序。"));
		}
		try {
			List<ArchiveEntry> entries = listArchiveEntries(executable, archive);
			Map<String, ArchiveEntry> entryIndex = buildEntryIndex(entries);
			List<ArchiveEntry> devEntries = filterByExtension(entries, ".dev");
			List<ArchiveEntry> famEntries = filterByExtension(entries, ".fam");
			if (devEntries.isEmpty()) {
				return ParseResult.failure("模型归档中未找到 DEV 定义文件。", buildSummary(archive.toString(), modelName, modelType, entries.size(), 0, famEntries.size(), "失败", "模型归档中未找到 DEV 定义文件。"));
			}

			Map<String, DeviceDefinition> devices = new LinkedHashMap<>();
			for (ArchiveEntry entry : devEntries) {
				String content = extractEntryText(executable, archive, entry.path);
				DeviceDefinition definition = parseDeviceDefinition(entry.path, content, entryIndex);
				devices.put(keyOf(entry.path), definition);
			}

			Map<String, FamilyDefinition> families = new LinkedHashMap<>();
			for (DeviceDefinition device : devices.values()) {
				if (isBlank(device.familyEntryPath)) {
					continue;
				}
				String familyKey = keyOf(device.familyEntryPath);
				if (families.containsKey(familyKey)) {
					continue;
				}
				ArchiveEntry familyEntry = entryIndex.get(familyKey);
				if (familyEntry == null) {
					families.put(familyKey, FamilyDefinition.missing(device.familyEntryPath));
					continue;
				}
				String content = extractEntryText(executable, archive, familyEntry.path);
				families.put(familyKey, parseFamilyDefinition(familyEntry.path, content));
			}

			List<DeviceDefinition> roots = findRootDevices(devices);
			List<OneModelGimNode> rootChildren = new ArrayList<>();
			for (DeviceDefinition root : roots) {
				rootChildren.add(buildNode(root, null, devices, families, new LinkedHashSet<>()));
			}
			if (rootChildren.isEmpty()) {
				return ParseResult.failure("未能确定模型根节点。", buildSummary(archive.toString(), modelName, modelType, entries.size(), devEntries.size(), famEntries.size(), "失败", "未能确定模型根节点。"));
			}

			Map<String, String> archiveProperties = buildSummary(archive.toString(), modelName, modelType,
					entries.size(), devEntries.size(), famEntries.size(), "成功", "已解析模型 / 子对象树。", executable);
			archiveProperties.put("根设备数", String.valueOf(rootChildren.size()));
			archiveProperties.put("根设备预览", joinLabels(rootChildren));
			String archiveLabel = trimToEmpty(modelName, archive.getFileName() == null ? archive.toString() : archive.getFileName().toString(), "未命名模型");
			OneModelGimNode archiveRoot = new OneModelGimNode(archiveLabel, "GIM 模型", archiveProperties, rootChildren);
			return ParseResult.success(archiveRoot, archiveProperties);
		} catch (Exception ex) {
			String message = "解析 .gim 失败：" + trimToEmpty(ex.getMessage(), ex.getClass().getSimpleName());
			return ParseResult.failure(message, buildSummary(archive.toString(), modelName, modelType, 0, 0, 0, "失败", message, executable));
		}
	}

	private Path resolveArchive(String modelPath) {
		if (isBlank(modelPath)) {
			return null;
		}
		try {
			return Paths.get(modelPath.trim()).toAbsolutePath().normalize();
		} catch (Exception ex) {
			return null;
		}
	}

	private List<ArchiveEntry> listArchiveEntries(String executable, Path archive) throws IOException, InterruptedException {
		String output = runCommand(Arrays.asList(executable, "l", "-slt", archive.toString()), resolveProcessCharset());
		List<ArchiveEntry> entries = new ArrayList<>();
		Map<String, String> block = new LinkedHashMap<>();
		boolean inEntrySection = false;
		for (String rawLine : output.split("\\r?\\n")) {
			String line = rawLine.trim();
			if (line.isEmpty()) {
				if (inEntrySection && !block.isEmpty()) {
					appendArchiveEntry(entries, block, archive);
					block = new LinkedHashMap<>();
				}
				continue;
			}
			if (line.startsWith("----------")) {
				if (inEntrySection && !block.isEmpty()) {
					appendArchiveEntry(entries, block, archive);
					block = new LinkedHashMap<>();
				}
				inEntrySection = true;
				continue;
			}
			int index = line.indexOf(" = ");
			if (index <= 0) {
				continue;
			}
			String key = line.substring(0, index).trim();
			String value = line.substring(index + 3).trim();
			if (inEntrySection) {
				block.put(key, value);
			}
		}
		if (!block.isEmpty()) {
			appendArchiveEntry(entries, block, archive);
		}
		return entries;
	}

	private void appendArchiveEntry(List<ArchiveEntry> entries, Map<String, String> block, Path archive) {
		String path = block.get("Path");
		if (isBlank(path)) {
			return;
		}
		String normalizedArchive = archive.toString().replace('/', '\\');
		String normalizedPath = path.replace('/', '\\');
		if (normalizedArchive.equalsIgnoreCase(normalizedPath)) {
			return;
		}
		entries.add(new ArchiveEntry(normalizedPath, parseLong(block.get("Size"))));
	}

	private Map<String, ArchiveEntry> buildEntryIndex(List<ArchiveEntry> entries) {
		Map<String, ArchiveEntry> index = new LinkedHashMap<>();
		for (ArchiveEntry entry : entries) {
			index.put(keyOf(entry.path), entry);
		}
		return index;
	}

	private List<ArchiveEntry> filterByExtension(List<ArchiveEntry> entries, String extension) {
		List<ArchiveEntry> result = new ArrayList<>();
		for (ArchiveEntry entry : entries) {
			if (entry.path.toLowerCase(Locale.ROOT).endsWith(extension)) {
				result.add(entry);
			}
		}
		return result;
	}

	private DeviceDefinition parseDeviceDefinition(String entryPath, String content, Map<String, ArchiveEntry> entryIndex) {
		Map<String, String> values = parseKeyValueLines(content);
		List<ChildReference> children = new ArrayList<>();
		int childCount = parseInt(values.get("SUBDEVICES.NUM"));
		for (int i = 0; i < childCount; i++) {
			String childEntryPath = resolveSiblingEntry(entryPath, values.get("SUBDEVICE" + i), entryIndex);
			if (isBlank(childEntryPath)) {
				continue;
			}
			children.add(new ChildReference(childEntryPath, trimToEmpty(values.get("TRANSFORMMATRIX" + i))));
		}

		List<String> solidModels = new ArrayList<>();
		int solidCount = parseInt(values.get("SOLIDMODELS.NUM"));
		for (int i = 0; i < solidCount; i++) {
			String solidModel = trimToEmpty(values.get("SOLIDMODEL" + i));
			if (!solidModel.isEmpty()) {
				solidModels.add(solidModel);
			}
		}

		String symbolName = trimToEmpty(values.get("SYMBOLNAME"), deriveEntryName(entryPath));
		String familyEntryPath = resolveSiblingEntry(entryPath, values.get("BASEFAMILYPOINTER"), entryIndex);
		return new DeviceDefinition(entryPath, symbolName, familyEntryPath, children, solidModels);
	}

	private FamilyDefinition parseFamilyDefinition(String entryPath, String content) {
		List<String> attributeNames = new ArrayList<>();
		for (String rawLine : content.split("\\r?\\n")) {
			String line = trimToEmpty(rawLine);
			if (line.startsWith("=") && line.endsWith("=") && line.length() > 2) {
				attributeNames.add(trimToEmpty(line.substring(1, line.length() - 1)));
			}
		}
		return new FamilyDefinition(entryPath, attributeNames, false);
	}

	private List<DeviceDefinition> findRootDevices(Map<String, DeviceDefinition> devices) {
		Set<String> referenced = new LinkedHashSet<>();
		for (DeviceDefinition device : devices.values()) {
			for (ChildReference child : device.children) {
				referenced.add(keyOf(child.entryPath));
			}
		}
		List<DeviceDefinition> roots = new ArrayList<>();
		for (DeviceDefinition device : devices.values()) {
			if (!referenced.contains(keyOf(device.entryPath))) {
				roots.add(device);
			}
		}
		if (!roots.isEmpty()) {
			roots.sort(Comparator.comparingInt((DeviceDefinition item) -> item.children.size()).reversed()
					.thenComparing(item -> item.entryPath, String.CASE_INSENSITIVE_ORDER));
			return roots;
		}
		List<DeviceDefinition> fallback = new ArrayList<>(devices.values());
		fallback.sort(Comparator.comparingInt((DeviceDefinition item) -> item.children.size()).reversed()
				.thenComparing(item -> item.entryPath, String.CASE_INSENSITIVE_ORDER));
		return fallback.isEmpty() ? Collections.emptyList() : Collections.singletonList(fallback.get(0));
	}

	private OneModelGimNode buildNode(DeviceDefinition device, String incomingTransform,
			Map<String, DeviceDefinition> devices, Map<String, FamilyDefinition> families, Set<String> branchVisited) {
		String visitKey = keyOf(device.entryPath);
		if (!branchVisited.add(visitKey)) {
			Map<String, String> cycleProperties = new LinkedHashMap<>();
			cycleProperties.put("节点", device.symbolName);
			cycleProperties.put("状态", "检测到循环引用，已停止继续展开。");
			cycleProperties.put("DEV 条目", device.entryPath);
			return new OneModelGimNode(device.symbolName, "循环引用", cycleProperties, Collections.emptyList());
		}

		FamilyDefinition family = isBlank(device.familyEntryPath) ? null : families.get(keyOf(device.familyEntryPath));
		List<OneModelGimNode> children = new ArrayList<>();
		for (ChildReference child : device.children) {
			DeviceDefinition childDevice = devices.get(keyOf(child.entryPath));
			if (childDevice == null) {
				Map<String, String> missingProperties = new LinkedHashMap<>();
				missingProperties.put("状态", "未在归档中找到子设备定义。");
				missingProperties.put("DEV 条目", child.entryPath);
				if (!isBlank(child.transformMatrix)) {
					missingProperties.put("父级变换矩阵", child.transformMatrix);
				}
				children.add(new OneModelGimNode(deriveEntryName(child.entryPath), "缺失子设备", missingProperties,
						Collections.emptyList()));
				continue;
			}
			children.add(buildNode(childDevice, child.transformMatrix, devices, families, new LinkedHashSet<>(branchVisited)));
		}

		Map<String, String> properties = new LinkedHashMap<>();
		properties.put("节点名称", device.symbolName);
		properties.put("节点类型", device.children.isEmpty() ? "叶子部件" : "组合部件");
		properties.put("DEV 条目", device.entryPath);
		properties.put("子设备数", String.valueOf(device.children.size()));
		properties.put("实体模型数", String.valueOf(device.solidModels.size()));
		if (!device.solidModels.isEmpty()) {
			properties.put("实体模型预览", joinPreview(device.solidModels));
		}
		if (!isBlank(device.familyEntryPath)) {
			properties.put("家族模板", device.familyEntryPath);
		}
		if (family != null) {
			properties.put("家族字段数", String.valueOf(family.attributeNames.size()));
			if (!family.attributeNames.isEmpty()) {
				properties.put("家族字段预览", joinPreview(family.attributeNames));
			}
			if (family.missing) {
				properties.put("家族状态", "模板文件缺失");
			}
		}
		if (!isBlank(incomingTransform)) {
			properties.put("父级变换矩阵", incomingTransform);
		}
		return new OneModelGimNode(device.symbolName, "模型部件", properties, children);
	}

	private String resolveSiblingEntry(String currentEntryPath, String siblingName, Map<String, ArchiveEntry> entryIndex) {
		if (isBlank(siblingName)) {
			return "";
		}
		String normalized = normalizeEntryPath(siblingName);
		ArchiveEntry direct = entryIndex.get(keyOf(normalized));
		if (direct != null) {
			return direct.path;
		}
		String folder = "";
		int index = currentEntryPath.lastIndexOf('\\');
		if (index >= 0) {
			folder = currentEntryPath.substring(0, index + 1);
		}
		String combined = normalizeEntryPath(folder + siblingName);
		ArchiveEntry resolved = entryIndex.get(keyOf(combined));
		return resolved == null ? combined : resolved.path;
	}

	private Map<String, String> parseKeyValueLines(String content) {
		Map<String, String> values = new LinkedHashMap<>();
		for (String rawLine : content.split("\\r?\\n")) {
			String line = rawLine.trim();
			if (line.isEmpty()) {
				continue;
			}
			int index = line.indexOf('=');
			if (index <= 0) {
				continue;
			}
			String key = line.substring(0, index).trim();
			String value = line.substring(index + 1).trim();
			values.put(key, value);
		}
		return values;
	}

	private String extractEntryText(String executable, Path archive, String entryPath) throws IOException, InterruptedException {
		byte[] bytes = runCommandBytes(Arrays.asList(executable, "e", "-so", archive.toString(), entryPath));
		return decodeGimText(bytes);
	}

	private String runCommand(List<String> command, Charset charset) throws IOException, InterruptedException {
		byte[] bytes = runCommandBytes(command);
		return new String(bytes, charset == null ? StandardCharsets.UTF_8 : charset);
	}

	private byte[] runCommandBytes(List<String> command) throws IOException, InterruptedException {
		ProcessBuilder builder = new ProcessBuilder(command);
		builder.redirectErrorStream(true);
		Process process = builder.start();
		byte[] bytes;
		try (InputStream input = process.getInputStream(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			byte[] buffer = new byte[4096];
			int read;
			while ((read = input.read(buffer)) >= 0) {
				output.write(buffer, 0, read);
			}
			bytes = output.toByteArray();
		}
		boolean finished = process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
		if (!finished) {
			process.destroyForcibly();
			throw new IOException("调用外部解析器超时：" + command.get(0));
		}
		String text = new String(bytes, resolveProcessCharset());
		if (process.exitValue() != 0) {
			throw new IOException(trimToEmpty(text, "外部解析器返回错误代码：" + process.exitValue()));
		}
		return bytes;
	}

	private String decodeGimText(byte[] bytes) {
		String utf8 = new String(bytes, StandardCharsets.UTF_8);
		if (utf8.indexOf('\uFFFD') < 0) {
			return utf8;
		}
		Charset fallback = resolveProcessCharset();
		return new String(bytes, fallback == null ? StandardCharsets.UTF_8 : fallback);
	}

	private Charset resolveProcessCharset() {
		try {
			return Charset.defaultCharset();
		} catch (Exception ignored) {
			return StandardCharsets.UTF_8;
		}
	}

	private String resolve7ZipExecutable() {
		List<String> candidates = new ArrayList<>();
		Collections.addAll(candidates,
				"7z.exe",
				"7za.exe",
				"C:\\Program Files\\7-Zip\\7z.exe",
				"C:\\Program Files (x86)\\7-Zip\\7z.exe");
		String path = System.getenv("PATH");
		if (!isBlank(path)) {
			for (String folder : path.split(java.io.File.pathSeparator)) {
				String trimmed = trimToEmpty(folder);
				if (trimmed.isEmpty()) {
					continue;
				}
				candidates.add(trimmed + java.io.File.separator + "7z.exe");
				candidates.add(trimmed + java.io.File.separator + "7za.exe");
			}
		}
		for (String candidate : candidates) {
			if (isBlank(candidate)) {
				continue;
			}
			if ((candidate.equalsIgnoreCase("7z.exe") || candidate.equalsIgnoreCase("7za.exe")) && isCommandAvailable(candidate)) {
				return candidate;
			}
			Path pathCandidate = Paths.get(candidate);
			if (Files.exists(pathCandidate) && Files.isRegularFile(pathCandidate)) {
				return pathCandidate.toString();
			}
		}
		return "";
	}

	private boolean isCommandAvailable(String command) {
		try {
			ProcessBuilder builder = new ProcessBuilder("where", command);
			builder.redirectErrorStream(true);
			Process process = builder.start();
			boolean finished = process.waitFor(5, TimeUnit.SECONDS);
			if (!finished) {
				process.destroyForcibly();
				return false;
			}
			return process.exitValue() == 0;
		} catch (Exception ignored) {
			return false;
		}
	}

	private Map<String, String> buildSummary(String modelPath, String modelName, String modelType,
			int entryCount, int devCount, int famCount, String status, String note) {
		return buildSummary(modelPath, modelName, modelType, entryCount, devCount, famCount, status, note, "");
	}

	private Map<String, String> buildSummary(String modelPath, String modelName, String modelType,
			int entryCount, int devCount, int famCount, String status, String note, String parser) {
		Map<String, String> properties = new LinkedHashMap<>();
		properties.put("解析状态", trimToEmpty(status, "未知"));
		properties.put("模型名称", trimToEmpty(modelName, deriveEntryName(modelPath), "未命名模型"));
		properties.put("模型类别", trimToEmpty(modelType, "未分类"));
		properties.put("模型文件", trimToEmpty(modelPath));
		if (!isBlank(parser)) {
			properties.put("解析器", parser);
		}
		properties.put("归档条目数", String.valueOf(Math.max(entryCount, 0)));
		properties.put("DEV 节点数", String.valueOf(Math.max(devCount, 0)));
		properties.put("FAM 模板数", String.valueOf(Math.max(famCount, 0)));
		properties.put("备注", trimToEmpty(note));
		return properties;
	}

	private String deriveEntryName(String value) {
		if (isBlank(value)) {
			return "";
		}
		String normalized = value.replace('/', '\\');
		int index = normalized.lastIndexOf('\\');
		return index >= 0 ? normalized.substring(index + 1) : normalized;
	}

	private String normalizeEntryPath(String path) {
		String normalized = trimToEmpty(path).replace('/', '\\');
		while (normalized.startsWith("\\")) {
			normalized = normalized.substring(1);
		}
		return normalized;
	}

	private String keyOf(String path) {
		return normalizeEntryPath(path).toLowerCase(Locale.ROOT);
	}

	private int parseInt(String value) {
		try {
			return Integer.parseInt(trimToEmpty(value));
		} catch (Exception ignored) {
			return 0;
		}
	}

	private long parseLong(String value) {
		try {
			return Long.parseLong(trimToEmpty(value));
		} catch (Exception ignored) {
			return -1L;
		}
	}

	private String joinLabels(List<OneModelGimNode> nodes) {
		List<String> labels = new ArrayList<>();
		for (OneModelGimNode node : nodes) {
			if (node != null && !isBlank(node.getLabel())) {
				labels.add(node.getLabel());
			}
		}
		return joinPreview(labels);
	}

	private String joinPreview(List<String> values) {
		if (values == null || values.isEmpty()) {
			return "";
		}
		List<String> cleaned = new ArrayList<>();
		for (String value : values) {
			String trimmed = trimToEmpty(value);
			if (!trimmed.isEmpty()) {
				cleaned.add(trimmed);
			}
		}
		if (cleaned.isEmpty()) {
			return "";
		}
		if (cleaned.size() <= LABEL_PREVIEW_LIMIT) {
			return join(cleaned);
		}
		List<String> preview = cleaned.subList(0, LABEL_PREVIEW_LIMIT);
		return join(preview) + " … 共 " + cleaned.size() + " 项";
	}

	private String join(List<String> values) {
		StringBuilder builder = new StringBuilder();
		for (String value : values) {
			if (builder.length() > 0) {
				builder.append('、');
			}
			builder.append(value);
		}
		return builder.toString();
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	private String trimToEmpty(String value, String... fallbacks) {
		if (!isBlank(value)) {
			return value.trim();
		}
		if (fallbacks != null) {
			for (String fallback : fallbacks) {
				if (!isBlank(fallback)) {
					return fallback.trim();
				}
			}
		}
		return "";
	}

	private static final class ArchiveEntry {
		private final String path;

		private ArchiveEntry(String path, long ignoredSize) {
			this.path = path;
		}
	}

	private static final class DeviceDefinition {
		private final String entryPath;
		private final String symbolName;
		private final String familyEntryPath;
		private final List<ChildReference> children;
		private final List<String> solidModels;

		private DeviceDefinition(String entryPath, String symbolName, String familyEntryPath,
				List<ChildReference> children, List<String> solidModels) {
			this.entryPath = entryPath;
			this.symbolName = symbolName;
			this.familyEntryPath = familyEntryPath;
			this.children = children;
			this.solidModels = solidModels;
		}
	}

	private static final class ChildReference {
		private final String entryPath;
		private final String transformMatrix;

		private ChildReference(String entryPath, String transformMatrix) {
			this.entryPath = entryPath;
			this.transformMatrix = transformMatrix;
		}
	}

	private static final class FamilyDefinition {
		private final List<String> attributeNames;
		private final boolean missing;

		private FamilyDefinition(String ignoredEntryPath, List<String> attributeNames, boolean missing) {
			this.attributeNames = attributeNames;
			this.missing = missing;
		}

		private static FamilyDefinition missing(String entryPath) {
			return new FamilyDefinition(entryPath, Collections.emptyList(), true);
		}
	}

	public static final class ParseResult {
		private final OneModelGimNode rootNode;
		private final Map<String, String> summaryProperties;
		private final String message;

		private ParseResult(OneModelGimNode rootNode, Map<String, String> summaryProperties, String message) {
			this.rootNode = rootNode;
			this.summaryProperties = summaryProperties == null ? Collections.emptyMap()
					: Collections.unmodifiableMap(new LinkedHashMap<>(summaryProperties));
			this.message = message == null ? "" : message.trim();
		}

		public static ParseResult success(OneModelGimNode rootNode, Map<String, String> summaryProperties) {
			return new ParseResult(rootNode, summaryProperties, "");
		}

		public static ParseResult failure(String message, Map<String, String> summaryProperties) {
			return new ParseResult(null, summaryProperties, message);
		}

		public boolean isSuccess() {
			return rootNode != null;
		}

		public OneModelGimNode getRootNode() {
			return rootNode;
		}

		public Map<String, String> getSummaryProperties() {
			return summaryProperties;
		}

		public String getMessage() {
			return message;
		}
	}
}





