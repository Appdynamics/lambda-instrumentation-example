// First thing first - AppDynamics tracer init
const tracer = require('appdynamics-lambda-tracer');
const _ = require('lodash');
const util = require('util');

const doStuff = util.promisify(setTimeout);

// Initialize tracer with default options.
tracer.init();

module.exports.greet = (event, context, callback) => {


    const response = {
        headers: {'Access-Control-Allow-Origin': '*'}, // CORS requirement
        statusCode: 200,
    };

    response.body = JSON.stringify({
        message: 'Hello AppDynamics Lambda Monitoring'
    });


    callback(null, response);
};

module.exports.greetAsync = async (event, context) => {

    var ms = _.random(50, 500);

    await doStuff(ms);

    const response = {
        headers: {'Access-Control-Allow-Origin': '*'}, // CORS requirement
        statusCode: 200,
    };

    response.body = JSON.stringify({
        message: 'Hello AppDynamics Lambda Monitoring - Async JS handler'
    });


    return response;
};

// Wire the tracer to the NodeJS module
tracer.mainModule(module);
