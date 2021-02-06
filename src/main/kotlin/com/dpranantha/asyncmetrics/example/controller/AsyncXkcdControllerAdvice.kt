package com.dpranantha.asyncmetrics.example.controller

import com.dpranantha.asyncmetrics.example.controller.exception.ComicNotFoundException
import com.dpranantha.asyncmetrics.example.controller.exception.ErrorMessage
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class AsyncXkcdControllerAdvice {
    @ExceptionHandler(ComicNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleComicNotFound(ex: ComicNotFoundException): ResponseEntity<ErrorMessage> {
        return ResponseEntity<ErrorMessage>(
            ErrorMessage(ex.message, HttpStatus.NOT_FOUND.value()),
            HttpStatus.NOT_FOUND
        )
    }
}
