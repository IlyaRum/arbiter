//package arbiter.data.serialize;
//
//import arbiter.data.Parameter;
//import com.fasterxml.jackson.core.JsonGenerator;
//import com.fasterxml.jackson.databind.JsonSerializer;
//import com.fasterxml.jackson.databind.SerializerProvider;
//
//import java.io.IOException;
//import java.util.List;
//
//public class ParametersMapSerializer extends JsonSerializer<List<Parameter>> {
//
//  @Override
//  public void serialize(List<Parameter> parameters, JsonGenerator gen, SerializerProvider serializers) throws IOException {
//
//    gen.writeStartObject();
//    for (Parameter param : parameters) {
//      if (param != null && param.getMappingFieldName() != null) {
//        gen.writeNumberField(param.getMappingFieldName(), param.getValue());
//      }
//    }
//    gen.writeEndObject();
//  }
//}
