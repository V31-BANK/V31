package org.v31bank.grpc.autoconfigure;

import java.time.Duration;

import io.grpc.ClientInterceptor;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.grpc.client.autoconfigure.GrpcClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.grpc.client.GlobalClientInterceptor;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.grpc.client.interceptor.DefaultDeadlineSetupClientInterceptor;

import org.v31bank.grpc.client.HeaderPropagationClientInterceptor;

/**
 * {@link AutoConfiguration Auto-configuration} for the calls a V31 service makes:
 * gives every one a deadline and carries the request identifier onward.
 *
 * <h2>Why a deadline is applied by default</h2>
 *
 * gRPC does not impose one. A call to a service that has stopped answering — not
 * refusing, answering nothing — waits until the connection breaks, which may be
 * minutes. The calling thread waits with it, and so does whoever is waiting on
 * that thread. Under load this is how one unhealthy service exhausts the thread
 * pools of everything in front of it while all of them look healthy.
 * <p>
 * Spring gRPC ships the interceptor for this but does not install it, so a call
 * without a deadline is what you get unless you ask otherwise. Here it is the
 * other way round: every call leaves with
 * {@code v31.grpc.client.default-deadline} unless it set one of its own, and
 * removing it is a deliberate act.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@AutoConfiguration(before = GrpcClientAutoConfiguration.class)
@ConditionalOnClass({ ClientInterceptor.class, GrpcChannelFactory.class })
@EnableConfigurationProperties(V31GrpcProperties.class)
public class V31GrpcClientAutoConfiguration {

    /**
     * Applies the default deadline to any call that did not set one.
     * <p>
     * Set {@code v31.grpc.client.deadline.enabled} to {@code false} to leave calls
     * unbounded, which is what gRPC does on its own.
     * @param properties the deadline to apply
     * @return the interceptor
     * @throws IllegalStateException if the configured duration is not positive,
     * since a deadline of zero expires every call the moment it leaves
     */
    @Bean
    @GlobalClientInterceptor
    @ConditionalOnMissingBean
    @ConditionalOnBooleanProperty(name = "v31.grpc.client.deadline.enabled", matchIfMissing = true)
    public DefaultDeadlineSetupClientInterceptor defaultDeadlineSetupClientInterceptor(V31GrpcProperties properties) {
        Duration duration = properties.getClient().getDeadline().getDuration();
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalStateException("v31.grpc.client.deadline.duration must be positive, but was " + duration
                    + "; set v31.grpc.client.deadline.enabled to false to leave calls without a deadline");
        }
        return new DefaultDeadlineSetupClientInterceptor(duration);
    }

    /**
     * Sends onward whatever the request being served arrived carrying.
     * @return the interceptor
     */
    @Bean
    @GlobalClientInterceptor
    @ConditionalOnMissingBean
    @ConditionalOnBooleanProperty(name = "v31.grpc.propagation.enabled", matchIfMissing = true)
    public HeaderPropagationClientInterceptor headerPropagationClientInterceptor() {
        return new HeaderPropagationClientInterceptor();
    }

}
