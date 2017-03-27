package com.tokenbrowser.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import com.tokenbrowser.view.BaseApplication;

import org.whispersystems.libsignal.InvalidMessageException;
import org.whispersystems.signalservice.api.SignalServiceMessageReceiver;
import org.whispersystems.signalservice.api.messages.SignalServiceAttachmentPointer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import okio.BufferedSink;
import okio.Okio;
import okio.Source;

public class FileUtil {

    public static final int MAX_SIZE = 1024 * 1024;

    public File saveFileFromUri(final Context context, final Uri uri) throws IOException {
        final String mimeType = context.getContentResolver().getType(uri);
        final MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        final String fileExtension = mimeTypeMap.getExtensionFromMimeType(mimeType);
        final String fileName = String.format("%s.%s", UUID.randomUUID().toString(), fileExtension);
        final File destFile = new File(BaseApplication.get().getFilesDir(), fileName);
        final InputStream inputStream = BaseApplication.get()
                .getContentResolver()
                .openInputStream(uri);
        return writeToFileFromInputStream(destFile, inputStream);
    }

    private File writeToFileFromInputStream(final File file, final InputStream inputStream) throws IOException {
        final BufferedSink sink = Okio.buffer(Okio.sink(file));
        final Source source = Okio.source(inputStream);
        sink.writeAll(source);
        sink.close();
        return file;
    }

    public File writeAttachmentToFileFromMessageReceiver(final SignalServiceAttachmentPointer attachment,
                                                         final SignalServiceMessageReceiver messageReceiver) {
        File file = null;
        try {
            @SuppressLint("DefaultLocale")
            final String fileName = String.format("%d.jpg", attachment.getId());
            file = new File(BaseApplication.get().getCacheDir(), fileName);
            final InputStream stream = messageReceiver.retrieveAttachment(attachment, file);
            final File destFile = new File(BaseApplication.get().getFilesDir(), fileName);
            return writeToFileFromInputStream(destFile, stream);
        } catch (IOException | InvalidMessageException e) {
            LogUtil.e(getClass(), "Error during writing attachment to file " + e.getMessage());
            return null;
        } finally {
            if (file != null) {
                file.delete();
            }
        }
    }

    public File createImageFileWithRandomName(final Context context) throws IOException {
        final String filename = UUID.randomUUID().toString() + ".jpg";
        return new File(context.getFilesDir(), filename);
    }

    public String getMimeTypeFromFilename(final String filename) {
        final String fileExtension = MimeTypeMap.getFileExtensionFromUrl(filename);
        return  MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
    }

    public void compressImage(final long maxSize, final File file) throws FileNotFoundException {
        if (file.length() <= maxSize) {
            return;
        }

        final int compressPercentage = (int)(((double)maxSize / file.length()) * 100);
        final Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        final OutputStream outputStream = new FileOutputStream(file);
        bitmap.compress(Bitmap.CompressFormat.JPEG, compressPercentage, outputStream);
    }
}
