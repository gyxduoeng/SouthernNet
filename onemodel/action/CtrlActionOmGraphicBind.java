package com.aircas.onemodel.action;

import com.aircas.onemodel.dialog.DialogOmGraphicBinding;
import com.supermap.desktop.core.Interface.IBaseItem;

public class CtrlActionOmGraphicBind extends AbstractOmAction {

	public CtrlActionOmGraphicBind(IBaseItem caller) {
		super(caller);
	}

	@Override
	public void run() {
		showDialog(new DialogOmGraphicBinding());
	}
}

