package com.saasvoting.ballotbox.application.port.out;

/** Receipt codes: 64 random hexadecimal characters (RI-8). */
public interface ReceiptGenerator {

    String newReceiptCode();
}
