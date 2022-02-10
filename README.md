
# EasyYoutube ~ Youtube in Java 

EasyYoutube is a java library of **Youtube data API v3**. This library contains almost
all the methods and endpoints of the API. You can edit, see, comment, reply, and can do a lot
more things through this library. Using this is as easy as [EasyInsta](https://github.com/ErrorxCode/EasyInsta).


![image](https://developers.google.com/youtube/images/youtube_home_page_data_api.png)


## Features
- Simple & lightweight
- Easy 2 use
- Async callbacks
- Auto authentication & token refresh
- **Supports** editing title,description and thumbnail
- **Supports** fetching of video metadata
- **Supports** commenting and replying
- **Supports** Uploading video (coming soon)
- **Supports** Many more.... (coming soon)
## Implementation

**Gradle**

In your project `build.gradle`
```groovy
allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```
In your module `buil.gradle` (app)
```groovy
dependencies {
	    implementation 'com.github.ErrorxCode:EasyYoutube:Tag'
	}
```

**Maven**
```xml
<repositories>
		<repository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</repository>
	</repositories>
```

Add the dependency
```xml
<dependency>
	    <groupId>com.github.ErrorxCode</groupId>
	    <artifactId>EasyYoutube</artifactId>
	    <version>Tag</version>
	</dependency>
```

## Acknowledgements

 - [Authentication guide](https://developers.google.com/youtube/v3/guides/authentication)
 - [Quota usage](https://developers.google.com/youtube/v3/determine_quota_cost)
 - [API Terms of service](https://developers.google.com/youtube/terms/api-services-terms-of-service)


## Documentation
- [Youtube docs](https://developers.google.com/youtube/v3/docs)



## API Reference

Before proceeding further, make sure that you have gone through the documentation part.
This reference will introduce you with the use of this library. So let's get started.

### Initializing / Authentincating

To use this library (or API), you first have to authenticate it from your (or users)
Google account to get `access token`. On Android, you have to use [Google sign in](https://developers.google.com/identity/sign-in/android/start-integrating).
for other platforms, see [Server side authentication](https://developers.google.com/youtube/v3/guides/auth/server-side-web-apps).

The example below shows a way to get authentication code from a google account on android.
```java
GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(new Scope("https://www.googleapis.com/auth/youtube.force-ssl"))
            .requestServerAuthCode(YOUR_WEB_CLIENT_ID)
            .requestEmail()
            .build();

Intent signInIntent = GoogleSignIn.getClient(this, options).getSignInIntent();
startActivityForResult(signInIntent,200);
```
Then in `onActivityResult()`, store the code in `authCode` variable:
```java
@Override
protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == 200){
        GoogleSignIn.getSignedInAccountFromIntent(data)
                .addOnFailureListener(System.out::println)
                .addOnSuccessListener(account -> authCode = account.getServerAuthCode());
    }
}
```
Now get the `refresh token` from that auth code and then initialize the `EasyYoutube` object passing it:

**Tip :** You can use convinience static `getRefreshToken()` method to get refresh code.

```java
EasyYoutube youtube = new EasyYoutube.Builder()
        .apiKey(new String[]{KEY1,KEY2})  // Pass more keys to increase quata
        .clientID(YOUR_WEB_CLIENT_ID)
        .clientSecret(YOUR_WEB_CLIENT_SECRET)
        .refreshToken(REFRESH_TOKEN)
        .enableAutoRefresh(true)     // This will take care of token expiration 
        .enableCache(CACHE_DIR)
        .build();
```
Here, `KEY1` & `KEY2` are the API key of 2 different google cloud accounts. You can pass 'n'
no. of keys. More the keys, more the quota. The library will automatically rotate keys when one's quota is reached.

### Callbacks & Futures.
Every async method of this library returns a `YoutubeFuture` object. You can use that future in many ways.
*Like this:*
```java
youtubeFuture
    .addOnSuccessListener(System.out::println)
    .addOnFailureListener(Throwable::printStackTrace);


// or


youtubeFuture.addOnCompleteListener(YoutubeFuture1 -> {
    if (YoutubeFuture.isSuccessful()){
        Object result = YoutubeFuture.getResult();
    } else
        YoutubeFuture.getException().printStackTrace();
});
```

### Getting video
**Note :** All the methods runs asynchronously. Use java concurrent technique's to make it synchronous
, such as [CountdownLatch](https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/CountDownLatch.html)
```java
youtube.getVideoAsync(VIDEO_ID).addOnCompleteListener(new YoutubeFuture.OnCompleteListener<Video>() {
    @Override
    public void onComplete(YoutubeFuture<Video> YoutubeFuture) {
        if (YoutubeFuture.isSuccessful()){
            Video video = YoutubeFuture.getResult();
            // This video object contains information about the video.
        } else
            YoutubeFuture.getException().printStackTrace();
    }
});
```

Now with that `video`, you can do anything. Like getting or changing the title and description of the video
**Example**:
```java
String channel = video.getChannelName();
String thumbnail = video.getThumbnailURL();
String[] comments = video.getComments();   // Brings top 100 comments of the video
long likes = video.getLikes();
... // And lot more.
```
You can also change a few of the properties of the video, and that will actually be
reflected the original video on youtube.

**Example:**
```java
video.setTitle("New title");
video.setDescription("New description");
video.setThumbnail(new File("thumbnail.jpg"));
```
**Warning:** Remember! You can only call these methods on your video.
This means that the google account used to authorize must be the owner or uploader of the video.


### Replying and commenting
**To comment on a video** :
```java
video.comment("Hey there !")
        .addOnFailureListener(Throwable::printStackTrace)
        .addOnSuccessListener(System.out::println);
```
**To reply of a (or many) comment** :
```java
video.reply2Comments("EasyYoutube library is awsome",new String[]{"Hi"});
```
Here, The second argument is of the array of the comments you wish to reply.
Pass that every comment of which you want to reply.

**Tip :** Pass `video.getComments()` to reply to all comments.


*Currently this library supports limited features. More features will be added in further releases.*
## FAQ

#### Q: Can we make bots out of this library ?

Ans : Of course, you can. But remember the [guidlines](https://developers.google.com/youtube/terms/developer-policies#iii.-general-developer-policies)

#### Q: Can we use it on platforms other than android ?

Ans : Maybe. Altho this library is a .aar file, It doesn't include any android class.
But I am not sure whether it will work on other platforms or not. If it doesn't, create
an issue and I will make it a .jar that can be used on any platform.

#### Q: Does it require your youtube account username & password ?
Ans: No, This only requires an access token of that account. Furthermore, access token
requires user consent about their youtube account being managed.


## Contributing

Contributions are always welcome!

The `code of conduct is the same as always. Please make a pull request or issue
regarding any change or update in the code.


## Feedback

If you have any feedback, please reach out to us at inboxrahil@xcoder.tk
Thanks for using our library. If it helped you, Just give it a star.
