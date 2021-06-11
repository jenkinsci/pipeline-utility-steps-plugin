package org.jenkinsci.plugins.pipeline.utility.steps.fs;

import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

/**
 * Computes the hash of a file on the agent.
 */
public class ComputeHashCallable extends MasterToSlaveFileCallable<String> {
    private final String hashAlgorithm;

    public ComputeHashCallable(String hashAlgorithm) {
        this.hashAlgorithm = hashAlgorithm;
    }

    @Override
    public String invoke(File file, VirtualChannel virtualChannel) throws IOException {
        if (file.exists() && file.isFile()) {
            try {
                return hashOfFile(file);
            } catch (NoSuchAlgorithmException e) {
                throw new IOException(e.getMessage(), e);
            }
        }
        return null;
    }

    public String hashOfFile(final File file) throws NoSuchAlgorithmException, IOException {
        final MessageDigest messageDigest = MessageDigest.getInstance(hashAlgorithm);

        try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
            final byte[] buffer = new byte[1024];
            for (int read; (read = is.read(buffer)) != -1; ) {
                messageDigest.update(buffer, 0, read);
            }
        }

        return byteToHex(messageDigest.digest());
    }

    private String byteToHex(byte[] bytes) {
        try (Formatter formatter = new Formatter()) {
            for (final byte b : bytes) {
                formatter.format("%02x", b);
            }
            return formatter.toString();
        }
    }
}
