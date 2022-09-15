package com.proton.ecg.ormonecg;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.proton.common.activity.base.BaseActivity;
import com.proton.common.bean.CarePatchECGReport;
import com.proton.common.bean.CarePatchUserInfo;
import com.proton.common.bean.MessageEvent;
import com.proton.common.bean.NSError;
import com.proton.common.callback.CarePatchReportListener;
import com.proton.common.db.ReportDBDao;
import com.proton.ecg.omron.CarePatchECGCardKitManager;
import com.proton.ecg.ormonecg.databinding.ActivityMainBinding;
import com.proton.common.bean.ReportDbBean;
import com.proton.report.utils.DateUtils;
import com.proton.report.utils.ReportUtils;
import com.wms.baseapp.utils.BlackToast;
import com.wms.common.adapter.CommonViewHolder;
import com.wms.common.adapter.recyclerview.CommonAdapter;
import com.wms.common.adapter.recyclerview.OnItemClickListener;
import com.wms.common.utils.DensityUtils;
import com.wms.logger.Logger;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity<ActivityMainBinding> {

    public static final int PERMISSION_CODE = 100;
    private List<ReportDbBean> reportList = new ArrayList<>();
    private CommonAdapter<ReportDbBean> adapter;

    @Override
    public int inflateContentView() {
        return R.layout.activity_main;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int i = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int i1 = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (i == PackageManager.PERMISSION_GRANTED && i1 == PackageManager.PERMISSION_GRANTED) {
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION}, 100);
        }
//        ReportDBDao.deleteAll();
        binding.btnConnect.setOnClickListener(v -> {
            String name = binding.etName.getText().toString();
            String age = binding.etAge.getText().toString();
            boolean boyChecked = binding.checkboxBoy.isChecked();
            int gender = boyChecked ? 1 : 2;
            Logger.w("gender==", gender);
            CarePatchUserInfo userInfo = null;
            String pdfDescription = binding.etPdfDescription.getText().toString();
            if (TextUtils.isEmpty(name) && TextUtils.isEmpty(age)) {
                userInfo = null;
            } else if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(age)) {
                userInfo = new CarePatchUserInfo(name, Integer.parseInt(age), gender);
            } else {
                if (TextUtils.isEmpty(name)) {
                    BlackToast.show("姓名不能为空");
                    return;
                }
                if (TextUtils.isEmpty(age)) {
                    BlackToast.show("年龄不能为空");
                    return;
                }
            }
            CarePatchECGCardKitManager.getInstance().updateDescriptor(pdfDescription);
            CarePatchECGCardKitManager.getInstance().startRecording(userInfo, new CarePatchReportListener() {
                @Override
                public void onReport(CarePatchECGReport ecgReport) {
                    Logger.w("报告回调:", ecgReport.toString());
                }

                @Override
                public void onError(NSError error) {
                    Logger.w("onError:", error.toString());
                    BlackToast.show(error.getDescription());
                }
            });
        });
        initListener();
    }

    private void initListener() {
        binding.checkboxBoy.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    binding.checkboxGirl.setChecked(false);
                }
            }
        });

        binding.checkboxGirl.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    binding.checkboxBoy.setChecked(false);
                }
            }
        });
    }

    @Override
    public void initView() {
        super.initView();
        Logger.w("width:", DensityUtils.getScreenWidth(), " ,height:", DensityUtils.getScreenHeight());
        adapter = new CommonAdapter<ReportDbBean>(this, reportList, R.layout.item_report_layout) {
            @Override
            public void convert(CommonViewHolder holder, ReportDbBean reportDbBean) {
                holder.setText(R.id.idStartTime, DateUtils.getYMDHMS(reportDbBean.getStartTime()));
            }
        };

        adapter.setOnItemClickListener(new OnItemClickListener<ReportDbBean>() {
            @Override
            public void onItemClick(ViewGroup parent, View view, ReportDbBean reportDbBean, int position) {
                CarePatchECGReport ecgReport = new CarePatchECGReport();
                ecgReport.setStartTime(reportDbBean.getStartTime());
                ecgReport.setDuration((int) reportDbBean.getDuration());
                ecgReport.setMemo(reportDbBean.getMemo());
                ecgReport.setLabel(reportDbBean.getLabel());
                ecgReport.setResult(ReportUtils.getAlgorithmByReportBean(reportDbBean));
                ecgReport.setPdfFilePath(reportDbBean.getPdfFilePath());
                ecgReport.setThumbnailFilePath(reportDbBean.getThumbnailFilePath());
                CarePatchECGCardKitManager.getInstance().showReport(ecgReport);
            }

            @Override
            public boolean onItemLongClick(ViewGroup parent, View view, ReportDbBean reportDbBean, int position) {
                return false;
            }
        });
        binding.idReportRecycler.setAdapter(adapter);
        binding.idReportRecycler.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    public void initData() {
        super.initData();
        fetchReportList();
    }

    private void fetchReportList() {
        reportList.clear();
        List<ReportDbBean> all = ReportDBDao.getAll();
        if (all.size() > 0) {
            reportList.addAll(all);
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void receiveEventBusMessage(MessageEvent message) {
        super.receiveEventBusMessage(message);
        if (message.getType() == MessageEvent.Type.SAVE_REPORT) {
            fetchReportList();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE) {
            int grantResult = grantResults[0];
        }
    }
}
