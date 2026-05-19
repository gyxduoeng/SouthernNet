package com.aircas.onemodel.action;

import com.aircas.onemodel.dialog.DialogOmProjectEdit;
import com.aircas.onemodel.dialog.DialogOneModelTextResult;
import com.aircas.onemodel.service.OneModelProjectService;
import com.supermap.desktop.core.Interface.IBaseItem;

public class CtrlActionOmProjectEdit extends AbstractOmAction {

	public CtrlActionOmProjectEdit(IBaseItem caller) {
		super(caller);
	}

	@Override
	public void run() {
		if (!enable()) {
			return;
		}
		try {
			showDialog(new DialogOmProjectEdit());
		} catch (Exception ex) {
			showDialog(new DialogOneModelTextResult("更改工程", ex.getMessage()));
		}
	}

	@Override
	public boolean enable() {
		return new OneModelProjectService().getCurrentProject() != null;
	}
}

