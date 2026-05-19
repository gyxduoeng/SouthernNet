package com.aircas.gimpro.action;

import com.supermap.desktop.controls.ui.controls.SmDialog;
import com.supermap.desktop.core.Application;
import com.supermap.desktop.core.Interface.IBaseItem;
import com.supermap.desktop.core.implement.CtrlAction;

/**
 * GIM Pro 动作基类。
 */
public abstract class AbstractGpAction extends CtrlAction {

	protected AbstractGpAction(IBaseItem caller) {
		super(caller);
	}

	@Override
	public boolean enable() {
		return true;
	}

	protected void showDialog(SmDialog dialog) {
		dialog.showDialog();
	}

	protected void output(String message) {
		Application.getActiveApplication().getOutput().output(message);
	}
}

