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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link VersionTolerantJavaDeserializer}.
 */
public class VersionTolerantJavaDeserializerTests {

	@Test
	void failsWhenNoAlternateUIDsConfigured() throws Exception {
		assertThat(Foo.serialVersionUID).isEqualTo(3L);
		String fileName = "./src/test/resources/deser/foo2";
		VersionTolerantJavaDeserializer deser = new VersionTolerantJavaDeserializer();
		try (FileInputStream fis = new FileInputStream(fileName)) {
			assertThatThrownBy(() -> deser.deserialize(fis))
					.isInstanceOf(InvalidClassException.class);
		}
	}

	@Test
	void passesWhenAlternateUIDsConfigured() throws Exception {
		assertThat(Foo.serialVersionUID).isEqualTo(3L);

		VersionTolerantJavaDeserializer deser = new VersionTolerantJavaDeserializer();
		deser.allowSerialVersionUID(VersionTolerantJavaDeserializerTests.Foo.class, 1L);
		deser.allowSerialVersionUID(VersionTolerantJavaDeserializerTests.Foo.class, 2L);

		String fileName = "./src/test/resources/deser/foo1";
		try (FileInputStream fis = new FileInputStream(fileName)) {
			VersionTolerantJavaDeserializerTests.Foo oldFoo = (VersionTolerantJavaDeserializerTests.Foo) deser.deserialize(fis);
			assertThat(oldFoo.toString()).isEqualTo("Foo (name=uno, serialVersionUID=3)");
		}

		fileName = "./src/test/resources/deser/foo2";
		try (FileInputStream fis = new FileInputStream(fileName)) {
			VersionTolerantJavaDeserializerTests.Foo oldFoo = (VersionTolerantJavaDeserializerTests.Foo) deser.deserialize(fis);
			assertThat(oldFoo.toString()).isEqualTo("Foo (name=dos, serialVersionUID=3)");
		}
	}

	@Disabled
	@Nested
	class GenerateSerializedFiles {

		@Test
		void generateOldFoo1SerializedFile() throws Exception {
			// Only run this once to generate the Foo1 serialized file - make sure Foo.serialVersionUID = 1L before running
			assertThat(Foo.serialVersionUID).isEqualTo(1L);
			String filename = "./src/test/resources/deser/foo1";
			doGenerateOldFooSerializedFile(filename, new Foo("uno"));
		}

		@Test
		void generateOldFoo2SerializedFile() throws Exception {
			// Only run this once to generate the Foo2 serialized file - make sure Foo.serialVersionUID = 2L before running
			assertThat(Foo.serialVersionUID).isEqualTo(2L);
			String filename = "./src/test/resources/deser/foo2";
			doGenerateOldFooSerializedFile(filename, new Foo("dos"));
		}

		private void doGenerateOldFooSerializedFile(String filename, Foo foo) throws Exception {
			FileOutputStream fos = new FileOutputStream(filename);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(foo);
			fos.close();
			FileInputStream fis = new FileInputStream(filename);
			ObjectInputStream ois = new ObjectInputStream(fis);
			Foo fooDeser = (Foo) ois.readObject();
			assertThat(fooDeser.getName()).isEqualTo(foo.getName());
			fis.close();
		}
	}

	static class Foo implements Serializable {

		private static final long serialVersionUID = 3L;

		private String name;

		public Foo(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

		public String toString() {
			return String.format("Foo (name=%s, serialVersionUID=%d)", this.name, serialVersionUID);
		}
	}
}
