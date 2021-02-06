package com.dpranantha.asyncmetrics.example.adapter.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable

@JsonIgnoreProperties(ignoreUnknown = true)
data class XkcdResponse(
    @JsonProperty("month")
    val month: String = "",
    @JsonProperty("num")
    val num: Int = -1,
    @JsonProperty("link")
    val link: String = "",
    @JsonProperty("year")
    val year: String = "",
    @JsonProperty("news")
    val news: String = "",
    @JsonProperty("safe_title")
    val safeTitle: String = "",
    @JsonProperty("transcript")
    val transcript: String = "",
    @JsonProperty("alt")
    val alt: String = "",
    @JsonProperty("img")
    val img: String = "",
    @JsonProperty("title")
    val title: String = "",
    @JsonProperty("day")
    val day: String = ""
): Serializable
