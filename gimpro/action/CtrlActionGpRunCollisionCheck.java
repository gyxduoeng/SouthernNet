package com.aircas.gimpro.action;

import com.aircas.gimpro.dialog.DialogGpTextResult;
import com.aircas.gimpro.service.GpCollisionDetectionService;
import com.supermap.desktop.core.Interface.IBaseItem;

/**
 * GIM Pro 三维碰撞检测入口。
 */
public class CtrlActionGpRunCollisionCheck extends AbstractGpAction {

	private final GpCollisionDetectionService collisionDetectionService = new GpCollisionDetectionService();

	public CtrlActionGpRunCollisionCheck(IBaseItem caller) {
		super(caller);
	}

	@Override
	public void run() {
		showDialog(new DialogGpTextResult("GIM Pro 三维碰撞检测", collisionDetectionService.buildProblemListReport()));
	}
}

