///*
// * Copyright 2022 Harness Inc. All rights reserved.
// * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// * that can be found in the licenses directory at the root of this repository, also available at
// * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
// */
//
// package io.harness.delegate.beans.instancesync.mapper;
//
// import io.harness.annotations.dev.HarnessTeam;
// import io.harness.annotations.dev.OwnedBy;
// import io.harness.delegate.beans.instancesync.ServerInstanceInfo;
// import io.harness.delegate.beans.instancesync.info.SshWinRmServerInstanceInfo;
// import lombok.experimental.UtilityClass;
// import software.wings.service.impl.aws.model.AwsEC2Instance;
//
// import java.util.List;
// import java.util.stream.Collectors;
//
//@UtilityClass
//@OwnedBy(HarnessTeam.CDP)
//@Deprecated
// public class AwsEc2InstanceToServiceInstanceInfoMapper {
//
//  public ServerInstanceInfo toServerInstanceInfo(AwsEC2Instance awsEC2Instance) {
//    return SshWinRmServerInstanceInfo.builder()
//        .host(awsEC2Instance.getPublicDnsName())
//        .build();
//  }
//
//  public List<ServerInstanceInfo> toServerInstanceInfoList(List<AwsEC2Instance> list) {
//    return list.stream()
//        .map(AwsEc2InstanceToServiceInstanceInfoMapper::toServerInstanceInfo)
//        .collect(Collectors.toList());
//  }
//
//}
