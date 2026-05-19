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
import javax.swing.JTextField;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.List;

/**
 * 打开工程弹窗。
 */
public class DialogOmProjectSelect extends SmDialog {

	private final OneModelProjectService projectService = new OneModelProjectService();
	private final JComboBox<ProjectItem> projectComboBox = new JComboBox<>();
	private final JLabel currentProjectLabel = new JLabel("当前工程：未选择");
	private final JTextField stationNameField = createReadOnlyField();
	private final JTextField voltageField = createReadOnlyField();
	private final JTextField versionField = createReadOnlyField();
	private final JTextField coordinateSystemField = createReadOnlyField();
	private final JTextField projectMapField = createReadOnlyField();
	private final JTextField projectFolderField = createReadOnlyField();
	private final JTextField statusField = createReadOnlyField();

	public DialogOmProjectSelect() {
		setTitle("打开工程");
		setSize(new Dimension(760, 390));
		setLayout(new GridBagLayout());
		buildLayout();
		reloadProjects();
	}

	private void buildLayout() {
		projectComboBox.addActionListener(e -> refreshSummary());

		JPanel formPanel = new JPanel(new GridBagLayout());
		formPanel.add(currentProjectLabel, new GridBagConstraintsHelper(0, 0, 2, 1).setInsets(0, 0, 8, 0).setAnchor(GridBagConstraints.WEST));
		formPanel.add(new JLabel("工程"), new GridBagConstraintsHelper(0, 1).setInsets(0, 0, 8, 6).setAnchor(GridBagConstraints.WEST));
		formPanel.add(projectComboBox, new GridBagConstraintsHelper(1, 1).setInsets(0, 0, 8, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
		formPanel.add(new JLabel("电站名称"), new GridBagConstraintsHelper(0, 2).setInsets(0, 0, 8, 6).setAnchor(GridBagConstraints.WEST));
		formPanel.add(stationNameField, new GridBagConstraintsHelper(1, 2).setInsets(0, 0, 8, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
		formPanel.add(new JLabel("电压等级"), new GridBagConstraintsHelper(0, 3).setInsets(0, 0, 8, 6).setAnchor(GridBagConstraints.WEST));
		formPanel.add(voltageField, new GridBagConstraintsHelper(1, 3).setInsets(0, 0, 8, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
		formPanel.add(new JLabel("版本号"), new GridBagConstraintsHelper(0, 4).setInsets(0, 0, 8, 6).setAnchor(GridBagConstraints.WEST));
		formPanel.add(versionField, new GridBagConstraintsHelper(1, 4).setInsets(0, 0, 8, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
		formPanel.add(new JLabel("坐标系 EPSG"), new GridBagConstraintsHelper(0, 5).setInsets(0, 0, 8, 6).setAnchor(GridBagConstraints.WEST));
		formPanel.add(coordinateSystemField, new GridBagConstraintsHelper(1, 5).setInsets(0, 0, 8, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
		formPanel.add(new JLabel("工程地图"), new GridBagConstraintsHelper(0, 6).setInsets(0, 0, 8, 6).setAnchor(GridBagConstraints.WEST));
		formPanel.add(projectMapField, new GridBagConstraintsHelper(1, 6).setInsets(0, 0, 8, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
		formPanel.add(new JLabel("工程目录"), new GridBagConstraintsHelper(0, 7).setInsets(0, 0, 8, 6).setAnchor(GridBagConstraints.WEST));
		formPanel.add(projectFolderField, new GridBagConstraintsHelper(1, 7).setInsets(0, 0, 8, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
		formPanel.add(new JLabel("状态"), new GridBagConstraintsHelper(0, 8).setInsets(0, 0, 0, 6).setAnchor(GridBagConstraints.WEST));
		formPanel.add(statusField, new GridBagConstraintsHelper(1, 8).setInsets(0, 0, 0, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));

		SmButton openButton = new SmButton("打开工程");
		openButton.addActionListener(e -> openProject());
		SmButton deleteButton = new SmButton("删除所选工程");
		deleteButton.addActionListener(e -> deleteSelectedProject());
		SmButton refreshButton = new SmButton("刷新");
		refreshButton.addActionListener(e -> reloadProjects());
		SmButton closeButton = new SmButton("关闭");
		closeButton.addActionListener(e -> dispose());
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(openButton);
		buttonPanel.add(deleteButton);
		buttonPanel.add(refreshButton);
		buttonPanel.add(closeButton);

		add(formPanel, new GridBagConstraintsHelper(0, 0).setInsets(8, 8, 4, 8).setWeight(1, 1).setFill(GridBagConstraints.BOTH));
		add(buttonPanel, new GridBagConstraintsHelper(0, 1).setInsets(0, 8, 8, 8).setAnchor(GridBagConstraints.EAST));
	}

	private void reloadProjects() {
		List<OneModelParameters> projects = projectService.listProjects();
		OneModelParameters current = projectService.getCurrentProject();
		DefaultComboBoxModel<ProjectItem> model = new DefaultComboBoxModel<>();
		for (OneModelParameters project : projects) {
			model.addElement(new ProjectItem(project));
		}
		projectComboBox.setModel(model);
		currentProjectLabel.setText("当前工程：" + (current == null ? "未选择" : current.getProjectName()));
		if (current != null) {
			selectProjectInCombo(current.getProjectId());
		} else if (model.getSize() > 0) {
			projectComboBox.setSelectedIndex(0);
		}
		refreshSummary();
	}

	private void selectProjectInCombo(String projectId) {
		for (int i = 0; i < projectComboBox.getItemCount(); i++) {
			ProjectItem item = projectComboBox.getItemAt(i);
			if (item != null && projectId.equals(item.parameters.getProjectId())) {
				projectComboBox.setSelectedIndex(i);
				return;
			}
		}
	}

	private void refreshSummary() {
		ProjectItem selected = (ProjectItem) projectComboBox.getSelectedItem();
		if (selected == null) {
			stationNameField.setText("");
			voltageField.setText("");
			versionField.setText("");
			coordinateSystemField.setText("");
			projectMapField.setText("");
			projectFolderField.setText("");
			statusField.setText("暂无工程");
			return;
		}
		OneModelParameters parameters = selected.parameters;
		stationNameField.setText(parameters.getStationName());
		voltageField.setText(parameters.getVoltageLevel());
		versionField.setText(parameters.getVersionId());
		coordinateSystemField.setText(parameters.getCoordinateSystemCode());
		projectMapField.setText(parameters.getProjectMapName());
		projectFolderField.setText(parameters.getProjectFolder());
		statusField.setText(projectService.isCurrentProject(parameters.getProjectId()) ? "当前已打开" : "未打开");
	}

	private void openProject() {
		ProjectItem selected = (ProjectItem) projectComboBox.getSelectedItem();
		if (selected == null) {
			JOptionPane.showMessageDialog(this, "当前没有可选择的工程。", "提示", JOptionPane.WARNING_MESSAGE);
			return;
		}
		try {
			projectService.selectProject(selected.parameters.getProjectId());
			JOptionPane.showMessageDialog(this,
					"已打开工程：" + selected.parameters.getProjectName(),
					"打开工程成功", JOptionPane.INFORMATION_MESSAGE);
			dispose();
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, ex.getMessage(), "打开工程失败", JOptionPane.WARNING_MESSAGE);
		}
	}

	private void deleteSelectedProject() {
		ProjectItem selected = (ProjectItem) projectComboBox.getSelectedItem();
		if (selected == null) {
			JOptionPane.showMessageDialog(this, "当前没有可删除的工程。", "提示", JOptionPane.WARNING_MESSAGE);
			return;
		}
		Object[] options = {"仅移除记录", "删除工程目录", "取消"};
		int mode = JOptionPane.showOptionDialog(this,
				"将删除工程：\n" + selected.parameters.getProjectName(),
				"删除所选工程", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
				null, options, options[2]);
		if (mode == 2 || mode == JOptionPane.CLOSED_OPTION) {
			return;
		}
		if (mode == 1 && projectService.isCurrentProject(selected.parameters.getProjectId())) {
			JOptionPane.showMessageDialog(this,
					"当前已打开工程禁止删除工程目录，请先关闭工程后再删除目录。",
					"删除所选工程", JOptionPane.WARNING_MESSAGE);
			return;
		}
		try {
			projectService.deleteProject(selected.parameters.getProjectId(), mode == 1);
			JOptionPane.showMessageDialog(this,
					mode == 1 ? "已删除工程目录。" : "已移除工程记录。",
					"删除工程完成", JOptionPane.INFORMATION_MESSAGE);
			reloadProjects();
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, ex.getMessage(), "删除工程失败", JOptionPane.WARNING_MESSAGE);
		}
	}

	private JTextField createReadOnlyField() {
		JTextField field = new JTextField();
		field.setEditable(false);
		return field;
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

