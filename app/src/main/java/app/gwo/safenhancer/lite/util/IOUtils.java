package app.gwo.safenhancer.lite.util;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import androidx.annotation.NonNull;

import static java.util.Objects.requireNonNull;

public final class IOUtils {

    private IOUtils() {}

    private static final int EOF = -1;

    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    public static long copy(@NonNull InputStream in, @NonNull OutputStream out) throws IOException {
        long count = 0;
        int n = 0;
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        while (EOF != (n = in.read(buffer))) {
            out.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    public static InputStream openInputStreamAdaptive(
            @NonNull Context context, @NonNull Uri uri) throws FileNotFoundException {
        if ("content".equals(uri.getScheme())) {
            return context.getContentResolver().openInputStream(uri);
        } else if ("file".equals(uri.getScheme())) {
            return new FileInputStream(new File(requireNonNull(uri.getPath())));
        } else {
            throw new IllegalArgumentException("Unsupported uri: " + uri.toString());
        }
    }

    public static OutputStream openOutputStreamAdaptive(
            @NonNull Context context, @NonNull Uri uri) throws FileNotFoundException {
        if ("content".equals(uri.getScheme())) {
            return context.getContentResolver().openOutputStream(uri);
        } else if ("file".equals(uri.getScheme())) {
            return new FileOutputStream(new File(requireNonNull(uri.getPath())));
        } else {
            throw new IllegalArgumentException("Unsupported uri: " + uri.toString());
        }
    }

}
