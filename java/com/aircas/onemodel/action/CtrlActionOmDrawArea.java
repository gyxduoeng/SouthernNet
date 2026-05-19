package com.aircas.onemodel.action;

import com.aircas.onemodel.dialog.DialogOmDrawArea;
import com.aircas.onemodel.dialog.DialogOneModelTextResult;
import com.aircas.onemodel.service.OneModelProjectService;
import com.supermap.desktop.core.Interface.IBaseItem;

public class CtrlActionOmDrawArea extends AbstractOmAction {

	public CtrlActionOmDrawArea(IBaseItem caller) {
		super(caller);
	}

	@Override
	public void run() {
		try {
			new OneModelProjectService().ensureCurrentProjectReady();
			showDialog(new DialogOmDrawArea());
		} catch (Exception ex) {
			showDialog(new DialogOneModelTextResult("绘制区域", ex.getMessage()));
		}
	}
}
