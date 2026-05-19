package com.aircas.onemodel.dialog;

import com.supermap.desktop.controls.ui.controls.SmDialog;
import com.supermap.desktop.controls.ui.controls.button.SmButton;
import com.supermap.desktop.core.ui.controls.GridBagConstraintsHelper;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

/**
 * 坐标系选择弹窗。
 */
public class DialogOmCoordinateSystemSelect extends SmDialog {

	private static final CoordinateOption[] COMMON_OPTIONS = new CoordinateOption[]{
			new CoordinateOption("4490", "CGCS2000 / EPSG:4490"),
			new CoordinateOption("4326", "WGS84 / EPSG:4326"),
			new CoordinateOption("3857", "Web Mercator / EPSG:3857"),
			new CoordinateOption("4547", "CGCS2000 3 Degree GK CM 114E / EPSG:4547"),
			new CoordinateOption("4548", "CGCS2000 3 Degree GK CM 117E / EPSG:4548")
	};

	private final JComboBox<CoordinateOption> optionComboBox = new JComboBox<>();
	private final JTextField customCodeField = new JTextField();
	private String selectedCode = "";

	public DialogOmCoordinateSystemSelect(String currentCode) {
		setTitle("选择坐标系");
		setSize(new Dimension(520, 220));
		setLayout(new GridBagLayout());
		loadOptions(currentCode);
		buildLayout();
	}

	public String getSelectedCode() {
		return selectedCode;
	}

	private void loadOptions(String currentCode) {
		DefaultComboBoxModel<CoordinateOption> model = new DefaultComboBoxModel<>(COMMON_OPTIONS);
		optionComboBox.setModel(model);
		String normalized = currentCode == null || currentCode.trim().isEmpty() ? "4490" : currentCode.trim();
		customCodeField.setText(normalized);
		for (int i = 0; i < model.getSize(); i++) {
			CoordinateOption option = model.getElementAt(i);
			if (option != null && normalized.equals(option.code)) {
				optionComboBox.setSelectedIndex(i);
				return;
			}
		}
		optionComboBox.setSelectedIndex(0);
	}

	private void buildLayout() {
		optionComboBox.addActionListener(e -> syncCustomCode());

		JPanel formPanel = new JPanel(new GridBagLayout());
		formPanel.add(new JLabel("常用坐标系"), new GridBagConstraintsHelper(0, 0).setInsets(0, 0, 8, 6).setAnchor(GridBagConstraints.WEST));
		formPanel.add(optionComboBox, new GridBagConstraintsHelper(1, 0).setInsets(0, 0, 8, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
		formPanel.add(new JLabel("EPSG 代码"), new GridBagConstraintsHelper(0, 1).setInsets(0, 0, 0, 6).setAnchor(GridBagConstraints.WEST));
		formPanel.add(customCodeField, new GridBagConstraintsHelper(1, 1).setInsets(0, 0, 0, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));

		SmButton okButton = new SmButton("确定");
		okButton.addActionListener(e -> accept());
		SmButton closeButton = new SmButton("关闭");
		closeButton.addActionListener(e -> dispose());
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(okButton);
		buttonPanel.add(closeButton);

		add(formPanel, new GridBagConstraintsHelper(0, 0).setInsets(8, 8, 4, 8).setWeight(1, 1).setFill(GridBagConstraints.BOTH));
		add(buttonPanel, new GridBagConstraintsHelper(0, 1).setInsets(0, 8, 8, 8).setAnchor(GridBagConstraints.EAST));
	}

	private void syncCustomCode() {
		CoordinateOption option = (CoordinateOption) optionComboBox.getSelectedItem();
		if (option != null) {
			customCodeField.setText(option.code);
		}
	}

	private void accept() {
		selectedCode = customCodeField.getText() == null ? "" : customCodeField.getText().trim();
		dispose();
	}

	private static final class CoordinateOption {
		private final String code;
		private final String text;

		private CoordinateOption(String code, String text) {
			this.code = code;
			this.text = text;
		}

		@Override
		public String toString() {
			return text;
		}
	}
}

