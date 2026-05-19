package com.aircas.onemodel.dialog;

import com.aircas.onemodel.model.OneModelLayerOption;
import com.aircas.onemodel.model.OneModelModelResource;
import com.aircas.onemodel.service.OneModelEquipmentAttributeService;
import com.aircas.onemodel.service.OneModelMapRepository;
import com.aircas.onemodel.service.OneModelModelScanner;
import com.supermap.desktop.controls.ui.controls.SmDialog;
import com.supermap.desktop.controls.ui.controls.button.SmButton;
import com.supermap.desktop.core.ui.controls.GridBagConstraintsHelper;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * 设备属性初始化/补录弹窗。
 */
public class DialogOmEquipmentAttributeInit extends SmDialog {

	private final OneModelMapRepository repository = new OneModelMapRepository();
	private final OneModelModelScanner modelScanner = new OneModelModelScanner();
	private final OneModelEquipmentAttributeService attributeService = new OneModelEquipmentAttributeService();

	private final JComboBox<OneModelLayerOption> layerComboBox = new JComboBox<>();
	private final JTextField modelCategoryField = new JTextField();
	private final JComboBox<ModelOption> modelComboBox = new JComboBox<>();
	private final JTextField namePrefixField = new JTextField();
	private final JTextField codePrefixField = new JTextField();
	private final JComboBox<String> statusComboBox = new JComboBox<>(new String[]{"现状", "规划"});
	private final JCheckBox overwriteCheckBox = new JCheckBox("覆盖已有属性值", false);
	private List<OneModelModelResource> resources = new ArrayList<>();
	private boolean internalUpdate;

	public DialogOmEquipmentAttributeInit() {
		setTitle("设备属性初始化 / 补录");
		setSize(new Dimension(760, 340));
		setLayout(new GridBagLayout());
		loadLayers();
		loadModels();
		buildLayout();
		refreshSuggestions();
		bindCategoryRefresh();
	}

	private void loadLayers() {
		List<OneModelLayerOption> layers = repository.listEquipmentLayers();
		layerComboBox.setModel(new DefaultComboBoxModel<>(layers.toArray(new OneModelLayerOption[0])));
		layerComboBox.addActionListener(e -> refreshSuggestions());
	}

	private void loadModels() {
		resources = modelScanner.scanModels();
		refreshModelOptions();
	}

	private void buildLayout() {
		JPanel formPanel = new JPanel(new GridBagLayout());
		formPanel.add(new JLabel("设备图层"), new GridBagConstraintsHelper(0, 0).setInsets(0, 0, 8, 6).setAnchor(GridBagConstraints.WEST));
		formPanel.add(layerComboBox, new GridBagConstraintsHelper(1, 0).setInsets(0, 0, 8, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
		formPanel.add(new JLabel("模型类别"), new GridBagConstraintsHelper(0, 1).setInsets(0, 0, 8, 6).setAnchor(GridBagConstraints.WEST));
		formPanel.add(modelCategoryField, new GridBagConstraintsHelper(1, 1).setInsets(0, 0, 8, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
		formPanel.add(new JLabel("模型推荐（可选）"), new GridBagConstraintsHelper(0, 2).setInsets(0, 0, 8, 6).setAnchor(GridBagConstraints.WEST));
		formPanel.add(modelComboBox, new GridBagConstraintsHelper(1, 2).setInsets(0, 0, 8, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
		formPanel.add(new JLabel("名称前缀"), new GridBagConstraintsHelper(0, 3).setInsets(0, 0, 8, 6).setAnchor(GridBagConstraints.WEST));
		formPanel.add(namePrefixField, new GridBagConstraintsHelper(1, 3).setInsets(0, 0, 8, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
		formPanel.add(new JLabel("编码前缀"), new GridBagConstraintsHelper(0, 4).setInsets(0, 0, 8, 6).setAnchor(GridBagConstraints.WEST));
		formPanel.add(codePrefixField, new GridBagConstraintsHelper(1, 4).setInsets(0, 0, 8, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
		formPanel.add(new JLabel("状态"), new GridBagConstraintsHelper(0, 5).setInsets(0, 0, 8, 6).setAnchor(GridBagConstraints.WEST));
		formPanel.add(statusComboBox, new GridBagConstraintsHelper(1, 5).setInsets(0, 0, 8, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
		formPanel.add(overwriteCheckBox, new GridBagConstraintsHelper(1, 6).setInsets(0, 0, 0, 0).setAnchor(GridBagConstraints.WEST));

		SmButton okButton = new SmButton("执行初始化 / 补录");
		okButton.addActionListener(e -> initializeAttributes());
		SmButton closeButton = new SmButton("关闭");
		closeButton.addActionListener(e -> dispose());
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(okButton);
		buttonPanel.add(closeButton);

		add(formPanel, new GridBagConstraintsHelper(0, 0).setInsets(8, 8, 4, 8).setWeight(1, 1).setFill(GridBagConstraints.BOTH));
		add(buttonPanel, new GridBagConstraintsHelper(0, 1).setInsets(0, 8, 8, 8).setAnchor(GridBagConstraints.EAST));
	}

	private void refreshSuggestions() {
		OneModelLayerOption option = (OneModelLayerOption) layerComboBox.getSelectedItem();
		String caption = option == null ? "" : option.getCaption();
		internalUpdate = true;
		try {
			modelCategoryField.setText(caption == null ? "" : caption);
			namePrefixField.setText(caption == null ? "设备" : caption);
			codePrefixField.setText(caption == null ? "EQ" : caption.replaceAll("\\s+", "").toUpperCase());
		} finally {
			internalUpdate = false;
		}
		refreshModelOptions();
		selectLayerDefaultModel(option);
	}

	private void bindCategoryRefresh() {
		modelCategoryField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				refreshIfNeeded();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				refreshIfNeeded();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				refreshIfNeeded();
			}

			private void refreshIfNeeded() {
				if (!internalUpdate) {
					refreshModelOptions();
				}
			}
		});
	}

	private void refreshModelOptions() {
		String category = modelCategoryField.getText() == null ? "" : modelCategoryField.getText().trim();
		DefaultComboBoxModel<ModelOption> model = new DefaultComboBoxModel<>();
		model.addElement(new ModelOption(null));
		for (OneModelModelResource resource : resources) {
			if (category.isEmpty() || resource.getModelType().contains(category) || category.contains(resource.getModelType())) {
				model.addElement(new ModelOption(resource));
			}
		}
		modelComboBox.setModel(model);
	}

	private void selectLayerDefaultModel(OneModelLayerOption layerOption) {
		OneModelModelResource defaultResource = attributeService.findLayerDefaultModel(layerOption, modelCategoryField.getText().trim());
		if (defaultResource == null) {
			return;
		}
		DefaultComboBoxModel<ModelOption> model = (DefaultComboBoxModel<ModelOption>) modelComboBox.getModel();
		int matchedIndex = -1;
		for (int i = 0; i < model.getSize(); i++) {
			ModelOption option = model.getElementAt(i);
			if (option != null && option.resource != null && isSameModel(option.resource, defaultResource)) {
				matchedIndex = i;
				break;
			}
		}
		if (matchedIndex < 0) {
			model.addElement(new ModelOption(defaultResource));
			matchedIndex = model.getSize() - 1;
		}
		modelComboBox.setSelectedIndex(matchedIndex);
	}

	private boolean isSameModel(OneModelModelResource left, OneModelModelResource right) {
		if (left == null || right == null) {
			return false;
		}
		String leftPath = left.getModelPath() == null ? "" : left.getModelPath().trim();
		String rightPath = right.getModelPath() == null ? "" : right.getModelPath().trim();
		if (!leftPath.isEmpty() && leftPath.equalsIgnoreCase(rightPath)) {
			return true;
		}
		String leftId = left.getModelId() == null ? "" : left.getModelId().trim();
		String rightId = right.getModelId() == null ? "" : right.getModelId().trim();
		return !leftId.isEmpty() && leftId.equalsIgnoreCase(rightId);
	}

	private void initializeAttributes() {
		OneModelLayerOption layerOption = (OneModelLayerOption) layerComboBox.getSelectedItem();
		if (layerOption == null) {
			JOptionPane.showMessageDialog(this, "当前没有可初始化的设备图层。", "提示", JOptionPane.WARNING_MESSAGE);
			return;
		}
		try {
			ModelOption option = (ModelOption) modelComboBox.getSelectedItem();
			String report = attributeService.initializeAttributes(
					layerOption,
					modelCategoryField.getText().trim(),
					option == null ? null : option.resource,
					namePrefixField.getText().trim(),
					codePrefixField.getText().trim(),
					String.valueOf(statusComboBox.getSelectedItem()),
					overwriteCheckBox.isSelected());
			new DialogOneModelTextResult("设备属性初始化结果", report).showDialog();
			dispose();
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, ex.getMessage(), "设备属性初始化失败", JOptionPane.WARNING_MESSAGE);
		}
	}

	private static final class ModelOption {
		private final OneModelModelResource resource;

		private ModelOption(OneModelModelResource resource) {
			this.resource = resource;
		}

		@Override
		public String toString() {
			return resource == null ? "不指定具体模型" : resource.toString();
		}
	}
}


