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
				+ "二维电站地图演示流程\n"
				+ "1. 工程管理：新建工程或打开已有工程，确认工程地图已经打开。\n"
				+ "2. 区域范围：通过输入坐标或手动绘制，建立站区范围。\n"
				+ "3. 设备图层：按设备类型创建设备图层，并选择推荐模型或自定义模型。\n"
				+ "4. 设备点：通过输入坐标或手动绘制补充设备点；手动绘制支持单点和连续绘制。\n"
				+ "5. 连接线：可选择两端设备自动连线，也可手动绘制连接线。\n"
				+ "6. 拓扑校正：检查孤立设备、未绑定连接线、重复连接、自连接和缺失引用。\n"
				+ "7. 保存后重新打开工程，确认区域、设备点、连接线和属性仍然存在。\n\n"
				+ "后续主线\n"
				+ "1. 图元绑定：确认设备点与图元类型的关系。\n"
				+ "2. 设备-模型绑定：确认设备对应的 GIM 模型资源。\n"
				+ "3. 派生结构：根据区域、设备和连接线生成结构树。\n"
				+ "4. 生成三维站体：在二维底座稳定后再进入三维生成。\n";
		showDialog(new DialogOneModelTextResult("帮助说明", content));
	}
}
