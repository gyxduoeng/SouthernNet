package com.aircas.onemodel.dialog;

import com.aircas.onemodel.model.OneModelParameters;
import com.aircas.onemodel.service.OmPathSupport;
import com.aircas.onemodel.service.OneModelProjectService;

/**
 * 兼容入口：保留类名，实际使用独立的“选择工程”弹窗。
 */
public class DialogOmProjectManager extends DialogOmProjectSelect {

	public DialogOmProjectManager() {
		super();
		setTitle("工程管理（兼容入口）");
	}
}
