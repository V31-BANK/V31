package org.v31bank.grpc.autoconfigure;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for V31 gRPC.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@ConfigurationProperties("v31.grpc")
public class V31GrpcProperties {

    private final Propagation propagation = new Propagation();

    private final Server server = new Server();

    private final Client client = new Client();

    public Propagation getPropagation() {
        return this.propagation;
    }

    public Server getServer() {
        return this.server;
    }

    public Client getClient() {
        return this.client;
    }

    /**
     * What travels with a request from one service to the next.
     */
    public static class Propagation {

        /**
         * Whether to carry a request's context onward — into the calls made while
         * serving it, and into the log lines written for it.
         */
        private boolean enabled = true;

        /**
         * Header names carried through beyond the request identifier, which is
         * always carried.
         * <p>
         * Lower case, since that is what gRPC requires of a metadata key and what
         * makes the same spelling work for an HTTP header too. Values are checked
         * before being carried: printable, and bounded in length.
         * <p>
         * Nothing secret belongs here. Every name listed is copied onto every
         * outgoing call, to every service reached, for the rest of the request.
         */
        private List<String> headers = new ArrayList<>();

        public boolean isEnabled() {
            return this.enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<String> getHeaders() {
            return this.headers;
        }

        public void setHeaders(List<String> headers) {
            this.headers = headers;
        }

    }

    /**
     * Properties for calls this service serves.
     */
    public static class Server {

        private final ExceptionHandling exceptionHandling = new ExceptionHandling();

        public ExceptionHandling getExceptionHandling() {
            return this.exceptionHandling;
        }

        /**
         * Exception handling properties.
         */
        public static class ExceptionHandling {

            /**
             * Whether to report a refused call with the platform's error code and
             * to keep an unexpected failure's message off the wire.
             */
            private boolean enabled = true;

            public boolean isEnabled() {
                return this.enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

        }

    }

    /**
     * Properties for calls this service makes.
     */
    public static class Client {

        private final Deadline deadline = new Deadline();

        public Deadline getDeadline() {
            return this.deadline;
        }

    }

    /**
     * Deadline properties.
     */
    public static class Deadline {

        /**
         * Whether a call that did not set a deadline is given one.
         * <p>
         * Turning this off restores what gRPC does out of the box, which is to
         * let a call wait until the connection breaks. That should be a
         * deliberate decision, not something arrived at by leaving a duration
         * unset.
         */
        private boolean enabled = true;

        /**
         * How long a call may run before it is given up on.
         * <p>
         * gRPC applies no deadline of its own: a call to a service that has
         * stopped answering — not refusing, answering nothing — waits until the
         * connection breaks, holding the caller's thread and whoever is waiting
         * on it. Under load that is how one unhealthy service exhausts the thread
         * pools of everything in front of it.
         * <p>
         * Must be positive. A zero or negative duration would expire every call
         * the moment it left, so it is refused at startup rather than discovered
         * in production.
         */
        private Duration duration = Duration.ofSeconds(5);

        public boolean isEnabled() {
            return this.enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Duration getDuration() {
            return this.duration;
        }

        public void setDuration(Duration duration) {
            this.duration = duration;
        }

    }

}
