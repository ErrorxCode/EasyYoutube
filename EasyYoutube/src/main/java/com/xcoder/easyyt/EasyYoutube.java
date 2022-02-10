package com.xcoder.easyyt;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Cache;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * This class is main class of the library. All the youtube operations are performed from this class. This library does not use or is not a
 * wrapper of that "java client" which google had provided. This is a whole new flavor of "Youtube data API v3" created from scratch,
 * which provides an easy programming interface. Each methods of this class runs asynchronously and returns a future.
 * You can call {@code get()} on that future to wait for response.
 *
 * @author Rahil khan
 * @version 1.0
 */
public class EasyYoutube {
    public static volatile String TOKEN;
    private static String CLIENT_ID;
    private static String CLIENT_SECRET;
    private static String REFRESH_TOKEN;
    private static String[] APIS;
    private static String API;
    private static boolean autoRefresh;
    private static File CACHE;
    private static OkHttpClient client;
    private static ExecutorService service = Executors.newFixedThreadPool(2);

    private EasyYoutube(){
        if (CACHE == null)
            client = new OkHttpClient();
        else
            client = new OkHttpClient.Builder().cache(new Cache(CACHE,5*1024*1024)).build();

        service.execute(() -> {
            try {
                TOKEN = getAccessToken();
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
        });
    }


    /**
     * Gets youtube video asynchronously in the form of java object. This object contains all the details of the video.
     * You can also update or change few of these details from the {@code setX()} method where 'x' is
     * the type of content you want to change.
     * @param id The ID of the video. Can be found in the last parameter of the video link
     * @return {@link YoutubeFuture} A placeholder holding {@link Video} as a result and {@link YoutubeException as an exception}
     */
    public YoutubeFuture<Video> getVideoAsync(@NotNull String id) {
        YoutubeFuture<Video> youtubeFuture = new YoutubeFuture<>();
        execute(true,RequestConfig.ENDPOINT_VIDEOS,RequestConfig.METHOD_GET,null,RequestConfig.PART_SNIP_AND_STATS,"&id=" + id)
                .addOnSuccessListener(result -> youtubeFuture.successListener.onSuccess(new Video(result,TOKEN)))
                .addOnFailureListener(exception -> youtubeFuture.failureListener.onFailed(exception))
                .addOnCompleteListener(YoutubeFuture -> {
                    if (YoutubeFuture.isSuccessful())
                        youtubeFuture.result = new Video(YoutubeFuture.result,TOKEN);
                    else
                        youtubeFuture.exception = YoutubeFuture.exception;

                    youtubeFuture.completeListener.onComplete(youtubeFuture);
                });
        return youtubeFuture;
    }


    /**
     * Gets youtube video synchronously in the form of java object. This object contains all the details of the video.
     * You can also update or change few of these details from the {@code setX()} method where 'x' is
     * the type of content you want to change.
     * @param id The ID of the video. Can be found in the last parameter of the video link
     * @return {@link YoutubeFuture} A placeholder holding {@link Video} as a result and {@link YoutubeException as an exception}
     */
    private Video getVideo(@NotNull String id){
        YoutubeFuture<String> execute = execute(false, RequestConfig.ENDPOINT_VIDEOS, RequestConfig.METHOD_GET, null, RequestConfig.PART_SNIP_AND_STATS, "&id=" + id);
        return new Video(execute.result,TOKEN);
    }


    protected static YoutubeFuture<String> execute(boolean async,String endpoint, String method, @Nullable JSONObject object, String... part) {
        YoutubeFuture<String> YoutubeFuture = new YoutubeFuture<>();
        String query = Arrays.toString(part).replace(", ","").replace("]","").replace("[","");
        String url = "https://www.googleapis.com/youtube/v3/" + endpoint + query + "&key=" + API;
        Request.Builder builder = new Request.Builder()
                .url(url)
                .addHeader("Accept","application/json")
                .addHeader("Content-Type","application/json");

        if (object != null){
            RequestBody body = RequestBody.create(object.toString().getBytes(StandardCharsets.UTF_8));
            builder.addHeader("Authorization","Bearer " + TOKEN);
            switch (method) {
                case "PUT":
                    builder.put(body);
                    break;

                case "GET":
                    builder.get();
                    break;

                case "POST":
                    builder.post(body);
                    break;
            }
        }

        Call call = client.newCall(builder.build());
        Callback callback = new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                YoutubeFuture.exception = new YoutubeException(e.getMessage(), "IO exception occurred");
                YoutubeFuture.completeListener.onComplete(YoutubeFuture);
                YoutubeFuture.failureListener.onFailed(YoutubeFuture.exception);
                YoutubeFuture.phaser.arrive();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    YoutubeFuture.result = response.body().string();
                    YoutubeFuture.successListener.onSuccess(YoutubeFuture.result);
                } else {
                    try {
                        JSONObject object = new JSONObject(response.body().string()).getJSONObject("error");
                        String reason = object.getJSONArray("errors").getJSONObject(0).getString("reason");
                        if (response.code() == 401 && autoRefresh) {
                            TOKEN = getAccessToken();
                            execute(async, endpoint, method, object, part);
                            return;
                        } else if (response.code() == 403 && APIS.length > 1){
                            List<String> list = Arrays.asList(APIS);
                            API = list.get(list.indexOf(API) + 1);
                            execute(async, endpoint, method, object, part);
                            return;
                        } else
                            YoutubeFuture.exception = new YoutubeException(object.getString("message"), reason);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        YoutubeFuture.exception = new YoutubeException("An unknown error occurred.", "Not a JSON response");
                    }
                    YoutubeFuture.failureListener.onFailed(YoutubeFuture.exception);
                }
                YoutubeFuture.completeListener.onComplete(YoutubeFuture);
                YoutubeFuture.phaser.arrive();
            }
        };
        if (async){
            call.enqueue(callback);
        } else {
            service.execute(() -> {
                try {
                    Response response = call.execute();
                    callback.onResponse(call,response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
        return YoutubeFuture;
    }


    /**
     * Gets a new access token from the refresh token.
     * @return A new access token or null if failed to get. The most probable possibility of failing is when an IException occurred
     * or when the response is invalid (does not contain access token)
     */
    protected static String getAccessToken() throws IOException, JSONException {
        String query = String.format("client_id=%1$s&" + "client_secret=%2$s&" + "refresh_token=%3$s&" + "grant_type=refresh_token",CLIENT_ID,CLIENT_SECRET,REFRESH_TOKEN);
        Request request = new Request.Builder().url("https://oauth2.googleapis.com/token?" + query).post(RequestBody.create(null,new byte[0])).build();
        Response response = client.newCall(request).execute();
        if (response.isSuccessful())
            return new JSONObject(response.body().string()).optString("access_token");
        else
            throw new JSONException("Response code : " + response.message());
    }


    public static String getRefreshToken(String code,String clientID,String clientSecret,String redirectUrl) throws YoutubeException {
        try {
            return service.submit(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    try {
                        String query = String.format("client_id=%1$s&" + "client_secret=%2$s&" + "code=%3$s&" + "redirect_uri=%4$s&" +  "grant_type=authorization_code",clientID,clientSecret,code,redirectUrl);
                        Response response = new OkHttpClient().newCall(new Request.Builder().url("https://oauth2.googleapis.com/token?" + query).post(RequestBody.create(null,new byte[0])).build()).execute();
                        if (response.isSuccessful()){
                            JSONObject json = new JSONObject(response.body().string());
                            return json.getString("refresh_token");
                        } else {
                            JSONObject json = new JSONObject(response.body().string());
                            System.out.println(json.toString(3));
                            String message = json.getString("error");
                            throw new YoutubeException(message,"BAD request");
                        }
                    } catch (JSONException | IOException e) {
                        throw new YoutubeException("Initialization failed. Cannot get refresh token due to invalid response or an IO error","Response error");
                    }
                }
            }).get();
        } catch (ExecutionException | InterruptedException e) {
            throw new YoutubeException(e.getLocalizedMessage(),"Unknown reason");
        }
    }


    /**
     * ClientID, ClientSecret,Api key and AuthCode is required. If not set, {@code NullPointerException} is thrown.
     */
    public static class Builder {
        public Builder clientID(String clientId) {
            CLIENT_ID = clientId;
            return this;
        }

        public Builder clientSecret(String clientSecret) {
            CLIENT_SECRET = clientSecret;
            return this;
        }

        public Builder refreshToken(String refreshCode) {
            REFRESH_TOKEN = refreshCode;
            return this;
        }

        /**
         * Sets the okhttp client to use for every request. This client will not use cache until configured to use manually, even if it is enabled.
         * @param client The client to use
         */
        public Builder setClient(OkHttpClient client){
            EasyYoutube.client = client;
            return this;
        }

        public Builder apiKey(String[] apiKeys) {
            APIS = apiKeys;
            API = apiKeys[0];
            return this;
        }

        public Builder enableCache(File cacheDir) {
            CACHE = cacheDir;
            return this;
        }

        /**
         * If called, this library will automatically manage the access token and refresh it whenever needed.
         */
        public Builder enableAutoRefresh(boolean auto){
            autoRefresh = auto;
            return this;
        }

        public EasyYoutube build(){
            if (APIS.length < 1 || CLIENT_ID == null || CLIENT_SECRET == null || REFRESH_TOKEN == null)
                throw new NullPointerException("ClientID, ClientSecret,Api key and REFRESH_TOKEN is required");
            else
                return new EasyYoutube();
        }
    }
}