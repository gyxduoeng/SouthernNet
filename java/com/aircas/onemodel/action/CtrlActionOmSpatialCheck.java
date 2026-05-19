package com.aircas.onemodel.action;

import com.aircas.onemodel.dialog.DialogOneModelTextResult;
import com.aircas.onemodel.service.OneModelSpatialInspector;
import com.supermap.desktop.core.Interface.IBaseItem;

public class CtrlActionOmSpatialCheck extends AbstractOmAction {

	public CtrlActionOmSpatialCheck(IBaseItem caller) {
		super(caller);
	}

	@Override
	public void run() {
		showDialog(new DialogOneModelTextResult("空间范围 / 碰撞检查", new OneModelSpatialInspector().inspect()));
	}
}

