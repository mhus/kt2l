



```java
public static HttpServletRequest getHttpRequest() {
    VaadinServletRequest request = VaadinServletRequest.getCurrent();
    if (request == null) return null;
    return request.getHttpServletRequest();

}
```

```java
static Principal getPrincipal() {
    VaadinServletRequest request = VaadinServletRequest.getCurrent();
    if (request == null) {
        LOGGER.warn("Request not found");
        return null;
    }
    var principal = request.getUserPrincipal();
    if (principal == null) {
        LOGGER.warn("Principal not found in request", new Throwable());
    }
    return principal;
}
```

