package com.aircas.onemodel.dialog;

import com.aircas.onemodel.service.OneModelMapRepository;
import com.supermap.desktop.controls.ui.controls.SmDialog;
import com.supermap.desktop.controls.ui.controls.button.SmButton;
import com.supermap.desktop.core.ui.controls.GridBagConstraintsHelper;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

/**
 * 新增区域面。
 */
public class DialogOmAreaEditor extends SmDialog {

	private final OneModelMapRepository repository = new OneModelMapRepository();
	private final JTextField nameField = new JTextField();
	private final JTextField typeField = new JTextField("功能分区");
	private final JTextField minXField = new JTextField("113.0");
	private final JTextField minYField = new JTextField("22.0");
	private final JTextField maxXField = new JTextField("114.0");
	private final JTextField maxYField = new JTextField("23.0");

	public DialogOmAreaEditor() {
		setTitle("新增区域面");
		setSize(new Dimension(620, 340));
		setLayout(new GridBagLayout());
		buildLayout();
	}

	private void buildLayout() {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.add(new JLabel("区域名称"), new GridBagConstraintsHelper(0, 0).setInsets(0, 0, 6, 6).setAnchor(GridBagConstraints.WEST));
		panel.add(nameField, new GridBagConstraintsHelper(1, 0).setInsets(0, 0, 6, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
		panel.add(new JLabel("区域类型"), new GridBagConstraintsHelper(0, 1).setInsets(0, 0, 6, 6).setAnchor(GridBagConstraints.WEST));
		panel.add(typeField, new GridBagConstraintsHelper(1, 1).setInsets(0, 0, 6, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
		panel.add(new JLabel("最小经度 / Min X"), new GridBagConstraintsHelper(0, 2).setInsets(0, 0, 6, 6).setAnchor(GridBagConstraints.WEST));
		panel.add(minXField, new GridBagConstraintsHelper(1, 2).setInsets(0, 0, 6, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
		panel.add(new JLabel("最小纬度 / Min Y"), new GridBagConstraintsHelper(0, 3).setInsets(0, 0, 6, 6).setAnchor(GridBagConstraints.WEST));
		panel.add(minYField, new GridBagConstraintsHelper(1, 3).setInsets(0, 0, 6, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
		panel.add(new JLabel("最大经度 / Max X"), new GridBagConstraintsHelper(0, 4).setInsets(0, 0, 6, 6).setAnchor(GridBagConstraints.WEST));
		panel.add(maxXField, new GridBagConstraintsHelper(1, 4).setInsets(0, 0, 6, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
		panel.add(new JLabel("最大纬度 / Max Y"), new GridBagConstraintsHelper(0, 5).setInsets(0, 0, 6, 6).setAnchor(GridBagConstraints.WEST));
		panel.add(maxYField, new GridBagConstraintsHelper(1, 5).setInsets(0, 0, 6, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
        panel.add(new JLabel("当前工程地图使用地理坐标时，请输入经纬度范围：经度 -180~180，纬度 -90~90。"), new GridBagConstraintsHelper(1, 6).setInsets(0, 0, 6, 0).setAnchor(GridBagConstraints.WEST));

		SmButton okButton = new SmButton("写入地图");
		okButton.addActionListener(e -> saveAndClose());
		SmButton closeButton = new SmButton("关闭");
		closeButton.addActionListener(e -> dispose());
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(okButton);
		buttonPanel.add(closeButton);

		add(panel, new GridBagConstraintsHelper(0, 0).setInsets(8, 8, 4, 8).setWeight(1, 1).setFill(GridBagConstraints.BOTH));
		add(buttonPanel, new GridBagConstraintsHelper(0, 1).setInsets(0, 8, 8, 8).setAnchor(GridBagConstraints.EAST));
	}

	private void saveAndClose() {
		try {
			repository.addArea(nameField.getText().trim(), typeField.getText().trim(),
					Double.parseDouble(minXField.getText().trim()),
					Double.parseDouble(minYField.getText().trim()),
					Double.parseDouble(maxXField.getText().trim()),
					Double.parseDouble(maxYField.getText().trim()));
			dispose();
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, "请输入合法的区域名称和坐标范围。", "提示", JOptionPane.WARNING_MESSAGE);
		}
	}
}


