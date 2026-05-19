package com.aircas.onemodel.action;

import com.aircas.onemodel.dialog.DialogOneModelTextResult;
import com.aircas.onemodel.model.OneModelParameters;
import com.aircas.onemodel.service.OneModelProjectService;
import com.supermap.desktop.core.Interface.IBaseItem;

public class CtrlActionOmInitMap extends AbstractOmAction {

	public CtrlActionOmInitMap(IBaseItem caller) {
		super(caller);
	}

	@Override
	public void run() {
		try {
			OneModelParameters project = new OneModelProjectService().ensureCurrentProjectReady();
			showDialog(new DialogOneModelTextResult("工程地图已准备",
					"已打开工程：" + project.getProjectName() + "\n"
							+ "工程地图：" + project.getProjectMapName() + "\n"
							+ "工作空间：" + project.getWorkspaceFilePath()));
		} catch (Exception ex) {
			showDialog(new DialogOneModelTextResult("工程地图准备失败", ex.getMessage()));
		}
	}
}

