package com.gracecode.android.gojuon.ui.fragment;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import com.gracecode.android.gojuon.Characters;
import com.gracecode.android.gojuon.R;
import com.gracecode.android.gojuon.adapter.CharactersAdapter;
import com.gracecode.android.gojuon.common.Gojuon;
import com.gracecode.android.gojuon.ui.dialog.StrokeDialog;
import com.gracecode.android.gojuon.ui.widget.CharacterLayout;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class CharactersFragment extends Fragment {
    private static final int DEFAULT_COLUMN_NUM = 5;
    private static final String STAT_COLUMNS = "stat_colums";
    private static final String STAT_CHARACTERS = "stat_characters";
    private static final String STROKE_DIALOG_TAG = "stroke_dialog_tag";

    private String[][] mCharacters;
    private Gojuon mGojuon;
    private SharedPreferences mSharedPreferences;
    private int mColumns = DEFAULT_COLUMN_NUM;
    private GridView mGridView;
    private CharactersAdapter mCharactersAdapter;
    private StrokeDialog mStrokeDialog;

    AdapterView.OnItemClickListener mOnItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            try {
                // Pronounce the character
                Gojuon.pronounce(getActivity(), mCharacters[i][Characters.INDEX_ROUMAJI]);

                // Detect current screen orientation.
                // @see https://stackoverflow.com/questions/3663665/how-can-i-get-the-current-screen-orientation
                int orientation = mGojuon.getScreenOrientation();
                if (mSharedPreferences.getBoolean(Gojuon.KEY_AUTO_ROTATE, false)
                        && orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    Fragment fragment = new Fragment();
                    View layout = getActivity().findViewById(R.id.layout_item_character);

                    if (getCharacters() == Characters.MONOGRAPHS) {
                        fragment = new StrokeDialog(getActivity());
                        setStrokeDialog((StrokeDialog) fragment, i);
                        layout.setVisibility(View.GONE);
                    } else {
                        if (layout instanceof CharacterLayout) {
                            ((CharacterLayout) layout).autoAdjustTextSize();
                        }
                        mCharactersAdapter.fillCharacters(layout, i);
                        layout.setVisibility(View.VISIBLE);
                    }

                    getFragmentManager().beginTransaction()
                            .replace(R.id.layout_item_stoke, fragment)
                            .commit();
                }

                // Mark as selected.
                if (mSharedPreferences.getBoolean(Gojuon.KEY_HIGHLIGHT_SELECTED, true)) {
                    view.setSelected(true);
                }

            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
    };

    public String getPreferencedShowType() {
        return mSharedPreferences.getString(Gojuon.KEY_SHOW_CHARACTER_TYPE, CharactersAdapter.TYPE_SHOW_CHARACTER_HIRAGANA);
    }

    public boolean isShowKatakana() {
        return getPreferencedShowType().equals(CharactersAdapter.TYPE_SHOW_CHARACTER_KATAGANA) ? true : false;
    }

    private String getResourceNameByPosition(int position) {
        return String.format("%s%d", (position / 5 == 0) ? "" : (position / 5) + "", position % 5);
    }

    private String getStrokeResourceNameByPosition(int position) {
        return String.format("stroke/%s%sstroke.png",
                isShowKatakana() ? "k" : "h", getResourceNameByPosition(position));
    }

    private String getCharacterResourceNameByPosition(int position) {
        return String.format("stroke/%s%s.png",
                isShowKatakana() ? "k" : "h", getResourceNameByPosition(position));
    }

    private Drawable getDrawableFromAssets(String path) throws IOException {
        InputStream inputStream = getActivity().getAssets().open(path);
        return Drawable.createFromStream(inputStream, null);
    }


    private void setStrokeDialog(final StrokeDialog fragment, final int position) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    fragment
                            .setStrokeDrawable(
                                    getDrawableFromAssets(getStrokeResourceNameByPosition(position)));

                    fragment
                            .setCharacterDrawable(
                                    getDrawableFromAssets(getCharacterResourceNameByPosition(position)));

                } catch (IOException e) {
                    fragment.dismiss();
                }
            }
        }, 100);
    }

    private void setStrokeDialog(final int position) {
        setStrokeDialog(mStrokeDialog, position);
    }

    AdapterView.OnItemLongClickListener mOnItemLongClickListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
            final int position = i;
            setStrokeDialog(position);
            mStrokeDialog.show(getFragmentManager(), STROKE_DIALOG_TAG);
            return false;
        }
    };

    public CharactersFragment() {

    }

    public CharactersFragment(String[][] characters) {
        this.mCharacters = characters;
    }

    public CharactersFragment(String[][] characters, int columns) {
        this.mCharacters = characters;
        this.mColumns = columns;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mColumns = savedInstanceState.getInt(STAT_COLUMNS, DEFAULT_COLUMN_NUM);
            mCharacters = (String[][]) savedInstanceState.getSerializable(STAT_CHARACTERS);
        }

        mGojuon = Gojuon.getInstance();
        mSharedPreferences = mGojuon.getSharedPreferences();
        mStrokeDialog = new StrokeDialog(getActivity());
    }

    public CharactersAdapter getAdapter() {
        return mCharactersAdapter;
    }

    public String[][] getCharacters() {
        return mCharacters;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_characters, null);
    }

    @Override
    public void onStart() {
        super.onStart();
        mGridView = (GridView) getView().findViewById(R.id.list);
        mGridView.setNumColumns(mColumns);
        mGridView.setSoundEffectsEnabled(false);
        mGridView.setChoiceMode(GridView.CHOICE_MODE_SINGLE);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STAT_COLUMNS, mColumns);
        outState.putSerializable(STAT_CHARACTERS, mCharacters);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mCharacters != null && mCharacters.length > 0) {
            mCharactersAdapter = new CharactersAdapter(getActivity(), Arrays.asList(mCharacters));
            mCharactersAdapter.setShowType(getPreferencedShowType());

            mGridView.setAdapter(mCharactersAdapter);
            setOnItemClickListener(mOnItemClickListener);

            if (getCharacters() == Characters.MONOGRAPHS) {
                setOnItemLongClickListener(mOnItemLongClickListener);
            }
        }
    }

    public void setOnItemLongClickListener(AdapterView.OnItemLongClickListener listener) {
        mGridView.setOnItemLongClickListener(listener);
    }

    public void setOnItemClickListener(AdapterView.OnItemClickListener listener) {
        mGridView.setOnItemClickListener(listener);
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 0:
                    mGridView.requestFocusFromTouch();
                    break;
            }
        }
    };
}
