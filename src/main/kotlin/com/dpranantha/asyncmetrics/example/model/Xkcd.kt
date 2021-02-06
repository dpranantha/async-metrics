package com.dpranantha.asyncmetrics.example.model

import java.io.Serializable

data class Xkcd(
    val num: Int,
    val img: String,
    val title: String,
    val month: String,
    val year: String,
    val transcript: String
): Serializable
