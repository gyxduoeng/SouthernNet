package com.aircas.onemodel.action;

import com.aircas.onemodel.dialog.DialogOneModelTextResult;
import com.aircas.onemodel.service.OneModelMapBridge;
import com.aircas.onemodel.service.OneModelProjectService;
import com.supermap.desktop.core.Interface.IBaseItem;

public class CtrlActionOmFinishDrawEquipmentPoint extends AbstractOmAction {

	public CtrlActionOmFinishDrawEquipmentPoint(IBaseItem caller) {
		super(caller);
	}

	@Override
	public void run() {
		try {
			new OneModelProjectService().ensureCurrentProjectReady();
			if (!new OneModelMapBridge().endDrawEquipmentPoint()) {
				showDialog(new DialogOneModelTextResult("结束绘制", "请先打开工程地图。"));
				return;
			}
			output("已结束设备点绘制，并锁定当前编辑图层。");
		} catch (Exception ex) {
			showDialog(new DialogOneModelTextResult("结束绘制", ex.getMessage()));
		}
	}
}