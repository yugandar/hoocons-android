package com.hoocons.hoocons_android.Activities;

import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.method.KeyListener;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.hoocons.hoocons_android.Helpers.SystemUtils;
import com.hoocons.hoocons_android.Managers.BaseActivity;
import com.hoocons.hoocons_android.Models.Emotion;
import com.hoocons.hoocons_android.R;
import com.hoocons.hoocons_android.SQLite.EmotionsDB;
import com.hoocons.hoocons_android.ViewFragments.EmotionFragment;

import org.aisen.android.common.utils.BitmapUtil;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ChatActivity extends BaseActivity implements View.OnClickListener,
        EmotionFragment.OnEmotionSelectedListener {
    @BindView(R.id.chatroom_send)
    ImageButton mSendButton;
    @BindView(R.id.chatroom_emoji)
    ImageButton mEmojiButton;
    @BindView(R.id.emo_container)
    View mEmoContainer;
    @BindView(R.id.chat_input_content)
    EditText mTextInput;
    @BindView(R.id.root_layout)
    ViewGroup mRootLayout;
    @BindView(R.id.root_container)
    View mRootContainer;

    private EmotionFragment emotionFragment;
    private int emotionHeight;
    private final LayoutTransition transitioner = new LayoutTransition();


    private final TextWatcher editContentWatcher = new TextWatcher() {
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (mTextInput.getText().toString().length() > 0) {
                mSendButton.setSelected(true);
            } else {
                mSendButton.setSelected(false);
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    private final View.OnKeyListener keyListener = new View.OnKeyListener() {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (keyCode == KeyEvent.KEYCODE_DEL) {
                Log.e(TAG, "onKey: DELETE");
                shouldRemoveLastIcon();
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        ButterKnife.bind(this);
        emotionFragment = new EmotionFragment();
        getSupportFragmentManager().beginTransaction().add(R.id.emo_container,
                emotionFragment, "EmotionFragment").commit();

        initEmotionLayout();
        initOnListener();
    }

    private void initEmotionLayout() {
        if (emotionFragment == null) {
            emotionFragment = EmotionFragment.newInstance();

            getSupportFragmentManager().beginTransaction().add(R.id.emo_container,
                    emotionFragment, "EmotionFragment").commit();
        }

        emotionFragment.setOnEmotionListener(this);

        ObjectAnimator animIn = ObjectAnimator.ofFloat(null, "translationY",
                SystemUtils.getScreenHeight(this), emotionHeight).
                setDuration(transitioner.getDuration(LayoutTransition.APPEARING));
        transitioner.setAnimator(LayoutTransition.APPEARING, animIn);

        ObjectAnimator animOut = ObjectAnimator.ofFloat(null, "translationY", emotionHeight,
                SystemUtils.getScreenHeight(this)).
                setDuration(transitioner.getDuration(LayoutTransition.DISAPPEARING));
        transitioner.setAnimator(LayoutTransition.DISAPPEARING, animOut);
        mRootLayout.setLayoutTransition(transitioner);

        mTextInput.setFilters(new InputFilter[] { emotionFilter });
        mTextInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideEmotionView(true);
            }
        });
    }

    private void initOnListener() {
        mSendButton.setOnClickListener(this);
        mEmojiButton.setOnClickListener(this);

        mTextInput.addTextChangedListener(editContentWatcher);
        mTextInput.setOnKeyListener(keyListener);
    }

    private void switchEmotionSoftInput() {
        if (mEmoContainer.isShown()) {
            hideEmotionView(true);
        } else {
            showEmotionView(SystemUtils.isKeyBoardShow(this));

        }
    }

    private void showEmotionView(boolean showAnimation) {
        mEmojiButton.setSelected(true);

        if (showAnimation) {
            transitioner.setDuration(200);
        } else {
            transitioner.setDuration(0);
        }

        int statusBarHeight = SystemUtils.getStatusBarHeight(ChatActivity.this);
        emotionHeight = SystemUtils.getKeyboardHeight(ChatActivity.this);

        SystemUtils.hideSoftInput(this, mTextInput);
        mEmoContainer.getLayoutParams().height = emotionHeight;
        mEmoContainer.setVisibility(View.VISIBLE);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        int lockHeight = SystemUtils.getAppContentHeight(this);
        lockContainerHeight(lockHeight);
    }

    private void lockContainerHeight(int paramInt) {
        LinearLayout.LayoutParams localLayoutParams = (LinearLayout.LayoutParams)
                mRootContainer.getLayoutParams();
        localLayoutParams.height = paramInt;
        localLayoutParams.weight = 0.0F;
    }

    private void hideEmotionView(boolean showKeyBoard) {
        mEmojiButton.setSelected(false);

        if (mEmoContainer.isShown()) {
            if (showKeyBoard) {
                LinearLayout.LayoutParams localLayoutParams = (LinearLayout.LayoutParams)
                        mRootContainer.getLayoutParams();

                localLayoutParams.height = mEmoContainer.getTop();
                localLayoutParams.weight = 0.0F;

                mEmoContainer.setVisibility(View.GONE);
                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

                SystemUtils.showKeyBoard(this, mTextInput);
                mTextInput.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        unlockContainerHeightDelayed();
                    }
                }, 200L);
            } else {
                mEmoContainer.setVisibility(View.GONE);
                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                unlockContainerHeightDelayed();
            }
        }
    }

    public void unlockContainerHeightDelayed() {
        ((LinearLayout.LayoutParams) mRootContainer.getLayoutParams()).weight = 1.0F;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.chatroom_emoji:
                switchEmotionSoftInput();
                break;
            default:
                break;
        }
    }

    @Override
    public void onEmotionSelected(Emotion emotion) {
        Editable editAble = mTextInput.getEditableText();
        int start = mTextInput.getSelectionStart();
        if ("[最右]".equals(emotion.getKey()))
            editAble.insert(start, "→_→");
        else
            editAble.insert(start, emotion.getKey());
    }

    private InputFilter emotionFilter = new InputFilter() {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            if ("".equals(source)) {
                return null;
            }

            byte[] emotionBytes = EmotionsDB.getEmotion(source.toString());
            if (emotionBytes != null) {
                byte[] data = emotionBytes;
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                int size = BaseActivity.getRunningActivity().getResources()
                        .getDimensionPixelSize(R.dimen.publish_emotion_size);
                bitmap = BitmapUtil.zoomBitmap(bitmap, size);
                SpannableString emotionSpanned = new SpannableString(source.toString());
                ImageSpan span = new ImageSpan(ChatActivity.this, bitmap);
                emotionSpanned.setSpan(span, 0, source.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                return emotionSpanned;
            } else {
                return source;
            }
        }
    };

    private void shouldRemoveLastIcon() {
        String text = mTextInput.getText().toString();
        int index = 0;
        if (text.charAt(text.length() - 1) == ']') {
            for(int i = text.length() - 1; i > 0 ; i--) {
                if (text.charAt(i) == '[') {
                    index = i;
                    break;
                }
            }
        } else {
            return;
        }

        String bracket = text.substring(index, text.length() - 1);
        byte[] emotionBytes = EmotionsDB.getEmotion(bracket);

        if (emotionBytes != null) {
            if (index > 0) {
                text = text.substring(0, index - 1);
            } else {
                text = "";
            }
            mTextInput.setText(text);
        }
    }

    @Override
    public void onBackPressed() {
        if (mEmoContainer.isShown()) {
            hideEmotionView(false);
        }

        super.onBackPressed();
    }
}
