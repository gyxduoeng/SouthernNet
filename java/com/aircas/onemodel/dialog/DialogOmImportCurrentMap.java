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
 * 兼容保留：打开工程地图弹窗。
 */
public class DialogOmImportCurrentMap extends SmDialog {

	private final OneModelProjectService projectService = new OneModelProjectService();

	private final JLabel projectLabel = new JLabel();
	private final JLabel mapLabel = new JLabel();
	private final JLabel summaryLabel = new JLabel("该功能当前仅负责打开并激活工程指定地图。", JLabel.LEFT);

	public DialogOmImportCurrentMap() {
		setTitle("打开工程地图");
		setSize(new Dimension(640, 200));
		setLayout(new GridBagLayout());
		initContext();
		buildLayout();
	}

	private void initContext() {
		OneModelParameters project = projectService.getCurrentProject();
		projectLabel.setText("当前工程：" + (project == null ? "未选择" : project.getProjectName()));
		mapLabel.setText("工程地图：" + (project == null ? "未选择" : project.getProjectMapName()));
	}

	private void buildLayout() {
		JPanel formPanel = new JPanel(new GridBagLayout());
		formPanel.add(projectLabel, new GridBagConstraintsHelper(0, 0, 2, 1).setInsets(0, 0, 6, 0).setAnchor(GridBagConstraints.WEST));
		formPanel.add(mapLabel, new GridBagConstraintsHelper(0, 1, 2, 1).setInsets(0, 0, 10, 0).setAnchor(GridBagConstraints.WEST));
		formPanel.add(summaryLabel, new GridBagConstraintsHelper(0, 2, 2, 1).setInsets(0, 0, 0, 0).setAnchor(GridBagConstraints.WEST));

		SmButton openButton = new SmButton("打开工程地图");
		openButton.addActionListener(e -> openProjectMap());
		SmButton closeButton = new SmButton("关闭");
		closeButton.addActionListener(e -> dispose());

		JPanel buttonPanel = new JPanel();
		buttonPanel.add(openButton);
		buttonPanel.add(closeButton);

		add(formPanel, new GridBagConstraintsHelper(0, 0).setInsets(8, 8, 4, 8).setWeight(1, 1).setFill(GridBagConstraints.BOTH));
		add(buttonPanel, new GridBagConstraintsHelper(0, 1).setInsets(0, 8, 8, 8).setAnchor(GridBagConstraints.EAST));
	}

	private void openProjectMap() {
		try {
			projectService.ensureCurrentProjectReady();
			dispose();
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, ex.getMessage(), "打开工程地图失败", JOptionPane.WARNING_MESSAGE);
		}
	}
}


