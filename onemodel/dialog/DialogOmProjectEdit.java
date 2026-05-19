package com.aircas.onemodel.dialog;

import com.aircas.onemodel.model.OneModelParameters;
import com.aircas.onemodel.service.OneModelProjectService;
import com.supermap.desktop.controls.ui.controls.SmDialog;
import com.supermap.desktop.controls.ui.controls.button.SmButton;
import com.supermap.desktop.core.ui.controls.GridBagConstraintsHelper;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

/**
 * 更改工程弹窗。
 */
public class DialogOmProjectEdit extends SmDialog {

	private final OneModelProjectService projectService = new OneModelProjectService();
	private final OneModelParameters currentProject = projectService.getCurrentProject();
	private final PanelOmProjectForm formPanel = new PanelOmProjectForm(currentProject, false);
	private final JLabel projectLabel = new JLabel();

	public DialogOmProjectEdit() {
		setTitle("更改工程");
		setSize(new Dimension(760, 380));
		setLayout(new GridBagLayout());
		if (currentProject == null) {
			throw new IllegalStateException("请先选择工程，再执行更改工程。");
		}
		projectLabel.setText("当前工程：" + currentProject.getProjectName());
		buildLayout();
	}

	private void buildLayout() {
		JPanel contentPanel = new JPanel(new GridBagLayout());
		contentPanel.add(projectLabel, new GridBagConstraintsHelper(0, 0).setInsets(0, 0, 8, 0).setAnchor(GridBagConstraints.WEST));
		contentPanel.add(formPanel, new GridBagConstraintsHelper(0, 1).setWeight(1, 1).setFill(GridBagConstraints.BOTH));

		SmButton saveButton = new SmButton("保存更改");
		saveButton.addActionListener(e -> updateProject());
		SmButton closeButton = new SmButton("关闭");
		closeButton.addActionListener(e -> dispose());
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(saveButton);
		buttonPanel.add(closeButton);

		add(contentPanel, new GridBagConstraintsHelper(0, 0).setInsets(8, 8, 4, 8).setWeight(1, 1).setFill(GridBagConstraints.BOTH));
		add(buttonPanel, new GridBagConstraintsHelper(0, 1).setInsets(0, 8, 8, 8).setAnchor(GridBagConstraints.EAST));
	}

	private void updateProject() {
		try {
			OneModelParameters updated = projectService.updateProject(formPanel.buildDraft(currentProject.getProjectId()));
			JOptionPane.showMessageDialog(this,
					"已更新工程：" + updated.getProjectName(),
					"更改工程成功", JOptionPane.INFORMATION_MESSAGE);
			dispose();
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, ex.getMessage(), "更改工程失败", JOptionPane.WARNING_MESSAGE);
		}
	}
}

