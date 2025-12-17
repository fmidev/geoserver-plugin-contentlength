package fi.fmi.geoserver.contentlength;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;

/**
 * Forces Content-Length header for the response if necessary.
 *
 * <p>{ContentLengthResponse} class is meant to be created by its own package classes and it is provided as
 * {org.geoserver.ows.Response} class for other framework classes. This {org.geoserver.ows.Response} class wrapper is
 * used by {ContentLengthDispatcherCallback} class.
 */
class ContentLengthResponse extends Response {

    /** Header key for Content-Length. */
    private static final String CONTENT_LENGTH_HEADER = "Content-Length";

    /** Response that this class wraps. */
    private Response response;

    /** Output stream is temporarily kept as byte content. */
    private byte[] content;
    /** Content-Length of the output stream. Negative value means that the length has not been set. */
    private int contentLength = -1;

    /**
     * Set the output stream content into the byte array.
     *
     * @param {Operation} operation The operation for the request. May be {null}.
     * @param {Object} result Operation result object whose content size is measured. May be {null}.
     * @param {Response} response The response to the operation. May be {null}.
     * @throws {IOException} Any I/O errors that occur.
     */
    private void setContent(final Operation operation, final Object result, final Response response)
            throws IOException {
        // Set the content if it has not already been set.
        if (null == content) {
            ByteArrayOutputStream output = null;
            try {
                output = new ByteArrayOutputStream();
                // Write value content into the temporary stream.
                response.write(result, output, operation);
                // Make sure buffered output bytes are written out.
                output.flush();
                // Notice, byte array can be used after the stream is closed.
                content = output.toByteArray();
                // Content length is set. Then, value is available even if
                // content byte array would be nulled later.
                contentLength = content.length;

            } finally {
                // Release resources in all cases.
                if (null != output) {
                    output.close();
                }
            }
        }
    }

    /**
     * Constructor to wrap response.
     *
     * @param {Response} The response object that should be wrapped. May not be {null}.
     */
    public ContentLengthResponse(Response response) {
        // Original response getters are used to properly initialize
        // this wrapper.
        super(response.getBinding(), response.getOutputFormats());
        this.response = response;
    }

    /**
     * Set the content length if it does not exist in the {HttpServletResponse}.
     *
     * <p>This function gets {result} content size which will be set for the Content-Length header when framework sets
     * headers for the response.
     *
     * <p>Notice, stream needs to be read into byte array if this function is used. Then, excess memory needs to be used
     * to temporarily save stream content.
     *
     * @param request The request.
     * @param operation The operation.
     * @param result The result of the operation.
     * @param response The response to the operation.
     * @return {boolean} {true} if header was set. Else {false}.
     * @throws {IOException} Any I/O errors that occur.
     */
    public boolean setContentLength(Request request, Operation operation, Object result, Response response)
            throws IOException {
        boolean contentLengthSet = false;
        // Content-Length header is forced into HttpServletResponse
        // if the header does not already exist.
        if (null != request
                && null != operation
                && null != result
                && null != response
                && response.canHandle(operation)) {
            final HttpServletResponse httpResponse = request.getHttpResponse();
            if (null != httpResponse && !httpResponse.containsHeader(CONTENT_LENGTH_HEADER)) {
                // Framework uses the result object for the outputstream content.
                // Handle stream content to get the content length for the response.
                setContent(operation, result, response);
                contentLengthSet = true;
            }
        }
        return contentLengthSet;
    }

    /**
     * Serializes <code>value</code> to <code>output</code>.
     *
     * <p>The function uses the byte array {content} if it is available. Also, if {content} is used here, reference is
     * set to {null} and the content is not available after that. The reason for this is to relese the content that is
     * originally gotten from the stream to get the content length. Because content providers may provide the content
     * stream only once, the content needs to be hold temporarily until it is asked once more by the framework. After
     * that, original functionality of the framework may be used. Then, framework flow acts as the wrapped
     * {org.geoserver.ows.Response} would have been used without the content length operation in the middle of the flow.
     *
     * <p>The <code>operation</code> bean is provided for context.
     *
     * @param value The value to serialize.
     * @param output The output stream.
     * @param operation The operation which resulted in <code>value</code>
     * @throws IOException Any I/O errors that occur
     * @throws ServiceException Any service errors that occur
     */
    public void write(Object value, OutputStream output, Operation operation) throws IOException, ServiceException {
        if (null == content) {
            response.write(value, output, operation);

        } else if (canHandle(operation)) {
            output.write(content);
            output.flush();
            // Release the content.
            content = null;
        }
    }

    /** See parent {org.geoserver.ows.Response} class for function description. */
    public boolean canHandle(Operation operation) {
        return response.canHandle(operation);
    }

    /** See parent {org.geoserver.ows.Response} class for function description. */
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return response.getMimeType(value, operation);
    }

    /**
     * Returns a 2xn array of Strings, each of which is an HTTP header pair to be set on the HTTP Response. Can return
     * null if there are no headers to be set on the response.
     *
     * <p>If content header has been set, this function will append Content-Length header to the 2D header array that is
     * provided by the {Response} class instance that this class instance wraps.
     *
     * @param value The value to serialize
     * @param operation The operation being performed.
     * @return 2xn string array containing string-pairs of HTTP headers/values
     */
    public String[][] getHeaders(Object value, Operation operation) throws ServiceException {
        // Get headers from the wrapped response.
        String[][] headers = response.getHeaders(value, operation);
        if (contentLength >= 0) {
            // Content length header needs to be added.
            if (null == headers) {
                // Only Content-Length header will be in the response headers.
                // Create the 2D array for the content.
                headers = new String[1][2];
                headers[0][0] = CONTENT_LENGTH_HEADER;
                // Convert the integer into string value.
                headers[0][1] = "" + contentLength;

            } else {
                // Original header count.
                int originalCount = headers.length;
                // Content-Length header will be added to the 2D array
                // if the header does not already exist.
                String[][] newHeaders = new String[originalCount + 1][2];
                boolean alreadyExists = false;
                for (int i = 0; i < originalCount; ++i) {
                    String key = headers[i][0];
                    if (CONTENT_LENGTH_HEADER.equalsIgnoreCase(key)) {
                        // Content-Length header already exists.
                        // No reason to continue.
                        alreadyExists = true;
                        break;
                    }
                    newHeaders[i][0] = key;
                    // Set also the header value.
                    newHeaders[i][1] = headers[i][1];
                }
                if (!alreadyExists) {
                    // Append Content-Length header because it
                    // did not exist in the original headers.
                    newHeaders[originalCount][0] = CONTENT_LENGTH_HEADER;
                    newHeaders[originalCount][1] = "" + contentLength;
                    // Replace the headers reference with the new 2D array.
                    headers = newHeaders;
                }
            }
        }
        return headers;
    }

    /** See parent {org.geoserver.ows.Response} class for function description. */
    public String getPreferredDisposition(Object value, Operation operation) {
        return response.getPreferredDisposition(value, operation);
    }

    /** See parent {org.geoserver.ows.Response} class for function description. */
    public String getAttachmentFileName(Object value, Operation operation) {
        return response.getAttachmentFileName(value, operation);
    }
}
