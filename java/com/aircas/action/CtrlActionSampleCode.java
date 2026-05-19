package com.aircas.action;

import com.supermap.desktop.core.Interface.IBaseItem;
import com.supermap.desktop.core.implement.CtrlAction;

/**
  * @version 1.0
 */
public class CtrlActionSampleCode extends CtrlAction {

	public CtrlActionSampleCode(IBaseItem caller) {
		super(caller);
	}

	@Override
	public void run() {
		new DialogSampleCode().showDialog();
	}

	@Override
	public boolean enable() {
		return true;
	}

	@Override
	public boolean check() {
		return super.check();
	}
}
