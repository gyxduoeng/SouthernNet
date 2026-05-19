package com.aircas.action;


import com.supermap.data.*;
import com.supermap.desktop.controls.ui.controls.*;
import com.supermap.desktop.controls.ui.controls.button.*;
import com.supermap.desktop.controls.ui.controls.comboBox.*;
import com.supermap.desktop.controls.utilities.*;
import com.supermap.desktop.core.Interface.*;
import com.supermap.desktop.core.enums.*;
import com.supermap.desktop.core.ui.controls.GridBagConstraintsHelper;
import com.supermap.desktop.core.utilties.*;


import javax.swing.*;
import java.awt.*;
import java.awt.event.*;


/**
  * @version 1.0
 */
public class DialogSampleCode extends SmDialog {

	private JPanel panelSourceData;
	private JLabel labelSourceDatasource;
	private SmComboBoxDatasource comboBoxSourceDatasource;
	private JLabel labelSourceDatasetName;
	private SmComboBoxDataset smComboBoxSourceDataset;

	private SmButton buttonOK;
	private SmButton buttonCancel;


	public DialogSampleCode() {
		super();
		this.setSize(new Dimension(430, 160));
		initComponents();
		initComponentsStatus();
		initLayout();
		initResources();
		registerEvents();
		this.componentList.add(this.buttonOK);
		this.componentList.add(this.buttonCancel);
		this.setFocusTraversalPolicy(this.policy);
	}

	private void initComponents() {
		this.panelSourceData = new JPanel();
		this.labelSourceDatasource = new JLabel();
		this.comboBoxSourceDatasource = new SmComboBoxDatasource();
		this.labelSourceDatasetName = new JLabel();
		this.smComboBoxSourceDataset = new SmComboBoxDataset();

		this.buttonOK = ComponentFactory.createButtonOK();
		this.buttonCancel = ComponentFactory.createButtonCancel();
	}

	private void initComponentsStatus() {
		this.comboBoxSourceDatasource.setIncludeReadOnly(false);
		this.smComboBoxSourceDataset.setSupportedDatasetTypes(DatasetType.POINT);
		if (this.comboBoxSourceDatasource.getSelectedDatasource() != null) {
			this.smComboBoxSourceDataset.setDatasource(this.comboBoxSourceDatasource.getSelectedDatasource());
		}

		checkoutButtonOK();
	}

	private void initLayout() {
		initLayoutPanelSourceData();

		this.setLayout(new GridBagLayout());
		this.add(this.panelSourceData, new GridBagConstraintsHelper(0, 0, 1, 1).setInsets(GridBagConstraintsHelper.FRAME_CONTROL_GAP, GridBagConstraintsHelper.FRAME_CONTROL_GAP, 0, GridBagConstraintsHelper.FRAME_CONTROL_GAP).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));

		this.add(new JPanel(), new GridBagConstraintsHelper(0, 2, 1, 1).setInsets(0, GridBagConstraintsHelper.FRAME_CONTROL_GAP, 0, GridBagConstraintsHelper.FRAME_CONTROL_GAP).setFill(GridBagConstraints.BOTH).setWeight(1, 1));

		this.add(ComponentFactory.createButtonPanel(this.buttonOK, this.buttonCancel), new GridBagConstraintsHelper(0, 3, 1, 1).setInsets(0, GridBagConstraintsHelper.FRAME_CONTROL_GAP, GridBagConstraintsHelper.FRAME_CONTROL_GAP, GridBagConstraintsHelper.FRAME_CONTROL_GAP).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
	}

	private void initLayoutPanelSourceData() {
		this.panelSourceData.setLayout(new GridBagLayout());

		this.labelSourceDatasource.setPreferredSize(DefaultValues.getLabelDefaultSize());
		this.labelSourceDatasetName.setPreferredSize(DefaultValues.getLabelDefaultSize());

		this.panelSourceData.add(this.labelSourceDatasource, new GridBagConstraintsHelper(0, 0, 1, 1).setInsets(GridBagConstraintsHelper.CONTROLS_GAP, GridBagConstraintsHelper.CONTROLS_GAP, 0, 0));
		this.panelSourceData.add(this.comboBoxSourceDatasource, new GridBagConstraintsHelper(1, 0, 1, 1).setWeight(1, 0).setAnchor(GridBagConstraints.CENTER).setFill(GridBagConstraints.HORIZONTAL).setInsets(GridBagConstraintsHelper.CONTROLS_GAP, GridBagConstraintsHelper.CONTROLS_GAP, 0, GridBagConstraintsHelper.CONTROLS_GAP).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));

		this.panelSourceData.add(this.labelSourceDatasetName, new GridBagConstraintsHelper(0, 1, 1, 1).setInsets(GridBagConstraintsHelper.CONTROLS_GAP, GridBagConstraintsHelper.CONTROLS_GAP, 0, 0));
		this.panelSourceData.add(this.smComboBoxSourceDataset, new GridBagConstraintsHelper(1, 1, 1, 1).setWeight(1, 0).setAnchor(GridBagConstraints.CENTER).setFill(GridBagConstraints.HORIZONTAL).setInsets(GridBagConstraintsHelper.CONTROLS_GAP, GridBagConstraintsHelper.CONTROLS_GAP, 0, GridBagConstraintsHelper.CONTROLS_GAP).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
	}


	private void initResources() {
		this.setTitle("添加到新地图");
		this.labelSourceDatasource.setText("数据源:");
		this.labelSourceDatasetName.setText("数据集");
	}

	private void registerEvents() {
		this.comboBoxSourceDatasource.addItemListener(this.itemListenerDatasource);
		this.buttonOK.addActionListener(this.actionListenerOK);
		this.buttonCancel.addActionListener(this.actionListenerCancel);
	}

	private final ItemListener itemListenerDatasource = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				smComboBoxSourceDataset.setDatasource(comboBoxSourceDatasource.getSelectedDatasource());
				checkoutButtonOK();
			}
		}
	};

	private final ActionListener actionListenerOK = e -> {
		run();
		setDialogResult(DialogResult.OK);
		dispose();
	};

	private final ActionListener actionListenerCancel = e -> dispose();


	private void checkoutButtonOK() {
		this.buttonOK.setEnabled(this.smComboBoxSourceDataset.getSelectedDataset() != null);
	}

	private void run() {
		// 打开一个空白地图窗口
		IFormMap formMap = (IFormMap) FormUtilities.fireNewWindowEvent(WindowType.MAP);
		// 将选择的数据集添加到地图里
		MapViewUIUtilities.addDatasetsToMap(formMap.getMapControl().getMap(), 0, smComboBoxSourceDataset.getSelectedDataset());
	}
}
