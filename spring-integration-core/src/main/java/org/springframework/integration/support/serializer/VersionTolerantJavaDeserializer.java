/*
 * Copyright 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.support.serializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;

import org.springframework.core.NestedIOException;
import org.springframework.core.serializer.Deserializer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * A {@link Deserializer} implementation that reads an input stream using Java serialization in a manner that
 * allows {@link Serializable} classes to be deserialized with more than one {@code serialVersionUID}.
 *
 * <pre>
 * <code>
 * 	VersionTolerantJavaDeserializer deser = new VersionTolerantJavaDeserializer();
 * 	deser.allowSerialVersionUID(MessageHistory.class, 1426799817181873282L);
 * 	deser.allowSerialVersionUID(MessageHistory.class, -2340400235574314134L);
 *
 * 	JdbcMessageStore messageStore = ...
 * 	messageStore.setDeserializer(deser);
 * 	Message{@literal <}Foo{@literal >} oldFoo = messageStore.getMessage(oldFooMsgUid);
 * </code>
 * </pre>
 *
 * @author  Chris Bono
 */
public class VersionTolerantJavaDeserializer implements Deserializer<Object> {

	private final MultiValueMap<Class<?>, Long> alternativeSerialVersionUIDs = new LinkedMultiValueMap<>();

	/**
	 * Allows an alternative {@code serialVersionUID} to be used when deserializing a class.
	 * @param clazz the class
	 * @param serialVersionUID the alternate uid to allow
	 */
	public void allowSerialVersionUID(Class<?> clazz, long serialVersionUID) {
		this.alternativeSerialVersionUIDs.add(clazz, serialVersionUID);
	}

	@Override
	public Object deserialize(InputStream inputStream) throws IOException {
		VersionTolerantObjectInputStream objectInputStream = new VersionTolerantObjectInputStream(inputStream);
		try {
			return objectInputStream.readObject();
		}
		catch (ClassNotFoundException ex) {
			throw new NestedIOException("Failed to deserialize object type", ex);
		}
	}

	private final class VersionTolerantObjectInputStream extends ObjectInputStream {

		private VersionTolerantObjectInputStream(InputStream in) throws IOException {
			super(in);
		}

		@Override
		protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
			ObjectStreamClass resultClassDescriptor = super.readClassDescriptor();

			String className = resultClassDescriptor.getName();
			Class<?> localClass = Class.forName(className);
			if (!alternativeSerialVersionUIDs().containsKey(localClass)) {
				return resultClassDescriptor;
			}

			ObjectStreamClass localClassDescriptor = ObjectStreamClass.lookup(localClass);
			if (localClassDescriptor == null) {
				return resultClassDescriptor;
			}

			final long localSerialVersionUID = localClassDescriptor.getSerialVersionUID();
			final long streamSerialVersionUID = resultClassDescriptor.getSerialVersionUID();
			if (streamSerialVersionUID != localSerialVersionUID && alternativeSerialVersionUIDs().get(localClass)
					.contains(streamSerialVersionUID)) {
				return localClassDescriptor;
			}

			return resultClassDescriptor;
		}

		private MultiValueMap<Class<?>, Long> alternativeSerialVersionUIDs() {
			return VersionTolerantJavaDeserializer.this.alternativeSerialVersionUIDs;
		}
	}
}
