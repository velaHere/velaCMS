package com.vela.velaCMS.security;

public final class OTPKeys {

    private OTPKeys() {}

    public static String resendCooldown(String userID) {
        return "otp:resend:user:" + userID + ":cooldown";
    }

    public static String resendUserHourly(String userID) {
        return "otp:resend:user:" + userID;
    }

    public static String resendIpHourly(String ip) {
        return "otp:resend:ip:" + ip.replace(":", "_");
    }
}
