/*
 * Copyright 2022-? the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.stun4j.guid.boot;

import static com.stun4j.guid.core.utils.Asserts.state;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import com.stun4j.guid.boot.GuidProperties.Strategy;
import com.stun4j.guid.boot.support.CuratorVersion;
import com.stun4j.guid.core.LocalGuid;
import com.stun4j.guid.core.LocalZkGuid;
import com.stun4j.guid.core.ZkGuidNode;
import com.stun4j.guid.core.utils.Exceptions;
import com.stun4j.guid.core.utils.NetworkUtils;

/**
 * Responsible for initializing Guid from its boot configuration files.
 * @author Jay Meng
 */
@Configuration
@EnableConfigurationProperties(GuidProperties.class)
public class GuidAutoConfigure {
  private static final Logger LOG = LoggerFactory.getLogger(GuidAutoConfigure.class);

  @Bean
  LocalGuid localGuid() {
    return LocalGuid.instance();
  }

  GuidAutoConfigure(GuidProperties props, ApplicationArguments appArgs) {
    // guid initialization if necessary
    if (!LocalGuid.isAllowInitialization()) {
      LOG.warn("The local-guid has already been initialized, the initialization initiated by Stf was ignored");
    } else {
      List<String> guidStrategies;
      if ((guidStrategies = appArgs.getOptionValues("stun4j.guid.strategy")) != null
          && StringUtils.hasText(guidStrategies.get(0))) {
        props.setStrategy(Strategy.valueOf(guidStrategies.get(0).toUpperCase()));
      }

      List<String> ipPres;
      BitEditing bitEdit = props.getBitEditing();
      switch (props.getStrategy()) {
        case ZK:
          String curatorVer = CuratorVersion.getVersion();
          String advice = "Please check 'https://github.com/stun4j/stun4j-guid/blob/master/README_en_US.md#notes' for detail.";
          state(curatorVer != null,
              "You must placed 'org.apache.curator:curator-recipes:2.13.0 or 3.3.0+' in your project's classpath,or you can't communicate with zk. "
                  + advice);
          LOG.info("Your 'curator-recipes' version is {},the recommended version is 2.13.0 or 3.3.0+. {}", curatorVer,
              advice);
          String ipPreToCheck = null;
          if ((ipPres = appArgs.getOptionValues("stun4j.guid.ip-start-with")) != null
              && StringUtils.hasText(ipPres.get(0))) {
            // TODO mj:check ip pattern(very strict)
            ipPreToCheck = ipPres.get(0);
          } else if (StringUtils.hasText(props.getIpStartWith())) {
            ipPreToCheck = props.getIpStartWith();
          }
          String localIp = NetworkUtils.getLocalhost(ipPreToCheck);
          List<String> connStrs = appArgs.getOptionValues("stun4j.guid.zk-conn-addr");
          List<String> nameSpaces = appArgs.getOptionValues("stun4j.guid.zk-namespace");
          String connStr = localIp + ":2181", nameSpace = ZkGuidNode.DFT_ZK_NAMESPACE_GUID;
          if (connStrs != null && StringUtils.hasText((connStrs.get(0)))) {
            connStr = connStrs.get(0);
          } else if (StringUtils.hasText(props.getZkConnAddr())) {
            connStr = props.getZkConnAddr();
          }
          if (nameSpaces != null && StringUtils.hasText((nameSpaces.get(0)))) {
            nameSpace = nameSpaces.get(0);
          } else if (StringUtils.hasText(props.getZkNamespace())) {
            nameSpace = props.getZkNamespace();
          }
          try {
            if (!bitEdit.isEnabled()) {
              LocalZkGuid.init(connStr, nameSpace, ipPreToCheck);
            } else {
              LocalZkGuid.init(connStr, nameSpace, bitEdit.getDigits(), bitEdit.getDatacenterIdBits(),
                  bitEdit.getWorkerIdBits(), bitEdit.getSeqBits(), bitEdit.isFixedDigitsEnabled(), ipPreToCheck);
            }
          } catch (Throwable e) {
            Exceptions.sneakyThrow(e);
          }
          break;
        case LOCAL_IP:
          String ipPreToPick;
          if ((ipPres = appArgs.getOptionValues("stun4j.guid.ip-start-with")) != null
              && StringUtils.hasText(ipPreToPick = ipPres.get(0))) {
            if (!bitEdit.isEnabled()) {
              // TODO mj:check ip pattern(very strict)
              LocalGuid.initWithLocalIp(ipPreToPick);
            } else {
              LocalGuid.initWithLocalIp(bitEdit.getDigits(), bitEdit.isShortDcWkIdBitsWhenUsingLocalIpStrategy(),
                  bitEdit.getSeqBits(), bitEdit.isFixedDigitsEnabled(), ipPreToPick);
            }
          } else if (StringUtils.hasText(ipPreToPick = props.getIpStartWith())) {
            if (!bitEdit.isEnabled()) {
              LocalGuid.initWithLocalIp(ipPreToPick);
            } else {
              LocalGuid.initWithLocalIp(bitEdit.getDigits(), bitEdit.isShortDcWkIdBitsWhenUsingLocalIpStrategy(),
                  bitEdit.getSeqBits(), bitEdit.isFixedDigitsEnabled(), ipPreToPick);
            }
          } else {
            if (!bitEdit.isEnabled()) {
              LocalGuid.initWithLocalIp();
            } else {
              LocalGuid.initWithLocalIp(bitEdit.getDigits(), bitEdit.isShortDcWkIdBitsWhenUsingLocalIpStrategy(),
                  bitEdit.getSeqBits(), bitEdit.isFixedDigitsEnabled());
            }
            LOG.warn(
                "Automatic selection of local IP is too arbitrary > It is highly recommended that you initialize the local-guid by specifying an IP prefix, or the global uniqueness of the guid could be broken");
          }
          break;
        case MANUAL:
          List<String> dcIds = appArgs.getOptionValues("stun4j.guid.datacenter-id");
          List<String> wKIds = appArgs.getOptionValues("stun4j.guid.worker-id");
          int dcId, wkId;
          String warnMsg = "You are running the risk of breaking the global uniqueness of local-guid(Cause you choose the 'manual' strategy) > Consider choose other initialization strategy for local-guid";
          if (dcIds != null || wKIds != null) {
            dcId = (dcIds == null || !StringUtils.hasText(dcIds.get(0))) ? 0 : Integer.parseInt(dcIds.get(0));
            wkId = (wKIds == null || !StringUtils.hasText(wKIds.get(0))) ? 0 : Integer.parseInt(wKIds.get(0));
            if (!bitEdit.isEnabled()) {
              LocalGuid.init(dcId, wkId);
            } else {
              LocalGuid.init(dcId, wkId, bitEdit.getDigits(), bitEdit.getDatacenterIdBits(), bitEdit.getWorkerIdBits(),
                  bitEdit.getSeqBits(), bitEdit.isFixedDigitsEnabled());
            }
            LOG.warn(warnMsg);
          } else if ((dcId = props.getDatacenterId()) >= 0 && (wkId = props.getWorkerId()) >= 0) {
            if (!bitEdit.isEnabled()) {
              LocalGuid.init(dcId, wkId);
            } else {
              LocalGuid.init(dcId, wkId, bitEdit.getDigits(), bitEdit.getDatacenterIdBits(), bitEdit.getWorkerIdBits(),
                  bitEdit.getSeqBits(), bitEdit.isFixedDigitsEnabled());
            }
            LOG.warn(warnMsg);
          } else {
            if (!bitEdit.isEnabled()) {
              LocalGuid.init(0, 0);
            } else {
              LocalGuid.init(0, 0, bitEdit.getDigits(), bitEdit.getDatacenterIdBits(), bitEdit.getWorkerIdBits(),
                  bitEdit.getSeqBits(), bitEdit.isFixedDigitsEnabled());
            }
            LOG.warn(
                "You are running the risk of breaking the global uniqueness of local-guid(Each node takes 0,0 as its dcId and wkId) > It is highly recommended that you choose other initialization strategy for local-guid");
          }
          break;
        default:
          break;
      }
    }
  }

}
