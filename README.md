
# EasyYoutube ~ Youtube in Java 

EasyYoutube is a convinince wrapper of java client for **Youtube data API v3**. This library simplify the use of API upto just one method call.
You can edit, see, comment, reply, and can do a lot more things through this library. Using this is as easy as [EasyInsta](https://github.com/ErrorxCode/EasyInsta).


![image](https://developers.google.com/youtube/images/youtube_home_page_data_api.png)


## Features
- Simple & lightweight
- Easy 2 use
- Async callbacks
- Auto authentication & token refresh
- Increased API quota / quota limit bypassed
- **Supports** editing title,description and thumbnail (soon)
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
	    implementation 'com.github.ErrorxCode:EasyYoutube:VERSION_HERE'
	}
```
[![](https://jitpack.io/v/ErrorxCode/EasyYoutube.svg)](https://jitpack.io/#ErrorxCode/EasyYoutube)

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
- [User guide](https://developers.google.com/youtube/v3/docs)


## FAQ

#### Q: Can we make bots out of this library ?

Ans : Of course, you can. But remember the [guidlines](https://developers.google.com/youtube/terms/developer-policies#iii.-general-developer-policies)

#### Q: Can we use it on platforms other than android ?

Ans : Yes this library is now platform independent. You can use it in any java application.

#### Q: Does it require your google account username & password ?
Ans: No, This only requires an refresh token of that account. Furthermore, refresh token
requires user consent about their youtube account being managed.


## Contributing

Contributions are always welcome!

The `code of conduct` is the same as always. What you can do is you can add more methods from [google api client](https://github.com/googleapis/google-api-java-client-services/tree/main/clients/google-api-services-youtube/v3) to this library. Please first make a pull request or issue
regarding any change or update in the code.


## Feedback

If you have any feedback, please reach out to us at inboxrahil@xcoder.tk
Thanks for using our library. If it helped you, Just give it a star.
