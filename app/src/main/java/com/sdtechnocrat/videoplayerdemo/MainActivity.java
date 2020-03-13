package com.sdtechnocrat.videoplayerdemo;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    EditText editTextURL;
    Button button;
    String vimeoUrl;

    ArrayList<ProgressiveMedia> progressiveMediaList;
    ArrayList<String> nameArray;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        editTextURL = findViewById(R.id.editTextURL);
        button = findViewById(R.id.button);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading...");

        button.setOnClickListener((view) -> {
            vimeoUrl = editTextURL.getText().toString();
            if (vimeoUrl.length() == 0) {
                Toast.makeText(this, "Enter valid video url", Toast.LENGTH_SHORT).show();
            } else {
                progressiveMediaList = new ArrayList<>();
                nameArray = new ArrayList<>();
                progressDialog.show();
                getMediaURL(vimeoUrl+"/config");
            }
        });
    }

    private void getMediaURL(String URL) {
        OkHttpClient okHttpClient = OkSingleton.getInstance();

        Request request = new Request.Builder()
                .url(URL)
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                } else {
                    // do something wih the result
                    String responseString = Objects.requireNonNull(response.body()).string();
                    Log.d("TAG", responseString);
                    doJSONParsing(responseString);
                }
            }
        });
    }

    private void doJSONParsing(String string) {
        try {
            JSONObject jsonObject = new JSONObject(string);
            JSONObject requestObj = jsonObject.optJSONObject("request");
            JSONObject filesObj = requestObj.optJSONObject("files");
            JSONArray progressiveArray = Objects.requireNonNull(filesObj).optJSONArray("progressive");

            for (int i = 0; i < progressiveArray.length(); i++) {
                JSONObject videoObj = progressiveArray.getJSONObject(i);

                String quality = videoObj.optString("quality");
                String width = videoObj.optString("width");
                String height = videoObj.optString("height");
                String fps = videoObj.optString("fps");

                String nameString = quality + " (" + width + "x" + height + ") - " + fps + " fps";
                nameArray.add(nameString);

                ProgressiveMedia progressiveMedia = new Gson().fromJson(videoObj.toString(), ProgressiveMedia.class);
                progressiveMediaList.add(progressiveMedia);
            }

            runOnUiThread(() -> {
                if (progressiveMediaList.size() > 0) {
                    showDialog();
                }
                progressDialog.dismiss();
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void showDialog() {

        CharSequence[] videoNames = nameArray.toArray(new CharSequence[nameArray.size()]);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setSingleChoiceItems(videoNames, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String videoUrl = progressiveMediaList.get(which).getUrl();
                Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
                intent.putExtra("videoUrl", videoUrl);
                startActivity(intent);
                dialog.dismiss();
            }
        });

        builder.create().show();
    }
}
