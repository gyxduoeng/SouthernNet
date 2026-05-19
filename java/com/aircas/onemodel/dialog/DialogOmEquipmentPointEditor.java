package com.aircas.onemodel.dialog;

import com.aircas.onemodel.model.OneModelAreaRecord;
import com.aircas.onemodel.model.OneModelEquipmentRecord;
import com.aircas.onemodel.model.OneModelModelResource;
import com.aircas.onemodel.service.OneModelMapRepository;
import com.aircas.onemodel.service.OneModelModelScanner;
import com.supermap.desktop.controls.ui.controls.SmDialog;
import com.supermap.desktop.controls.ui.controls.button.SmButton;
import com.supermap.desktop.core.ui.controls.GridBagConstraintsHelper;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 新增设备点。
 */
public class DialogOmEquipmentPointEditor extends SmDialog {

    private final OneModelMapRepository repository = new OneModelMapRepository();
    private final OneModelModelScanner modelScanner = new OneModelModelScanner();
    private final JTextField equipmentNameField = new JTextField();
    private final JComboBox<String> equipmentTypeComboBox = new JComboBox<>();
    private final JComboBox<AreaOption> areaComboBox = new JComboBox<>();
    private final JComboBox<ModelOption> modelComboBox = new JComboBox<>();
    private final JComboBox<String> statusComboBox = new JComboBox<>(new String[]{"现状", "规划"});
    private final JTextField xField = new JTextField("0");
    private final JTextField yField = new JTextField("0");
    private final List<OneModelModelResource> resources = new ArrayList<>();
    private boolean updatingType;

    public DialogOmEquipmentPointEditor() {
        setTitle("新增设备点");
        setSize(new Dimension(620, 360));
        setLayout(new GridBagLayout());
        loadAreas();
        loadModels();
        buildLayout();
        bindTypeRefresh();
    }

    private void loadAreas() {
        DefaultComboBoxModel<AreaOption> model = new DefaultComboBoxModel<>();
        model.addElement(new AreaOption(null));
        try {
            for (OneModelAreaRecord area : repository.listAreas()) {
                model.addElement(new AreaOption(area));
            }
        } catch (Exception ignored) {
            // 没有区域时仍允许创建设备点。
        }
        areaComboBox.setModel(model);
    }

    private void loadModels() {
        resources.clear();
        resources.addAll(modelScanner.scanModels());
        Set<String> categories = new LinkedHashSet<>(modelScanner.scanModelCategories());
        DefaultComboBoxModel<String> typeModel = new DefaultComboBoxModel<>();
        for (String category : categories) {
            typeModel.addElement(category);
        }
        equipmentTypeComboBox.setEditable(true);
        equipmentTypeComboBox.setModel(typeModel);
        if (typeModel.getSize() > 0) {
            equipmentTypeComboBox.setSelectedIndex(0);
        }
        refreshModelOptions();
    }

    private void buildLayout() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.add(new JLabel("设备名称"), new GridBagConstraintsHelper(0, 0).setInsets(0, 0, 8, 6).setAnchor(GridBagConstraints.WEST));
        panel.add(equipmentNameField, new GridBagConstraintsHelper(1, 0).setInsets(0, 0, 8, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
        panel.add(new JLabel("设备类型"), new GridBagConstraintsHelper(0, 1).setInsets(0, 0, 8, 6).setAnchor(GridBagConstraints.WEST));
        panel.add(equipmentTypeComboBox, new GridBagConstraintsHelper(1, 1).setInsets(0, 0, 8, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
        panel.add(new JLabel("所属区域"), new GridBagConstraintsHelper(0, 2).setInsets(0, 0, 8, 6).setAnchor(GridBagConstraints.WEST));
        panel.add(areaComboBox, new GridBagConstraintsHelper(1, 2).setInsets(0, 0, 8, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
        panel.add(new JLabel("绑定模型"), new GridBagConstraintsHelper(0, 3).setInsets(0, 0, 8, 6).setAnchor(GridBagConstraints.WEST));
        panel.add(modelComboBox, new GridBagConstraintsHelper(1, 3).setInsets(0, 0, 8, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
        panel.add(new JLabel("状态"), new GridBagConstraintsHelper(0, 4).setInsets(0, 0, 8, 6).setAnchor(GridBagConstraints.WEST));
        panel.add(statusComboBox, new GridBagConstraintsHelper(1, 4).setInsets(0, 0, 8, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
        panel.add(new JLabel("X"), new GridBagConstraintsHelper(0, 5).setInsets(0, 0, 8, 6).setAnchor(GridBagConstraints.WEST));
        panel.add(xField, new GridBagConstraintsHelper(1, 5).setInsets(0, 0, 8, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));
        panel.add(new JLabel("Y"), new GridBagConstraintsHelper(0, 6).setInsets(0, 0, 0, 6).setAnchor(GridBagConstraints.WEST));
        panel.add(yField, new GridBagConstraintsHelper(1, 6).setInsets(0, 0, 0, 0).setWeight(1, 0).setFill(GridBagConstraints.HORIZONTAL));

        SmButton okButton = new SmButton("写入设备点");
        okButton.addActionListener(e -> saveAndClose());
        SmButton closeButton = new SmButton("关闭");
        closeButton.addActionListener(e -> dispose());
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(okButton);
        buttonPanel.add(closeButton);

        add(panel, new GridBagConstraintsHelper(0, 0).setInsets(8, 8, 4, 8).setWeight(1, 1).setFill(GridBagConstraints.BOTH));
        add(buttonPanel, new GridBagConstraintsHelper(0, 1).setInsets(0, 8, 8, 8).setAnchor(GridBagConstraints.EAST));
    }

    private void bindTypeRefresh() {
        equipmentTypeComboBox.addActionListener(e -> {
            if (!updatingType) {
                refreshModelOptions();
            }
        });
        if (equipmentTypeComboBox.isEditable()) {
            JTextField editor = (JTextField) equipmentTypeComboBox.getEditor().getEditorComponent();
            editor.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    refreshModelOptions();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    refreshModelOptions();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    refreshModelOptions();
                }
            });
        }
    }

    private void refreshModelOptions() {
        String type = getEquipmentTypeText();
        DefaultComboBoxModel<ModelOption> model = new DefaultComboBoxModel<>();
        model.addElement(new ModelOption(null));
        for (OneModelModelResource resource : resources) {
            if (type.isEmpty() || resource.getModelType().contains(type) || type.contains(resource.getModelType())) {
                model.addElement(new ModelOption(resource));
            }
        }
        modelComboBox.setModel(model);
    }

    private void saveAndClose() {
        String equipmentName = equipmentNameField.getText() == null ? "" : equipmentNameField.getText().trim();
        String equipmentType = getEquipmentTypeText();
        if (equipmentName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入设备名称。", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (equipmentType.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入设备类型。", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            AreaOption areaOption = (AreaOption) areaComboBox.getSelectedItem();
            ModelOption modelOption = (ModelOption) modelComboBox.getSelectedItem();
            OneModelEquipmentRecord record = repository.addEquipment(
                    areaOption == null ? "" : areaOption.getAreaId(),
                    equipmentName,
                    equipmentType,
                    modelOption == null ? null : modelOption.resource,
                    String.valueOf(statusComboBox.getSelectedItem()),
                    Double.parseDouble(xField.getText().trim()),
                    Double.parseDouble(yField.getText().trim()));
            JOptionPane.showMessageDialog(this, "已写入设备点：" + record.toString(), "新增设备点", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "请输入合法的 X/Y 坐标。", "提示", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "新增设备点失败", JOptionPane.WARNING_MESSAGE);
        }
    }

    private String getEquipmentTypeText() {
        Object editorItem = equipmentTypeComboBox.isEditable() ? equipmentTypeComboBox.getEditor().getItem() : equipmentTypeComboBox.getSelectedItem();
        return editorItem == null ? "" : String.valueOf(editorItem).trim();
    }

    private static final class AreaOption {
        private final OneModelAreaRecord area;

        private AreaOption(OneModelAreaRecord area) {
            this.area = area;
        }

        private String getAreaId() {
            return area == null ? "" : area.getAreaId();
        }

        @Override
        public String toString() {
            return area == null ? "不指定区域" : area.toString();
        }
    }

    private static final class ModelOption {
        private final OneModelModelResource resource;

        private ModelOption(OneModelModelResource resource) {
            this.resource = resource;
        }

        @Override
        public String toString() {
            return resource == null ? "不绑定模型" : resource.toString();
        }
    }
}
