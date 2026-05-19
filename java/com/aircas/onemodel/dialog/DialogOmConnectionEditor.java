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
 * 新建关联关系。
 */
public class DialogOmConnectionEditor extends SmDialog {

	private final OneModelMapRepository repository = new OneModelMapRepository();
	private final JComboBox<OneModelEquipmentRecord> fromComboBox = new JComboBox<>();
	private final JComboBox<OneModelEquipmentRecord> toComboBox = new JComboBox<>();
	private final JComboBox<String> typeComboBox = new JComboBox<>(new String[]{"电气连接", "从属关系"});
	private final JComboBox<String> statusComboBox = new JComboBox<>(new String[]{"现状", "规划"});

	public DialogOmConnectionEditor() {
		setTitle("新建关联关系");
		setSize(new Dimension(520, 260));
		setLayout(new GridBagLayout());
		loadEquipments();
		buildLayout();
	}

	private void loadEquipments() {
		List<OneModelEquipmentRecord> equipments = repository.listEquipments();
		DefaultComboBoxModel<OneModelEquipmentRecord> fromModel = new DefaultComboBoxModel<>(equipments.toArray(new OneModelEquipmentRecord[0]));
		DefaultComboBoxModel<OneModelEquipmentRecord> toModel = new DefaultComboBoxModel<>(equipments.toArray(new OneModelEquipmentRecord[0]));
		fromComboBox.setModel(fromModel);
		toComboBox.setModel(toModel);
	}

	private void buildLayout() {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.add(new JLabel("设备 A"), new GridBagConstraintsHelper(0, 0).setInsets(0, 0, 6, 6).setAnchor(GridBagConstraints.WEST));
		panel.add(fromComboBox, new GridBagConstraintsHelper(1, 0).setInsets(0, 0, 6, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
		panel.add(new JLabel("设备 B"), new GridBagConstraintsHelper(0, 1).setInsets(0, 0, 6, 6).setAnchor(GridBagConstraints.WEST));
		panel.add(toComboBox, new GridBagConstraintsHelper(1, 1).setInsets(0, 0, 6, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
		panel.add(new JLabel("关系类型"), new GridBagConstraintsHelper(0, 2).setInsets(0, 0, 6, 6).setAnchor(GridBagConstraints.WEST));
		panel.add(typeComboBox, new GridBagConstraintsHelper(1, 2).setInsets(0, 0, 6, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
		panel.add(new JLabel("状态"), new GridBagConstraintsHelper(0, 3).setInsets(0, 0, 6, 6).setAnchor(GridBagConstraints.WEST));
		panel.add(statusComboBox, new GridBagConstraintsHelper(1, 3).setInsets(0, 0, 6, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
		panel.add(new JLabel("系统将自动生成设备 A 与设备 B 之间的直线几何。"), new GridBagConstraintsHelper(0, 4, 2, 1).setInsets(0, 0, 6, 0).setAnchor(GridBagConstraints.WEST));

		SmButton okButton = new SmButton("写入关联关系");
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
		OneModelEquipmentRecord from = (OneModelEquipmentRecord) fromComboBox.getSelectedItem();
		OneModelEquipmentRecord to = (OneModelEquipmentRecord) toComboBox.getSelectedItem();
		if (from == null || to == null) {
			JOptionPane.showMessageDialog(this, "请先创建设备点，再建立关联关系。", "提示", JOptionPane.WARNING_MESSAGE);
			return;
		}
		if (from.getEquipmentId().equals(to.getEquipmentId())) {
			JOptionPane.showMessageDialog(this, "关联关系的两端设备不能相同。", "提示", JOptionPane.WARNING_MESSAGE);
			return;
		}
		try {
			repository.addConnection(from.getEquipmentId(), to.getEquipmentId(),
					String.valueOf(typeComboBox.getSelectedItem()), String.valueOf(statusComboBox.getSelectedItem()));
			dispose();
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, ex.getMessage(), "提示", JOptionPane.WARNING_MESSAGE);
		}
	}
}

