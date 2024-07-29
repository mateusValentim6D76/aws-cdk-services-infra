package com.myorg;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.rds.*;
import software.constructs.Construct;

import java.util.Collection;
import java.util.Collections;

public class RdsStack extends Stack {

    protected static final String RDS_ENDPOINT = "rds_endpoint";
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
                .defaultValue("Senha da instancia RDS")
                .build();

        /*
         Buscando o security group
         */
        ISecurityGroup iSecurityGroup = SecurityGroup.fromSecurityGroupId(this, id, vpc.getVpcDefaultSecurityGroup());
        //qualquer ip tem acesso, porem estamos dentro de uma VPC, apenas dispositivos dentro de uma VPC conseguira acessar essa porta.
        iSecurityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(3306));

        /*
         Definindo nome indentificador da instancia, username, password, tipo da instancia, security group, subnet.
         */
        DatabaseInstance databaseInstance = DatabaseInstance.Builder.create(this, "Rds01")
                .instanceIdentifier("aws_mv_project01_db")
                .engine(DatabaseInstanceEngine.mysql(MySqlInstanceEngineProps.builder()
                        .version(MysqlEngineVersion.VER_5_7)
                        .build()))
                .vpc(vpc)
                .credentials(Credentials.fromUsername(RDS_USER, CredentialsFromUsernameOptions.builder()
                        .password(SecretValue.plainText(databasePassword.getValueAsString()))
                        .build()))
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE2, InstanceSize.MICRO))
                .multiAz(false)
                .allocatedStorage(10)
                .securityGroups(Collections.singletonList(iSecurityGroup))
                .vpcSubnets(SubnetSelection.builder()
                        .subnets(vpc.getPrivateSubnets())
                        .build())
                .build();
        /*
        Expondo o endpoint e senha do DB
         */
        CfnOutput.Builder.create(this, RDS_ENDPOINT)
                .exportName(RDS_ENDPOINT)
                .value(databaseInstance.getDbInstanceEndpointAddress())
                .build();

        CfnOutput.Builder.create(this, RDS_PASSWORD)
                .exportName(RDS_PASSWORD)
                .value(databasePassword.getValueAsString())
                .build();
    }
}