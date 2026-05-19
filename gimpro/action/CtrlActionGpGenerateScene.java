package com.aircas.gimpro.action;

import com.aircas.gimpro.dialog.DialogGpTextResult;
import com.aircas.gimpro.service.GpSceneGenerationService;
import com.supermap.desktop.core.Interface.IBaseItem;

/**
 * GIM Pro 三维场景生成入口。
 */
public class CtrlActionGpGenerateScene extends AbstractGpAction {

	private final GpSceneGenerationService generationService = new GpSceneGenerationService();

	public CtrlActionGpGenerateScene(IBaseItem caller) {
		super(caller);
	}

	@Override
	public void run() {
		showDialog(new DialogGpTextResult("GIM Pro 三维场景生成", generationService.generateAndWriteSceneReport()));
	}
}

