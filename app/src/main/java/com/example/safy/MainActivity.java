package com.example.safy;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;

import org.opencv.dnn.Dnn;
import org.opencv.utils.Converters;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends AppCompatActivity
        implements CameraBridgeViewBase.CvCameraViewListener2 {

    int m_Camidx = 1;//front : 1, back : 0
    CameraBridgeViewBase m_CameraView;
    BaseLoaderCallback baseLoaderCallback;

    boolean startYolo=false;
    boolean firstTimeYolo=false;
    Net yolo;

    Button mBtnConnect;
    BluetoothAdapter mBluetoothAdapter;
    Set<BluetoothDevice> mPairedDevices;
    List<String> mListPairedDevices;

    Handler mBluetoothHandler;
    ConnectedBluetoothThread mThreadConnectedBluetooth;
    BluetoothDevice mBluetoothDevice;
    BluetoothSocket mBluetoothSocket;

    final static int BT_REQUEST_ENABLE = 1;
    final static int BT_MESSAGE_READ = 2;
    final static int BT_CONNECTING_STATUS = 3;
    final static UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    float sensorData = 0;



    //파일 경로지정
    private static String getPath(String file, Context context){
        AssetManager assetManager = context.getAssets();
        BufferedInputStream inputStream = null;
        try {
            inputStream = new BufferedInputStream(assetManager.open(file));
            byte[] data=new byte[inputStream.available()];
            inputStream.read(data);
            inputStream.close();
            //Create copy file in storage.
            File outFile = new File(context.getFilesDir(), file);
            FileOutputStream os = new FileOutputStream(outFile);
            os.write(data);
            os.close();
            //Return a path to file which may be read in common way.
            return outFile.getAbsolutePath();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }


    public void YOLO(View Button){
        if (!startYolo){
            startYolo = true;
            if(!firstTimeYolo){
                firstTimeYolo = true;
                String yoloCfg = getPath("custom_yolov3.cfg", this); //핸드폰내 외부 저장소 경로
                String yoloWeights = getPath("custom_yolov3.weights", this);

                yolo = Dnn.readNetFromDarknet(yoloCfg, yoloWeights);
            }
        } else{
            startYolo = false;
        }
    }

    public void END(View Button) {
        Intent intent = new Intent(MainActivity.this, StartActivity.class);
        startActivity(intent); //액티비티 이동
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //화면 꺼짐 방지
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        m_CameraView = (CameraBridgeViewBase)findViewById(R.id.activity_surface_view);
        m_CameraView.setVisibility(SurfaceView.VISIBLE);
        m_CameraView.setCvCameraViewListener(this);
        m_CameraView.setCameraIndex(m_Camidx);

        //System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        baseLoaderCallback = new BaseLoaderCallback(this) {
            @Override
            public void onManagerConnected(int status) {
                super.onManagerConnected(status);

                switch (status) {

                    case BaseLoaderCallback.SUCCESS:
                        m_CameraView.enableView();
                        break;
                    default:
                        super.onManagerConnected(status);
                        break;
                }
            }
        };

        mBtnConnect = (Button)findViewById(R.id.btnConnect);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBtnConnect.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                listPairedDevices();
            }
        });

        mBluetoothHandler = new Handler(){
            public void handleMessage(android.os.Message msg){
                if(msg.what == BT_MESSAGE_READ){
                    String readMessage = null;
                    try {
                        readMessage = new String((byte[]) msg.obj, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    //Toast.makeText(getApplicationContext(), readMessage, Toast.LENGTH_LONG).show();
                    //mTvReceiveData.setText(readMessage);
                    sensorData = Float.parseFloat(readMessage);

                    if (sensorData <=100 ) {
                        ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME);
                        tone.startTone(ToneGenerator.TONE_DTMF_S, 500);
                        //Toast.makeText(getApplicationContext(), (int) sensorData, Toast.LENGTH_LONG).show();
                        Toast.makeText(getApplicationContext(), "거리: " + readMessage, Toast.LENGTH_LONG).show();

                    }
                }
            }
        };

        final Button questionButton = (Button)findViewById(R.id.questionButton);
        questionButton.setOnClickListener(new View.OnClickListener(){

            public void onClick(View view) {
                AlertDialog.Builder dlg = new AlertDialog.Builder(MainActivity.this);
                dlg.setTitle("사용법");
                dlg.setMessage("오른쪽 의 블루투스 버튼을 눌러 킥보드의 센서와 연결해주세요!\nSTART 버튼을 누르고 주행을 시작하세요.\n헬멧을 착용하지 않거나 두 명 이상 탑승하면 왼쪽 위에 경고 표시가 떠요.\n");
                dlg.show();
            }
        });
    }

    void listPairedDevices() {
        if (mBluetoothAdapter.isEnabled()) {
            mPairedDevices = mBluetoothAdapter.getBondedDevices();

            if (mPairedDevices.size() > 0) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("장치 선택");

                mListPairedDevices = new ArrayList<String>();
                for (BluetoothDevice device : mPairedDevices) {
                    mListPairedDevices.add(device.getName());
                    //mListPairedDevices.add(device.getName() + "\n" + device.getAddress());
                }
                final CharSequence[] items = mListPairedDevices.toArray(new CharSequence[mListPairedDevices.size()]);
                mListPairedDevices.toArray(new CharSequence[mListPairedDevices.size()]);

                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        connectSelectedDevice(items[item].toString());
                    }
                });
                AlertDialog alert = builder.create();
                alert.show();
            } else {
                Toast.makeText(getApplicationContext(), "페어링된 장치가 없습니다.", Toast.LENGTH_LONG).show();
            }
        }
        else {
            Toast.makeText(getApplicationContext(), "블루투스가 비활성화 되어 있습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    void connectSelectedDevice(String selectedDeviceName) {
        for(BluetoothDevice tempDevice : mPairedDevices) {
            if (selectedDeviceName.equals(tempDevice.getName())) {
                mBluetoothDevice = tempDevice;
                break;
            }
        }


        //create & connect socket
        try {
            mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(BT_UUID);
            //Toast.makeText(getApplicationContext(), "UUID 블루투스.", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            //flag = false;
            Toast.makeText(getApplicationContext(), "블루투스 연결 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
        //Make a connection to the BluetoothSocket
        try {
            mBluetoothSocket.connect();
            Toast.makeText(getApplicationContext(), " 블루투스 connect.", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            try {
                mBluetoothSocket.close();
                Toast.makeText(getApplicationContext(), "socket close.", Toast.LENGTH_LONG).show();
            } catch (IOException e2) {
                Toast.makeText(getApplicationContext(), "connect 오류", Toast.LENGTH_SHORT).show();
                e2.printStackTrace();
                //Log.e(TAG, "unable to close() socket during connection failure", e2);
            }
        }
        mThreadConnectedBluetooth = new ConnectedBluetoothThread(mBluetoothSocket);
        //Toast.makeText(getApplicationContext(), "블루투스 연결 완료.", Toast.LENGTH_LONG).show();
        mThreadConnectedBluetooth.start();
        //Toast.makeText(getApplicationContext(), "블루투스 연결 완료.", Toast.LENGTH_LONG).show();
        mBluetoothHandler.obtainMessage(BT_CONNECTING_STATUS, 1, -1).sendToTarget();
        //Toast.makeText(getApplicationContext(), "블루투스 연결 완료.", Toast.LENGTH_LONG).show();
    }

    private class ConnectedBluetoothThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedBluetoothThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "소켓 연결 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = mmInStream.available();
                    if (bytes != 0) {
                        SystemClock.sleep(100);
                        bytes = mmInStream.available();
                        bytes = mmInStream.read(buffer, 0, bytes);
                        mBluetoothHandler.obtainMessage(BT_MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                    }
                } catch (IOException e) {
                    break;
                }
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "소켓 해제 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
            }
        }

    }


    @Override
    public void onResume()
    {
        super.onResume();

        if (OpenCVLoader.initDebug()) {
            //Log.d(TAG, "onResume :: OpenCV library found inside package. Using it!");
            //m_LoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            baseLoaderCallback.onManagerConnected(baseLoaderCallback.SUCCESS);
        }
    }


    @Override
    public void onPause()
    {
        super.onPause();
        if (m_CameraView != null)
            m_CameraView.disableView();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();

        if (m_CameraView != null)
            m_CameraView.disableView();
    }
    /*
    public native String stringFromJNI();
    */
    @Override
    public void onCameraViewStarted(int width, int height) {
        //카메라 뷰 시작될때
        if (startYolo) {
            String yoloCfg = getPath("custom_yolov3.cfg", this); //핸드폰내 외부 저장소 경로
            String yoloWeights = getPath("custom_yolov3.weights", this);
            yolo = Dnn.readNetFromDarknet(yoloCfg, yoloWeights);

        }
    }

    @Override
    public void onCameraViewStopped() {

    }

    Handler handler1 = new Handler(Looper.getMainLooper());
    Handler handler2 = new Handler(Looper.getMainLooper());
    Handler handler3 = new Handler(Looper.getMainLooper());
    Handler handler4 = new Handler(Looper.getMainLooper());


    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        //matInput = inputFrame.rgba();
        Mat matInput = inputFrame.rgba();

        ImageView onePerson = (ImageView)findViewById(R.id.person1);
        ImageView twoPerson = (ImageView)findViewById(R.id.person2);
        ImageView noHelmet = (ImageView)findViewById(R.id.helmet_n);


        if (startYolo) {
            //Imgproc을 이용해 이미지 프로세싱을 한다.
            Imgproc.cvtColor(matInput, matInput, Imgproc.COLOR_RGBA2RGB);//rgba 체계를 rgb로 변경
            //Imgproc.Canny(frame, frame, 100, 200);
            //Mat gray=Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY)
            Mat imageBlob = Dnn.blobFromImage(matInput, 0.00392, new Size(416, 416), new Scalar(0, 0, 0),false, false);
            //뉴런 네트워크에 이미지 넣기

            yolo.setInput(imageBlob);

            //cfg 파일에서 yolo layer number을 확인하여 이를 순전파에 넣어준다.
            //yolv3는 yolo layer가 3개라서 initialCapacity를 3로 준다.
            java.util.List<Mat> result = new java.util.ArrayList<Mat>(3);

            List<String> outBlobNames = new java.util.ArrayList<>();
            outBlobNames.add(0, "yolo_82");
            outBlobNames.add(1, "yolo_94");
            outBlobNames.add(2, "yolo_106");

            //순전파를 진행
            yolo.forward(result, outBlobNames);

            //30%이상의 확률만 출력해준다.
            float confThreshold = 0.3f;

            //class id
            List<Integer> clsIds = new ArrayList<>();
            List<Float> confs = new ArrayList<>();
            List<Rect> rects = new ArrayList<>();


            for (int i = 0; i < result.size(); ++i) {

                Mat level = result.get(i);

                for (int j = 0; j < level.rows(); ++j) { //iterate row
                    Mat row = level.row(j);
                    Mat scores = row.colRange(5, level.cols());

                    Core.MinMaxLocResult mm = Core.minMaxLoc(scores);


                    float confidence = (float) mm.maxVal;

                    //여러개의 클래스들 중에 가장 정확도가 높은(유사한) 클래스 아이디를 찾아낸다.
                    Point classIdPoint = mm.maxLoc;


                    if (confidence > confThreshold) {
                        int centerX = (int) (row.get(0, 0)[0] * matInput.cols());
                        int centerY = (int) (row.get(0, 1)[0] * matInput.rows());
                        int width = (int) (row.get(0, 2)[0] * matInput.cols());
                        int height = (int) (row.get(0, 3)[0] * matInput.rows());


                        int left = centerX - width / 2;
                        int top = centerY - height / 2;

                        clsIds.add((int) classIdPoint.x);
                        confs.add((float) confidence);

                        rects.add(new Rect(left, top, width, height));
                    }

                }
            }
            int ArrayLength = confs.size();

            if (ArrayLength >= 1) {
                // Apply non-maximum suppression procedure.
                float nmsThresh = 0.2f;


                MatOfFloat confidences = new MatOfFloat(Converters.vector_float_to_Mat(confs));
                Rect[] boxesArray = rects.toArray(new Rect[0]);
                MatOfRect boxes = new MatOfRect(boxesArray);
                MatOfInt indices = new MatOfInt();

                Dnn.NMSBoxes(boxes, confidences, confThreshold, nmsThresh, indices);

                int personCount = 0;
                int helmetCount = 0;

                // Count result:
                int[] ind = indices.toArray();
                for (int i = 0; i < ind.length; ++i) {

                    int idx = ind[i];
                    Rect box = boxesArray[idx];

                    int idGuy = clsIds.get(idx);

                    float conf = confs.get(idx);


                    List<String> classNames = Arrays.asList("Helmet", "Mask", "Face", "Head_with_helmet", "Person", "Person_no_helmet", "Person_with_helmet", "Head");
                    int intConf = (int) (conf * 100);

                    if (classNames.get(idGuy) == "Helmet") {
                        //Imgproc.putText(matInput, classNames.get(idGuy) + " ", box.tl(), Core.FONT_HERSHEY_SIMPLEX, 2, new Scalar(255, 255, 0), 2);
                        Imgproc.rectangle(matInput, box.tl(), box.br(), new Scalar(0, 103, 163), 2);
                        helmetCount += 1;
                    }
                    if (classNames.get(idGuy) == "Mask" || classNames.get(idGuy) == "Face") {
                        Imgproc.rectangle(matInput, box.tl(), box.br(), new Scalar(128, 128, 128), 1);
                        personCount += 1;
                    }

                    //Imgproc.rectangle(matInput, box.tl(), box.br(), new Scalar(0, 0, 255), 1);
                }

                if (personCount >= 2) {
                    handler2.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            onePerson.setVisibility(View.INVISIBLE);
                            twoPerson.setVisibility(View.VISIBLE);
                            //Toast.makeText(getApplicationContext(),"두 명 이상 탑승은 불가능합니다.", Toast.LENGTH_SHORT).show();
                        }
                    }, 0);
                }
                else if (personCount == 1) {
                    handler1.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            onePerson.setVisibility(View.VISIBLE);
                            twoPerson.setVisibility(View.INVISIBLE);
                        }
                    }, 0);
                    if (helmetCount < 1) {
                        handler3.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                noHelmet.setVisibility(View.VISIBLE);
                                //Toast.makeText(getApplicationContext(),"헬멧을 착용해주세요.", Toast.LENGTH_SHORT).show();
                            }
                        }, 0);
                    }
                    else {
                        handler4.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                noHelmet.setVisibility(View.INVISIBLE);
                                //Toast.makeText(getApplicationContext(),"헬멧을 착용해주세요.", Toast.LENGTH_SHORT).show();
                            }
                        }, 0);
                    }
                }

            }
        }

        return matInput; //프레임 리턴
    }
}

