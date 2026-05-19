package com.aircas.onemodel.action;

import com.aircas.onemodel.service.OneModelProjectService;
import com.supermap.desktop.core.Interface.IBaseItem;

import javax.swing.JOptionPane;
import java.lang.reflect.Method;

public class CtrlActionOmProjectClose extends AbstractOmAction {

	public CtrlActionOmProjectClose(IBaseItem caller) {
		super(caller);
	}

	@Override
	public void run() {
		if (new OneModelProjectService().getCurrentProject() == null) {
			JOptionPane.showMessageDialog(null, "当前没有已打开的工程。", "关闭工程", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		int confirm = JOptionPane.showConfirmDialog(null,
				"确认关闭当前工程吗？",
				"关闭工程", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
		if (confirm != JOptionPane.OK_OPTION) {
			return;
		}
		try {
			OneModelProjectService projectService = new OneModelProjectService();
			Method method = OneModelProjectService.class.getMethod("closeCurrentProject");
			method.invoke(projectService);
			JOptionPane.showMessageDialog(null, "当前工程已关闭。", "关闭工程", JOptionPane.INFORMATION_MESSAGE);
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(null, ex.getMessage(), "关闭工程失败", JOptionPane.WARNING_MESSAGE);
		}
	}
}


