package org.v31bank.grpc.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.grpc.client.interceptor.DefaultDeadlineSetupClientInterceptor;
import org.springframework.grpc.server.exception.GrpcExceptionHandler;

import org.v31bank.grpc.client.HeaderPropagationClientInterceptor;
import org.v31bank.grpc.server.BusinessExceptionGrpcExceptionHandler;
import org.v31bank.grpc.server.HeaderPropagationServerInterceptor;
import org.v31bank.grpc.server.UnexpectedExceptionGrpcExceptionHandler;
import org.v31bank.grpc.web.HeaderPropagationFilter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the gRPC auto-configurations.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class V31GrpcAutoConfigurationTests {

    private final ApplicationContextRunner server = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(V31GrpcServerAutoConfiguration.class));

    private final ApplicationContextRunner client = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(V31GrpcClientAutoConfiguration.class));

    private final WebApplicationContextRunner web = new WebApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(V31GrpcWebAutoConfiguration.class));

    @Test
    void contributesTheExceptionHandlersAndThePropagationInterceptor() {
        this.server.run((context) -> {
            assertThat(context).hasSingleBean(BusinessExceptionGrpcExceptionHandler.class);
            assertThat(context).hasSingleBean(UnexpectedExceptionGrpcExceptionHandler.class);
            assertThat(context).hasSingleBean(HeaderPropagationServerInterceptor.class);
            assertThat(context.getBeansOfType(GrpcExceptionHandler.class)).hasSize(2);
        });
    }

    @Test
    void leavesExceptionsAloneWhenAskedTo() {
        this.server.withPropertyValues("v31.grpc.server.exception-handling.enabled=false").run((context) -> {
            assertThat(context).doesNotHaveBean(GrpcExceptionHandler.class);
            assertThat(context).hasSingleBean(HeaderPropagationServerInterceptor.class);
        });
    }

    @Test
    void carriesNothingWhenPropagationIsTurnedOff() {
        this.server.withPropertyValues("v31.grpc.propagation.enabled=false")
            .run((context) -> assertThat(context).doesNotHaveBean(HeaderPropagationServerInterceptor.class));
        this.client.withPropertyValues("v31.grpc.propagation.enabled=false")
            .run((context) -> assertThat(context).doesNotHaveBean(HeaderPropagationClientInterceptor.class));
        this.web.withPropertyValues("v31.grpc.propagation.enabled=false")
            .run((context) -> assertThat(context).doesNotHaveBean(HeaderPropagationFilter.class));
    }

    @Test
    void givesEveryCallADeadlineAndCarriesTheContextOnward() {
        this.client.run((context) -> {
            assertThat(context).hasSingleBean(DefaultDeadlineSetupClientInterceptor.class);
            assertThat(context).hasSingleBean(HeaderPropagationClientInterceptor.class);
        });
    }

    @Test
    void readsTheContextOffTheIncomingHttpRequest() {
        this.web.run((context) -> assertThat(context).hasSingleBean(HeaderPropagationFilter.class));
    }

    @Test
    void addsNoHttpFilterToAServiceWithNoHttpEntryPoint() {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(V31GrpcWebAutoConfiguration.class))
            .run((context) -> assertThat(context).doesNotHaveBean(HeaderPropagationFilter.class));
    }

    @Test
    void leavesCallsUnboundedOnlyWhenAskedExplicitly() {
        this.client.withPropertyValues("v31.grpc.client.deadline.enabled=false")
            .run((context) -> assertThat(context).doesNotHaveBean(DefaultDeadlineSetupClientInterceptor.class));
    }

    @Test
    void refusesToStartOnADeadlineThatWouldExpireEveryCall() {
        this.client.withPropertyValues("v31.grpc.client.deadline.duration=0s").run((context) -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).rootCause()
                .hasMessageContaining("v31.grpc.client.deadline.duration must be positive");
        });
    }

    @Test
    void appliesTheConfiguredDeadline() {
        this.client.withPropertyValues("v31.grpc.client.deadline.duration=250ms")
            .run((context) -> assertThat(context).hasSingleBean(DefaultDeadlineSetupClientInterceptor.class));
    }

}
