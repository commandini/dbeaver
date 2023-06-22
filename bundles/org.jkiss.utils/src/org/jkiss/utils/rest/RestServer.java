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
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.jkiss.code.NotNull;
import org.jkiss.utils.BeanUtils;
import org.jkiss.utils.rest.RestUtils.MethodInvokeRequest;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RestServer<T> {
    private final HttpServer server;

    public RestServer(@NotNull T object, @NotNull Gson gson, int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", createHandler(object, gson));
        server.setExecutor(createExecutor());
        server.start();
    }

    public void stop() throws IOException {
        server.stop(5);
    }

    @NotNull
    protected Executor createExecutor() {
        return new ThreadPoolExecutor(1, 10, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    }

    @NotNull
    protected RequestHandler<T> createHandler(@NotNull T object, @NotNull Gson gson) {
        return new RequestHandler<>(object, gson);
    }

    protected static class RequestHandler<T> implements HttpHandler {
        private final T object;
        private final Gson gson;

        protected RequestHandler(@NotNull T object, @NotNull Gson gson) {
            this.object = object;
            this.gson = gson;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            final MethodInvokeRequest request;

            try (Reader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))) {
                request = gson.fromJson(reader, MethodInvokeRequest.class);
            } catch (Exception e) {
                throw new IOException("Error reading request", e);
            }

            final Class<?> type;
            final Object result;

            try {
                final Method method = BeanUtils.getMethod(object.getClass(), request.getSignature());
                final Parameter[] parameters = method.getParameters();
                final Object[] args = new Object[parameters.length];

                for (int i = 0; i < parameters.length; i++) {
                    args[i] = gson.fromJson(request.getArguments()[i], parameters[i].getParameterizedType());
                }

                type = method.getReturnType();
                result = method.invoke(object, args);
            } catch (Exception e) {
                throw new IOException("Error invoking target method " + request.getSignature(), e);
            }

            final String response;

            if (type != void.class) {
                response = gson.toJson(result, type);
            } else {
                response = "";
            }

            exchange.sendResponseHeaders(200, response.length());

            try (Writer writer = new OutputStreamWriter(exchange.getResponseBody())) {
                writer.write(response);
            }
        }
    }
}
