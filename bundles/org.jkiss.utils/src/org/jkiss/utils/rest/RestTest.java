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

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class RestTest {
    public static void main(String[] args) throws Exception {
        final var server = RestUtils.createServer(new CalculatorImpl(), 8000);
        final var client = RestUtils.createClient(URI.create("http://localhost:8000"), Calculator.class);

        System.out.println(client.add(5));
        System.out.println(client.add(5, 7));
        System.out.println(client.add(1, 2, 3, 4, 5));
        System.out.println(client.test(Map.of("a", 123, "b", Map.of("lol", "kek"), "c", List.of("12345"))));

        server.stop();
    }

    public interface Calculator {
        int add(int a);

        int add(int a, int b);

        int add(int a, int... rest);

        Map<String, Object> test(Map<String, Object> map);
    }

    private static class CalculatorImpl implements Calculator {
        @Override
        public int add(int a) {
            return a + a;
        }

        @Override
        public int add(int a, int b) {
            return a + b;
        }

        @Override
        public int add(int a, int... rest) {
            return a + IntStream.of(rest).sum();
        }

        @Override
        public Map<String, Object> test(Map<String, Object> map) {
            return map;
        }
    }
}
