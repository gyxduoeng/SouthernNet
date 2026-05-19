package com.aircas.onemodel.dialog;

import com.aircas.onemodel.service.OneModelMapBridge;
import com.supermap.desktop.controls.ui.controls.SmDialog;
import com.supermap.desktop.controls.ui.controls.button.SmButton;
import com.supermap.desktop.core.ui.controls.GridBagConstraintsHelper;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

/**
 * 进入连接线手动绘制模式。
 */
public class DialogOmDrawConnectionLine extends SmDialog {

	private final OneModelMapBridge mapBridge = new OneModelMapBridge();
	private final JComboBox<String> typeComboBox = new JComboBox<>(new String[]{"电气连接", "从属关系"});
	private final JComboBox<String> statusComboBox = new JComboBox<>(new String[]{"现状", "规划"});

	public DialogOmDrawConnectionLine() {
		setTitle("手动绘制连接线");
		setSize(new Dimension(540, 230));
		setLayout(new GridBagLayout());
		buildLayout();
	}

	private void buildLayout() {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.add(new JLabel("连接类型"), new GridBagConstraintsHelper(0, 0).setInsets(0, 0, 8, 6).setAnchor(GridBagConstraints.WEST));
		panel.add(typeComboBox, new GridBagConstraintsHelper(1, 0).setInsets(0, 0, 8, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
		panel.add(new JLabel("状态"), new GridBagConstraintsHelper(0, 1).setInsets(0, 0, 8, 6).setAnchor(GridBagConstraints.WEST));
		panel.add(statusComboBox, new GridBagConstraintsHelper(1, 1).setInsets(0, 0, 8, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
		panel.add(new JLabel("线端点靠近设备点时会自动识别起止设备。"), new GridBagConstraintsHelper(0, 2, 2, 1).setInsets(0, 0, 8, 0).setAnchor(GridBagConstraints.WEST));

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
		try {
			String type = String.valueOf(typeComboBox.getSelectedItem());
			String status = String.valueOf(statusComboBox.getSelectedItem());
			if (!mapBridge.beginCreateConnectionLine(type, status)) {
				JOptionPane.showMessageDialog(this, "未能将连接线图层设为当前可编辑图层，请先打开工程地图。", "提示", JOptionPane.WARNING_MESSAGE);
				return;
			}
			JOptionPane.showMessageDialog(this, "已进入手动绘制连接线模式。绘制一条线后会自动写入属性并退出绘制。", "连接线", JOptionPane.INFORMATION_MESSAGE);
			dispose();
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, ex.getMessage(), "绘制连接线失败", JOptionPane.WARNING_MESSAGE);
		}
	}
}