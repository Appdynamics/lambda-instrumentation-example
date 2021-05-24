# Containerized Lambda Function Monitoring

This example demonstrates how to instrument a Node.JS or Python Lambda function packaged in a container image using AppDynamics via the AppDynamics AWS Lambda Extension. The example `serverless.yml` file makes the assumption that the image build process will be handled by Serverless. The example `Dockerfile` takes in build-time arguments used to download the extension into the container image.

Currently the Serverless framework does not support Docker build-time arguments.
