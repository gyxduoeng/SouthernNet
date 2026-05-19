package com.aircas.onemodel.action;

import com.aircas.onemodel.dialog.DialogOneModelTextResult;
import com.supermap.desktop.core.Interface.IBaseItem;

public class CtrlActionOmHelp extends AbstractOmAction {

	public CtrlActionOmHelp(IBaseItem caller) {
		super(caller);
	}

	@Override
	public void run() {
		String content = "OneModel 主线帮助\n\n"
				+ "1. 打开“工程管理”，新建或选择工程。\n"
				+ "2. 选择工程后，会自动加载工程工作空间并打开工程指定地图。\n"
				+ "3. “打开工程地图”用于重新打开或激活工程指定地图。\n"
				+ "4. 可先使用“区域范围 / 设备点 / 连接线”完成当前工程的基础图层准备。\n"
				+ "5. 之后依次执行“拓扑校正 / 图元绑定 / 设备-模型绑定 / 派生结构 / 生成三维站体 / 空间检查”。\n";
		showDialog(new DialogOneModelTextResult("帮助说明", content));
	}
}

