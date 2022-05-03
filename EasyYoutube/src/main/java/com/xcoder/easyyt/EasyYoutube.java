package com.xcoder.easyyt;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtube.model.Comment;
import com.google.api.services.youtube.model.CommentSnippet;
import com.google.api.services.youtube.model.CommentThread;
import com.google.api.services.youtube.model.CommentThreadListResponse;
import com.google.api.services.youtube.model.CommentThreadSnippet;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.Subscription;
import com.google.api.services.youtube.model.SubscriptionListResponse;
import com.google.api.services.youtube.model.SubscriptionSnippet;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;
import com.google.api.services.youtube.model.VideoSnippet;
import com.xcoder.tasks.AsyncTask;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * EasyYoutube is a convenience class for accessing Youtube data API. It provides high-level abstraction over the original
 * google api client library with additional features such as AsyncTask callbacks and higher API quota. All the methods are thread-safe and runs asynchronously. This library
 * uses {@link AsyncTask} library for multi-threading and callbacks.
 *
 * @author Rahil khan
 * @version 2.0
 */
public class EasyYoutube {
    private final Stack<YouTube> stack = new Stack<>();
    private YouTube yt;

    /**
     * Initialize the youtube service with the provided projects credentials. You can pass multiple projects
     * with the refresh tokens of same google account; each fetched using respective project auth credentials.
     * This will increase your quota, as library will automatically change the project when ones quota is reached.
     *
     * @param projects The google cloud console projects auth2 credentials
     * @throws GeneralSecurityException If the credential is wrong or expired or access of the account is revoked.
     */
    public EasyYoutube(Project... projects) throws GeneralSecurityException, IOException {
        NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
        GsonFactory factory = GsonFactory.getDefaultInstance();
        for (Project project : projects) {
            Credential credential = new GoogleCredential.Builder()
                    .setClientSecrets(project.clientId, project.clientSecret)
                    .setJsonFactory(factory)
                    .setTransport(transport)
                    .build()
                    .setRefreshToken(project.refreshToken);

            stack.add(new YouTube(transport, factory, credential));
        }
        yt = stack.pop();
    }

    /**
     * Fetches video's metadata from videos id.
     *
     * @param videoId  The id of the video of which you want to retrieve data.
     * @param metadata The fields in form of array, you wish to retrieve.
     *                 Should be one of {@link VideoMetadata#METADATA_FULL}
     *                 {@link VideoMetadata#METADATA_BASIC} or {@link VideoMetadata#METADATA_ADVANCE}
     * @return A task with {@link VideoMetadata} as result and error as exception
     */
    public AsyncTask<VideoMetadata> getVideo(String videoId, String[] metadata) {
        List<String> fields = Arrays.asList(metadata);
        return AsyncTask.callAsync(() -> {
            HttpResponse response = yt.videos()
                    .list(fields)
                    .setId(Collections.singletonList(videoId))
                    .executeUnparsed();

            if (response.isSuccessStatusCode()) {
                List<Video> videos = response.parseAs(VideoListResponse.class).getItems();
                if (videos.size() == 0)
                    throw new IllegalArgumentException("Video not found or is private for id " + videoId);
                else {
                    Video video = videos.get(0);
                    VideoMetadata info = new VideoMetadata();
                    info.title = video.getSnippet().getTitle();
                    info.description = video.getSnippet().getDescription();
                    info.channelTitle = video.getSnippet().getChannelTitle();
                    info.channelId = video.getSnippet().getChannelId();
                    info.categoryId = video.getSnippet().getCategoryId();
                    info.thumbnail = video.getSnippet().getThumbnails().getHigh().getUrl();
                    if (fields.containsAll(Arrays.asList(VideoMetadata.METADATA_FULL))) {
                        info.duration = video.getContentDetails().getDuration();
                        info.dimension = video.getContentDetails().getDimension();
                        info.caption = video.getContentDetails().getCaption();
                        info.isLicenced = video.getContentDetails().getLicensedContent();
                        info.quality = video.getContentDetails().getDefinition();
                    }
                    if (fields.containsAll(Arrays.asList(VideoMetadata.METADATA_ADVANCE))) {
                        info.dislikes = video.getStatistics().getDislikeCount().longValue();
                        info.likes = video.getStatistics().getLikeCount().longValue();
                        info.views = video.getStatistics().getViewCount().longValue();
                        info.commentsCount = video.getStatistics().getCommentCount().longValue();
                    }
                    return info;
                }
            } else if (response.getStatusCode() == 401) {
                yt = stack.pop();
                return getVideo(videoId, metadata).getResult(5);
            } else
                throw new Exception(response.getStatusMessage());
        });
    }

    /**
     * Comments on a youtube video.
     *
     * @param videoId The id of the video
     * @param comment The comment to post
     * @return AsyncTask of void result.
     */
    public AsyncTask<?> comment(@Nonnull String videoId, @Nonnull String comment) {
        return AsyncTask.callAsync(() -> {
            CommentThread thread = new CommentThread().setSnippet(new CommentThreadSnippet()
                    .setVideoId(videoId)
                    .setTopLevelComment(new Comment().setSnippet(new CommentSnippet().setTextOriginal(comment))));
            HttpResponse response = yt.commentThreads().insert(Collections.singletonList("snippet"), thread).executeUnparsed();
            if (!response.isSuccessStatusCode()) {
                if (response.getStatusCode() == 401) {
                    yt = stack.pop();
                    return comment(videoId, comment);
                } else
                    throw new Exception(response.getStatusMessage());
            }
            return null;
        });
    }

    /**
     * Get top 100 newest comments of a youtube video.
     *
     * @param videoId The id of the video
     * @return AsyncTask of List<Comment> result.
     */
    public AsyncTask<String[]> getComments(@Nonnull String videoId) {
        return AsyncTask.callAsync(() -> {
            HttpResponse response = yt.commentThreads().list(Collections.singletonList("snippet")).setVideoId(videoId).setMaxResults(100L).executeUnparsed();
            if (response.isSuccessStatusCode())
                return response.parseAs(CommentThreadListResponse.class).getItems().stream().map(commentThread -> commentThread.getSnippet().getTopLevelComment().getSnippet().getTextOriginal()).toArray(String[]::new);
            else if (response.getStatusCode() == 401) {
                yt = stack.pop();
                return getComments(videoId).getResult(5);
            } else
                throw new Exception(response.getStatusMessage());
        });
    }

    /**
     * Replies to top 100 newest comments on a youtube video.
     *
     * @param videoId          The id of the video
     * @param reply            The text to reply
     * @param progressListener An interface for monitoring progress and errors
     */
    public void replyAll(@Nonnull String videoId, @Nonnull String reply, @Nonnull ProgressListener progressListener) {
        new Thread(() -> {
            try {
                HttpResponse response = yt.commentThreads().list(Collections.singletonList("id")).setVideoId(videoId).setMaxResults(100L).executeUnparsed();
                if (response.isSuccessStatusCode()) {
                    List<CommentThread> comments = response.parseAs(CommentThreadListResponse.class).getItems();
                    for (int i = 0; i < comments.size(); i++) {
                        CommentThread thread = comments.get(i);
                        Comment comment = new Comment().setSnippet(new CommentSnippet().setTextOriginal(reply).setParentId(thread.getId()));
                        response = yt.comments().insert(Collections.singletonList("snippet"), comment).executeUnparsed();
                        if (response.isSuccessStatusCode())
                            progressListener.onProgress((i * 100) / comments.size());
                        else if (response.getStatusCode() == 401) {
                            yt = stack.pop();
                            replyAll(videoId, reply, progressListener);
                        } else
                            throw new Exception(response.getStatusMessage());
                    }
                    progressListener.onProgress(100);
                } else if (response.getStatusCode() == 401) {
                    yt = stack.pop();
                    replyAll(videoId, reply, progressListener);
                } else
                    throw new Exception(response.getStatusMessage());
            } catch (Exception e) {
                progressListener.onError(e);
            }
        }).start();
    }

    /**
     * Replies to top 100 newest comments for whose your predicate return true. If predicate return false, it will not reply to that comment.
     *
     * @param videoId          The id of the video
     * @param reply            The text to reply
     * @param condition        The predicate to check if the comment should be replied to
     * @param progressListener An interface for monitoring progress and errors
     */
    public void replyMatching(@Nonnull String videoId, @Nonnull String reply, @Nonnull Predicate<String> condition, @Nonnull ProgressListener progressListener) {
        new Thread(() -> {
            try {
                HttpResponse response = yt.commentThreads().list(Collections.singletonList("id,snippet")).setVideoId(videoId).setMaxResults(100L).executeUnparsed();
                if (response.isSuccessStatusCode()) {
                    List<CommentThread> comments = response.parseAs(CommentThreadListResponse.class).getItems();
                    for (int i = 0; i < comments.size(); i++) {
                        CommentThread thread = comments.get(i);
                        Comment comment = new Comment().setSnippet(new CommentSnippet().setTextOriginal(reply).setParentId(thread.getId()));
                        var text = thread.getSnippet().getTopLevelComment().getSnippet().getTextOriginal();
                        if (condition.test(text)) {
                            response = yt.comments().insert(Collections.singletonList("snippet"), comment).executeUnparsed();
                            if (response.isSuccessStatusCode())
                                progressListener.onProgress((i * 100) / comments.size());
                            else if (response.getStatusCode() == 401) {
                                yt = stack.pop();
                                replyAll(videoId, reply, progressListener);
                            } else
                                throw new Exception(response.getStatusMessage());
                        }
                    }
                    progressListener.onProgress(100);
                } else if (response.getStatusCode() == 401) {
                    yt = stack.pop();
                    replyAll(videoId, reply, progressListener);
                } else
                    throw new Exception(response.getStatusMessage());
            } catch (Exception e) {
                progressListener.onError(e);
            }
        }).start();
    }

    /**
     * Updates video title or description of the video.
     *
     * @param videoId     The id of the video
     * @param title       The new title or null if you don't want to change it
     * @param description The new description or null if you don't want to change it
     * @return Empty AsyncTask if successful, otherwise with an exception
     */
    public AsyncTask<?> updateVideoMetadata(@Nonnull String videoId, @Nullable String title, @Nullable String description) {
        return AsyncTask.callAsync(() -> {
            VideoMetadata video = getVideo(videoId, VideoMetadata.METADATA_BASIC).getResult(5);
            HttpResponse response = yt.videos().update(
                    Collections.singletonList("snippet"),
                    new Video().setId(videoId).setSnippet(new VideoSnippet()
                            .setTitle(title == null ? video.title : title)
                            .setCategoryId(video.categoryId)
                            .setDescription(description == null ? video.description : description)))
                    .executeUnparsed();

            if (!response.isSuccessStatusCode()) {
                if (response.getStatusCode() == 401) {
                    yt = stack.pop();
                    return updateVideoMetadata(videoId, title, description);
                } else
                    throw new Exception(response.getStatusMessage());
            }
            return null;
        });
    }

    /**
     * Gets the youtube channel id of the authorized account.
     *
     * @return The channel id in form of AsyncTask
     */
    public AsyncTask<String> getSelfChannelId() {
        return AsyncTask.callAsync(() -> {
            HttpResponse response = yt.channels().list(Collections.singletonList("id")).setMine(true).executeUnparsed();
            if (response.isSuccessStatusCode()) {
                return response.parseAs(ChannelListResponse.class).getItems().get(0).getId();
            } else {
                if (response.getStatusCode() == 401) {
                    yt = stack.pop();
                    return getSelfChannelId().getResult(5);
                } else
                    throw new Exception(response.getStatusMessage());
            }
        });
    }

    /**
     * Subscribes to a youtube channel
     * @param channelId The channel id of the channel to subscribe to
     * @return AsyncTask with the id of the subscription
     */
    public AsyncTask<String> subscribe(@Nonnull String channelId) {
        SubscriptionSnippet snippet = new SubscriptionSnippet().setResourceId(new ResourceId().setChannelId(channelId));
        Subscription subscription = new Subscription().setSnippet(snippet);
        return AsyncTask.callAsync(() -> {
            HttpResponse response = yt.subscriptions().insert(Collections.singletonList("snippet"),subscription).executeUnparsed();
            if (response.isSuccessStatusCode()) {
                return response.parseAs(Subscription.class).getId();
            } else {
                if (response.getStatusCode() == 401) {
                    yt = stack.pop();
                    return subscribe(channelId).getResult(5);
                } else
                    throw new Exception(response.getStatusMessage());
            }
        });
    }

    /**
     * Unsubscribes from a youtube channel
     * @param subscriptionId The id of the subscription to unsubscribe from
     * @return AsyncTask with the id of the subscription
     */
    public AsyncTask<?> unsubscribe(@Nonnull String subscriptionId) {
        return AsyncTask.callAsync(() -> {
            HttpResponse response = yt.subscriptions().delete(subscriptionId).executeUnparsed();
            if (!response.isSuccessStatusCode()) {
                if (response.getStatusCode() == 401) {
                    yt = stack.pop();
                    return unsubscribe(subscriptionId);
                } else
                    throw new Exception(response.getStatusMessage());
            }
            return null;
        });
    }
}