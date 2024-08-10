package com.myorg;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.applicationautoscaling.EnableScalingProps;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.events.targets.SnsTopic;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.sns.subscriptions.SqsSubscription;
import software.amazon.awscdk.services.sqs.DeadLetterQueue;
import software.amazon.awscdk.services.sqs.Queue;
import software.constructs.Construct;

import java.util.HashMap;
import java.util.Map;


public class Service02Stack extends Stack {


    public Service02Stack(final Construct scope, final String id, Cluster cluster, SnsTopic productEventsTopic) {
        this(scope, id, null, cluster, productEventsTopic);
    }

    public Service02Stack(final Construct scope, final String id, final StackProps props, Cluster cluster, SnsTopic productEventsTopic) {
        super(scope, id, props);

        Queue productEventsDlq = Queue.Builder.create(this, "ProductEventsDlq")
                .queueName("product-events-dlq")
                .build();

        DeadLetterQueue deadLetterQueue = DeadLetterQueue
                .builder()
                .queue(productEventsDlq)
                .maxReceiveCount(3)
                .build();

        Queue productEventsQueue = Queue.Builder.create(this, "ProductEvents")
                .queueName("product-events")
                .deadLetterQueue(deadLetterQueue)
                .build();

        SqsSubscription sqsSubscription = SqsSubscription.Builder.create(productEventsQueue).build();
        productEventsTopic.getTopic().addSubscription(sqsSubscription);

        Map<String, String> environmentVariables = new HashMap<>();
        environmentVariables.put("AWS_REGION", "us-east-1");
        environmentVariables.put("AWS_SQS_QUEUE_PRODUCT_EVENT_NAME", productEventsQueue.getQueueName());


        ApplicationLoadBalancedFargateService productServiceConsumer = ApplicationLoadBalancedFargateService
                .Builder
                .create(this, "ALB02")
                .cluster(cluster)
                .serviceName("mv-service-01")
                .listenerPort(8081)
                .desiredCount(2)
                .memoryLimitMiB(1024)
                .cpu(512)
                .taskImageOptions(ApplicationLoadBalancedTaskImageOptions.builder()
                        .containerName("aws-mv-product-consumer")
                        .image(ContainerImage.fromRegistry("mateusvalentim/aws-mv-product-consumer:1.0.0"))
                        .containerPort(8081)
                        .logDriver(LogDriver.awsLogs(AwsLogDriverProps.builder()
                                .logGroup(LogGroup.Builder.create(this, "aws-mv-consumer-log-group")
                                        .logGroupName("aws-mv-consumer-log-group")
                                        .removalPolicy(RemovalPolicy.DESTROY)
                                        .build())
                                .streamPrefix("product-service-consumer")
                                .build()))
                        .environment(environmentVariables)
                        .build())
                .publicLoadBalancer(true)
                .build();

        productServiceConsumer.getTargetGroup().configureHealthCheck(new HealthCheck.Builder()
                .path("/actuator/healt")
                .port("8081")
                .healthyHttpCodes("200")
                .build());

        ScalableTaskCount scalableTaskCount = productServiceConsumer.getService().autoScaleTaskCount(EnableScalingProps.builder()
                .minCapacity(2)
                .maxCapacity(4)
                .build());

        scalableTaskCount.scaleOnCpuUtilization("product-service-consumer-auto-scaling", CpuUtilizationScalingProps.builder()
                .targetUtilizationPercent(70)
                .scaleInCooldown(Duration.seconds(60))
                .scaleOutCooldown(Duration.seconds(60))
                .build());
    }
}
