package com.aircas.onemodel.dialog;

import com.aircas.onemodel.model.OneModelParameters;
import com.aircas.onemodel.service.OneModelProjectService;
import com.supermap.desktop.controls.ui.controls.SmDialog;
import com.supermap.desktop.controls.ui.controls.button.SmButton;
import com.supermap.desktop.core.ui.controls.GridBagConstraintsHelper;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

/**
 * 新建工程弹窗。
 */
public class DialogOmProjectCreate extends SmDialog {

	private final OneModelProjectService projectService = new OneModelProjectService();
	private final PanelOmProjectForm formPanel = new PanelOmProjectForm(null, true);

	public DialogOmProjectCreate() {
		setTitle("新建工程");
		setSize(new Dimension(760, 360));
		setLayout(new GridBagLayout());
		buildLayout();
	}

	private void buildLayout() {
		SmButton createButton = new SmButton("新建工程");
		createButton.addActionListener(e -> createProject());
		SmButton closeButton = new SmButton("关闭");
		closeButton.addActionListener(e -> dispose());
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(createButton);
		buttonPanel.add(closeButton);

		add(formPanel, new GridBagConstraintsHelper(0, 0).setInsets(8, 8, 4, 8).setWeight(1, 1).setFill(GridBagConstraints.BOTH));
		add(buttonPanel, new GridBagConstraintsHelper(0, 1).setInsets(0, 8, 8, 8).setAnchor(GridBagConstraints.EAST));
	}

	private void createProject() {
		try {
			OneModelParameters created = projectService.createProject(formPanel.buildDraft(null));
			JOptionPane.showMessageDialog(this,
					"已创建工程：" + created.getProjectName(),
					"新建工程成功", JOptionPane.INFORMATION_MESSAGE);
			dispose();
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, ex.getMessage(), "新建工程失败", JOptionPane.WARNING_MESSAGE);
		}
	}
}

