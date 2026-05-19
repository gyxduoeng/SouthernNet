package com.aircas.onemodel.action;

import com.aircas.onemodel.dialog.DialogOmAreaEditor;
import com.supermap.desktop.core.Interface.IBaseItem;

public class CtrlActionOmAddArea extends AbstractOmAction {

	public CtrlActionOmAddArea(IBaseItem caller) {
		super(caller);
	}

	@Override
	public void run() {
		showDialog(new DialogOmAreaEditor());
	}
}

