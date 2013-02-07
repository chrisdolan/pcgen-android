package net.chrisdolan.pcgen.viewer;

import java.io.File;

import net.chrisdolan.pcgen.viewer.model.AndroidUIDelegate;
import net.chrisdolan.pcgen.viewer.model.CharacterLoadHelper;
import net.chrisdolan.pcgen.viewer.model.CharacterLoadHelper.Options;
import net.chrisdolan.pcgen.viewer.model.CharacterLoadHelper.Result;

import pcgen.system.ConfigurationSettings;
import pcgen.system.PCGenTask;
import pcgen.system.PCGenTaskEvent;
import pcgen.system.PCGenTaskListener;
import android.os.AsyncTask;

public class CharacterLoadTask extends AsyncTask<File, CharacterLoadTaskProgress, CharacterLoadTaskResult> {
	private final CharacterLoadHelper loader;
	private final File htmlTemplate;

	public CharacterLoadTask(CharacterLoadHelper loader) {
		this(loader, new File(ConfigurationSettings.getPreviewDir(), "d20/fantasy/Standard.htm"));
	}
	public CharacterLoadTask(CharacterLoadHelper loader, File previewTemplate) {
		this.loader = loader;
		this.htmlTemplate = previewTemplate;
	}

	@Override
	protected CharacterLoadTaskResult doInBackground(File... pcgFiles) {
		if (pcgFiles.length != 1)
			throw new IllegalArgumentException();

		PCGenTaskListener taskListener = new PCGenTaskListener() {
			private final long start = System.currentTimeMillis();
			public void progressChanged(PCGenTaskEvent event) {
				PCGenTask task = event.getSource();

				long now = System.currentTimeMillis();
				long ms = now - start;
				long min = ms / 60000;
				long sec = ms / 1000 - min*60;
				String time = "" + min + "m" + (sec < 10 ? "0" : "") + sec + "s"; 

				CharacterLoadTaskProgress prog = new CharacterLoadTaskProgress();
				prog.maximum = task.getMaximum();
				prog.value = task.getProgress();
				prog.message = time + " " + task.getMessage();
				publishProgress(prog);
			}
			public void errorOccurred(PCGenTaskEvent event) {
				// TODO Auto-generated method stub
			}
		};

		Result result = loader.load(pcgFiles[0], new AndroidUIDelegate(), taskListener,
				new Options().setHtmlWanted(true).setHtmlTemplate(htmlTemplate));
		CharacterLoadTaskResult out = new CharacterLoadTaskResult();
		if (result != null) {
			out.pc = result.getPlayerCharacter();
			out.html = result.getHtml();
		}
		return out;
	}

}
