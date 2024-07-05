package com.myorg;

import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ecs.AwsLogDriverProps;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.LogDriver;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.logs.LogGroup;
import software.constructs.Construct;


public class Service01Stack extends Stack {
    public Service01Stack(final Construct scope, final String id, Cluster cluster) {
        this(scope, id, null, cluster);
    }

    public Service01Stack(final Construct scope, final String id, final StackProps props, Cluster cluster) {
        super(scope, id, props);
        ApplicationLoadBalancedFargateService.Builder
                .create(this, "ALB01")
                .cluster(cluster)
                .serviceName("mv-service-01")
                .listenerPort(8080)
                .desiredCount(2)
                .memoryLimitMiB(1024)
                .cpu(512)
                .taskImageOptions(ApplicationLoadBalancedTaskImageOptions.builder()
                        .containerName("mv_aws_container01")
                        .image(ContainerImage.fromRegistry("mateusvalentim/aws_mv_project1:1.0.0"))
                        .containerPort(8080)
                        .logDriver(LogDriver.awsLogs(AwsLogDriverProps.builder()
                                .logGroup(LogGroup.Builder.create(this, "MvServiceLogGroup01")
                                        .logGroupName("Service01")
                                        .removalPolicy(RemovalPolicy.DESTROY)
                                        .build())
                                .streamPrefix("Service01")
                                .build()))
                        .build())
                .publicLoadBalancer(true)
                .build();
    }
}
