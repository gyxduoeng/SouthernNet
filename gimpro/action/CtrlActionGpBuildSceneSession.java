package com.aircas.gimpro.action;

import com.aircas.gimpro.dialog.DialogGpTextResult;
import com.aircas.gimpro.session.GpSceneSessionService;
import com.supermap.desktop.core.Interface.IBaseItem;

/**
 * GIM Pro 场景会话建立入口。
 */
public class CtrlActionGpBuildSceneSession extends AbstractGpAction {

	private final GpSceneSessionService sessionService = new GpSceneSessionService();

	public CtrlActionGpBuildSceneSession(IBaseItem caller) {
		super(caller);
	}

	@Override
	public void run() {
		showDialog(new DialogGpTextResult("GIM Pro 场景会话", sessionService.buildAndWriteSession()));
	}
}

