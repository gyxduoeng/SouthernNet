package com.aircas.onemodel.action;

import com.aircas.onemodel.dialog.DialogOmDrawConnectionLine;
import com.aircas.onemodel.dialog.DialogOneModelTextResult;
import com.aircas.onemodel.service.OneModelProjectService;
import com.supermap.desktop.core.Interface.IBaseItem;

public class CtrlActionOmDrawConnectionLine extends AbstractOmAction {

	public CtrlActionOmDrawConnectionLine(IBaseItem caller) {
		super(caller);
	}

	@Override
	public void run() {
		try {
			new OneModelProjectService().ensureCurrentProjectReady();
			showDialog(new DialogOmDrawConnectionLine());
		} catch (Exception ex) {
			showDialog(new DialogOneModelTextResult("绘制连接线", ex.getMessage()));
		}
	}
}