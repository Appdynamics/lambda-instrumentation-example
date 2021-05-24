FROM amazon/aws-cli:2.2.4 AS downloader

ARG version_number=10
ARG access_key
ARG secret_key
ARG region
ARG cachebust

ENV AWS_ACCESS_KEY_ID=${access_key}
ENV AWS_SECRET_ACCESS_KEY=${secret_key}
ENV AWS_REGION=${region}
ENV VERSION_NUMBER=${version_number}

RUN yum install -y jq curl
RUN echo "Cache Bust: $cachebust"
RUN aws lambda get-layer-version-by-arn --arn arn:aws:lambda:$AWS_REGION:716333212585:layer:appdynamics-lambda-extension:$VERSION_NUMBER | jq -r '.Content.Location' | xargs curl -o extension.zip

FROM public.ecr.aws/lambda/python:3.8

COPY --from=downloader /aws/extension.zip .
RUN yum install -y unzip && unzip extension.zip -d /opt && rm -f extension.zip

COPY app.py ./

# You can overwrite command in `serverless.yml` template
CMD ["app.handler"]
