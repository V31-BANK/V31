package org.v31bank.grpc.client;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

import org.v31bank.grpc.context.RequestContext;

/**
 * Sends onward whatever the request being served arrived carrying, so that a
 * trace, a tenant or a locale does not stop at the edge of a service.
 * <p>
 * Without it, following one customer's complaint through the platform means
 * finding the identifier in one service's logs, working out from timestamps which
 * call it made, and starting again — for every hop.
 * <p>
 * Nothing is sent when there is nothing to send. A scheduled job calling another
 * service is not part of anybody's request, and inventing values here would only
 * make it look like it was.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public class HeaderPropagationClientInterceptor implements ClientInterceptor {

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions, Channel next) {
        return new SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                RequestContext.current()
                    .forEach((name, value) -> headers.put(Metadata.Key.of(name, Metadata.ASCII_STRING_MARSHALLER),
                            value));
                super.start(responseListener, headers);
            }
        };
    }

}
