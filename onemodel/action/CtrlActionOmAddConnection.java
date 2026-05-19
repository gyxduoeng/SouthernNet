package com.aircas.onemodel.action;

import com.aircas.onemodel.dialog.DialogOmConnectionEditor;
import com.supermap.desktop.core.Interface.IBaseItem;

public class CtrlActionOmAddConnection extends AbstractOmAction {

	public CtrlActionOmAddConnection(IBaseItem caller) {
		super(caller);
	}

	@Override
	public void run() {
		showDialog(new DialogOmConnectionEditor());
	}
}

