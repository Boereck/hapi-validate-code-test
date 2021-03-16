package org.fancy;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.Test;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;

/**
 * Before running this test, start the HAPI docker container with command: <br>
 * <code>docker run -it -p 8080:8080 --rm hapiproject/hapi:latest</code>
 */
class CodeSystemValidateTest {

  private static final String SERVER_BASE = "http://localhost:8080/fhir";
  private static final String CODE_SYSTEM_PATH = "/fancy.json";

  @Test
  void testOnValidationOnInstance() {

    FhirContext context = FhirContext.forR4();

    IGenericClient client = createClient(context);
    IParser jsonParser = context.newJsonParser().setPrettyPrint(true);

    CodeSystem codeSystem = loadCodeSystem(context);

    MethodOutcome updateOutcome = client.update().resource(codeSystem).execute();
    // MethodOutcome updateOutcome = client.create().resource(codeSystem).execute();
    IBaseResource updateResultResource = updateOutcome.getResource();

    String updateResultStr = jsonParser.encodeResourceToString(updateResultResource);
    System.err.println(updateResultStr);

    Parameters parameters =
        new Parameters()
            // .addParameter("system", codeSystem.getUrlElement())
            .addParameter("code", new CodeType("fancy-two"));

    IdType codeSystemId = codeSystem.getIdElement().toUnqualifiedVersionless();
    // IIdType codeSystemId = updateResultResource.getIdElement();
    Parameters validationResult =
        client
            .operation()
            .onInstance(codeSystemId)
            .named("validate-code")
            .withParameters(parameters)
            .execute();

    System.err.println(jsonParser.encodeResourceToString(validationResult));

    boolean result = validationResult.getParameterBool("result");
    assertTrue(result); // this fails
  }

  @Test
  void testOnValidationOnType() {

    FhirContext context = FhirContext.forR4();

    IGenericClient client = createClient(context);
    IParser jsonParser = context.newJsonParser().setPrettyPrint(true);

    CodeSystem codeSystem = loadCodeSystem(context);

    MethodOutcome updateOutcome = client.update().resource(codeSystem).execute();
    //    MethodOutcome updateOutcome = client.create().resource(codeSystem).execute();
    IBaseResource updateResultResource = updateOutcome.getResource();

    String updateResultStr = jsonParser.encodeResourceToString(updateResultResource);
    System.err.println(updateResultStr);

    Parameters parameters =
        new Parameters()
            .addParameter("url", codeSystem.getUrlElement())
            .addParameter("code", new CodeType("fancy-two"));

    Parameters validationResult =
        client
            .operation()
            .onType(CodeSystem.class)
            .named("validate-code")
            .withParameters(parameters)
            .execute();

    System.err.println(jsonParser.encodeResourceToString(validationResult));

    boolean result = validationResult.getParameterBool("result");
    assertTrue(result); // this fails
  }

  private IGenericClient createClient(FhirContext context) {
    IGenericClient client = context.newRestfulGenericClient(SERVER_BASE);

    LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
    loggingInterceptor.setLogRequestSummary(true);
    loggingInterceptor.setLogRequestBody(false);
    client.registerInterceptor(loggingInterceptor);
    return client;
  }

  private CodeSystem loadCodeSystem(FhirContext context) {
    IParser jsonParser = context.newJsonParser();
    InputStream resourceAsStream = getClass().getResourceAsStream(CODE_SYSTEM_PATH);
    return jsonParser.parseResource(CodeSystem.class, resourceAsStream);
  }
}
