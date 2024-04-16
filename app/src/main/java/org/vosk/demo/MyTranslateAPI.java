package org.vosk.demo;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyTranslateAPI {
    String langFrom;
    String langTo;
    ExecutorService executors;

    public void translate(String text) {
        if (executors != null) executors.shutdown();
        executors = Executors.newSingleThreadExecutor();
        executors.execute(() -> {
            Handler mainHandler = new Handler(Looper.getMainLooper());
            try {
                StringBuilder temp = new StringBuilder();
                String url = "https://translate.googleapis.com/translate_a/single?" + "client=gtx&" + "sl=" +
                        langFrom + "&tl=" + langTo + "&dt=t&q=" + URLEncoder.encode(text, "UTF-8");
                JSONArray total = getJsonArray(url);
                for (int i = 0; i < total.length(); i++) {
                    JSONArray currentLine = (JSONArray) total.get(i);
                    temp.append(currentLine.get(0).toString());
                }
                if (temp.length() > 0) {
                    String result = temp.substring(0, 1).toUpperCase() + temp.substring(1);
                    mainHandler.post(() -> listener.onSuccess(result));
                } else {
                    mainHandler.post(() -> listener.onFailure("Invalid Input String"));
                }
            } catch (Exception e) {
                Log.d("AAA", "translate: " + e);
                mainHandler.post(() -> listener.onFailure(e.getMessage()));
            }
        });
    }

    private static JSONArray getJsonArray(String url) throws IOException, JSONException {
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        JSONArray main = new JSONArray(response.toString());
        return (JSONArray) main.get(0);
    }

    public MyTranslateAPI(String langFrom, String langTo) {
        this.langFrom = langFrom;
        this.langTo = langTo;
    }

    private TranslateListener listener;

    public void setTranslateListener(TranslateListener listener) {
        this.listener = listener;
    }

    public interface TranslateListener {
        void onSuccess(String translatedText);

        void onFailure(String ErrorText);
    }

}