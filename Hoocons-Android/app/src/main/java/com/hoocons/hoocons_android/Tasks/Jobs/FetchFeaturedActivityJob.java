package com.hoocons.hoocons_android.Tasks.Jobs;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;
import com.hoocons.hoocons_android.EventBus.BadRequest;
import com.hoocons.hoocons_android.EventBus.FetchFeaturedActivitySuccess;
import com.hoocons.hoocons_android.EventBus.ServerErrorRequest;
import com.hoocons.hoocons_android.Networking.ApiViewSets.ActivityApiViewSet;
import com.hoocons.hoocons_android.Networking.NetContext;
import com.hoocons.hoocons_android.Networking.Responses.ActivityResponse;
import com.hoocons.hoocons_android.Networking.Services.ActivityServices;
import com.hoocons.hoocons_android.Tasks.JobProperties.JobGroup;
import com.hoocons.hoocons_android.Tasks.JobProperties.Priority;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by hungnguyen on 8/11/17.
 */

public class FetchFeaturedActivityJob extends Job {
    private int pageNum;

    public FetchFeaturedActivityJob(int pageNum) {
        super(new Params(Priority.HIGH).requireNetwork().persist().groupBy(JobGroup.event));
        this.pageNum = pageNum;
    }

    @Override
    public void onAdded() {

    }

    @Override
    public void onRun() throws Throwable {
        ActivityServices services = NetContext.instance.create(ActivityServices.class);
        services.getPageActivities(pageNum).enqueue(new Callback<ActivityApiViewSet>() {
            @Override
            public void onResponse(Call<ActivityApiViewSet> call,
                                   Response<ActivityApiViewSet> response) {
                if (response.code() == 200) {
                    EventBus.getDefault().post(new FetchFeaturedActivitySuccess(response.body()));
                } else {
                    EventBus.getDefault().post(new BadRequest());
                }
            }

            @Override
            public void onFailure(Call<ActivityApiViewSet> call, Throwable t) {
                EventBus.getDefault().post(new ServerErrorRequest());
            }
        });
    }

    @Override
    protected void onCancel(int cancelReason, @Nullable Throwable throwable) {

    }

    @Override
    protected RetryConstraint shouldReRunOnThrowable(@NonNull Throwable throwable, int runCount, int maxRunCount) {
        return null;
    }
}
