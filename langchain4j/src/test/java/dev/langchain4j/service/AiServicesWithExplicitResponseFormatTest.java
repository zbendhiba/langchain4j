package dev.langchain4j.service;

import static dev.langchain4j.data.message.UserMessage.userMessage;
import static dev.langchain4j.model.chat.Capability.RESPONSE_FORMAT_JSON_SCHEMA;
import static dev.langchain4j.model.chat.request.ResponseFormatType.JSON;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.mock.ChatModelMock;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AiServicesWithExplicitResponseFormatTest {

    interface Assistant {
        String chat(String userMessage);
    }

    interface PersonExtractor {

        class Person {
            String name;
            Integer age;
        }

        Person extractPersonFrom(String text);
    }

    @Test
    void should_use_explicit_response_format_on_builder() {

        // given
        ChatModel model = spy(ChatModelMock.thatAlwaysResponds("hello"));

        ResponseFormat explicitFormat = ResponseFormat.builder()
                .type(JSON)
                .jsonSchema(JsonSchema.builder()
                        .name("MySchema")
                        .rootElement(JsonObjectSchema.builder()
                                .addStringProperty("result")
                                .build())
                        .build())
                .build();

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .responseFormat(explicitFormat)
                .build();

        // when
        assistant.chat("hello");

        // then
        verify(model)
                .chat(ChatRequest.builder()
                        .messages(singletonList(userMessage("hello")))
                        .responseFormat(explicitFormat)
                        .build());
    }

    @Test
    void should_use_json_schema_convenience_method_on_builder() {

        // given
        ChatModel model = spy(ChatModelMock.thatAlwaysResponds("hello"));

        JsonSchema jsonSchema = JsonSchema.builder()
                .name("MySchema")
                .rootElement(
                        JsonObjectSchema.builder().addStringProperty("result").build())
                .build();

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .responseFormat(jsonSchema)
                .build();

        // when
        assistant.chat("hello");

        // then
        verify(model)
                .chat(ChatRequest.builder()
                        .messages(singletonList(userMessage("hello")))
                        .responseFormat(ResponseFormat.builder()
                                .type(JSON)
                                .jsonSchema(jsonSchema)
                                .build())
                        .build());
    }

    @Test
    void should_use_explicit_response_format_instead_of_auto_generated_when_model_supports_json_schema() {

        // given
        ChatModel modelWithJsonSchemaCapability = spy(new ChatModelMock("{\"name\":\"Alice\",\"age\":32}") {
            @Override
            public Set<Capability> supportedCapabilities() {
                return Set.of(RESPONSE_FORMAT_JSON_SCHEMA);
            }
        });

        ResponseFormat explicitFormat = ResponseFormat.builder()
                .type(JSON)
                .jsonSchema(JsonSchema.builder()
                        .name("CustomSchema")
                        .rootElement(JsonObjectSchema.builder()
                                .addStringProperty("name")
                                .addIntegerProperty("age")
                                .build())
                        .build())
                .build();

        PersonExtractor extractor = AiServices.builder(PersonExtractor.class)
                .chatModel(modelWithJsonSchemaCapability)
                .responseFormat(explicitFormat)
                .build();

        // when
        extractor.extractPersonFrom("Alice is 32 years old");

        // then
        verify(modelWithJsonSchemaCapability)
                .chat(ChatRequest.builder()
                        .messages(singletonList(userMessage("Alice is 32 years old")))
                        .responseFormat(explicitFormat)
                        .build());
    }

    @Test
    void should_not_append_output_format_instructions_when_explicit_response_format_is_set() {

        // given
        ChatModel model = spy(ChatModelMock.thatAlwaysResponds("{\"name\":\"Alice\",\"age\":32}"));

        ResponseFormat explicitFormat = ResponseFormat.builder()
                .type(JSON)
                .jsonSchema(JsonSchema.builder()
                        .name("Person")
                        .rootElement(JsonObjectSchema.builder()
                                .addStringProperty("name")
                                .addIntegerProperty("age")
                                .build())
                        .build())
                .build();

        PersonExtractor extractor = AiServices.builder(PersonExtractor.class)
                .chatModel(model)
                .responseFormat(explicitFormat)
                .build();

        // when
        extractor.extractPersonFrom("Alice is 32 years old");

        // then
        verify(model)
                .chat(ChatRequest.builder()
                        .messages(singletonList(userMessage("Alice is 32 years old")))
                        .responseFormat(explicitFormat)
                        .build());
    }

    @Test
    void should_preserve_existing_behavior_when_no_explicit_response_format_is_set() {

        // given
        ChatModel model = spy(ChatModelMock.thatAlwaysResponds("hello"));

        Assistant assistant =
                AiServices.builder(Assistant.class).chatModel(model).build();

        // when
        assistant.chat("hello");

        // then
        verify(model)
                .chat(ChatRequest.builder()
                        .messages(singletonList(userMessage("hello")))
                        .build());
    }

    @Test
    void should_throw_when_response_format_is_null() {

        // when / then
        assertThatThrownBy(() -> AiServices.builder(Assistant.class)
                        .chatModel(ChatModelMock.thatAlwaysResponds("hello"))
                        .responseFormat((ResponseFormat) null)
                        .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("responseFormat");
    }

    @Test
    void should_throw_when_json_schema_is_null() {

        // when / then
        assertThatThrownBy(() -> AiServices.builder(Assistant.class)
                        .chatModel(ChatModelMock.thatAlwaysResponds("hello"))
                        .responseFormat((JsonSchema) null)
                        .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("jsonSchema");
    }

    @Test
    void should_return_parsed_pojo_when_explicit_response_format_is_set() {

        // given
        ChatModel model = ChatModelMock.thatAlwaysResponds("{\"name\":\"Alice\",\"age\":32}");

        ResponseFormat explicitFormat = ResponseFormat.builder()
                .type(JSON)
                .jsonSchema(JsonSchema.builder()
                        .name("Person")
                        .rootElement(JsonObjectSchema.builder()
                                .addStringProperty("name")
                                .addIntegerProperty("age")
                                .build())
                        .build())
                .build();

        PersonExtractor extractor = AiServices.builder(PersonExtractor.class)
                .chatModel(model)
                .responseFormat(explicitFormat)
                .build();

        // when
        PersonExtractor.Person person = extractor.extractPersonFrom("Alice is 32 years old");

        // then
        assertThat(person.name).isEqualTo("Alice");
        assertThat(person.age).isEqualTo(32);
    }
}
