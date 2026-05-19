package com.aircas.onemodel.dialog;

import com.aircas.onemodel.model.OneModelParameters;
import com.aircas.onemodel.service.OmPathSupport;
import com.aircas.onemodel.service.OneModelProjectService;
import com.supermap.desktop.controls.ui.controls.button.SmButton;
import com.supermap.desktop.core.ui.controls.GridBagConstraintsHelper;

import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.File;
import java.nio.file.Path;

/**
 * 工程表单面板。
 */
public class PanelOmProjectForm extends JPanel {

	private final OmPathSupport pathSupport = new OmPathSupport();

	private final JTextField projectNameField = new JTextField();
	private final JTextField stationNameField = new JTextField();
	private final JComboBox<String> voltageCombo = new JComboBox<>(new String[]{"35kV", "110kV", "220kV", "500kV", "750kV"});
	private final JTextField versionField = new JTextField();
	private final JTextField modelLibraryField = new JTextField();
	private final JTextField projectFolderField = new JTextField();
	private final JTextField projectMapField = new JTextField();
	private final JTextField coordinateSystemField = new JTextField("4490");
	private final boolean projectFolderEditable;
	private String lastAutoFolder = "";
	private boolean internalFolderUpdate;

	public PanelOmProjectForm(OneModelParameters parameters, boolean projectFolderEditable) {
		this.projectFolderEditable = projectFolderEditable;
		setLayout(new GridBagLayout());
		buildLayout();
		bindProjectFolderSuggestion();
		fill(parameters);
	}

	private void buildLayout() {
		add(new JLabel("工程名称"), new GridBagConstraintsHelper(0, 0).setInsets(0, 0, 8, 6).setAnchor(GridBagConstraints.WEST));
		add(projectNameField, new GridBagConstraintsHelper(1, 0).setInsets(0, 0, 8, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));

		add(new JLabel("电站名称"), new GridBagConstraintsHelper(0, 1).setInsets(0, 0, 8, 6).setAnchor(GridBagConstraints.WEST));
		add(stationNameField, new GridBagConstraintsHelper(1, 1).setInsets(0, 0, 8, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));

		add(new JLabel("电压等级"), new GridBagConstraintsHelper(0, 2).setInsets(0, 0, 8, 6).setAnchor(GridBagConstraints.WEST));
		add(voltageCombo, new GridBagConstraintsHelper(1, 2).setInsets(0, 0, 8, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));

		add(new JLabel("版本号"), new GridBagConstraintsHelper(0, 3).setInsets(0, 0, 8, 6).setAnchor(GridBagConstraints.WEST));
		add(versionField, new GridBagConstraintsHelper(1, 3).setInsets(0, 0, 8, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));

		add(new JLabel("模型库目录"), new GridBagConstraintsHelper(0, 4).setInsets(0, 0, 8, 6).setAnchor(GridBagConstraints.WEST));
		add(modelLibraryField, new GridBagConstraintsHelper(1, 4).setInsets(0, 0, 8, 6).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
		SmButton browseModelButton = new SmButton("选择目录");
		browseModelButton.addActionListener(e -> chooseDirectory(modelLibraryField));
		add(browseModelButton, new GridBagConstraintsHelper(2, 4).setInsets(0, 0, 8, 0));

		add(new JLabel("工程目录"), new GridBagConstraintsHelper(0, 5).setInsets(0, 0, 8, 6).setAnchor(GridBagConstraints.WEST));
		projectFolderField.setEditable(projectFolderEditable);
		add(projectFolderField, new GridBagConstraintsHelper(1, 5).setInsets(0, 0, 8, 6).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
		if (projectFolderEditable) {
			SmButton browseProjectButton = new SmButton("选择目录");
			browseProjectButton.addActionListener(e -> chooseProjectFolder());
			add(browseProjectButton, new GridBagConstraintsHelper(2, 5).setInsets(0, 0, 8, 0));
		}

		add(new JLabel("工程地图"), new GridBagConstraintsHelper(0, 6).setInsets(0, 0, 0, 6).setAnchor(GridBagConstraints.WEST));
		add(projectMapField, new GridBagConstraintsHelper(1, 6).setInsets(0, 0, 8, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));

		add(new JLabel("坐标系 EPSG"), new GridBagConstraintsHelper(0, 7).setInsets(0, 0, 0, 6).setAnchor(GridBagConstraints.WEST));
		coordinateSystemField.setEditable(false);
		add(coordinateSystemField, new GridBagConstraintsHelper(1, 7).setInsets(0, 0, 0, 6).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
		SmButton chooseCoordButton = new SmButton("选择坐标系");
		chooseCoordButton.addActionListener(e -> chooseCoordinateSystem());
		add(chooseCoordButton, new GridBagConstraintsHelper(2, 7).setInsets(0, 0, 0, 0));
	}

	public void fill(OneModelParameters parameters) {
		if (parameters == null) {
			projectNameField.setText("");
			stationNameField.setText("");
			voltageCombo.setSelectedItem("220kV");
			versionField.setText("V1");
			modelLibraryField.setText("");
			projectMapField.setText("工程地图");
			coordinateSystemField.setText("4490");
			setProjectFolderText(buildDefaultProjectFolder("").toString());
			return;
		}
		projectNameField.setText(parameters.getProjectName());
		stationNameField.setText(parameters.getStationName());
		voltageCombo.setSelectedItem(parameters.getVoltageLevel());
		versionField.setText(parameters.getVersionId());
		modelLibraryField.setText(parameters.getModelLibraryPath());
		setProjectFolderText(parameters.getProjectFolder());
		projectMapField.setText(parameters.getProjectMapName());
		coordinateSystemField.setText(parameters.getCoordinateSystemCode());
	}

	public OneModelProjectService.ProjectDraft buildDraft(String projectId) {
		Path folder = projectFolderEditable
				? resolveCreateProjectFolder(projectNameField.getText().trim(), projectFolderField.getText().trim())
				: new File(projectFolderField.getText().trim()).toPath().toAbsolutePath().normalize();
		setProjectFolderText(folder.toString());
		return new OneModelProjectService.ProjectDraft()
				.setProjectId(projectId)
				.setProjectName(projectNameField.getText().trim())
				.setStationName(stationNameField.getText().trim())
				.setVoltageLevel(String.valueOf(voltageCombo.getSelectedItem()))
				.setVersionId(versionField.getText().trim())
				.setModelLibraryPath(modelLibraryField.getText().trim())
				.setProjectFolder(folder.toString())
				.setProjectMapName(projectMapField.getText().trim())
				.setCoordinateSystemCode(coordinateSystemField.getText().trim());
	}

	private void chooseCoordinateSystem() {
		DialogOmCoordinateSystemSelect dialog = new DialogOmCoordinateSystemSelect(coordinateSystemField.getText().trim());
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);
		String selected = dialog.getSelectedCode();
		if (selected != null && !selected.trim().isEmpty()) {
			coordinateSystemField.setText(selected.trim());
		}
	}

	private void chooseDirectory(JTextField targetField) {
		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setAcceptAllFileFilterUsed(false);
		if (!targetField.getText().trim().isEmpty()) {
			chooser.setCurrentDirectory(new File(targetField.getText().trim()));
		}
		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			targetField.setText(chooser.getSelectedFile().getAbsolutePath());
		}
	}

	private void chooseProjectFolder() {
		if (projectFolderField.getText().trim().isEmpty()) {
			setProjectFolderText(buildDefaultProjectFolder(projectNameField.getText().trim()).toString());
		}
		chooseDirectory(projectFolderField);
		if (!projectFolderField.getText().trim().isEmpty()) {
			Path folder = resolveCreateProjectFolder(projectNameField.getText().trim(), projectFolderField.getText().trim());
			setProjectFolderText(folder.toString());
		}
	}

	private void bindProjectFolderSuggestion() {
		if (!projectFolderEditable) {
			return;
		}
		projectNameField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				refreshAutoFolder();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				refreshAutoFolder();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				refreshAutoFolder();
			}
		});
	}

	private void refreshAutoFolder() {
		if (internalFolderUpdate) {
			return;
		}
		String current = projectFolderField.getText().trim();
		if (current.isEmpty() || current.equals(lastAutoFolder)) {
			setProjectFolderText(buildDefaultProjectFolder(projectNameField.getText().trim()).toString());
		}
	}

	private void setProjectFolderText(String text) {
		internalFolderUpdate = true;
		try {
			projectFolderField.setText(text == null ? "" : text);
			lastAutoFolder = projectFolderField.getText().trim();
		} finally {
			internalFolderUpdate = false;
		}
	}

	private Path resolveCreateProjectFolder(String projectName, String folderText) {
		String folderName = sanitizeFolderName(projectName == null || projectName.trim().isEmpty() ? "新建工程" : projectName.trim());
		if (folderText == null || folderText.trim().isEmpty()) {
			return buildDefaultProjectFolder(projectName);
		}
		Path chosen = new File(folderText.trim()).toPath().toAbsolutePath().normalize();
		Path fileName = chosen.getFileName();
		if (fileName != null && folderName.equals(fileName.toString())) {
			return chosen;
		}
		return chosen.resolve(folderName);
	}

	private Path buildDefaultProjectFolder(String projectName) {
		String name = projectName == null || projectName.trim().isEmpty() ? "新建工程" : projectName.trim();
		return pathSupport.resolveProjectRoot().resolve("data").resolve("projects").resolve(sanitizeFolderName(name));
	}

	private String sanitizeFolderName(String name) {
		return name.replaceAll("[\\:*?\"<>|/]+", "-").replaceAll("\\s+", "-");
	}
}

