package io.protop.core.error;

public enum ServiceError {

    // 001xx - project manifest errors
    MANIFEST_ERROR,

    // 002xx - registry errors
    VERSION_ALREADY_PUBLISHED,
    AUTH_FAILED,

    // 003xx - file system errors
    STORAGE_ERROR,

    // 004xx - rc errors
    RC_ERROR;
}
