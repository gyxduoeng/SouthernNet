package com.aircas.onemodel.action;

import com.aircas.onemodel.dialog.DialogOmProjectSelect;
import com.supermap.desktop.core.Interface.IBaseItem;

/**
 * 兼容入口：默认打开“选择工程”。
 */
public class CtrlActionOmProjectManager extends AbstractOmAction {

	public CtrlActionOmProjectManager(IBaseItem caller) {
		super(caller);
	}

	@Override
	public void run() {
		showDialog(new DialogOmProjectSelect());
	}
}

