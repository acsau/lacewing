package com.csa.ttn.firefly1;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.TimeZone;

import static com.google.android.gms.internal.zzagr.runOnUiThread;

public class DgnsFragment extends Fragment{

    private static final String TAG = "DgnsFragment";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_ENABLE_BT = 3;

    // Layout Views
    private TextView trialCodeText;
    private EditText trialCodeEdit;
    public static String trialCode = "";
    private int ambientTmp;
    private Button trialSet;
    private ImageButton init;
    private ImageButton tfp;
    private ImageButton lfp;
    private ImageButton vref;
    private ImageButton reg63;
    private ImageButton calib;
    private ImageButton run;
    private ImageButton load;
    private ImageButton pause;
    private SeekBar seekframe;
    private ImageButton memplay;
    private ImageButton mempause;
    private ImageButton memprev;
    private ImageButton memnext;
    private ImageView spatial;
    private LineChart temporal;
    private Handler handler = new Handler();

    private int width = 78;
    private int height = 56;
    private int pixel = 0;
    private byte[] frameArray = new byte[2*width*height];;
    private int[] colourArray = new int[width*height];
    private int[] firstFrame = new int[width*height];
    private int[] secondFrame = new int[width*height];
    private Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    private double t63 = 140;
    private double B = (Math.log((750/t63)))/(63-26);
    private double A = t63*Math.exp((63*B));

    private long startTime;
    private long elapsedTime;

    private LineDataSet dataSet;
    private LineData lineData;

    // Switches for activating TTN configuration switches
    private boolean swInit = false;
    private boolean swTfp = false;
    private boolean swLfp = false;
    private boolean swVref = false;
    private boolean swCalib = false;
    private boolean calibShow = false;
    private boolean swReg63 = false;
    private boolean regulating = false;
    private boolean runpause = false;
    private boolean frame1 = false;
    private boolean frame2 = false;
    private int frame12 = 0;
    private boolean ttnConfigDone = false;
    public static boolean newexp = false;

    private float VsFullRange = 1023f;
    private float VsDiffRange = 140f;
    private int pbDelay = 10;

    // Used when loading saved data
    private File filePath;
    private ArrayList<Float> timeIntLoad = new ArrayList<Float>();
    private ArrayList<Float> avgVsLoad = new ArrayList<Float>();
    private ArrayList<Double> temperatureLoad = new ArrayList<Double>();
    private byte[] rawVs;
    private byte[] rawVs2;
    private int frameCount = 0;
    private int targetFrame = 0;
    private int exactFrame;
    private int progressChanged = 0;
    private boolean dataLoaded = false;

    public static String tfpFileName = "temperaturefootprint.txt";
    public static String vsChemLoadFileName = "vsChem_raw.txt";
    public static String vsChemFileName = "vsChem_export.csv";
    public static String dataFileName = "data_export.csv";
    static MenuItem temperature;

    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;

    /**
     * String buffer for outgoing messages
     */
    private StringBuffer mOutStringBuffer;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Member object for the chat services
     */
    private BluetoothChatService mChatService = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            FragmentActivity activity = getActivity();
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            activity.finish();
        }
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else if (mChatService == null) {
            setupChat();
        }
    }


    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mChatService != null) {
            mChatService.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dgns, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        // Initialize all layout elements
        trialCodeText = (TextView) view.findViewById(R.id.trial_text);
        trialCodeEdit = (EditText) view.findViewById(R.id.trial_code);
        trialSet = (Button) view.findViewById(R.id.trial_set);
        spatial = (ImageView) getView().findViewById(R.id.ttn_spatial);
        temporal = (LineChart) getView().findViewById(R.id.ttn_temporal);
        init = (ImageButton) view.findViewById(R.id.init);
        tfp = (ImageButton) view.findViewById(R.id.tfp);
        lfp = (ImageButton) view.findViewById(R.id.lfp);
        vref = (ImageButton) view.findViewById(R.id.vref);
        calib = (ImageButton) view.findViewById(R.id.calib);
        reg63 = (ImageButton) view.findViewById(R.id.temp);
        load = (ImageButton) view.findViewById(R.id.load);
        run = (ImageButton) view.findViewById(R.id.start);
        pause = (ImageButton) view.findViewById(R.id.pause);
        seekframe = (SeekBar) view.findViewById(R.id.seekbar);
        memplay = (ImageButton) view.findViewById(R.id.memplay);
        mempause = (ImageButton) view.findViewById(R.id.mempause);
        memprev = (ImageButton) view.findViewById(R.id.memprev);
        memnext = (ImageButton) view.findViewById(R.id.memnext);
        memplay.setEnabled(false);
        mempause.setEnabled(false);
        memprev.setEnabled(false);
        memnext.setEnabled(false);

        ttnConfig();

        trialSet.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                trialCode = trialCodeEdit.getText().toString();
                if (trialCode.matches("")) {
                    Toast.makeText(getActivity(), R.string.tcode_empty, Toast.LENGTH_SHORT).show();
                    return;
                }
                View view2 = getActivity().getCurrentFocus();
                if (view2 != null) {
                    InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view2.getWindowToken(), 0);
                }
                trialCodeEdit.clearFocus();
                newexp = false;

                filePath = new File(Environment.getExternalStorageDirectory(), StartActivity.PARENT_DIRECTORY + "/" +
                        StartActivity.userName + "/" + trialCode );

                if (!filePath.exists()){
                    // mkdirs returns false if path already exists
                    newexp = filePath.mkdirs();
                }
                if (newexp){
                    try {

                        File file1 = new File(filePath, trialCode + "_" + dataFileName);
                        File file2 = new File(filePath, trialCode + "_" + vsChemFileName);
                        File file3 = new File(filePath, trialCode + "_" + vsChemLoadFileName);
                        file1.createNewFile();
                        file2.createNewFile();
                        file3.createNewFile();

                        String header = "Absolute Time,Time Elapsed,Temperature,Vref,Average Output";
                        // Second argument to FileWriter must be set 'true' to enable file appending
                        FileWriter fwInit = new FileWriter(file1.getAbsoluteFile(), true);
                        // Define an output buffer
                        BufferedWriter bwInit = new BufferedWriter(fwInit);
                        // Create the headers for each column
                        bwInit.write(header);
                        bwInit.newLine();
                        // .close automatically flushes buffer before closing
                        bwInit.close();

                        toggleRunLoadMode(true);
                        Toast.makeText(getActivity(), R.string.tcode_ready, Toast.LENGTH_SHORT).show();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else{
                    toggleRunLoadMode(true);
                    Toast.makeText(getActivity(), R.string.tcode_exists, Toast.LENGTH_SHORT).show();
                }
            }
        });

        seekframe.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                progressChanged = progress;
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            public void onStopTrackingTouch(final SeekBar seekBar) {
                if (dataLoaded){
                    targetFrame = progressChanged;
                }
            }
        });

        load.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dataLoaded = false;
                if (trialCode.matches("")) {
                    Toast.makeText(getActivity(), R.string.tcode_empty, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!newexp){

                    seekframe.setProgress(0);
                    timeIntLoad.clear();
                    avgVsLoad.clear();
                    temperatureLoad.clear();

                    toggleRunLoadMode(false);

                    final AlertDialog.Builder loadDataDialogue = new AlertDialog.Builder(getActivity());
                    ProgressBar progressBar = new ProgressBar(getActivity());
                    loadDataDialogue.setView(progressBar);
                    loadDataDialogue.setTitle("Loading saved data");
                    loadDataDialogue.setCancelable(false);

                    final AlertDialog loadData = loadDataDialogue.create();

                    new AsyncTask<Void, Void, Void>(){
                        @Override
                        protected void onPreExecute() {
                            loadData.show();
                            super.onPreExecute();
                        }

                        @Override
                        protected Void doInBackground(Void... params){
                            final File file = new File(filePath, trialCode + "_" + dataFileName);
                            final File fileVs = new File(filePath, trialCode + "_" + vsChemLoadFileName);

                            try {
                                frameCount = 0;
                                FileReader fs = new FileReader(file);
                                BufferedReader br = new BufferedReader(fs);
                                // Skip the header and empty line
                                br.readLine();
                                br.readLine();

                                // Start here
                                String line;
                                /* Split the row string by recognizing "," from csv formats
                                 * This separates the cells into String[] arrays
                                 * Ensure entries are not null before reading
                                 */
                                while ((line = br.readLine()) != null){
                                    timeIntLoad.add(Float.parseFloat(line.split(",")[1]));
                                    avgVsLoad.add(Float.parseFloat(line.split(",")[4]));
                                    temperatureLoad.add(Double.parseDouble(line.split(",")[2]));
                                    frameCount++;
                                }
                                br.close();

                                seekframe.setMax(frameCount-1);

                                rawVs = new byte[width*height*2];
                                rawVs2 = new byte[width*height*2];

                                FileInputStream fiData = new FileInputStream(fileVs.getAbsoluteFile());
                                fiData.read(rawVs);
                                fiData.skip(20*2*4368);
                                fiData.read(rawVs2);
                                fiData.close();

                            }catch(IOException e) {
                                e.printStackTrace();
                            }

                            frameArray = Arrays.copyOfRange(rawVs, 0,8736);

                            for (pixel = 0; pixel<4368; pixel++){
                                int px = ((frameArray[pixel*2] & 0xFF) << 8)|(frameArray[(pixel*2)+1] & 0xFF);
                                firstFrame[pixel] = px;
                                colourArray[pixel] = convertColour(px, VsFullRange);
                            }

                            frameArray = Arrays.copyOfRange(rawVs2, 0,8736);

                            for (pixel = 0; pixel<4368; pixel++){
                                int px = ((frameArray[pixel*2] & 0xFF) << 8)|(frameArray[(pixel*2)+1] & 0xFF);
                                secondFrame[pixel] = px;
                                colourArray[pixel] = convertColour(px, VsFullRange);
                            }

                            // Load the average readout curve (static)
                            ArrayList<Entry> avgData = new ArrayList<Entry>();

                            for (int i=0; i<frameCount; i++){
                                avgData.add(new Entry(timeIntLoad.get(i), avgVsLoad.get(i)));
                            }

                            LineDataSet avgVs = new LineDataSet(avgData, "Label"); // add entries to dataset

                            avgVs.setAxisDependency(YAxis.AxisDependency.RIGHT);
                            avgVs.setDrawValues(false);
                            avgVs.setColor(R.color.imperialBlue);
                            avgVs.setDrawCircles(false);
                            final LineData avgVsLine = new LineData(avgVs);

                            final String tempnow = "Temp: " + (int)(Math.round(temperatureLoad.get(0))) + "\u2103";
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    //must be called from the UI thread
                                    temporal.setData(avgVsLine);
                                    temporal.notifyDataSetChanged();
                                    temporal.setTouchEnabled(true);
                                    temporal.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
                                    temporal.getXAxis().setLabelCount(6, true);
                                    temporal.getLegend().setEnabled(true);
                                    temporal.getAxisRight().setLabelCount(5, true);
                                    temporal.getAxisLeft().setEnabled(false);
                                    temporal.getDescription().setEnabled(false);
                                    temporal.getLegend().setEnabled(false);
                                    temporal.invalidate(); // refresh line chart

                                    bmp.setPixels(colourArray, 0, width, 0, 0, width, height);
                                    Bitmap dispBmp = Bitmap.createScaledBitmap(bmp, width*11, height*11, false);
                                    spatial.setImageBitmap(dispBmp);

                                    temperature.setTitle(tempnow);
                                }
                            });

                            dataLoaded = true;
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Void result) {
                            loadData.dismiss();
                            super.onPostExecute(result);
                        }
                    }.execute();
                }
            }
        });

        run.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (trialCode.matches("")) {
                    Toast.makeText(getActivity(), R.string.tcode_empty, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!newexp){
                    Toast.makeText(getActivity(), R.string.tcode_empty, Toast.LENGTH_SHORT).show();
                    return;
                }

                ttnConfigDone = true;
                if(ttnConfigDone){
                    seekframe.setProgress(0);
                    dataLoaded = false;
                    runpause = true;
                    swTfp = false;
                    regulating = false;
                    calibShow = false;
                    frame1 = true;
                    frame2 = false;
                    frame12 = 0;
                    bluetoothSend("W");
                }
            }
        });
        pause.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ttnConfigDone=true;
                if(ttnConfigDone && newexp){
                    dataLoaded = true;
                    newexp = false;
                    runpause = false;
                    swTfp = false;
                    calibShow = false;
                    bluetoothSend("X");
                    Toast.makeText(getActivity(), R.string.rec_end, Toast.LENGTH_SHORT).show();
                }
            }
        });

        memplay.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                memplay.setEnabled(false);
                mempause.setEnabled(true);
                memplay.setClickable(false);
                mempause.setClickable(true);
                memprev.setEnabled(false);
                memnext.setEnabled(false);
                memprev.setClickable(false);
                memnext.setClickable(false);
                memplay.setVisibility(View.INVISIBLE);
                mempause.setVisibility(View.VISIBLE);

                exactFrame = targetFrame;
                playData(r);
            }
        });
        mempause.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                memplay.setEnabled(true);
                mempause.setEnabled(false);
                memplay.setClickable(true);
                mempause.setClickable(false);
                memprev.setEnabled(true);
                memnext.setEnabled(true);
                memprev.setClickable(true);
                memnext.setClickable(true);
                memplay.setVisibility(View.VISIBLE);
                mempause.setVisibility(View.INVISIBLE);

                handler.removeCallbacks(r);
                targetFrame = exactFrame;
            }
        });
        memprev.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (targetFrame>0){
                    targetFrame--;
                    seekframe.setProgress(targetFrame);
                }
                else {
                    seekframe.setProgress(0);
                }

            }
        });
        memnext.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (targetFrame<(frameCount-1)){
                    targetFrame++;
                    seekframe.setProgress(targetFrame);
                }
                else {
                    seekframe.setProgress(frameCount-1);
                }
            }
        });
    }

    private Runnable r = new Runnable() {
        @Override
        public void run() {
            playData(r);
        }
    };

    private void playData(Runnable r) {
        if (exactFrame<frameCount){
            final File fileVs = new File(filePath, trialCode + "_" + vsChemLoadFileName);
            rawVs = new byte[width*height*2];

            try{
                FileInputStream fiData = new FileInputStream(fileVs.getAbsoluteFile());
                fiData.skip(exactFrame*2*4368);
                fiData.read(rawVs);
                fiData.close();
            }catch(IOException e) {
                e.printStackTrace();
            }

            frameArray = rawVs;
            for (pixel = 0; pixel < 4368; pixel++) {
                // frameArray is int[], 32-bit structure; readBuf is byte[], 8-bit 2's complement signed structure
                // Create unsigned value for colour mapping by bit-shifting and concatenating readBuf elements
                int px = ((frameArray[pixel * 2] & 0xFF) << 8) | (frameArray[(pixel * 2) + 1] & 0xFF);
                if ((exactFrame<20)){
                    colourArray[pixel] = convertColour(px, VsFullRange);
                }
                else {
                    colourArray[pixel] = convertColour((px-secondFrame[pixel]+(VsDiffRange/2f)), VsDiffRange);
                }
            }
            final String tempnow = "Temp: " + (int) (Math.round(temperatureLoad.get(exactFrame))) + "\u2103";
            new AsyncTask<Void, Void, Void>() {

                @Override
                protected Void doInBackground(Void... params) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //must be called from the UI thread
                            temperature.setTitle(tempnow);

                            bmp.setPixels(colourArray, 0, width, 0, 0, width, height);
                            Bitmap dispBmp = Bitmap.createScaledBitmap(bmp, width * 11, height * 11, false);
                            spatial.setImageBitmap(dispBmp);
                        }
                    });

                    return null;
                }

            }.execute();

            seekframe.setProgress(exactFrame);

            exactFrame++;
            handler.postDelayed(r, pbDelay);
        }
        else {
            handler.removeCallbacks(r);
            targetFrame=0;
            seekframe.setProgress(targetFrame);
            memplay.setEnabled(true);
            mempause.setEnabled(false);
            memplay.setClickable(true);
            mempause.setClickable(false);
            memprev.setEnabled(true);
            memnext.setEnabled(true);
            memprev.setClickable(true);
            memnext.setClickable(true);
            memplay.setVisibility(View.VISIBLE);
            mempause.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Set up the UI and background operations for chat.
     */
    private void setupChat() {

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(getActivity(), mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }

    private void ttnConfig(){
        init.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ttnConfigDone = false;
                swTfp = false;
                calibShow = false;
                bluetoothSend("Q");
                init.setBackgroundColor(Color.parseColor("#FFDDA5"));
            }
        });
        tfp.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ttnConfigDone = false;
                swTfp = true;
                calibShow = false;
                bluetoothSend("R");
                tfp.setBackgroundColor(Color.parseColor("#FFDDA5"));
            }
        });
        lfp.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ttnConfigDone = false;
                swTfp = false;
                calibShow = false;
                bluetoothSend("S");
                lfp.setBackgroundColor(Color.parseColor("#FFDDA5"));
            }
        });
        vref.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ttnConfigDone = false;
                swTfp = false;
                calibShow = true;
                bluetoothSend("T");
                vref.setBackgroundColor(Color.parseColor("#FFDDA5"));
            }
        });
        calib.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ttnConfigDone = false;
                swTfp = false;
                calibShow = true;
                bluetoothSend("U");
                calib.setBackgroundColor(Color.parseColor("#FFDDA5"));
            }
        });
        reg63.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ttnConfigDone = false;
                swTfp = false;
                calibShow = false;
                getAmbientTemp();
                /*if(regulating){
                    bluetoothSend("C");
                    regulating = false;
                }
                else {
                    getAmbientTemp();
                    regulating = true;
                }*/
            }
        });
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private void bluetoothSend(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(getActivity(), R.string.not_connected, Toast.LENGTH_SHORT).show();
        }
        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);

            // Reset out string buffer to zero
            mOutStringBuffer.setLength(0);
        }
    }

    private int convertColour (float value, float range){

        float[] hsv = new float[3];
        // Span from red = MAX to blue = MIN, ignoring purple
        hsv[0] = (1f-(value/range))*230f;
        hsv[1] = 1f;
        hsv[2] = 0.8f;
        return Color.HSVToColor(hsv);
    }

    private void getAmbientTemp(){
        /* Create a dialog to prompt for location name and disease
         * using two editText in one dialog
         */
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
        final EditText ambientTempGet = new EditText(getContext());

        // set up linear layout that contains an edit text and has padding at the sides
        LinearLayout linearLayout = new LinearLayout(getContext());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        ambientTempGet.setHint("\u0020Enter Ambient Temperature (\u2103)");
        ambientTempGet.setSingleLine();
        ambientTempGet.setInputType(2); //TYPE_CLASS_NUMBER
        linearLayout.addView(ambientTempGet);
        linearLayout.setPadding(50,30,50,30);
        dialogBuilder.setView(linearLayout);
        dialogBuilder.setTitle("\u0020Current Room Temperature");

        dialogBuilder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    ambientTmp = Integer.parseInt(ambientTempGet.getText().toString());
                    if ((ambientTmp>-30)&&(ambientTmp<65)){
                        bluetoothSend("V");
                        int tauNew = ((int)(A*Math.exp(((63 - ((double)ambientTmp-26))*(-B)))));
                        A = t63*Math.exp(((63 - ((double)ambientTmp-26))*B));
                        //temperature.setTitle(String.valueOf(tauNew));
                        byte[] tau63 = new byte[2];
                        tau63[1] = (byte)(tauNew & 0xFF);
                        tau63[0] = (byte)((tauNew >>> 8) & 0xFF);   // Unsigned bitwise right-shift
                        //tau63[1] = (byte)(((int)tauNew >> 16) & 0xFF);
                        //tau63[0] = (byte)(((int)tauNew >> 24) & 0xFF);

                        // Send the corresponding tau_63 to PXE
                        mChatService.write(tau63);

                        // Reset out string buffer to zero
                        mOutStringBuffer.setLength(0);

                        reg63.setBackgroundColor(Color.parseColor("#FFDDA5"));
                    }
                    else {
                        Toast.makeText(getActivity(), "Please enter a valid temperature to begin experiments", Toast.LENGTH_SHORT).show();
                    }
                } catch(NumberFormatException nfe) {
                    Toast.makeText(getActivity(), "Please enter a valid temperature to begin experiments", Toast.LENGTH_SHORT).show();
                }
            }
        });
        dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(getActivity(), "Please enter a valid temperature to begin experiments", Toast.LENGTH_SHORT).show();
            }
        });
        dialogBuilder.show();
    }

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            FragmentActivity activity = getActivity();
            switch (msg.what) {
                case Constants.MESSAGE_READ:
                    final byte[] readBuf = (byte[]) msg.obj;
                    if (ttnConfigDone){
                        if (runpause){
                            if (frame1){
                                startTime = System.currentTimeMillis();
                                new AsyncTask<Void, Void, Void>(){
                                    @Override
                                    protected Void doInBackground(Void... params){
                                        final float secs = 0;
                                        final StringBuffer vschem = new StringBuffer();

                                        float readavg = 0;
                                        float readctr = 0;
                                        for (pixel = 0; pixel<4368; pixel++){
                                            // frameArray is int[], 32-bit structure; readBuf is byte[], 8-bit 2's complement signed structure
                                            // Create unsigned value for colour mapping by bit-shifting and concatenating readBuf elements
                                            int px = ((readBuf[pixel*2] & 0xFF) << 8)|(readBuf[(pixel*2)+1] & 0xFF);
                                            vschem.append(px);
                                            vschem.append(",");
                                            colourArray[pixel] = convertColour(px, VsFullRange);

                                            if ((px!=0) && (px!=1023)){
                                                readavg += px;
                                                readctr++;
                                            }
                                        }

                                        final float thisavg = readavg / readctr;

                                        double out = (((readBuf[8738] & 0xFF) << 8)|(readBuf[8739] & 0xFF));
                                        final double tempnew = (-1/B)*(Math.log(out/A));

                                        double vref_dac = (((readBuf[8736] & 0xFF) << 8)|(readBuf[8737] & 0xFF));
                                        final double vref = (vref_dac/1023)*10-5;

                                        final String tempnow = "Temp: " + (int)Math.round(tempnew) + "\u2103";

                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                //must be called from the UI thread

                                                ArrayList<Entry> entries = new ArrayList<Entry>();
                                                dataSet = new LineDataSet(entries, "Label"); // add entries to dataset
                                                dataSet.addEntry(new Entry(0, thisavg));
                                                dataSet.setDrawValues(false);
                                                dataSet.setColor(R.color.imperialBlue);
                                                dataSet.setDrawCircles(false);
                                                dataSet.setAxisDependency(YAxis.AxisDependency.RIGHT);
                                                lineData = new LineData(dataSet);
                                                temporal.setTouchEnabled(true);
                                                temporal.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
                                                temporal.getXAxis().setLabelCount(6, true);
                                                temporal.getLegend().setEnabled(true);
                                                temporal.getAxisRight().setLabelCount(5, true);
                                                temporal.getAxisLeft().setEnabled(false);
                                                temporal.getDescription().setEnabled(false);
                                                temporal.getLegend().setEnabled(false);
                                                temporal.setData(lineData);
                                                temporal.notifyDataSetChanged();
                                                temporal.getAxisRight().setAxisMinimum(dataSet.getYMin()-10f);
                                                temporal.getAxisRight().setAxisMaximum(dataSet.getYMax()+10f);
                                                temporal.invalidate(); // refresh line chart

                                                temperature.setTitle(tempnow);

                                                bmp.setPixels(colourArray, 0, width, 0, 0, width, height);
                                                Bitmap dispBmp = Bitmap.createScaledBitmap(bmp, width*11, height*11, false);
                                                spatial.setImageBitmap(dispBmp);
                                            }
                                        });

                                    /* Get the current date and time in GMT
                                    * Calendar outputs in integers, convert to string
                                    * Maintain double digit format by appending zero when needed, e.g. 01/03/2017 at 05:07:08
                                    */
                                        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
                                        // Add 1 to returned value of MONTH since Jan = 0 and Dec = 11
                                        int monthOfYear = c.get(Calendar.MONTH) + 1;
                                        String day = c.get(Calendar.DAY_OF_MONTH) < 10 ? "0" + c.get(Calendar.DAY_OF_MONTH) : c.get(Calendar.DAY_OF_MONTH) + "";
                                        String month = monthOfYear < 10 ? "0" + monthOfYear: monthOfYear + "";
                                        String hour = c.get(Calendar.HOUR_OF_DAY) < 10 ? "0" + c.get(Calendar.HOUR_OF_DAY) : c.get(Calendar.HOUR_OF_DAY) + "";
                                        String minute = c.get(Calendar.MINUTE) < 10 ? "0" + c.get(Calendar.MINUTE) : c.get(Calendar.MINUTE) + "";
                                        String second = c.get(Calendar.SECOND) < 10 ? "0" + c.get(Calendar.SECOND) : c.get(Calendar.SECOND) + "";


                                        try {

                                            File file = new File(Environment.getExternalStorageDirectory(), "/" +
                                                    StartActivity.PARENT_DIRECTORY + "/" +
                                                    StartActivity.userName + "/" + trialCode + "/" + trialCode + "_" + vsChemLoadFileName);

                                            // Second argument to FileOutputStream must be set 'false' to enable file overwriting
                                            FileOutputStream foData = new FileOutputStream(file.getAbsoluteFile(), true);
                                            // Store the data into the file
                                            foData.write(readBuf, 0, 8736);
                                            // Release resources associated to the outputStream
                                            foData.close();

                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        try {
                                            File file = new File(Environment.getExternalStorageDirectory(), "/" +
                                                    StartActivity.PARENT_DIRECTORY + "/" +
                                                    StartActivity.userName + "/" + trialCode + "/" + trialCode + "_" + vsChemFileName);

                                            FileWriter fwData = new FileWriter(file.getAbsoluteFile(), true);
                                            BufferedWriter bwData = new BufferedWriter(fwData);
                                            // Start a new line first, to make sure data is stored from column A
                                            bwData.newLine();

                                            bwData.write("" + vschem);

                                            bwData.close();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        try {
                                            File file = new File(Environment.getExternalStorageDirectory(), "/" +
                                                    StartActivity.PARENT_DIRECTORY + "/" +
                                                    StartActivity.userName + "/" + trialCode + "/" + trialCode + "_" + dataFileName);

                                            FileWriter fwData = new FileWriter(file.getAbsoluteFile(), true);
                                            BufferedWriter bwData = new BufferedWriter(fwData);
                                            // Start a new line first, to make sure data is stored from column A
                                            bwData.newLine();

                                            bwData.write(day + "/" + month + "/" + c.get(Calendar.YEAR) +
                                                    " at " + hour + ":" + minute + ":" + second + "," +
                                                    String.valueOf(secs) + "," +
                                                    String.valueOf(tempnew) + "," +
                                                    String.valueOf(vref) + "," +
                                                    String.valueOf(thisavg));

                                            bwData.close();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        return null;
                                    }
                                }.execute();
                                frame12++;
                                frame1 = false;
                                frame2 = true;
                            }
                            else if (frame2){
                                elapsedTime = System.currentTimeMillis() - startTime;
                                new AsyncTask<Void, Void, Void>(){
                                    @Override
                                    protected Void doInBackground(Void... params){
                                        final float secs = (float)(elapsedTime/1000);
                                        final StringBuffer vschem = new StringBuffer();

                                        float readavg = 0;
                                        float readctr = 0;
                                        for (pixel = 0; pixel<4368; pixel++){
                                            // frameArray is int[], 32-bit structure; readBuf is byte[], 8-bit 2's complement signed structure
                                            // Create unsigned value for colour mapping by bit-shifting and concatenating readBuf elements
                                            int px = ((readBuf[pixel*2] & 0xFF) << 8)|(readBuf[(pixel*2)+1] & 0xFF);
                                            vschem.append(px);
                                            vschem.append(",");
                                            secondFrame[pixel] = px;
                                            colourArray[pixel] = convertColour(px, VsFullRange);

                                            if ((px!=0) && (px!=1023)){
                                                readavg += px;
                                                readctr++;
                                            }
                                        }

                                        final float thisavg = readavg / readctr;

                                        double out = (((readBuf[8738] & 0xFF) << 8)|(readBuf[8739] & 0xFF));
                                        final double tempnew = (-1/B)*(Math.log(out/A));

                                        double vref_dac = (((readBuf[8736] & 0xFF) << 8)|(readBuf[8737] & 0xFF));
                                        final double vref = (vref_dac/1023)*10-5;

                                        final String tempnow = "Temp: " + (int)Math.round(tempnew) + "\u2103";

                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                //must be called from the UI thread

                                                dataSet.addEntry(new Entry(secs, thisavg));
                                                lineData.notifyDataChanged();
                                                temporal.notifyDataSetChanged();
                                                temporal.getAxisRight().setAxisMinimum(dataSet.getYMin()-10f);
                                                temporal.getAxisRight().setAxisMaximum(dataSet.getYMax()+10f);
                                                //temporal.getXAxis().setAxisMaximum(dataSet.getXMax()+10f);
                                                temporal.invalidate(); // refresh line chart

                                                temperature.setTitle(tempnow);

                                                bmp.setPixels(colourArray, 0, width, 0, 0, width, height);
                                                Bitmap dispBmp = Bitmap.createScaledBitmap(bmp, width*11, height*11, false);
                                                spatial.setImageBitmap(dispBmp);
                                            }
                                        });

                                    /* Get the current date and time in GMT
                                    * Calendar outputs in integers, convert to string
                                    * Maintain double digit format by appending zero when needed, e.g. 01/03/2017 at 05:07:08
                                    */
                                        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
                                        // Add 1 to returned value of MONTH since Jan = 0 and Dec = 11
                                        int monthOfYear = c.get(Calendar.MONTH) + 1;
                                        String day = c.get(Calendar.DAY_OF_MONTH) < 10 ? "0" + c.get(Calendar.DAY_OF_MONTH) : c.get(Calendar.DAY_OF_MONTH) + "";
                                        String month = monthOfYear < 10 ? "0" + monthOfYear: monthOfYear + "";
                                        String hour = c.get(Calendar.HOUR_OF_DAY) < 10 ? "0" + c.get(Calendar.HOUR_OF_DAY) : c.get(Calendar.HOUR_OF_DAY) + "";
                                        String minute = c.get(Calendar.MINUTE) < 10 ? "0" + c.get(Calendar.MINUTE) : c.get(Calendar.MINUTE) + "";
                                        String second = c.get(Calendar.SECOND) < 10 ? "0" + c.get(Calendar.SECOND) : c.get(Calendar.SECOND) + "";


                                        try {

                                            File file = new File(Environment.getExternalStorageDirectory(), "/" +
                                                    StartActivity.PARENT_DIRECTORY + "/" +
                                                    StartActivity.userName + "/" + trialCode + "/" + trialCode + "_" + vsChemLoadFileName);

                                            // Second argument to FileOutputStream must be set 'false' to enable file overwriting
                                            FileOutputStream foData = new FileOutputStream(file.getAbsoluteFile(), true);
                                            // Store the data into the file
                                            foData.write(readBuf, 0, 8736);
                                            // Release resources associated to the outputStream
                                            foData.close();

                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        try {
                                            File file = new File(Environment.getExternalStorageDirectory(), "/" +
                                                    StartActivity.PARENT_DIRECTORY + "/" +
                                                    StartActivity.userName + "/" + trialCode + "/" + trialCode + "_" + vsChemFileName);

                                            FileWriter fwData = new FileWriter(file.getAbsoluteFile(), true);
                                            BufferedWriter bwData = new BufferedWriter(fwData);
                                            // Start a new line first, to make sure data is stored from column A
                                            bwData.newLine();

                                            bwData.write("" + vschem);

                                            bwData.close();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        try {
                                            File file = new File(Environment.getExternalStorageDirectory(), "/" +
                                                    StartActivity.PARENT_DIRECTORY + "/" +
                                                    StartActivity.userName + "/" + trialCode + "/" + trialCode + "_" + dataFileName);

                                            FileWriter fwData = new FileWriter(file.getAbsoluteFile(), true);
                                            BufferedWriter bwData = new BufferedWriter(fwData);
                                            // Start a new line first, to make sure data is stored from column A
                                            bwData.newLine();

                                            bwData.write(day + "/" + month + "/" + c.get(Calendar.YEAR) +
                                                    " at " + hour + ":" + minute + ":" + second + "," +
                                                    String.valueOf(secs) + "," +
                                                    String.valueOf(tempnew) + "," +
                                                    String.valueOf(vref) + "," +
                                                    String.valueOf(thisavg));

                                            bwData.close();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        return null;
                                    }
                                }.execute();
                                frame12++;
                                if (frame12==20) {
                                    frame2 = false;
                                }
                            }
                            else{
                                elapsedTime = System.currentTimeMillis() - startTime;
                                new AsyncTask<Void, Void, Void>(){
                                    @Override
                                    protected Void doInBackground(Void... params){
                                        final float secs = (float)(elapsedTime/1000);
                                        final StringBuffer vschem = new StringBuffer();

                                        float readavg = 0;
                                        float readctr = 0;
                                        for (pixel = 0; pixel<4368; pixel++){
                                            // frameArray is int[], 32-bit structure; readBuf is byte[], 8-bit 2's complement signed structure
                                            // Create unsigned value for colour mapping by bit-shifting and concatenating readBuf elements
                                            int px = ((readBuf[pixel*2] & 0xFF) << 8)|(readBuf[(pixel*2)+1] & 0xFF);
                                            vschem.append(px);
                                            vschem.append(",");
                                            colourArray[pixel] = convertColour((px-secondFrame[pixel]+(VsDiffRange/2f)), VsDiffRange);

                                            if ((px!=0) && (px!=1023)){
                                                readavg += px;
                                                readctr++;
                                            }
                                        }

                                        final float thisavg = readavg / readctr;

                                        double out = (((readBuf[8738] & 0xFF) << 8)|(readBuf[8739] & 0xFF));
                                        final double tempnew = (-1/B)*(Math.log(out/A));

                                        double vref_dac = (((readBuf[8736] & 0xFF) << 8)|(readBuf[8737] & 0xFF));
                                        final double vref = (vref_dac/1023)*10-5;

                                        final String tempnow = "Temp: " + (int)Math.round(tempnew) + "\u2103";



                                    /* Get the current date and time in GMT
                                    * Calendar outputs in integers, convert to string
                                    * Maintain double digit format by appending zero when needed, e.g. 01/03/2017 at 05:07:08
                                    */
                                        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
                                        // Add 1 to returned value of MONTH since Jan = 0 and Dec = 11
                                        int monthOfYear = c.get(Calendar.MONTH) + 1;
                                        String day = c.get(Calendar.DAY_OF_MONTH) < 10 ? "0" + c.get(Calendar.DAY_OF_MONTH) : c.get(Calendar.DAY_OF_MONTH) + "";
                                        String month = monthOfYear < 10 ? "0" + monthOfYear: monthOfYear + "";
                                        String hour = c.get(Calendar.HOUR_OF_DAY) < 10 ? "0" + c.get(Calendar.HOUR_OF_DAY) : c.get(Calendar.HOUR_OF_DAY) + "";
                                        String minute = c.get(Calendar.MINUTE) < 10 ? "0" + c.get(Calendar.MINUTE) : c.get(Calendar.MINUTE) + "";
                                        String second = c.get(Calendar.SECOND) < 10 ? "0" + c.get(Calendar.SECOND) : c.get(Calendar.SECOND) + "";


                                        try {

                                            File file = new File(Environment.getExternalStorageDirectory(), "/" +
                                                    StartActivity.PARENT_DIRECTORY + "/" +
                                                    StartActivity.userName + "/" + trialCode + "/" + trialCode + "_" + vsChemLoadFileName);

                                            // Second argument to FileOutputStream must be set 'false' to enable file overwriting
                                            FileOutputStream foData = new FileOutputStream(file.getAbsoluteFile(), true);
                                            // Store the data into the file
                                            foData.write(readBuf, 0, 8736);
                                            // Release resources associated to the outputStream
                                            foData.close();

                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        try {
                                            File file = new File(Environment.getExternalStorageDirectory(), "/" +
                                                    StartActivity.PARENT_DIRECTORY + "/" +
                                                    StartActivity.userName + "/" + trialCode + "/" + trialCode + "_" + vsChemFileName);

                                            FileWriter fwData = new FileWriter(file.getAbsoluteFile(), true);
                                            BufferedWriter bwData = new BufferedWriter(fwData);
                                            // Start a new line first, to make sure data is stored from column A
                                            bwData.newLine();

                                            bwData.write("" + vschem);

                                            bwData.close();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        try {
                                            File file = new File(Environment.getExternalStorageDirectory(), "/" +
                                                    StartActivity.PARENT_DIRECTORY + "/" +
                                                    StartActivity.userName + "/" + trialCode + "/" + trialCode + "_" + dataFileName);

                                            FileWriter fwData = new FileWriter(file.getAbsoluteFile(), true);
                                            BufferedWriter bwData = new BufferedWriter(fwData);
                                            // Start a new line first, to make sure data is stored from column A
                                            bwData.newLine();

                                            bwData.write(day + "/" + month + "/" + c.get(Calendar.YEAR) +
                                                    " at " + hour + ":" + minute + ":" + second + "," +
                                                    String.valueOf(secs) + "," +
                                                    String.valueOf(tempnew) + "," +
                                                    String.valueOf(vref) + "," +
                                                    String.valueOf(thisavg));

                                            bwData.close();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        return null;
                                    }
                                }.execute();
                            }

                        }

                    }
                    if (!ttnConfigDone && !swTfp && !calibShow){
                        switch (readBuf[0]) {
                            case 'a': init.setBackgroundColor(Color.parseColor("#DCF7C3"));
                                swInit = true;
                                break;
                            case 'n': init.setBackgroundColor(Color.parseColor("#FFDCDC"));
                                swInit = false;
                                break;
                            case 'c': lfp.setBackgroundColor(Color.parseColor("#DCF7C3"));
                                swLfp = true;
                                break;
                            case 'f': reg63.setBackgroundColor(Color.parseColor("#DCF7C3"));
                                swReg63 = true;
                                break;
                            case 's':
                                swLfp = false;
                                byte[] tempfootprintload = new byte[988];
                                // Add the entry to MapMarkerLocation.csv in the Firefly folder
                                try {

                                    File file = new File(Environment.getExternalStorageDirectory(), "/" +
                                            StartActivity.PARENT_DIRECTORY + "/" +
                                            StartActivity.userName+ "/" + tfpFileName);

                                    if (!file.exists()) {
                                        Toast.makeText(activity, "No existing temperature footprint", Toast.LENGTH_SHORT).show();
                                        lfp.setBackgroundColor(Color.parseColor("#FFDCDC"));
                                        byte[] tmpend = {(byte)0xFF};
                                        mChatService.write(tmpend);
                                    }else{
                                        FileInputStream fiData = new FileInputStream(file.getAbsoluteFile());
                                        // Store the data into the file
                                        fiData.read(tempfootprintload);
                                        // Release resources associated to the outputStream
                                        fiData.close();

                                        Toast.makeText(activity, "Temperature footprint found", Toast.LENGTH_SHORT).show();

                                        // Send the temperature footprint to Pixie
                                        mChatService.write(tempfootprintload);

                                        // Reset out string buffer to zero
                                        mOutStringBuffer.setLength(0);
                                    }


                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                break;
                        }
                        if(swInit&&swLfp&&swVref&&swCalib&&swReg63) {
                            ttnConfigDone = true;
                            Bitmap okBmp = BitmapFactory.decodeResource(getResources(), R.drawable.ready);
                            spatial.setImageBitmap(okBmp);
                        }
                    }
                    else if (!ttnConfigDone && swTfp && !calibShow){
                        if (readBuf[988] == 'b'){
                            byte[] tempfootprintsave = Arrays.copyOf(readBuf, 988);
                            // Add the entry to temperaturefootprint.txt in the Firefly folder
                            try {

                                File file = new File(Environment.getExternalStorageDirectory(), "/" +
                                        StartActivity.PARENT_DIRECTORY + "/" +
                                        StartActivity.userName+ "/" + tfpFileName);

                                if (!file.exists()) {
                                    file.createNewFile();
                                }
                                // Second argument to FileOutputStream must be set 'false' to enable file overwriting
                                FileOutputStream foData = new FileOutputStream(file.getAbsoluteFile(), false);
                                // Store the data into the file
                                foData.write(tempfootprintsave);
                                // Release resources associated to the outputStream
                                foData.close();

                                Toast.makeText(activity, "Temperature footprint created", Toast.LENGTH_SHORT).show();


                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            tfp.setBackgroundColor(Color.parseColor("#DCF7C3"));
                            swTfp = false;
                        }
                    }
                    else if (!ttnConfigDone && !swTfp && calibShow){
                        for (pixel = 0; pixel<4368; pixel++){
                            // frameArray is int[], 32-bit structure; readBuf is byte[], 8-bit 2's complement signed structure
                            // Create unsigned value for colour mapping by bit-shifting and concatenating readBuf elements
                            colourArray[pixel] = convertColour(((readBuf[pixel*2] & 0xFF) << 8)|(readBuf[(pixel*2)+1] & 0xFF), VsFullRange);
                        }

                        bmp.setPixels(colourArray, 0, width, 0, 0, width, height);
                        Bitmap dispBmp = Bitmap.createScaledBitmap(bmp, width*11, height*11, false);
                        spatial.setImageBitmap(dispBmp);

                        if (readBuf[8736] == 'd'){
                            vref.setBackgroundColor(Color.parseColor("#DCF7C3"));
                            swVref = true;
                        }
                        else if (readBuf[8736] == 'e'){
                            calib.setBackgroundColor(Color.parseColor("#DCF7C3"));
                            swCalib = true;
                        }
                        calibShow = false;
                    }
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != activity) {
                        Toast.makeText(activity, "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(getActivity(), R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
        }
    }

    /**
     * Establish connection with other device
     *
     * @param data   An {@link Intent} with {@link DeviceListActivity#EXTRA_DEVICE_ADDRESS} extra.
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device, secure);
    }

    public void toggleRunLoadMode(boolean state){
        // State true = Run mode

        load.setEnabled(state);
        run.setEnabled(state);
        pause.setEnabled(state);
        load.setClickable(state);
        run.setClickable(state);
        pause.setClickable(state);

        memplay.setEnabled(!state);
        memprev.setEnabled(!state);
        memnext.setEnabled(!state);
        memplay.setClickable(!state);
        memprev.setClickable(!state);
        memnext.setClickable(!state);

        if (state){
            load.setVisibility(View.VISIBLE);
            run.setVisibility(View.VISIBLE);
            pause.setVisibility(View.VISIBLE);
            memplay.setVisibility(View.INVISIBLE);
            memprev.setVisibility(View.INVISIBLE);
            memnext.setVisibility(View.INVISIBLE);
        }
        else {
            load.setVisibility(View.INVISIBLE);
            run.setVisibility(View.INVISIBLE);
            pause.setVisibility(View.INVISIBLE);
            memplay.setVisibility(View.VISIBLE);
            memprev.setVisibility(View.VISIBLE);
            memnext.setVisibility(View.VISIBLE);
        }

        mempause.setEnabled(false);
        mempause.setClickable(false);
        mempause.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        temperature = menu.getItem(0);
        menu.findItem(R.id.bluetooth_scan).setVisible(true);
        menu.findItem(R.id.temperature).setVisible(true);
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.bluetooth_scan) {
            // Launch the DeviceListActivity to see devices and do scan
            Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
            return true;
        }
        else return false;
    }
}
