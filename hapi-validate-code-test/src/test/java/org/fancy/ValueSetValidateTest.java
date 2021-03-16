package org.fancy;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;

import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.Test;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;

/**
 * Before running this test, start the HAPI docker container with command: <br>
 * <code>docker run -it -p 8080:8080 --rm hapiproject/hapi:latest</code>
 */
public class ValueSetValidateTest {

  private static final String SERVER_BASE = "http://localhost:8080/fhir";
  private static final String CODE_SYSTEM_PATH = "/fancy.json";
  private static final String VALUE_SET_PATH = "/fancyValueSet.json";

  @Test()
  public void testOnValidationOnInstance() {

    FhirContext context = FhirContext.forR4();

    IGenericClient client = createClient(context);
    IParser jsonParser = context.newJsonParser().setPrettyPrint(true);

    CodeSystem codeSystem = loadCodeSystem(context, jsonParser);
    ValueSet valueSet = loadValueSet(context, jsonParser);

    client.update().resource(codeSystem).execute();
    client.update().resource(valueSet).execute();

    String code = valueSet.getCompose().getIncludeFirstRep().getConceptFirstRep().getCode();

    Parameters parameters =
        new Parameters()
            .addParameter("code", new CodeType(code))
            .addParameter("system", codeSystem.getUrlElement());

    IdType valueSetId = valueSet.getIdElement().toUnqualifiedVersionless();
    Parameters validationResult =
        client
            .operation()
            .onInstance(valueSetId)
            .named("validate-code")
            .withParameters(parameters)
            .execute();

    System.err.println(jsonParser.encodeResourceToString(validationResult));

    boolean result = validationResult.getParameterBool("result");
    assertTrue(result); // this works!
  }

  private ValueSet loadValueSet(FhirContext context, IParser jsonParser) {
    InputStream resourceAsStream = getClass().getResourceAsStream(VALUE_SET_PATH);
    return jsonParser.parseResource(ValueSet.class, resourceAsStream);
  }

  private CodeSystem loadCodeSystem(FhirContext context, IParser jsonParser) {
    InputStream resourceAsStream = getClass().getResourceAsStream(CODE_SYSTEM_PATH);
    return jsonParser.parseResource(CodeSystem.class, resourceAsStream);
  }

  private IGenericClient createClient(FhirContext context) {
    IGenericClient client = context.newRestfulGenericClient(SERVER_BASE);

    LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
    loggingInterceptor.setLogRequestSummary(true);
    loggingInterceptor.setLogRequestBody(false);
    client.registerInterceptor(loggingInterceptor);
    return client;
  }
}
