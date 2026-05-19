package com.aircas.onemodel.dialog;

import com.aircas.onemodel.model.OneModelModelResource;
import com.aircas.onemodel.service.OneModelMapRepository;
import com.aircas.onemodel.service.OneModelModelScanner;
import com.aircas.onemodel.service.OneModelSessionStore;
import com.supermap.desktop.controls.ui.controls.SmDialog;
import com.supermap.desktop.controls.ui.controls.button.SmButton;
import com.supermap.desktop.core.ui.controls.GridBagConstraintsHelper;

import javax.swing.DefaultComboBoxModel;
import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 新增设备图层。
 */
public class DialogOmEquipmentEditor extends SmDialog {

	private final OneModelMapRepository repository = new OneModelMapRepository();
	private final OneModelModelScanner modelScanner = new OneModelModelScanner();
	private final JTextField coordinateSystemField = new JTextField();
	private final JComboBox<String> modelCategoryComboBox = new JComboBox<>();
	private final JRadioButton recommendedModelRadioButton = new JRadioButton("推荐模型", true);
	private final JRadioButton customModelRadioButton = new JRadioButton("自定义模型");
	private final JComboBox<ModelOption> modelComboBox = new JComboBox<>();
	private final JTextField customModelPathField = new JTextField();
	private final SmButton browseModelButton = new SmButton("选择文件");
	private final List<OneModelModelResource> resources = new ArrayList<>();
	private boolean updatingModelSelection;
	private boolean updatingModelSource;

	public DialogOmEquipmentEditor() {
		setTitle("新增设备");
		setSize(new Dimension(760, 320));
		setLayout(new GridBagLayout());
		modelCategoryComboBox.setMaximumRowCount(20);
		modelComboBox.setMaximumRowCount(20);
		coordinateSystemField.setEditable(false);
		coordinateSystemField.setText(OneModelSessionStore.getInstance().getParameters().getCoordinateSystemCode());
		loadModels();
		buildLayout();
		bindModelCategoryRefresh();
	}

	private void loadModels() {
		resources.clear();
		resources.addAll(modelScanner.scanModels());
		Set<String> categories = new LinkedHashSet<>(modelScanner.scanModelCategories());
		DefaultComboBoxModel<String> categoryModel = new DefaultComboBoxModel<>();
		for (String category : categories) {
			categoryModel.addElement(category);
		}
		modelCategoryComboBox.setEditable(true);
		modelCategoryComboBox.setModel(categoryModel);
		if (categoryModel.getSize() > 0) {
			modelCategoryComboBox.setSelectedIndex(0);
		}
		refreshModelRecommendations();
	}

	private void buildLayout() {
		JPanel panel = new JPanel(new GridBagLayout());
		ButtonGroup sourceGroup = new ButtonGroup();
		sourceGroup.add(recommendedModelRadioButton);
		sourceGroup.add(customModelRadioButton);
		JPanel sourcePanel = new JPanel();
		sourcePanel.add(recommendedModelRadioButton);
		sourcePanel.add(customModelRadioButton);
		panel.add(new JLabel("模型类别"), new GridBagConstraintsHelper(0, 0).setInsets(0, 0, 6, 6).setAnchor(GridBagConstraints.WEST));
		panel.add(modelCategoryComboBox, new GridBagConstraintsHelper(1, 0).setInsets(0, 0, 6, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
		panel.add(new JLabel("模型来源"), new GridBagConstraintsHelper(0, 1).setInsets(0, 0, 6, 6).setAnchor(GridBagConstraints.WEST));
		panel.add(sourcePanel, new GridBagConstraintsHelper(1, 1).setInsets(0, 0, 6, 0).setAnchor(GridBagConstraints.WEST));
		panel.add(new JLabel("推荐模型"), new GridBagConstraintsHelper(0, 2).setInsets(0, 0, 6, 6).setAnchor(GridBagConstraints.WEST));
		panel.add(modelComboBox, new GridBagConstraintsHelper(1, 2).setInsets(0, 0, 6, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
		panel.add(new JLabel("自定义模型路径"), new GridBagConstraintsHelper(0, 3).setInsets(0, 0, 6, 6).setAnchor(GridBagConstraints.WEST));
		panel.add(customModelPathField, new GridBagConstraintsHelper(1, 3).setInsets(0, 0, 6, 6).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
		browseModelButton.addActionListener(e -> chooseCustomModelPath());
		panel.add(browseModelButton, new GridBagConstraintsHelper(2, 3).setInsets(0, 0, 6, 0));
		panel.add(new JLabel("坐标系 EPSG"), new GridBagConstraintsHelper(0, 4).setInsets(0, 0, 6, 6).setAnchor(GridBagConstraints.WEST));
		panel.add(coordinateSystemField, new GridBagConstraintsHelper(1, 4).setInsets(0, 0, 6, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));

		SmButton okButton = new SmButton("创建设备图层");
		okButton.addActionListener(e -> saveAndClose());
		SmButton closeButton = new SmButton("关闭");
		closeButton.addActionListener(e -> dispose());
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(okButton);
		buttonPanel.add(closeButton);

		add(panel, new GridBagConstraintsHelper(0, 0).setInsets(8, 8, 4, 8).setWeight(1, 1).setFill(GridBagConstraints.BOTH));
		add(buttonPanel, new GridBagConstraintsHelper(0, 1).setInsets(0, 8, 8, 8).setAnchor(GridBagConstraints.EAST));
	}

	private void bindModelCategoryRefresh() {
		modelCategoryComboBox.addActionListener(e -> {
			if (!updatingModelSelection) {
				refreshModelRecommendations();
			}
		});
		modelComboBox.addActionListener(e -> {
			if (updatingModelSelection) {
				return;
			}
			ModelOption option = (ModelOption) modelComboBox.getSelectedItem();
			if (option != null && option.resource != null) {
				setModelSource(true);
				modelCategoryComboBox.getEditor().setItem(option.resource.getModelType());
				customModelPathField.setText("");
			}
		});
		recommendedModelRadioButton.addActionListener(e -> syncModelSourceState());
		customModelRadioButton.addActionListener(e -> syncModelSourceState());
		customModelPathField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				selectCustomModelIfNeeded();
				syncModelSourceState();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				selectRecommendedModelIfNeeded();
				syncModelSourceState();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				selectCustomModelIfNeeded();
				syncModelSourceState();
			}
		});
		syncModelSourceState();
	}

	private void refreshModelRecommendations() {
		String currentCategory = getModelCategoryText();
		DefaultComboBoxModel<ModelOption> model = new DefaultComboBoxModel<>();
		model.addElement(new ModelOption(null));
		for (OneModelModelResource resource : resources) {
			if (currentCategory.isEmpty() || resource.getModelType().contains(currentCategory) || currentCategory.contains(resource.getModelType())) {
				model.addElement(new ModelOption(resource));
			}
		}
		updatingModelSelection = true;
		try {
			modelComboBox.setModel(model);
			modelComboBox.setSelectedIndex(0);
		} finally {
			updatingModelSelection = false;
		}
	}

	private String getModelCategoryText() {
		Object editorItem = modelCategoryComboBox.isEditable() ? modelCategoryComboBox.getEditor().getItem() : modelCategoryComboBox.getSelectedItem();
		return editorItem == null ? "" : String.valueOf(editorItem).trim();
	}

	private void chooseCustomModelPath() {
		setModelSource(false);
		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		if (!customModelPathField.getText().trim().isEmpty()) {
			chooser.setSelectedFile(new java.io.File(customModelPathField.getText().trim()));
		}
		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			customModelPathField.setText(chooser.getSelectedFile().getAbsolutePath());
		}
	}

	private void syncModelSourceState() {
		boolean useCustomModel = customModelRadioButton.isSelected();
		modelComboBox.setEnabled(!useCustomModel);
		customModelPathField.setEnabled(useCustomModel);
		browseModelButton.setEnabled(useCustomModel);
		if (useCustomModel) {
			updatingModelSelection = true;
			try {
				if (modelComboBox.getItemCount() > 0) {
					modelComboBox.setSelectedIndex(0);
				}
			} finally {
				updatingModelSelection = false;
			}
		} else if (customModelPathField.getText() != null && !customModelPathField.getText().trim().isEmpty()) {
			customModelPathField.setText("");
		}
	}

	private OneModelModelResource resolveSelectedModel(String modelCategory) {
		if (customModelRadioButton.isSelected()) {
			String customModelPath = customModelPathField.getText() == null ? "" : customModelPathField.getText().trim();
			if (!customModelPath.isEmpty()) {
				String fileName = new java.io.File(customModelPath).getName();
				String modelName = fileName == null || fileName.trim().isEmpty() ? customModelPath : fileName;
				return new OneModelModelResource(
						"MR-" + Math.abs(customModelPath.hashCode()),
						modelName,
						modelCategory,
						customModelPath,
						"{\"modelName\":\"" + escape(modelName) + "\",\"modelType\":\"" + escape(modelCategory) + "\",\"modelPath\":\"" + escape(customModelPath) + "\"}");
			}
			return null;
		}
		ModelOption option = (ModelOption) modelComboBox.getSelectedItem();
		return option == null ? null : option.resource;
	}

	private void saveAndClose() {
		String modelCategory = getModelCategoryText();
		if (modelCategory.isEmpty()) {
			JOptionPane.showMessageDialog(this, "请手工确定模型类别。", "提示", JOptionPane.WARNING_MESSAGE);
			return;
		}
		if (customModelRadioButton.isSelected()
				&& (customModelPathField.getText() == null || customModelPathField.getText().trim().isEmpty())) {
			JOptionPane.showMessageDialog(this, "已选择自定义模型，请先指定模型文件路径。", "提示", JOptionPane.WARNING_MESSAGE);
			return;
		}
		try {
			String datasetName = repository.createEquipmentLayer(modelCategory, resolveSelectedModel(modelCategory));
			JOptionPane.showMessageDialog(this,
					"已创建模型类别图层：" + modelCategory + "\n数据集：" + datasetName,
					"新增设备", JOptionPane.INFORMATION_MESSAGE);
			dispose();
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, ex.getMessage(), "提示", JOptionPane.WARNING_MESSAGE);
		}
	}

	private void setModelSource(boolean useRecommendedModel) {
		updatingModelSource = true;
		try {
			recommendedModelRadioButton.setSelected(useRecommendedModel);
			customModelRadioButton.setSelected(!useRecommendedModel);
		} finally {
			updatingModelSource = false;
		}
	}

	private void selectCustomModelIfNeeded() {
		if (updatingModelSource) {
			return;
		}
		if (customModelPathField.getText() != null && !customModelPathField.getText().trim().isEmpty()) {
			setModelSource(false);
		}
	}

	private void selectRecommendedModelIfNeeded() {
		if (updatingModelSource) {
			return;
		}
		if (customModelPathField.getText() == null || customModelPathField.getText().trim().isEmpty()) {
			setModelSource(true);
		}
	}

	private static final class ModelOption {
		private final OneModelModelResource resource;

		private ModelOption(OneModelModelResource resource) {
			this.resource = resource;
		}

		@Override
		public String toString() {
			return resource == null ? "暂不指定具体模型" : resource.toString();
		}
	}

	private String escape(String value) {
		return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
	}
}

