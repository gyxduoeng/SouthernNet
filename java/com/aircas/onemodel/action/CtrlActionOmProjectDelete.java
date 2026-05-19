package com.aircas.onemodel.action;

import com.aircas.onemodel.dialog.DialogOmProjectDelete;
import com.supermap.desktop.core.Interface.IBaseItem;

public class CtrlActionOmProjectDelete extends AbstractOmAction {

	public CtrlActionOmProjectDelete(IBaseItem caller) {
		super(caller);
	}

	@Override
	public void run() {
		showDialog(new DialogOmProjectDelete());
	}
}

