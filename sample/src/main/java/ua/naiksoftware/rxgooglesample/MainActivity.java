package ua.naiksoftware.rxgooglesample;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;

import ua.naiksoftware.rxgoogle.RxGoogle;
import ua.naiksoftware.rxgoogle.StatusException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    ProgressBar progressBar;

    ArrayList<FitnessSessionData> fitnessSessionDataList = new ArrayList<>();

    Subscription subscription;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = (RecyclerView) findViewById(R.id.rv_main);
        progressBar = (ProgressBar) findViewById(R.id.pb_main);

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        RxGoogle.init(this, new Api[] { Fitness.SESSIONS_API, Fitness.HISTORY_API }, new Scope[] { new Scope(Scopes.FITNESS_ACTIVITY_READ) });
        RxGoogle.setDefaultTimeout(15, TimeUnit.SECONDS);

        getFitnessData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(subscription != null && !subscription.isUnsubscribed()) {
            subscription.unsubscribe();
        }
    }

    private void getFitnessData() {
        fitnessSessionDataList.clear();
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        DataReadRequest.Builder dataReadRequestBuilder = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                .aggregate(DataType.TYPE_CALORIES_EXPENDED, DataType.AGGREGATE_CALORIES_EXPENDED)
                .bucketBySession(1, TimeUnit.MINUTES)
                // 3 months back
                .setTimeRange(System.currentTimeMillis() - 7776000000L, System.currentTimeMillis(), TimeUnit.MILLISECONDS);

        DataReadRequest dataReadRequest = dataReadRequestBuilder.build();
        DataReadRequest dataReadRequestServer = dataReadRequestBuilder.enableServerQueries().build();

        // First, request all data from the server. If there is an error (e.g. timeout),
        // switch to normal request
        subscription = RxGoogle.Fit.History.read(dataReadRequestServer)
                .doOnError(throwable -> { if(throwable instanceof StatusException && ((StatusException)throwable).getStatus().getStatusCode() == CommonStatusCodes.TIMEOUT) Log.e("MainActivity", "Timeout on server query request"); })
                .compose(RxGoogle.OnExceptionResumeNext.with(RxGoogle.Fit.History.read(dataReadRequest)))
                .flatMapObservable(dataReadResult -> Observable.from(dataReadResult.getBuckets()))
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(bucket -> {
                    FitnessSessionData fitnessSessionData = new FitnessSessionData();
                    fitnessSessionData.name = bucket.getSession().getName();
                    fitnessSessionData.appName = bucket.getSession().getAppPackageName();
                    fitnessSessionData.activity = bucket.getSession().getActivity();
                    fitnessSessionData.start = new Date(bucket.getSession().getStartTime(TimeUnit.MILLISECONDS));
                    fitnessSessionData.end = new Date(bucket.getSession().getEndTime(TimeUnit.MILLISECONDS));
                    fitnessSessionData.steps = bucket.getDataSet(DataType.AGGREGATE_STEP_COUNT_DELTA).getDataPoints().get(0).getValue(Field.FIELD_STEPS).asInt();
                    fitnessSessionData.calories = (int) bucket.getDataSet(DataType.AGGREGATE_CALORIES_EXPENDED).getDataPoints().get(0).getValue(Field.FIELD_CALORIES).asFloat();

                    fitnessSessionDataList.add(fitnessSessionData);
                }, e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e("MainActivity", "Error reading fitness data", e);
                    Snackbar.make(recyclerView, "Error getting Fit data", Snackbar.LENGTH_INDEFINITE)
                            .setAction("Retry", v -> getFitnessData())
                            .show();

                }, () -> {
                    recyclerView.setAdapter(new FitnessSessionAdapter());
                    progressBar.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);

                    if(fitnessSessionDataList.isEmpty()) {
                        Snackbar.make(recyclerView, "No sessions found", Snackbar.LENGTH_INDEFINITE).show();
                    }
                });
    }


    private class FitnessSessionViewHolder extends RecyclerView.ViewHolder {
        private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        private TextView name;
        private TextView dateTimeStart;
        private TextView dateTimeStop;
        private TextView activity;
        private TextView calories;
        private TextView steps;
        private ImageView icon;

        public FitnessSessionViewHolder(View itemView) {
            super(itemView);

            name = (TextView) itemView.findViewById(R.id.tv_name);
            dateTimeStart = (TextView) itemView.findViewById(R.id.tv_datetime_start);
            dateTimeStop = (TextView) itemView.findViewById(R.id.tv_datetime_stop);
            activity = (TextView) itemView.findViewById(R.id.tv_activity);
            calories = (TextView) itemView.findViewById(R.id.tv_calories);
            steps = (TextView) itemView.findViewById(R.id.tv_steps);
            icon = (ImageView) itemView.findViewById(R.id.iv_icon);
        }

        public void bind(FitnessSessionData fitnessSessionData) {
            dateTimeStart.setText("Start: " + DATE_FORMAT.format(fitnessSessionData.start));
            dateTimeStop.setText("Stop: " + DATE_FORMAT.format(fitnessSessionData.end));
            calories.setText(fitnessSessionData.calories + "kcal");
            steps.setText(fitnessSessionData.steps + " steps");
            activity.setText("Activity: " + fitnessSessionData.activity);
            name.setText("Session" + (!TextUtils.isEmpty(fitnessSessionData.name) ? ": " + fitnessSessionData.name : ""));

            try {
                icon.setVisibility(View.VISIBLE);
                icon.setImageDrawable(getPackageManager().getApplicationIcon(fitnessSessionData.appName));
            } catch(Exception ignore) {
                icon.setVisibility(View.GONE);
            }
        }
    }

    private class FitnessSessionAdapter extends RecyclerView.Adapter<FitnessSessionViewHolder> {

        @Override
        public FitnessSessionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_fitness_session, parent, false);

            return new FitnessSessionViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(FitnessSessionViewHolder holder, int position) {
            holder.bind(fitnessSessionDataList.get(position));
        }

        @Override
        public int getItemCount() {
            return fitnessSessionDataList.size();
        }
    }

    private static class FitnessSessionData {
        public Date start;
        public Date end;
        public String name;
        public String appName;
        public String activity;
        public int steps;
        public int calories;

    }
}
