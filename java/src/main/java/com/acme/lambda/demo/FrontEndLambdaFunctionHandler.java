package com.acme.lambda.demo;

import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.InvokeRequest;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import com.google.gson.Gson;

import com.appdynamics.serverless.tracers.aws.api.AppDynamics;
import com.appdynamics.serverless.tracers.aws.api.Tracer;
import com.appdynamics.serverless.tracers.aws.api.Transaction;
import com.appdynamics.serverless.tracers.aws.api.ExitCall;

import java.util.*;


/**
 * This class is an example for a Lambda function that is wired up to an AWS API Gateway instance. 
 * 
 * The request / response data for this example is strongly typed. It can also be derived from an input/output stream.
 * @author Wayne Brown
 */
public class FrontEndLambdaFunctionHandler implements RequestHandler<WebFrontEndRequest, WebFrontEndResponse> {
    
    Tracer tracer = null;
    Transaction txn = null;

    /**
     * Handles the Lambda request.
     * @param input The input request data from the API Gateway.
     * @param context The lambda context.
     * @return Response data to send back through the API Gateway.
     */
    @Override
    public WebFrontEndResponse handleRequest(WebFrontEndRequest input, Context context) {
        WebFrontEndResponse resp = null;                

        String correlationHeader = "";
        
        // Setting a default BT name if one does not come across from the AWS API Gateway.
        String bt_name = "/";
        if (!input.getPath().equals(null)) {
            bt_name = input.getPath();
        }        

        // Build the AppD configuration. Components are assumed to be in Lambda environment variables.
        // We are doing manual tracer setup. See the docs at https://docs.appdynamics.com/display/PRO45/Instrument+Your+Function+Code for
        // requirements for auto tracer setup. 
        AppDynamics.Config.Builder configBuilder = new AppDynamics.Config.Builder();
        configBuilder.accountName(System.getenv("ACCOUNT_NAME")).applicationName(System.getenv("APPLICATION_NAME"))
					.tierName(System.getenv("TIER_NAME")).controllerHost(System.getenv("CONTROLLER_HOST"))
					.controllerPort(Integer.parseInt(System.getenv("CONTROLLER_PORT"))).defaultBtName(bt_name)
					.controllerAccessKey(System.getenv("ACCOUNT_ACCESS_KEY")).lambdaContext(context);

        tracer = AppDynamics.getTracer(configBuilder.build());        

        if (input.getSingularity() != null) {
            correlationHeader = input.getSingularity().toString();
        } else {
            if (input.getHeaders() != null
                    && input.getHeaders().containsKey(Tracer.APPDYNAMICS_TRANSACTION_CORRELATION_HEADER_KEY)) {
                correlationHeader = input.getHeaders().get(Tracer.APPDYNAMICS_TRANSACTION_CORRELATION_HEADER_KEY);
            }
        }

        txn = tracer.createTransaction(correlationHeader);
        txn.start();             

        // Put your Lambda implementation here.
        
        // End put your Lambda implementation here.


        // For this example we're building a response to send back through the API Gateway.
        resp = WebFrontEndResponse.Builder.newInstance().withStatusCode(200).withIsBase64Encoded(false)
                .withHeader("Content-Type", "application/json").withBody("{ \"status\" :\"OK\" }").create();

        // Ending the transaction manually because tracer setup was done manually.
        if (txn != null) {
            txn.stop();
        }

        return resp;        
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
