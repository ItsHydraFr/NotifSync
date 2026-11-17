# NotifSync

Archives every notification on this phone (Room database) and forwards each one to an
[ntfy](https://ntfy.sh) topic. Your laptop and second phone just subscribe to the same topic.

## Build
1. Open this folder in Android Studio (it will create the Gradle wrapper and sync).
2. Run on your phone, or Build > Build APK(s) and sideload.

## Set up
1. Open NotifSync > **Grant notification access** and enable it.
2. Tap **Random topic** (long random name = your secret) or type your own.
3. Second phone: install the ntfy app > Subscribe to the same topic (use the same server).
4. Laptop: open `https://ntfy.sh/<your-topic>` in a browser and allow desktop notifications
   (or install the ntfy desktop client / use `ntfy subscribe <topic>`).
5. Settings > Battery > NotifSync > Unrestricted, so Android doesn't kill the listener.

## Privacy
Notifications can contain OTP codes and private messages. On the public ntfy.sh server anyone who
knows the topic name can read it, so keep it long and random, or self-host ntfy and use an access token.
Do NOT install NotifSync on the receiving phone with the same topic (it would loop).

## Behavior
Skips ongoing notifications (media, charging), group summaries, and duplicate reposts within 3 s.
Unsent items retry automatically when the network returns (WorkManager).
