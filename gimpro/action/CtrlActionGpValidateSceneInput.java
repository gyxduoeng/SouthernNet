package com.aircas.gimpro.action;

import com.aircas.gimpro.dialog.DialogGpTextResult;
import com.aircas.gimpro.service.GpSceneValidationService;
import com.supermap.desktop.core.Interface.IBaseItem;

/**
 * GIM Pro 三维输入校验入口。
 */
public class CtrlActionGpValidateSceneInput extends AbstractGpAction {

	private final GpSceneValidationService validationService = new GpSceneValidationService();

	public CtrlActionGpValidateSceneInput(IBaseItem caller) {
		super(caller);
	}

	@Override
	public void run() {
		showDialog(new DialogGpTextResult("GIM Pro 三维输入校验", validationService.buildValidationReport()));
	}
}

