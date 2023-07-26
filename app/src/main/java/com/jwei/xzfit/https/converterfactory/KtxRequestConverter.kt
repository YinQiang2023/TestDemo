package com.jwei.xzfit.https.converterfactory

import kotlinx.serialization.SerializationStrategy
import okhttp3.MediaType
import okhttp3.RequestBody
import retrofit2.Converter

internal class KtxRequestConverter<T>(
  private val contentType: MediaType,
  private val saver: SerializationStrategy<T>,
  private val serializer: Serializer
) : Converter<T, RequestBody> {
    override fun convert(value: T) = serializer.toRequestBody(contentType, saver, value)
}
