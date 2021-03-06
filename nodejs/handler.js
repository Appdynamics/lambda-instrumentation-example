// TODO: Add in call to require AppDynamics Tracer
const tracer = require('appdynamics-lambda-tracer');


const AWS = require('aws-sdk');

// Other requirements
const _ = require('lodash');
const util = require('util');
const { v4: uuidv4 } = require('uuid');
const faker = require('faker');
const { exit } = require('process');

const doStuff = util.promisify(setTimeout);
const dynamoDb = new AWS.DynamoDB.DocumentClient();

// First lambda function
module.exports.doFunctionAsync = async (event, context) => {

    const response = {
        headers: { 'Access-Control-Allow-Origin': '*' }, // CORS requirement
        statusCode: 200,
    };

    if (event.path == "/person/submit") {
        var person = personInfo();

        // TODO: Add in exit call creation for DynamoDB
        var exitCall = null; 
        if (tracer != null) {
            exitCall = tracer.startExitCall({
                exitType: 'CUSTOM',
                exitSubType: 'Amazon Web Services',
                identifyingProperties: {
                    'VENDOR': process.env.CANDIDATE_TABLE + " DynamoDB"
                }
            });
        }

        try {

            var person_result = await submitPerson(person);
            var result = {
                status: "PersonCreated",
                data: person
            };

            response.body = JSON.stringify(result);
            response.statusCode = 201;

        } catch (e) {
            // Example of reporting an exit call error
            if (tracer != null && exitCall != null) {
                tracer.reportExitCallError(exitCall, 'DynamoDB Error', 'Error in making the DynamoDB query for submitting person');
            }

            response.statusCode = 500;
            var result = {
                status: "Error",
                data: e
            };
            response.body = JSON.stringify(result);

        }

        // Example of stopping the exit call.
        if (tracer != null && exitCall != null) {
            tracer.stopExitCall(exitCall);
        }

    } else if (event.path == "/person/random") {
        const lambda = new AWS.Lambda();

        var params = {
            FunctionName: context.functionName.replace("lambda-1", "lambda-2"),
            InvocationType: "RequestResponse",
            Payload: '{}'
        };

        // NOTE: With the NodeJS tracer, we do not have to manually set up exit calls to 
        // other Lambda functions as we do with the Java tracer.

        try {
            var lambda_resp = await lambda.invoke(params).promise();
            var data = JSON.parse(lambda_resp.Payload);
            var result = {
                status: "Found",
                data: data.Item
            }
            response.body = JSON.stringify(result);
        } catch (e) {
            response.statusCode = 500;
            var result = {
                status: "Error",
                data: e
            };
            response.body = JSON.stringify(result);
        }
    } else {
        var ms = _.random(50, 500);
        await doStuff(ms);

        // Generating a random error 5% of the time to show custom error reporting.
        if (_.random(10, 100) % 20 == 0) {

            tracer.reportTransactionError(new Error('Unknown error from ' + event.path));

            response.statusCode = 500;
            response.body = JSON.stringify({
                status: "Unknown error from " + event.path,
                data: context
            });
        } else {
            response.body = JSON.stringify({
                status: 'Hello AppDynamics Lambda Monitoring - Async JS handler from ' + event.path + ". ",
                data: context
            });
        }        
    }


    return response;
};

// Second lambda function
module.exports.doFunctionAsync2 = async (event, context) => {

    var id_results, ids, id;

    // Example of a custom exit call.
    var exitCall = null;
    if (tracer != null) {
        exitCall = tracer.startExitCall({
            exitType: 'CUSTOM',
            exitSubType: 'Amazon Web Services',
            identifyingProperties: {
                'VENDOR': process.env.CANDIDATE_TABLE + " DynamoDB"
            }
        });
    }

    try {
        id_results = await getPersonIds();
        ids = _(id_results.Items).map(function (i) {
            return i.id;
        }).value();

        id = ids[_.random(ids.length - 1)];

        try {
            var person = await getPerson(id);

            // Example of stopping the exit call.
            if (tracer != null && exitCall != null) {
                tracer.stopExitCall(exitCall);
            }

            context.succeed(person);
        } catch (e2) {

            // Example of reporting an exit call error followed by stopping the exit call.
            if (tracer != null && exitCall != null) {
                tracer.reportExitCallError(exitCall, 'DynamoDB Error', 'Error in making the DynamoDB query for retrieving person');
                tracer.stopExitCall(exitCall);
            }

            context.fail(e2);
        }
    } catch (e) {
        // Example of reporting an exit call error followed by stopping the exit call.
        if (tracer != null && exitCall != null) {
            tracer.reportExitCallError(exitCall, 'DynamoDB Error', 'Error in making the DynamoDB query for retrieving person set');        
            tracer.stopExitCall(exitCall);
        }        

        context.fail(e);
    }

};

const getPersonIds = () => {
    const params = {
        TableName: process.env.CANDIDATE_TABLE,
        ProjectionExpression: "id"
    };

    return dynamoDb.scan(params).promise();
}

const getPerson = i => {
    const params2 = {
        TableName: process.env.CANDIDATE_TABLE,
        Key: {
            "id": i
        }
    };

    return dynamoDb.get(params2).promise();
}

const submitPerson = p => {
    const personInfo = {
        TableName: process.env.CANDIDATE_TABLE,
        Item: p
    };

    return dynamoDb.put(personInfo).promise();
};

const personInfo = () => {
    var timestamp = new Date().getTime();
    var retval = faker.helpers.userCard();
    retval.id = uuidv4();
    retval.submittedAt = timestamp;
    retval.updatedAt = timestamp;

    return retval;
};