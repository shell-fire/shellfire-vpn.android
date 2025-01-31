package de.shellfire.vpn.android.webservice;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import de.shellfire.vpn.android.utils.Util;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;

public class LogRequestBody extends RequestBody {
    private static final MediaType MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
    private final Reader reader;
    private final String userMessage;

    public LogRequestBody(String userMessage, Reader reader) {
        this.userMessage = userMessage;
        this.reader = reader;
    }

    @Override
    public MediaType contentType() {
        return MEDIA_TYPE;
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {

        String escapedUserMessage = Util.escapeJson(userMessage + "\r\n");

        // Convert to UTF-8 bytes and back to a string to enforce UTF-8 encoding
        byte[] utf8Bytes = escapedUserMessage.getBytes(StandardCharsets.UTF_8);
        String utf8EscapedUserMessage = new String(utf8Bytes, StandardCharsets.UTF_8);


        sink.writeUtf8("{\"userMessage\":\"" + utf8EscapedUserMessage + "\",\"log\":\"");

        char[] buffer = new char[1024];
        int read;

        if (reader != null) {
            while ((read = reader.read(buffer)) != -1) {
                for (int i = 0; i < read; i++) {
                    char c = buffer[i];
                    switch (c) {
                        case '\\':
                            sink.writeUtf8("\\\\");
                            break;
                        case '"':
                            sink.writeUtf8("\\\"");
                            break;
                        case '\n':
                            sink.writeUtf8("\\n");
                            break;
                        case '\r':
                            sink.writeUtf8("\\r");
                            break;
                        case '\t':
                            sink.writeUtf8("\\t");
                            break;
                        default:
                            sink.writeUtf8(String.valueOf(c));
                            break;
                    }
                }
            }
        }

        sink.writeUtf8("\"}");
    }

    @Override
    public long contentLength() {
        // Returning -1 to indicate that the length is unknown and force chunked encoding
        return -1;
    }
}
