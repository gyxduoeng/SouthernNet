package com.aircas.onemodel.dialog;

import com.aircas.onemodel.model.OneModelLayerOption;
import com.aircas.onemodel.service.OneModelMapBridge;
import com.aircas.onemodel.service.OneModelMapRepository;
import com.supermap.desktop.controls.ui.controls.SmDialog;
import com.supermap.desktop.controls.ui.controls.button.SmButton;
import com.supermap.desktop.core.ui.controls.GridBagConstraintsHelper;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.List;

/**
 * 选择设备图层并进入地图绘点模式。
 */
public class DialogOmDrawEquipmentPoint extends SmDialog {

	private final OneModelMapRepository repository = new OneModelMapRepository();
	private final OneModelMapBridge mapBridge = new OneModelMapBridge();
	private final JComboBox<OneModelLayerOption> layerComboBox = new JComboBox<>();
	private final JComboBox<String> modeComboBox = new JComboBox<>(new String[]{"单点绘制", "连续绘制"});

	public DialogOmDrawEquipmentPoint() {
		setTitle("绘制设备点");
		setSize(new Dimension(620, 230));
		setLayout(new GridBagLayout());
		loadLayers();
		buildLayout();
	}

	private void loadLayers() {
		List<OneModelLayerOption> layers = repository.listEquipmentLayers();
		layerComboBox.setModel(new DefaultComboBoxModel<>(layers.toArray(new OneModelLayerOption[0])));
	}

	private void buildLayout() {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.add(new JLabel("目标设备图层"), new GridBagConstraintsHelper(0, 0).setInsets(0, 0, 8, 6).setAnchor(GridBagConstraints.WEST));
		panel.add(layerComboBox, new GridBagConstraintsHelper(1, 0).setInsets(0, 0, 8, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
		panel.add(new JLabel("绘制模式"), new GridBagConstraintsHelper(0, 1).setInsets(0, 0, 8, 6).setAnchor(GridBagConstraints.WEST));
		panel.add(modeComboBox, new GridBagConstraintsHelper(1, 1).setInsets(0, 0, 8, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));

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
		OneModelLayerOption option = (OneModelLayerOption) layerComboBox.getSelectedItem();
		if (option == null) {
			JOptionPane.showMessageDialog(this, "当前没有设备图层，请先新增设备图层。", "提示", JOptionPane.WARNING_MESSAGE);
			return;
		}
		try {
			boolean continuous = "连续绘制".equals(String.valueOf(modeComboBox.getSelectedItem()));
			if (!mapBridge.beginCreatePoint(option.getDatasetName(), continuous)) {
				JOptionPane.showMessageDialog(this, "未能将设备图层设为当前可编辑图层，请先打开工程地图。", "提示", JOptionPane.WARNING_MESSAGE);
				return;
			}
			String message = continuous
					? "已进入连续绘制设备点模式：" + option.getCaption() + "。完成后请在设备点菜单中点击“结束绘制”。"
					: "已进入单点绘制设备点模式：" + option.getCaption() + "。请在地图上单击一次，完成后会自动退出绘制并锁定图层。";
			JOptionPane.showMessageDialog(this, message, "绘制设备点", JOptionPane.INFORMATION_MESSAGE);
			dispose();
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, ex.getMessage(), "绘制设备点失败", JOptionPane.WARNING_MESSAGE);
		}
	}
}
