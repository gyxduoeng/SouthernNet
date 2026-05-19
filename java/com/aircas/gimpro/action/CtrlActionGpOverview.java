package com.aircas.gimpro.action;

import com.aircas.gimpro.dialog.DialogGpTextResult;
import com.supermap.desktop.core.Interface.IBaseItem;

/**
 * GIM Pro 说明入口。
 */
public class CtrlActionGpOverview extends AbstractGpAction {

	public CtrlActionGpOverview(IBaseItem caller) {
		super(caller);
	}

	@Override
	public void run() {
		String content = "GIM Pro 基础说明\n\n"
				+ "1. GIM Pro 与 OM（OneModel）为两个独立的 iDesktopX 插件软件。\n"
				+ "2. GIM Pro 当前在共享工程成果基础上继续完成三维侧工作。\n"
				+ "3. 当前功能方向为：多源数据整合、三维场景生成、三维碰撞检测、轻量化能力接入。\n"
				+ "4. 三维场景生成第一版优先直读共享数据源，scene-plan.json 仅保留为兼容输入。\n"
				+ "5. 模型库调整与属性查看已并入三维场景生成流程。\n"
				+ "6. 三维碰撞检测第一版仅输出问题列表。\n"
				+ "7. 轻量化底层能力完全优先复用 iDesktopX 平台。\n";
		showDialog(new DialogGpTextResult("GIM Pro 说明", content));
	}
}

