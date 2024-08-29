package com.myorg;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.rds.*;
import software.constructs.Construct;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class RdsStack extends Stack {

    protected static final String RDS_ENDPOINT = "rds-endpoint";
    protected static final String RDS_USER = "admin";
    protected static final String RDS_PASSWORD = "rds-password";

    public RdsStack(final Construct scope, final String id, Vpc vpc) {
        this(scope, id, null, vpc);
    }

    public RdsStack(final Construct scope, final String id, final StackProps props, Vpc vpc) {
        super(scope, id, props);

        /*
        Expondo a senha do banco para ser utilizada no service
         */
        CfnParameter databasePassword = CfnParameter.Builder.create(this, "databasePassword")
                .type("String")
                .description("tiger123")
                .build();

        /*
         Buscando o security group
         */
        ISecurityGroup iSecurityGroup = SecurityGroup.fromSecurityGroupId(this, id, vpc.getVpcDefaultSecurityGroup());
        // qualquer ip tem acesso, porem estamos dentro de uma VPC, apenas dispositivos dentro de uma VPC conseguira acessar essa porta.
        iSecurityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(3306));

        /*
         Obtendo sub-redes privadas da VPC
         */
//        List<ISubnet> publicSubnets = vpc.getPublicSubnets().stream()
//                .map(subnet -> Subnet.fromSubnetId(this, subnet.getSubnetId(), subnet.getSubnetId()))
//                .collect(Collectors.toList());

        /*
         Definindo nome indentificador da instancia, username, password, tipo da instancia, security group, subnet.
         */
        DatabaseInstance databaseInstance = DatabaseInstance.Builder.create(this, "Rds01")
                .instanceIdentifier("aws-mv-product01-db")
                .engine(DatabaseInstanceEngine.mysql(MySqlInstanceEngineProps.builder()
                        //.version(MysqlEngineVersion.VER_5_7_44)
                        .version(MysqlEngineVersion.VER_8_0)
                        .build()))
                .vpc(vpc)
                .credentials(Credentials.fromUsername(RDS_USER, CredentialsFromUsernameOptions.builder()
                        .password(SecretValue.unsafePlainText(databasePassword.getValueAsString()))
                        .build()))
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE3, InstanceSize.MICRO))
                .multiAz(false)
                .allocatedStorage(10)
                .securityGroups(Collections.singletonList(iSecurityGroup))
                .vpcSubnets(SubnetSelection.builder()
                        .subnets(vpc.getPublicSubnets())
                        .build())
                .publiclyAccessible(true)
                .build();

        /*
        Expondo o endpoint e senha do DB
         */
        CfnOutput.Builder.create(this, "RdsEndpoint")
                .exportName("rds-endpoint")
                .value(databaseInstance.getDbInstanceEndpointAddress())
                .build();

        CfnOutput.Builder.create(this, "RdsPassword")
                .exportName("rds-password")
                .value(databasePassword.getValueAsString())
                .build();
    }
}