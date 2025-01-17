/*
 * Copyright (c) 2011-2019 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.lealone.orm.json.jackson;

import org.lealone.orm.json.spi.JsonCodec;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class JacksonFactory implements org.lealone.orm.json.spi.JsonFactory {

    public static final JacksonFactory INSTANCE = new JacksonFactory();

    public static final JacksonCodec CODEC;

    static {
        // JacksonCodec codec;
        // try {
        // codec = new DatabindCodec();
        // } catch (Throwable ignore) {
        // // No databind
        // codec = new JacksonCodec();
        // }
        // CODEC = codec;

        CODEC = new JacksonCodec();
    }

    @Override
    public JsonCodec codec() {
        return CODEC;
    }
}
