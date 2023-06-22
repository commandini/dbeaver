/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.utils.rest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import org.jkiss.code.NotNull;
import org.jkiss.utils.BeanUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class RestUtils {
    private static final Gson gson = new GsonBuilder()
        .setLenient()
        .disableHtmlEscaping()
        .serializeNulls()
        .create();

    private RestUtils() {
        // prevents instantiation
    }

    @NotNull
    public static <T> T createClient(@NotNull URI uri, @NotNull Class<T> cls) {
        return createClient(uri, cls, gson);
    }

    @NotNull
    public static <T> T createClient(@NotNull URI uri, @NotNull Class<T> cls, @NotNull Gson gson) {
        final Object proxy = Proxy.newProxyInstance(
            cls.getClassLoader(),
            new Class[]{cls},
            new ClientProxy(uri, gson)
        );

        return cls.cast(proxy);
    }

    @NotNull
    public static <T> RestServer<T> createServer(@NotNull T object, int port) {
        try {
            return new RestServer<>(object, gson, port);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static class MethodInvokeRequest {
        private final String signature;
        private final JsonElement[] args;

        public MethodInvokeRequest(@NotNull String signature, @NotNull JsonElement[] args) {
            this.signature = signature;
            this.args = args;
        }

        @NotNull
        public static MethodInvokeRequest of(@NotNull Method method, @NotNull Object[] args, @NotNull Gson gson) {
            final String signature = BeanUtils.getSignature(method);
            final Parameter[] parameters = method.getParameters();
            final JsonElement[] elements = new JsonElement[parameters.length];

            for (int i = 0; i < parameters.length; i++) {
                elements[i] = gson.toJsonTree(args[i], parameters[i].getParameterizedType());
            }

            return new MethodInvokeRequest(signature, elements);
        }

        @NotNull
        public String getSignature() {
            return signature;
        }

        @NotNull
        public JsonElement[] getArguments() {
            return args;
        }
    }

    private static class ClientProxy implements InvocationHandler {
        private final URI uri;
        private final Gson gson;
        private final HttpClient client;

        private ClientProxy(@NotNull URI uri, @NotNull Gson gson) {
            this.uri = uri;
            this.gson = gson;
            this.client = HttpClient.newHttpClient();
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return BeanUtils.handleObjectMethod(proxy, method, args);
            }

            final MethodInvokeRequest request = MethodInvokeRequest.of(method, args, gson);

            final HttpResponse<?> response = client.send(
                HttpRequest.newBuilder()
                    .uri(uri)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(request)))
                    .build(),
                info -> HttpResponse.BodySubscribers.mapping(
                    HttpResponse.BodySubscribers.ofInputStream(),
                    is -> gson.fromJson(new InputStreamReader(is), method.getReturnType())
                )
            );

            return response.body();
        }
    }
}
