package com.xxy.demo;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class RecordActivity extends Activity {

    private Button speakButton;// 按住说话
    private TextView message;
    private SendSoundsThread sendSoundsThread = new SendSoundsThread();
    private ReceiveSoundsThread receiveSoundsThread = new ReceiveSoundsThread();
    private boolean isFirst = true;
    private Speex speex;
    // 设备信息：手机名+Android版本
    private String DevInfo = android.os.Build.MODEL + " Android " + android.os.Build.VERSION.RELEASE;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        speex=new Speex();
        speex.init();
        message = (TextView) findViewById(R.id.Message);

        speakButton = (Button) findViewById(R.id.speakButton);
        speakButton.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {

                if (event.getAction() == MotionEvent.ACTION_DOWN)
                {
                    message.setText("松开结束");
                if (isFirst){
                        sendSoundsThread.start();
                        receiveSoundsThread.start();
                        isFirst = false;
                    }
                    sendSoundsThread.setRunning(true);
                    receiveSoundsThread.setRunning(false);
                    if (ContextCompat.checkSelfPermission(RecordActivity.this,
                            Manifest.permission.RECORD_AUDIO)
                            != PackageManager.PERMISSION_GRANTED) {
                        System.out.println("request Permission");
                        ActivityCompat.requestPermissions(RecordActivity.this,
                                new String[]{Manifest.permission.RECORD_AUDIO},
                                1);

                    } else {
                        System.out.println("Permission granted!");
                    }
//                    if (isFirst)
//                    {
//                        sendSoundsThread.start();
//                        receiveSoundsThread.start();
//                        isFirst = false;
//                    }
//                    sendSoundsThread.setRunning(true);
//                    receiveSoundsThread.setRunning(false);
                }
                else if (event.getAction() == MotionEvent.ACTION_UP)
                {
                    message.setText("按住说话");
                    sendSoundsThread.setRunning(false);
                    receiveSoundsThread.setRunning(true);
                }
                return false;
            }
        });
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    if (isFirst){
                    sendSoundsThread.start();
                    receiveSoundsThread.start();
                    isFirst = false;
                    }
                    sendSoundsThread.setRunning(true);
                    receiveSoundsThread.setRunning(false);
                    System.out.println("User granted permission");

                } else {
                    System.out.println("User didn't grante permission");

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    class SendSoundsThread extends Thread
    {


        private byte[] recordBytes = new byte[640];
        private AudioRecord recorder = null;
        private boolean isRunning=false;
        public SendSoundsThread()
        {
            super();
            // 录音机 44100
            int bufferSize = AudioRecord.getMinBufferSize(8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            //实例化AudioRecord
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, 8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);

            Log.e("ddd2222",recorder.getRecordingState()+"");
                   }

        @Override
        public synchronized void run()
        {
            super.run();
            if (recorder!=null) {
                recorder.startRecording();
            }
            while (true)
            {

                if (isRunning)
                {
                    try
                    {
                        DatagramSocket clientSocket = new DatagramSocket();
                        InetAddress IP = InetAddress.getByName(AppConfig.IPAddress);// 向这个网络广播
//                        int sizeInShorts = speex.getFrameSize();
//                        short[] audioData = new short[sizeInShorts];
//                        int sizeInBytes = speex.getFrameSize();
//                        int number = recorder.read(audioData, 0,
//                                sizeInShorts);
//                        short[] dst = new short[sizeInBytes];
//                        byte[] encoded = new byte[sizeInBytes];
//                        int count = speex.encode(dst, 0, encoded,
//                                number);
                        // 获取音频数据
                        recorder.read(recordBytes, 0, recordBytes.length);

                        // 构建数据包 头+体
                        //dataPacket dataPacket = new dataPacket(DevInfo.getBytes(), encoded);
                        dataPacket dataPacket = new dataPacket(DevInfo.getBytes(), recordBytes);

                        // 构建数据报
                        DatagramPacket sendPacket = new DatagramPacket(dataPacket.getAllData(),
                                dataPacket.getAllData().length, IP, AppConfig.Port);
                        Log.e("wwwwwwwwwww",dataPacket.getAllData().length+"");
                        // 发送
                        clientSocket.send(sendPacket);
                        clientSocket.close();

                    }
                    catch (SocketException e)
                    {
                        e.printStackTrace();
                    }
                    catch (UnknownHostException e)
                    {
                        e.printStackTrace();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }

        public void setRunning(boolean isRunning)
        {
            this.isRunning = isRunning;
        }
    }

    class ReceiveSoundsThread extends Thread
    {
        private AudioTrack player = null;
        private boolean isRunning = false;
        private byte[] recordBytes = new byte[670];

        public ReceiveSoundsThread()
        {
            // 播放器
            int playerBufferSize = AudioTrack.getMinBufferSize(8000, AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            player = new AudioTrack(AudioManager.STREAM_MUSIC, 8000, AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, playerBufferSize, AudioTrack.MODE_STREAM);
        }

        @Override
        public synchronized void run()
        {
            super.run();

            try
            {
                @SuppressWarnings("resource")
                DatagramSocket serverSocket = new DatagramSocket(AppConfig.Port);
                while (true)
                {
                    if (isRunning)
                    {

                        DatagramPacket receivePacket = new DatagramPacket(recordBytes, recordBytes.length);
                        serverSocket.receive(receivePacket);

                        byte[] data = receivePacket.getData();

                        byte[] head = new byte[30];
                        byte[] body = new byte[640];

                        // 获得包头
                        for (int i = 0; i < head.length; i++)
                        {
                            head[i] = data[i];
                        }

                        // 获得包体
                        for (int i = 0; i < body.length; i++)
                        {
                            body[i] = data[i + 30];
                        }

                        // 获得头信息 通过头信息判断是否是自己发出的语音
                        String thisDevInfo = new String(head).trim();
                        System.out.println(thisDevInfo);
                        Log.e("ddddd",thisDevInfo+"");
//                        if (!thisDevInfo.equals(DevInfo))
//                        {
                            player.write(body, 0, body.length);
                            player.play();
                        //}
                    }
                }
            }
            catch (SocketException e)
            {
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        public void setRunning(boolean isRunning)
        {
            this.isRunning = isRunning;
        }
    }
    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

}
