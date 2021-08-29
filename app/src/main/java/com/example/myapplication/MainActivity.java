package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.SystemClock;
import android.serialport.SerialPort;
import android.util.Log;

import com.licheedev.hwutils.ByteUtil;
import com.licheedev.myutils.LogPlus;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            SerialPort serialPort = new SerialPort(new File("/dev/ttyS3"), 9600);
            SerialReadThread mReadThread = new SerialReadThread(serialPort.getInputStream());
            mReadThread.start();
            OutputStream mOutputStream = serialPort.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private class SerialReadThread extends Thread {

        private static final String TAG = "SerialReadThread";

        private BufferedInputStream mInputStream;

        public SerialReadThread(InputStream is) {
            mInputStream = new BufferedInputStream(is);
        }

        @Override
        public void run() {
            byte[] received = new byte[1024];
            int size;

            LogPlus.e("开始读线程");

            while (true) {

                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
                try {

                    int available = mInputStream.available();

                    if (available > 0) {
                        size = mInputStream.read(received);
                        if (size > 0) {
                            onDataReceive(received, size);
                        }
                    } else {
                        // 暂停一点时间，免得一直循环造成CPU占用率过高
                        SystemClock.sleep(1);
                    }
                } catch (IOException e) {
                    LogPlus.e("读取数据失败", e);
                }
                //Thread.yield();
            }

            LogPlus.e("结束读进程");
        }

        /**
         * 处理获取到的数据
         *
         * @param received
         * @param size
         */
        private void onDataReceive(byte[] received, int size) {
            // TODO: 2018/3/22 解决粘包、分包等
            String hexStr = ByteUtil.bytes2HexStr(received, 0, size);
            //LogManager.instance().post(new RecvMessage(hexStr));
            Log.i("Message", "MSG: " + new RecvMessage(hexStr));
        }

        /**
         * 停止读线程
         */
        public void close() {

            try {
                mInputStream.close();
            } catch (IOException e) {
                LogPlus.e("异常", e);
            } finally {
                super.interrupt();
            }
        }
    }
    private class RecvMessage implements IMessage {

        private String command;
        private String message;

        public RecvMessage(String command) {
            this.command = command;
            this.message = currentTime() + "    收到命令：" + command;
        }

        @Override
        public String getMessage() {
            return message;
        }

        @Override
        public boolean isToSend() {
            return false;
        }
    }

    private interface IMessage {
        /**
         * 消息文本
         *
         * @return
         */
        String getMessage();

        /**
         * 是否发送的消息
         *
         * @return
         */
        boolean isToSend();
    }


        public static final SimpleDateFormat DEFAULT_FORMAT =
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

        public static String currentTime() {
            Date date = new Date();
            return DEFAULT_FORMAT.format(date);
        }

}
