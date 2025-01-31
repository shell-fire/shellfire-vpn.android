package de.shellfire.vpn.android.webservice.model;

public class BaseResponse<T> {
    private String status;
    private String message;
    private String errorCode;
    private T data;

    // Getters and setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    // Convenience methods
    public boolean isSuccess() {
        return "success".equalsIgnoreCase(status);
    }

    public boolean isError() {
        return !isSuccess();
    }
}
