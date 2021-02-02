package havis.device.rf.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Provides the serialization/deserialization of objects in JSON format. This
 * implementation uses the jackson JSON processor. Changes of optional parameter
 * (e.g. pretty print, mix-ins) is not possible after the first
 * serialization/deserialize. In this case, re-initialize the serializer first.
 */
public class JsonSerializer implements Serializable {
	private static final long serialVersionUID = -6643155452416709683L;

	ObjectMapper mapper;

	// Class Type
	private final Class<?> clazz;

	/**
	 * Initializes the serializer with a special class type. This class type
	 * will be used to deserialize JSON strings.
	 * 
	 * @param clazz the class type of the serializer
	 */
	public <T> JsonSerializer(Class<T> clazz) {
		this.clazz = clazz;
		
		mapper = new ObjectMapper();
		mapper.enableDefaultTyping();
		mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);	
	}

	/**
	 * Enables/Disables the prettyPrint for the JSON structure.
	 * 
	 * @param enable whether to enable pretty print
	 */
	public void setPrettyPrint(boolean enable) {
		if (enable)
			mapper.enable(SerializationFeature.INDENT_OUTPUT);
	}
	
	/**
	 * Serializes an object to a JSON string.
	 * 
	 * @param obj the object to serialize
	 * @return the serialized object as JSON string
	 * @throws IOException
	 */
	public String serialize(Object obj) throws IOException {
		return mapper.writeValueAsString(obj);
	}

	/**
	 * Deserializes a JSON string to an object.
	 * 
	 * @param json the JSON string to deserialize
	 * @return the deserialized object
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public <T> T deserialize(String json) throws JsonParseException,
			JsonMappingException, IOException {
		return (T) mapper.readValue(json, clazz);
	}

	/**
	 * Deserializes a JSON stream to an object.
	 * 
	 * @param stream the JSON stream to deserialize
	 * @return the deserialized object
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public <T> T deserialize(InputStream stream, Class<T> clazz) throws JsonParseException, JsonMappingException, IOException {
		return (T) mapper.readValue(stream, clazz);
	}
}