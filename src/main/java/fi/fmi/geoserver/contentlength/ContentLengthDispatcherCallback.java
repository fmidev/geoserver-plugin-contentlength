package fi.fmi.geoserver.contentlength;

import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.geoserver.platform.Operation;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.ows.DispatcherCallback;

/**
 * Forces Content-Length into headers given by {org.geoserver.ows.Response}.
 * 
 * DispatcherCallback extensions are automatically loaded by GeoServer framework.
 * Framework {org.geoserver.ows.Dispatcher} instance calls {responseDispatched}
 * callback function of {org.geoserver.ows.DispatcherCallback} instances before
 * flushing response output stream. Therefore, callback may add additional header
 * into response before flush is done.
 * 
 * This class uses {ContentLengthResponse} to get the content length from the
 * stream content.
 */
public class ContentLengthDispatcherCallback implements DispatcherCallback {

    /**
     * See parent {org.geoserver.ows.DispatcherCallback} class for function description.
     */
    public Request init(Request request) {
        // Nothing to do.
        return null;
    }

    /**
     * See parent {org.geoserver.ows.DispatcherCallback} class for function description.
     */
    public Service serviceDispatched(Request request, Service service)
            throws ServiceException {
        // Nothing to do.
        return null;
    }

    /**
     * See parent {org.geoserver.ows.DispatcherCallback} class for function description.
     */
    public Operation operationDispatched(Request request, Operation operation) {
        // Nothing to do.
        return null;
    }

    /**
     * See parent {org.geoserver.ows.DispatcherCallback} class for function description.
     */
    public Object operationExecuted(Request request, Operation operation, Object result) {
        // Nothing to do.
        return null;
    }

    /**
     * Called after the response to a request has been dispatched.
     * <p>
     * Wraps the given {response} if Content-Length should be included
     * as part of the {response} headers.
     * </p>
     * <p>
     * <b>Note:</b> This method is only called when the operation returns a
     * value.
     * </p>
     * <p>
     * This method can modify the response object, or wrap and return it. If
     * null is returned the response passed in is used normally.
     * </p>
     * 
     * @param request The request.
     * @param operation The operation.
     * @param result The result of the operation.
     * @param response The response to the operation.
     */
    public Response responseDispatched(
            Request request, Operation operation, Object result, Response response) {
        Response wrapperResponse = null;
        try {
            ContentLengthResponse contentLengthResponse = 
                    new ContentLengthResponse(response);
            // Set the content length header.
            if (contentLengthResponse.
                    setContentLength(request, operation, result, response)) {
                // Use the wrapper response in the framework flow
                // because Content-Length header was set.
                wrapperResponse = contentLengthResponse;
            }

        } catch (final Exception e) {
            // Just ignore whole operation.
            System.err.println(getClass().getName() + ": " + e.toString());
        }
        return wrapperResponse;
    }

    /**
     * See parent {org.geoserver.ows.DispatcherCallback} class for function description.
     */
    public void finished(Request request) {
        // Nothing to do.
    }

}
