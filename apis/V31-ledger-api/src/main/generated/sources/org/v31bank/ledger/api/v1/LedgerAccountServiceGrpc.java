package org.v31bank.ledger.api.v1;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * The chart of accounts, as other services see it.
 * This file is the contract. A field is never renumbered and never reused: a
 * caller compiled against an older copy reads by number, so changing what a
 * number means silently corrupts data rather than failing to compile. Removing
 * one means reserving it.
 * </pre>
 */
@io.grpc.stub.annotations.GrpcGenerated
public final class LedgerAccountServiceGrpc {

  private LedgerAccountServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "v31.ledger.v1.LedgerAccountService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<org.v31bank.ledger.api.v1.CreateLedgerAccountRequest,
      org.v31bank.ledger.api.v1.CreateLedgerAccountResponse> getCreateLedgerAccountMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateLedgerAccount",
      requestType = org.v31bank.ledger.api.v1.CreateLedgerAccountRequest.class,
      responseType = org.v31bank.ledger.api.v1.CreateLedgerAccountResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.v31bank.ledger.api.v1.CreateLedgerAccountRequest,
      org.v31bank.ledger.api.v1.CreateLedgerAccountResponse> getCreateLedgerAccountMethod() {
    io.grpc.MethodDescriptor<org.v31bank.ledger.api.v1.CreateLedgerAccountRequest, org.v31bank.ledger.api.v1.CreateLedgerAccountResponse> getCreateLedgerAccountMethod;
    if ((getCreateLedgerAccountMethod = LedgerAccountServiceGrpc.getCreateLedgerAccountMethod) == null) {
      synchronized (LedgerAccountServiceGrpc.class) {
        if ((getCreateLedgerAccountMethod = LedgerAccountServiceGrpc.getCreateLedgerAccountMethod) == null) {
          LedgerAccountServiceGrpc.getCreateLedgerAccountMethod = getCreateLedgerAccountMethod =
              io.grpc.MethodDescriptor.<org.v31bank.ledger.api.v1.CreateLedgerAccountRequest, org.v31bank.ledger.api.v1.CreateLedgerAccountResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateLedgerAccount"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.v31bank.ledger.api.v1.CreateLedgerAccountRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.v31bank.ledger.api.v1.CreateLedgerAccountResponse.getDefaultInstance()))
              .setSchemaDescriptor(new LedgerAccountServiceMethodDescriptorSupplier("CreateLedgerAccount"))
              .build();
        }
      }
    }
    return getCreateLedgerAccountMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.v31bank.ledger.api.v1.GetLedgerAccountRequest,
      org.v31bank.ledger.api.v1.GetLedgerAccountResponse> getGetLedgerAccountMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetLedgerAccount",
      requestType = org.v31bank.ledger.api.v1.GetLedgerAccountRequest.class,
      responseType = org.v31bank.ledger.api.v1.GetLedgerAccountResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.v31bank.ledger.api.v1.GetLedgerAccountRequest,
      org.v31bank.ledger.api.v1.GetLedgerAccountResponse> getGetLedgerAccountMethod() {
    io.grpc.MethodDescriptor<org.v31bank.ledger.api.v1.GetLedgerAccountRequest, org.v31bank.ledger.api.v1.GetLedgerAccountResponse> getGetLedgerAccountMethod;
    if ((getGetLedgerAccountMethod = LedgerAccountServiceGrpc.getGetLedgerAccountMethod) == null) {
      synchronized (LedgerAccountServiceGrpc.class) {
        if ((getGetLedgerAccountMethod = LedgerAccountServiceGrpc.getGetLedgerAccountMethod) == null) {
          LedgerAccountServiceGrpc.getGetLedgerAccountMethod = getGetLedgerAccountMethod =
              io.grpc.MethodDescriptor.<org.v31bank.ledger.api.v1.GetLedgerAccountRequest, org.v31bank.ledger.api.v1.GetLedgerAccountResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetLedgerAccount"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.v31bank.ledger.api.v1.GetLedgerAccountRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.v31bank.ledger.api.v1.GetLedgerAccountResponse.getDefaultInstance()))
              .setSchemaDescriptor(new LedgerAccountServiceMethodDescriptorSupplier("GetLedgerAccount"))
              .build();
        }
      }
    }
    return getGetLedgerAccountMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.v31bank.ledger.api.v1.ListLedgerAccountsRequest,
      org.v31bank.ledger.api.v1.ListLedgerAccountsResponse> getListLedgerAccountsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListLedgerAccounts",
      requestType = org.v31bank.ledger.api.v1.ListLedgerAccountsRequest.class,
      responseType = org.v31bank.ledger.api.v1.ListLedgerAccountsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.v31bank.ledger.api.v1.ListLedgerAccountsRequest,
      org.v31bank.ledger.api.v1.ListLedgerAccountsResponse> getListLedgerAccountsMethod() {
    io.grpc.MethodDescriptor<org.v31bank.ledger.api.v1.ListLedgerAccountsRequest, org.v31bank.ledger.api.v1.ListLedgerAccountsResponse> getListLedgerAccountsMethod;
    if ((getListLedgerAccountsMethod = LedgerAccountServiceGrpc.getListLedgerAccountsMethod) == null) {
      synchronized (LedgerAccountServiceGrpc.class) {
        if ((getListLedgerAccountsMethod = LedgerAccountServiceGrpc.getListLedgerAccountsMethod) == null) {
          LedgerAccountServiceGrpc.getListLedgerAccountsMethod = getListLedgerAccountsMethod =
              io.grpc.MethodDescriptor.<org.v31bank.ledger.api.v1.ListLedgerAccountsRequest, org.v31bank.ledger.api.v1.ListLedgerAccountsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListLedgerAccounts"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.v31bank.ledger.api.v1.ListLedgerAccountsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.v31bank.ledger.api.v1.ListLedgerAccountsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new LedgerAccountServiceMethodDescriptorSupplier("ListLedgerAccounts"))
              .build();
        }
      }
    }
    return getListLedgerAccountsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.v31bank.ledger.api.v1.UpdateLedgerAccountRequest,
      org.v31bank.ledger.api.v1.UpdateLedgerAccountResponse> getUpdateLedgerAccountMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UpdateLedgerAccount",
      requestType = org.v31bank.ledger.api.v1.UpdateLedgerAccountRequest.class,
      responseType = org.v31bank.ledger.api.v1.UpdateLedgerAccountResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.v31bank.ledger.api.v1.UpdateLedgerAccountRequest,
      org.v31bank.ledger.api.v1.UpdateLedgerAccountResponse> getUpdateLedgerAccountMethod() {
    io.grpc.MethodDescriptor<org.v31bank.ledger.api.v1.UpdateLedgerAccountRequest, org.v31bank.ledger.api.v1.UpdateLedgerAccountResponse> getUpdateLedgerAccountMethod;
    if ((getUpdateLedgerAccountMethod = LedgerAccountServiceGrpc.getUpdateLedgerAccountMethod) == null) {
      synchronized (LedgerAccountServiceGrpc.class) {
        if ((getUpdateLedgerAccountMethod = LedgerAccountServiceGrpc.getUpdateLedgerAccountMethod) == null) {
          LedgerAccountServiceGrpc.getUpdateLedgerAccountMethod = getUpdateLedgerAccountMethod =
              io.grpc.MethodDescriptor.<org.v31bank.ledger.api.v1.UpdateLedgerAccountRequest, org.v31bank.ledger.api.v1.UpdateLedgerAccountResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UpdateLedgerAccount"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.v31bank.ledger.api.v1.UpdateLedgerAccountRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.v31bank.ledger.api.v1.UpdateLedgerAccountResponse.getDefaultInstance()))
              .setSchemaDescriptor(new LedgerAccountServiceMethodDescriptorSupplier("UpdateLedgerAccount"))
              .build();
        }
      }
    }
    return getUpdateLedgerAccountMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.v31bank.ledger.api.v1.DeleteLedgerAccountRequest,
      org.v31bank.ledger.api.v1.DeleteLedgerAccountResponse> getDeleteLedgerAccountMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteLedgerAccount",
      requestType = org.v31bank.ledger.api.v1.DeleteLedgerAccountRequest.class,
      responseType = org.v31bank.ledger.api.v1.DeleteLedgerAccountResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.v31bank.ledger.api.v1.DeleteLedgerAccountRequest,
      org.v31bank.ledger.api.v1.DeleteLedgerAccountResponse> getDeleteLedgerAccountMethod() {
    io.grpc.MethodDescriptor<org.v31bank.ledger.api.v1.DeleteLedgerAccountRequest, org.v31bank.ledger.api.v1.DeleteLedgerAccountResponse> getDeleteLedgerAccountMethod;
    if ((getDeleteLedgerAccountMethod = LedgerAccountServiceGrpc.getDeleteLedgerAccountMethod) == null) {
      synchronized (LedgerAccountServiceGrpc.class) {
        if ((getDeleteLedgerAccountMethod = LedgerAccountServiceGrpc.getDeleteLedgerAccountMethod) == null) {
          LedgerAccountServiceGrpc.getDeleteLedgerAccountMethod = getDeleteLedgerAccountMethod =
              io.grpc.MethodDescriptor.<org.v31bank.ledger.api.v1.DeleteLedgerAccountRequest, org.v31bank.ledger.api.v1.DeleteLedgerAccountResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteLedgerAccount"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.v31bank.ledger.api.v1.DeleteLedgerAccountRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.v31bank.ledger.api.v1.DeleteLedgerAccountResponse.getDefaultInstance()))
              .setSchemaDescriptor(new LedgerAccountServiceMethodDescriptorSupplier("DeleteLedgerAccount"))
              .build();
        }
      }
    }
    return getDeleteLedgerAccountMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static LedgerAccountServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<LedgerAccountServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<LedgerAccountServiceStub>() {
        @java.lang.Override
        public LedgerAccountServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new LedgerAccountServiceStub(channel, callOptions);
        }
      };
    return LedgerAccountServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports all types of calls on the service
   */
  public static LedgerAccountServiceBlockingV2Stub newBlockingV2Stub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<LedgerAccountServiceBlockingV2Stub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<LedgerAccountServiceBlockingV2Stub>() {
        @java.lang.Override
        public LedgerAccountServiceBlockingV2Stub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new LedgerAccountServiceBlockingV2Stub(channel, callOptions);
        }
      };
    return LedgerAccountServiceBlockingV2Stub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static LedgerAccountServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<LedgerAccountServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<LedgerAccountServiceBlockingStub>() {
        @java.lang.Override
        public LedgerAccountServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new LedgerAccountServiceBlockingStub(channel, callOptions);
        }
      };
    return LedgerAccountServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static LedgerAccountServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<LedgerAccountServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<LedgerAccountServiceFutureStub>() {
        @java.lang.Override
        public LedgerAccountServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new LedgerAccountServiceFutureStub(channel, callOptions);
        }
      };
    return LedgerAccountServiceFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * The chart of accounts, as other services see it.
   * This file is the contract. A field is never renumbered and never reused: a
   * caller compiled against an older copy reads by number, so changing what a
   * number means silently corrupts data rather than failing to compile. Removing
   * one means reserving it.
   * </pre>
   */
  public interface AsyncService {

    /**
     */
    default void createLedgerAccount(org.v31bank.ledger.api.v1.CreateLedgerAccountRequest request,
        io.grpc.stub.StreamObserver<org.v31bank.ledger.api.v1.CreateLedgerAccountResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCreateLedgerAccountMethod(), responseObserver);
    }

    /**
     */
    default void getLedgerAccount(org.v31bank.ledger.api.v1.GetLedgerAccountRequest request,
        io.grpc.stub.StreamObserver<org.v31bank.ledger.api.v1.GetLedgerAccountResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetLedgerAccountMethod(), responseObserver);
    }

    /**
     * <pre>
     * Returns one page, newest first.
     * </pre>
     */
    default void listLedgerAccounts(org.v31bank.ledger.api.v1.ListLedgerAccountsRequest request,
        io.grpc.stub.StreamObserver<org.v31bank.ledger.api.v1.ListLedgerAccountsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListLedgerAccountsMethod(), responseObserver);
    }

    /**
     */
    default void updateLedgerAccount(org.v31bank.ledger.api.v1.UpdateLedgerAccountRequest request,
        io.grpc.stub.StreamObserver<org.v31bank.ledger.api.v1.UpdateLedgerAccountResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUpdateLedgerAccountMethod(), responseObserver);
    }

    /**
     * <pre>
     * Returns the account that was removed.
     * </pre>
     */
    default void deleteLedgerAccount(org.v31bank.ledger.api.v1.DeleteLedgerAccountRequest request,
        io.grpc.stub.StreamObserver<org.v31bank.ledger.api.v1.DeleteLedgerAccountResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteLedgerAccountMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service LedgerAccountService.
   * <pre>
   * The chart of accounts, as other services see it.
   * This file is the contract. A field is never renumbered and never reused: a
   * caller compiled against an older copy reads by number, so changing what a
   * number means silently corrupts data rather than failing to compile. Removing
   * one means reserving it.
   * </pre>
   */
  public static abstract class LedgerAccountServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return LedgerAccountServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service LedgerAccountService.
   * <pre>
   * The chart of accounts, as other services see it.
   * This file is the contract. A field is never renumbered and never reused: a
   * caller compiled against an older copy reads by number, so changing what a
   * number means silently corrupts data rather than failing to compile. Removing
   * one means reserving it.
   * </pre>
   */
  public static final class LedgerAccountServiceStub
      extends io.grpc.stub.AbstractAsyncStub<LedgerAccountServiceStub> {
    private LedgerAccountServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected LedgerAccountServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new LedgerAccountServiceStub(channel, callOptions);
    }

    /**
     */
    public void createLedgerAccount(org.v31bank.ledger.api.v1.CreateLedgerAccountRequest request,
        io.grpc.stub.StreamObserver<org.v31bank.ledger.api.v1.CreateLedgerAccountResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateLedgerAccountMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getLedgerAccount(org.v31bank.ledger.api.v1.GetLedgerAccountRequest request,
        io.grpc.stub.StreamObserver<org.v31bank.ledger.api.v1.GetLedgerAccountResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetLedgerAccountMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Returns one page, newest first.
     * </pre>
     */
    public void listLedgerAccounts(org.v31bank.ledger.api.v1.ListLedgerAccountsRequest request,
        io.grpc.stub.StreamObserver<org.v31bank.ledger.api.v1.ListLedgerAccountsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListLedgerAccountsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void updateLedgerAccount(org.v31bank.ledger.api.v1.UpdateLedgerAccountRequest request,
        io.grpc.stub.StreamObserver<org.v31bank.ledger.api.v1.UpdateLedgerAccountResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUpdateLedgerAccountMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Returns the account that was removed.
     * </pre>
     */
    public void deleteLedgerAccount(org.v31bank.ledger.api.v1.DeleteLedgerAccountRequest request,
        io.grpc.stub.StreamObserver<org.v31bank.ledger.api.v1.DeleteLedgerAccountResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteLedgerAccountMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service LedgerAccountService.
   * <pre>
   * The chart of accounts, as other services see it.
   * This file is the contract. A field is never renumbered and never reused: a
   * caller compiled against an older copy reads by number, so changing what a
   * number means silently corrupts data rather than failing to compile. Removing
   * one means reserving it.
   * </pre>
   */
  public static final class LedgerAccountServiceBlockingV2Stub
      extends io.grpc.stub.AbstractBlockingStub<LedgerAccountServiceBlockingV2Stub> {
    private LedgerAccountServiceBlockingV2Stub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected LedgerAccountServiceBlockingV2Stub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new LedgerAccountServiceBlockingV2Stub(channel, callOptions);
    }

    /**
     */
    public org.v31bank.ledger.api.v1.CreateLedgerAccountResponse createLedgerAccount(org.v31bank.ledger.api.v1.CreateLedgerAccountRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getCreateLedgerAccountMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.v31bank.ledger.api.v1.GetLedgerAccountResponse getLedgerAccount(org.v31bank.ledger.api.v1.GetLedgerAccountRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getGetLedgerAccountMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Returns one page, newest first.
     * </pre>
     */
    public org.v31bank.ledger.api.v1.ListLedgerAccountsResponse listLedgerAccounts(org.v31bank.ledger.api.v1.ListLedgerAccountsRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getListLedgerAccountsMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.v31bank.ledger.api.v1.UpdateLedgerAccountResponse updateLedgerAccount(org.v31bank.ledger.api.v1.UpdateLedgerAccountRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getUpdateLedgerAccountMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Returns the account that was removed.
     * </pre>
     */
    public org.v31bank.ledger.api.v1.DeleteLedgerAccountResponse deleteLedgerAccount(org.v31bank.ledger.api.v1.DeleteLedgerAccountRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getDeleteLedgerAccountMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do limited synchronous rpc calls to service LedgerAccountService.
   * <pre>
   * The chart of accounts, as other services see it.
   * This file is the contract. A field is never renumbered and never reused: a
   * caller compiled against an older copy reads by number, so changing what a
   * number means silently corrupts data rather than failing to compile. Removing
   * one means reserving it.
   * </pre>
   */
  public static final class LedgerAccountServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<LedgerAccountServiceBlockingStub> {
    private LedgerAccountServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected LedgerAccountServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new LedgerAccountServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public org.v31bank.ledger.api.v1.CreateLedgerAccountResponse createLedgerAccount(org.v31bank.ledger.api.v1.CreateLedgerAccountRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateLedgerAccountMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.v31bank.ledger.api.v1.GetLedgerAccountResponse getLedgerAccount(org.v31bank.ledger.api.v1.GetLedgerAccountRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetLedgerAccountMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Returns one page, newest first.
     * </pre>
     */
    public org.v31bank.ledger.api.v1.ListLedgerAccountsResponse listLedgerAccounts(org.v31bank.ledger.api.v1.ListLedgerAccountsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListLedgerAccountsMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.v31bank.ledger.api.v1.UpdateLedgerAccountResponse updateLedgerAccount(org.v31bank.ledger.api.v1.UpdateLedgerAccountRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUpdateLedgerAccountMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Returns the account that was removed.
     * </pre>
     */
    public org.v31bank.ledger.api.v1.DeleteLedgerAccountResponse deleteLedgerAccount(org.v31bank.ledger.api.v1.DeleteLedgerAccountRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteLedgerAccountMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service LedgerAccountService.
   * <pre>
   * The chart of accounts, as other services see it.
   * This file is the contract. A field is never renumbered and never reused: a
   * caller compiled against an older copy reads by number, so changing what a
   * number means silently corrupts data rather than failing to compile. Removing
   * one means reserving it.
   * </pre>
   */
  public static final class LedgerAccountServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<LedgerAccountServiceFutureStub> {
    private LedgerAccountServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected LedgerAccountServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new LedgerAccountServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.v31bank.ledger.api.v1.CreateLedgerAccountResponse> createLedgerAccount(
        org.v31bank.ledger.api.v1.CreateLedgerAccountRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateLedgerAccountMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.v31bank.ledger.api.v1.GetLedgerAccountResponse> getLedgerAccount(
        org.v31bank.ledger.api.v1.GetLedgerAccountRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetLedgerAccountMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Returns one page, newest first.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.v31bank.ledger.api.v1.ListLedgerAccountsResponse> listLedgerAccounts(
        org.v31bank.ledger.api.v1.ListLedgerAccountsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListLedgerAccountsMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.v31bank.ledger.api.v1.UpdateLedgerAccountResponse> updateLedgerAccount(
        org.v31bank.ledger.api.v1.UpdateLedgerAccountRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUpdateLedgerAccountMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Returns the account that was removed.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.v31bank.ledger.api.v1.DeleteLedgerAccountResponse> deleteLedgerAccount(
        org.v31bank.ledger.api.v1.DeleteLedgerAccountRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteLedgerAccountMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_CREATE_LEDGER_ACCOUNT = 0;
  private static final int METHODID_GET_LEDGER_ACCOUNT = 1;
  private static final int METHODID_LIST_LEDGER_ACCOUNTS = 2;
  private static final int METHODID_UPDATE_LEDGER_ACCOUNT = 3;
  private static final int METHODID_DELETE_LEDGER_ACCOUNT = 4;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_CREATE_LEDGER_ACCOUNT:
          serviceImpl.createLedgerAccount((org.v31bank.ledger.api.v1.CreateLedgerAccountRequest) request,
              (io.grpc.stub.StreamObserver<org.v31bank.ledger.api.v1.CreateLedgerAccountResponse>) responseObserver);
          break;
        case METHODID_GET_LEDGER_ACCOUNT:
          serviceImpl.getLedgerAccount((org.v31bank.ledger.api.v1.GetLedgerAccountRequest) request,
              (io.grpc.stub.StreamObserver<org.v31bank.ledger.api.v1.GetLedgerAccountResponse>) responseObserver);
          break;
        case METHODID_LIST_LEDGER_ACCOUNTS:
          serviceImpl.listLedgerAccounts((org.v31bank.ledger.api.v1.ListLedgerAccountsRequest) request,
              (io.grpc.stub.StreamObserver<org.v31bank.ledger.api.v1.ListLedgerAccountsResponse>) responseObserver);
          break;
        case METHODID_UPDATE_LEDGER_ACCOUNT:
          serviceImpl.updateLedgerAccount((org.v31bank.ledger.api.v1.UpdateLedgerAccountRequest) request,
              (io.grpc.stub.StreamObserver<org.v31bank.ledger.api.v1.UpdateLedgerAccountResponse>) responseObserver);
          break;
        case METHODID_DELETE_LEDGER_ACCOUNT:
          serviceImpl.deleteLedgerAccount((org.v31bank.ledger.api.v1.DeleteLedgerAccountRequest) request,
              (io.grpc.stub.StreamObserver<org.v31bank.ledger.api.v1.DeleteLedgerAccountResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getCreateLedgerAccountMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              org.v31bank.ledger.api.v1.CreateLedgerAccountRequest,
              org.v31bank.ledger.api.v1.CreateLedgerAccountResponse>(
                service, METHODID_CREATE_LEDGER_ACCOUNT)))
        .addMethod(
          getGetLedgerAccountMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              org.v31bank.ledger.api.v1.GetLedgerAccountRequest,
              org.v31bank.ledger.api.v1.GetLedgerAccountResponse>(
                service, METHODID_GET_LEDGER_ACCOUNT)))
        .addMethod(
          getListLedgerAccountsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              org.v31bank.ledger.api.v1.ListLedgerAccountsRequest,
              org.v31bank.ledger.api.v1.ListLedgerAccountsResponse>(
                service, METHODID_LIST_LEDGER_ACCOUNTS)))
        .addMethod(
          getUpdateLedgerAccountMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              org.v31bank.ledger.api.v1.UpdateLedgerAccountRequest,
              org.v31bank.ledger.api.v1.UpdateLedgerAccountResponse>(
                service, METHODID_UPDATE_LEDGER_ACCOUNT)))
        .addMethod(
          getDeleteLedgerAccountMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              org.v31bank.ledger.api.v1.DeleteLedgerAccountRequest,
              org.v31bank.ledger.api.v1.DeleteLedgerAccountResponse>(
                service, METHODID_DELETE_LEDGER_ACCOUNT)))
        .build();
  }

  private static abstract class LedgerAccountServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    LedgerAccountServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return org.v31bank.ledger.api.v1.LedgerAccountProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("LedgerAccountService");
    }
  }

  private static final class LedgerAccountServiceFileDescriptorSupplier
      extends LedgerAccountServiceBaseDescriptorSupplier {
    LedgerAccountServiceFileDescriptorSupplier() {}
  }

  private static final class LedgerAccountServiceMethodDescriptorSupplier
      extends LedgerAccountServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    LedgerAccountServiceMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (LedgerAccountServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new LedgerAccountServiceFileDescriptorSupplier())
              .addMethod(getCreateLedgerAccountMethod())
              .addMethod(getGetLedgerAccountMethod())
              .addMethod(getListLedgerAccountsMethod())
              .addMethod(getUpdateLedgerAccountMethod())
              .addMethod(getDeleteLedgerAccountMethod())
              .build();
        }
      }
    }
    return result;
  }
}
