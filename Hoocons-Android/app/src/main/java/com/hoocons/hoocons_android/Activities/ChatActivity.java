package com.hoocons.hoocons_android.Activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.text.Editable;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.hoocons.hoocons_android.Adapters.ChatMessagesAdapter;
import com.hoocons.hoocons_android.CustomUI.ChatWrapperKeyboard;
import com.hoocons.hoocons_android.ImplementedView.InternalImagesRecyclerView;
import com.hoocons.hoocons_android.CustomUI.xhs_common.Constants;
import com.hoocons.hoocons_android.CustomUI.xhs_common.SimpleCommonUtils;
import com.hoocons.hoocons_android.Helpers.AppConstant;
import com.hoocons.hoocons_android.Helpers.AppUtils;
import com.hoocons.hoocons_android.Helpers.ChatUtils;
import com.hoocons.hoocons_android.Interface.InfiniteScrollListener;
import com.hoocons.hoocons_android.Interface.OnChatMessageClickListener;
import com.hoocons.hoocons_android.Interface.OnStickerPagerFragmentInteractionListener;
import com.hoocons.hoocons_android.Managers.BaseApplication;
import com.hoocons.hoocons_android.Managers.SharedPreferencesManager;
import com.hoocons.hoocons_android.Models.ChatMessage;
import com.hoocons.hoocons_android.R;
import com.sj.emoji.EmojiBean;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import sj.keyboard.data.EmoticonEntity;
import sj.keyboard.interfaces.EmoticonClickListener;
import sj.keyboard.widget.EmoticonsEditText;
import sj.keyboard.widget.FuncLayout;

public class ChatActivity extends FragmentActivity
        implements View.OnClickListener,
        OnStickerPagerFragmentInteractionListener,
        OnChatMessageClickListener,
        FuncLayout.OnFuncKeyBoardListener {

    @BindView(R.id.user_state)
    TextView mUserState;
    @BindView(R.id.user_name)
    TextView mUserName;
    @BindView(R.id.message_list)
    RecyclerView mRecyclerView;

    @BindView(R.id.keyboard_wrapper)
    ChatWrapperKeyboard wrapperKeyboard;

    private int emotionHeight;
    private String chatRoomId;
    private List<ChatMessage> chatMessageList;
    private ChatMessagesAdapter messagesAdapter;
    private boolean isLoading = false;
    private DatabaseReference messageListDataRef;
    private int lastShownNamePos;
    private EmoticonClickListener emoticonClickListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        ButterKnife.bind(this);

        Intent intent = getIntent();
        chatRoomId = intent.getStringExtra("chatroom_uid");

        // Setting up
        lastShownNamePos = -1;
        messageListDataRef = BaseApplication.getInstance().getDatabase()
                .child("messages").child(chatRoomId);
        chatMessageList = new ArrayList<>();

        initCommonEmojiLayout();
        initRecyclerView();
        initDataChangeListener();
    }

    private void initCommonEmojiLayout() {
        emoticonClickListener = new EmoticonClickListener() {
            @Override
            public void onEmoticonClick(Object o, int actionType, boolean isDelBtn) {

                if (isDelBtn) {
                    SimpleCommonUtils.delClick(wrapperKeyboard.getEtChat());
                } else {
                    if (o == null) {
                        return;
                    }
                    if (actionType == Constants.EMOTICON_CLICK_BIGIMAGE) {
                        if (o instanceof EmoticonEntity) {
                            // OnSendImage(((EmoticonEntity)o).getIconUri());
                        }
                    } else {
                        String content = null;
                        if (o instanceof EmojiBean) {
                            content = ((EmojiBean) o).emoji;
                        } else if (o instanceof EmoticonEntity) {
                            content = ((EmoticonEntity) o).getContent();
                        }

                        if (TextUtils.isEmpty(content)) {
                            return;
                        }
                        int index = wrapperKeyboard.getEtChat().getSelectionStart();
                        Editable editable = wrapperKeyboard.getEtChat().getText();
                        editable.insert(index, content);
                    }
                }
            }
        };

        SimpleCommonUtils.initEmoticonsEditText(wrapperKeyboard.getEtChat());
        wrapperKeyboard.setAdapter(SimpleCommonUtils.getCommonAdapter(this, emoticonClickListener));
        wrapperKeyboard.addOnFuncKeyBoardListener(this);

        // Init images layout
        InternalImagesRecyclerView imagesRecyclerView = new InternalImagesRecyclerView(this);
        imagesRecyclerView.init(this);
        wrapperKeyboard.addFuncView(ChatWrapperKeyboard.FUNC_TYPE_IMAGE, imagesRecyclerView);
        wrapperKeyboard.getEtChat().setOnSizeChangedListener(new EmoticonsEditText.OnSizeChangedListener() {
            @Override
            public void onSizeChanged(int w, int h, int oldw, int oldh) {
                scrollToBottom();
            }
        });
        wrapperKeyboard.getBtnSend().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (wrapperKeyboard.getEtChat().getText().length() > 0) {
                    sendPlainTextMessage();
                    wrapperKeyboard.getEtChat().setText("");
                }
            }
        });
        wrapperKeyboard.getEmoticonsToolBarView().addFixedToolItemView(false, R.drawable.ic_line_plus, null, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ChatActivity.this, "ADD", Toast.LENGTH_SHORT).show();
            }
        });
        wrapperKeyboard.getEmoticonsToolBarView().addToolItemView(R.drawable.ic_setting_unselected, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ChatActivity.this, "SETTING", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void scrollToBottom() {
        mRecyclerView.requestLayout();
        mRecyclerView.post(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.smoothScrollToPosition(mRecyclerView.getBottom());
            }
        });
    }

    private void initDataChangeListener() {
        messageListDataRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                chatMessageList.clear();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    ChatMessage message = snapshot.getValue(ChatMessage.class);
                    assert message != null;
                    message.setId(snapshot.getKey());
                    message.setPosted(true);
                    chatMessageList.add(message);
                }

                messagesAdapter.notifyDataSetChanged();
                scrollToBottom();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        messageListDataRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                handleOnChildAdded(dataSnapshot.getKey());
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                handleOnChildUpdated(dataSnapshot.getKey(), dataSnapshot.getValue(ChatMessage.class));
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void handleOnChildAdded(String key) {
        for (int i = 0; i < chatMessageList.size(); i++) {
            ChatMessage m = chatMessageList.get(i);

            if (m.getId().equals(key)) {
                m.setPosted(true);
                messagesAdapter.notifyItemChanged(i);
                return;
            }
        }
    }

    private void handleOnChildUpdated(String key, ChatMessage newMessage) {
        for (int i = 0; i < chatMessageList.size(); i++) {
            ChatMessage m = chatMessageList.get(i);

            if (m.getId().equals(key)) {
                m = newMessage;
                messagesAdapter.notifyItemChanged(i);
                return;
            }
        }
    }

    private void initRecyclerView() {
        messagesAdapter = new ChatMessagesAdapter(this, chatMessageList, this);

        final LinearLayoutManager mLayoutManager = new LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false);
        ((SimpleItemAnimator) mRecyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
        mRecyclerView.setFocusable(false);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setHasFixedSize(false);
        mRecyclerView.setAdapter(messagesAdapter);
        mRecyclerView.addOnScrollListener(new InfiniteScrollListener(mLayoutManager) {

            @Override
            protected void loadMoreItems() {
            }

            @Override
            public int getTotalItems() {
                return messagesAdapter.getItemCount();
            }

            @Override
            public boolean isLastItem() {
                return ((LinearLayoutManager) mLayoutManager).findLastCompletelyVisibleItemPosition()
                      == (messagesAdapter.getItemCount() - 1);
            }

            @Override
            public boolean isLoading() {
                return isLoading;
            }
        });
    }

    private void sendPlainTextMessage() {
        ChatMessage message = new ChatMessage(
                SharedPreferencesManager.getDefault().getUserId(),
                AppConstant.MESSAGE_TYPE_TEXT,
                AppUtils.getCurrentUTCTime(),
                wrapperKeyboard.getEtChat().getText().toString(),
                null, false, null, null, null, false);
        ChatUtils.pushMessage(chatRoomId, message);

        // Add temporary message to the list
        message.setPosted(false);
        chatMessageList.add(message);
        wrapperKeyboard.getEtChat().setText("");

        scrollToBottom();
        messagesAdapter.notifyItemInserted(messagesAdapter.getItemCount());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_send:
                if (wrapperKeyboard.getEtChat().getText().length() > 0) {
                    sendPlainTextMessage();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    @Override
    public void onMessageClickListener(int position) {
        if (lastShownNamePos != -1) {
            ChatMessage oldMessage = chatMessageList.get(lastShownNamePos);
            boolean isShown = oldMessage.isShownName();
            oldMessage.setShownName(!isShown);
            messagesAdapter.notifyItemChanged(lastShownNamePos);
        }

        ChatMessage message = chatMessageList.get(position);
        boolean isShown = message.isShownName();
        message.setShownName(!isShown);
        lastShownNamePos = position;
        messagesAdapter.notifyItemChanged(position);
    }

    @Override
    public void onMessageLocationClickListener(int position) {

    }

    @Override
    public void onMessageImageClickListener(int position) {

    }

    @Override
    public void onMessageContactClickListener(int position) {

    }

    @Override
    public void OnFuncPop(int height) {

    }

    @Override
    public void OnFuncClose() {

    }
}
