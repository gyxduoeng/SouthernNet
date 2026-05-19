package com.aircas.onemodel.action;

import com.aircas.onemodel.dialog.DialogOneModelTextResult;
import com.aircas.onemodel.service.OneModelTopologyChecker;
import com.supermap.desktop.core.Interface.IBaseItem;

public class CtrlActionOmTopologyCheck extends AbstractOmAction {

	public CtrlActionOmTopologyCheck(IBaseItem caller) {
		super(caller);
	}

	@Override
	public void run() {
		showDialog(new DialogOneModelTextResult("拓扑校正", new OneModelTopologyChecker().buildReport()));
	}
}

