package com.xcoder.easyyt;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * A mutable modal class representing video of youtube. You can also set values of this object and they will get reflected to original video
 */
public class Video {
    private final JSONObject object;
    private final String ID;
    private final String TOKEN;
    private final String channelID;
    private final String categoryID;
    private final String title;
    private final String description;
    private final String thumbnailURL;
    private final String publishDate;
    private final String channelName;
    private final long likes;
    private final long dislikes;
    private final long commentCount;
    private final String[] tags;
    private String[] commentIDs;
    private String[] comments;

    public Video(String json,String token){
        TOKEN = token;
        try {
            object = new JSONObject(json).getJSONArray("items").getJSONObject(0);
            ID = object.getString("id");

            JSONObject snippet = object.getJSONObject("snippet");
            title = snippet.optString("title");
            categoryID = snippet.optString("categoryId");
            description = snippet.optString("description");
            channelName = snippet.optString("channelTitle");
            publishDate = snippet.optString("publishedAt");
            channelID = snippet.optString("channelId");
            thumbnailURL = snippet.getJSONObject("thumbnails").getJSONObject("high").optString("url");
            tags = toArray(snippet.optJSONArray("tags"));

            JSONObject statistics = object.getJSONObject("statistics");
            likes = statistics.optLong("likeCount");
            dislikes = statistics.optLong("dislikeCount");
            commentCount = statistics.optLong("commentCount");
        } catch (JSONException e) {
            throw new RuntimeException(new YoutubeException("Invalid JSON response. The request was not succeeded","API request failed"));
        }
    }

    public YoutubeFuture<String> setTitle(String title) {
        try {
            JSONObject snippet = new JSONObject()
                    .put("title", title)
                    .put("description", description)
                    .put("categoryId", categoryID);
            JSONObject object = new JSONObject()
                    .put("id", ID)
                    .put("snippet", snippet);
            return EasyYoutube.execute(true,RequestConfig.ENDPOINT_VIDEOS,RequestConfig.METHOD_PUT,object,RequestConfig.PART_SNIPPET);
        } catch (JSONException e) {
            throw new RuntimeException("Something horribly gone wrong. Have you used reflection on this class ?");
        }
    }

    public YoutubeFuture<String> setDescription(String description) {
        try {
            JSONObject snippet = new JSONObject()
                    .put("title", title)
                    .put("description", description)
                    .put("categoryId", categoryID);
            JSONObject object = new JSONObject().put("id", ID).put("snippet",snippet);
            return EasyYoutube.execute(true,RequestConfig.ENDPOINT_VIDEOS,RequestConfig.METHOD_PUT,object,RequestConfig.PART_SNIPPET);
        } catch (JSONException e) {
            throw new RuntimeException("Something horribly gone wrong. Have you used reflection on this class ?");
        }
    }

    public YoutubeFuture<Void> setThumbnail(File thumbnail) {
        YoutubeFuture<Void> youtubeFuture = new YoutubeFuture<>();
        Request request = new Request.Builder()
                .url("https://www.googleapis.com/upload/youtube/v3/thumbnails/set?videoId=" + ID)
                .addHeader("Accept","application/json")
                .addHeader("Content-Type","application/json")
                .addHeader("Authorization","Bearer " + TOKEN)
                .post(RequestBody.create(thumbnail,MediaType.parse("image/png"))).build();
        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                youtubeFuture.exception = new YoutubeException(e.getMessage(),"IOError occurred");
                youtubeFuture.failureListener.onFailed(youtubeFuture.exception);
                youtubeFuture.completeListener.onComplete(youtubeFuture);
                youtubeFuture.phaser.arrive();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful()){
                    youtubeFuture.successListener.onSuccess(null);
                } else {
                    youtubeFuture.exception = new YoutubeException(response.body().string(), response.message());
                    youtubeFuture.failureListener.onFailed(youtubeFuture.exception);
                }
                youtubeFuture.completeListener.onComplete(youtubeFuture);
            }
        });
        return youtubeFuture;
    }


    /**
     * Posts a comment on this video.
     * @param comment Comment text
     * @return A comment resource as a JSON response
     */
    public YoutubeFuture<String> comment(@NotNull String comment){
        try {
            JSONObject snippet = new JSONObject();
            snippet.put("channelId",channelID);
            snippet.put("videoId",ID);
            snippet.put("topLevelComment",new JSONObject().put("snippet",new JSONObject().put("textOriginal",comment)));

            return EasyYoutube.execute(true,RequestConfig.ENDPOINT_COMMENTS,RequestConfig.METHOD_POST,new JSONObject().put("snippet",snippet),RequestConfig.PART_SNIPPET);
        } catch (JSONException e) {
            throw new RuntimeException("Something horribly gone wrong. Have you used reflection on this class ?");
        }
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getThumbnailURL() {
        return thumbnailURL;
    }

    public String getPublishDate() {
        return publishDate;
    }

    public String getChannelName() {
        return channelName;
    }

    public long getLikes() {
        return likes;
    }

    public long getDislikes() {
        return dislikes;
    }

    public long getCommentCount() {
        return commentCount;
    }

    public String[] getTags() {
        return tags;
    }


    /**
     * Gets top 100 comments of the video.
     * @return String[] of comments containing top level comments.
     */
    public String[] getComments() {
        CountDownLatch latch = new CountDownLatch(1);
        EasyYoutube.execute(false,RequestConfig.ENDPOINT_COMMENTS,RequestConfig.METHOD_GET,null,RequestConfig.PART_SNIPPET,"&maxResults=100","&videoId=" + ID).addOnCompleteListener(new YoutubeFuture.OnCompleteListener<String>() {
            @Override
            public void onComplete(YoutubeFuture<String> YoutubeFuture) {
                if (YoutubeFuture.isSuccessful()){
                    try {
                        String json = YoutubeFuture.result;
                        JSONArray items = new JSONObject(json).getJSONArray("items");
                        comments = new String[items.length()];
                        commentIDs = new String[comments.length];
                        for (int i = 0; i < items.length(); i++) {
                            commentIDs[i] = items.getJSONObject(i).getString("id");
                            comments[i] = items.getJSONObject(i).getJSONObject("snippet").getJSONObject("topLevelComment").getJSONObject("snippet").getString("textOriginal");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else
                    comments = new String[0];

                latch.countDown();
            }
        });
        try {
            latch.await();
            return comments;
        } catch (InterruptedException e) {
            e.printStackTrace();
            comments = new String[0];
            return comments;
        }
    }


    /**
     * This method will reply to all the comments that are provided as an argument
     * If any of the comment which is not in top 100s, an exception is thrown.
     * @param reply The text to reply.
     * @param toComments The comments of which to reply, must be in top 100 comments on the video.
     * @throws RuntimeException if any of the comment is not present in the video
     */
    public void reply2Comments(@NotNull String reply,@NotNull String[] toComments){
        List<String> list = Arrays.asList(getComments());
        for (int i = 0; i < toComments.length; i++) {
            if (list.contains(toComments[i])){
                try {
                    JSONObject snippet = new JSONObject();
                    snippet.put("textOriginal",reply);
                    snippet.put("parentId",commentIDs[i]);
                    EasyYoutube.execute(false,RequestConfig.ENDPOINT_REPLY,RequestConfig.METHOD_POST,new JSONObject().put("snippet",snippet),RequestConfig.PART_SNIPPET);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else
                throw new RuntimeException(new YoutubeException("There is no comment like " + toComments[i] + " in top 100s of the video","Invalid comment"));
        }
    }


    private String[] toArray(JSONArray array) throws JSONException {
        if (array == null)
            return new String[]{};

        String[] items = new String[array.length()];
        for (int i = 0; i < array.length(); i++) {
            items[i] = array.optString(i);
        }
        return items;
    }


    @Override
    public String toString() {
        try {
            return object.toString(3);
        } catch (JSONException e) {
            return object.toString();
        }
    }
}
