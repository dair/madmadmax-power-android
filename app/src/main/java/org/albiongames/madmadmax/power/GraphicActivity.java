package org.albiongames.madmadmax.power;

import android.app.Activity;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

public class GraphicActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graphic);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        ImageView logo = (ImageView)findViewById(R.id.logoImageView);
        logo.setColorFilter(Color.YELLOW);


    }
}
