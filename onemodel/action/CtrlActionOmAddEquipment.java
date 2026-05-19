package com.aircas.onemodel.action;

import com.aircas.onemodel.dialog.DialogOmEquipmentEditor;
import com.supermap.desktop.core.Interface.IBaseItem;

public class CtrlActionOmAddEquipment extends AbstractOmAction {

	public CtrlActionOmAddEquipment(IBaseItem caller) {
		super(caller);
	}

	@Override
	public void run() {
		showDialog(new DialogOmEquipmentEditor());
	}
}

