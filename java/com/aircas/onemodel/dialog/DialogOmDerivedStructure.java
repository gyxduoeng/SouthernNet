package com.aircas.onemodel.dialog;

import com.aircas.onemodel.service.OneModelStructureBuilder;
import com.supermap.desktop.controls.ui.controls.SmDialog;
import com.supermap.desktop.controls.ui.controls.button.SmButton;
import com.supermap.desktop.core.ui.controls.GridBagConstraintsHelper;

import javax.swing.JSplitPane;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.JTextArea;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

/**
 * 派生对象树与关系结构弹窗。
 */
public class DialogOmDerivedStructure extends SmDialog {

	public DialogOmDerivedStructure() {
		OneModelStructureBuilder builder = new OneModelStructureBuilder();
		setTitle("派生对象树与关系结构");
		setSize(new Dimension(780, 520));
		setLayout(new GridBagLayout());
		JTree tree = new JTree(builder.buildTree());
		JTextArea summaryArea = new JTextArea(builder.buildSummary());
		summaryArea.setEditable(false);
		summaryArea.setLineWrap(true);
		summaryArea.setWrapStyleWord(true);
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(tree), new JScrollPane(summaryArea));
		splitPane.setResizeWeight(0.45);
		SmButton closeButton = new SmButton("关闭");
		closeButton.addActionListener(e -> dispose());
		add(splitPane, new GridBagConstraintsHelper(0, 0).setInsets(8, 8, 4, 8).setWeight(1, 1).setFill(GridBagConstraints.BOTH));
		add(closeButton, new GridBagConstraintsHelper(0, 1).setInsets(0, 8, 8, 8).setAnchor(GridBagConstraints.EAST));
	}
}

