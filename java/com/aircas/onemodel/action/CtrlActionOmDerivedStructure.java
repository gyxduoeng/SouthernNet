package com.aircas.onemodel.action;

import com.aircas.onemodel.dialog.DialogOmDerivedStructure;
import com.supermap.desktop.core.Interface.IBaseItem;

public class CtrlActionOmDerivedStructure extends AbstractOmAction {

	public CtrlActionOmDerivedStructure(IBaseItem caller) {
		super(caller);
	}

	@Override
	public void run() {
		showDialog(new DialogOmDerivedStructure());
	}
}

