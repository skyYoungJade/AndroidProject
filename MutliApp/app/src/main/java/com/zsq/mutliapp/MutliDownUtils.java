package com.zsq.mutliapp;

import android.content.Context;
import android.util.Log;
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

/**
 * 多线程断点下载
 * Created by zsq on 2016/9/27.
 */
public class MutliDownUtils {
    private static MutliDownUtils instance = null;
    private URL url;  //url信息
    private String BASEURL;//url
    private int length;//下载文件长度
    private Context context;
    private String filePath;//新建文件路径
    private File file;//生成的文件
    private int total = 0;//下载的总量
    private boolean isDownLoad = true;
    private List<HashMap<String, Integer>> threadList;//记录所下载的长度
    private CallbackInfo callInfo; //回调info
    private CallbackProgress callProgress;//回调进度


    public static MutliDownUtils getInstance() {
        if (instance == null) {
            instance = new MutliDownUtils();
        }
        return instance;
    }


    /**
     * 初始化
     *
     * @param url
     * @param cx
     * @param path
     */
    public void initialization(String url, Context cx, String path) {
        BASEURL = url;
        context = cx;
        filePath = path;
        threadList = new ArrayList<>();
//        startTask(false);
    }

    /**
     * 执行任务
     *
     * @param is 暂停或继续
     */
    private void startTask(boolean is) {
        isDownLoad = is;

        if (!isDownLoad) { // 添加暂停功能
            return;
        }
        if (threadList.size() == 0) {//判断是重头下载 还是 暂停后继续下载
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        url = new URL(BASEURL);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestMethod("GET");
                        connection.setReadTimeout(5000);
                        length = connection.getContentLength();//文件长度
                        if (length < 0) { //如果文件不存在
                            Toast.makeText(context, "文件不存在~", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Log.w("log:", "info:" + length + "字节");
                        Log.w("log:", "文件名:" + getFileName(BASEURL));
                        if (callInfo != null) callInfo.getInfo(getFileName(BASEURL), length); //设置回调

                        //创建文件夹
                        createPath(filePath);
                        //生成的文件
                        file = new File(filePath, getFileName(BASEURL));
                        RandomAccessFile accessFile = new RandomAccessFile(file, "rw");
                        accessFile.setLength(length);

                        int blockSize = length / 3;
                        for (int i = 0; i < 3; i++) {
                            int begin = i * blockSize;
                            int end = (i + 1) * blockSize;
                            if (i == 2) {
                                end = length;
                            }
                            //记录下载长度的变化
                            HashMap<String, Integer> map = new HashMap<>();
                            map.put("begin", begin);
                            map.put("end", end);
                            map.put("finished", 0);
                            threadList.add(map);

                            //创建线程,下载文件
                            Thread thread = new Thread(new DownloadRunnable(i, begin, end, file, url));
                            thread.start();

                        }


                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }).start();
        } else {//恢复下载
            for (int i = 0; i < threadList.size(); i++) {
                HashMap<String, Integer> map = threadList.get(i);
                int begin = map.get("begin");
                int end = map.get("end");
                int finished = map.get("finished");
                Thread thread = new Thread(new DownloadRunnable(i, begin + finished, end, file, url));
                thread.start();

            }
        }
    }

    /**
     * 执行任务
     *
     * @param is :暂停或继续
     */

    public void execute(boolean is) {
        startTask(is);

    }


    /**
     * 断点下载
     */
    class DownloadRunnable implements Runnable {
        private int begin;
        private int end;
        private File file;
        private URL url;
        private int id;

        public DownloadRunnable(int id, int begin, int end, File file, URL url) {
            this.id = id;
            this.begin = begin;
            this.end = end;
            this.file = file;
            this.url = url;

        }

        @Override
        public void run() {
            try {
                if (begin > end) return;
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Range", "bytes=" + begin + "-" + end);//设置起止位置-断点下载的关键点
                InputStream inputStream = connection.getInputStream();//以流的方式获取
                byte[] buf = new byte[1024 * 1024];//设置大小1M
                RandomAccessFile accessFile = new RandomAccessFile(file, "rw");//读写权限
                accessFile.seek(begin);//读写位置的定位

                int len = 0;
                HashMap<String, Integer> map = threadList.get(id);//取出信息
                while ((len = inputStream.read(buf)) != -1 && isDownLoad) {//流的写入 && 是否暂停
                    accessFile.write(buf, 0, len);
                    updateProgress(len);
                    map.put("finished", map.get("finished") + len);//数据的更新
                }
                inputStream.close();
                accessFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }


    /**
     * 更新进度
     *
     * @param add ：增量
     */
    synchronized private void updateProgress(int add) {
        total += add;
        if (callProgress != null) callProgress.update(total);
//        Log.w("更新进度:", "updateProgress: " + total);
    }

    /**
     * 设置info回调
     *
     * @param callInfo
     */
    public void setCallInfo(CallbackInfo callInfo) {
        this.callInfo = callInfo;
    }

    /**
     * 设置progress回调
     *
     * @param callProgress
     */
    public void setCallProgress(CallbackProgress callProgress) {
        this.callProgress = callProgress;
    }


    /**
     * 创建目录
     */
    private void createPath(String path) {
        File file = new File(path);
        if (!file.exists()) {
            file.mkdir();
        }
    }

    /**
     * 文件名(以最后/后内容命名)
     *
     * @param url
     * @return
     */
    private String getFileName(String url) {
        int index = url.lastIndexOf("/");
        return url.substring(index);
    }

    /**
     * 回调信息
     */
    public interface CallbackInfo {
        void getInfo(String fileName, int fileSize);
    }

    /**
     * 回调进度
     */
    public interface CallbackProgress {
        void update(int add);
    }
}
