package com.library.example.cifar10;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.renderscript.RenderScript;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import messagepack.ParamUnpacker;
import network.CNNdroid;

import static android.graphics.Color.blue;
import static android.graphics.Color.green;
import static android.graphics.Color.red;

public class ImageActivity extends Activity {

    private static final String TAG = "ImageActivity";
    TextView text;
    Button btn;
    RenderScript myRenderScript;
    int imgSize = 0;
    int textSize = 20;
    CNNdroid myConv = null;
    float[][][] mean = null;
    String[] labels;

    private static final String sRoot = Environment.getExternalStorageDirectory().getAbsolutePath();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_image);

        text = (TextView) findViewById(R.id.textView);
        text.setText("Press \"Load Model\" to Load Network Parameters...");
        btn = (Button) findViewById(R.id.button);

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;

        if (width > 1080)
            imgSize = 1000;
        else if (width > 720) {
            imgSize = 500;
            textSize = 15;
        } else {
            imgSize = 300;
            textSize = 10;
        }

        myRenderScript = RenderScript.create(this);

        readLabels();

        new prepareModel().execute(myRenderScript);
    }

    private class prepareModel extends AsyncTask<RenderScript, Void, CNNdroid> {

        ProgressDialog progDailog;
        RelativeLayout layout = (RelativeLayout) findViewById(R.id.layout);

        protected void onPreExecute() {
            text.setText("Loading Model Network Parameters...");
            text.setTextSize(textSize);
            btn.setVisibility(View.GONE);
            layout.setClickable(false);
            layout.setFocusable(false);
            layout.setFocusableInTouchMode(false);
            progDailog = new ProgressDialog(ImageActivity.this);
            progDailog.setMessage("Please Wait...");
            progDailog.setIndeterminate(false);
            progDailog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progDailog.setCancelable(true);
            progDailog.show();
        }

        @Override
        protected CNNdroid doInBackground(RenderScript... params) {
            try {
                myConv = new CNNdroid(myRenderScript, sRoot + "/Cifar10/Cifar10_def.txt");
                ParamUnpacker pu = new ParamUnpacker();
                mean = (float[][][]) pu.unpackerFunction(sRoot + "/Cifar10/mean.msg", float[][][].class);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return myConv;
        }

        protected void onPostExecute(CNNdroid result) {
            text.setText("\n\n\n\nPress \"Take Photo\" to\nCapture an Object Image...");
            text.setTextSize(textSize + 5);
            text.setTextColor(Color.argb(255, 255, 222, 206));
            btn.setText("Take Photo");
            btn.setVisibility(View.VISIBLE);
            progDailog.dismiss();
            layout.setClickable(true);
            layout.setFocusable(true);
            layout.setFocusableInTouchMode(true);
        }
    }


    public void btnClicked(View view) {
        readImage(view);
    }

    private String accuracy(float[] input_matrix, String[] labels, int topk) {
        String result = "";
        int[] max_num = {-1, -1, -1, -1, -1, -1, -1, -1};
        float[] max = new float[topk];
        for (int k = 0; k < topk; ++k) {
            for (int i = 0; i < 8; ++i) {
                if (input_matrix[i] > max[k]) {
                    boolean newVal = true;
                    for (int j = 0; j < topk; ++j)
                        if (i == max_num[j])
                            newVal = false;
                    if (newVal) {
                        max[k] = input_matrix[i];
                        max_num[k] = i;
                    }
                }
            }
        }

        for (int i = 0; i < topk; i++)
            result += labels[max_num[i]] + " , P = " + max[i] * 100 + " %\n\n";
        return result;
    }

    private int mWidthPixel = 32;
    private int mHeightPixel = 32;

    public void readImage(View view) {
        // 初始化输入矩阵
        float[][][][] inputBatch = new float[1][3][mWidthPixel][mHeightPixel];
        ImageView img = (ImageView) findViewById(R.id.imageView);
        TextView text = (TextView) findViewById(R.id.textView);

        // 读取图像源文件 并缩放到预定大小（32）
        Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.airplane);
        Bitmap bmp1 = Bitmap.createScaledBitmap(bmp, imgSize, imgSize, true);
        Bitmap bmp2 = Bitmap.createScaledBitmap(bmp, mWidthPixel, mHeightPixel, false);
        img.setImageBitmap(bmp1);

        long begin = System.currentTimeMillis();

        for (int j = 0; j < mWidthPixel; ++j) {
            for (int k = 0; k < mHeightPixel; ++k) {
                // 读取图像的pixel值，并归一化 means矩阵由网络初始化时读取mean.msg生成
                // 注意生成的矩阵顺序为 [batch, channel, width, height]
                int color = bmp2.getPixel(j, k);
                inputBatch[0][0][k][j] = (float) (android.graphics.Color.blue(color)) - mean[0][j][k];
                inputBatch[0][1][k][j] = (float) (android.graphics.Color.blue(color)) - mean[1][j][k];
                inputBatch[0][2][k][j] = (float) (android.graphics.Color.blue(color)) - mean[2][j][k];
            }
        }

        // 调用网络计算
        float[][] output = (float[][]) myConv.compute(inputBatch);

        Toast.makeText(this, "" + (System.currentTimeMillis() - begin), Toast.LENGTH_SHORT).show();
        text.setText("\n" + accuracy(output[0], labels, 8));
        text.setTextSize(textSize);
    }

    private void readLabels() {
        labels = new String[1000];
        File f = new File(sRoot + "/Cifar10/labels.txt");
        Scanner s = null;
        int iter = 0;
        try {
            s = new Scanner(f);
            while (s.hasNextLine()) {
                String str = s.nextLine();
                labels[iter++] = str;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
