package com.aircas.onemodel.dock;

import com.aircas.onemodel.model.OneModelGimNode;
import com.aircas.onemodel.model.OneModelSelectedEquipmentContext;
import com.aircas.onemodel.service.OneModelGimTreeService;
import com.aircas.onemodel.service.OneModelModelExplorerService;
import com.supermap.desktop.core.Application;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工作空间管理器风格的模型树面板。
 */
public class OneModelWorkspaceTreeDockPanel extends JPanel {

	private static final String ROOT_LABEL = "模型";

	private final OneModelModelExplorerService explorerService = OneModelModelExplorerService.getInstance();
	private final JLabel titleLabel = new JLabel("模型树", SwingConstants.LEFT);
	private final JTree tree = new JTree();

	public OneModelWorkspaceTreeDockPanel() {
		setLayout(new BorderLayout(6, 6));
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
		add(titleLabel, BorderLayout.NORTH);

		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.setRootVisible(true);
		tree.setShowsRootHandles(true);
		tree.setCellRenderer(new WorkspaceLikeTreeCellRenderer());
		tree.addTreeSelectionListener(e -> {
			Object nodeObject = tree.getLastSelectedPathComponent();
			if (nodeObject instanceof DefaultMutableTreeNode) {
				Object userObject = ((DefaultMutableTreeNode) nodeObject).getUserObject();
				if (userObject instanceof PropertyNode) {
					PropertyNode node = (PropertyNode) userObject;
					explorerService.selectNode(node.label, node.properties);
					refreshPropertyDock();
				}
			}
		});

		JScrollPane scrollPane = new JScrollPane(tree);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		add(scrollPane, BorderLayout.CENTER);

		render(explorerService.getCurrentContext(), explorerService.getCurrentParseResult(), explorerService.getCurrentMessage());
	}

	public void render(OneModelSelectedEquipmentContext context, OneModelGimTreeService.ParseResult parseResult,
			String message) {
		if (context == null) {
			renderPlaceholder(message);
			return;
		}
		titleLabel.setText("模型树 - " + context.getDisplayName());
		DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(new PropertyNode(ROOT_LABEL,
				mergeRootProperties(context, parseResult)));
		rootNode.add(buildStructureNode(parseResult));
		rootNode.add(new DefaultMutableTreeNode(new PropertyNode("绑定信息", context.getModelSummaryProperties())));
		rootNode.add(new DefaultMutableTreeNode(new PropertyNode("设备属性", context.getEquipmentProperties())));
		rootNode.add(new DefaultMutableTreeNode(new PropertyNode("模型属性", context.getModelAttributeProperties())));
		rootNode.add(new DefaultMutableTreeNode(new PropertyNode("解析信息", parseResult == null
				? placeholderProperties("尚未执行 .gim 解析。") : parseResult.getSummaryProperties())));
		tree.setModel(new DefaultTreeModel(rootNode));
		expandAll();
		tree.setSelectionRow(rootNode.getChildCount() > 0 ? 1 : 0);
	}

	private void renderPlaceholder(String message) {
		titleLabel.setText("模型树");
		Map<String, String> placeholder = placeholderProperties(message);
		DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(new PropertyNode(ROOT_LABEL, placeholder));
		rootNode.add(new DefaultMutableTreeNode(new PropertyNode("绑定信息", placeholder)));
		tree.setModel(new DefaultTreeModel(rootNode));
		expandAll();
		tree.setSelectionRow(0);
	}

	private Map<String, String> mergeRootProperties(OneModelSelectedEquipmentContext context,
			OneModelGimTreeService.ParseResult parseResult) {
		Map<String, String> properties = new LinkedHashMap<>();
		properties.put("设备", context.getDisplayName());
		properties.putAll(context.getModelSummaryProperties());
		if (parseResult != null) {
			properties.put("结构解析", parseResult.isSuccess() ? "已加载模型 / 子对象树" : "未完成深解析");
			if (!parseResult.getMessage().isEmpty()) {
				properties.put("解析提示", parseResult.getMessage());
			}
		}
		return properties;
	}

	private DefaultMutableTreeNode buildStructureNode(OneModelGimTreeService.ParseResult parseResult) {
		if (parseResult == null) {
			return new DefaultMutableTreeNode(new PropertyNode("模型结构", placeholderProperties("尚未执行 .gim 解析。")));
		}
		if (!parseResult.isSuccess() || parseResult.getRootNode() == null) {
			Map<String, String> properties = new LinkedHashMap<>(parseResult.getSummaryProperties());
			if (!parseResult.getMessage().isEmpty()) {
				properties.put("提示", parseResult.getMessage());
			}
			return new DefaultMutableTreeNode(new PropertyNode("模型结构", properties));
		}
		return toTreeNode(parseResult.getRootNode());
	}

	private DefaultMutableTreeNode toTreeNode(OneModelGimNode node) {
		DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(new PropertyNode(node.getLabel(), node.getProperties()));
		for (OneModelGimNode child : node.getChildren()) {
			treeNode.add(toTreeNode(child));
		}
		return treeNode;
	}

	private Map<String, String> placeholderProperties(String message) {
		Map<String, String> properties = new LinkedHashMap<>();
		properties.put("状态", message == null || message.trim().isEmpty() ? "暂无数据" : message.trim());
		return properties;
	}

	private void expandAll() {
		for (int i = 0; i < tree.getRowCount(); i++) {
			tree.expandRow(i);
		}
	}

	private void refreshPropertyDock() {
		OneModelEquipmentPropertyDockPanel propertyPanel = resolvePropertyDockPanel();
		if (propertyPanel == null) {
			return;
		}
		OneModelSelectedEquipmentContext context = explorerService.getCurrentContext();
		if (context == null) {
			propertyPanel.clearSelection(explorerService.getCurrentMessage());
			return;
		}
		propertyPanel.renderSelection(context, explorerService.getCurrentNodeLabel(),
				explorerService.getCurrentNodeProperties());
	}

	private OneModelEquipmentPropertyDockPanel resolvePropertyDockPanel() {
		try {
			Object dockbar = Application.getActiveApplication().getMainFrame()
					.getDockbarManager().get(OneModelEquipmentPropertyDockPanel.class.getName());
			if (dockbar == null) {
				return null;
			}
			Object component = dockbar.getClass().getMethod("getInnerComponent").invoke(dockbar);
			return component instanceof OneModelEquipmentPropertyDockPanel
					? (OneModelEquipmentPropertyDockPanel) component
					: null;
		} catch (Exception ignored) {
			return null;
		}
	}

	private static final class PropertyNode {
		private final String label;
		private final Map<String, String> properties;

		private PropertyNode(String label, Map<String, String> properties) {
			this.label = label;
			this.properties = properties;
		}

		@Override
		public String toString() {
			return label;
		}
	}

	private static final class WorkspaceLikeTreeCellRenderer extends DefaultTreeCellRenderer {

		private final Icon rootIcon = UIManager.getIcon("FileView.computerIcon");
		private final Icon folderIcon = UIManager.getIcon("FileView.directoryIcon");
		private final Icon leafIcon = UIManager.getIcon("FileView.fileIcon");

		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
				boolean leaf, int row, boolean hasFocus) {
			super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
			if (row == 0 && rootIcon != null) {
				setIcon(rootIcon);
			} else if (leaf) {
				setIcon(leafIcon != null ? leafIcon : getDefaultLeafIcon());
			} else {
				setIcon(folderIcon != null ? folderIcon : (expanded ? getDefaultOpenIcon() : getDefaultClosedIcon()));
			}
			return this;
		}
	}
}


