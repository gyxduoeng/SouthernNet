package com.aircas.onemodel.action;

import com.aircas.onemodel.dialog.DialogOmDrawEquipmentPoint;
import com.aircas.onemodel.dialog.DialogOneModelTextResult;
import com.aircas.onemodel.service.OneModelProjectService;
import com.supermap.desktop.core.Interface.IBaseItem;

public class CtrlActionOmDrawEquipmentPoint extends AbstractOmAction {

	public CtrlActionOmDrawEquipmentPoint(IBaseItem caller) {
		super(caller);
	}

	@Override
	public void run() {
		try {
			new OneModelProjectService().ensureCurrentProjectReady();
			showDialog(new DialogOmDrawEquipmentPoint());
		} catch (Exception ex) {
			showDialog(new DialogOneModelTextResult("绘制设备点", ex.getMessage()));
		}
	}
}
