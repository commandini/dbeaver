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
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URI;

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
        return RestClient.connect(uri, cls, gson);
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
}
