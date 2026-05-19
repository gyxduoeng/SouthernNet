package com.aircas.onemodel.action;

import com.aircas.onemodel.dialog.DialogOmEquipmentPointEditor;
import com.supermap.desktop.core.Interface.IBaseItem;

public class CtrlActionOmAddEquipmentPoint extends AbstractOmAction {

    public CtrlActionOmAddEquipmentPoint(IBaseItem caller) {
        super(caller);
    }

    @Override
    public void run() {
        showDialog(new DialogOmEquipmentPointEditor());
    }
}
