package com.jwei.publicone.https.x509;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

public class ZhHostnameVerifier implements HostnameVerifier {
    @Override
    public boolean verify(String hostname, SSLSession session) {

        return true;
    }

}