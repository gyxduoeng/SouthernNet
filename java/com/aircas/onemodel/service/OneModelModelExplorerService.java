package com.aircas.onemodel.service;

import com.aircas.onemodel.model.OneModelSelectedEquipmentContext;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 模型浏览共享状态服务。
 *
 * <p>负责在“工作空间管理器风格”的模型树面板与属性面板之间同步当前设备、解析结果和节点选择。</p>
 */
public final class OneModelModelExplorerService {

	private static final String ROOT_LABEL = "模型";
	private static final OneModelModelExplorerService INSTANCE = new OneModelModelExplorerService();

	private OneModelSelectedEquipmentContext currentContext;
	private OneModelGimTreeService.ParseResult currentParseResult;
	private String currentMessage = "请先选中一个设备点。";
	private String currentNodeLabel = ROOT_LABEL;
	private Map<String, String> currentNodeProperties = placeholderProperties(currentMessage);

	private OneModelModelExplorerService() {
	}

	public static OneModelModelExplorerService getInstance() {
		return INSTANCE;
	}

	public synchronized void showContext(OneModelSelectedEquipmentContext context,
			OneModelGimTreeService.ParseResult parseResult) {
		this.currentContext = context;
		this.currentParseResult = parseResult;
		this.currentMessage = "";
		this.currentNodeLabel = ROOT_LABEL;
		this.currentNodeProperties = buildRootProperties(context, parseResult);
	}

	public synchronized void clear(String message) {
		this.currentContext = null;
		this.currentParseResult = null;
		this.currentMessage = isBlank(message) ? "请先选中一个设备点。" : message.trim();
		this.currentNodeLabel = ROOT_LABEL;
		this.currentNodeProperties = placeholderProperties(this.currentMessage);
	}

	public synchronized void selectNode(String label, Map<String, String> properties) {
		this.currentNodeLabel = isBlank(label) ? ROOT_LABEL : label.trim();
		if (properties == null || properties.isEmpty()) {
			this.currentNodeProperties = placeholderProperties("暂无数据");
		} else {
			this.currentNodeProperties = Collections.unmodifiableMap(new LinkedHashMap<>(properties));
		}
	}

	public synchronized OneModelSelectedEquipmentContext getCurrentContext() {
		return currentContext;
	}

	public synchronized OneModelGimTreeService.ParseResult getCurrentParseResult() {
		return currentParseResult;
	}

	public synchronized String getCurrentMessage() {
		return currentMessage;
	}

	public synchronized String getCurrentNodeLabel() {
		return currentNodeLabel;
	}

	public synchronized Map<String, String> getCurrentNodeProperties() {
		return currentNodeProperties;
	}

	private Map<String, String> buildRootProperties(OneModelSelectedEquipmentContext context,
			OneModelGimTreeService.ParseResult parseResult) {
		Map<String, String> properties = new LinkedHashMap<>();
		if (context != null) {
			properties.put("设备", context.getDisplayName());
			properties.putAll(context.getModelSummaryProperties());
		}
		if (parseResult != null) {
			properties.put("结构解析", parseResult.isSuccess() ? "已加载模型 / 子对象树" : "未完成深解析");
			if (!isBlank(parseResult.getMessage())) {
				properties.put("解析提示", parseResult.getMessage());
			}
		}
		if (properties.isEmpty()) {
			return placeholderProperties("暂无数据");
		}
		return Collections.unmodifiableMap(properties);
	}

	private static Map<String, String> placeholderProperties(String message) {
		Map<String, String> properties = new LinkedHashMap<>();
		properties.put("状态", isBlank(message) ? "请先选中一个设备点。" : message.trim());
		return Collections.unmodifiableMap(properties);
	}

	private static boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}
}

