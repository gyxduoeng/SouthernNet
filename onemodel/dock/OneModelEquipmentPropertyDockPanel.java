package com.aircas.onemodel.dock;

import com.aircas.onemodel.model.OneModelSelectedEquipmentContext;
import com.aircas.onemodel.service.OneModelModelExplorerService;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 设备模型属性右侧 dock 面板。
 */
public class OneModelEquipmentPropertyDockPanel extends JPanel {

	private final OneModelModelExplorerService explorerService = OneModelModelExplorerService.getInstance();
	private final JLabel titleLabel = new JLabel("模型编辑", SwingConstants.LEFT);
	private final JLabel nodeLabel = new JLabel("当前节点：模型", SwingConstants.LEFT);
	private final DefaultTableModel tableModel = new DefaultTableModel(new Object[]{"属性", "值"}, 0) {
		@Override
		public boolean isCellEditable(int row, int column) {
			return false;
		}
	};
	private final JTable propertyTable = new JTable(tableModel);

	public OneModelEquipmentPropertyDockPanel() {
		setLayout(new BorderLayout(6, 6));
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		JPanel headerPanel = new JPanel(new BorderLayout(0, 4));
		titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
		headerPanel.add(titleLabel, BorderLayout.NORTH);
		nodeLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
		headerPanel.add(nodeLabel, BorderLayout.SOUTH);
		add(headerPanel, BorderLayout.NORTH);

		propertyTable.setFillsViewportHeight(true);
		propertyTable.setRowSelectionAllowed(false);
		propertyTable.getColumnModel().getColumn(0).setPreferredWidth(140);
		propertyTable.getColumnModel().getColumn(1).setPreferredWidth(320);
		JScrollPane scrollPane = new JScrollPane(propertyTable);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		add(scrollPane, BorderLayout.CENTER);

		if (explorerService.getCurrentContext() == null) {
			clearSelection(explorerService.getCurrentMessage());
		} else {
			renderSelection(explorerService.getCurrentContext(), explorerService.getCurrentNodeLabel(),
					explorerService.getCurrentNodeProperties());
		}
	}

	public void renderSelection(OneModelSelectedEquipmentContext context, String selectedNodeLabel,
			Map<String, String> properties) {
		if (context == null) {
			clearSelection("请先选中一个设备点。");
			return;
		}
		titleLabel.setText("模型属性 - " + context.getDisplayName());
		nodeLabel.setText("当前节点：" + (selectedNodeLabel == null || selectedNodeLabel.trim().isEmpty()
				? "模型"
				: selectedNodeLabel.trim()));
		updatePropertyTable(properties);
	}

	public void clearSelection(String message) {
		titleLabel.setText("模型属性");
		nodeLabel.setText("当前节点：模型");
		Map<String, String> placeholder = new LinkedHashMap<>();
		placeholder.put("状态", message == null || message.trim().isEmpty() ? "请先选中一个设备点。" : message.trim());
		updatePropertyTable(placeholder);
	}

	private void updatePropertyTable(Map<String, String> properties) {
		tableModel.setRowCount(0);
		if (properties == null || properties.isEmpty()) {
			tableModel.addRow(new Object[]{"提示", "暂无数据"});
			return;
		}
		for (Map.Entry<String, String> entry : properties.entrySet()) {
			tableModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
		}
	}
}


