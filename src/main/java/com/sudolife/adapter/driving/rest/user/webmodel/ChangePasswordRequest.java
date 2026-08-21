package com.sudolife.adapter.driving.rest.user.webmodel;

public record ChangePasswordRequest(String currentPassword, String newPassword) {
}
