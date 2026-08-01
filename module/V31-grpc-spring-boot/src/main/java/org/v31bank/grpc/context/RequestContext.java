package org.v31bank.grpc.context;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import io.grpc.Context;

/**
 * What travels with a request, wherever it goes next.
 * <p>
 * A request arrives carrying values that mean something further in — which trace
 * it belongs to, which tenant asked, which language to answer in — and the calls
 * made while serving it have to carry the same ones, or the meaning stops at the
 * first hop. This is where they live between arriving and being sent on.
 *
 * <h2>Two carriers, because there are two kinds of thread</h2>
 *
 * A servlet request runs on the thread it arrived on, so a {@link ThreadLocal}
 * holds it. A gRPC call does not: the interceptor runs on one thread and the
 * handler on another, and a {@code ThreadLocal} set in the interceptor is simply
 * not there when the handler runs. {@link Context} is gRPC's own carrier and is
 * propagated across those hops, so it holds it there.
 * <p>
 * {@link #current()} reads the gRPC context first — authoritative while a call is
 * being served — and falls back to the thread, so a gRPC call made while serving
 * an HTTP request picks up what that request arrived with.
 *
 * <h2>What must not go in here</h2>
 *
 * Nothing secret. Everything placed here is copied onto every outgoing call, to
 * every service reached, for the rest of the request. A credential put here would
 * be handed to services that have no business holding one, and would reach any of
 * them that logs its metadata. Credentials travel deliberately, per call.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public final class RequestContext {

    /**
     * Where the values live while a gRPC call is being served.
     */
    public static final Context.Key<Map<String, String>> CONTEXT_KEY = Context.key("v31-request-context");

    /**
     * What a value has to look like to be carried.
     * <p>
     * Printable ASCII and bounded: these values end up in gRPC metadata, in
     * response headers, and in log lines. One containing a newline could forge a
     * log entry or terminate a header early, and one of unbounded length is a way
     * to make every downstream call expensive.
     */
    private static final Pattern ACCEPTED_VALUE = Pattern.compile("[\\x20-\\x7E]{1,256}");

    private static final ThreadLocal<Map<String, String>> THREAD_LOCAL = new ThreadLocal<>();

    private RequestContext() {
    }

    /**
     * Return what this request is carrying.
     * @return the values, empty when there is nothing to carry
     */
    public static Map<String, String> current() {
        Map<String, String> fromContext = CONTEXT_KEY.get();
        if (fromContext != null) {
            return fromContext;
        }
        Map<String, String> fromThread = THREAD_LOCAL.get();
        return (fromThread != null) ? fromThread : Map.of();
    }

    /**
     * Return one value this request is carrying.
     * @param name the name it travels under, lower case
     * @return the value, or {@code null} when the request is not carrying it
     */
    public static String get(String name) {
        return current().get(name);
    }

    /**
     * Attach values to the current thread until the returned scope is closed.
     * <p>
     * For the HTTP side, where the request runs on one thread. Values left behind
     * on a pooled thread would be attributed to whatever it served next, which is
     * why this returns something that has to be closed rather than a pair of
     * methods that can be left unpaired.
     *
     * <pre class="code">
     * try (RequestContext.Scope scope = RequestContext.attach(values)) {
     *     chain.doFilter(request, response);
     * }
     * </pre>
     *
     * @param values what the request is carrying
     * @return the scope to close when the request is done with
     */
    public static Scope attach(Map<String, String> values) {
        Objects.requireNonNull(values, "values must not be null");
        Map<String, String> previous = THREAD_LOCAL.get();
        THREAD_LOCAL.set(Map.copyOf(values));
        return () -> {
            if (previous != null) {
                THREAD_LOCAL.set(previous);
            }
            else {
                THREAD_LOCAL.remove();
            }
        };
    }

    /**
     * Add a value to a set being built, if it is one that may be carried.
     * @param values the set being built
     * @param name the name to carry it under, lower case
     * @param value the value, ignored when absent or not safe to carry
     */
    public static void put(Map<String, String> values, String name, String value) {
        if (value != null && ACCEPTED_VALUE.matcher(value).matches()) {
            values.put(name, value);
        }
    }

    /**
     * Return an empty set to build up.
     * @return the set
     */
    public static Map<String, String> newValues() {
        return new LinkedHashMap<>();
    }

    /**
     * What {@link #attach} returns: closing it puts the thread back as it was.
     */
    @FunctionalInterface
    public interface Scope extends AutoCloseable {

        @Override
        void close();

    }

}
