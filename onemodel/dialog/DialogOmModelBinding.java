package com.aircas.onemodel.dialog;

import com.aircas.onemodel.model.OneModelEquipmentRecord;
import com.aircas.onemodel.model.OneModelModelResource;
import com.aircas.onemodel.service.OneModelMapRepository;
import com.aircas.onemodel.service.OneModelModelScanner;
import com.supermap.desktop.controls.ui.controls.SmDialog;
import com.supermap.desktop.controls.ui.controls.button.SmButton;
import com.supermap.desktop.core.ui.controls.GridBagConstraintsHelper;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * 设备-模型绑定弹窗。
 */
public class DialogOmModelBinding extends SmDialog {

	private final OneModelMapRepository repository = new OneModelMapRepository();
	private final OneModelModelScanner modelScanner = new OneModelModelScanner();
	private final JComboBox<OneModelEquipmentRecord> equipmentComboBox = new JComboBox<>();
	private final JList<OneModelModelResource> modelList = new JList<>();
	private List<OneModelModelResource> resources = new ArrayList<>();

	public DialogOmModelBinding() {
		setTitle("设备-模型绑定");
		setSize(new Dimension(760, 460));
		setLayout(new GridBagLayout());
		loadEquipments();
		loadModels();
		buildLayout();
	}

	private void loadEquipments() {
		List<OneModelEquipmentRecord> equipments = repository.listEquipments();
		equipmentComboBox.setModel(new DefaultComboBoxModel<>(equipments.toArray(new OneModelEquipmentRecord[0])));
		equipmentComboBox.addActionListener(e -> refreshModelList());
	}

	private void loadModels() {
		resources = modelScanner.scanModels();
		refreshModelList();
	}

	private void refreshModelList() {
		OneModelEquipmentRecord selected = (OneModelEquipmentRecord) equipmentComboBox.getSelectedItem();
		List<OneModelModelResource> filtered = new ArrayList<>();
		for (OneModelModelResource resource : resources) {
			if (selected == null || resource.getModelType().contains(selected.getModelCategory()) || selected.getModelCategory().contains(resource.getModelType()) || "未分类".equals(resource.getModelType())) {
				filtered.add(resource);
			}
		}
		modelList.setListData(filtered.toArray(new OneModelModelResource[0]));
		modelList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	}

	private void buildLayout() {
		JPanel headerPanel = new JPanel(new GridBagLayout());
		headerPanel.add(new JLabel("设备"), new GridBagConstraintsHelper(0, 0).setInsets(0, 0, 6, 6).setAnchor(GridBagConstraints.WEST));
		headerPanel.add(equipmentComboBox, new GridBagConstraintsHelper(1, 0).setInsets(0, 0, 6, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
		add(headerPanel, new GridBagConstraintsHelper(0, 0).setInsets(8, 8, 4, 8).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
		add(new JScrollPane(modelList), new GridBagConstraintsHelper(0, 1).setInsets(0, 8, 4, 8).setWeight(1, 1).setFill(GridBagConstraints.BOTH));

		SmButton okButton = new SmButton("写入模型绑定");
		okButton.addActionListener(e -> saveAndClose());
		SmButton closeButton = new SmButton("关闭");
		closeButton.addActionListener(e -> dispose());
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(okButton);
		buttonPanel.add(closeButton);
		add(buttonPanel, new GridBagConstraintsHelper(0, 2).setInsets(0, 8, 8, 8).setAnchor(GridBagConstraints.EAST));
	}

	private void saveAndClose() {
		OneModelEquipmentRecord equipment = (OneModelEquipmentRecord) equipmentComboBox.getSelectedItem();
		OneModelModelResource resource = modelList.getSelectedValue();
		if (equipment == null || resource == null) {
			JOptionPane.showMessageDialog(this, "请选择设备和模型资源。", "提示", JOptionPane.WARNING_MESSAGE);
			return;
		}
		repository.updateModelBinding(equipment.getEquipmentId(), resource);
		dispose();
	}
}

