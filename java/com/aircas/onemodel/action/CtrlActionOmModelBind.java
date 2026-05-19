package com.aircas.onemodel.action;

import com.aircas.onemodel.dialog.DialogOmModelBinding;
import com.supermap.desktop.core.Interface.IBaseItem;

public class CtrlActionOmModelBind extends AbstractOmAction {

	public CtrlActionOmModelBind(IBaseItem caller) {
		super(caller);
	}

	@Override
	public void run() {
		showDialog(new DialogOmModelBinding());
	}
}

