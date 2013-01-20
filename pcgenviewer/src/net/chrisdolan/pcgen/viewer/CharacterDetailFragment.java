package net.chrisdolan.pcgen.viewer;

import java.io.IOException;

import net.chrisdolan.pcgen.viewer.model.CharacterContent;
import net.chrisdolan.pcgen.viewer.model.CharacterItem;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Shows a single .pcg character
 * @author chris
 */
public class CharacterDetailFragment extends Fragment {

    public static final String ARG_ITEM_ID = "item_id";

    CharacterItem mItem;

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
        View rootView = inflater.inflate(R.layout.fragment_character_detail, container, false);
        if (mItem != null) {
            ((TextView) rootView.findViewById(R.id.character_detail)).setText(mItem.toString());
        }
        return rootView;
    }
}
