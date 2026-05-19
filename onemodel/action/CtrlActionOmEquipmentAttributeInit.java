package com.aircas.onemodel.action;

import com.aircas.onemodel.dialog.DialogOmEquipmentAttributeInit;
import com.supermap.desktop.core.Interface.IBaseItem;

public class CtrlActionOmEquipmentAttributeInit extends AbstractOmAction {

	public CtrlActionOmEquipmentAttributeInit(IBaseItem caller) {
		super(caller);
	}

	@Override
	public void run() {
		showDialog(new DialogOmEquipmentAttributeInit());
	}
}

