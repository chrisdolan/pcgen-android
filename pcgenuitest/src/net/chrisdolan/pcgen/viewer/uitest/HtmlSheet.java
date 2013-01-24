package net.chrisdolan.pcgen.viewer.uitest;

import gmgen.pluginmgr.PluginManager;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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
import pcgen.core.facade.DataSetFacade;
import pcgen.core.facade.SourceSelectionFacade;
import pcgen.core.prereq.PrerequisiteTestFactory;
import pcgen.gui2.converter.TokenConverter;
import pcgen.gui2.csheet.CharacterSheetPanel;
import pcgen.io.ExportHandler;
import pcgen.persistence.CampaignFileLoader;
import pcgen.persistence.GameModeFileLoader;
import pcgen.persistence.PersistenceLayerException;
import pcgen.persistence.SourceFileLoader;
import pcgen.persistence.lst.TokenStore;
import pcgen.persistence.lst.output.prereq.PrerequisiteWriterFactory;
import pcgen.persistence.lst.prereq.PreParserFactory;
import pcgen.rules.persistence.TokenLibrary;
import pcgen.system.CharacterManager;
import pcgen.system.ConfigurationSettings;
import pcgen.system.PCGenTask;
import pcgen.system.PCGenTaskEvent;
import pcgen.system.PCGenTaskExecutor;
import pcgen.system.PCGenTaskListener;
import pcgen.system.PluginClassLoader;
import pcgen.util.Logging;
import pcgen.util.PJEP;

public class HtmlSheet {
	private final CharacterSheetPanel sheetPanel = new CharacterSheetPanel();
	private final JProgressBar progressBar = new JProgressBar();
	private final JLabel progressLabel = new JLabel();

	public HtmlSheet() {
		sheetPanel.setCharacterSheet(new File(ConfigurationSettings.getPreviewDir(), "d20/fantasy/Standard.htm"));
	}

	private void setCharacterFile(final File file) {
		Thread t = new Thread(new Runnable() {
			public void run() {
				PCGenTaskExecutor executor = new PCGenTaskExecutor();
				executor.addPCGenTaskListener(new PCGenTaskListener() {
					private final long start = System.currentTimeMillis();
					public void progressChanged(final PCGenTaskEvent event) {
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								long now = System.currentTimeMillis();
								long ms = now - start;
								long min = ms / 60000;
								long sec = ms / 1000 - min*60;
								String time = "" + min + "m" + (sec < 10 ? "0" : "") + sec + "s"; 
								PCGenTask task = event.getSource();
								progressBar.setMaximum(task.getMaximum());
								progressBar.setValue(task.getProgress());
								progressLabel.setText(time + " " + task.getMessage());
							}
						});
					}
					public void errorOccurred(PCGenTaskEvent event) {
						// TODO Auto-generated method stub
					}
				});

				executor.addPCGenTask(createLoadPluginTask());
				executor.addPCGenTask(new GameModeFileLoader());
				executor.addPCGenTask(new CampaignFileLoader());
				executor.addPCGenTask(new PCGenTask() {
					{
						setMaximum(100);
					}
					@Override
					public void execute() {
						MockUIDelegate delegate = new MockUIDelegate();
						setProgress("Find sources", 0);
						SourceSelectionFacade sources = CharacterManager.getRequiredSourcesForCharacter(file, delegate);
						if (sources == null)
							throw new NullPointerException("Could not find sources -- " + file);
						setProgress("Load sources", 10);
						SourceFileLoader sourceLoader = new SourceFileLoader(sources, delegate);
						sourceLoader.execute();
						setProgress("Get data set", 80);
						DataSetFacade data = sourceLoader.getDataSetFacade();
						if (data == null)
							throw new NullPointerException("data set facade is null");
						setProgress("Open character", 85);
						CharacterFacade character = CharacterManager.openCharacter(file, delegate, data);
						setProgress("Launch HTML", 100);
						sheetPanel.setCharacter(character);
					}
				});
				executor.execute();
			}
		});
		t.setDaemon(true);
		t.start();
	}

	private static PluginClassLoader createLoadPluginTask()
	{
		String pluginsDir = ConfigurationSettings.getPluginsDir();
		PluginClassLoader loader = new PluginClassLoader(new File(pluginsDir));
		loader.addPluginLoader(TokenLibrary.getInstance());
		loader.addPluginLoader(TokenStore.inst());
		try
		{
			loader.addPluginLoader(PreParserFactory.getInstance());
		}
		catch (PersistenceLayerException ex)
		{
			Logging.errorPrint("createLoadPluginTask failed", ex);
		}
		loader.addPluginLoader(PrerequisiteTestFactory.getInstance()); // not needed for render but it causes a ton of errors
		loader.addPluginLoader(PrerequisiteWriterFactory.getInstance()); // not needed for render
		loader.addPluginLoader(PJEP.getJepPluginLoader()); // not needed for render
		loader.addPluginLoader(ExportHandler.getPluginLoader()); // yes, needed
		loader.addPluginLoader(TokenConverter.getPluginLoader());
		loader.addPluginLoader(PluginManager.getInstance());
		return loader;
	}

	public void show() {
		JFrame frame = new JFrame("HtmlSheet");
		frame.setPreferredSize(new Dimension(600, 800));
		frame.setLayout(new GridBagLayout());

		GridBagConstraints sheetConstraints = new GridBagConstraints();
		sheetConstraints.fill = GridBagConstraints.BOTH;
		sheetConstraints.gridwidth = 2;
		sheetConstraints.weightx = 1.0;
		sheetConstraints.weighty = 1.0;
		frame.add(sheetPanel, sheetConstraints);

		GridBagConstraints barConstraints = new GridBagConstraints();
		barConstraints.fill = GridBagConstraints.HORIZONTAL;
		barConstraints.gridy = 1;
		barConstraints.weightx = 1.0;
		frame.add(progressBar, barConstraints);

		GridBagConstraints labelConstraints = new GridBagConstraints();
		labelConstraints.fill = GridBagConstraints.HORIZONTAL;
		labelConstraints.gridx = 1;
		labelConstraints.gridy = 1;
		labelConstraints.weightx = 1.0;
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

		Startup.setPCGenFileRoot(new File("/Users/chris/Work/pcgen-svn"));
		HtmlSheet htmlSheet = new HtmlSheet();
		htmlSheet.setCharacterFile(new File("../pcgenviewer/assets/Hrig-5.17.pcg"));
		htmlSheet.show();
	}
}
