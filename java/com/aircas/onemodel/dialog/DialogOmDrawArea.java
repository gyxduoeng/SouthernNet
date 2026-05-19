package com.aircas.onemodel.dialog;

import com.aircas.onemodel.service.OneModelMapBridge;
import com.supermap.desktop.controls.ui.controls.SmDialog;
import com.supermap.desktop.controls.ui.controls.button.SmButton;
import com.supermap.desktop.core.ui.controls.GridBagConstraintsHelper;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

/**
 * 绘制区域面。
 */
public class DialogOmDrawArea extends SmDialog {

	private final OneModelMapBridge mapBridge = new OneModelMapBridge();
	private final JTextField nameField = new JTextField("区域");
	private final JTextField typeField = new JTextField("功能分区");

	public DialogOmDrawArea() {
		setTitle("绘制区域");
		setSize(new Dimension(560, 220));
		setLayout(new GridBagLayout());
		buildLayout();
	}

	private void buildLayout() {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.add(new JLabel("区域名称"), new GridBagConstraintsHelper(0, 0).setInsets(0, 0, 8, 6).setAnchor(GridBagConstraints.WEST));
		panel.add(nameField, new GridBagConstraintsHelper(1, 0).setInsets(0, 0, 8, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
		panel.add(new JLabel("区域类型"), new GridBagConstraintsHelper(0, 1).setInsets(0, 0, 8, 6).setAnchor(GridBagConstraints.WEST));
		panel.add(typeField, new GridBagConstraintsHelper(1, 1).setInsets(0, 0, 8, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
		panel.add(new JLabel("在地图合法经纬度范围内拖拽绘制矩形区域。"), new GridBagConstraintsHelper(1, 2).setInsets(0, 0, 8, 0).setAnchor(GridBagConstraints.WEST));

		SmButton drawButton = new SmButton("开始绘制");
		drawButton.addActionListener(e -> beginDraw());
		SmButton closeButton = new SmButton("关闭");
		closeButton.addActionListener(e -> dispose());
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(drawButton);
		buttonPanel.add(closeButton);

		add(panel, new GridBagConstraintsHelper(0, 0).setInsets(8, 8, 4, 8).setWeight(1, 1).setFill(GridBagConstraints.BOTH));
		add(buttonPanel, new GridBagConstraintsHelper(0, 1).setInsets(0, 8, 8, 8).setAnchor(GridBagConstraints.EAST));
	}

	private void beginDraw() {
		String areaName = nameField.getText() == null ? "" : nameField.getText().trim();
		if (areaName.isEmpty()) {
			JOptionPane.showMessageDialog(this, "请输入区域名称。", "提示", JOptionPane.WARNING_MESSAGE);
			return;
		}
		try {
			if (!mapBridge.beginCreateArea(areaName, typeField.getText().trim())) {
				JOptionPane.showMessageDialog(this, "未能进入区域绘制模式，请先打开工程地图。", "提示", JOptionPane.WARNING_MESSAGE);
				return;
			}
			JOptionPane.showMessageDialog(this, "已进入区域绘制模式，请在地图上拖拽绘制矩形区域。", "绘制区域", JOptionPane.INFORMATION_MESSAGE);
			dispose();
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, ex.getMessage(), "绘制区域失败", JOptionPane.WARNING_MESSAGE);
		}
	}
}
