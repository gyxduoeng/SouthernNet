package com.aircas.onemodel.action;

import com.aircas.onemodel.dialog.DialogOneModelTextResult;
import com.aircas.onemodel.model.OneModelParameters;
import com.aircas.onemodel.service.OneModelMapRepository;
import com.aircas.onemodel.service.OneModelProjectService;
import com.supermap.desktop.core.Interface.IBaseItem;

public class CtrlActionOmStatus extends AbstractOmAction {

	public CtrlActionOmStatus(IBaseItem caller) {
		super(caller);
	}

	@Override
	public void run() {
		OneModelMapRepository repository = new OneModelMapRepository();
		OneModelParameters current = new OneModelProjectService().getCurrentProject();
		StringBuilder builder = new StringBuilder();
		builder.append("OneModel 当前工程状态\n\n");
		if (current == null) {
			builder.append("当前未选择工程。\n");
			showDialog(new DialogOneModelTextResult("当前工程状态", builder.toString()));
			return;
		}
		builder.append("工程：").append(current.getProjectName()).append("\n");
		builder.append("工程地图：").append(current.getProjectMapName()).append("\n");
		builder.append("工作空间：").append(current.getWorkspaceFilePath()).append("\n\n");
		builder.append("区域数：").append(repository.listAreas().size()).append("\n");
		builder.append("设备数：").append(repository.listEquipments().size()).append("\n");
		builder.append("关联关系数：").append(repository.listConnections().size()).append("\n");
		showDialog(new DialogOneModelTextResult("当前工程状态", builder.toString()));
	}
}

