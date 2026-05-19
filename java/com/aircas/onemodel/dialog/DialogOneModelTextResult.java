package com.aircas.onemodel.dialog;

import com.supermap.desktop.controls.ui.controls.SmDialog;
import com.supermap.desktop.controls.ui.controls.button.SmButton;
import com.supermap.desktop.core.ui.controls.GridBagConstraintsHelper;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

/**
 * 通用文本结果弹窗。
 */
public class DialogOneModelTextResult extends SmDialog {

	public DialogOneModelTextResult(String title, String content) {
		setTitle(title);
		setSize(new Dimension(760, 520));
		setLayout(new GridBagLayout());
		JTextArea textArea = new JTextArea(content == null ? "" : content);
		textArea.setEditable(false);
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		SmButton closeButton = new SmButton("关闭");
		closeButton.addActionListener(e -> dispose());
		add(new JScrollPane(textArea), new GridBagConstraintsHelper(0, 0).setInsets(8, 8, 4, 8).setWeight(1, 1).setFill(GridBagConstraints.BOTH));
		add(closeButton, new GridBagConstraintsHelper(0, 1).setInsets(0, 8, 8, 8).setAnchor(GridBagConstraints.EAST));
	}
}

