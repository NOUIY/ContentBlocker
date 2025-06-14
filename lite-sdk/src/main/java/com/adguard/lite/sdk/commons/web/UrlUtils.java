/**
 * This file is part of AdGuard Content Blocker (https://github.com/AdguardTeam/ContentBlocker).
 * Copyright © 2018 AdGuard Content Blocker. All rights reserved.
 * <p>
 * AdGuard Content Blocker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 * <p>
 * AdGuard Content Blocker is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * AdGuard Content Blocker.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.adguard.lite.sdk.commons.web;

import com.adguard.lite.sdk.commons.io.IoUtils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CountingInputStream;
import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Helper for working with URLs (extract keywords, extract domain name)
 */
public class UrlUtils {

    private final static Logger LOG = LoggerFactory.getLogger(UrlUtils.class);
    private final static int DEFAULT_READ_TIMEOUT = 10000; // 10 seconds
    private final static int DEFAULT_SOCKET_TIMEOUT = 10000; // 10 seconds
    private final static String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/40.0.2214.85 Safari/537.36";
    private static final int READ_BUFFER_SIZE = 4096;

    /**
     * Tries to url encode specified text (using utf-8 encoding).
     * If something gone wrong -- returns input text as is.
     *
     * @param text Text to encode
     * @return Encoded string
     */
    public static String urlEncode(String text) {
        try {
            return URLEncoder.encode(text, "utf-8");
        } catch (Exception ex) {
            LOG.warn("Error encoding " + text, ex);
        }
        return text;
    }

    /**
     * Downloads string from the specified url
     *
     * @param url Url
     * @return Response
     */
    @SuppressWarnings("UnusedDeclaration")
    public static String downloadString(String url) throws MalformedURLException {
        return downloadString(url, DEFAULT_READ_TIMEOUT, DEFAULT_SOCKET_TIMEOUT);
    }

    /**
     * Downloads string from the specified url
     *
     * @param url Url
     * @param limit response size limit in bytes
     * @return Response
     */
    @SuppressWarnings("UnusedDeclaration")
    public static String downloadString(String url, long limit) throws MalformedURLException {
        return downloadString(new URL(url), null, DEFAULT_READ_TIMEOUT, DEFAULT_SOCKET_TIMEOUT, "utf-8", limit);
    }

    /**
     * Downloads string from the specified url
     *
     * @param url               Url
     * @param readTimeout       Read timeout
     * @param connectionTimeout Connection timeout
     * @return Downloaded string or null
     * @throws MalformedURLException
     */
    public static String downloadString(String url, int readTimeout, int connectionTimeout) throws MalformedURLException {
        return downloadString(new URL(url), null, readTimeout, connectionTimeout, "utf-8", -1);
    }

    /**
     * Downloads content from the specified url using specified proxy (or do not using it) and timeouts.
     * Returns null if there's an error.
     *
     * @param url           url
     * @param proxy         proxy to use
     * @param readTimeout   read timeout
     * @param socketTimeout connection timeout
     * @param limit         response size limit in bytes
     * @return Downloaded string
     */
    public static String downloadString(URL url, Proxy proxy, int readTimeout, int socketTimeout, String encoding, long limit) {
        HttpURLConnection connection = null;
        InputStream inputStream = null;

        try {
            connection = (HttpURLConnection) (proxy == null ? url.openConnection() : url.openConnection(proxy));
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setReadTimeout(readTimeout);
            connection.setConnectTimeout(socketTimeout);
            connection.setRequestProperty("Accept-Encoding", "gzip");
            connection.connect();
            if (connection.getResponseCode() >= 400) {
                throw new IOException("Response status is " + connection.getResponseCode());
            }

            if (connection.getResponseCode() >= 301) {
                String location = connection.getHeaderField("Location");
                // HttpURLConnection does not follow redirects from HTTP to HTTPS
                // So we handle it manually
                return downloadString(new URL(location), proxy, readTimeout, socketTimeout, encoding, limit);
            }

            if (connection.getResponseCode() == 204) {
                return StringUtils.EMPTY;
            }

            CountingInputStream countingInputStream = new CountingInputStream(connection.getInputStream());
            inputStream = countingInputStream;

            if ("gzip".equals(connection.getHeaderField("Content-Encoding"))) {
                inputStream = new GZIPInputStream(inputStream);
            }

            StringBuilderWriter stringBuilderWriter = new StringBuilderWriter();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, encoding);
            char[] buffer = new char[READ_BUFFER_SIZE];
            int readBytes;
            while((readBytes = inputStreamReader.read(buffer)) != -1) {
                if (limit != -1 && countingInputStream.getByteCount() > limit) {
                    throw new IOException("The response exceeded the limit of " + limit + " bytes");
                }
                stringBuilderWriter.write(buffer, 0, readBytes);
            }

            return stringBuilderWriter.toString();
        } catch (IOException ex) {
            if (LOG.isDebugEnabled()) {
                LOG.warn("Error downloading string from {}:\r\n", url, ex);
            } else {
                LOG.warn("Cannot download string from {}: {}", url, ex.getMessage());
            }
            // Ignoring exception
            return null;
        } finally {
            IoUtils.closeQuietly(inputStream);
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Sends a POST request
     *
     * @param url           URL
     * @param postData      Post body
     * @param encoding      Post body encoding
     * @param contentType   Post body content type
     * @param readTimeout   Read timeout
     * @param socketTimeout Socket timeout
     * @return Response
     */
    public static String postRequest(URL url, String postData, String encoding, String contentType, int readTimeout, int socketTimeout) {
        return postRequest(url, postData, encoding, contentType, false, readTimeout, socketTimeout);
    }

    /**
     * Sends a POST request
     *
     * @param url           URL
     * @param postData      Post request body
     * @param encoding      Post request body encoding
     * @param contentType   Body content type
     * @param compress      If true - compress bod
     * @param readTimeout   Read timeout
     * @param socketTimeout Socket timeout
     * @return Response
     */
    public static String postRequest(URL url, String postData, String encoding, String contentType, boolean compress, int readTimeout, int socketTimeout) {
        HttpURLConnection connection = null;
        OutputStream outputStream = null;

        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            if (contentType != null) {
                connection.setRequestProperty("Content-Type", contentType);
            }
            if (compress) {
                connection.setRequestProperty("Content-Encoding", "gzip");
            }
            connection.setConnectTimeout(socketTimeout);
            connection.setReadTimeout(readTimeout);
            connection.setDoOutput(true);
            connection.connect();
            if (postData != null) {
                outputStream = connection.getOutputStream();

                if (compress) {
                    outputStream = new GZIPOutputStream(outputStream);
                }

                if (encoding != null) {
                    IOUtils.write(postData, outputStream, encoding);
                } else {
                    IOUtils.write(postData, outputStream);
                }

                if (compress) {
                    ((GZIPOutputStream) outputStream).finish();
                } else {
                    outputStream.flush();
                }
            }

            return IOUtils.toString(connection.getInputStream(), encoding);
        } catch (Exception ex) {
            LOG.error("Error posting request to {}, post data length={}\r\n", url, StringUtils.length(postData), ex);
            // Ignoring exception
            return null;
        } finally {
            IOUtils.closeQuietly(outputStream);

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

}