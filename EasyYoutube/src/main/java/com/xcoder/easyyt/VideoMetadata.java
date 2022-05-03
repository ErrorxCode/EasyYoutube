package com.xcoder.easyyt;

public class VideoMetadata {
    public static String[] METADATA_BASIC = new String[]{"snippet"};
    public static String[] METADATA_FULL = new String[]{"snippet","contentDetails", "statistics"};
    public static String[] METADATA_ADVANCE = new String[]{"snippet","statistics"};
    public String title;
    public String categoryId;
    public String description;
    public String thumbnail;
    public String channelId;
    public String channelTitle;
    public long likes;
    public long dislikes;
    public long views;
    public long commentsCount;
    public String duration;
    public String dimension;
    public String caption;
    public boolean isLicenced;
    public String quality;

    @Override
    public String toString() {
        return "VideoMetadata{" +
                "title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", thumbnail='" + thumbnail + '\'' +
                ", channelId='" + channelId + '\'' +
                ", channelTitle='" + channelTitle + '\'' +
                ", likes=" + likes +
                ", dislikes=" + dislikes +
                ", views=" + views +
                ", commentsCount=" + commentsCount +
                ", duration='" + duration + '\'' +
                ", dimension='" + dimension + '\'' +
                ", caption='" + caption + '\'' +
                ", isLicenced=" + isLicenced +
                ", quality='" + quality + '\'' +
                '}';
    }
}
