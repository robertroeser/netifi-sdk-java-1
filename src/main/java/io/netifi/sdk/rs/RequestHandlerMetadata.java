package io.netifi.sdk.rs;

import io.netifi.sdk.serializer.Serializer;

import java.lang.reflect.Method;

/** */
public class RequestHandlerMetadata {
  private final Serializer<?> requestSerializer;
  private final Serializer<?> responseSerializer;
  private final Method method;
  private final Class<?> registeredType;
  private final Object object;
  private final long namespaceId;
  private final long classId;
  private final long methodId;

  public RequestHandlerMetadata(
      Serializer<?> requestSerializer,
      Serializer<?> responseSerializer,
      Method method,
      Class<?> registeredType,
      Object object,
      long namespaceId,
      long classId,
      long methodId) {
    this.requestSerializer = requestSerializer;
    this.responseSerializer = responseSerializer;
    this.method = method;
    this.registeredType = registeredType;
    this.object = object;
    this.namespaceId = namespaceId;
    this.classId = classId;
    this.methodId = methodId;
  }

  public String getStringKey() {
    return namespaceId + ":" + classId + ":" + methodId;
  }

  public Serializer<?> getRequestSerializer() {
    return requestSerializer;
  }

  public Serializer<?> getResponseSerializer() {
    return responseSerializer;
  }

  public Method getMethod() {
    return method;
  }

  public Object getObject() {
    return object;
  }

  public long getNamespaceId() {
    return namespaceId;
  }

  public long getClassId() {
    return classId;
  }

  public long getMethodId() {
    return methodId;
  }

  public Class<?> getRegisteredType() {
    return registeredType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RequestHandlerMetadata metadata = (RequestHandlerMetadata) o;

    if (namespaceId != metadata.namespaceId) return false;
    if (classId != metadata.classId) return false;
    return methodId == metadata.methodId;
  }

  @Override
  public int hashCode() {
    int result = (int) (namespaceId ^ (namespaceId >>> 32));
    result = 31 * result + (int) (classId ^ (classId >>> 32));
    result = 31 * result + (int) (methodId ^ (methodId >>> 32));
    return result;
  }

  @Override
  public String toString() {
    return "\nRequestHandlerMetadata{\n"
        + "\trequestSerializer=" + requestSerializer
        + ",\n\tresponseSerializer=" + responseSerializer
        + ",\n\tmethod=" + method
        + ",\n\targument=" + (method.getParameters().length > 0 ? method.getParameters()[0] : "null")
        + ",\n\treturnType=" + (method.getGenericReturnType())
        + ",\n\tregisteredType=" + registeredType
        + ",\n\tobject=" + object
        + ",\n\tnamespaceId=" + namespaceId
        + ",\n\tclassId=" + classId
        + ",\n\tmethodId=" + methodId
        + "\n}";
  }
}
