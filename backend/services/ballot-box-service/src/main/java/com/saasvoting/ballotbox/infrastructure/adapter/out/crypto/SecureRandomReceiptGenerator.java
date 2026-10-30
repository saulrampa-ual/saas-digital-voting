package com.saasvoting.ballotbox.infrastructure.adapter.out.crypto;

import com.saasvoting.ballotbox.application.port.out.ReceiptGenerator;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.HexFormat;

/** RI-8: 32 random bytes as 64 lowercase hexadecimal characters. */
@Component
public class SecureRandomReceiptGenerator implements ReceiptGenerator {

    private final SecureRandom random = new SecureRandom();

    @Override
    public String newReceiptCode() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
