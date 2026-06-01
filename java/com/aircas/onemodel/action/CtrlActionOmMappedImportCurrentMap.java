package com.aircas.onemodel.action;

import com.aircas.onemodel.dialog.DialogOmMappedImportCurrentMap;
import com.aircas.onemodel.dialog.DialogOneModelTextResult;
import com.supermap.desktop.core.Interface.IBaseItem;

public class CtrlActionOmMappedImportCurrentMap extends AbstractOmAction {

	public CtrlActionOmMappedImportCurrentMap(IBaseItem caller) {
		super(caller);
	}

	@Override
	public void run() {
		try {
			showDialog(new DialogOmMappedImportCurrentMap());
		} catch (Exception ex) {
			showDialog(new DialogOneModelTextResult("已有电网映射导入", ex.getMessage()));
		}
	}
}