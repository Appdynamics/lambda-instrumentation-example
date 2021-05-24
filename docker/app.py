import json
import random
import time

def handler(event, context):

    time.sleep(random.uniform(0.3, 0.8))

    body = {
        "message": "Hello, world, from Python 3.8! Your function executed successfully!",
    }

    response = {
        "statusCode": 200,
        "body": json.dumps(body)
    }

    return response
