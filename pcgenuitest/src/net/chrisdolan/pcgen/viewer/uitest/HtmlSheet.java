package net.chrisdolan.pcgen.viewer.uitest;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.lang.Thread.UncaughtExceptionHandler;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import net.chrisdolan.pcgen.viewer.model.MockUIDelegate;
import net.chrisdolan.pcgen.viewer.model.Startup;
import pcgen.core.facade.CharacterFacade;
import pcgen.gui2.csheet.CharacterSheetPanel;
import pcgen.system.ConfigurationSettings;
import pcgen.system.PCGenTask;
import pcgen.system.PCGenTaskEvent;
import pcgen.system.PCGenTaskListener;

public class HtmlSheet {
	private final CharacterSheetPanel sheetPanel = new CharacterSheetPanel();
	private final JProgressBar progressBar = new JProgressBar();
	private final JLabel progressLabel = new JLabel();

	public HtmlSheet() {
		sheetPanel.setCharacterSheet(new File(ConfigurationSettings.getPreviewDir(), "d20/fantasy/Standard.htm"));
	}

	private HtmlSheet setCharacterFile(final File file) {
		Thread t = new Thread(new Runnable() {
			public void run() {
				MockUIDelegate delegate = new MockUIDelegate();

				PCGenTaskListener taskListener = new PCGenTaskListener() {
					private final long start = System.currentTimeMillis();
					public void progressChanged(PCGenTaskEvent event) {
						PCGenTask task = event.getSource();

						long now = System.currentTimeMillis();
						long ms = now - start;
						long min = ms / 60000;
						long sec = ms / 1000 - min*60;
						String time = "" + min + "m" + (sec < 10 ? "0" : "") + sec + "s"; 

						final int maximum = task.getMaximum();
						final int progress = task.getProgress();
						final String message = time + " " + task.getMessage();

						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								progressBar.setMaximum(maximum);
								progressBar.setValue(progress);
								progressLabel.setText(message);
							}
						});
					}
					public void errorOccurred(PCGenTaskEvent event) {
						// TODO Auto-generated method stub
					}
				};

				CharacterLoadHelper.Callback callback = new CharacterLoadHelper.Callback() {
					public void character(CharacterFacade character) {
						if (null == character) {
							// todo
						} else {
							sheetPanel.setCharacter(character);
						}
					}
				};

				new CharacterLoadHelper().load(file, delegate, taskListener, callback);
			}
		});
		t.setDaemon(true);
		t.start();
		return this;
	}

	public void show() {
		JFrame frame = new JFrame("HtmlSheet");
		frame.setPreferredSize(new Dimension(600, 800));
		frame.setLayout(new GridBagLayout());

		GridBagConstraints sheetConstraints = new GridBagConstraints();
		sheetConstraints.fill = GridBagConstraints.BOTH;
		sheetConstraints.gridx = 0;
		sheetConstraints.gridy = 0;
		sheetConstraints.gridwidth = 2;
		sheetConstraints.weightx = 1.0;
		sheetConstraints.weighty = 1.0;
		frame.add(sheetPanel, sheetConstraints);

		GridBagConstraints barConstraints = new GridBagConstraints();
		barConstraints.fill = GridBagConstraints.HORIZONTAL;
		barConstraints.gridx = 0;
		barConstraints.gridy = 1;
		frame.add(progressBar, barConstraints);

		GridBagConstraints labelConstraints = new GridBagConstraints();
		labelConstraints.fill = GridBagConstraints.HORIZONTAL;
		labelConstraints.gridx = 1;
		labelConstraints.gridy = 1;
		labelConstraints.insets = new Insets(0, 5, 0, 0);
//		progressLabel.setBorder(new LineBorder(Color.RED));
		frame.add(progressLabel, labelConstraints);

		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}

	public static void main(String[] args) {
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			public void uncaughtException(Thread t, Throwable e) {
				e.printStackTrace();
			}
		});

		File pcGenRoot = new File("/Users/chris/Work/pcgen-svn");
		Startup.setPCGenFileRoot(pcGenRoot);

		File pcgFile = new File("../pcgenviewer/assets/Hrig-5.17.pcg");
//		File pcgFile = new File(pcGenRoot, "characters/CodeMonkey.pcg");
//		File pcgFile = new File(pcGenRoot, "characters/Everything.pcg");
//		File pcgFile = new File(pcGenRoot, "characters/Sorcerer.pcg");
//		File pcgFile = new File(pcGenRoot, "characters/SpecialWizard.pcg");
		new HtmlSheet().setCharacterFile(pcgFile).show();
	}
}
