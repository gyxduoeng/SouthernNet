package com.aircas.onemodel.dialog;

import com.aircas.onemodel.model.OneModelParameters;
import com.aircas.onemodel.service.OneModelProjectService;
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
 * 删除工程弹窗。
 */
public class DialogOmProjectDelete extends SmDialog {

	private final OneModelProjectService projectService = new OneModelProjectService();
	private final JComboBox<ProjectItem> projectComboBox = new JComboBox<>();
	private final JLabel folderLabel = new JLabel();
	private final JLabel mapLabel = new JLabel();

	public DialogOmProjectDelete() {
		setTitle("删除工程");
		setSize(new Dimension(760, 240));
		setLayout(new GridBagLayout());
		buildLayout();
		reloadProjects();
	}

	private void buildLayout() {
		projectComboBox.addActionListener(e -> refreshDetails());

		JPanel formPanel = new JPanel(new GridBagLayout());
		formPanel.add(new JLabel("工程"), new GridBagConstraintsHelper(0, 0).setInsets(0, 0, 8, 6).setAnchor(GridBagConstraints.WEST));
		formPanel.add(projectComboBox, new GridBagConstraintsHelper(1, 0).setInsets(0, 0, 8, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
		formPanel.add(folderLabel, new GridBagConstraintsHelper(0, 1, 2, 1).setInsets(0, 0, 8, 0).setAnchor(GridBagConstraints.WEST));
		formPanel.add(mapLabel, new GridBagConstraintsHelper(0, 2, 2, 1).setInsets(0, 0, 0, 0).setAnchor(GridBagConstraints.WEST));

		SmButton removeRegistryButton = new SmButton("仅移除记录");
		removeRegistryButton.addActionListener(e -> deleteProject(false));
		SmButton deleteFilesButton = new SmButton("删除工程目录");
		deleteFilesButton.addActionListener(e -> deleteProject(true));
		SmButton closeButton = new SmButton("关闭");
		closeButton.addActionListener(e -> dispose());
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(removeRegistryButton);
		buttonPanel.add(deleteFilesButton);
		buttonPanel.add(closeButton);

		add(formPanel, new GridBagConstraintsHelper(0, 0).setInsets(8, 8, 4, 8).setWeight(1, 1).setFill(GridBagConstraints.BOTH));
		add(buttonPanel, new GridBagConstraintsHelper(0, 1).setInsets(0, 8, 8, 8).setAnchor(GridBagConstraints.EAST));
	}

	private void reloadProjects() {
		List<OneModelParameters> projects = projectService.listProjects();
		DefaultComboBoxModel<ProjectItem> model = new DefaultComboBoxModel<>();
		for (OneModelParameters project : projects) {
			model.addElement(new ProjectItem(project));
		}
		projectComboBox.setModel(model);
		if (model.getSize() > 0) {
			projectComboBox.setSelectedIndex(0);
		}
		refreshDetails();
	}

	private void refreshDetails() {
		ProjectItem selected = (ProjectItem) projectComboBox.getSelectedItem();
		if (selected == null) {
			folderLabel.setText("工程目录：-");
			mapLabel.setText("工程地图：-");
			return;
		}
		folderLabel.setText("工程目录：" + selected.parameters.getProjectFolder());
		mapLabel.setText("工程地图：" + selected.parameters.getProjectMapName());
	}

	private void deleteProject(boolean deleteFiles) {
		ProjectItem selected = (ProjectItem) projectComboBox.getSelectedItem();
		if (selected == null) {
			JOptionPane.showMessageDialog(this, "当前没有可删除的工程。", "提示", JOptionPane.WARNING_MESSAGE);
			return;
		}
		int confirm = JOptionPane.showConfirmDialog(this,
				(deleteFiles ? "将删除工程记录和工程目录：\n" : "将移除工程记录，保留工程目录：\n") + selected.parameters.getProjectName(),
				"确认删除", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
		if (confirm != JOptionPane.OK_OPTION) {
			return;
		}
		try {
			projectService.deleteProject(selected.parameters.getProjectId(), deleteFiles);
			JOptionPane.showMessageDialog(this,
					deleteFiles ? "已删除工程目录。" : "已移除工程记录。",
					"删除工程完成", JOptionPane.INFORMATION_MESSAGE);
			reloadProjects();
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, ex.getMessage(), "删除工程失败", JOptionPane.WARNING_MESSAGE);
		}
	}

	private static final class ProjectItem {
		private final OneModelParameters parameters;

		private ProjectItem(OneModelParameters parameters) {
			this.parameters = parameters;
		}

		@Override
		public String toString() {
			return parameters.getProjectName() + "  [" + parameters.getVoltageLevel() + "]";
		}
	}
}

