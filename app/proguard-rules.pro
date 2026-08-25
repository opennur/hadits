# Retrofit and Gson models are accessed through reflection.
-keep class org.opennur.hadits.data.remote.** { *; }

# Keep Room database types and generated lookup names stable.
-keep class org.opennur.hadits.data.local.** { *; }

# WorkManager instantiates this worker from its persisted class name.
-keep class org.opennur.hadits.data.DownloadAllWorker { *; }

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
