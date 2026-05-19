package com.aircas.onemodel.action;

import com.aircas.onemodel.dialog.DialogOneModelTextResult;
import com.aircas.onemodel.service.OneModelScenePlanner;
import com.supermap.desktop.core.Interface.IBaseItem;

public class CtrlActionOmGenerateScene extends AbstractOmAction {

	public CtrlActionOmGenerateScene(IBaseItem caller) {
		super(caller);
	}

	@Override
	public void run() {
		showDialog(new DialogOneModelTextResult("生成三维站体", new OneModelScenePlanner().generatePlan()));
	}
}

