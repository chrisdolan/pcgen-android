package net.chrisdolan.pcgen.viewer;

import java.io.IOException;
import java.util.Random;
import java.util.logging.Logger;

import net.chrisdolan.pcgen.viewer.model.CharacterContent;
import net.chrisdolan.pcgen.viewer.model.CharacterItem;
import pcgen.core.PlayerCharacter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Shows a single .pcg character
 * @author chris
 */
public class CharacterDetailFragment extends Fragment {
    private static final Logger logger = Logger.getLogger(CharacterDetailFragment.class.getName());

	public static final String ARG_ITEM_ID = "item_id";

	private CharacterItem mItem;
	private View rootView;
	private int loadNum = 0;
	private CharacterLoadTask currentTask;

	public CharacterDetailFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try {
			CharacterContent.initFromContext(getActivity().getApplicationContext());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (getArguments().containsKey(ARG_ITEM_ID)) {
			mItem = CharacterContent.ITEM_MAP.get(getArguments().getString(ARG_ITEM_ID));
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.fragment_character_detail, container, false);
		rootView.findViewById(R.id.reload_button).setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				reloadPC();
			}
		});
		loadPC();
		return rootView;
	}

	private void reloadPC() {
		if (mItem != null) {
			if (currentTask != null) {
				currentTask.cancel(false);
				currentTask = null;
			}
			mItem.character = null;
			mItem.html = null;
			loadPC();
		}
	}

	private void loadPC() {
		applyItem();
		if (mItem != null) {
			if (mItem.character == null && currentTask == null) {
				final int localLoadNum = ++loadNum;
				TextView charName = (TextView) rootView.findViewById(R.id.character_detail);
				View progressPanel = rootView.findViewById(R.id.progresspanel);
				final ProgressBar progress = (ProgressBar) rootView.findViewById(R.id.progress);
				final TextView progressMsg = (TextView) rootView.findViewById(R.id.progress_message);
				charName.setText(mItem.file.getName());
				progress.setProgress(0);
				progress.setMax(100);
				progressMsg.setText("Loading " + mItem.file.getName());
				progressPanel.setVisibility(View.VISIBLE);
				currentTask = new CharacterLoadTask(mItem.loader) {
					private final int rand = new Random().nextInt();
					@Override
					protected void onPostExecute(CharacterLoadTaskResult result) {
						if (localLoadNum != loadNum || isCancelled())
							return;
						mItem.character = result.pc;
						mItem.html = result.html;
						currentTask = null;
						applyItem();
					}
					@Override
					protected void onProgressUpdate(CharacterLoadTaskProgress... prog) {
						if (localLoadNum != loadNum || isCancelled())
							return;
						int newmax = prog[0].maximum;
						int newval = prog[0].value;
						int oldmax = progress.getMax();
						if (oldmax > newmax) {
							newval = (int) (0.5f + (float)newval * (float)oldmax / (float)newmax);
							newmax = oldmax;
						}
						if (oldmax != newmax)
							progress.setMax(newmax);
						progress.setProgress(newval);
						progressMsg.setText(prog[0].message);
						logger.warning("progress("+rand+"): " + newval + "/" + newmax + " = " + prog[0].message);
					}
				};
				currentTask.execute(mItem.file);
			}
		}
	}

	private void applyItem() {
		TextView charName = (TextView) rootView.findViewById(R.id.character_detail);
		WebView webView = (WebView) rootView.findViewById(R.id.html);
		//View progressPanel = rootView.findViewById(R.id.progresspanel);
		//progressPanel.setVisibility(View.GONE);
		charName.setText(mItem == null ? "<no character>" : mItem.character == null ? mItem.file.getName() : mItem.character.getName());
		String html = mItem == null ? "" : mItem.html == null ? "Loading..." : mItem.html;
		webView.loadData(html, "text/html", "UTF-8");
	}
}
