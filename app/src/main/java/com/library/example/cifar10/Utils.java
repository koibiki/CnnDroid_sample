package com.library.example.cifar10;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class Utils {
    public static void copyDBToSD(Context context, String path, String filename) {
        AssetManager am = context.getAssets();
        try {
            InputStream inputStream = am.open(filename);
            File file = new File(path, filename);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            if (!file.exists()) {
                FileOutputStream fos = new FileOutputStream(file);
                int len = 0;
                byte[] buffer = new byte[1024];
                while ((len = inputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
