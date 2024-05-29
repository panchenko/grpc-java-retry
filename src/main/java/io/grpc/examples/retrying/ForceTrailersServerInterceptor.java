package io.grpc.examples.retrying;

import io.grpc.*;

// copied from the URL specified below, eventually be part of the released grpc version
@SuppressWarnings({
        "java:S119", // template variable name style
        "java:S1181" // catch Exception instead of Throwable
})
@ExperimentalApi("https://github.com/grpc/grpc-java/issues/10124")
public class ForceTrailersServerInterceptor implements ServerInterceptor {
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        ForceTrailersServerCall<ReqT, RespT> interceptedCall = new ForceTrailersServerCall<>(call);
        try {
            return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(
                    next.startCall(interceptedCall, headers)) {
                @Override
                public void onMessage(ReqT message) {
                    try {
                        super.onMessage(message);
                    } catch (Throwable t) {
                        sendHeaders();
                        throw t;
                    }
                }

                @Override
                public void onHalfClose() {
                    try {
                        super.onHalfClose();
                    } catch (Throwable t) {
                        sendHeaders();
                        throw t;
                    }
                }

                @Override
                public void onCancel() {
                    try {
                        super.onCancel();
                    } catch (Throwable t) {
                        sendHeaders();
                        throw t;
                    }
                }

                @Override
                public void onComplete() {
                    try {
                        super.onComplete();
                    } catch (Throwable t) {
                        sendHeaders();
                        throw t;
                    }
                }

                @Override
                public void onReady() {
                    try {
                        super.onReady();
                    } catch (Throwable t) {
                        sendHeaders();
                        throw t;
                    }
                }

                private void sendHeaders() {
                    interceptedCall.maybeSendEmptyHeaders();
                }
            };
        } catch (RuntimeException e) {
            interceptedCall.maybeSendEmptyHeaders();
            throw e;
        }
    }

    static class ForceTrailersServerCall<ReqT, RespT> extends
            ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT> {

        private volatile boolean headersSent = false;

        ForceTrailersServerCall(ServerCall<ReqT, RespT> delegate) {
            super(delegate);
        }

        void maybeSendEmptyHeaders() {
            if (!headersSent) {
                this.sendHeaders(new Metadata());
            }
        }

        @Override
        public void sendHeaders(Metadata headers) {
            headersSent = true;
            super.sendHeaders(headers);
        }

        @Override
        public void close(Status status, Metadata trailers) {
            maybeSendEmptyHeaders();
            super.close(status, trailers);
        }
    }
}
