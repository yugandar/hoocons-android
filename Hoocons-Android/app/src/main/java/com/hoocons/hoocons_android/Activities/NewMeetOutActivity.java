package com.hoocons.hoocons_android.Activities;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.FragmentManager;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.beardedhen.androidbootstrap.BootstrapButton;
import com.facebook.rebound.BaseSpringSystem;
import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringSystem;
import com.facebook.rebound.SpringUtil;
import com.github.ksoichiro.android.observablescrollview.ObservableRecyclerView;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollView;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.github.ksoichiro.android.observablescrollview.ScrollState;
import com.github.ksoichiro.android.observablescrollview.ScrollUtils;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.hoocons.hoocons_android.Adapters.ImageLoaderAdapter;
import com.hoocons.hoocons_android.CustomUI.CustomFlowLayout;
import com.hoocons.hoocons_android.CustomUI.view.ViewHelper;
import com.hoocons.hoocons_android.Helpers.AppUtils;
import com.hoocons.hoocons_android.Managers.BaseActivity;
import com.hoocons.hoocons_android.Managers.SharedPreferencesManager;
import com.hoocons.hoocons_android.R;
import com.hoocons.hoocons_android.ViewHolders.SquaredImageViewHolder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.iwf.photopicker.PhotoPicker;
import xyz.klinker.giphy.Giphy;
import xyz.klinker.giphy.GiphyActivity;

public class NewMeetOutActivity extends BaseActivity implements
        ObservableScrollViewCallbacks, View.OnClickListener {
    @BindView(R.id.action_back)
    ImageButton mActionBack;
    @BindView(R.id.profile_img)
    ImageView mProfileImage;
    @BindView(R.id.obs_scrollview)
    ObservableScrollView mScrollView;
    @BindView(R.id.custom_toolbar)
    RelativeLayout mCustomToolbar;
    @BindView(R.id.header_area)
    RelativeLayout mHeaderArea;
    @BindView(R.id.custom_bar_text)
    TextView mCustomBarText;
    @BindView(R.id.linear)
    View mCustomBarLinear;
    @BindView(R.id.recycler)
    ObservableRecyclerView mImageRecycler;
    @BindView(R.id.add_image_action)
    ImageView mAddImageBtn;
    @BindView(R.id.meeting_time_view)
    LinearLayout mMeetingTimeView;
    @BindView(R.id.meeting_from_timestamp)
    TextView mFromDateTime;
    @BindView(R.id.meeting_to_timestamp)
    TextView mToDateTime;

    @BindView(R.id.submit_new_meeting)
    Button mSubmitMeeting;

    @BindView(R.id.meeting_tags_ed)
    EditText mAddTopicEdt;
    @BindView(R.id.add_topic_btn)
    BootstrapButton mAddTopicBtn;
    @BindView(R.id.topic_flow_layout)
    CustomFlowLayout mFlowLayout;

    private final int PHOTO_PICKER = 1;
    private List<String> topics;
    private ArrayList<String> mImagePaths;

    private View mOverlayView;
    private int mActionBarSize;
    private int mFlexibleSpaceImageHeight;

    private static final float MAX_TEXT_SCALE_DELTA = 0.3f;

    private final BaseSpringSystem mSpringSystem = SpringSystem.create();
    private final ImageSpringListener springListener = new ImageSpringListener();
    private Spring mScaleSpring;

    private DatePickerDialog mFromDatePicker;
    private TimePickerDialog mFromTimePicker;

    private DatePickerDialog mToDatePicker;
    private TimePickerDialog mToTimePicker;
    private ImageLoaderAdapter mImagesAdapter;

    private int fromYear = 0, fromMonth = 0, fromDate = 0;
    private int fromHour, fromMin;
    private int toYear = 0, toMonth = 0, toDate = 0;
    private int toHour, toMin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_meeting);
        ButterKnife.bind(this);

        topics = new ArrayList<>();
        mImagePaths = new ArrayList<>();

        initGeneralView();
        initView();
        initOnClickListener();
    }

    private void initOnClickListener() {
        mSubmitMeeting.setOnClickListener(this);
        mActionBack.setOnClickListener(this);

        mFromDateTime.setOnClickListener(this);
        mToDateTime.setOnClickListener(this);

        mAddTopicBtn.setOnClickListener(this);
        mAddImageBtn.setOnClickListener(this);
    }

    private void initGeneralView() {
        mFlexibleSpaceImageHeight = getResources().getDimensionPixelSize(R.dimen.flexible_space_image_height);
        mActionBarSize = 148;

        mCustomToolbar.bringToFront();
        mOverlayView = findViewById(R.id.overlay);

        mScrollView.setScrollViewCallbacks(this);
        setTitle(null);
        ScrollUtils.addOnGlobalLayoutListener(mScrollView, new Runnable() {
            @Override
            public void run() {
                mScrollView.scrollTo(mFlexibleSpaceImageHeight, 0);
            }
        });

        // Init submit button affect
        // Create the animation spring.
        mScaleSpring = mSpringSystem.createSpring();
        mScaleSpring.addListener(springListener);

        mSubmitMeeting.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mScaleSpring.setEndValue(0.3);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        mScaleSpring.setEndValue(0);
                        break;
                }
                return false;
            }
        });
    }

    private void initView() {
        // Load profile picture
        AppUtils.loadCircleImage(this, SharedPreferencesManager.getDefault().getUserProfileUrl(),
                mProfileImage);
    }

    private boolean doesHaveContent() {
        return false;
    }

    @Override
    public void onScrollChanged(int scrollY, boolean firstScroll, boolean dragging) {
        // Translate overlay and image
        float flexibleRange = mFlexibleSpaceImageHeight - mActionBarSize;
        int minOverlayTransitionY = mActionBarSize - mOverlayView.getHeight();
        ViewHelper.setTranslationY(mOverlayView, ScrollUtils.getFloat(-scrollY, minOverlayTransitionY, 0));
        ViewHelper.setTranslationY(mHeaderArea, ScrollUtils.getFloat(-scrollY / 2, minOverlayTransitionY, 0));

        // Change alpha of overlay
        ViewHelper.setAlpha(mOverlayView, ScrollUtils.getFloat((float) scrollY / flexibleRange, 0, 1));

        // Scale title text
        float scale = 1 + ScrollUtils.getFloat((flexibleRange - scrollY) / flexibleRange, 0, MAX_TEXT_SCALE_DELTA);

        Log.i(TAG, String.format("onScrollChanged: %s", String.valueOf(scrollY)));

        if (scrollY >= 480) {
            showCustomBar();
        } else {
            hideCustomBar();
        }
    }

    private void hideCustomBar() {
        mCustomBarText.setVisibility(View.GONE);
        mCustomBarLinear.setVisibility(View.GONE);
        mCustomToolbar.setBackgroundColor(getResources().getColor(R.color.transparent));
    }

    private void showCustomBar() {
        mCustomBarText.setVisibility(View.VISIBLE);
        mCustomBarLinear.setVisibility(View.VISIBLE);
        mCustomToolbar.setBackgroundColor(getResources().getColor(R.color.white));
    }

    private void showFromDateTimePicker() {
        Calendar now = Calendar.getInstance();
        final OnDateSetListener listener = new OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                fromYear = year;
                fromMonth = month;
                fromDate = dayOfMonth;
                mFromDateTime.setText(AppUtils.getCurrentTimeStringFromDateTime(fromYear,
                        fromMonth, fromDate, fromHour, fromMin));

                showFromTimePicker();
            }
        };

        if (fromYear == 0) {
            mFromDatePicker = new DatePickerDialog(this, listener, now.get(Calendar.YEAR),
                    now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
        } else {
            mFromDatePicker = new DatePickerDialog(this, listener, fromYear,
                    fromMonth, fromDate);
        }

        mFromDatePicker.show();
    }

    private void showFromTimePicker() {
        Calendar now = Calendar.getInstance();

        final TimePickerDialog.OnTimeSetListener listener = new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                fromHour = hourOfDay;
                fromMin = minute;

                mFromDateTime.setText(AppUtils.getCurrentTimeStringFromDateTime(fromYear,
                        fromMonth, fromDate, fromHour, fromMin));
            }
        };

        if (fromHour == 0 && fromMin == 0) {
            mFromTimePicker = new TimePickerDialog(this, listener, now.get(Calendar.HOUR_OF_DAY),
                    now.get(Calendar.MINUTE), true);
        } else {
            mFromTimePicker = new TimePickerDialog(this, listener, fromHour,
                    fromMin, true);
        }

        mFromTimePicker.show();
    }

    private void showToTimePicker() {
        Calendar now = Calendar.getInstance();

        final TimePickerDialog.OnTimeSetListener listener = new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                toHour = hourOfDay;
                toMin = minute;

                mToDateTime.setText(AppUtils.getCurrentTimeStringFromDateTime(toYear,
                        toMonth, toDate, toHour, toMin));
            }
        };

        if (toHour == 0 && toMin == 0) {
            mToTimePicker = new TimePickerDialog(this, listener, now.get(Calendar.HOUR_OF_DAY),
                    now.get(Calendar.MINUTE), true);
        } else {
            mToTimePicker = new TimePickerDialog(this, listener, toHour,
                    toMin, true);
        }

        mToTimePicker.show();
    }

    private void showToDateTimePicker() {
        Calendar now = Calendar.getInstance();
        final OnDateSetListener listener = new OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                toYear = year;
                toMonth = month;
                toDate = dayOfMonth;
                mToDateTime.setText(AppUtils.getCurrentTimeStringFromDateTime(toYear,
                        toMonth, toDate, toHour, toMin));

                showToTimePicker();
            }
        };

        if (toYear == 0) {
            mToDatePicker = new DatePickerDialog(this, listener, now.get(Calendar.YEAR),
                    now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
        } else {
            mToDatePicker = new DatePickerDialog(this, listener, toYear,
                    toMonth, toDate);
        }

        mToDatePicker.show();
    }

    private void addTopicView() {
        if (mAddTopicEdt.getText().length() > 0) {
            String topic = mAddTopicEdt.getText().toString();

            if (topics.contains(topic)) {
                Toast.makeText(this, getResources().getText(R.string.already_created),
                        Toast.LENGTH_SHORT).show();
            } else {
                topics.add(topic);
                initFlowLayoutView();
            }

            mAddTopicEdt.setText("");
        }
    }

    private void initFlowLayoutView() {
        mFlowLayout.removeAllViews();
        mFlowLayout.setVisibility(View.VISIBLE);

        for (int i = 0; i < topics.size(); i++) {
            final RelativeLayout item = (RelativeLayout) getLayoutInflater().inflate(R.layout.topic_flow_layout,
                    mFlowLayout, false);
            final TextView topic = (TextView) item.findViewById(R.id.topic_flow_text);
            final ImageView closeicon =
                    (ImageView) item.findViewById(R.id.topic_flow_close);

            topic.setText(topics.get(i));
            item.setTag(i);

            mFlowLayout.addView(item);
            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int i = (int) v.getTag();
                    item.setVisibility(View.GONE);
                    updateTags(i);
                    topics.remove(i);

                    if (topics.size() == 0) {
                        mFlowLayout.setVisibility(View.GONE);
                    }
                }
            });
        }
    }

    private void updateTags(int i) {
        for (i = i + 1; i < topics.size(); i++) {
            RelativeLayout flowChild = (RelativeLayout) mFlowLayout.getChildAt(i);
            int temp = (int) flowChild.getTag();
            flowChild.setTag(temp - 1);
        }
    }

    @Override
    public void onDownMotionEvent() {

    }

    @Override
    public void onUpOrCancelMotionEvent(ScrollState scrollState) {

    }

    private void finishActivity() {
        FragmentManager fm = getFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            Log.i("NewEventActivity", "popping backstack");
            fm.popBackStack();
            overridePendingTransition(R.anim.fix_anim, R.anim.slide_down_out);
        } else {
            Log.i("NewEventActivity", "nothing on backstack, calling super");
            super.onBackPressed();
        }
    }

    @Override
    public void onBackPressed(){
        if (doesHaveContent()) {

        } else {
            finishActivity();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == PHOTO_PICKER) {
                if (data != null){
                    final ArrayList<String> images =
                            data.getStringArrayListExtra(PhotoPicker.KEY_SELECTED_PHOTOS);

                    if (images.size() > 0) {
                        loadPickedImages(images);
                    }
                }
            } else {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    private void loadPickedImages(final ArrayList<String> imageList) {
        mImagePaths.clear();
        mImagePaths.addAll(imageList);
        mImagesAdapter = new ImageLoaderAdapter(this, imageList);

        mImageRecycler.setLayoutManager(new GridLayoutManager(this, 4, LinearLayoutManager.VERTICAL, false));
        mImageRecycler.setAdapter(mImagesAdapter);
        mImageRecycler.setItemAnimator(new DefaultItemAnimator());
        mImageRecycler.setNestedScrollingEnabled(false);
        mImageRecycler.setVisibility(View.VISIBLE);

        if (imageList.size() > 0) {
            mAddImageBtn.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.submit_new_meeting:
                break;
            case R.id.action_back:
                onBackPressed();
                break;
            case R.id.meeting_from_timestamp:
                showFromDateTimePicker();
                break;
            case R.id.meeting_to_timestamp:
                showToDateTimePicker();
                break;
            case R.id.add_topic_btn:
                addTopicView();
                break;
            case R.id.add_image_action:
                AppUtils.startImagePicker(this, 8, PHOTO_PICKER);
            default:
                break;
        }
    }

    private class ImageSpringListener extends SimpleSpringListener {
        @Override
        public void onSpringUpdate(Spring spring) {
            // On each update of the spring value, we adjust the scale of the image view to match the
            // springs new value. We use the SpringUtil linear interpolation function mapValueFromRangeToRange
            // to translate the spring's 0 to 1 scale to a 100% to 50% scale range and apply that to the View
            // with setScaleX/Y. Note that rendering is an implementation detail of the application and not
            // Rebound itself. If you need Gingerbread compatibility consider using NineOldAndroids to update
            // your view properties in a backwards compatible manner.
            float mappedValue = (float) SpringUtil.mapValueFromRangeToRange(spring.getCurrentValue(), 0, 1, 1, 0.5);
            mSubmitMeeting.setScaleX(mappedValue);
            mSubmitMeeting.setScaleY(mappedValue);
        }
    }
}