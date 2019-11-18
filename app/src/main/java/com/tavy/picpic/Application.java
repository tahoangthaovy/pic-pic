package com.tavy.picpic;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import ly.img.android.PESDK;

/**
 * Created by user on 6/30/2018.
 */

public class Application extends android.app.Application {

    @Override
    public void onCreate() {
        super.onCreate();

        PESDK.init(this);
    }

}
