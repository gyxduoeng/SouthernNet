package com.aircas.onemodel.action;

import com.aircas.onemodel.dialog.DialogOmProjectCreate;
import com.supermap.desktop.core.Interface.IBaseItem;

public class CtrlActionOmProjectCreate extends AbstractOmAction {

	public CtrlActionOmProjectCreate(IBaseItem caller) {
		super(caller);
	}

	@Override
	public void run() {
		showDialog(new DialogOmProjectCreate());
	}
}
