package com.aircas.onemodel.dialog;

import com.aircas.onemodel.model.OneModelEquipmentRecord;
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
 * 图元绑定弹窗。
 */
public class DialogOmGraphicBinding extends SmDialog {

	private final OneModelMapRepository repository = new OneModelMapRepository();
	private final JComboBox<OneModelEquipmentRecord> equipmentComboBox = new JComboBox<>();
	private final JComboBox<String> graphicTypeComboBox = new JComboBox<>(new String[]{"设备点", "主设备图元", "二次设备图元", "关系锚点"});

	public DialogOmGraphicBinding() {
		setTitle("图元绑定");
		setSize(new Dimension(480, 200));
		setLayout(new GridBagLayout());
		loadEquipments();
		buildLayout();
	}

	private void loadEquipments() {
		List<OneModelEquipmentRecord> equipments = repository.listEquipments();
		equipmentComboBox.setModel(new DefaultComboBoxModel<>(equipments.toArray(new OneModelEquipmentRecord[0])));
	}

	private void buildLayout() {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.add(new JLabel("设备"), new GridBagConstraintsHelper(0, 0).setInsets(0, 0, 6, 6).setAnchor(GridBagConstraints.WEST));
		panel.add(equipmentComboBox, new GridBagConstraintsHelper(1, 0).setInsets(0, 0, 6, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
		panel.add(new JLabel("图元类型"), new GridBagConstraintsHelper(0, 1).setInsets(0, 0, 6, 6).setAnchor(GridBagConstraints.WEST));
		panel.add(graphicTypeComboBox, new GridBagConstraintsHelper(1, 1).setInsets(0, 0, 6, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));

		SmButton okButton = new SmButton("写入绑定");
		okButton.addActionListener(e -> saveAndClose());
		SmButton closeButton = new SmButton("关闭");
		closeButton.addActionListener(e -> dispose());
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(okButton);
		buttonPanel.add(closeButton);

		add(panel, new GridBagConstraintsHelper(0, 0).setInsets(8, 8, 4, 8).setWeight(1, 1).setFill(GridBagConstraints.BOTH));
		add(buttonPanel, new GridBagConstraintsHelper(0, 1).setInsets(0, 8, 8, 8).setAnchor(GridBagConstraints.EAST));
	}

	private void saveAndClose() {
		OneModelEquipmentRecord equipment = (OneModelEquipmentRecord) equipmentComboBox.getSelectedItem();
		if (equipment == null) {
			JOptionPane.showMessageDialog(this, "请先创建设备点。", "提示", JOptionPane.WARNING_MESSAGE);
			return;
		}
		repository.updateGraphicBinding(equipment.getEquipmentId(), String.valueOf(graphicTypeComboBox.getSelectedItem()));
		dispose();
	}
}

