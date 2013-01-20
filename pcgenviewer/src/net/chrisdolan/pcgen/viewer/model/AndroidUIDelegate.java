package net.chrisdolan.pcgen.viewer.model;

import java.util.logging.Logger;

import pcgen.core.facade.CharacterFacade;
import pcgen.core.facade.ChooserFacade;
import pcgen.core.facade.UIDelegate;
import pcgen.system.PropertyContext;

final class AndroidUIDelegate implements UIDelegate {
	Logger logger = Logger.getLogger(AndroidUIDelegate.class.getName());
	@Override
	public boolean showWarningPrompt(String title, String message) {
		// TODO
		return false;
	}
	@Override
	public void showWarningMessage(String title, String message) {
		logger.warning(title + ": " + message);
	}
	@Override
	public boolean showWarningConfirm(String title, String message) {
		// TODO
		return false;
	}
	@Override
	public void showLevelUpInfo(CharacterFacade character, int oldLevel) {
		// TODO
	}
	@Override
	public String showInputDialog(String title, String message, String initialValue) {
		// TODO
		return null;
	}
	@Override
	public void showInfoMessage(String title, String message) {
		logger.info(title + ": " + message);
	}
	@Override
	public boolean showGeneralChooser(ChooserFacade chooserFacade) {
		// TODO
		return false;
	}
	@Override
	public void showErrorMessage(String title, String message) {
		logger.severe(title + ": " + message);
	}
	@Override
	public Boolean maybeShowWarningConfirm(String title, String message,
			String checkBoxText, PropertyContext context, String contextProp) {
		// TODO
		return false;
	}
}