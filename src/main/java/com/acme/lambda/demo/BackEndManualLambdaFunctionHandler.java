package com.acme.lambda.demo;

import java.util.HashMap;
import java.io.*;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.InvokeRequest;

import com.appdynamics.serverless.tracers.aws.api.AppDynamics;
import com.appdynamics.serverless.tracers.aws.api.Tracer;
import com.appdynamics.serverless.tracers.aws.api.Transaction;
import com.appdynamics.serverless.tracers.aws.api.ExitCall;

import java.nio.charset.Charset;
import java.util.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.io.IOUtils;

/**
 * This class is an example for a Lambda function that is called internally. This uses manual tracer setup for AppD.
 * 
 * The request / response data for this example is read from/written to an InputStream / OutputStream. It can also be strongly typed.
 * @author Wayne Brown
 */
public class BackEndManualLambdaFunctionHandler implements RequestStreamHandler {

	Transaction txn = null;
	Tracer tracer = null;

	/**
	 * Handles the Lambda request
	 * @param inputStream The input stream
	 * @param outputStream The output stream
	 * @param context The Lambda context
	 * @throws IOException 
	 */
	public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {		
		
		String retval = "";		
		String correlationHeader = "";
		
		String str = IOUtils.toString(inputStream, "UTF-8");
		Gson gson = new GsonBuilder().create();
		Type type = new TypeToken<HashMap<String, String>>() {
		}.getType();
		HashMap<String, String> lambda_body = gson.fromJson(str, type);

		// Setting a default BT name if one does not come across as part of the payload.
		String bt_name = "/";
		if (lambda_body.containsKey("bt_name")) {
			bt_name = lambda_body.get("bt_name");
		}

		// We are using manual tracer setup here. See the docs at https://docs.appdynamics.com/display/PRO45/Instrument+Your+Function+Code for auto tracer setup.
		AppDynamics.Config.Builder configBuilder = new AppDynamics.Config.Builder();
		configBuilder.accountName(System.getenv("ACCOUNT_NAME")).applicationName(System.getenv("APPLICATION_NAME"))
				.tierName(System.getenv("TIER_NAME")).controllerHost(System.getenv("CONTROLLER_HOST"))
				.controllerPort(Integer.parseInt(System.getenv("CONTROLLER_PORT"))).defaultBtName(bt_name)
				.controllerAccessKey(System.getenv("ACCOUNT_ACCESS_KEY")).lambdaContext(context);

		tracer = AppDynamics.getTracer(configBuilder.build());

		if (lambda_body.containsKey(Tracer.APPDYNAMICS_TRANSACTION_CORRELATION_HEADER_KEY)) {
			correlationHeader = lambda_body.get(Tracer.APPDYNAMICS_TRANSACTION_CORRELATION_HEADER_KEY);
		}

		txn = tracer.createTransaction(correlationHeader);
		txn.start();

		// In this example we're sleeping a random amount of time and returning a random GUID.
		// If there are any issues, the error is reported. Finally, the transaction is stopped.
		// Because we're doing manual tracer setup, we have to specify when to report transaction errors.
		try {
			Random rnd2 = new Random();
			int min_ms = 0;
			Thread.sleep(min_ms + rnd2.nextInt(100));

		} catch (InterruptedException ex) {
			if (txn != null) {
				txn.reportError(ex);
			}
		} catch (Throwable e) {
			if (txn != null) {
				txn.reportError(e);
			}
		} finally {
			retval = UUID.randomUUID().toString();
			if (txn != null) {
				txn.stop();
			}
		}		
		outputStream.write(retval.getBytes(Charset.forName("UTF-8")));
	}

	/**
     * Example of how to call a Lambda function from within a Lambda (passing along the BT).
     * @param bt The BT name
     * @param functionName The Lambda function to call
     */
    public void CallLambda(String bt, String functionName) {
        
        // The payload to send to the Lambda function
        HashMap<String, String> payload = new HashMap<>();
        
        // Our AppD exit call
        ExitCall exitCall = null;

        // Add the BT name to the payload.
        payload.put("bt_name", bt);
        
        // If we have an AppD transaction, build out and start the exit call, and add the correlation header to the payload.
        if (txn != null) {
            HashMap<String, String> identifyingProperties = new HashMap<>();
            identifyingProperties.put("DESTINATION", functionName);
            identifyingProperties.put("DESTINATION_TYPE", "LAMBDA");
            exitCall = txn.createExitCall("CUSTOM", identifyingProperties);
            String outgoingHeader = exitCall.getCorrelationHeader();
            exitCall.start();
            payload.put(Tracer.APPDYNAMICS_TRANSACTION_CORRELATION_HEADER_KEY, outgoingHeader);
        }
        
        // Build out the Lambda client and Lambda invocation request.
        AWSLambda lambdaClient = AWSLambdaClientBuilder.standard().build();
        InvokeRequest invokeRequest = new InvokeRequest().withFunctionName(functionName)
                .withPayload(new Gson().toJson(payload));

        // Invoke the Lambda. If there is an error, record the error as part of the exit call. Finally, stop the exit call.
        try {
            lambdaClient.invoke(invokeRequest).getPayload();
            // System.out.println("Got payload");
        } catch (Throwable e) {
            if (exitCall != null) {
                exitCall.reportError(e);
            }
            e.printStackTrace();
        } finally {
            if (exitCall != null) {
                exitCall.stop();
            }
        }
    }

}
