import appdynamics

import time
import boto3
import json
from random import seed
from random import randint

lambda_client = boto3.client('lambda')

@appdynamics.tracer
def lambda_handler(event, context):  

    if event['path'] == "/employee/random":
        response = lambda_client.invoke(
            FunctionName = 'appd-lambda-python-dev-lambda-2',
            InvocationType = 'RequestResponse'
        )

        responsePayload = response['Payload'].read().decode('utf-8')

        retval = {}

        if responsePayload is None:            
            retval = {
                "statusCode" : 404,
                "body" : None
            }
        elif randint(1, 100) == 74:             # Throw a random error in the transaction
            appdynamics.report_error(error_name="Unknown", error_message="Unknown Error in Lambda Handler")
            retval = {
                "statusCode" : 500,
                "body" : "Unknown Error"
            }
        else:
            retval = {
                "statusCode" : 200,
                "body" : responsePayload
            }
        
    else:
        retval = {}
        retval = {
            "statusCode" : 200,
            "body" : "Huzzah!"
        }

    return retval
