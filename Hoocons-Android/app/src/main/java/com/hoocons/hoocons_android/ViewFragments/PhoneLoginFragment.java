package com.hoocons.hoocons_android.ViewFragments;


import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.hbb20.CountryCodePicker;
import com.hoocons.hoocons_android.Activities.MainActivity;
import com.hoocons.hoocons_android.EventBus.CompleteLoginRequest;
import com.hoocons.hoocons_android.Helpers.AppUtils;
import com.hoocons.hoocons_android.Managers.SharedPreferencesManager;
import com.hoocons.hoocons_android.Networking.NetContext;
import com.hoocons.hoocons_android.Networking.Requests.CredentialRequest;
import com.hoocons.hoocons_android.Networking.Responses.TokenResponse;
import com.hoocons.hoocons_android.Networking.Responses.UserInfoResponse;
import com.hoocons.hoocons_android.Networking.Services.UserServices;
import com.hoocons.hoocons_android.R;
import com.vstechlab.easyfonts.EasyFonts;

import org.greenrobot.eventbus.EventBus;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PhoneLoginFragment extends Fragment implements View.OnClickListener{
    private final String TAG = PhoneLoginFragment.class.getSimpleName();

    @BindView(R.id.action_close)
    ImageButton mCloseButton;
    @BindView(R.id.forget_password)
    Button mResetPasswordBtn;
    @BindView(R.id.country_code_picker)
    CountryCodePicker mCountryCodePicker;
    @BindView(R.id.phone_input)
    EditText mPhoneInput;
    @BindView(R.id.password_input)
    EditText mPasswordInput;
    @BindView(R.id.login_button)
    Button mLoginBtn;
    @BindView(R.id.actionbar_title)
    TextView mActionBarTitle;

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;
    private String mCountryCode;
    private String mPhoneNumber;

    private MaterialDialog pDialog;

    public PhoneLoginFragment() {
        // Required empty public constructor
    }

    public static PhoneLoginFragment newInstance(String param1, String param2) {
        PhoneLoginFragment fragment = new PhoneLoginFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_phone_login, container, false);
        ButterKnife.bind(this, rootView);

        mCountryCode = mCountryCodePicker.getDefaultCountryCodeWithPlus();
        pDialog = new MaterialDialog.Builder(getContext())
                .title(R.string.logging_in)
                .content(R.string.please_wait)
                .progress(true, 0)
                .build();

        initView();
        initTypeFace();

        return rootView;
    }

    private void initTypeFace() {
        mResetPasswordBtn.setTypeface(EasyFonts.robotoRegular(getContext()));
        mActionBarTitle.setTypeface(EasyFonts.robotoBold(getContext()));
        mPhoneInput.setTypeface(EasyFonts.robotoRegular(getContext()));
        mPasswordInput.setTypeface(EasyFonts.robotoRegular(getContext()));
        mCountryCodePicker.setTypeFace(EasyFonts.robotoRegular(getContext()));
    }

    private void initView() {
        mResetPasswordBtn.setOnClickListener(this);

        mLoginBtn.setOnClickListener(this);

        mCountryCodePicker.setOnCountryChangeListener(new CountryCodePicker.OnCountryChangeListener() {
            @Override
            public void onCountrySelected() {
                Toast.makeText(getContext(), "Updated " + mCountryCodePicker.getSelectedCountryName(),
                        Toast.LENGTH_SHORT).show();
                mCountryCode =  mCountryCodePicker.getSelectedCountryCodeWithPlus();
            }
        });
    }

    private void commitForgetPasswordScreen() {
        final FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.setCustomAnimations(R.anim.fade_in_from_right, R.anim.fade_out_to_left);
        ft.replace(R.id.login_container, new VerifyPhoneFragment(), "verify_phone_fragment");
        ft.commit();
    }

    private boolean validateLoginFields() {
        String phoneNumber = mPhoneInput.getText().toString();
        if (!phoneNumber.matches("[0-9]+") && phoneNumber.length() < 9) {
            mPhoneInput.setError(getResources().getString(R.string.error_phone_number));
            return false;
        } else if (mPasswordInput.getText().toString().length() == 0) {
            mPasswordInput.setError(getResources().getString(R.string.error_empty_password));
            return false;
        }
        return true;
    }

    private void showProcessDialog() {
        pDialog.show();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.login_button:
                if (validateLoginFields()) {
                    showProcessDialog();
                    mPhoneNumber = String.format("%s%s", mCountryCode, mPhoneInput.getText().toString());
                    Log.e(TAG, String.format("Phone #: %s", mPhoneNumber));

                    attemptToLogin(mPhoneNumber, mPasswordInput.getText().toString());
                }
                break;
            case R.id.forget_password:
                commitForgetPasswordScreen();
                break;
            default:
                break;
        }
    }

    private void showWarningDialog() {
        new MaterialDialog.Builder(getContext())
                .title(getResources().getString(R.string.warning))
                .content(getResources().getString(R.string.account_banned_warn))
                .positiveText("OK")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                    }
                })
                .cancelable(true)
                .cancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        dialogInterface.dismiss();
                    }
                })
                .show();
    }

    private void attemptToLogin(final String phoneNumber, final String password) {
        UserServices services = NetContext.instance.create(UserServices.class);
        services.login(new CredentialRequest(phoneNumber, password)).enqueue(new Callback<TokenResponse>() {
            @Override
            public void onResponse(Call<TokenResponse> call, Response<TokenResponse> response) {
                if (response.code() == 201) {
                    // Code 201: login complete
                    TokenResponse response1 = response.body();
                    assert response1 != null;

                    SharedPreferencesManager.getDefault().setUserToken(response1.getAccessToken());
                    SharedPreferencesManager.getDefault().setUserId(response1.getUserId());
                    SharedPreferencesManager.getDefault().setCredentials(new String[] {phoneNumber, password});
                    AppUtils.signInAnonymously(getActivity());
                    getUserInfo();
                } else if (response.code() == 401) {
                    // Code 401: wrong credentials
                    pDialog.dismiss();
                    Toast.makeText(getContext(), getResources().getString(R.string.login_failed),
                            Toast.LENGTH_SHORT).show();
                } else if (response.code() ==  403) {
                    pDialog.dismiss();
                    // Code 403: this account is banned
                    showWarningDialog();
                } else if (response.code() == 404) {
                    // Code  404: Server error
                    pDialog.dismiss();
                    Toast.makeText(getContext(), getResources().getString(R.string.server_error)
                            , Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<TokenResponse> call, Throwable t) {
                Toast.makeText(getContext(), getResources().getString(R.string.server_error), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getUserInfo() {
        UserServices service = NetContext.instance.create(UserServices.class);
        service.getUserInfo(SharedPreferencesManager.getDefault().getUserId())
                .enqueue(new Callback<UserInfoResponse>() {
            @Override
            public void onResponse(Call<UserInfoResponse> call, Response<UserInfoResponse> response) {
                UserInfoResponse resp = response.body();
                pDialog.dismiss();
                if (response.code() == 200) {
                    attachUserPk(resp.getUserPK());
                    // Code 200 is success getting user data
                    if (resp.getNickname().equals(resp.getUsername())){
                        commitUserInfoUpdateScreen();
                    } else {
                        SharedPreferencesManager.getDefault()
                                .setUserKeyInfo(new UserInfoResponse(resp.getDisplayName(),
                                        resp.getNickname(), resp.getProfileUrl()));

                        EventBus.getDefault().post(new CompleteLoginRequest());
                    }
                } else if (response.code() == 404) {
                    // Code 404 is not found info
                    // Todo: handle code 404 -- If UserId is null, reload user info on splash
                }
            }

            @Override
            public void onFailure(Call<UserInfoResponse> call, Throwable t) {
                pDialog.dismiss();
                Toast.makeText(getContext(), getResources().getString(R.string.server_error), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void attachUserPk(int pk) {
        SharedPreferencesManager.getDefault().setUserId(pk);
    }

    private void commitUserInfoUpdateScreen() {
        final FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.setCustomAnimations(R.anim.fade_in_from_right, R.anim.fade_out_to_left);
        ft.replace(R.id.login_container, new NewUserInfoFragment(), "new_user_info_fragment");
        ft.commit();
    }

    private void showNewUserDialog() {
        new MaterialDialog.Builder(getContext())
                .title(getResources().getString(R.string.new_user_question))
                .content(getResources().getString(R.string.new_user_detect_and_req_register))
                .positiveText(getResources().getString(R.string.register))
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        transferToRegisterFlow();
                    }
                })
                .negativeText(getResources().getString(R.string.not_now))
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                    }
                })
                .cancelable(false)
                .show();
    }

    private void transferToRegisterFlow() {

    }

    /**********************************************
     * EVENTBUS CATCHING FIELDS
    ***********************************************/
}
