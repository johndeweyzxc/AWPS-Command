package com.johndeweydev.awps.models.data;

public record BridgeUploadRequestHttp(String ssid, String bssid, String client_mac_address,
                                      String key_type, String a_nonce, String hash_data,
                                      String key_data, String date_captured) {
}
