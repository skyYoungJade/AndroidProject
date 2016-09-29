package com.zsq.mutliapp;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * 多线程断点下载
 */
public class MainActivity extends AppCompatActivity {

    @BindView(R.id.textInfo) TextView textInfo;
    @BindView(R.id.bnt_download) Button bntDownload;
    @BindView(R.id.progress) ProgressBar progress;

    private static final String BASEURL = "http://10.55.30.11:80/test/flask.pdf";
    private static final String path = Environment.getExternalStorageDirectory() + "/zsq";
    private int length;//文件的长度
    private String file_Name = "";//文件名称
    private boolean isLoading = false;
    private MutliDownUtils downUtils;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        downUtils = MutliDownUtils.getInstance();
        downUtils.initialization(BASEURL, this, path);
        downUtils.setCallInfo(new MutliDownUtils.CallbackInfo() {
            @Override
            public void getInfo(String fileName, int fileSize) {
                length = fileSize;
                file_Name = fileName;
                textInfo.post(new Runnable() {
                    @Override
                    public void run() {
                        textInfo.setText("Info: " + file_Name + "; \n 大小: " + length + "byte");
                    }
                });
                progress.setMax(length);
                progress.setProgress(0);
            }
        });
        downUtils.setCallProgress(new MutliDownUtils.CallbackProgress() {
            @Override
            public void update(final int add) {
                progress.post(new Runnable() {
                    @Override
                    public void run() {
                        progress.setProgress(add);
                        if (add >= length) {//下载完成
                            Toast.makeText(MainActivity.this, "下载完成！", Toast.LENGTH_SHORT).show();
                            bntDownload.setText("下载完成");
                        }
                    }
                });
            }
        });

    }

    @OnClick(R.id.bnt_download)
    public void onClick() {
        if (isLoading) { //暂停下载
            bntDownload.setText("点击下载");
            isLoading = false;
            downUtils.execute(false);
            return;
        }
        isLoading = true;
        bntDownload.setText("点击暂停");
        downUtils.execute(true);//执行下载
        Toast.makeText(MainActivity.this, "执行下载", Toast.LENGTH_SHORT).show();

    }


}
