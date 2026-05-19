package com.aircas.onemodel.action;

import com.aircas.onemodel.dialog.DialogOmProjectSelect;
import com.supermap.desktop.core.Interface.IBaseItem;

public class CtrlActionOmProjectSelect extends AbstractOmAction {

	public CtrlActionOmProjectSelect(IBaseItem caller) {
		super(caller);
	}

	@Override
	public void run() {
		showDialog(new DialogOmProjectSelect());
	}
}

