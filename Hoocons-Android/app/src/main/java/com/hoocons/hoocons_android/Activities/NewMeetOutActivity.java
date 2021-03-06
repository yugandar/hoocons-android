package com.hoocons.hoocons_android.Activities;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.FragmentManager;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
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

import com.afollestad.materialdialogs.MaterialDialog;
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
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.tasks.OnSuccessListener;
import com.hoocons.hoocons_android.Adapters.ImageLoaderAdapter;
import com.hoocons.hoocons_android.CustomUI.CustomFlowLayout;
import com.hoocons.hoocons_android.CustomUI.view.ViewHelper;
import com.hoocons.hoocons_android.Helpers.AppUtils;
import com.hoocons.hoocons_android.Helpers.MapUtils;
import com.hoocons.hoocons_android.Helpers.PermissionUtils;
import com.hoocons.hoocons_android.Managers.BaseActivity;
import com.hoocons.hoocons_android.Managers.BaseApplication;
import com.hoocons.hoocons_android.Managers.SharedPreferencesManager;
import com.hoocons.hoocons_android.R;
import com.hoocons.hoocons_android.Tasks.Jobs.NewMeetoutJob;
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
    @BindView(R.id.meeting_name)
    EditText mMeetingNameInput;
    @BindView(R.id.meeting_desc)
    EditText mMeetingDescInput;

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

    @BindView(R.id.meeting_location)
    TextView mMeetingLocation;
    @BindView(R.id.meeting_location_name)
    TextView mMeetingLocationName;
    @BindView(R.id.meeting_location_address)
    TextView mMeetingLocationAddress;
    @BindView(R.id.meeting_loc_details)
    LinearLayout mMeetingLocDetails;

    // Meeting Location
    private double meetingLong;
    private double meetingLat;
    private String meetingLocationName;
    private String meetingLocationAddress;

    private final int PHOTO_PICKER = 1;
    private final int PLACE_PICKER_REQUEST = 2;
    private final int REQUEST_LOCATION_PERMISSION = 3;

    private List<String> topics;
    private ArrayList<String> mImagePaths;

    private View mOverlayView;
    private int mActionBarSize;
    private int mFlexibleSpaceImageHeight;

    private static final float MAX_TEXT_SCALE_DELTA = 0.3f;

    private final BaseSpringSystem mSpringSystem = SpringSystem.create();
    private final ImageSpringListener springListener = new ImageSpringListener();
    private Spring mScaleSpring;

    private FusedLocationProviderClient mFusedLocationClient;

    private DatePickerDialog mFromDatePicker;
    private TimePickerDialog mFromTimePicker;

    private DatePickerDialog mToDatePicker;
    private TimePickerDialog mToTimePicker;
    private ImageLoaderAdapter mImagesAdapter;
    private Location lastKnownLocation;

    private int fromYear = 0, fromMonth = 0, fromDate = 0;
    private int fromHour, fromMin;
    private int toYear = 0, toMonth = 0, toDate = 0;
    private int toHour, toMin;

    private String fromDateTime;
    private String toDateTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_meeting);
        ButterKnife.bind(this);

        // init google location client
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        topics = new ArrayList<>();
        mImagePaths = new ArrayList<>();

        initGeneralView();
        initView();
        initOnClickListener();

        if (mightNeedLocationPermission()) {
            initLocationTracker();
        }
    }

    private boolean mightNeedLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            List<String> permissions = new ArrayList<>();
            permissions.add(android.Manifest.permission.ACCESS_COARSE_LOCATION);
            permissions.add(android.Manifest.permission.ACCESS_FINE_LOCATION);

            PermissionUtils.requestPermissions(this,
                    REQUEST_LOCATION_PERMISSION, permissions);
            return false;
        }

        return true;
    }

    private void initLocationTracker()  {
        if (mightNeedLocationPermission()) {
            try {
                mFusedLocationClient.getLastLocation()
                        .addOnSuccessListener(new OnSuccessListener<Location>() {
                            @Override
                            public void onSuccess(Location location) {
                                lastKnownLocation = location;
                            }
                        });
            } catch (SecurityException e) {

            }
        }
    }

    private void initOnClickListener() {
        mSubmitMeeting.setOnClickListener(this);
        mActionBack.setOnClickListener(this);

        mFromDateTime.setOnClickListener(this);
        mToDateTime.setOnClickListener(this);

        mAddTopicBtn.setOnClickListener(this);
        mAddImageBtn.setOnClickListener(this);

        mMeetingLocation.setOnClickListener(this);
        mMeetingLocDetails.setOnClickListener(this);
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
                mProfileImage, null);
    }

    private boolean doesHaveContent() {
        return false;
    }

    private void showMeetoutNameAlertDialog() {
        new MaterialDialog.Builder(this)
                .title(R.string.meetout_name_invalid_title)
                .content(R.string.meetout_name_invalid_error)
                .positiveText(R.string.dialog_ok)
                .icon(getResources().getDrawable(R.drawable.dialogs_warning))
                .show();
    }

    private boolean isValidName() {
        if (mMeetingNameInput.getText().length() == 0) {
            mMeetingNameInput.setError(getResources().getString(R.string.meetout_name_empty_error));
            Toast.makeText(this, R.string.invalid_content, Toast.LENGTH_SHORT).show();
            return false;
        } else {
            String text = mMeetingNameInput.getText().toString();

            if (text.length() > 10 && text.matches("^[\\p{Alnum}]*$")) {
                return true;
            } else {
                showMeetoutNameAlertDialog();
                return false;
            }
        }
    }

    private boolean isValidDescription() {
        if (mMeetingDescInput.getText().length() < 20) {
            mMeetingDescInput.setError(getResources().getString(R.string.meetout_desc_empty_error));
            Toast.makeText(this, getResources().getString(R.string.meetout_desc_empty_error), Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void showInvalidTimeFrameError() {
        Toast.makeText(this, getResources().getString(R.string.invalid_time_frame) ,Toast.LENGTH_SHORT).show();
        mToDateTime.setError(getResources().getString(R.string.invalid_time_frame));
    }

    private boolean isValidTime() {
        if (toYear < fromYear) {
            showInvalidTimeFrameError();
            return false;
        } else if (toYear == fromYear && toMonth < fromMonth) {
            showInvalidTimeFrameError();
            return false;
        } else if (toYear == fromYear && toMonth == fromMonth && toDate < fromDate) {
            showInvalidTimeFrameError();
            return false;
        } else if (toYear == fromYear && toMonth == fromMonth && toDate == fromDate && toHour < fromHour) {
            showInvalidTimeFrameError();
            return false;
        } else if (toYear == fromYear && toMonth == fromMonth && toDate == fromDate && toHour == fromHour && toMin <= fromMin) {
            showInvalidTimeFrameError();
            return false;
        } else {
            return true;
        }
    }

    private void showMeetoutLocationRequest() {
        new MaterialDialog.Builder(this)
                .content(R.string.meetout_location_request)
                .positiveText(R.string.dialog_ok)
                .show();
    }

    private boolean isValidMeetoutAddress() {
        if (meetingLocationName == null || meetingLocationAddress == null) {
            showMeetoutLocationRequest();
            return false;
        }
        return true;
    }

    private boolean isContainEnoughTopics() {
        if (topics.size() <= 2) {
            mAddTopicEdt.setError(getResources().getString(R.string.meetout_topics_error));
            Toast.makeText(this, getResources().getString(R.string.invalid_content), Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void showMeetOutMediaRequestDialog() {
        new MaterialDialog.Builder(this)
                .title(R.string.meetout_medias_missing)
                .content(R.string.meetout_medias_missing_desc)
                .positiveText(R.string.dialog_ok)
                .icon(getResources().getDrawable(R.drawable.dialogs_warning))
                .show();
    }

    private boolean isContainEnoughMedias() {
        if (mImagePaths.size() <= 2) {
            showMeetOutMediaRequestDialog();
            return false;
        }

        return true;
    }

    private boolean isValidInformation() {
        return isValidName() && isValidDescription() && isValidTime()
                && isValidMeetoutAddress() && isContainEnoughTopics()
                && isContainEnoughMedias();
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

        if (scrollY >= 482) {
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

                fromDateTime = AppUtils.getCurrentTimeStringFromDateTime(fromYear,
                        fromMonth, fromDate, fromHour, fromMin);
                mFromDateTime.setText(fromDateTime);
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

                toDateTime = AppUtils.getCurrentTimeStringFromDateTime(toYear,
                        toMonth, toDate, toHour, toMin);
                mToDateTime.setText(toDateTime);
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

    private void startGooglePlacePicker() {
        try {
            if (mightNeedLocationPermission()) {
                PlacePicker.IntentBuilder intentBuilder =
                        new PlacePicker.IntentBuilder();
                if (lastKnownLocation == null) {
                    lastKnownLocation = MapUtils.getGpsLocation(this);
                }
                intentBuilder.setLatLngBounds(MapUtils.getCurrentLatLngBound(lastKnownLocation));
                Intent intent = intentBuilder.build(NewMeetOutActivity.this);
                startActivityForResult(intent, PLACE_PICKER_REQUEST);
            }
        } catch (GooglePlayServicesRepairableException
                | GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
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

    private void createNewMeetoutJob() {
        NewMeetoutJob job = new NewMeetoutJob(
                mMeetingNameInput.getText().toString(),
                mMeetingDescInput.getText().toString(),
                meetingLong, meetingLat,
                lastKnownLocation.getLongitude(), lastKnownLocation.getLatitude(),
                meetingLocationName, meetingLocationAddress,
                fromDateTime, toDateTime,
                mImagePaths, topics
        );

        BaseApplication.getInstance().getJobManager().addJobInBackground(job);
        finish();
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
            } else if (requestCode == PLACE_PICKER_REQUEST) {
                Place place = PlacePicker.getPlace(data, this);
                String toastMsg = String.format("Place: %s", place.getName());
                Toast.makeText(this, toastMsg, Toast.LENGTH_LONG).show();

                meetingLat = place.getLatLng().latitude;
                meetingLong = place.getLatLng().longitude;
                meetingLocationName = place.getName().toString();
                meetingLocationAddress = place.getAddress().toString();

                // modify view details
                mMeetingLocDetails.setVisibility(View.VISIBLE);
                mMeetingLocation.setVisibility(View.GONE);

                mMeetingLocationName.setText(place.getName());
                mMeetingLocationAddress.setText(place.getAddress());
            } else {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            boolean allGranted = false;
            for (int grantResult : grantResults) {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    allGranted = true;
                } else {
                    allGranted = false;
                    break;
                }
            }

            if(allGranted){
                initLocationTracker();
            } else {
                // Todo show dialog
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
                if (isValidInformation()) {
                    createNewMeetoutJob();
                }
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
                break;
            case R.id.meeting_loc_details:
            case R.id.meeting_location:
                startGooglePlacePicker();
                break;
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
