package com.dpranantha.asyncmetrics.example.controller.exception

import java.io.Serializable

data class ErrorMessage(val message: String?, val errorCode: Int) : Serializable
