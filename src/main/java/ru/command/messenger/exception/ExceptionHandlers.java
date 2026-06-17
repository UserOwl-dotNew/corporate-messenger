package ru.command.messenger.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice("ru.command.messenger")
@Slf4j
public class ExceptionHandlers {
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler({NotFoundException.class})
    public ErrorResponse handlerNotFoundException(final NotFoundException e) {
        log.error("Not found error: {}", e.getMessage(), e);
        return new ErrorResponse(e.getMessage());
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler({DuplicateException.class})
    public ErrorResponse handlerDuplicateException(final DuplicateException e) {
        log.error("Duplicate error: {}", e.getMessage(), e);
        return new ErrorResponse(e.getMessage());
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler
    public ErrorResponse handlerThrowable(final Throwable e) {
        log.error("Unexpected error: {}", e.getMessage(), e);
        return new ErrorResponse("Произошла непредвиденная ошибка" + e.getMessage());
    }
}
