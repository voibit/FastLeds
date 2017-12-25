package net.voidbit.fastleds;

import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Switch;
import org.json.*;
import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.OnColorChangedListener;
import com.flask.colorpicker.OnColorSelectedListener;
import com.flask.colorpicker.slider.AlphaSlider;
import com.loopj.android.http.*;
import com.marcoscg.easylicensesdialog.EasyLicensesDialogCompat;
import java.util.ArrayList;
import cz.msebera.android.httpclient.Header;

public class Connect extends AppCompatActivity {

    static ArrayList<String> patterns;
    static ArrayList<String> palettes;
    int r;
    int g;
    int b;
    String url;
    ColorPickerView colorPickerView;
    static boolean disable;
    Switch power;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        disable=true;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        url=PreferenceManager.getDefaultSharedPreferences(this).getString("URL", "192.168.1.222:80");
        EditText ipedit = findViewById(R.id.editText);
        power = findViewById(R.id.switchPower);
        ipedit.setText(url);
        RestClient.setBaseUrl("http://"+url+"/");

        ipedit.setImeActionLabel(this.getString(R.string.action_connect), KeyEvent.KEYCODE_ENTER);
        ipedit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i==KeyEvent.KEYCODE_ENTER) {
                    connect();
                    InputMethodManager imm = (InputMethodManager) textView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(textView.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });


        /*
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        */

        Button connectButton = findViewById(R.id.connectButton);

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connect();
            }
        });

        patterns = new ArrayList<String>();
        palettes = new ArrayList<String>();

/*
        String json = "{\"patterns\": [\"pat1\",\"pat2\"],\"palettes\":[\"pal1\",\"pal2\"]}";
        try {
            JSONObject jsonTest = new JSONObject(json);
            parseAll(jsonTest);
        }
        catch (JSONException e) {
            Log.e("log_tag", "Error parsing data " + e.toString());
        }
*/

        colorPickerView = findViewById(R.id.color_picker_view);

        colorPickerView.addOnColorChangedListener(new OnColorChangedListener() {
            @Override public void onColorChanged(int selectedColor) {
                // Handle on color change
                String color =Integer.toHexString(selectedColor);
                int light = Integer.parseInt(color.substring(0,2),16);

                r = Integer.parseInt(color.substring(2,4),16);
                g = Integer.parseInt(color.substring(4,6),16);
                b = Integer.parseInt(color.substring(6,8),16);
/*
                Log.d("ColorPicker", "onColorChanged: 0x" + color);
                Log.d("ColorPicker", "onColorChanged: r " + r);
                Log.d("ColorPicker", "onColorChanged: g " + g);
                Log.d("ColorPicker", "onColorChanged: b " + b);
*/
                if (!disable) postRGB(r,g,b);

            }
        });
        power.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (!disable) postValue("power",b?1:0);
            }
        });
        /*
        colorPickerView.addOnColorSelectedListener(new OnColorSelectedListener() {
            @Override
            public void onColorSelected(int selectedColor) {
                Toast.makeText(
                        Connect.this,
                        "selectedColor: " + Integer.toHexString(selectedColor).toUpperCase(),
                        Toast.LENGTH_SHORT).show();
            }
        });
        */

    }

    private void connect() {
        EditText ipedit = findViewById(R.id.editText);
        String ip  = ipedit.getText().toString();
        RestClient.setBaseUrl("http://"+ip+"/");

        //Snackbar.make(findViewById(android.R.id.content), "Connecting to: "+ip,Snackbar.LENGTH_SHORT);
        try {
            getAllData();
        }
        catch (JSONException e) {
            //Log.e("log_tag", "Error parsing data " + e.toString());
        }
        PreferenceManager.getDefaultSharedPreferences(this).edit().putString("URL", ip).apply();

    }

    static public void jsonToArrayList(JSONArray j, ArrayList<String> a) throws JSONException{
        for(int i=0; i < j.length(); i++) {
            a.add(j.getString(i));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_connect, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_about) {
            about();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void about() {
        new EasyLicensesDialogCompat(this)
                .setTitle(R.string.app_about)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    public void parseAll(JSONObject data) {
        int color=0;
        float alpha =0;
        int patternIndex=0;
        int paletteIndex=0;
        patterns.clear();
        palettes.clear();
        disable=true;
        try {
            jsonToArrayList(data.getJSONArray("patterns"), patterns);
            jsonToArrayList(data.getJSONArray("palettes"), palettes);
            patternIndex=data.getJSONObject("pattern").getInt("index");
            paletteIndex=data.getJSONObject("palette").getInt("index");
            color=65536*data.getJSONObject("solidColor").getInt("r");
            color+=256*data.getJSONObject("solidColor").getInt("g");
            color+=data.getJSONObject("solidColor").getInt("b");
            alpha = data.getInt("brightness");
            power.setChecked(data.getInt("power")==1);

        }
        catch (JSONException e) {
            Log.e("Json", "Error parsing data " + e.toString());
        }

        ColorPickerView colorPickerView = findViewById(R.id.color_picker_view);
        colorPickerView.setColor(color,false);
        colorPickerView.setAlphaValue(alpha/255);

        ArrayAdapter<String> patternsAdapter = new ArrayAdapter<>(this,
                R.layout.spinner_item, patterns);
        patternsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner patternS = findViewById(R.id.pattern);
        patternS.setSelection(patternIndex);
        patternS.setAdapter(patternsAdapter);

        ArrayAdapter<String> palettesAdapter = new ArrayAdapter<>(this,
                R.layout.spinner_item, palettes);
        palettesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner paletteS = findViewById(R.id.palette);
        paletteS.setSelection(paletteIndex);
        paletteS.setAdapter(palettesAdapter);

        paletteS.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (!disable) postValue("palette",i);
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
        patternS.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (!disable) postValue("pattern",i);
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        Toast.makeText(Connect.this, R.string.response_connected, Toast.LENGTH_SHORT).show();
        CountDownTimer timer = new CountDownTimer(1000,1000) {
            public void onTick(long millisUntilFinished) {
            }
            public void onFinish() {
                disable=false;
            }
        };
        timer.start();
    }


     public void getAllData() throws JSONException{
        RestClient.get("all", null, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                // Pull out the first event on the public timeline
                //JSONObject firstEvent = timeline.get(0);
                parseAll(response);
            }
            @Override
            public void onFailure(int code, Header[] header, Throwable e, JSONObject obj) {
                Toast.makeText(Connect.this, R.string.response_failed, Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onRetry(int retryNo) {
                Toast.makeText(Connect.this, R.string.response_retry+" "+retryNo, Toast.LENGTH_SHORT).show();
            }
        });
    }//getAlldata

    public void postRGB(int r, int g, int b) {
        RequestParams params = new RequestParams();
        params.put("r", r);
        params.put("g", g);
        params.put("b", b);
        RestClient.post("solidColor", params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                //Log.e("postRGB","changed color :) ");
            }
            @Override
            public void onFailure(int code, Header[] header, Throwable e, JSONObject obj) {
                //Log.e("postRGB","error.. "+code);
                Toast.makeText(Connect.this, R.string.response_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }
    public void postValue(final String url, int value) {
        RequestParams params = new RequestParams();
        params.put("value", value);
        RestClient.post(url, params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                //Log.e("postValue","changed "+url+" to:"+value);
            }
            @Override
            public void onFailure(int code, Header[] header, Throwable e, JSONObject obj) {
                //Log.e("postValue: "+url,"error.. "+code);
                Toast.makeText(Connect.this, R.string.response_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }
}