package com.examsystem.common;

import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(BizException.class)
  public ApiResponse<Void> handleBiz(BizException e) {
    return ApiResponse.fail(e.getCode(), e.getMessage());
  }

  @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
  public ApiResponse<Void> handleValid(Exception e) {
    String message = "参数错误";
    if (e instanceof MethodArgumentNotValidException) {
      MethodArgumentNotValidException ex = (MethodArgumentNotValidException) e;
      if (ex.getBindingResult().getFieldError() != null) {
        message = ex.getBindingResult().getFieldError().getDefaultMessage();
      }
    } else if (e instanceof BindException) {
      BindException ex = (BindException) e;
      if (ex.getBindingResult().getFieldError() != null) {
        message = ex.getBindingResult().getFieldError().getDefaultMessage();
      }
    }
    return ApiResponse.fail(4000, message);
  }

  @ExceptionHandler(Exception.class)
  public ApiResponse<Void> handleOther(Exception e) {
    return ApiResponse.fail(5000, "系统错误");
  }
}

