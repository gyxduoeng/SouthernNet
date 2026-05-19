package com.aircas.onemodel.action;

import com.aircas.onemodel.dialog.DialogOneModelTextResult;
import com.aircas.onemodel.service.OneModelProjectService;
import com.supermap.desktop.core.Interface.IBaseItem;

public class CtrlActionOmImportCurrentMap extends AbstractOmAction {

	public CtrlActionOmImportCurrentMap(IBaseItem caller) {
		super(caller);
	}

	@Override
	public void run() {
		try {
			new OneModelProjectService().ensureCurrentProjectReady();
		} catch (Exception ex) {
			showDialog(new DialogOneModelTextResult("请先选择工程", ex.getMessage()));
		}
	}
}


