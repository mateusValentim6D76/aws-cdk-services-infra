package com.myorg;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.applicationautoscaling.EnableScalingProps;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.events.targets.SnsTopic;
import software.amazon.awscdk.services.logs.LogGroup;
import software.constructs.Construct;

import java.util.HashMap;
import java.util.Map;


public class Service01Stack extends Stack {


    public Service01Stack(final Construct scope, final String id, Cluster cluster, SnsTopic productEventsTopic) {
        this(scope, id, null, cluster, productEventsTopic);
    }

    public Service01Stack(final Construct scope, final String id, final StackProps props, Cluster cluster, SnsTopic productEventsTopic) {
        super(scope, id, props);

        Map<String, String> environmentVariables = new HashMap<>();
        environmentVariables.put("SPRING_DATASOURCE_URL", "jdbc:mariadb://" + Fn.importValue(RdsStack.RDS_ENDPOINT) + ":3306/aws-mv-product?createDatabaseIfNotExist=true");
        environmentVariables.put("SPRING_DATASOURCE_USERNAME", RdsStack.RDS_USER);
        environmentVariables.put("SPRING_DATASOURCE_PASSWORD", Fn.importValue(RdsStack.RDS_PASSWORD));
        environmentVariables.put("AWS_REGION", "us-east-1");
        environmentVariables.put("AWS_SNS_TOPIC_PRODUCT_EVENTS_ARN", productEventsTopic.getTopic().getTopicArn());

        ApplicationLoadBalancedFargateService service01 = ApplicationLoadBalancedFargateService
                .Builder
                .create(this, "ALB01")
                .cluster(cluster)
                .serviceName("mv-service-01")
                .listenerPort(8080)
                .desiredCount(2)
                .memoryLimitMiB(1024)
                .cpu(512)
                .taskImageOptions(ApplicationLoadBalancedTaskImageOptions.builder()
                        .containerName("aws-mv-product-service")
                        .image(ContainerImage.fromRegistry("mateusvalentim/aws_mv_project1:1.2.0"))
                        .containerPort(8080)
                        .logDriver(LogDriver.awsLogs(AwsLogDriverProps.builder()
                                .logGroup(LogGroup.Builder.create(this, "aws-mv-product-service")
                                        .logGroupName("aws-mv-product-service")
                                        .removalPolicy(RemovalPolicy.DESTROY)
                                        .build())
                                .streamPrefix("product-service")
                                .build()))
                        .environment(environmentVariables)
                        .build())
                .publicLoadBalancer(true)
                .build();

        service01.getTargetGroup().configureHealthCheck(new HealthCheck.Builder()
                .path("/actuator/healt")
                .port("8080")
                .healthyHttpCodes("200")
                .build());

        ScalableTaskCount scalableTaskCount = service01.getService().autoScaleTaskCount(EnableScalingProps.builder()
                .minCapacity(2)
                .maxCapacity(4)
                .build());

        scalableTaskCount.scaleOnCpuUtilization("product-service-auto-scaling", CpuUtilizationScalingProps.builder()
                .targetUtilizationPercent(70)
                .scaleInCooldown(Duration.seconds(60))
                .scaleOutCooldown(Duration.seconds(60))
                .build());

        productEventsTopic.getTopic().grantPublish(service01.getTaskDefinition().getTaskRole());
    }
}
