package com.aircas.onemodel.dialog;

import com.aircas.onemodel.model.OneModelLayerOption;
import com.aircas.onemodel.service.OneModelActiveMapService;
import com.aircas.onemodel.service.OneModelCurrentMapImportService;
import com.aircas.onemodel.service.OneModelMapRepository;
import com.supermap.data.DatasetType;
import com.supermap.desktop.controls.ui.controls.SmDialog;
import com.supermap.desktop.controls.ui.controls.button.SmButton;
import com.supermap.desktop.core.ui.controls.GridBagConstraintsHelper;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * 将已加载的外部电网图层映射导入到 OneModel 标准图层。
 */
public class DialogOmMappedImportCurrentMap extends SmDialog {

	private static final String MODE_SKIP = "跳过";
	private static final String MODE_EXISTING = "导入已有图层";
	private static final String MODE_NEW = "新建设备图层";

	private final OneModelActiveMapService activeMapService = new OneModelActiveMapService();
	private final OneModelCurrentMapImportService importService = new OneModelCurrentMapImportService();
	private final OneModelMapRepository repository = new OneModelMapRepository();
	private final JComboBox<OneModelActiveMapService.DatasetRef> areaComboBox = new JComboBox<>();
	private final JComboBox<OneModelActiveMapService.DatasetRef> connectionComboBox = new JComboBox<>();
	private final JCheckBox clearBeforeImportCheckBox = new JCheckBox("导入前清空当前 OneModel 运行数据");
	private final List<EquipmentMappingRow> equipmentRows = new ArrayList<>();
	private List<OneModelLayerOption> targetLayers = new ArrayList<>();

	public DialogOmMappedImportCurrentMap() {
		setTitle("已有电网映射导入");
		setSize(new Dimension(980, 620));
		setLayout(new GridBagLayout());
		loadContext();
		buildLayout();
	}

	private void loadContext() {
		OneModelActiveMapService.SelectionSnapshot snapshot = activeMapService.loadSelectionSnapshot();
		targetLayers = repository.listEquipmentLayers();
		areaComboBox.setModel(new DefaultComboBoxModel<>(snapshot.getAreaOptions().toArray(new OneModelActiveMapService.DatasetRef[0])));
		connectionComboBox.setModel(new DefaultComboBoxModel<>(snapshot.getConnectionOptions().toArray(new OneModelActiveMapService.DatasetRef[0])));
		areaComboBox.setSelectedItem(snapshot.getDefaultArea());
		connectionComboBox.setSelectedItem(snapshot.getDefaultConnection());
		for (OneModelActiveMapService.DatasetRef ref : snapshot.getEquipmentOptions()) {
			if (ref != null && !ref.isNone() && DatasetType.POINT.equals(ref.getDatasetType())) {
				equipmentRows.add(new EquipmentMappingRow(ref, targetLayers));
			}
		}
	}

	private void buildLayout() {
		JPanel topPanel = new JPanel(new GridBagLayout());
		topPanel.add(new JLabel("区域面"), new GridBagConstraintsHelper(0, 0).setInsets(0, 0, 6, 6).setAnchor(GridBagConstraints.WEST));
		topPanel.add(areaComboBox, new GridBagConstraintsHelper(1, 0).setInsets(0, 0, 6, 12).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
		topPanel.add(new JLabel("连接线"), new GridBagConstraintsHelper(2, 0).setInsets(0, 0, 6, 6).setAnchor(GridBagConstraints.WEST));
		topPanel.add(connectionComboBox, new GridBagConstraintsHelper(3, 0).setInsets(0, 0, 6, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
		topPanel.add(clearBeforeImportCheckBox, new GridBagConstraintsHelper(0, 1, 4, 1).setInsets(0, 0, 0, 0).setAnchor(GridBagConstraints.WEST));

		JPanel rowsPanel = new JPanel(new GridBagLayout());
		rowsPanel.add(new JLabel("源设备点图层"), new GridBagConstraintsHelper(0, 0).setInsets(0, 0, 6, 8).setAnchor(GridBagConstraints.WEST));
		rowsPanel.add(new JLabel("导入方式"), new GridBagConstraintsHelper(1, 0).setInsets(0, 0, 6, 8).setAnchor(GridBagConstraints.WEST));
		rowsPanel.add(new JLabel("已有目标图层"), new GridBagConstraintsHelper(2, 0).setInsets(0, 0, 6, 8).setAnchor(GridBagConstraints.WEST));
		rowsPanel.add(new JLabel("新图层/设备类型"), new GridBagConstraintsHelper(3, 0).setInsets(0, 0, 6, 0).setAnchor(GridBagConstraints.WEST));
		if (equipmentRows.isEmpty()) {
			rowsPanel.add(new JLabel("未发现外部点图层。请先拖入 udbx 数据源，或将点数据集添加到工程地图。"),
					new GridBagConstraintsHelper(0, 1, 4, 1).setInsets(4, 0, 0, 0).setWeight(1, 1).setAnchor(GridBagConstraints.NORTHWEST));
		} else {
			for (int i = 0; i < equipmentRows.size(); i++) {
				equipmentRows.get(i).addTo(rowsPanel, i + 1);
			}
			rowsPanel.add(new JPanel(), new GridBagConstraintsHelper(0, equipmentRows.size() + 1, 4, 1).setWeight(1, 1).setFill(GridBagConstraints.BOTH));
		}

		SmButton importButton = new SmButton("开始导入");
		importButton.addActionListener(e -> importData());
		SmButton closeButton = new SmButton("关闭");
		closeButton.addActionListener(e -> dispose());
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(importButton);
		buttonPanel.add(closeButton);

		add(topPanel, new GridBagConstraintsHelper(0, 0).setInsets(8, 8, 4, 8).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
		add(new JScrollPane(rowsPanel), new GridBagConstraintsHelper(0, 1).setInsets(0, 8, 4, 8).setWeight(1, 1).setFill(GridBagConstraints.BOTH));
		add(buttonPanel, new GridBagConstraintsHelper(0, 2).setInsets(0, 8, 8, 8).setAnchor(GridBagConstraints.EAST));
	}

	private void importData() {
		try {
			List<OneModelCurrentMapImportService.EquipmentLayerMapping> mappings = new ArrayList<>();
			for (EquipmentMappingRow row : equipmentRows) {
				mappings.add(row.toMapping());
			}
			OneModelCurrentMapImportService.MappedImportRequest request = new OneModelCurrentMapImportService.MappedImportRequest(
					(OneModelActiveMapService.DatasetRef) areaComboBox.getSelectedItem(),
					mappings,
					(OneModelActiveMapService.DatasetRef) connectionComboBox.getSelectedItem(),
					clearBeforeImportCheckBox.isSelected());
			String report = importService.importCurrentMapWithMappings(request);
			DialogOneModelTextResult result = new DialogOneModelTextResult("已有电网映射导入", report);
			result.showDialog();
			dispose();
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, ex.getMessage(), "映射导入失败", JOptionPane.WARNING_MESSAGE);
		}
	}

	private static String deriveCategoryName(OneModelActiveMapService.DatasetRef ref) {
		String value = ref == null ? "" : ref.getDatasetName();
		value = value == null ? "" : value.trim();
		return value.isEmpty() ? "导入设备" : value.replace('_', ' ');
	}

	private static final class EquipmentMappingRow {

		private final OneModelActiveMapService.DatasetRef source;
		private final JLabel sourceLabel;
		private final JComboBox<String> modeComboBox = new JComboBox<>(new String[]{MODE_SKIP, MODE_EXISTING, MODE_NEW});
		private final JComboBox<OneModelLayerOption> targetLayerComboBox = new JComboBox<>();
		private final JTextField newCategoryField;

		private EquipmentMappingRow(OneModelActiveMapService.DatasetRef source, List<OneModelLayerOption> targetLayers) {
			this.source = source;
			this.sourceLabel = new JLabel(source.toString());
			this.targetLayerComboBox.setModel(new DefaultComboBoxModel<>(targetLayers.toArray(new OneModelLayerOption[0])));
			this.newCategoryField = new JTextField(deriveCategoryName(source));
			this.modeComboBox.setSelectedItem(MODE_NEW);
			this.modeComboBox.addActionListener(e -> refreshState());
			refreshState();
		}

		private void addTo(JPanel panel, int row) {
			panel.add(sourceLabel, new GridBagConstraintsHelper(0, row).setInsets(0, 0, 5, 8).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
			panel.add(modeComboBox, new GridBagConstraintsHelper(1, row).setInsets(0, 0, 5, 8).setFill(GridBagConstraints.HORIZONTAL));
			panel.add(targetLayerComboBox, new GridBagConstraintsHelper(2, row).setInsets(0, 0, 5, 8).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
			panel.add(newCategoryField, new GridBagConstraintsHelper(3, row).setInsets(0, 0, 5, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
		}

		private void refreshState() {
			String mode = String.valueOf(modeComboBox.getSelectedItem());
			boolean existing = MODE_EXISTING.equals(mode);
			boolean createNew = MODE_NEW.equals(mode);
			targetLayerComboBox.setEnabled(existing);
			newCategoryField.setEnabled(createNew || existing);
			if (existing && targetLayerComboBox.getItemCount() == 0) {
				modeComboBox.setSelectedItem(MODE_NEW);
			}
		}

		private OneModelCurrentMapImportService.EquipmentLayerMapping toMapping() {
			String mode = String.valueOf(modeComboBox.getSelectedItem());
			if (MODE_SKIP.equals(mode)) {
				return new OneModelCurrentMapImportService.EquipmentLayerMapping(source,
						OneModelCurrentMapImportService.EquipmentLayerMapping.TargetMode.SKIP, "", "");
			}
			if (MODE_EXISTING.equals(mode)) {
				OneModelLayerOption target = (OneModelLayerOption) targetLayerComboBox.getSelectedItem();
				String datasetName = target == null ? "" : target.getDatasetName();
				String category = target == null ? newCategoryField.getText() : target.getCaption();
				return new OneModelCurrentMapImportService.EquipmentLayerMapping(source,
						OneModelCurrentMapImportService.EquipmentLayerMapping.TargetMode.EXISTING_LAYER, datasetName, category);
			}
			return new OneModelCurrentMapImportService.EquipmentLayerMapping(source,
					OneModelCurrentMapImportService.EquipmentLayerMapping.TargetMode.NEW_LAYER, "", newCategoryField.getText());
		}
	}
}