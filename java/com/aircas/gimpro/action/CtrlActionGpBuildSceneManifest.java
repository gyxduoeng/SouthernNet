package com.aircas.gimpro.action;

import com.aircas.gimpro.dialog.DialogGpTextResult;
import com.aircas.gimpro.service.GpSceneManifestService;
import com.supermap.desktop.core.Interface.IBaseItem;

/**
 * GIM Pro 场景清单生成入口。
 */
public class CtrlActionGpBuildSceneManifest extends AbstractGpAction {

	private final GpSceneManifestService manifestService = new GpSceneManifestService();

	public CtrlActionGpBuildSceneManifest(IBaseItem caller) {
		super(caller);
	}

	@Override
	public void run() {
		showDialog(new DialogGpTextResult("GIM Pro 场景清单", manifestService.buildAndWriteManifest()));
	}
}

