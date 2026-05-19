package com.aircas;

import com.supermap.desktop.core.*;

import java.util.Locale;
import java.util.ResourceBundle;

/**
  * @version 1.0
 * @description 获取词条
 */
public class DevelopProperties {

	private static final String DEVELOP = "Develop";

	public static String getString(String key) {
		return getString(DEVELOP, key);
	}

	private static String getString(String baseName, String key) {
		String result = "";

		ResourceBundle resourceBundle = ResourceBundle.getBundle(baseName, Locale.getDefault());
		if (resourceBundle != null) {
			try {
				result = resourceBundle.getString(key);
			} catch (Exception e) {
				Application.getActiveApplication().getOutput().output(e);
			}
		}
		return result;
	}
}
