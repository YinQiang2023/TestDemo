package com.jwei.publicone.utils

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.bitmap_recycle.LruArrayPool
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions
import com.jwei.publicone.base.BaseApplication
import okhttp3.Call
import okhttp3.OkHttpClient
import java.io.InputStream
import java.util.concurrent.TimeUnit

/**
 * Created by Android on 2023/5/13.
 */
@GlideModule
class MyGlideAppModule : AppGlideModule() {
    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        val client: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()
        val factory = OkHttpUrlLoader.Factory(client as Call.Factory)
        registry.replace(GlideUrl::class.java, InputStream::class.java, factory)
    }

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        val calculator: MemorySizeCalculator = MemorySizeCalculator.Builder(BaseApplication.application).build()
        val defaultMemoryCacheSize: Int = calculator.memoryCacheSize
        val defaultBitmapPoolSize: Int = calculator.bitmapPoolSize
        val defaultArrayPoolSize: Int = calculator.arrayPoolSizeInBytes
        builder.setDefaultRequestOptions(

            RequestOptions()
                //在全局设置中将图片质量设置为565，如果遇到显示gif文件的时候，会出现gif图片周边出现黑框的问题，需要在加载图片时候，单独针对gif结尾的url将图片质量改回8888
                /*RequestOptions options = new RequestOptions()
                     .centerCrop()
                 if(!TextUtils.isEmpty(url) && url.endsWith(".gif")) {
                     options.format(DecodeFormat.PREFER_ARGB_8888);
                 }
                 GlideApp.with(context)
                     .load(url)
                     .apply(options)
                     .into(imageView);
                )*/
                .format(DecodeFormat.PREFER_RGB_565)
                //缓存所有不同形状的图片
                .diskCacheStrategy(DiskCacheStrategy.ALL)
        )
        builder.setMemoryCache(LruResourceCache(defaultMemoryCacheSize / 2L))
        builder.setBitmapPool(LruBitmapPool(defaultBitmapPoolSize / 2L))
        builder.setArrayPool(LruArrayPool(defaultArrayPoolSize / 2))
    }
}